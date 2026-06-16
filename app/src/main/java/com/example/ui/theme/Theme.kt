package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = PrimaryCyan,
  secondary = LightCyan,
  tertiary = Pink80,
  background = DarkBackground,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceVariant,
  onPrimary = androidx.compose.ui.graphics.Color.White,
  onSecondary = androidx.compose.ui.graphics.Color.White,
  onTertiary = androidx.compose.ui.graphics.Color.White,
  onBackground = TextLightGray,
  onSurface = TextLightGray,
  onSurfaceVariant = TextGray
)

private val LightColorScheme = lightColorScheme(
  primary = PrimaryCyan,
  secondary = LightCyan,
  tertiary = Pink40,
  background = DarkBackground,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceVariant,
  onPrimary = androidx.compose.ui.graphics.Color.White,
  onSecondary = androidx.compose.ui.graphics.Color.White,
  onTertiary = androidx.compose.ui.graphics.Color.White,
  onBackground = TextLightGray,
  onSurface = TextLightGray,
  onSurfaceVariant = TextGray

  /* Other default colors to override
  background = Color(0xFFFFFBFE),
  surface = Color(0xFFFFFBFE),
  onPrimary = Color.White,
  onSecondary = Color.White,
  onTertiary = Color.White,
  onBackground = Color(0xFF1C1B1F),
  onSurface = Color(0xFF1C1B1F),
  */
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for stealth look
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Force custom colors
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
