package maha.garage.domain.engine

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.TireSwapLogEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

object TireSwapCalculator {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    data class LiveTireDistances(
        val summerTotalKm: Int,
        val winterTotalKm: Int,
        val currentMountDrivenSinceSwapKm: Int
    )

    /**
     * 마지막 스왑 이후 현재까지 차량 주행거리 변화를 반영한
     * 섬머 및 윈터 타이어의 실시간 총 누적 주행거리를 계산합니다.
     */
    fun calculateLiveTireDistances(car: CarEntity): LiveTireDistances {
        val drivenSinceSwap = (car.currentMileage - car.lastSwapMileage).coerceAtLeast(0)

        return if (car.currentTireMode == "SUMMER") {
            LiveTireDistances(
                summerTotalKm = car.summerTireMileage + drivenSinceSwap,
                winterTotalKm = car.winterTireMileage,
                currentMountDrivenSinceSwapKm = drivenSinceSwap
            )
        } else {
            LiveTireDistances(
                summerTotalKm = car.summerTireMileage,
                winterTotalKm = car.winterTireMileage + drivenSinceSwap,
                currentMountDrivenSinceSwapKm = drivenSinceSwap
            )
        }
    }

    /**
     * 사용자가 직접 수정한 실시간 타이어 총 누적거리(Live Total) 및 이전 교체 시점 주행거리로부터
     * CarEntity에 영구 저장할 기본 누적 주행거리(baseMileage)를 역산출합니다.
     */
    fun computeBaseTireMileages(
        car: CarEntity,
        newCarMileage: Int,
        targetSummerTotal: Int?,
        targetWinterTotal: Int?,
        targetSwapMileage: Int?
    ): Triple<Int, Int, Int> {
        val effectiveSwapMileage = targetSwapMileage ?: car.lastSwapMileage
        val drivenSinceSwap = (newCarMileage - effectiveSwapMileage).coerceAtLeast(0)

        val newSummerBase = if (targetSummerTotal != null) {
            if (car.currentTireMode == "SUMMER") {
                (targetSummerTotal - drivenSinceSwap).coerceAtLeast(0)
            } else {
                targetSummerTotal
            }
        } else {
            car.summerTireMileage
        }

        val newWinterBase = if (targetWinterTotal != null) {
            if (car.currentTireMode == "WINTER") {
                (targetWinterTotal - drivenSinceSwap).coerceAtLeast(0)
            } else {
                targetWinterTotal
            }
        } else {
            car.winterTireMileage
        }

        return Triple(newSummerBase, newWinterBase, effectiveSwapMileage)
    }

    data class SwapResult(
        val updatedCar: CarEntity,
        val swapLog: TireSwapLogEntity
    )

    /**
     * 타이어 스왑(교체 장착)을 수행합니다.
     * 이전 장착 타이어의 누적 주행거리를 확정 누적하고, 신규 모드로 전환 및 로그를 생성합니다.
     */
    fun performTireSwap(
        car: CarEntity,
        targetMode: String, // "SUMMER" or "WINTER"
        swapMileage: Int = car.currentMileage,
        swapDate: String = LocalDate.now().format(dateFormatter),
        storageLocation: String? = null,
        frontTreadDepthMm: Double? = null,
        rearTreadDepthMm: Double? = null,
        swapCost: Long = 0L,
        notes: String? = null
    ): SwapResult {
        require(targetMode in setOf("SUMMER", "WINTER") && targetMode != car.currentTireMode)
        require(swapMileage >= car.lastSwapMileage && swapMileage >= 0)
        require(swapCost >= 0)
        require(listOfNotNull(frontTreadDepthMm, rearTreadDepthMm).all { it.isFinite() && it >= 0 })
        val currentMode = car.currentTireMode
        val drivenDuringPreviousMode = (swapMileage - car.lastSwapMileage).coerceAtLeast(0)

        // 이전 장착 타이어의 주행거리 가산
        val newSummerMileage = if (currentMode == "SUMMER") {
            car.summerTireMileage + drivenDuringPreviousMode
        } else {
            car.summerTireMileage
        }

        val newWinterMileage = if (currentMode == "WINTER") {
            car.winterTireMileage + drivenDuringPreviousMode
        } else {
            car.winterTireMileage
        }

        val updatedCar = car.copy(
            currentTireMode = targetMode,
            summerTireMileage = newSummerMileage,
            winterTireMileage = newWinterMileage,
            lastSwapMileage = swapMileage,
            currentMileage = maxOf(car.currentMileage, swapMileage),
            updatedAt = System.currentTimeMillis()
        )

        val swapLog = TireSwapLogEntity(
            id = UUID.randomUUID().toString(),
            carId = car.id,
            swapDate = swapDate,
            swapMileage = swapMileage,
            fromMode = currentMode,
            toMode = targetMode,
            drivenDistanceDuringMode = drivenDuringPreviousMode,
            storageLocation = storageLocation,
            frontTreadDepthMm = frontTreadDepthMm,
            rearTreadDepthMm = rearTreadDepthMm,
            swapCost = swapCost,
            notes = notes
        )

        return SwapResult(updatedCar, swapLog)
    }
}
