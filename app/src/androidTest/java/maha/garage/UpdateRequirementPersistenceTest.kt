package maha.garage

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import maha.garage.util.UpdateRequirementStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UpdateRequirementPersistenceTest {
    @Test fun recreationRetainsRequirementAndUpdatedInstallClearsIt() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val suffix = UUID.randomUUID().toString()
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                base.getSharedPreferences("regression_${suffix}_$name", mode)
        }
        try {
            UpdateRequirementStore(context, 4).requireVersion(5)
            assertEquals(5L, UpdateRequirementStore(context, 4).target)
            assertEquals(0L, UpdateRequirementStore(context, 5).target)
            assertEquals(0L, UpdateRequirementStore(context, 4).target)
        } finally {
            base.deleteSharedPreferences("regression_${suffix}_maha_update_requirement")
        }
    }

    @Test fun successfulNoUpdateClearPersistsAcrossRecreation() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val suffix = UUID.randomUUID().toString()
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                base.getSharedPreferences("regression_${suffix}_$name", mode)
        }
        try {
            val store = UpdateRequirementStore(context, 4)
            store.requireVersion(5)
            store.clear()
            assertEquals(0L, UpdateRequirementStore(context, 4).target)
        } finally {
            base.deleteSharedPreferences("regression_${suffix}_maha_update_requirement")
        }
    }
}
