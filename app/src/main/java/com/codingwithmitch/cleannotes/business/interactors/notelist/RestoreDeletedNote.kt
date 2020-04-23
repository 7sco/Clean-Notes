package com.codingwithmitch.cleannotes.business.interactors.notelist

import com.codingwithmitch.cleannotes.business.cache.CacheResponseHandler
import com.codingwithmitch.cleannotes.business.data.abstraction.NoteRepository
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.state.*
import com.codingwithmitch.cleannotes.business.util.safeCacheCall
import com.codingwithmitch.cleannotes.framework.presentation.notelist.state.NoteListViewState
import com.codingwithmitch.cleannotes.framework.presentation.notelist.state.NoteListViewState.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RestoreDeletedNote(
    private val noteRepository: NoteRepository
){

    fun restoreDeletedNote(
        note: Note,
        stateEvent: StateEvent
    ): Flow<DataState<NoteListViewState>> = flow {

        val cacheResult = safeCacheCall(IO){
            noteRepository.insertNote(note)
        }

        emit(
            object: CacheResponseHandler<NoteListViewState, Long>(
                response = cacheResult,
                stateEvent = stateEvent
            ){
                override suspend fun handleSuccess(resultObj: Long): DataState<NoteListViewState> {
                    return if(resultObj > 0){
                        val viewState =
                            NoteListViewState(
                                notePendingDelete = NotePendingDelete(
                                    note = note
                                )
                            )
                        DataState.data(
                            response = Response(
                                message = RESTORE_NOTE_SUCCESS,
                                uiComponentType = UIComponentType.Toast(),
                                messageType = MessageType.Success()
                            ),
                            data = viewState,
                            stateEvent = stateEvent
                        )
                    }
                    else{
                        DataState.data(
                            response = Response(
                                message = RESTORE_NOTE_FAILED,
                                uiComponentType = UIComponentType.Toast(),
                                messageType = MessageType.Error()
                            ),
                            data = null,
                            stateEvent = stateEvent
                        )
                    }
                }
            }.getResult()
        )

    }

    companion object{

        val RESTORE_NOTE_SUCCESS = "Successfully restored the deleted note."
        val RESTORE_NOTE_FAILED = "Failed to restore the deleted note."

    }
}












