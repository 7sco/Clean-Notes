package com.codingwithmitch.cleannotes.framework.datasource.cache

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.coroutines.*
import org.junit.FixMethodOrder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters


// These tests are trash. Do not use.
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4ClassRunner::class)
class BasicDbTests {

//    private val dateUtil: DateUtil = DateUtil()
//    private lateinit var db: NoteDatabase
//    private lateinit var noteRepository: NoteRepository
//    private lateinit var interactors: NoteListInteractors
//
//
//    @Before
//    fun before() {
//        val context = ApplicationProvider.getApplicationContext<Context>()
//        db = Room.inMemoryDatabaseBuilder(
//            context,
//            NoteDatabase::class.java
//        ).build()
//        noteRepository = NoteRepositoryImpl(
//            NoteCacheDataSourceImpl(
//                noteDao = db.noteDao(),
//                noteMapper = NoteMapper(dateUtil),
//                dateUtil = dateUtil
//            )
//        )
//        interactors =
//            NoteListInteractors(
//                InsertNewNote(noteRepository, NoteFactory(dateUtil)),
//                DeleteNote(noteRepository),
//                SearchNotes(noteRepository),
//                GetNumNotes(noteRepository),
//                RestoreDeletedNote(noteRepository),
//                DeleteMultipleNotes(noteRepository),
//                InsertMultipleNotes(noteRepository)
//            )
//    }
//
//    @After
//    @Throws(IOException::class)
//    fun after() {
//        db.close()
//    }
//
//    @Test
//    fun a_insertNewNote_confirmInserted(){
//        runBlocking {
//            val newTitle = "first note"
//            val newBody = "something I should not forget!"
//            val stateEvent = SearchNotesEvent()
//
//            // insertNewNote "Job"
//            launch {
//                interactors
//                    .insertNewNote
//                    .insertNewNote(
//                        title = newTitle,
//                        body = newBody,
//                        stateEvent = stateEvent
//                    ).collect(object : FlowCollector<DataState<NoteListViewState>> {
//                        override suspend fun emit(value: DataState<NoteListViewState>) {
//                            println("basicDbTests: inserted new note ${value.data?.newNote}")
//                            assertEquals(newTitle, value.data?.newNote?.title)
//                            assertEquals(newBody, value.data?.newNote?.body)
//                        }
//                    })
//            }.join()
//
//            var foundNote: Note? = null
//            // Search for the new note in db "Job"
//            launch {
//                interactors.searchNotes.searchNotes(
//                    query = "",
//                    filterAndOrder = ORDER_BY_ASC_DATE_UPDATED,
//                    page = 0,
//                    stateEvent = stateEvent
//                ).collect(object : FlowCollector<DataState<NoteListViewState>> {
//                    override suspend fun emit(value: DataState<NoteListViewState>) {
//                        // loop through results and make sure one of them is the new note
//                        val notesList = value.data?.noteList
//                        println("basicDbTests: ${notesList}")
//                        if (notesList != null) {
//                            for (note in notesList) {
//                                if (note.title.equals(newTitle)
//                                    && note.body.equals(newBody)
//                                ) {
//                                    foundNote = note
//                                    assertEquals(newTitle, note.title)
//                                    assertEquals(newBody, note.body)
//                                    break
//                                }
//                            }
//                            if (foundNote == null) {
//                                throw Exception("NewNote was not found in search.")
//                            }
//                        }
//                    }
//                })
//            }
//        }
//    }


//    @Test
//    fun b_insertNewNote_delete_confirmDeleted(){
//        runBlocking {
//
//            val newTitle = "first note"
//            val newBody = "something I should not forget!"
//            val stateEvent = SearchNotesEvent()
//
//            // insert new new "job"
//            launch {
//                interactors
//                    .insertNewNote
//                    .insertNewNote(
//                        title = newTitle,
//                        body = newBody,
//                        stateEvent = stateEvent
//                    ).collect(object : FlowCollector<DataState<NoteListViewState>> {
//                        override suspend fun emit(value: DataState<NoteListViewState>) {
//                            value.data?.newNote?.let { note ->
//                                println("basicDbTests: inserted new note ${value.data?.newNote}")
//                                assertEquals(newTitle, value.data?.newNote?.title)
//                                assertEquals(newBody, value.data?.newNote?.body)
//                            }
//                        }
//                    })
//            }.join()
//
//
//            var foundNote: Note? = null
//            // Search for the new note in db "Job"
//            launch {
//                interactors.searchNotes.searchNotes(
//                    query = "",
//                    filterAndOrder = ORDER_BY_ASC_DATE_UPDATED,
//                    page = 0,
//                    stateEvent = stateEvent
//                ).collect(object : FlowCollector<DataState<NoteListViewState>> {
//                    override suspend fun emit(value: DataState<NoteListViewState>) {
//                        // loop through results and make sure one of them is the new note
//                        val notesList = value.data?.noteList
//                        println("basicDbTests: ${notesList}")
//                        if (notesList != null) {
//                            for (note in notesList) {
//                                if (note.title.equals(newTitle)
//                                    && note.body.equals(newBody)
//                                ) {
//                                    foundNote = note
//                                    assertEquals(newTitle, note.title)
//                                    assertEquals(newBody, note.body)
//                                    break
//                                }
//                            }
//                            if (foundNote == null) {
//                                throw Exception("NewNote was not found in search.")
//                            }
//                        }
//                    }
//                })
//            }.join()
//
//            // deleteNote "job"
//            println("basicDbTests: attempting delete: ${foundNote}")
//            launch {
//                interactors.deleteNote.deleteNote(
//                    primaryKey = foundNote?.id ?: -1,
//                    stateEvent = DeleteNoteEvent(foundNote?.id ?: -1)
//                ).collect(object : FlowCollector<DataState<NoteListViewState>> {
//                    override suspend fun emit(value: DataState<NoteListViewState>) {
//                        assertEquals(
//                            DeleteNote.DELETE_NOTE_SUCCESS,
//                            value.stateMessage?.response?.message
//                        )
//                    }
//                })
//            }.join()
//
//            // confirm the note is no longer in the db
//            foundNote = null
//            launch {
//                interactors.searchNotes.searchNotes(
//                    query = "",
//                    filterAndOrder = ORDER_BY_ASC_DATE_UPDATED,
//                    page = 0,
//                    stateEvent = stateEvent
//                ).collect(object : FlowCollector<DataState<NoteListViewState>> {
//                    override suspend fun emit(value: DataState<NoteListViewState>) {
//                        // loop through results and make sure one of them is the new note
//                        val notesList = value.data?.noteList
//                        println("basicDbTests: ${notesList}")
//                        if (notesList != null) {
//                            for (note in notesList) {
//                                if (note.title.equals(newTitle)
//                                    && note.body.equals(newBody)
//                                ) {
//                                    foundNote = note
//                                    break
//                                }
//                            }
//                            if (foundNote != null) {
//                                throw Exception("NewNote was not deleted!")
//                            }
//                        }
//                    }
//                })
//            }
//        }
//    }

}






























