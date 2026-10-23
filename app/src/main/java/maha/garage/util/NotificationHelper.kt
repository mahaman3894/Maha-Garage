package maha.garage.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import maha.garage.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_MAINTENANCE = "maha_garage_maintenance_channel"
    const val CHANNEL_TIRE_SWAP = "maha_garage_tire_swap_channel"
    const val CHANNEL_COMPLIANCE = "maha_garage_compliance_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val complianceChannel = NotificationChannel(
                CHANNEL_COMPLIANCE,
                "자동차 검사 및 보험 만기 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "자동차 정기/종합/재검사 기한 및 자동차 보험 만기일 안내"
                enableVibration(true)
            }

            val maintenanceChannel = NotificationChannel(
                CHANNEL_MAINTENANCE,
                "기타 안내",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(complianceChannel)
            notificationManager?.createNotificationChannel(maintenanceChannel)
        }
    }

    private fun getLaunchPendingIntent(context: Context, carId: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("mahagarage://vehicle/${Uri.encode(carId.orEmpty())}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (carId != null) {
                putExtra("target_vehicle_id", carId)
            }
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, carId.hashCode(), intent, flags)
    }

    fun showOverdueNotification(
        context: Context,
        carId: String,
        carName: String,
        overdueItems: List<String>
    ): Boolean {
        val title = "🚨 [정비 주기 초과] $carName"
        val content = "${overdueItems.joinToString(", ")} 교체/점검 주기가 초과되었습니다. 즉시 정비가 권장됩니다."

        val notification = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(context, carId))
            .build()

        return try {
            val manager = NotificationManagerCompat.from(context)
            val channel = if (100 == 300) CHANNEL_TIRE_SWAP else CHANNEL_MAINTENANCE
            if (!manager.areNotificationsEnabled() || context.getSystemService(NotificationManager::class.java)
                    .getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE) return false
            manager.notify("vehicle:$carId", 100, notification)
            true
        } catch (_: SecurityException) { false }
    }

    fun showDueSoonNotification(
        context: Context,
        carId: String,
        carName: String,
        dueSoonItems: List<String>
    ): Boolean {
        val title = "⚠️ [정비 도래 임박] $carName"
        val content = "${dueSoonItems.joinToString(", ")} 교체 시기가 임박했습니다 (1,000km / 30일 이내)."

        val notification = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(context, carId))
            .build()

        return try {
            val manager = NotificationManagerCompat.from(context)
            val channel = if (200 == 300) CHANNEL_TIRE_SWAP else CHANNEL_MAINTENANCE
            if (!manager.areNotificationsEnabled() || context.getSystemService(NotificationManager::class.java)
                    .getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE) return false
            manager.notify("vehicle:$carId", 200, notification)
            true
        } catch (_: SecurityException) { false }
    }

    fun showTireSwapNotification(
        context: Context,
        carId: String,
        carName: String,
        currentMode: String,
        targetMode: String,
        message: String
    ): Boolean {
        val title = "🛞 [환절기 타이어 스왑 안내] $carName"
        val content = "현재 $currentMode 장착 중입니다. $message ($targetMode 교체 권장)"

        val notification = NotificationCompat.Builder(context, CHANNEL_TIRE_SWAP)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(context, carId))
            .build()

        return try {
            val manager = NotificationManagerCompat.from(context)
            val channel = CHANNEL_TIRE_SWAP
            if (!manager.areNotificationsEnabled() || context.getSystemService(NotificationManager::class.java)
                    .getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE) return false
            manager.notify("tire:$carId", 300, notification)
            true
        } catch (_: SecurityException) { false }
    }

    fun showInspectionNotification(
        context: Context,
        carId: String,
        carName: String,
        title: String,
        content: String,
        isOverdue: Boolean = false
    ): Boolean {
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLIANCE)
            .setSmallIcon(if (isOverdue) android.R.drawable.stat_notify_error else android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(if (isOverdue) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(context, carId))
            .build()

        return postCompliance(context, carId, notification, inspection = true)
    }

    fun showInsuranceNotification(
        context: Context,
        carId: String,
        carName: String,
        title: String,
        content: String,
        isOverdue: Boolean = false
    ): Boolean {
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLIANCE)
            .setSmallIcon(if (isOverdue) android.R.drawable.stat_notify_error else android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(if (isOverdue) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(context, carId))
            .build()

        return postCompliance(context, carId, notification, inspection = false)
    }

    private fun postCompliance(
        context: Context,
        carId: String,
        notification: android.app.Notification,
        inspection: Boolean
    ): Boolean = synchronized(NotificationSettingsStore.notificationLock) {
        try {
            val settings = NotificationSettingsStore(context)
            if (inspection && !settings.isInspectionEnabled) return@synchronized false
            if (!inspection && !settings.isInsuranceEnabled) return@synchronized false
            val manager = NotificationManagerCompat.from(context)
            val platform = context.getSystemService(NotificationManager::class.java) ?: return@synchronized false
            val channel = platform.getNotificationChannel(CHANNEL_COMPLIANCE) ?: return@synchronized false
            if (!manager.areNotificationsEnabled() || channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return@synchronized false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && channel.group != null &&
                platform.getNotificationChannelGroup(channel.group)?.isBlocked == true) return@synchronized false
            val tag = if (inspection) "inspection:$carId" else "insurance:$carId"
            manager.notify(tag, if (inspection) 400 else 500, notification)
            true
        } catch (_: SecurityException) { false }
    }
    fun cancelInspection(context: Context, carId: String) {
        NotificationManagerCompat.from(context).cancel("inspection:$carId", 400)
    }

    fun cancelInsurance(context: Context, carId: String) {
        NotificationManagerCompat.from(context).cancel("insurance:$carId", 500)
    }

    fun cancelMaintenance(context: Context, carId: String, id: Int) {
        NotificationManagerCompat.from(context).cancel("vehicle:$carId", id)
    }

    fun cancelTireSwap(context: Context, carId: String) {
        NotificationManagerCompat.from(context).cancel("tire:$carId", 300)
    }

    
    fun cancelAllForCar(context: Context, carId: String) {
        cancelInspection(context, carId)
        cancelInsurance(context, carId)
        cancelMaintenance(context, carId, 100)
        cancelMaintenance(context, carId, 200)
        cancelTireSwap(context, carId)
    }

    fun showTestNotification(context: Context) {
        val title = "🚗 마하개러지 알림 테스트"
        val content = "알림 시스템이 정상 작동 중입니다. 자동차 정기검사 기한 및 보험 갱신 시 알림이 발송됩니다."

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLIANCE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9999, notification)
        } catch (_: SecurityException) {
            // 알림 권한 미부여 시 무시
        }
    }
}
