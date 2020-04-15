package com.codingwithmitch.cleannotes.notes.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codingwithmitch.cleannotes.core.business.safeCacheCall
import com.codingwithmitch.cleannotes.notes.business.interactors.use_cases.DeleteNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

class DeleteNoteWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {


    override suspend fun doWork(): Result {

        delay(DeleteNote.DELETE_UNDO_TIMEOUT)

//        val cacheResult = safeCacheCall(Dispatchers.IO){
//            noteRepository.deleteNote(primaryKey)
//        }



        return Result.success()
    }


}
























