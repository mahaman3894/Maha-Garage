package maha.garage.ui

import android.net.Uri
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import maha.garage.R

/**
 * 앱 실행 시 표시되는 웰컴 애니메이션 화면.
 *
 * 1. 동영상 배경색(#FAFAFA)과 일치시켜 세로 화면에서도 상하 여백 없이 자연스럽게 표시
 * 2. 화면 어디든 탭/터치 또는 뒤로가기 누르면 즉시 메인 화면으로 전환 (버튼 없이 순수 터치로 동작)
 * 3. 영상 재생 완료 시 자동으로 메인 화면으로 전환
 * 4. 재생 에러 발생 시 즉시 메인 화면으로 전환하여 사용자 대기 방지
 */
@Composable
fun WelcomeAnimationScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    val context = LocalContext.current
    val videoViewRef = remember { mutableStateOf<VideoView?>(null) }

    BackHandler {
        currentOnDismiss.value.invoke()
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef.value?.stopPlayback()
            videoViewRef.value = null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        currentOnDismiss.value.invoke()
                    },
                    onTap = {
                        currentOnDismiss.value.invoke()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val screenRatio = maxWidth.value / maxHeight.value
        val videoAspectRatio = 16f / 9f

        val videoModifier = if (screenRatio > videoAspectRatio) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(videoAspectRatio)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspectRatio)
        }

        Box(
            modifier = videoModifier,
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoViewRef.value = this
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        val videoUri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.welcome_animation}")
                        setVideoURI(videoUri)

                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            start()
                        }

                        setOnCompletionListener {
                            currentOnDismiss.value.invoke()
                        }

                        setOnErrorListener { _, _, _ ->
                            currentOnDismiss.value.invoke()
                            true
                        }

                        setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_DOWN) {
                                currentOnDismiss.value.invoke()
                            }
                            true
                        }
                    }
                }
            )
        }
    }
}
