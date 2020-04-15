package com.codingwithmitch.cleannotes.notes.workmanager

import android.content.Context
import androidx.work.*
import com.codingwithmitch.cleannotes.core.di.scopes.FeatureScope
import com.codingwithmitch.cleannotes.notes.business.domain.repository.NoteRepository
import javax.inject.Inject

@FeatureScope
class NoteWorkerFactory
@Inject
constructor(
    private val noteRepository: NoteRepository
): WorkerFactory(){

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        val workerKlass = Class.forName(workerClassName).asSubclass(CoroutineWorker::class.java)
        val constructor = workerKlass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
        val instance = constructor.newInstance(appContext, workerParameters)

        when(instance){

            is DeleteNoteWorker -> {
                instance.noteRepository = noteRepository
            }

        }

        return instance
    }

}


























