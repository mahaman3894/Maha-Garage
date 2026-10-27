package maha.garage.ui.tutorial

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import maha.garage.MahaGarageApp
import maha.garage.ui.GarageContent
import maha.garage.util.ParsedReceiptData

private val Ink = Color(0xFF152235)
private val Accent = Color(0xFFFFB52E)

class TutorialPreferences(context: Context, storeName: String = "maha_tutorial") {
    private val prefs = context.applicationContext.getSharedPreferences(storeName, Context.MODE_PRIVATE)
    val completed get() = prefs.getBoolean("is_tutorial_completed", false)
    suspend fun finish() = withContext(Dispatchers.IO) {
        check(prefs.edit().putBoolean("is_tutorial_completed", true).commit()) { "튜토리얼 완료 상태를 저장하지 못했습니다. 다시 시도해주세요." }
    }
}

@Composable
fun TutorialExperience(app: MahaGarageApp, onExit: () -> Unit) {
    var checkpoint by rememberSaveable { mutableIntStateOf(0) }
    var generation by remember { mutableIntStateOf(0) }
    key(generation) {
        TutorialRun(app, checkpoint, onCheckpoint = { checkpoint = it },
            onPrevious = { index -> checkpoint = index; generation++ }, onExit = onExit)
    }
}

@Composable
private fun TutorialRun(app: MahaGarageApp, initialIndex: Int, onCheckpoint: (Int) -> Unit,
    onPrevious: (Int) -> Unit, onExit: () -> Unit) {
    var failure by remember { mutableStateOf<String?>(null) }
    val session by produceState<TutorialSession?>(null) {
        try { value = TutorialSession.create(app, initialIndex) }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { failure = "연습 화면을 준비하지 못했습니다. ${e.message.orEmpty()}" }
    }
    val current = session
    if (current == null) {
        Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (failure == null) CircularProgressIndicator(color = Accent)
                Spacer(Modifier.height(20.dp))
                Text(failure ?: "실제 화면으로 연습할 준비를 하고 있어요", color = Color.White)
                if (failure != null) TextButton(onClick = onExit) { Text("차고로 돌아가기", color = Accent) }
            }
        }
        return
    }
    DisposableEffect(current) { onDispose { current.close() } }
    LaunchedEffect(current.index) { onCheckpoint(current.index) }
    var overlayToken by remember(current.index) { mutableStateOf<Any?>(null) }
    LaunchedEffect(current.index, current.anchors[current.cue.target]?.token, current.busy) {
        if (!current.busy) current.anchors[current.cue.target]?.let { overlayToken = it.token }
    }
    CompositionLocalProvider(LocalTutorial provides current) {
        GarageContent(app = app, tutorial = current)
        // Recreate above native app dialogs only after their target has registered.
        key(current.index, overlayToken) {
            TutorialOverlay(current, onPrevious, onExit)
        }
    }
}

/** Coordinate conversion is independent of density, navigation bars, native modal placement and aspect ratio. */
fun tutorialLocalRect(screenRect: Rect, overlayOrigin: Offset): Rect = screenRect.translate(-overlayOrigin)

@Composable
private fun TutorialOverlay(session: TutorialSession, onPrevious: (Int) -> Unit, onExit: () -> Unit) {
    val scope = rememberCoroutineScope()
    var origin by remember { mutableStateOf(Offset.Zero) }
    var height by remember { mutableFloatStateOf(1f) }
    val cue = session.cue
    val anchor = session.anchors[cue.target]
    var ready by remember { mutableStateOf(false) }
    val cursorX = remember { Animatable(35f) }
    val cursorY = remember { Animatable(100f) }
    val click = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    var pointerInitialized by remember { mutableStateOf(false) }
    val target = anchor?.let { tutorialLocalRect(it.bounds, origin) }
    suspend fun finish() {
        if (session.busy) return
        session.busy = true
        fade.animateTo(0f, tween(260))
        onExit()
    }
    suspend fun advance() {
        val actual = session.anchors[cue.target] ?: return
        if (session.busy || !ready) return
        session.busy = true
        try {
            val rect = tutorialLocalRect(actual.bounds, origin)
            cursorX.animateTo(rect.center.x, tween(220, easing = FastOutSlowInEasing))
            cursorY.animateTo(rect.center.y, tween(180, easing = FastOutSlowInEasing))
            session.pointerScreen = rect.center + origin
            click.snapTo(0f)
            click.animateTo(1f, tween(420))
            if (cue.click) actual.click()
            delay(if (cue.target == "scanner.capture") 1150 else 650)
            if (session.index == TutorialCues.lastIndex) {
                session.busy = false
                finish()
            } else {
                fade.animateTo(0f, tween(160))
                session.index++
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (e: Exception) { session.feedback = e.message ?: "시연을 다시 시도해주세요." }
        finally { session.busy = false }
    }
    Dialog(
        onDismissRequest = { scope.launch { finish() } },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
            dismissOnClickOutside = false, dismissOnBackPress = true)
    ) {
        TutorialUndimWindow()
        val view = LocalView.current
        BoxWithConstraints(Modifier.fillMaxSize().semantics { contentDescription = "마하개러지 실제 화면 튜토리얼" }
            .clickable(enabled = ready && !session.busy, indication = null, interactionSource = remember { MutableInteractionSource() }) { scope.launch { advance() } }
            .onGloballyPositioned { c ->
                val location = IntArray(2)
                view.rootView.getLocationOnScreen(location)
                origin = c.boundsInWindow().topLeft + Offset(location[0].toFloat(), location[1].toFloat())
                height = c.size.height.toFloat()
            }) {
            val compact = maxHeight < 430.dp
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val panelHeight = if (compact) 200.dp else 270.dp
            val navigationBottom = WindowInsets.navigationBars.getBottom(density)
            val panelBottom = height - navigationBottom - with(density) { 36.dp.toPx() }
            val panelWidth = minOf(widthPx - with(density) { 32.dp.toPx() }, with(density) { 540.dp.toPx() })
            val defaultPanel = Rect((widthPx - panelWidth) / 2, panelBottom - with(density) { panelHeight.toPx() },
                (widthPx + panelWidth) / 2, panelBottom)
            val cardAtTop = target?.inflate(with(density) { 8.dp.toPx() })?.overlaps(defaultPanel) == true
            val cardAlignment = if (cardAtTop) Alignment.TopCenter else Alignment.BottomCenter
            LaunchedEffect(anchor?.token, origin, target?.center) {
                if (anchor == null) return@LaunchedEffect
                ready = false
                try { anchor.reveal() } catch (_: IllegalStateException) { }
                delay(180)
                val latest = session.anchors[cue.target] ?: return@LaunchedEffect
                val rect = tutorialLocalRect(latest.bounds, origin)
                if (!pointerInitialized) {
                    session.pointerScreen?.let { previous ->
                        cursorX.snapTo((previous.x - origin.x).coerceIn(0f, widthPx))
                        cursorY.snapTo((previous.y - origin.y).coerceIn(0f, height))
                    }
                    pointerInitialized = true
                }
                fade.animateTo(1f, tween(300))
                launch { cursorX.animateTo(rect.center.x, tween(800, easing = FastOutSlowInEasing)) }
                cursorY.animateTo(rect.center.y, tween(800, easing = FastOutSlowInEasing))
                session.pointerScreen = rect.center + origin
                ready = true
            }
            Canvas(Modifier.fillMaxSize()) {
                val hole = target?.inflate(7.dp.toPx())
                val scrim = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(Offset.Zero, size))
                    if (hole != null) addRoundRect(androidx.compose.ui.geometry.RoundRect(hole, CornerRadius(15.dp.toPx())))
                }
                drawPath(scrim, Color(0xFF07101E).copy(alpha = .76f * fade.value))
                if (hole != null) {
                    // Soft outer focus halo; the actual control remains unobscured and sharp.
                    for (n in 5 downTo 1) {
                        val halo = hole.inflate(n * 3.dp.toPx())
                        drawRoundRect(Accent.copy(alpha = .025f * fade.value), halo.topLeft, halo.size,
                            CornerRadius(18.dp.toPx()), style = Stroke(7.dp.toPx()))
                    }
                    drawRoundRect(Accent.copy(alpha = fade.value), hole.topLeft, hole.size,
                        CornerRadius(15.dp.toPx()), style = Stroke(2.dp.toPx()))
                    if (click.value > 0 && click.value < 1) {
                        drawCircle(Accent.copy(alpha = (1 - click.value) * .8f),
                            (9 + 31 * click.value).dp.toPx(), Offset(cursorX.value, cursorY.value), style = Stroke(2.dp.toPx()))
                    }
                    val pointer = Path().apply {
                        moveTo(0f,0f); lineTo(0f,25.dp.toPx()); lineTo(6.dp.toPx(),19.dp.toPx())
                        lineTo(12.dp.toPx(),32.dp.toPx()); lineTo(17.dp.toPx(),29.dp.toPx())
                        lineTo(11.dp.toPx(),17.dp.toPx()); lineTo(24.dp.toPx(),17.dp.toPx()); close()
                    }
                    translate(cursorX.value, cursorY.value) {
                        drawPath(pointer, Ink.copy(alpha = fade.value), style = Stroke(3.dp.toPx(), join = StrokeJoin.Round))
                        drawPath(pointer, Color.White.copy(alpha = fade.value))
                    }
                }
            }
            Surface(modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp)
                .clickable(enabled = ready && !session.busy, indication = null, interactionSource = remember { MutableInteractionSource() }) { scope.launch { advance() } },
                color = Ink.copy(alpha = .95f), shape = RoundedCornerShape(50)) {
                Text("연습 모드 · 실제 데이터 저장 안 함", color = Color.White,
                    fontSize = 11.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
            }
            IconButton(onClick = { scope.launch { finish() } }, enabled = !session.busy,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Default.Close, "튜토리얼 종료", tint = Color.White)
            }
            Surface(
                modifier = Modifier.align(cardAlignment)
                    .then(if (cardAtTop) Modifier.statusBarsPadding().padding(top = 56.dp) else Modifier.navigationBarsPadding().padding(bottom = 24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = 540.dp).fillMaxWidth().height(panelHeight)
                    .testTag("tutorial-cue-${cue.target}")
                    .clickable(enabled = ready && !session.busy, indication = null, interactionSource = remember { MutableInteractionSource() }) { scope.launch { advance() } },
                shape = RoundedCornerShape(23.dp), color = Color(0xFFF9FBFF), shadowElevation = 18.dp
            ) {
                Column(Modifier.padding(if (compact) 12.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("STEP ${cue.chapter} / 5", color = Color(0xFFB06B00), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${session.index + 1} / ${TutorialCues.size}", color = Ink.copy(alpha = .5f), fontSize = 11.sp)
                    }
                    LinearProgressIndicator(progress = { (session.index + 1f) / TutorialCues.size },
                        modifier = Modifier.fillMaxWidth().height(3.dp), color = Accent, trackColor = Color(0xFFE8EDF4))
                    Text(cue.title, color = Ink, fontSize = if (compact) 16.sp else 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text(cue.body, color = Ink.copy(alpha = .75f), fontSize = 12.sp, lineHeight = 18.sp)
                        session.feedback?.let { Text(it, color = Color(0xFF986100), fontSize = 11.sp) }
                        if (!ready) Text("실제 화면의 위치를 확인하고 있어요…", color = Ink.copy(alpha = .5f), fontSize = 10.sp)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onPrevious(session.index - 1) }, enabled = session.index > 0 && !session.busy,
                            modifier = Modifier.widthIn(min = 40.dp), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("이전", color = Ink, fontSize = 13.sp) }
                        TextButton(onClick = { scope.launch { finish() } }, enabled = !session.busy,
                            modifier = Modifier.widthIn(min = 40.dp), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("건너뛰기", color = Ink.copy(alpha = .6f), fontSize = 13.sp) }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { scope.launch { advance() } }, enabled = ready && !session.busy, modifier = Modifier.testTag("tutorial-next"),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEFA), contentColor = Ink,
                                disabledContainerColor = Color(0xFF87CEFA).copy(alpha = .55f), disabledContentColor = Ink.copy(alpha = .5f)), shape = RoundedCornerShape(12.dp)) {
                            Text(if (session.index == TutorialCues.lastIndex) "완료" else if (cue.click) "시연 · 다음" else "다음", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Camera UI replacement is reached before permission launchers or CameraX can be created. */
@Composable
fun TutorialReceiptDemo(onDataParsed: (ParsedReceiptData) -> Unit, onDismiss: () -> Unit) {
    var scanned by remember { mutableStateOf(false) }
    val scan by animateFloatAsState(if (scanned) 1f else 0f, tween(780), label = "receipt-scan")
    LaunchedEffect(scanned) {
        if (scanned) { delay(900); onDataParsed(ParsedReceiptData(50.0,1700,85000,"예시 주유소")) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.93f), color = Ink, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.heightIn(max = 580.dp).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("영수증 OCR · 연습 촬영", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Box {
                Surface(color = Color.White, shape = RoundedCornerShape(6.dp)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("MAHA 예시 주유소", fontWeight = FontWeight.ExtraBold)
                        Text("주유량                 50.00 L")
                        Text("단가                    1,700원")
                        HorizontalDivider()
                        Text("합계                   85,000원", fontWeight = FontWeight.Bold)
                        Text("튜토리얼 전용 영수증", color = Color.Gray, fontSize = 11.sp)
                    }
                }
                if (scanned) Canvas(Modifier.matchParentSize()) {
                    val y = size.height * scan
                    drawLine(Accent, Offset(0f,y), Offset(size.width,y), strokeWidth = 3.dp.toPx())
                    drawRect(Accent.copy(alpha = .08f), size = androidx.compose.ui.geometry.Size(size.width,y))
                }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { scanned = true },
                    modifier = Modifier.tutorialTarget("scanner.capture") { scanned = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                    Icon(Icons.Default.CameraAlt, null, tint = Ink)
                    Spacer(Modifier.width(8.dp)); Text(if (scanned) "인식 완료" else "촬영 · 자동 인식", color = Ink)
                }
            }
        }
    }
}
