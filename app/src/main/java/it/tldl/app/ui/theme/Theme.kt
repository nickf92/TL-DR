package it.tldl.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF00325B),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF81D4FA),
    onSecondary = Color(0xFF003548),
    secondaryContainer = Color(0xFF004D67),
    onSecondaryContainer = Color(0xFFC2E8FF),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF003915),
    tertiaryContainer = Color(0xFF005322),
    onTertiaryContainer = Color(0xFF9DF49E),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE2E2E2),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF24272B),
    onSurfaceVariant = Color(0xFFC3C7CE),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF282828),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF00325B),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF81D4FA),
    onSecondary = Color(0xFF003548),
    secondaryContainer = Color(0xFF18222A),
    onSecondaryContainer = Color(0xFFC2E8FF),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF003915),
    tertiaryContainer = Color(0xFF0D3316),
    onTertiaryContainer = Color(0xFF9DF49E),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFC3C7CE),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5C0003),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC2E8FF),
    onSecondaryContainer = Color(0xFF001E2C),
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6C9),
    onTertiaryContainer = Color(0xFF002107),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E3E8),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceContainer = Color(0xFFF0F4F8),
    surfaceContainerHigh = Color(0xFFE6EAEF),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

fun getAmoledDarkColorScheme(): ColorScheme = AmoledDarkColorScheme

@Composable
fun TLDLTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useAmoled: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                val baseDynamic = dynamicDarkColorScheme(context)
                if (useAmoled) {
                    baseDynamic.copy(background = Color.Black, surface = Color.Black)
                } else baseDynamic
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> {
            if (useAmoled) AmoledDarkColorScheme else DarkColorScheme
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
