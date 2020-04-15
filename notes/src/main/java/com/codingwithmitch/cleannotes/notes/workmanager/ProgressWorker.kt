package com.codingwithmitch.cleannotes.notes.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.codingwithmitch.cleannotes.core.util.printLogD
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