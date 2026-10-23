package maha.garage.ui.vehicle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import maha.garage.data.local.entity.CarEntity
import maha.garage.ui.theme.*

/**
 * 국내 주요 손해보험사 긴급출동 정보
 */
data class InsurerEmergencyContact(
    val name: String,
    val phone: String,
    val brandName: String,
    val color: Color
)

val MAJOR_INSURERS = listOf(
    InsurerEmergencyContact("삼성화재", "1588-5114", "애니카 긴급출동", Color(0xFF1E40AF)),
    InsurerEmergencyContact("DB손해보험", "1588-0100", "프로미 긴급출동", Color(0xFF047857)),
    InsurerEmergencyContact("현대해상", "1588-5656", "하이카 긴급출동", Color(0xFFEA580C)),
    InsurerEmergencyContact("KB손해보험", "1544-0114", "매직카 긴급출동", Color(0xFFD97706)),
    InsurerEmergencyContact("메리츠화재", "1566-7711", "메리츠 긴급출동", Color(0xFFDC2626)),
    InsurerEmergencyContact("한화손해보험", "1566-8000", "차도리 긴급출동", Color(0xFFE11D48)),
    InsurerEmergencyContact("캐롯손해보험", "1566-0300", "퍼마일 긴급출동", Color(0xFF0284C7)),
    InsurerEmergencyContact("흥국화재", "1688-1688", "흥국 긴급출동", Color(0xFF7C3AED)),
    InsurerEmergencyContact("롯데손해보험", "1588-3344", "롯데 긴급출동", Color(0xFFBE123C)),
    InsurerEmergencyContact("하나손해보험", "1566-3000", "하나 긴급출동", Color(0xFF0F766E)),
    InsurerEmergencyContact("AXA손해보험", "1566-1566", "악사 긴급출동", Color(0xFF1D4ED8))
)

/**
 * 사고·고장 신고 및 긴급출동 다이얼로그
 * 1. 차량 정보 즉시 확인 (상담원 통화 시 필요한 차량번호, 차종, 보험사, 계기판 키로수)
 * 2. 등록된 보험사 원클릭 바로 연결
 * 3. 국내 11대 주요 손해보험사 긴급출동 전화번호 목록
 * 4. 한국도로공사 고속도로 무료 견인(1588-2504) & 112/119 원클릭 연결
 * 5. 교통사고 현장 대처 및 사설 견인차 방지 가이드
 */
@Composable
fun EmergencyDispatchDialog(
    car: CarEntity,
    onDismiss: () -> Unit,
    onSaveInsurancePhone: ((phone: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showAllInsurers by remember { mutableStateOf(false) }
    var showAccidentGuide by remember { mutableStateOf(false) }

    fun dial(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace("-", "")}"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyVehicleInfo() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = buildString {
            append("차량번호: ${car.carNumber}\n")
            append("차종: ${car.make} ${car.modelName}\n")
            if (!car.insuranceCompany.isNullOrBlank()) append("보험사: ${car.insuranceCompany}\n")
            if (!car.insurancePolicyNumber.isNullOrBlank()) append("증권번호: ${car.insurancePolicyNumber}\n")
            append("현재 주행거리: %,d km\n".format(car.currentMileage))
            append("구동/유종: ${car.powertrain.displayName}")
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("차량정보", text))
        Toast.makeText(context, "차량 정보가 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SketchRedSoft,
                border = BorderStroke(1.2.dp, SketchRed.copy(alpha = 0.6f)),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "긴급출동",
                        tint = SketchRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "사고 · 고장 긴급출동",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftInk,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "내 차 정보 확인 및 원클릭 출동 접수",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmGray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 통화 상담원 대응용 내 차량 요약 카드
                Surface(
                    color = Color(0xFFFFF7ED), // 앰버 소프트
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "통화 시 안내 정보",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C)
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { copyVehicleInfo() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "복사",
                                    tint = Color(0xFFC2410C),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "정보 복사",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = car.carNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = SoftInk
                            )
                            Text(
                                text = "${car.make} ${car.modelName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftInk
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "보험사: ${car.insuranceCompany ?: "미등록"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmGray
                            )
                            Text(
                                text = "계기판: %,d km".format(car.currentMileage),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftInk
                            )
                        }

                        if (!car.insurancePolicyNumber.isNullOrBlank()) {
                            Text(
                                text = "증권번호: ${car.insurancePolicyNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // 2. 내 등록 보험사 원클릭 바로 연결
                val registeredPhone = car.insuranceEmergencyCall?.takeIf { it.isNotBlank() }
                if (registeredPhone != null) {
                    val companyName = car.insuranceCompany?.takeIf { it.isNotBlank() } ?: "내 보험사"
                    Button(
                        onClick = { dial(registeredPhone) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SketchRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$companyName 긴급출동 연결 ($registeredPhone)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, WarmLine),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ToolOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "등록된 전용 긴급출동 번호가 없습니다. 아래 목록에서 가입 보험사를 터치해 바로 통화하세요.",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGray
                            )
                        }
                    }
                }

                // 3. 주요 손해보험사 긴급출동 목록
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WarmLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "국내 주요 보험사 긴급출동",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoftInk
                            )
                            Text(
                                text = if (showAllInsurers) "접기 ▲" else "전체 11개사 보기 ▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = GarageAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showAllInsurers = !showAllInsurers }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val displayInsurers = if (showAllInsurers) {
                            MAJOR_INSURERS
                        } else {
                            // 등록된 보험사와 일치하는 항목을 최우선 배치하거나 상위 4개
                            val matched = MAJOR_INSURERS.find { it.name.contains(car.insuranceCompany ?: "") || (car.insuranceCompany ?: "").contains(it.name) }
                            if (matched != null) {
                                listOf(matched) + MAJOR_INSURERS.filter { it != matched }.take(3)
                            } else {
                                MAJOR_INSURERS.take(4)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            displayInsurers.forEach { insurer ->
                                val isMyInsurer = car.insuranceCompany?.contains(insurer.name) == true || insurer.name.contains(car.insuranceCompany ?: "")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isMyInsurer) insurer.color.copy(alpha = 0.08f) else Color(0xFFF9FAFB),
                                    border = BorderStroke(1.dp, if (isMyInsurer) insurer.color else WarmLine.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { dial(insurer.phone) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = insurer.color.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = insurer.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = insurer.color,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = insurer.brandName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SoftInk
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = insurer.phone,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = SoftInk
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "전화걸기",
                                                tint = GarageAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. 고속도로 무료 견인 및 공공 긴급 구난
                Surface(
                    color = Color(0xFFF0FDF4), // 그린 소프트
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "고속도로 무료 긴급견인 및 공공 구조",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "고속도로에서 멈춘 경우 가장 가까운 안전지대(휴게소/영업소)까지 한국도로공사에서 무료로 견인해 드립니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { dial("1588-2504") },
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF16A34A)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF166534)),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("도로공사 무료견인\n1588-2504", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { dial("112") },
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, WarmLine),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftInk),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("경찰 112", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { dial("119") },
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, WarmLine),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SketchRed),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("구급 119", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 5. 사고 현장 대처 요령 접이식 카드
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showAccidentGuide = !showAccidentGuide }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "💡 사고 현장 4대 대처 요령 (사설 견인차 주의)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Text(
                                text = if (showAccidentGuide) "닫기 ▲" else "보기 ▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AnimatedVisibility(visible = showAccidentGuide) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("1. 비상등 켜고 가드레일 밖 안전지대로 즉시 대피 (고속도로 2차사고 방지)", style = MaterialTheme.typography.labelSmall, color = SoftInk)
                                Text("2. 현장 사진 촬영: 원거리 4방향, 바퀴 조향각도, 파손부위, 상대방 블랙박스 유무", style = MaterialTheme.typography.labelSmall, color = SoftInk)
                                Text("3. 사설 렉카(견인차) 갈고리 연결 절대 금지: 동의 없이 차를 걸지 못하게 하세요. 보험사 무료견인 또는 한국도로공사(1588-2504) 무료견인을 이용하세요.", style = MaterialTheme.typography.labelSmall, color = SketchRed, fontWeight = FontWeight.Bold)
                                Text("4. 가입 보험사에 즉시 전화하여 사고 접수 및 출동을 요청하세요.", style = MaterialTheme.typography.labelSmall, color = SoftInk)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SoftInk),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("닫기", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
