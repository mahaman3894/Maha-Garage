package maha.garage.util

import android.content.Context
import maha.garage.domain.engine.UpdateGatePolicy

class UpdateRequirementStore(context: Context, private val installed: Long) {
    private val prefs = context.applicationContext.getSharedPreferences("maha_update_requirement", Context.MODE_PRIVATE)
    var target: Long = UpdateGatePolicy.restoredTarget(installed, prefs.getLong("target", 0L))
        private set

    init {
        if (target == 0L) prefs.edit().remove("target").commit()
    }

    /** Commit before opening external UI so an immediate process death retains the gate. */
    fun requireVersion(version: Long) {
        target = maxOf(target, UpdateGatePolicy.restoredTarget(installed, version))
        check(prefs.edit().putLong("target", target).commit()) { "업데이트 상태를 저장하지 못했습니다." }
    }

    fun clear() {
        check(prefs.edit().remove("target").commit()) { "업데이트 상태를 저장하지 못했습니다." }
        target = 0L
    }
}
