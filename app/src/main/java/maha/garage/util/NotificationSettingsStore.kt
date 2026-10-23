package maha.garage.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 알림 설정 SharedPreferences 영구 저장소.
 * 사용자가 요청한 대로 [자동차 정기검사/재검사] 및 [자동차 보험 만기/갱신] 2개 핵심 알림만 관리합니다.
 */
class NotificationSettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var isInspectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_INSPECTION, true)
        set(value) = synchronized(notificationLock) {
            prefs.edit().putBoolean(KEY_NOTIFY_INSPECTION, value).apply()
            if (!value) cancelDisplayed("inspection:", 400)
        }

    var isInsuranceEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_INSURANCE, true)
        set(value) = synchronized(notificationLock) {
            prefs.edit().putBoolean(KEY_NOTIFY_INSURANCE, value).apply()
            if (!value) cancelDisplayed("insurance:", 500)
        }

    private fun cancelDisplayed(prefix: String, id: Int) {
        val manager = appContext.getSystemService(android.app.NotificationManager::class.java) ?: return
        manager.activeNotifications.filter { it.id == id && it.tag?.startsWith(prefix) == true }
            .forEach { manager.cancel(it.tag, it.id) }
    }

    companion object {
        // Serialize preference changes/cancellation with the final check and notify().
        internal val notificationLock = Any()
        private const val PREFS_NAME = "maha_garage_notification_settings"
        const val KEY_NOTIFY_INSPECTION = "notify_inspection"
        const val KEY_NOTIFY_INSURANCE = "notify_insurance"
    }
}
