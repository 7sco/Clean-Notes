package com.codingwithmitch.cleannotes.notes.business.interactors.use_cases

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.codingwithmitch.cleannotes.core.business.state.*
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.NoteListFragment.WorkManagerConstants.DELETE_NOTE_JOB_TAG
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.state.NoteListViewState
import com.codingwithmitch.cleannotes.notes.workmanager.DeleteNoteWorker
import com.codingwithmitch.cleannotes.notes.workmanager.DeleteNoteWorker.Companion.DELETE_NOTE_WORKER_ARG_PK
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@FlowPreview
@ExperimentalCoroutinesApi
class DeleteNote(
    private val workManager: WorkManager
){
//class DeleteNote<ViewState>(
//    private val noteRepository: NoteRepository
//){

    fun deleteNote(
        primaryKey: Int,
        stateEvent: StateEvent
    ): Flow<DataState<NoteListViewState>> = flow {

        val stringData = workDataOf(DELETE_NOTE_WORKER_ARG_PK to primaryKey)
        val workRequest = OneTimeWorkRequestBuilder<DeleteNoteWorker>()
            .setInputData(stringData)
            .addTag(DELETE_NOTE_JOB_TAG)
            .build()

        workManager
            .enqueueUniqueWork(
                DELETE_NOTE_JOB_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

        emit(
            DataState.data<NoteListViewState>(
                response = Response(
                    message = DELETE_NOTE_PENDING,
                    uiComponentType = UIComponentType.None(),
                    messageType = MessageType.Info()
                ),
                data = null,
                stateEvent = stateEvent
            )
        )
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
        val DELETE_NOTE_SUCCESS = "Successfully deleted note."
        val DELETE_NOTE_PENDING = "Delete pending..."
        val DELETE_NOTE_FAILED = "Failed to delete note."
        val DELETE_NOTE_FAILED_NO_PRIMARY_KEY = "Could not delete that note. No primary key found."
        val DELETE_ARE_YOU_SURE = "Are you sure you want to delete this?\nThis action can't be undone."
        val DELETE_UNDO_TIMEOUT = 3000L
        val DELETE_UNDO = "Undo delete"
    }
}













