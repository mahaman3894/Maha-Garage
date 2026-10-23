package maha.garage.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.domain.engine.InspectionStatusType
import maha.garage.domain.engine.VehicleComplianceCalculator
import maha.garage.util.NotificationHelper
import maha.garage.util.NotificationSettingsStore
import java.time.LocalDate

/**
 * 백그라운드 정밀 알림 Worker.
 * 사용자 요구사항에 따라 불필요한 소모품/타이어 알림을 모두 배제하고,
 * 오직 [자동차 법정 정기검사/재검사 기한] 및 [자동차 보험 만기/갱신일] 알림만 정밀 발송합니다.
 */
class MaintenanceNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private val notificationMutex = Mutex()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        notificationMutex.withLock {
            try {
                val db = MahaGarageDatabase.getInstance(applicationContext)

                // 1. Rest Area (휴면 차고)에 보관된 차량의 모든 알림(검사, 보험, 정비) 일괄 취소 및 원천 배제
                val archivedCars = db.carDao().getAllArchivedCarsSnapshot()
                for (archived in archivedCars) {
                    NotificationHelper.cancelAllForCar(applicationContext, archived.id)
                }

                // 2. 오직 현재 활성 운용 중인 차량(isArchived = 0)만 알림 대상 처리
                val cars = db.carDao().getAllActiveCarsSnapshot()
                if (cars.isEmpty()) return@withContext Result.success()

                val settings = NotificationSettingsStore(applicationContext)
                val prefs = applicationContext.getSharedPreferences("maha_garage_notification_prefs", Context.MODE_PRIVATE)
                val today = LocalDate.now()

                for (car in cars) {
                    val carName = "${car.make} ${car.modelName} (${car.carNumber})".trim()

                    // 기존 잔여 레거시 소모품/타이어 알림 일괄 취소 (더 이상 발송하지 않음)
                    NotificationHelper.cancelMaintenance(applicationContext, car.id, 100)
                    NotificationHelper.cancelMaintenance(applicationContext, car.id, 200)
                    NotificationHelper.cancelTireSwap(applicationContext, car.id)

                    // 1. 자동차 정기검사 및 법정 재검사 기한 알림
                    if (settings.isInspectionEnabled) {
                        handleInspectionNotification(car, carName, today, prefs)
                    } else {
                        NotificationHelper.cancelInspection(applicationContext, car.id)
                    }

                    // 2. 자동차 보험 만기일 및 갱신 기한 알림
                    if (settings.isInsuranceEnabled) {
                        handleInsuranceNotification(car, carName, today, prefs)
                    } else {
                        NotificationHelper.cancelInsurance(applicationContext, car.id)
                    }
                }

                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }

    /**
     * 자동차 정기검사/종합검사/재검사 기한 알림 처리
     */
    private fun handleInspectionNotification(
        car: maha.garage.data.local.entity.CarEntity,
        carName: String,
        today: LocalDate,
        prefs: android.content.SharedPreferences
    ) {
        val inspectionEval = VehicleComplianceCalculator.evaluateInspection(car, today)
        val isInspectionAlertTarget = inspectionEval.status == InspectionStatusType.URGENT ||
                inspectionEval.status == InspectionStatusType.OVERDUE ||
                inspectionEval.status == InspectionStatusType.RETEST_DUE

        val isRetest = !car.retestDeadline.isNullOrBlank()
        val scheduleKey = if (isRetest) "retest_${car.retestDeadline}" else "insp_${car.inspectionEndDate ?: "none"}"
        val lastSentKey = "last_alert_${car.id}_$scheduleKey"
        val lastSigKey = "sig_alert_${car.id}_$scheduleKey"

        if (isInspectionAlertTarget) {
            val lastAlertSent = prefs.getLong(lastSentKey, 0L)
            val lastSig = prefs.getString(lastSigKey, null)
            val currentSig = "${inspectionEval.status.name}:${inspectionEval.expiryDateStr}:${car.retestDeadline}:${car.lastInspectionDate}"

            val isStatusChanged = (lastSig == null || lastSig != currentSig)
            val isCooldownElapsed = (System.currentTimeMillis() - lastAlertSent > 3 * 24 * 60 * 60 * 1000L)

            if (isStatusChanged || isCooldownElapsed) {
                val isOverdue = inspectionEval.status == InspectionStatusType.OVERDUE
                val title = if (isRetest) {
                    if (isOverdue) "🚨 [자동차 재검사 기한 초과] $carName"
                    else "⚠️ [자동차 재검사 기한 안내] $carName"
                } else if (isOverdue) {
                    "🚨 [자동차 검사 기한 초과] $carName"
                } else {
                    "📅 [자동차 검사 기한 임박] $carName"
                }

                val content = if (isRetest) {
                    if (isOverdue) {
                        "정기/종합검사 부적합에 따른 재검사 기한이 ${-inspectionEval.daysToDeadline}일 초과되었습니다. 지연 과태료가 부과될 수 있습니다."
                    } else {
                        "정기/종합검사 부적합에 따른 법정 재검사 기한이 ${inspectionEval.daysToDeadline}일 남았습니다. 기한 내 수검해 주세요."
                    }
                } else if (isOverdue) {
                    "자동차 ${inspectionEval.inspectionType} 기한이 ${-inspectionEval.daysToDeadline}일 초과되었습니다. 지연 과태료가 부과될 수 있습니다."
                } else {
                    "자동차 ${inspectionEval.inspectionType} 최종 기한까지 ${inspectionEval.daysToDeadline}일 남았습니다. 사전 예약을 권장합니다."
                }

                val sent = NotificationHelper.showInspectionNotification(
                    context = applicationContext,
                    carId = car.id,
                    carName = carName,
                    title = title,
                    content = content,
                    isOverdue = isOverdue
                )
                if (sent) {
                    prefs.edit()
                        .putLong(lastSentKey, System.currentTimeMillis())
                        .putString(lastSigKey, currentSig)
                        .apply()
                }
            }
        } else {
            NotificationHelper.cancelInspection(applicationContext, car.id)
            prefs.edit()
                .remove(lastSentKey)
                .remove(lastSigKey)
                .apply()
        }
    }

    /**
     * 자동차 보험 갱신 및 만기일 알림 처리
     * 만료 30일 전, 7일 전, 당일, 만료 초과 시 단계별 알림 발송
     */
    private fun handleInsuranceNotification(
        car: maha.garage.data.local.entity.CarEntity,
        carName: String,
        today: LocalDate,
        prefs: android.content.SharedPreferences
    ) {
        val endDateStr = car.insuranceEndDate?.trim()
        val alert = maha.garage.domain.engine.InsuranceNotificationPolicy.evaluate(endDateStr, today)
        if (alert == null) {
            NotificationHelper.cancelInsurance(applicationContext, car.id)
            return
        }
        val daysRemaining = alert.daysRemaining
        val scheduleKey = "ins_${endDateStr}"
        val lastSentKey = "last_ins_${car.id}_$scheduleKey"
        val lastSigKey = "sig_ins_${car.id}_$scheduleKey"

        run {
            val lastSent = prefs.getLong(lastSentKey, 0L)
            val currentSig = alert.stage
            val lastSig = prefs.getString(lastSigKey, null)
            val isStatusChanged = (lastSig == null || lastSig != currentSig)
            val isCooldownElapsed = (System.currentTimeMillis() - lastSent > 3 * 24 * 60 * 60 * 1000L)

            if (isStatusChanged || isCooldownElapsed) {
                val isOverdue = daysRemaining < 0
                val title = when {
                    daysRemaining < 0 -> "🚨 [자동차 보험 만기 초과] $carName"
                    daysRemaining == 0L -> "🚨 [자동차 보험 만기 당일] $carName"
                    daysRemaining <= 7 -> "⚠️ [자동차 보험 만기 임박] $carName"
                    else -> "📅 [자동차 보험 갱신 안내] $carName"
                }

                val companyText = car.insuranceCompany?.let { "($it) " } ?: ""
                val content = when {
                    daysRemaining < 0 -> "${companyText}자동차 보험 만료일($endDateStr)이 ${-daysRemaining}일 지났습니다. 과태료 및 무보험 사고에 유의하세요."
                    daysRemaining == 0L -> "${companyText}자동차 보험 만기일 당일입니다! 미가입 상태가 되지 않도록 오늘 중 갱신해 주세요."
                    daysRemaining <= 7 -> "${companyText}자동차 보험 만기까지 ${daysRemaining}일 남았습니다. 만기 전 갱신 가입을 권장합니다."
                    else -> "${companyText}자동차 보험 만료일($endDateStr)까지 ${daysRemaining}일 남았습니다. 사전 갱신 비교를 준비해 보세요."
                }

                val sent = NotificationHelper.showInsuranceNotification(
                    context = applicationContext,
                    carId = car.id,
                    carName = carName,
                    title = title,
                    content = content,
                    isOverdue = isOverdue
                )
                if (sent) {
                    prefs.edit()
                        .putLong(lastSentKey, System.currentTimeMillis())
                        .putString(lastSigKey, currentSig)
                        .apply()
                }
            }
        }
    }
}
