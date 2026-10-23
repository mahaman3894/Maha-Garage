package maha.garage.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import maha.garage.data.local.entity.CarEntity
import maha.garage.domain.engine.VehicleComplianceCalculator
import maha.garage.ui.theme.*

@Composable
fun VehicleInspectionInsuranceSection(car: CarEntity, onOpenInspectionResultDialog: () -> Unit,
    onOpenInsuranceEditDialog: () -> Unit, onOpenGuideDialog: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val today = rememberRenewalToday()
    val inspection = VehicleComplianceCalculator.evaluateInspection(car, today)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("정기검사 · 보험 관리", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
        TextButton(onClick = onOpenGuideDialog) { Text("검사 주기 안내", color = GarageAccent) }
        VehicleRenewalCards(car, onOpenInsuranceEditDialog, onOpenInspectionResultDialog, today = today)
        if (inspection.startDateStr != "-") Text("검사 가능 기간\n${inspection.startDateStr} ~ ${inspection.endDateStr}",
            style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cyberts.kr/cp/pvr/pvr/readCpPvrPvrPeriodPage.do"))) }, modifier = Modifier.weight(1f)) { Text("검사 조회") }
            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cyberts.kr/srs/main.do"))) }, modifier = Modifier.weight(1f)) { Text("검사 예약") }
        }
        Button(onClick = onOpenInspectionResultDialog, modifier = Modifier.fillMaxWidth()) { Text("검사결과 등록") }
        OutlinedButton(onClick = onOpenInsuranceEditDialog, modifier = Modifier.fillMaxWidth()) { Text("보험정보 변경") }
        var showEmergencyDialog by remember { mutableStateOf(false) }
        Button(
            onClick = { showEmergencyDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SketchRed),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("사고 · 고장 긴급출동", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
        }

        if (showEmergencyDialog) {
            maha.garage.ui.vehicle.EmergencyDispatchDialog(
                car = car,
                onDismiss = { showEmergencyDialog = false }
            )
        }
    }
}
