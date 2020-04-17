package com.codingwithmitch.cleannotes.notes.business.interactors.notelist

import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.DeleteNote
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.GetNumNotes
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.InsertNewNote
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.SearchNotes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

// Use cases
@ExperimentalCoroutinesApi
@FlowPreview
class NoteListInteractors (
    val insertNewNote: InsertNewNote,
    val deleteNote: DeleteNote,
    val searchNotes: SearchNotes,
    val getNumNotes: GetNumNotes
)














