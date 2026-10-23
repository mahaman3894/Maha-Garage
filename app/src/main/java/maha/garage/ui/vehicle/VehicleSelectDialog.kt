package maha.garage.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import maha.garage.data.catalog.VehicleCatalogHelper
import maha.garage.data.catalog.VehicleCatalogItem
import maha.garage.ui.theme.*

@Composable
fun VehicleSelectDialog(
    initialBrand: String? = null,
    onDismiss: () -> Unit,
    onSelectVehicle: (VehicleCatalogItem) -> Unit
) {
    val context = LocalContext.current
    var fullCatalog by remember { mutableStateOf<List<VehicleCatalogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf(if (initialBrand.isNullOrBlank()) "전체" else initialBrand) }

    val majorBrands = listOf("전체", "현대", "기아", "제네시스", "BMW", "벤츠", "아우디", "폭스바겐", "볼보", "테슬라", "포르쉐", "쉐보레", "KG모빌리티", "르노코리아", "토요타", "렉서스", "혼다", "미니")

    LaunchedEffect(Unit) {
        fullCatalog = VehicleCatalogHelper.getCatalog(context)
        isLoading = false
    }

    val filteredList = remember(fullCatalog, searchQuery, selectedBrand) {
        val brandFilter = if (selectedBrand == "전체") null else selectedBrand
        VehicleCatalogHelper.search(fullCatalog, searchQuery, brandFilter)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 다이얼로그 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GaragePrimaryLight.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = GaragePrimaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "차량 모델 카탈로그 검색",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "국내 판매 현행/단종 제원 및 타이밍 데이터 매칭",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 검색창
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("모델명 또는 트림 검색 (예: 아반떼, 그랜저, 쏘렌토, 520d)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "지우기", tint = TextSecondaryDark)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GaragePrimaryLight,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 제조사 필터 칩 목록
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(majorBrands) { brand ->
                        val isSelected = selectedBrand == brand
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedBrand = brand },
                            label = { Text(brand) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GaragePrimaryLight.copy(alpha = 0.25f),
                                selectedLabelColor = GaragePrimaryLight,
                                containerColor = SurfaceVariantDark,
                                labelColor = TextSecondaryDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderDark)
                Spacer(modifier = Modifier.height(8.dp))

                // 검색 결과 카운트 안내
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "검색 결과: ${filteredList.size}건",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "원하는 차량을 누르면 제원이 자동 입력됩니다.",
                        style = MaterialTheme.typography.labelSmall,
                        color = GaragePrimaryLight
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 차량 목록
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GaragePrimaryLight)
                    }
                } else if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.SearchOff, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("검색된 차량 모델이 없습니다.", color = TextSecondaryDark, style = MaterialTheme.typography.bodyMedium)
                            Text("다른 검색어로 검색해보세요.", color = TextSecondaryDark.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            VehicleCatalogItemRow(
                                item = item,
                                onClick = { onSelectVehicle(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleCatalogItemRow(
    item: VehicleCatalogItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 브랜드 및 모델명, 연식
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GaragePrimaryLight.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.brand,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GaragePrimaryLight
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.modelName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceDark
                ) {
                    Text(
                        text = "${item.modelYear}년식",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
            }

            // 라인업 / 트림
            if (item.lineup.isNotBlank() || item.trim.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(item.lineup, item.trim).filter { it.isNotBlank() }.joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 배지 열: 파워트레인 배지, 타이밍 구동계 배지, 배기량/출력
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 타이밍 구동 방식 배지
                val (tLabel, tColor) = when (item.timingType) {
                    "CHAIN" -> "⛓️ 체인" to StatusNormal
                    "BELT" -> "🔄 타이밍벨트" to StatusDueSoon
                    "NA" -> "⚡ 전기구동" to StatusNormal
                    else -> "타이밍 미정" to TextSecondaryDark
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, tColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = tLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tColor
                    )
                }

                // 파워트레인 / 차종
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceDark
                ) {
                    Text(
                        text = item.powertrain,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }

                // 배기량 / 출력
                val specSummary = listOf(item.displacement, item.power).filter { it.isNotBlank() }.joinToString(" · ")
                if (specSummary.isNotBlank()) {
                    Text(
                        text = specSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
