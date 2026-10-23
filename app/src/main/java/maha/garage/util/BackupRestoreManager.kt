package maha.garage.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.data.local.entity.*
import maha.garage.data.model.enums.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ImportResult(
    val carsCount: Int,
    val rulesCount: Int,
    val visitsCount: Int,
    val linesCount: Int,
    val swapLogsCount: Int,
    val readingsCount: Int,
    val inspectionsCount: Int = 0,
    val missingPhotos: Int = 0
)

data class BackupExport(val file: File, val missingPhotos: Int)

object BackupRestoreManager {

    suspend fun generateBackupJson(db: MahaGarageDatabase, context: Context? = null): JSONObject = withContext(Dispatchers.IO) {
        val snapshot = db.withTransaction {
        val cars = db.carDao().getAllCarsDirect()
        val rules = db.maintenanceRuleDao().getAllRulesDirect()
        val visitsWithLines = db.serviceVisitDao().getAllVisitsDirect()
        val swapLogs = db.tireSwapLogDao().getAllSwapLogsDirect()
        val readings = db.meterReadingDao().getAllReadingsDirect()

        val root = JSONObject()
        root.put("app", "MahaGarage")
        root.put("version", 3)
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("exportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))

        // 1. 차량 목록 직렬화
        val carsArray = JSONArray()
        for (car in cars) {
            val cObj = JSONObject().apply {
                put("id", car.id)
                put("carNumber", car.carNumber)
                put("modelName", car.modelName)
                put("nickname", car.nickname ?: "")
                put("make", car.make)
                put("modelYear", car.modelYear)
                put("firstRegistrationDate", car.firstRegistrationDate ?: "")
                put("powertrain", car.powertrain.name)
                put("vehicleClass", car.vehicleClass.name)
                put("currentMileage", car.currentMileage)
                if (car.engineHours != null) put("engineHours", car.engineHours)
                put("currentTireMode", car.currentTireMode)
                put("summerTireMileage", car.summerTireMileage)
                put("winterTireMileage", car.winterTireMileage)
                put("lastSwapMileage", car.lastSwapMileage)
                put("summerTireName", car.summerTireName ?: "")
                put("winterTireName", car.winterTireName ?: "")
                put("photoUri", car.photoUri ?: "")
                put("isSevereCondition", car.isSevereCondition)
                put("maintenanceStandard", car.maintenanceStandard)
                if (car.wheelAlignmentMileage != null) put("wheelAlignmentMileage", car.wheelAlignmentMileage)
                put("isArchived", car.isArchived)
                put("updatedAt", car.updatedAt)

                // 제원 및 타이밍 구동계 필드
                put("timingType", car.timingType)
                put("timingDesc", car.timingDesc ?: "")
                put("trimName", car.trimName ?: "")
                put("displacement", car.displacement ?: "")
                put("power", car.power ?: "")
                put("torque", car.torque ?: "")
                put("transmission", car.transmission ?: "")
                put("driveSystem", car.driveSystem ?: "")
                put("fuelEconomy", car.fuelEconomy ?: "")
                put("dimensions", car.dimensions ?: "")

                // 정기검사 및 보험
                put("isCommercial", car.isCommercial)
                put("inspectionType", car.inspectionType)
                put("inspectionStartDate", car.inspectionStartDate ?: "")
                put("inspectionEndDate", car.inspectionEndDate ?: "")
                put("isInspectionDateEstimated", car.isInspectionDateEstimated)
                put("lastInspectionDate", car.lastInspectionDate ?: "")
                put("lastInspectionResult", car.lastInspectionResult ?: "")
                put("retestDeadline", car.retestDeadline ?: "")
                put("insuranceCompany", car.insuranceCompany ?: "")
                put("insuranceStartDate", car.insuranceStartDate ?: "")
                put("insuranceEndDate", car.insuranceEndDate ?: "")
                put("insurancePolicyNumber", car.insurancePolicyNumber ?: "")
                put("insuranceEmergencyCall", car.insuranceEmergencyCall ?: "")
            }
            carsArray.put(cObj)
        }
        root.put("cars", carsArray)

        // 2. 정비 규칙 직렬화
        val rulesArray = JSONArray()
        for (rule in rules) {
            val rObj = JSONObject().apply {
                put("id", rule.id)
                put("carId", rule.carId)
                put("itemCode", rule.itemCode)
                put("actionType", rule.actionType.name)
                if (rule.intervalKm != null) put("intervalKm", rule.intervalKm)
                if (rule.intervalMonths != null) put("intervalMonths", rule.intervalMonths)
                if (rule.intervalHours != null) put("intervalHours", rule.intervalHours)
                if (rule.firstIntervalKm != null) put("firstIntervalKm", rule.firstIntervalKm)
                if (rule.firstIntervalMonths != null) put("firstIntervalMonths", rule.firstIntervalMonths)
                put("sourceType", rule.sourceType.name)
                put("isVerified", rule.isVerified)
                put("isEnabled", rule.isEnabled)
                put("lastServiceDate", rule.lastServiceDate ?: "")
                if (rule.lastServiceMileage != null) put("lastServiceMileage", rule.lastServiceMileage)
                if (rule.lastServiceHours != null) put("lastServiceHours", rule.lastServiceHours)
                put("lastConditionValue", rule.lastConditionValue ?: "")
                if (rule.baselineDate != null) put("baselineDate", rule.baselineDate)
                if (rule.baselineMileage != null) put("baselineMileage", rule.baselineMileage)
                if (rule.baselineHours != null) put("baselineHours", rule.baselineHours)
                put("userCustomNotes", rule.userCustomNotes ?: "")
            }
            rulesArray.put(rObj)
        }
        root.put("rules", rulesArray)

        // 3. 정비 방문 및 작업 항목 직렬화
        val visitsArray = JSONArray()
        val linesArray = JSONArray()
        for (vwl in visitsWithLines) {
            val v = vwl.visit
            val vObj = JSONObject().apply {
                put("id", v.id)
                put("carId", v.carId)
                put("serviceDate", v.serviceDate)
                put("odometerKm", v.odometerKm)
                if (v.engineHours != null) put("engineHours", v.engineHours)
                put("workshop", v.workshop ?: "")
                put("totalCost", v.totalCost)
                put("notes", v.notes ?: "")
                put("receiptPhotoUri", v.receiptPhotoUri ?: "")
                put("createdAt", v.createdAt)
            }
            visitsArray.put(vObj)

            for (l in vwl.lines) {
                val lObj = JSONObject().apply {
                    put("id", l.id)
                    put("visitId", l.visitId)
                    put("itemCode", l.itemCode)
                    put("actionType", l.actionType.name)
                    put("partsCost", l.partsCost)
                    put("laborCost", l.laborCost)
                    put("conditionValue", l.conditionValue ?: "")
                    put("partSpecification", l.partSpecification ?: "")
                    put("notes", l.notes ?: "")
                }
                linesArray.put(lObj)
            }
        }
        root.put("serviceVisits", visitsArray)
        root.put("serviceLines", linesArray)

        // 4. 타이어 스왑 로그 직렬화
        val swapLogsArray = JSONArray()
        for (log in swapLogs) {
            val sObj = JSONObject().apply {
                put("id", log.id)
                put("carId", log.carId)
                put("swapDate", log.swapDate)
                put("swapMileage", log.swapMileage)
                put("fromMode", log.fromMode)
                put("toMode", log.toMode)
                put("drivenDistanceDuringMode", log.drivenDistanceDuringMode)
                put("storageLocation", log.storageLocation ?: "")
                if (log.frontTreadDepthMm != null) put("frontTreadDepthMm", log.frontTreadDepthMm)
                if (log.rearTreadDepthMm != null) put("rearTreadDepthMm", log.rearTreadDepthMm)
                put("swapCost", log.swapCost)
                put("notes", log.notes ?: "")
                put("createdAt", log.createdAt)
            }
            swapLogsArray.put(sObj)
        }
        root.put("tireSwapLogs", swapLogsArray)

        // 5. 주행거리 누적 로그 직렬화
        val readingsArray = JSONArray()
        for (reading in readings) {
            val rdObj = JSONObject().apply {
                put("id", reading.id)
                put("carId", reading.carId)
                put("readingDate", reading.readingDate)
                put("odometerKm", reading.odometerKm)
                if (reading.engineHours != null) put("engineHours", reading.engineHours)
                put("inputSource", reading.inputSource)
                put("createdAt", reading.createdAt)
            }
            readingsArray.put(rdObj)
        }
        root.put("meterReadings", readingsArray)

        // 6. 개별 타이어 직렬화
        val tires = db.tireDao().getAllTiresDirect()
        val tiresArray = JSONArray()
        for (t in tires) {
            val tObj = maha.garage.domain.engine.TireArchiveCodec.encode(t)
            tiresArray.put(tObj)
        }
        root.put("tires", tiresArray)

        // 7. 타이어 장착/위치교환/스왑 이력 직렬화
        val histories = db.tireDao().getAllHistoryDirect()
        val historiesArray = JSONArray()
        for (h in histories) {
            val hObj = JSONObject().apply {
                put("id", h.id)
                put("carId", h.carId)
                put("tireId", h.tireId)
                put("eventType", h.eventType)
                put("fromPosition", h.fromPosition ?: "")
                put("toPosition", h.toPosition ?: "")
                put("eventDate", h.eventDate)
                put("odometerKm", h.odometerKm)
                put("drivenDistanceSinceLastEvent", h.drivenDistanceSinceLastEvent)
                if (h.treadDepthMm != null) put("treadDepthMm", h.treadDepthMm)
                put("cost", h.cost)
                put("notes", h.notes ?: "")
                put("createdAt", h.createdAt)
            }
            historiesArray.put(hObj)
        }
        root.put("tireHistories", historiesArray)

        // 8. 자동차 정기검사 이력 직렬화
        val inspectionHistories = db.carInspectionDao().getAllInspectionsDirect()
        val inspectionsArray = JSONArray()
        for (ins in inspectionHistories) {
            val insObj = JSONObject().apply {
                put("id", ins.id)
                put("carId", ins.carId)
                put("inspectionDate", ins.inspectionDate)
                put("inspectionType", ins.inspectionType)
                put("stationName", ins.stationName ?: "")
                put("isPassed", ins.isPassed)
                put("nextExpiryDate", ins.nextExpiryDate ?: "")
                put("retestDeadline", ins.retestDeadline ?: "")
                put("cost", ins.cost)
                put("memo", ins.memo ?: "")
                put("certificatePhotoUri", ins.certificatePhotoUri ?: "")
                put("createdAt", ins.createdAt)
            }
            inspectionsArray.put(insObj)
        }
        root.put("carInspections", inspectionsArray)
        root
        }
        if (context != null) BackupPhotoArchive.pack(context, snapshot)
        snapshot
    }

    suspend fun exportBackup(context: Context, db: MahaGarageDatabase): BackupExport = withContext(Dispatchers.IO) {
        val root = generateBackupJson(db, context)
        val fileName = "MahaGarage_Backup_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.json"
        val backupFile = File(context.cacheDir, fileName)
        backupFile.writeText(root.toString(2), Charsets.UTF_8)
        BackupExport(backupFile, root.optInt("missingPhotos", 0))
    }

    fun shareBackup(context: Context, backupFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "[마하개러지] 데이터 백업 파일")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "마하개러지 백업 파일 내보내기").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    suspend fun importBackup(context: Context, db: MahaGarageDatabase, jsonContent: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        val restoredFiles = mutableListOf<File>()
        var committed = false
        try {
            val root = JSONObject(jsonContent)
            if (!root.has("app") || root.getString("app") != "MahaGarage") {
                return@withContext Result.failure(IllegalArgumentException("올바른 마하개러지 백업 파일이 아닙니다."))
            }

            val missingPhotos = BackupPhotoArchive.unpack(context, root, restoredFiles)

            // 차량 파싱
            val carsList = mutableListOf<CarEntity>()
            val carsArray = root.optJSONArray("cars") ?: JSONArray()
            for (i in 0 until carsArray.length()) {
                val c = carsArray.getJSONObject(i)
                carsList.add(
                    CarEntity(
                        id = c.getString("id"),
                        carNumber = c.getString("carNumber"),
                        modelName = c.getString("modelName"),
                        nickname = c.optString("nickname").ifEmpty { null },
                        make = c.optString("make", ""),
                        firstRegistrationDate = c.optString("firstRegistrationDate").ifEmpty { null },
                        modelYear = c.optInt("modelYear", 2023),
                        powertrain = runCatching { PowertrainType.valueOf(c.getString("powertrain")) }.getOrDefault(PowertrainType.GASOLINE),
                        vehicleClass = runCatching { VehicleClass.valueOf(c.getString("vehicleClass")) }.getOrDefault(VehicleClass.SEDAN),
                        currentMileage = c.optInt("currentMileage", 0),
                        engineHours = if (c.has("engineHours")) c.getDouble("engineHours") else null,
                        currentTireMode = c.optString("currentTireMode", "SUMMER"),
                        summerTireMileage = c.optInt("summerTireMileage", 0),
                        winterTireMileage = c.optInt("winterTireMileage", 0),
                        lastSwapMileage = c.optInt("lastSwapMileage", 0),
                        summerTireName = c.optString("summerTireName").ifEmpty { null },
                        winterTireName = c.optString("winterTireName").ifEmpty { null },
                        photoUri = c.optString("photoUri").ifEmpty { null },
                        isSevereCondition = c.optBoolean("isSevereCondition", false),
                        maintenanceStandard = c.optString("maintenanceStandard", if (c.optBoolean("isSevereCondition", false)) "SEVERE" else "OFFICIAL"),
                        wheelAlignmentMileage = if (c.has("wheelAlignmentMileage") && !c.isNull("wheelAlignmentMileage")) c.getInt("wheelAlignmentMileage") else null,
                        isArchived = c.optBoolean("isArchived", false),
                        updatedAt = c.optLong("updatedAt", System.currentTimeMillis()),
                        timingType = c.optString("timingType", "CHAIN"),
                        timingDesc = c.optString("timingDesc").ifEmpty { null },
                        trimName = c.optString("trimName").ifEmpty { null },
                        displacement = c.optString("displacement").ifEmpty { null },
                        power = c.optString("power").ifEmpty { null },
                        torque = c.optString("torque").ifEmpty { null },
                        transmission = c.optString("transmission").ifEmpty { null },
                        driveSystem = c.optString("driveSystem").ifEmpty { null },
                        fuelEconomy = c.optString("fuelEconomy").ifEmpty { null },
                        dimensions = c.optString("dimensions").ifEmpty { null },

                        // 정기검사 및 보험 (임의 가짜 데이터 대신 null 및 isInspectionDateEstimated=true 처리)
                        isCommercial = c.optBoolean("isCommercial", false),
                        inspectionType = c.optString("inspectionType", "정기검사"),
                        inspectionStartDate = if (c.has("inspectionStartDate")) c.optString("inspectionStartDate").ifEmpty { null } else null,
                        inspectionEndDate = if (c.has("inspectionEndDate")) c.optString("inspectionEndDate").ifEmpty { null } else null,
                        isInspectionDateEstimated = c.optBoolean("isInspectionDateEstimated", true),
                        lastInspectionDate = c.optString("lastInspectionDate").ifEmpty { null },
                        lastInspectionResult = c.optString("lastInspectionResult").ifEmpty { null },
                        retestDeadline = if (c.has("retestDeadline")) c.optString("retestDeadline").ifEmpty { null } else null,
                        insuranceCompany = if (c.has("insuranceCompany")) c.optString("insuranceCompany").ifEmpty { null } else null,
                        insuranceStartDate = if (c.has("insuranceStartDate")) c.optString("insuranceStartDate").ifEmpty { null } else null,
                        insuranceEndDate = if (c.has("insuranceEndDate")) c.optString("insuranceEndDate").ifEmpty { null } else null,
                        insurancePolicyNumber = c.optString("insurancePolicyNumber").ifEmpty { null },
                        insuranceEmergencyCall = if (c.has("insuranceEmergencyCall")) c.optString("insuranceEmergencyCall").ifEmpty { null } else null
                    )
                )
            }

            // 규칙 파싱
            val rulesList = mutableListOf<MaintenanceRuleEntity>()
            val rulesArray = root.optJSONArray("rules") ?: JSONArray()
            for (i in 0 until rulesArray.length()) {
                val r = rulesArray.getJSONObject(i)
                rulesList.add(
                    MaintenanceRuleEntity(
                        id = r.getString("id"),
                        carId = r.getString("carId"),
                        itemCode = r.getString("itemCode"),
                        actionType = runCatching { ActionType.valueOf(r.getString("actionType")) }.getOrDefault(ActionType.REPLACE),
                        intervalKm = if (r.has("intervalKm")) r.getInt("intervalKm") else null,
                        intervalMonths = if (r.has("intervalMonths")) r.getInt("intervalMonths") else null,
                        intervalHours = if (r.has("intervalHours")) r.getDouble("intervalHours") else null,
                        firstIntervalKm = if (r.has("firstIntervalKm")) r.getInt("firstIntervalKm") else null,
                        firstIntervalMonths = if (r.has("firstIntervalMonths")) r.getInt("firstIntervalMonths") else null,
                        sourceType = runCatching { SourceType.valueOf(r.getString("sourceType")) }.getOrDefault(SourceType.VERIFIED_OEM),
                        isVerified = r.optBoolean("isVerified", false),
                        isEnabled = r.optBoolean("isEnabled", true),
                        lastServiceDate = r.optString("lastServiceDate").ifEmpty { null },
                        lastServiceMileage = if (r.has("lastServiceMileage")) r.getInt("lastServiceMileage") else null,
                        lastServiceHours = if (r.has("lastServiceHours")) r.getDouble("lastServiceHours") else null,
                        lastConditionValue = r.optString("lastConditionValue").ifEmpty { null },
                        baselineDate = if (r.has("baselineDate")) r.optString("baselineDate").ifEmpty { null } else null,
                        baselineMileage = if (r.has("baselineMileage")) r.getInt("baselineMileage") else null,
                        baselineHours = if (r.has("baselineHours")) r.getDouble("baselineHours") else null,
                        userCustomNotes = r.optString("userCustomNotes").ifEmpty { null }
                    )
                )
            }

            // 방문 및 작업 라인 파싱
            val visitsList = mutableListOf<ServiceVisitEntity>()
            val visitsArray = root.optJSONArray("serviceVisits") ?: JSONArray()
            for (i in 0 until visitsArray.length()) {
                val v = visitsArray.getJSONObject(i)
                visitsList.add(
                    ServiceVisitEntity(
                        id = v.getString("id"),
                        carId = v.getString("carId"),
                        serviceDate = v.getString("serviceDate"),
                        odometerKm = v.getInt("odometerKm"),
                        engineHours = if (v.has("engineHours")) v.getDouble("engineHours") else null,
                        workshop = v.optString("workshop").ifEmpty { null },
                        totalCost = v.optLong("totalCost", 0L),
                        notes = v.optString("notes").ifEmpty { null },
                        receiptPhotoUri = v.optString("receiptPhotoUri").ifEmpty { null },
                        createdAt = v.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val linesList = mutableListOf<ServiceLineEntity>()
            val linesArray = root.optJSONArray("serviceLines") ?: JSONArray()
            for (i in 0 until linesArray.length()) {
                val l = linesArray.getJSONObject(i)
                linesList.add(
                    ServiceLineEntity(
                        id = l.getString("id"),
                        visitId = l.getString("visitId"),
                        itemCode = l.getString("itemCode"),
                        actionType = runCatching { ActionType.valueOf(l.getString("actionType")) }.getOrDefault(ActionType.REPLACE),
                        partsCost = l.optLong("partsCost", 0L),
                        laborCost = l.optLong("laborCost", 0L),
                        conditionValue = l.optString("conditionValue").ifEmpty { null },
                        partSpecification = l.optString("partSpecification").ifEmpty { null },
                        notes = l.optString("notes").ifEmpty { null }
                    )
                )
            }

            // 타이어 스왑 로그 파싱
            val swapLogsList = mutableListOf<TireSwapLogEntity>()
            val swapLogsArray = root.optJSONArray("tireSwapLogs") ?: JSONArray()
            for (i in 0 until swapLogsArray.length()) {
                val s = swapLogsArray.getJSONObject(i)
                swapLogsList.add(
                    TireSwapLogEntity(
                        id = s.getString("id"),
                        carId = s.getString("carId"),
                        swapDate = s.getString("swapDate"),
                        swapMileage = s.getInt("swapMileage"),
                        fromMode = s.getString("fromMode"),
                        toMode = s.getString("toMode"),
                        drivenDistanceDuringMode = s.getInt("drivenDistanceDuringMode"),
                        storageLocation = s.optString("storageLocation").ifEmpty { null },
                        frontTreadDepthMm = if (s.has("frontTreadDepthMm")) s.getDouble("frontTreadDepthMm") else null,
                        rearTreadDepthMm = if (s.has("rearTreadDepthMm")) s.getDouble("rearTreadDepthMm") else null,
                        swapCost = s.optLong("swapCost", 0L),
                        notes = s.optString("notes").ifEmpty { null },
                        createdAt = s.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // 계측 이력 파싱
            val readingsList = mutableListOf<MeterReadingEntity>()
            val readingsArray = root.optJSONArray("meterReadings") ?: JSONArray()
            for (i in 0 until readingsArray.length()) {
                val rd = readingsArray.getJSONObject(i)
                readingsList.add(
                    MeterReadingEntity(
                        id = rd.optString("id", java.util.UUID.randomUUID().toString()),
                        carId = rd.getString("carId"),
                        readingDate = rd.getString("readingDate"),
                        odometerKm = rd.getInt("odometerKm"),
                        engineHours = if (rd.has("engineHours")) rd.getDouble("engineHours") else null,
                        inputSource = rd.optString("inputSource", "MANUAL"),
                        createdAt = rd.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // 타이어 파싱
            val tiresList = mutableListOf<TireEntity>()
            val tiresArray = root.optJSONArray("tires") ?: JSONArray()
            for (i in 0 until tiresArray.length()) {
                val t = tiresArray.getJSONObject(i)
                tiresList.add(maha.garage.domain.engine.TireArchiveCodec.decode(t))
            }

            // 타이어 이력 파싱
            val historiesList = mutableListOf<TireInstallationHistoryEntity>()
            val historiesArray = root.optJSONArray("tireHistories") ?: JSONArray()
            for (i in 0 until historiesArray.length()) {
                val h = historiesArray.getJSONObject(i)
                historiesList.add(
                    TireInstallationHistoryEntity(
                        id = h.getString("id"),
                        carId = h.getString("carId"),
                        tireId = h.getString("tireId"),
                        eventType = h.getString("eventType"),
                        fromPosition = h.optString("fromPosition").ifEmpty { null },
                        toPosition = h.optString("toPosition").ifEmpty { null },
                        eventDate = h.getString("eventDate"),
                        odometerKm = h.getInt("odometerKm"),
                        drivenDistanceSinceLastEvent = h.optInt("drivenDistanceSinceLastEvent", 0),
                        treadDepthMm = if (h.has("treadDepthMm")) h.getDouble("treadDepthMm") else null,
                        cost = h.optLong("cost", 0L),
                        notes = h.optString("notes").ifEmpty { null },
                        createdAt = h.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // 8. 자동차 정기검사 이력 파싱
            val inspectionsList = mutableListOf<CarInspectionHistoryEntity>()
            val inspectionsArray = root.optJSONArray("carInspections") ?: JSONArray()
            for (i in 0 until inspectionsArray.length()) {
                val ins = inspectionsArray.getJSONObject(i)
                inspectionsList.add(
                    CarInspectionHistoryEntity(
                        id = ins.getString("id"),
                        carId = ins.getString("carId"),
                        inspectionDate = ins.getString("inspectionDate"),
                        inspectionType = ins.optString("inspectionType", "정기검사"),
                        stationName = ins.optString("stationName").ifEmpty { null },
                        isPassed = ins.optBoolean("isPassed", true),
                        nextExpiryDate = ins.optString("nextExpiryDate").ifEmpty { null },
                        retestDeadline = ins.optString("retestDeadline").ifEmpty { null },
                        cost = ins.optInt("cost", 0),
                        memo = ins.optString("memo").ifEmpty { null },
                        certificatePhotoUri = ins.optString("certificatePhotoUri").ifEmpty { null },
                        createdAt = ins.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // 트랜잭션으로 Room DB에 일괄 저장
            db.withTransaction {
                if (carsList.isNotEmpty()) db.carDao().insertCars(carsList)
                if (rulesList.isNotEmpty()) db.maintenanceRuleDao().insertRules(rulesList)
                if (visitsList.isNotEmpty()) db.serviceVisitDao().insertVisits(visitsList)
                if (linesList.isNotEmpty()) db.serviceVisitDao().insertLines(linesList)
                if (swapLogsList.isNotEmpty()) db.tireSwapLogDao().insertSwapLogs(swapLogsList)
                if (readingsList.isNotEmpty()) db.meterReadingDao().insertReadings(readingsList)
                if (tiresList.isNotEmpty()) db.tireDao().insertTires(tiresList)
                if (historiesList.isNotEmpty()) db.tireDao().insertHistories(historiesList)
                if (inspectionsList.isNotEmpty()) db.carInspectionDao().insertInspections(inspectionsList)
            }

            committed = true
            Result.success(
                ImportResult(
                    carsCount = carsList.size,
                    rulesCount = rulesList.size,
                    visitsCount = visitsList.size,
                    linesCount = linesList.size,
                    swapLogsCount = swapLogsList.size,
                    readingsCount = readingsList.size,
                    inspectionsCount = inspectionsList.size,
                    missingPhotos = missingPhotos
                )
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (!committed) restoredFiles.forEach { it.delete() }
        }
    }
}
