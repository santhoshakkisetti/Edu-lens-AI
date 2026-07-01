package com.example.ui.theme

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
    primary = GeometricTextDark,
    secondary = GeometricLabelDark,
    tertiary = GeometricAccentDark,
    background = GeometricBgDark,
    surface = GeometricSurfaceDark,
    onBackground = GeometricTextDark,
    onSurface = GeometricTextDark,
    onSurfaceVariant = GeometricLabelDark,
    onPrimary = GeometricBgDark,
    error = RoseAccent,
    outline = GeometricBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = GeometricTextLight,
    secondary = GeometricLabelLight,
    tertiary = GeometricAccentLight,
    background = GeometricBgLight,
    surface = GeometricSurfaceLight,
    onBackground = GeometricTextLight,
    onSurface = GeometricTextLight,
    onSurfaceVariant = GeometricLabelLight,
    onPrimary = Color.White,
    error = RoseAccent,
    outline = GeometricBorderLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set false to prefer our premium Indigo educational brand identity
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
