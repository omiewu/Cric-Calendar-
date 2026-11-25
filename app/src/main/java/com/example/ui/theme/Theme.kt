package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class CricketFormatColors(
    val testBg: Color,
    val testFg: Color,
    val odiBg: Color,
    val odiFg: Color,
    val t20Bg: Color,
    val t20Fg: Color,
    val liveDot: Color,
    val liveBannerBg: Color,
    val countdownBg: Color,
    val countdownFg: Color
)

val LocalCricketColors = staticCompositionLocalOf {
    CricketFormatColors(
        testBg = TestCrimsonContainerLight,
        testFg = TestCrimsonLight,
        odiBg = OdiCobaltContainerLight,
        odiFg = OdiCobaltLight,
        t20Bg = T20AmberContainerLight,
        t20Fg = T20AmberLight,
        liveDot = LivePulseRed,
        liveBannerBg = Color(0xFF450A0A),
        countdownBg = EmeraldMintContainer,
        countdownFg = EmeraldPitchDark
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPitch,
    onPrimary = Color.White,
    primaryContainer = EmeraldMintContainer,
    onPrimaryContainer = EmeraldPitchDark,
    secondary = Color(0xFF335C52),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EAE4),
    onSecondaryContainer = Color(0xFF10332B),
    tertiary = TrophyGold,
    onTertiary = Color.White,
    tertiaryContainer = TrophyGoldContainerLight,
    onTertiaryContainer = Color(0xFF452404),
    background = PavilionBackgroundLight,
    onBackground = SlateInkLight,
    surface = PavilionSurfaceLight,
    onSurface = SlateInkLight,
    surfaceVariant = PavilionSurfaceVariantLight,
    onSurfaceVariant = SlateMutedLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FBFA),
    surfaceContainer = Color(0xFFEEF4F1),
    surfaceContainerHigh = Color(0xFFE5EEEA),
    surfaceContainerHighest = Color(0xFFDCE7E2),
    outline = SlateOutlineLight,
    outlineVariant = Color(0xFFD8E4DF),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLuminous,
    onPrimary = Color(0xFF042619),
    primaryContainer = EmeraldDeepContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF8EC5B6),
    onSecondary = Color(0xFF0A2E25),
    secondaryContainer = Color(0xFF1A3A33),
    onSecondaryContainer = Color(0xFFC2ECE0),
    tertiary = TrophyGoldBright,
    onTertiary = Color(0xFF3B2102),
    tertiaryContainer = TrophyGoldContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    background = FloodlightBackgroundDark,
    onBackground = ChalkInkDark,
    surface = FloodlightSurfaceDark,
    onSurface = ChalkInkDark,
    surfaceVariant = FloodlightSurfaceVariantDark,
    onSurfaceVariant = SlateMutedDark,
    surfaceContainerLowest = Color(0xFF060C0E),
    surfaceContainerLow = Color(0xFF0E181C),
    surfaceContainer = Color(0xFF132026),
    surfaceContainerHigh = FloodlightSurfaceHighDark,
    surfaceContainerHighest = Color(0xFF1E323B),
    outline = SlateOutlineDark,
    outlineVariant = Color(0xFF21343D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val CricPulseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun CricPulseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val cricketColors = if (darkTheme) {
        CricketFormatColors(
            testBg = TestCrimsonContainerDark,
            testFg = TestCrimsonDark,
            odiBg = OdiCobaltContainerDark,
            odiFg = OdiCobaltDark,
            t20Bg = T20AmberContainerDark,
            t20Fg = T20AmberDark,
            liveDot = LivePulseRed,
            liveBannerBg = Color(0xFF450A0A),
            countdownBg = Color(0xFF0E3E2C),
            countdownFg = EmeraldLuminous
        )
    } else {
        CricketFormatColors(
            testBg = TestCrimsonContainerLight,
            testFg = TestCrimsonLight,
            odiBg = OdiCobaltContainerLight,
            odiFg = OdiCobaltLight,
            t20Bg = T20AmberContainerLight,
            t20Fg = T20AmberLight,
            liveDot = LivePulseRed,
            liveBannerBg = Color(0xFFFEE2E2),
            countdownBg = EmeraldMintContainer,
            countdownFg = EmeraldPitchDark
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalCricketColors provides cricketColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CricPulseTypography,
            shapes = CricPulseShapes,
            content = content
        )
    }
}
