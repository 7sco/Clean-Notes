package com.codingwithmitch.cleannotes.notes.business.interactors.notelist

import android.content.Context
import android.util.Log
import androidx.work.*
import com.codingwithmitch.cleannotes.core.business.cache.CacheResponseHandler
import com.codingwithmitch.cleannotes.core.business.safeCacheCall
import com.codingwithmitch.cleannotes.core.business.state.*
import com.codingwithmitch.cleannotes.core.util.Constants
import com.codingwithmitch.cleannotes.core.util.TodoCallback
import com.codingwithmitch.cleannotes.core.util.printLogD
import com.codingwithmitch.cleannotes.notes.business.domain.model.Note
import com.codingwithmitch.cleannotes.notes.business.domain.repository.NoteRepository
import com.codingwithmitch.cleannotes.notes.di.NotesFeatureImpl
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.NoteListFragment
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.state.NoteListViewState
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.state.NoteListViewState.*
import com.codingwithmitch.cleannotes.presentation.BaseApplication
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.lang.Runnable
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import javax.inject.Inject

const val DELETENOTE_PENDING_ERROR = "There is already a pending delete operation."
const val DELETENOTE_GENERIC_ERROR = "Error deleting the note."

@FlowPreview
@ExperimentalCoroutinesApi
class DeleteNote(
    private val workManager: WorkManager,
    private val noteRepository: NoteRepository
){
//class DeleteNote<ViewState>(
//    private val noteRepository: NoteRepository
//){

    fun deleteNote(
        notePendingDelete: NotePendingDelete,
        stateEvent: StateEvent
    ): Flow<DataState<NoteListViewState>> = flow {

        if(notePendingDelete.note != null){
            // remove the note from the list
            emit(buildRemovePendingNoteFromListMessage(notePendingDelete))

            // delegate to WorkManager to finish
           queueJobWithWorkManager((notePendingDelete.note as Note).id)
        }
        else{
            emit(buildGenericErrorDeletingNote())
            emit(buildCompleteStateEventMessage(stateEvent))
        }
    }

    private fun buildGenericErrorDeletingNote(): DataState<NoteListViewState>{
        return DataState.error(
            response = Response(
                message = DELETENOTE_GENERIC_ERROR,
                uiComponentType = UIComponentType.Toast(),
                messageType = MessageType.Info()
            ),
            stateEvent = null
        )
    }

    private fun queueJobWithWorkManager(primaryKey: Int) {
        val inputData = workDataOf(
            DELETE_NOTE_WORKER_ARG_PK to primaryKey
        )
        val workRequest = OneTimeWorkRequestBuilder<DeleteNoteWorker>()
            .setInputData(inputData)
            .build()

        workManager
            .enqueueUniqueWork(
                DELETE_NOTE_JOB_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

    }

    // when job is complete, emit StateEvent to DataChannelManager
    private fun buildCompleteStateEventMessage(
        stateEvent: StateEvent
    ): DataState<NoteListViewState>{
        return DataState.data<NoteListViewState>(
            response = null,
            data = null,
            stateEvent = stateEvent
        )
    }

    // Trigger removal of pending note from list
    private fun buildRemovePendingNoteFromListMessage(
        note: NotePendingDelete
    ): DataState<NoteListViewState>{
        return DataState.data(
            response = null,
            data = NoteListViewState(
                notePendingDelete = note
            ),
            stateEvent = null
        )
    }


    class DeleteNoteWorker
    constructor(
        context: Context,
        parameters: WorkerParameters
    ) : CoroutineWorker(context, parameters) {

        @Inject
        lateinit var noteRepository: NoteRepository

        private var primaryKey: Int = -1

        override suspend fun doWork(): Result {

            try{

                inject()

                if(!::noteRepository.isInitialized){
                    throw CancellationException("DeleteNoteWorker: Must set the NoteRepository.")
                }
                primaryKey = inputData.getInt(DELETE_NOTE_WORKER_ARG_PK, -1)
                if(primaryKey < 0){
                    throw CancellationException("DeleteNoteWorker: Missing primary key.")
                }

                // show "undo" snackbar for canceling the delete
                setProgressShowUndoSnackbar()

                delay(100)

                clearProgress(DELETE_NOTE_STATE_MESSAGE)

                // give user time to "undo"
                delay(DELETE_UNDO_TIMEOUT)

                val cacheResult = safeCacheCall(IO){
                    noteRepository.deleteNote(primaryKey)
                }

                val result = object: CacheResponseHandler<NoteListViewState, Int>(
                    response = cacheResult,
                    stateEvent = null
                ){
                    override suspend fun handleSuccess(resultObj: Int): DataState<NoteListViewState> {
                        return if(resultObj > 0){
                            printLogD("observer: DeleteNote", "done getting result")
                            DataState.data(
                                response = Response(
                                    message = DELETE_NOTE_SUCCESS,
                                    uiComponentType = UIComponentType.None(),
                                    messageType = MessageType.None()
                                ),
                                data = null,
                                stateEvent = null
                            )
                        }
                        else{
                            DataState.data(
                                response = Response(
                                    message = DELETE_NOTE_FAILED,
                                    uiComponentType = UIComponentType.None(),
                                    messageType = MessageType.None()
                                ),
                                data = null,
                                stateEvent = null
                            )
                        }
                    }
                }.getResult()

                // BUG?
                // very weird...
                // This MUST be here or the when statement (below) never executes...
                setProgress(
                    workDataOf(
                        "result" to "done getting result from CacheResponseHandler."
                    )
                )

                when(result.stateMessage?.response?.message){

                    DELETE_NOTE_FAILED -> {
                        setProgressDeleteFailed()
                    }

                    DELETE_NOTE_SUCCESS -> {
                        setProgressDeleteSuccess()
                    }

                    else -> {
                        setProgressDeleteFailed()
                    }
                }
                return Result.success()
            }catch (e: CancellationException){
                Log.e(Constants.TAG, "DeleteNoteWorker: cancelled! ${e.printStackTrace()}")
                setProgressDeleteFailed()
                delay(200)
                return Result.failure()
            }
        }

        // clear the progress so the same thing isn't emitted on cancelation
        private suspend fun clearProgress(key: String){
            setProgress(
                workDataOf(
                    key to ""
                )
            )
        }

        private suspend fun setProgressShowUndoSnackbar(){
            setProgress(
                workDataOf(
                    DELETE_NOTE_STATE_MESSAGE to DELETE_NOTE_SHOW_UNDO_SNACKBAR
                )
            )
        }

        private suspend fun setProgressDeleteSuccess(){
            printLogD("DeleteNoteWorker", "SUCCESS")
            setProgress(
                workDataOf(
                    DELETE_NOTE_STATE_MESSAGE to DELETE_NOTE_SUCCESS
                )
            )
        }

        private suspend fun setProgressDeleteFailed(){
            printLogD("DeleteNoteWorker", "FAILED")
            setProgress(
                workDataOf(
                    DELETE_NOTE_STATE_MESSAGE to DELETE_NOTE_FAILED
                )
            )
        }

        // I don't like this but the alternative is a custom factory.
        // and a custom factory doesn't seem like a good option because I will have
        // multiple modules. That gets ugly.
        private fun inject(){
            val appComponent = (applicationContext as BaseApplication).appComponent
            val noteComponent = (appComponent.notesFeature() as NotesFeatureImpl)
                .getProvider().noteComponent
            noteComponent?.inject(this)
        }

    }



//    fun deleteNote(
//        primaryKey: Int,
//        stateEvent: StateEvent
//    ): Flow<DataState<ViewState>> = flow {
//
//        var shouldContinue = true
//        emit(
//            DataState.data<ViewState>(
//                response = Response(
//                    message = DELETE_NOTE_PENDING,
//                    uiComponentType = UIComponentType.SnackBar(
//                        object: SnackbarUndoCallback{
//                            override fun undo() {
//                                shouldContinue = false
//                            }
//                        }
//                    ),
//                    messageType = MessageType.Info()
//                ),
//                data = null,
//                stateEvent = null
//            )
//        )
//
//        // wait to see if user presses "undo"
//        val delayJob = CoroutineScope(IO).async{
//            repeat(100){
//                delay(DELETE_UNDO_TIMEOUT / 100)
//                if(!shouldContinue){
//                    return@async
//                }
//            }
//            return@async
//        }
//        delayJob.await()
//
//        if(shouldContinue){
//            val cacheResult = safeCacheCall(IO){
//                noteRepository.deleteNote(primaryKey)
//            }
//
//            emit(
//                object: CacheResponseHandler<ViewState, Int>(
//                    response = cacheResult,
//                    stateEvent = stateEvent
//                ){
//                    override suspend fun handleSuccess(resultObj: Int): DataState<ViewState> {
//                        return if(resultObj > 0){
//                            DataState.data(
//                                response = Response(
//                                    message = DELETE_NOTE_SUCCESS,
//                                    uiComponentType = UIComponentType.None(),
//                                    messageType = MessageType.Success()
//                                ),
//                                data = null,
//                                stateEvent = stateEvent
//                            )
//                        }
//                        else{
//                            DataState.data(
//                                response = Response(
//                                    message = DELETE_NOTE_FAILED,
//                                    uiComponentType = UIComponentType.Toast(),
//                                    messageType = MessageType.Error()
//                                ),
//                                data = null,
//                                stateEvent = stateEvent
//                            )
//                        }
//                    }
//                }.getResult()
//            )
//        }else{
//            emit(
//                DataState.data<ViewState>(
//                    response = Response(
//                        message = DELETE_UNDO,
//                        uiComponentType = UIComponentType.None(),
//                        messageType = MessageType.Info()
//                    ),
//                    data = null,
//                    stateEvent = stateEvent
//                )
//            )
//        }
//
//    }

    companion object{
        const val DELETE_NOTE_STATE_MESSAGE = "state_message"
        const val DELETE_NOTE_SHOW_UNDO_SNACKBAR = "show_undo_snackbar"
        const val DELETE_NOTE_JOB_TAG = "delete_note"
        const val DELETE_NOTE_SUCCESS = "Successfully deleted note."
        const val DELETE_NOTE_PENDING = "Delete pending..."
        const val DELETE_NOTE_FAILED = "Failed to delete note."
        const val DELETE_NOTE_FAILED_NO_PRIMARY_KEY = "Could not delete that note. No primary key found."
        const val DELETE_ARE_YOU_SURE = "Are you sure you want to delete this?\nThis action can't be undone."
        const val DELETE_UNDO_TIMEOUT = 3000L
        const val DELETE_UNDO = "Undo delete"

        const val DELETE_NOTE_WORKER_ARG_PK = "primary_key"
    }
}













