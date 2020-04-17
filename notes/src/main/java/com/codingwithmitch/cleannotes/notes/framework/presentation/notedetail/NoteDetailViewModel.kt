package com.codingwithmitch.cleannotes.notes.framework.presentation.notedetail

import androidx.lifecycle.LiveData
import com.codingwithmitch.cleannotes.core.business.state.*
import com.codingwithmitch.cleannotes.core.di.scopes.FeatureScope
import com.codingwithmitch.cleannotes.core.framework.BaseViewModel
import com.codingwithmitch.cleannotes.core.util.printLogD
import com.codingwithmitch.cleannotes.notes.business.domain.model.Note
import com.codingwithmitch.cleannotes.notes.business.interactors.notedetail.NoteDetailInteractors
import com.codingwithmitch.cleannotes.notes.business.interactors.notedetail.UpdateNote.Companion.UPDATE_NOTE_FAILED
import com.codingwithmitch.cleannotes.notes.framework.datasource.model.NoteEntity
import com.codingwithmitch.cleannotes.notes.framework.presentation.notedetail.state.*
import com.codingwithmitch.cleannotes.notes.framework.presentation.notedetail.state.CollapsingToolbarState.*
import com.codingwithmitch.cleannotes.notes.framework.presentation.notedetail.state.NoteDetailStateEvent.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


const val NOTE_DETAIL_ERROR_RETRIEVEING_SELECTED_NOTE = "Error retrieving selected note from bundle."
const val NOTE_DETAIL_SELECTED_NOTE_BUNDLE_KEY = "selectedNote"
const val NOTE_TITLE_CANNOT_BE_EMPTY = "Note title can not be empty."

@ExperimentalCoroutinesApi
@FlowPreview
@FeatureScope
class NoteDetailViewModel
@Inject
constructor(
    private val noteInteractors: NoteDetailInteractors
): BaseViewModel<NoteDetailViewState>(){

    private val noteInteractionManager: NoteInteractionManager =
        NoteInteractionManager()
    val noteTitleInteractionState: LiveData<NoteInteractionState>
        get() = noteInteractionManager.noteTitleState
    val noteBodyInteractionState: LiveData<NoteInteractionState>
        get() = noteInteractionManager.noteBodyState
    val collapsingToolbarState: LiveData<CollapsingToolbarState>
        get() = noteInteractionManager.collapsingToolbarState

    override fun handleNewData(data: NoteDetailViewState) {
        // no data coming in from requests...
    }

    override fun setStateEvent(stateEvent: StateEvent) {

        if(!isJobAlreadyActive(stateEvent)){
            val job: Flow<DataState<NoteDetailViewState>> = when(stateEvent){

                is UpdateNoteEvent -> {
                    val pk = getNote()?.id
                    if(!isNoteTitleNull() && pk != null){
                        noteInteractors.updateNote.updateNote(
                            primaryKey = pk,
                            newTitle = getNote()!!.title,
                            newBody = getNote()!!.body,
                            stateEvent = stateEvent
                        )
                    }else{
                        emitStateMessageEvent(
                            stateMessage = StateMessage(
                                response = Response(
                                    message = UPDATE_NOTE_FAILED,
                                    uiComponentType = UIComponentType.Dialog(),
                                    messageType = MessageType.Error()
                                )
                            ),
                            stateEvent = stateEvent
                        )
                    }
                }

                is CreateStateMessageEvent -> {
                    emitStateMessageEvent(
                        stateMessage = stateEvent.stateMessage,
                        stateEvent = stateEvent
                    )
                }

                else -> {
                    emitInvalidStateEvent(stateEvent)
                }
            }
            launchJob(stateEvent, job)
        }
    }

    private fun isNoteTitleNull(): Boolean {
        val title = getNote()?.title
        if (title.isNullOrBlank()) {
            setStateEvent(
                CreateStateMessageEvent(
                    stateMessage = StateMessage(
                        response = Response(
                            message = NOTE_TITLE_CANNOT_BE_EMPTY,
                            uiComponentType = UIComponentType.Dialog(),
                            messageType = MessageType.Info()
                        )
                    )
                )
            )
            return true
        } else {
            return false
        }
    }

    fun getNote(): Note? {
        return getCurrentViewStateOrNew().note
    }

    fun resetCollapsingToolbarState(){
        setCollapsingToolbarState(Expanded())
    }

    override fun initNewViewState(): NoteDetailViewState {
        return NoteDetailViewState()
    }

    fun getCollapsingToolbarState(): CollapsingToolbarState? {
        return collapsingToolbarState.value
    }

    fun setNote(note: Note?){
        val update = getCurrentViewStateOrNew()
        update.note = note
        setViewState(update)
    }

    fun setNoteFromBundle(note: Note?){
        if(getCurrentViewStateOrNew().note == null){
            setNote(note)
        }
    }

    fun setCollapsingToolbarState(
        state: CollapsingToolbarState
    ) = noteInteractionManager.setCollapsingToolbarState(state)

    fun updateNote(title: String?, body: String?){
        updateNoteTitle(title)
        updateNoteBody(body)
    }

    fun updateNoteTitle(title: String?){
        printLogD("DetailViewModel", "updating title: ${title}")
        if(title == null){
            setStateEvent(
                CreateStateMessageEvent(
                    stateMessage = StateMessage(
                        response = Response(
                            message = NoteEntity.nullTitleError(),
                            uiComponentType = UIComponentType.Dialog(),
                            messageType = MessageType.Error()
                        )
                    )
                )
            )
        }
        else{
            val update = getCurrentViewStateOrNew()
            val updatedNote = update.note?.copy(
                title = title
            )
            update.note = updatedNote
            setViewState(update)
        }
    }

    fun updateNoteBody(body: String?){
        val update = getCurrentViewStateOrNew()
        val updatedNote = update.note?.copy(
            body = body?: ""
        )
        update.note = updatedNote
        setViewState(update)
    }

    fun setNoteInteractionTitleState(state: NoteInteractionState){
        noteInteractionManager.setNewNoteTitleState(state)
    }

    fun setNoteInteractionBodyState(state: NoteInteractionState){
        noteInteractionManager.setNewNoteBodyState(state)
    }

    fun isToolbarCollapsed() = collapsingToolbarState.toString()
        .equals(Collapsed().toString())

    fun isToolbarExpanded() = collapsingToolbarState.toString()
        .equals(Expanded().toString())

    // return true if in EditState
    fun checkEditState() = noteInteractionManager.checkEditState()

    fun exitEditState() = noteInteractionManager.exitEditState()

    fun isEditingTitle() = noteInteractionManager.isEditingTitle()

    fun isEditingBody() = noteInteractionManager.isEditingBody()

    // force observers to refresh
    fun triggerNoteObservers(){
        getCurrentViewStateOrNew().note?.let { note ->
            setNote(note)
        }
    }
}












































