package maha.garage.ui.vehicle

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import maha.garage.ui.theme.*
import maha.garage.util.OdometerParser

/**
 * 차량 계기판(ODO) 전용 고성능 온디바이스 OCR 스캐너 모달 다이얼로그
 */
@Composable
fun OdometerScannerDialog(
    onOdometerParsed: (Int) -> Unit,
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
            Toast.makeText(context, "계기판 촬영을 위해 카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
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
                OdometerCameraContent(
                    onOdometerParsed = onOdometerParsed,
                    onDismiss = onDismiss
                )
            } else {
                PermissionRationaleContent(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun OdometerCameraContent(
    onOdometerParsed: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("계기판의 누적 주행거리(ODO)를 사각 프레임에 맞춰 촬영하세요") }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControlInstance by remember { mutableStateOf<CameraControl?>(null) }

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
            statusMessage = "앨범 계기판 사진 분석 중..."
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val parsedKm = OdometerParser.parse(visionText.text)
                        isProcessing = false
                        if (parsedKm != null) {
                            Toast.makeText(context, "계기판 주행거리 %,d km가 인식되었습니다.".format(parsedKm), Toast.LENGTH_SHORT).show()
                            onOdometerParsed(parsedKm)
                        } else {
                            statusMessage = "주행거리(ODO) 숫자를 찾지 못했습니다. 다시 촬영해 주세요."
                            Toast.makeText(context, "주행거리 인식 실패: 숫자가 선명하지 않습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        isProcessing = false
                        statusMessage = "인식에 실패했습니다. 다시 시도해 주세요."
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
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        cameraControlInstance = camera.cameraControl
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. 가이드 뷰파인더 오버레이 (계기판 누적거리 전용 가로형 프레임)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxWidth = size.width * 0.88f
            val boxHeight = size.height * 0.28f // 계기판 ODO 디스플레이에 최적화된 가로 프레임
            val left = (size.width - boxWidth) / 2f
            val top = (size.height - boxHeight) / 2.3f

            // 주변 어두운 오버레이
            drawRect(color = Color(0x99000000))

            // 중앙 투명 컷아웃
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // 타겟 프레임 네온 앰버/오렌지 테두리
            drawRoundRect(
                color = ToolOrange,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 4개 모서리 포인트 강조 라인
            val cornerLen = 24.dp.toPx()
            val strokeW = 4.dp.toPx()
            val c = Color(0xFFFFD54F)
            // 좌상단
            drawLine(c, Offset(left, top + 8.dp.toPx()), Offset(left + cornerLen, top + 8.dp.toPx()), strokeW)
            drawLine(c, Offset(left + 8.dp.toPx(), top), Offset(left + 8.dp.toPx(), top + cornerLen), strokeW)
            // 우상단
            drawLine(c, Offset(left + boxWidth - cornerLen, top + 8.dp.toPx()), Offset(left + boxWidth, top + 8.dp.toPx()), strokeW)
            drawLine(c, Offset(left + boxWidth - 8.dp.toPx(), top), Offset(left + boxWidth - 8.dp.toPx(), top + cornerLen), strokeW)
            // 좌하단
            drawLine(c, Offset(left, top + boxHeight - 8.dp.toPx()), Offset(left + cornerLen, top + boxHeight - 8.dp.toPx()), strokeW)
            drawLine(c, Offset(left + 8.dp.toPx(), top + boxHeight - cornerLen), Offset(left + 8.dp.toPx(), top + boxHeight), strokeW)
            // 우하단
            drawLine(c, Offset(left + boxWidth - cornerLen, top + boxHeight - 8.dp.toPx()), Offset(left + boxWidth, top + boxHeight - 8.dp.toPx()), strokeW)
            drawLine(c, Offset(left + boxWidth - 8.dp.toPx(), top + boxHeight - cornerLen), Offset(left + boxWidth - 8.dp.toPx(), top + boxHeight), strokeW)
        }

        // 3. 상단 툴바
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
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(42.dp)
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
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ToolOrange.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = ToolOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "계기판 ODO OCR 스캔",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (isTorchOn) ToolOrange.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(
                    onClick = {
                        val next = !isTorchOn
                        isTorchOn = next
                        cameraControlInstance?.enableTorch(next)
                    }
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "플래시 토글",
                        tint = if (isTorchOn) Color.White else Color(0xFFCBD5E1)
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
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = statusMessage,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 셔터 버튼 및 앨범 버튼 행
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 앨범 불러오기
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        enabled = !isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "앨범에서 계기판 사진 선택",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 카메라 렌즈 스타일 셔터 버튼
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
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
                            statusMessage = "계기판 주행거리 분석 중..."

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
                                                    val parsedKm = OdometerParser.parse(visionText.text)
                                                    imageProxy.close()
                                                    isProcessing = false
                                                    if (parsedKm != null) {
                                                        Toast.makeText(context, "계기판 주행거리 %,d km가 인식되었습니다.".format(parsedKm), Toast.LENGTH_SHORT).show()
                                                        onOdometerParsed(parsedKm)
                                                    } else {
                                                        statusMessage = "주행거리 숫자를 찾지 못했습니다. 계기판을 정면에서 촬영해 주세요."
                                                        Toast.makeText(context, "주행거리 인식 실패: 숫자가 흐리거나 잘렸습니다.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    imageProxy.close()
                                                    isProcessing = false
                                                    statusMessage = "텍스트 인식 오류가 발생했습니다."
                                                    Toast.makeText(context, "인식 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            imageProxy.close()
                                            isProcessing = false
                                            statusMessage = "이미지를 가져올 수 없습니다."
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isProcessing = false
                                        statusMessage = "촬영 실패: ${exception.message}"
                                    }
                                }
                            )
                        }
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = ToolOrange,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = ToolOrange,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "촬영하기",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // 우측 균형용 빈 공간
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun PermissionRationaleContent(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = ToolOrange.copy(alpha = 0.15f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = ToolOrange,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "계기판 촬영 카메라 권한 필요",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "계기판의 누적 주행거리(ODO)를 OCR로 자동 인식하려면 카메라 접근 권한이 필요합니다.",
            color = Color(0xFF94A3B8),
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ToolOrange)
        ) {
            Text("카메라 권한 허용", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onDismiss) {
            Text("닫기", color = Color(0xFF64748B))
        }
    }
}
