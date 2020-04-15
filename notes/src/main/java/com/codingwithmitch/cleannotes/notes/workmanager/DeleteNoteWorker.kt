package com.codingwithmitch.cleannotes.notes.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.codingwithmitch.cleannotes.core.business.safeCacheCall
import com.codingwithmitch.cleannotes.core.util.printLogD
import com.codingwithmitch.cleannotes.di.AppComponent
import com.codingwithmitch.cleannotes.notes.business.domain.repository.NoteRepository
import com.codingwithmitch.cleannotes.notes.business.interactors.use_cases.DeleteNote
import com.codingwithmitch.cleannotes.notes.di.NotesFeatureImpl
import com.codingwithmitch.cleannotes.presentation.BaseApplication
import com.codingwithmitch.cleannotes.presentation.MainActivity
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

        val appComponent = (applicationContext as BaseApplication).appComponent
        val noteComponent = (appComponent.notesFeature() as NotesFeatureImpl)
            .getProvider().noteComponent
        noteComponent?.inject(this)

        if(!::noteRepository.isInitialized){
            throw CancellationException("DeleteNoteWorker: Must set the NoteRepository.")
        }
        primaryKey = inputData.getInt("primary_key", -1)
        if(primaryKey < 0){
            throw CancellationException("DeleteNoteWorker: Missing primary key.")
        }

        // show "undo" snackbar for canceling the delete
        setProgress(
            workDataOf(
                MainActivity.STATE_MESSAGE to MainActivity.SHOW_UNDO_SNACKBAR
            )
        )
        delay(DeleteNote.DELETE_UNDO_TIMEOUT)

        val cacheResult = safeCacheCall(Dispatchers.IO){
            noteRepository.deleteNote(primaryKey)
        }


        return Result.success()
    }


}
























