package com.hasancankula.evtelemetry.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    secondary = BrandSecondaryDark,
    background = BrandBackgroundDark,
    surface = BrandSurfaceDark,
    surfaceVariant = Color(0xFF2D2D2D), // Gece modunda kartların arkaplanı
    onPrimary = Color.Black, // Buton içi yazılar
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    secondary = BrandSecondaryLight,
    background = BrandBackgroundLight,
    surface = BrandSurfaceLight,
    surfaceVariant = Color(0xFFE9EDF5), // Gündüz modunda kartlar için tatlı bir açık mavi/gri
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun EVTelemetryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // YENİ: Dinamik rengi varsayılan olarak false yaptık. Artık marka renklerimiz patlayacak!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}