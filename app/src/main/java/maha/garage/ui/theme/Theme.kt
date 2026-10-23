package maha.garage.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ToolYellow,
    onPrimary = SoftInk,
    primaryContainer = ToolYellowSoft,
    onPrimaryContainer = SoftInk,
    secondary = ToolOrange,
    onSecondary = SoftInk,
    secondaryContainer = ToolOrangeSoft,
    onSecondaryContainer = SoftInk,
    tertiary = JackGreen,
    onTertiary = SoftInk,
    background = PaperWhite,
    onBackground = SoftInk,
    surface = SurfaceWhite,
    onSurface = SoftInk,
    surfaceVariant = SurfaceWhiteStrong,
    onSurfaceVariant = WarmGray,
    outline = WarmLine,
    error = SketchRed,
    onError = PaperWhite
)

private val DarkColorScheme = LightColorScheme // ui.md: 차콜/다크 대신 밝은 종이 테마를 기본 단일 테마로 유지

@Composable
fun MahaGarageTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PaperWhite.toArgb()
            window.navigationBarColor = SurfaceWhiteStrong.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

