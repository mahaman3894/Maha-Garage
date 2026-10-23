package maha.garage.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    const val PERIODIC_WORK_NAME = "maha_garage_daily_maintenance_check"

    fun scheduleDailyCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<MaintenanceNotificationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    fun triggerImmediateCheck(context: Context) {
        val immediateRequest = OneTimeWorkRequestBuilder<MaintenanceNotificationWorker>()
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "maha_garage_immediate_check", ExistingWorkPolicy.REPLACE, immediateRequest)
    }
}
