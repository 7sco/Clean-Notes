package com.codingwithmitch.cleannotes.notes.workmanager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.codingwithmitch.cleannotes.core.business.cache.CacheResponseHandler
import com.codingwithmitch.cleannotes.core.business.safeCacheCall
import com.codingwithmitch.cleannotes.core.business.state.DataState
import com.codingwithmitch.cleannotes.core.business.state.MessageType
import com.codingwithmitch.cleannotes.core.business.state.Response
import com.codingwithmitch.cleannotes.core.business.state.UIComponentType
import com.codingwithmitch.cleannotes.core.util.Constants.TAG
import com.codingwithmitch.cleannotes.core.util.printLogD
import com.codingwithmitch.cleannotes.notes.business.domain.repository.NoteRepository
import com.codingwithmitch.cleannotes.notes.business.interactors.use_cases.DeleteNote
import com.codingwithmitch.cleannotes.notes.business.interactors.use_cases.DeleteNote.Companion.DELETE_NOTE_FAILED
import com.codingwithmitch.cleannotes.notes.business.interactors.use_cases.DeleteNote.Companion.DELETE_NOTE_SUCCESS
import com.codingwithmitch.cleannotes.notes.di.NotesFeatureImpl
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.NoteListFragment.WorkManagerConstants.SHOW_UNDO_SNACKBAR
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.NoteListFragment.WorkManagerConstants.STATE_MESSAGE
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.state.NoteListViewState
import com.codingwithmitch.cleannotes.presentation.BaseApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import java.util.concurrent.CancellationException
import javax.inject.Inject

@ExperimentalCoroutinesApi
@FlowPreview
class DeleteNoteWorker
constructor(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    // I don't like this but the alternative is ugly
    @Inject
    lateinit var noteRepository: NoteRepository

    private var primaryKey: Int = -1

    override suspend fun doWork(): Result {

        try{

            val appComponent = (applicationContext as BaseApplication).appComponent
            val noteComponent = (appComponent.notesFeature() as NotesFeatureImpl)
                .getProvider().noteComponent
            noteComponent?.inject(this)

            if(!::noteRepository.isInitialized){
                throw CancellationException("DeleteNoteWorker: Must set the NoteRepository.")
            }
            primaryKey = inputData.getInt(DELETE_NOTE_WORKER_ARG_PK, -1)
            if(primaryKey < 0){
                throw CancellationException("DeleteNoteWorker: Missing primary key.")
            }

            // show "undo" snackbar for canceling the delete
            setProgress(
                workDataOf(
                    STATE_MESSAGE to SHOW_UNDO_SNACKBAR
                )
            )
            delay(DeleteNote.DELETE_UNDO_TIMEOUT)

            val cacheResult = safeCacheCall(Dispatchers.IO){
                noteRepository.deleteNote(primaryKey)
            }

            val result = object: CacheResponseHandler<NoteListViewState, Int>(
                response = cacheResult,
                stateEvent = null
            ){
                override suspend fun handleSuccess(resultObj: Int): DataState<NoteListViewState> {
                    return if(resultObj > 0){
                        DataState.data(
                            response = Response(
                                message = DELETE_NOTE_SUCCESS,
                                uiComponentType = UIComponentType.None(),
                                messageType = MessageType.Success()
                            ),
                            data = null,
                            stateEvent = null
                        )
                    }
                    else{
                        DataState.data(
                            response = Response(
                                message = DELETE_NOTE_FAILED,
                                uiComponentType = UIComponentType.Toast(),
                                messageType = MessageType.Error()
                            ),
                            data = null,
                            stateEvent = null
                        )
                    }
                }
            }.getResult()

            when(result.stateMessage?.response?.message){

                DELETE_NOTE_FAILED -> {
                    setProgress(
                        workDataOf(
                            STATE_MESSAGE to DELETE_NOTE_FAILED
                        )
                    )
                }

                DELETE_NOTE_SUCCESS -> {
                    printLogD("DeleteNoteWorker", "SUCCESS")
                    setProgress(
                        workDataOf(
                            STATE_MESSAGE to DELETE_NOTE_SUCCESS
                        )
                    )
                }

                else -> {
                    setProgress(
                        workDataOf(
                            STATE_MESSAGE to DELETE_NOTE_FAILED
                        )
                    )
                }
            }
            return Result.success()
        }catch (e: CancellationException){
            Log.e(TAG, "DeleteNoteWorker: cancelled! ${e.printStackTrace()}")
            setProgress(
                workDataOf(
                    STATE_MESSAGE to DELETE_NOTE_FAILED
                )
            )
            return Result.failure()
        }
    }


    companion object{

        const val DELETE_NOTE_WORKER_ARG_PK = "primary_key"
    }


}
























