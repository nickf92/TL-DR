package it.tldl.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF81D4FA),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF81D4FA),
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF181818),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF0288D1),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
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
