package com.codingwithmitch.cleannotes.notes.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.codingwithmitch.cleannotes.core.business.state.*
import com.codingwithmitch.cleannotes.core.util.TodoCallback
import com.codingwithmitch.cleannotes.core.util.printLogD
import com.codingwithmitch.cleannotes.notes.business.interactors.use_cases.DeleteNote
import com.codingwithmitch.cleannotes.presentation.MainActivity.WorkManagerConstants.SHOW_UNDO_SNACKBAR
import com.codingwithmitch.cleannotes.presentation.MainActivity.WorkManagerConstants.STATE_MESSAGE
import kotlinx.coroutines.delay

class ProgressWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    companion object {
        const val Progress = "Progress"
        private const val delayDuration = 1000L
    }

    override suspend fun doWork(): Result {
        val inputData = this.inputData
        printLogD("WorkManager: (Progress Worker)", "input data: ${inputData}")
//        val callback = object: TodoCallback{
//            override fun execute() {
//                this@ProgressWorker.stop()
//            }
//        }
//        setProgress(
//            workDataOf(
//                STATE_MESSAGE to StateMessage(
//                    response =  Response(
//                        message = DeleteNote.DELETE_NOTE_PENDING,
//                        uiComponentType = UIComponentType.SnackBar(
//                            object: SnackbarUndoCallback {
//                                override fun undo() {
//                                    callback.execute()
//                                }
//                            }
//                        ),
//                        messageType = MessageType.Info()
//                    )
//                )
//            )
//        )

        setProgress(
            workDataOf(
                STATE_MESSAGE to SHOW_UNDO_SNACKBAR
            )
        )
        delay(200L)
        val update1 = workDataOf(Progress to 0)
        setProgress(update1)
        delay(delayDuration)
        val update2 = workDataOf(Progress to 20)
        setProgress(update2)
        delay(delayDuration)
        val update3 = workDataOf(Progress to 40)
        setProgress(update3)
        delay(delayDuration)
        val update4 = workDataOf(Progress to 60)
        setProgress(update4)
        delay(delayDuration)
        val update5 = workDataOf(Progress to 80)
        setProgress(update5)
        delay(delayDuration)
        val update6 = workDataOf(Progress to 100)
        setProgress(update6)
        val outputData = workDataOf("output_data" to "DONE doing the THING.")
        return Result.success(outputData)
    }
}