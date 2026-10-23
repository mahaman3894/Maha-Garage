package maha.garage

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import maha.garage.data.local.DatabaseSeeder
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.data.repository.MaintenanceRepositoryImpl
import maha.garage.data.repository.VehicleRepositoryImpl
import maha.garage.domain.repository.MaintenanceRepository
import maha.garage.domain.repository.VehicleRepository
import maha.garage.util.NotificationHelper
import maha.garage.worker.WorkManagerScheduler

class MahaGarageApp : Application() {

    val database: MahaGarageDatabase by lazy {
        MahaGarageDatabase.getInstance(this)
    }

    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepositoryImpl(database)
    }

    val maintenanceRepository: MaintenanceRepository by lazy {
        MaintenanceRepositoryImpl(database)
    }

    val fuelRepository: maha.garage.domain.repository.FuelRepository by lazy {
        maha.garage.data.repository.FuelRepositoryImpl(database.fuelRecordDao(), database.carDao())
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)

        // 백그라운드에서 초기 데이터 검증 및 시딩
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseSeeder(database).seedIfNeeded()
        }

        // 매일 24시간 간격 정비 만기 및 타이어 환절기 스왑 백그라운드 점검 예약
        WorkManagerScheduler.scheduleDailyCheck(this)
    }
}
