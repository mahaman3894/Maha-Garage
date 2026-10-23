package maha.garage.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import maha.garage.data.local.converter.Converters
import maha.garage.data.local.dao.*
import maha.garage.data.local.entity.*
import maha.garage.data.seed.DefaultCatalogData

@Database(
    entities = [
        CarEntity::class,
        MaintenanceCatalogEntity::class,
        MaintenanceRuleEntity::class,
        ServiceVisitEntity::class,
        ServiceLineEntity::class,
        TireSwapLogEntity::class,
        MeterReadingEntity::class,
        TireEntity::class,
        TireInstallationHistoryEntity::class,
        CarInspectionHistoryEntity::class,
        FuelRecordEntity::class
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MahaGarageDatabase : RoomDatabase() {

    abstract fun carDao(): CarDao
    abstract fun catalogDao(): MaintenanceCatalogDao
    abstract fun maintenanceRuleDao(): MaintenanceRuleDao
    abstract fun serviceVisitDao(): ServiceVisitDao
    abstract fun tireSwapLogDao(): TireSwapLogDao
    abstract fun meterReadingDao(): MeterReadingDao
    abstract fun tireDao(): TireDao
    abstract fun carInspectionDao(): CarInspectionDao
    abstract fun fuelRecordDao(): FuelRecordDao

    companion object {
        @Volatile
        private var INSTANCE: MahaGarageDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("maintenance_catalog", "maintenance_rules")) {
                    val columns = mutableSetOf<String>()
                    db.query("PRAGMA table_info(`$table`)").use { cursor ->
                        while (cursor.moveToNext()) columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    }
                    for (column in listOf("firstIntervalKm", "firstIntervalMonths")) {
                        if (column !in columns) db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` INTEGER DEFAULT NULL")
                    }
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cars ADD COLUMN firstRegistrationDate TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cars ADD COLUMN timingType TEXT NOT NULL DEFAULT 'CHAIN'")
                db.execSQL("ALTER TABLE cars ADD COLUMN timingDesc TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN trimName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN displacement TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN power TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN torque TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN transmission TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN driveSystem TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN fuelEconomy TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN dimensions TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tires` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `carId` TEXT NOT NULL,
                        `position` TEXT NOT NULL,
                        `season` TEXT NOT NULL,
                        `brand` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `spec` TEXT NOT NULL,
                        `dot` TEXT NOT NULL,
                        `cumulativeMileage` INTEGER NOT NULL,
                        `installedAtMileage` INTEGER NOT NULL,
                        `installDate` TEXT NOT NULL,
                        `treadDepthMm` REAL,
                        `storageLocation` TEXT,
                        `isMounted` INTEGER NOT NULL,
                        `purchaseCost` INTEGER NOT NULL,
                        `notes` TEXT,
                        `setId` TEXT,
                        `assignedWheelPosition` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tires_carId` ON `tires` (`carId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tire_installation_history` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `carId` TEXT NOT NULL,
                        `tireId` TEXT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `fromPosition` TEXT,
                        `toPosition` TEXT,
                        `eventDate` TEXT NOT NULL,
                        `odometerKm` INTEGER NOT NULL,
                        `drivenDistanceSinceLastEvent` INTEGER NOT NULL,
                        `treadDepthMm` REAL,
                        `cost` INTEGER NOT NULL,
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tire_installation_history_carId` ON `tire_installation_history` (`carId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tire_installation_history_tireId` ON `tire_installation_history` (`tireId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // cars 테이블에 정기검사 및 보험 관련 필드 추가 (기본값 NULL 및 isInspectionDateEstimated=1 처리)
                db.execSQL("ALTER TABLE cars ADD COLUMN isCommercial INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cars ADD COLUMN inspectionType TEXT NOT NULL DEFAULT '정기검사'")
                db.execSQL("ALTER TABLE cars ADD COLUMN inspectionStartDate TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN inspectionEndDate TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN isInspectionDateEstimated INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE cars ADD COLUMN lastInspectionDate TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN lastInspectionResult TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN insuranceCompany TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN insuranceStartDate TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN insuranceEndDate TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN insurancePolicyNumber TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cars ADD COLUMN insuranceEmergencyCall TEXT DEFAULT NULL")

                // car_inspection_history 테이블 생성 (Room 스키마와 100% 일치)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `car_inspection_history` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `carId` TEXT NOT NULL,
                        `inspectionDate` TEXT NOT NULL,
                        `inspectionType` TEXT NOT NULL,
                        `stationName` TEXT,
                        `isPassed` INTEGER NOT NULL,
                        `nextExpiryDate` TEXT,
                        `retestDeadline` TEXT,
                        `cost` INTEGER NOT NULL,
                        `memo` TEXT,
                        `certificatePhotoUri` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_car_inspection_history_carId` ON `car_inspection_history` (`carId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. cars 테이블에 retestDeadline 컬럼 추가
                db.execSQL("ALTER TABLE cars ADD COLUMN retestDeadline TEXT DEFAULT NULL")

                // 2. car_inspection_history 테이블 스키마 보정 및 인덱스/외래키 보장 (기존 v6 데이터 완전 보존)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `car_inspection_history_new` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `carId` TEXT NOT NULL,
                        `inspectionDate` TEXT NOT NULL,
                        `inspectionType` TEXT NOT NULL,
                        `stationName` TEXT,
                        `isPassed` INTEGER NOT NULL,
                        `nextExpiryDate` TEXT,
                        `retestDeadline` TEXT,
                        `cost` INTEGER NOT NULL,
                        `memo` TEXT,
                        `certificatePhotoUri` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='car_inspection_history'")
                val tableExists = cursor.moveToFirst()
                cursor.close()

                if (tableExists) {
                    db.execSQL("INSERT OR IGNORE INTO `car_inspection_history_new` SELECT * FROM `car_inspection_history`")
                    db.execSQL("DROP TABLE `car_inspection_history`")
                }
                db.execSQL("ALTER TABLE `car_inspection_history_new` RENAME TO `car_inspection_history`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_car_inspection_history_carId` ON `car_inspection_history` (`carId`)")

                // 3. 기존 검사 이력에서 최신 부적합 판정의 재검사 기한을 cars.retestDeadline으로 이관
                db.execSQL("""
                    UPDATE cars 
                    SET retestDeadline = (
                        SELECT h.retestDeadline 
                        FROM car_inspection_history h 
                        WHERE h.carId = cars.id 
                        ORDER BY h.inspectionDate DESC, h.createdAt DESC 
                        LIMIT 1
                    )
                    WHERE (cars.retestDeadline IS NULL OR trim(cars.retestDeadline) = '') 
                      AND EXISTS (
                        SELECT 1 
                        FROM car_inspection_history h2 
                        WHERE h2.carId = cars.id 
                          AND h2.isPassed = 0 
                          AND h2.retestDeadline IS NOT NULL 
                          AND trim(h2.retestDeadline) != ''
                          AND h2.id = (
                              SELECT h3.id 
                              FROM car_inspection_history h3 
                              WHERE h3.carId = cars.id 
                              ORDER BY h3.inspectionDate DESC, h3.createdAt DESC 
                              LIMIT 1
                          )
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 직전 v7 설치 기기에서 누락될 수 있는 기존 재검사 기한을 확실하게 cars.retestDeadline으로 이관
                db.execSQL("""
                    UPDATE cars 
                    SET retestDeadline = (
                        SELECT h.retestDeadline 
                        FROM car_inspection_history h 
                        WHERE h.carId = cars.id 
                        ORDER BY h.inspectionDate DESC, h.createdAt DESC 
                        LIMIT 1
                    )
                    WHERE (cars.retestDeadline IS NULL OR trim(cars.retestDeadline) = '') 
                      AND EXISTS (
                        SELECT 1 
                        FROM car_inspection_history h2 
                        WHERE h2.carId = cars.id 
                          AND h2.isPassed = 0 
                          AND h2.retestDeadline IS NOT NULL 
                          AND trim(h2.retestDeadline) != ''
                          AND h2.id = (
                              SELECT h3.id 
                              FROM car_inspection_history h3 
                              WHERE h3.carId = cars.id 
                              ORDER BY h3.inspectionDate DESC, h3.createdAt DESC 
                              LIMIT 1
                          )
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // maintenance_rules 테이블에 관리 시작 기준 필드 추가 (기존 데이터 완전 보존)
                db.execSQL("ALTER TABLE `maintenance_rules` ADD COLUMN `baselineDate` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `maintenance_rules` ADD COLUMN `baselineMileage` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `maintenance_rules` ADD COLUMN `baselineHours` REAL DEFAULT NULL")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // cars 테이블에 정비 관리 기준 및 최근 휠 얼라인먼트 키로수 추가
                db.execSQL("ALTER TABLE `cars` ADD COLUMN `maintenanceStandard` TEXT NOT NULL DEFAULT 'OFFICIAL'")
                db.execSQL("ALTER TABLE `cars` ADD COLUMN `wheelAlignmentMileage` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 기존 데이터 중 동일 차량, 동일 계절, 동일 바퀴 위치에 중복 STORAGE 타이어가 존재하는 경우,
                // 가장 최근 업데이트된 1개를 제외한 구형 중복 타이어를 'RETIRED'로 정리하여 계절 복귀 조합의 무결성 보장
                db.execSQL("""
                    UPDATE tires 
                    SET position = 'RETIRED'
                    WHERE isMounted = 0 
                      AND position = 'STORAGE'
                      AND id NOT IN (
                          SELECT t1.id FROM tires t1
                          WHERE t1.isMounted = 0 AND t1.position = 'STORAGE'
                            AND t1.updatedAt = (
                                SELECT max(t2.updatedAt) FROM tires t2
                                WHERE t2.carId = t1.carId 
                                  AND t2.season = t1.season 
                                  AND coalesce(t2.assignedWheelPosition, t2.position) = coalesce(t1.assignedWheelPosition, t1.position)
                                  AND t2.isMounted = 0 AND t2.position = 'STORAGE'
                            )
                      )
                """.trimIndent())
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `fuel_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` TEXT NOT NULL,
                        `odometer` INTEGER NOT NULL,
                        `fuelAmount` REAL NOT NULL,
                        `unitPrice` INTEGER NOT NULL,
                        `totalPrice` INTEGER NOT NULL,
                        `isFull` INTEGER NOT NULL,
                        `isMissedPrevious` INTEGER NOT NULL,
                        `calculatedEconomy` REAL,
                        `gasStationName` TEXT,
                        `refuelDate` INTEGER NOT NULL,
                        `receiptImageUri` TEXT,
                        FOREIGN KEY(`vehicleId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_records_vehicleId` ON `fuel_records` (`vehicleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_records_refuelDate` ON `fuel_records` (`refuelDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_records_odometer` ON `fuel_records` (`odometer`)")
            }
        }

        fun getInstance(context: Context): MahaGarageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MahaGarageDatabase::class.java,
                    "maha_garage.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 최초 DB 생성 시 기본 카탈로그 데이터 시딩
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).catalogDao().insertAll(DefaultCatalogData.items)
                            }
                        }
                    })
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
