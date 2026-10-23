package maha.garage.ui.vehicle

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.seed.DefaultCatalogData
import maha.garage.domain.engine.ApplicabilityResolver
import maha.garage.domain.engine.MaintenanceScheduleEngine
import maha.garage.ui.components.VehicleConsumablesSketchMap
import maha.garage.ui.theme.MahaGarageTheme
import java.time.LocalDate

private val previewCar = CarEntity(carNumber = "46라0991", modelName = "뉴 파일럿", make = "혼다", currentMileage = 145000)

@Preview(name = "Phone - actual sketch component", widthDp = 336, heightDp = 540)
@Preview(name = "Wide - actual sketch component", widthDp = 720, heightDp = 640)
@Preview(name = "Phone - large text", widthDp = 336, heightDp = 800, fontScale = 1.5f)
@Composable
private fun SketchPreview() {
    val catalog = DefaultCatalogData.items.associateBy { it.itemCode }
    val evaluations = ApplicabilityResolver.resolveDefaultRules(previewCar, DefaultCatalogData.items).mapNotNull { rule ->
        catalog[rule.itemCode]?.let { MaintenanceScheduleEngine.evaluateItem(previewCar, rule, it, LocalDate.of(2026, 10, 17)) }
    }
    MahaGarageTheme(darkTheme = true) {
        VehicleConsumablesSketchMap(previewCar.vehicleClass, previewCar.powertrain, evaluations, {}, diagramHeight = 390.dp)
    }
}

@Preview(name = "Mileage gate - actual dialog", widthDp = 360, heightDp = 720)
@Composable
private fun MileageDialogPreview() {
    MahaGarageTheme(darkTheme = true) {
        UpdateMileageDialog(previewCar, onDismiss = {}, onConfirm = {})
    }
}

@Preview(name = "Renewal dates - phone", widthDp = 360, heightDp = 700)
@Preview(name = "Renewal dates - wide", widthDp = 760, heightDp = 430)
@Preview(name = "Renewal dates - large text", widthDp = 360, heightDp = 1100, fontScale = 1.5f)
@Composable
private fun RenewalDatesPreview() {
    val today = LocalDate.of(2026, 10, 18)
    val car = previewCar.copy(insuranceCompany = "삼성화재 다이렉트", insuranceStartDate = "2025-11-17", insuranceEndDate = "2026-11-17",
        lastInspectionDate = "2024-10-10", inspectionEndDate = "2026-10-10", isInspectionDateEstimated = false)
    MahaGarageTheme(darkTheme = true) {
        maha.garage.ui.components.VehicleRenewalCards(car, {}, {}, today = today)
    }
}
