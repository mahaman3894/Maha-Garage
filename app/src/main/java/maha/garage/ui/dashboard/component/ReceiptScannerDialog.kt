package maha.garage.ui.dashboard.component

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import maha.garage.ui.theme.*
import maha.garage.util.ParsedReceiptData
import maha.garage.util.ReceiptParser

@Composable
fun ReceiptScannerDialog(
    onDataParsed: (ParsedReceiptData) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "영수증 촬영을 위해 카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                CameraScannerContent(
                    onDataParsed = onDataParsed,
                    onDismiss = onDismiss
                )
            } else {
                // 권한 요청 안내 화면
                PermissionRationaleContent(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun CameraScannerContent(
    onDataParsed: (ParsedReceiptData) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("영수증을 사각 프레임 안에 맞춘 후 촬영하세요") }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val recognizer = remember {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    // 갤러리 이미지 선택 기능 (보조)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && !isProcessing) {
            isProcessing = true
            statusMessage = "앨범 영수증 분석 중..."
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val parsed = ReceiptParser.parse(visionText.text)
                        isProcessing = false
                        if (parsed.fuelAmount != null || parsed.totalPrice != null) {
                            val msg = buildString {
                                append("영수증 인식 완료: ")
                                parsed.fuelAmount?.let { append("%.2fL ".format(it)) }
                                parsed.totalPrice?.let { append("%,d원 ".format(it)) }
                                parsed.gasStationName?.let { append("($it)") }
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onDataParsed(parsed)
                        } else {
                            statusMessage = "영수증에서 주유량/금액을 찾지 못했습니다.\n수량과 결제금액이 선명하게 보이도록 다시 선택해 주세요."
                            Toast.makeText(context, "주유량 또는 금액 인식 실패: 영수증 사진을 확인해 주세요.", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        isProcessing = false
                        statusMessage = "인식에 실패했습니다. 다시 시도해주세요."
                        Toast.makeText(context, "텍스트 인식 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                isProcessing = false
                statusMessage = "이미지를 불러올 수 없습니다."
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. CameraX PreviewView
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. 가이드 뷰파인더 오버레이 (반투명 마스크 + 사각 타겟팅 박스)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxWidth = size.width * 0.85f
            val boxHeight = size.height * 0.58f
            val left = (size.width - boxWidth) / 2f
            val top = (size.height - boxHeight) / 2.6f

            // 주변 어두운 오버레이
            drawRect(color = Color(0x99000000))

            // 중앙 투명 컷아웃
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(20.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // 타겟 프레임 네온 오렌지 테두리
            drawRoundRect(
                color = Color(0xFFFF9800),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 4개 모서리 포인트 강조 라인
            val cornerLen = 28.dp.toPx()
            val strokeW = 4.dp.toPx()
            // 좌상단
            drawLine(Color.White, Offset(left, top + 10.dp.toPx()), Offset(left + cornerLen, top + 10.dp.toPx()), strokeW)
            drawLine(Color.White, Offset(left + 10.dp.toPx(), top), Offset(left + 10.dp.toPx(), top + cornerLen), strokeW)
            // 우상단
            drawLine(Color.White, Offset(left + boxWidth - cornerLen, top + 10.dp.toPx()), Offset(left + boxWidth, top + 10.dp.toPx()), strokeW)
            drawLine(Color.White, Offset(left + boxWidth - 10.dp.toPx(), top), Offset(left + boxWidth - 10.dp.toPx(), top + cornerLen), strokeW)
            // 좌하단
            drawLine(Color.White, Offset(left, top + boxHeight - 10.dp.toPx()), Offset(left + cornerLen, top + boxHeight - 10.dp.toPx()), strokeW)
            drawLine(Color.White, Offset(left + 10.dp.toPx(), top + boxHeight - cornerLen), Offset(left + 10.dp.toPx(), top + boxHeight), strokeW)
            // 우하단
            drawLine(Color.White, Offset(left + boxWidth - cornerLen, top + boxHeight - 10.dp.toPx()), Offset(left + boxWidth, top + boxHeight - 10.dp.toPx()), strokeW)
            drawLine(Color.White, Offset(left + boxWidth - 10.dp.toPx(), top + boxHeight - cornerLen), Offset(left + boxWidth - 10.dp.toPx(), top + boxHeight), strokeW)
        }

        // 3. 상단 툴바 (닫기 버튼 & 타이틀)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ToolOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "영수증 OCR 자동인식",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 앨범에서 선택 버튼
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "앨범에서 선택",
                        tint = Color.White
                    )
                }
            }
        }

        // 4. 하단 셔터 및 컨트롤 바
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 안내 텍스트
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = statusMessage,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 카메라 렌즈 스타일 셔터 버튼
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF334155),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                ToolOrange,
                                Color(0xFFFFD54F),
                                ToolOrange,
                                Color(0xFFE65100),
                                ToolOrange
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable(enabled = !isProcessing) {
                        if (isProcessing) return@clickable
                        isProcessing = true
                        statusMessage = "영수증 텍스트 분석 중..."

                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                @androidx.annotation.OptIn(ExperimentalGetImage::class)
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val inputImage = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        recognizer.process(inputImage)
                                            .addOnSuccessListener { visionText ->
                                                val parsed = ReceiptParser.parse(visionText.text)
                                                imageProxy.close()
                                                isProcessing = false
                                                if (parsed.fuelAmount != null || parsed.totalPrice != null) {
                                                    val msg = buildString {
                                                        append("영수증 인식 완료: ")
                                                        parsed.fuelAmount?.let { append("%.2fL ".format(it)) }
                                                        parsed.totalPrice?.let { append("%,d원 ".format(it)) }
                                                        parsed.gasStationName?.let { append("($it)") }
                                                    }
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    onDataParsed(parsed)
                                                } else {
                                                    statusMessage = "영수증에서 주유량/금액을 찾지 못했습니다.\n수량과 결제금액이 프레임 안에 들어오도록 다시 촬영해 주세요."
                                                    Toast.makeText(context, "주유량 또는 금액 인식 실패: 영수증을 프레임에 맞춰 다시 촬영해 주세요.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                imageProxy.close()
                                                isProcessing = false
                                                statusMessage = "인식 실패: 다시 시도해주세요"
                                                Toast.makeText(
                                                    context,
                                                    "영수증 인식 실패: ${e.localizedMessage}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else {
                                        imageProxy.close()
                                        isProcessing = false
                                        statusMessage = "촬영 이미지를 가져올 수 없습니다"
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isProcessing = false
                                    statusMessage = "촬영 오류: 다시 시도해주세요"
                                    Toast.makeText(
                                        context,
                                        "촬영 실패: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
            ) {
                // 내부 렌즈 조리개 디테일
                Surface(
                    shape = CircleShape,
                    color = if (isProcessing) ToolOrange.copy(alpha = 0.3f) else Color.White,
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = ToolOrange,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "영수증 촬영",
                                tint = SoftInk,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRationaleContent(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = ToolYellowSoft,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ToolOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "카메라 권한 필요",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftInk
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "주유 영수증을 자동으로 인식하여 리터, 단가, 총금액을 등록하려면 카메라 접근 권한이 필요합니다.",
                    fontSize = 13.sp,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("권한 허용하기", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("취소", color = WarmGray)
                }
            }
        }
    }
}
