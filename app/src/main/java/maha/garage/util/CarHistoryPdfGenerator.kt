package maha.garage.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import maha.garage.data.local.dao.ServiceVisitWithLines
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.model.enums.ActionType
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CarHistoryPdfGenerator {

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var low = 0
        var high = text.length
        var result = text
        while (low <= high) {
            val mid = (low + high) / 2
            val candidate = text.substring(0, mid) + "…"
            if (paint.measureText(candidate) <= maxWidth) {
                result = candidate
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    /**
     * 표준 A4 (595 x 842 포인트) 규격으로 카 히스토리 정비 이력 리포트 PDF 생성 (비동기 IO)
     */
    suspend fun generatePdf(
        context: Context,
        car: CarEntity,
        periodText: String,
        visits: List<ServiceVisitWithLines>,
        isSummaryOnly: Boolean = false
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 30, 45)
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.rgb(100, 110, 125)
            textSize = 8.5f
            isAntiAlias = true
        }

        val sectionTitlePaint = Paint().apply {
            color = Color.rgb(25, 40, 65)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(40, 45, 55)
            textSize = 8.5f
            isAntiAlias = true
        }

        val subTextPaint = Paint().apply {
            color = Color.rgb(110, 120, 135)
            textSize = 8f
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(20, 25, 35)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rightAlignPaint = Paint().apply {
            color = Color.rgb(20, 25, 35)
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(240, 243, 248)
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(215, 222, 232)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        val primaryBarPaint = Paint().apply {
            color = Color.rgb(0, 150, 180)
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var yPos = margin

        // 1. 헤더 액센트 바 & 문서 제목
        canvas.drawRoundRect(RectF(margin, yPos, margin + 4f, yPos + 26f), 2f, 2f, primaryBarPaint)
        canvas.drawText("MAHA GARAGE · 차량 정비 이력 보고서 (Car History)", margin + 10f, yPos + 15f, titlePaint)
        canvas.drawText("출력 일시: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}  |  관리 시스템: 마하개러지 차계부", margin + 10f, yPos + 26f, subTitlePaint)
        yPos += 38f

        // 2. 차량 정보 & 조회 요약 테이블 박스
        val infoBoxTop = yPos
        val infoBoxHeight = 64f
        val boxRect = RectF(margin, infoBoxTop, pageWidth - margin, infoBoxTop + infoBoxHeight)
        canvas.drawRoundRect(boxRect, 6f, 6f, tableHeaderPaint)
        canvas.drawRoundRect(boxRect, 6f, 6f, borderPaint)

        // 차량 정보 텍스트 (임의 가짜값 제거, 미등록 시 명확히 표기)
        canvas.drawText("차량명", margin + 12f, infoBoxTop + 16f, subTitlePaint)
        canvas.drawText("${car.make} ${car.modelName}", margin + 12f, infoBoxTop + 30f, boldTextPaint)
        canvas.drawText("차량번호: ${car.carNumber}  |  연식: ${car.modelYear}년식  |  누적주행거리: %,d km".format(car.currentMileage), margin + 12f, infoBoxTop + 45f, textPaint)
        val insuranceText = car.insuranceCompany?.let { "보험사: $it" } ?: "보험사: 미등록"
        val inspectionText = car.inspectionEndDate?.let { "정기검사 만료: $it" } ?: "정기검사: 미등록"
        canvas.drawText("$insuranceText  |  $inspectionText", margin + 12f, infoBoxTop + 57f, textPaint)

        // 우측 요약 통계
        val totalCost = visits.sumOf { it.visit.totalCost }
        val rightX = pageWidth - margin - 12f
        canvas.drawText("조회 기간: $periodText", rightX, infoBoxTop + 16f, rightAlignPaint)
        canvas.drawText("총 정비 횟수: ${visits.size}회", rightX, infoBoxTop + 31f, rightAlignPaint)
        val costSummaryPaint = Paint().apply {
            color = Color.rgb(0, 120, 160)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("기록된 비용 합계: %,d 원".format(totalCost), rightX, infoBoxTop + 50f, costSummaryPaint)
        yPos += infoBoxHeight + 14f

        // 3. 정비 내역 테이블 헤더
        val headerTitle = if (isSummaryOnly) "■ 주요 교체 & 정비 내역 요약 (최근 12건)" else "■ 상세 교체 & 정비 내역 (전체 ${visits.size}건)"
        canvas.drawText(headerTitle, margin, yPos + 10f, sectionTitlePaint)
        yPos += 16f

        // 컬럼 정의: 날짜(65pt) | 주행거리(60pt) | 작업 내용(245pt) | 정비소(65pt) | 비용(50pt)
        val colDate = margin + 5f
        val colMileage = margin + 68f
        val colDesc = margin + 135f
        val colWorkshop = margin + 382f
        val colCost = pageWidth - margin - 6f

        val headerTextPaint = Paint().apply {
            color = Color.rgb(60, 70, 85)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        fun drawTableHeader(currentY: Float) {
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 18f, tableHeaderPaint)
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 18f, borderPaint)
            canvas.drawText("날짜", colDate, currentY + 13f, headerTextPaint)
            canvas.drawText("주행거리", colMileage, currentY + 13f, headerTextPaint)
            canvas.drawText("작업 내용 및 규격", colDesc, currentY + 13f, headerTextPaint)
            canvas.drawText("정비소", colWorkshop, currentY + 13f, headerTextPaint)
            canvas.drawText("비용(원)", colCost, currentY + 13f, Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT })
        }

        drawTableHeader(yPos)
        yPos += 18f

        fun checkNewPage(neededHeight: Float) {
            if (yPos + neededHeight > pageHeight - margin - 18f) {
                // 페이지 번호 출력
                canvas.drawText("- $pageNumber -", pageWidth / 2f, pageHeight - margin + 6f, Paint().apply {
                    color = Color.GRAY
                    textSize = 8.5f
                    textAlign = Paint.Align.CENTER
                })
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin

                drawTableHeader(yPos)
                yPos += 18f
            }
        }

        var globalRowIndex = 0

        if (isSummaryOnly) {
            // 한 장 요약 모드: 최근 12건 한 줄씩 축약 출력
            val visitsToPrint = visits.take(12)
            for (item in visitsToPrint) {
                val rowHeight = 22f
                checkNewPage(rowHeight)

                if (globalRowIndex % 2 == 1) {
                    canvas.drawRect(margin, yPos, pageWidth - margin, yPos + rowHeight, Paint().apply {
                        color = Color.rgb(250, 252, 255)
                        style = Paint.Style.FILL
                    })
                }
                canvas.drawRect(margin, yPos, pageWidth - margin, yPos + rowHeight, borderPaint)

                val v = item.visit
                val lines = item.lines

                canvas.drawText(v.serviceDate, colDate, yPos + 14f, textPaint)
                canvas.drawText("%,d km".format(v.odometerKm), colMileage, yPos + 14f, textPaint)

                val workSummary = if (lines.isNotEmpty()) {
                    val first = lines.first()
                    val itemName = maha.garage.data.seed.DefaultCatalogData.items.find { it.itemCode == first.itemCode }?.displayName ?: first.itemCode
                    val actionName = when (first.actionType) {
                        ActionType.REPLACE -> "교체"
                        ActionType.INSPECT -> "점검"
                        ActionType.REFILL -> "보충"
                        ActionType.CLEAN -> "클리닝"
                        ActionType.ROTATE -> "위치교환"
                        ActionType.SWAP -> "스왑"
                    }
                    val spec = first.partSpecification?.let { "($it)" } ?: ""
                    val lineDesc = "$itemName $actionName $spec".trim()
                    if (lines.size > 1) "$lineDesc 외 ${lines.size - 1}건" else lineDesc
                } else {
                    v.notes ?: "일반 정비 점검"
                }
                val truncatedDesc = truncateText(workSummary, boldTextPaint, 240f)
                canvas.drawText(truncatedDesc, colDesc, yPos + 14f, boldTextPaint)

                val workshopText = truncateText(v.workshop ?: "-", textPaint, 60f)
                canvas.drawText(workshopText, colWorkshop, yPos + 14f, textPaint)

                canvas.drawText("%,d".format(v.totalCost), colCost, yPos + 14f, rightAlignPaint)

                yPos += rowHeight
                globalRowIndex++
            }
        } else {
            // 전체 정비 이력 모드: 모든 작업 세부내역(lines)을 누락 없이 행별로 전개 출력
            for (item in visits) {
                val v = item.visit
                val lines = item.lines

                if (lines.isEmpty()) {
                    val rowHeight = 22f
                    checkNewPage(rowHeight)

                    if (globalRowIndex % 2 == 1) {
                        canvas.drawRect(margin, yPos, pageWidth - margin, yPos + rowHeight, Paint().apply {
                            color = Color.rgb(250, 252, 255)
                            style = Paint.Style.FILL
                        })
                    }
                    canvas.drawRect(margin, yPos, pageWidth - margin, yPos + rowHeight, borderPaint)

                    canvas.drawText(v.serviceDate, colDate, yPos + 14f, textPaint)
                    canvas.drawText("%,d km".format(v.odometerKm), colMileage, yPos + 14f, textPaint)

                    val desc = truncateText(v.notes ?: "일반 정비 점검", boldTextPaint, 240f)
                    canvas.drawText(desc, colDesc, yPos + 14f, boldTextPaint)

                    val workshopText = truncateText(v.workshop ?: "-", textPaint, 60f)
                    canvas.drawText(workshopText, colWorkshop, yPos + 14f, textPaint)

                    canvas.drawText("%,d".format(v.totalCost), colCost, yPos + 14f, rightAlignPaint)

                    yPos += rowHeight
                    globalRowIndex++
                } else {
                    for ((lineIdx, line) in lines.withIndex()) {
                        val rowHeight = 21f
                        checkNewPage(rowHeight)

                        if (globalRowIndex % 2 == 1) {
                            canvas.drawRect(margin, yPos, pageWidth - margin, yPos + rowHeight, Paint().apply {
                                color = Color.rgb(250, 252, 255)
                                style = Paint.Style.FILL
                            })
                        }
                        canvas.drawRect(margin, yPos, pageWidth - margin, yPos + rowHeight, borderPaint)

                        if (lineIdx == 0) {
                            canvas.drawText(v.serviceDate, colDate, yPos + 14f, textPaint)
                            canvas.drawText("%,d km".format(v.odometerKm), colMileage, yPos + 14f, textPaint)
                            val workshopText = truncateText(v.workshop ?: "-", textPaint, 60f)
                            canvas.drawText(workshopText, colWorkshop, yPos + 14f, textPaint)
                        } else {
                            canvas.drawText(" ↳", colDate + 6f, yPos + 13f, subTextPaint)
                        }

                        val itemName = maha.garage.data.seed.DefaultCatalogData.items.find { it.itemCode == line.itemCode }?.displayName ?: line.itemCode
                        val actionName = when (line.actionType) {
                            ActionType.REPLACE -> "교체"
                            ActionType.INSPECT -> "점검"
                            ActionType.REFILL -> "보충"
                            ActionType.CLEAN -> "클리닝"
                            ActionType.ROTATE -> "위치교환"
                            ActionType.SWAP -> "스왑"
                        }
                        val spec = line.partSpecification?.let { " [$it]" } ?: ""
                        val lineCost = line.partsCost + line.laborCost
                        val lineDesc = "$itemName $actionName$spec".trim()
                        val truncatedDesc = truncateText(lineDesc, boldTextPaint, 240f)
                        canvas.drawText(truncatedDesc, colDesc, yPos + 14f, boldTextPaint)

                        val hasIndividualLineCosts = lines.any { (it.partsCost + it.laborCost) > 0 }
                        val costToDisplay = if (hasIndividualLineCosts) {
                            lineCost
                        } else {
                            if (lineIdx == 0) v.totalCost else 0
                        }

                        if (costToDisplay > 0) {
                            canvas.drawText("%,d".format(costToDisplay), colCost, yPos + 14f, rightAlignPaint)
                        } else {
                            canvas.drawText("-", colCost, yPos + 14f, rightAlignPaint)
                        }

                        yPos += rowHeight
                        globalRowIndex++
                    }
                }
            }
        }

        // 페이지 번호 출력
        canvas.drawText("- $pageNumber -", pageWidth / 2f, pageHeight - margin + 6f, Paint().apply {
            color = Color.GRAY
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        })
        pdfDocument.finishPage(page)

        // 파일 쓰기
        val fileName = "CarHistory_${car.carNumber.replace(" ", "")}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.pdf"
        val pdfFile = File(context.cacheDir, fileName)
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        pdfFile
    }

    /**
     * 안드로이드 시스템 공유 시트(Chooser) 호출
     */
    fun sharePdf(context: Context, car: CarEntity, pdfFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "[마하개러지] ${car.make} ${car.modelName} (${car.carNumber}) 정비 이력 리포트")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "카 히스토리 PDF 보내기")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * 호환용 단일 동기/비동기 호출 헬퍼
     */
    suspend fun generateAndSharePdf(
        context: Context,
        car: CarEntity,
        periodText: String,
        visits: List<ServiceVisitWithLines>,
        isSummaryOnly: Boolean = false
    ): File {
        val file = generatePdf(context, car, periodText, visits, isSummaryOnly)
        sharePdf(context, car, file)
        return file
    }
}
