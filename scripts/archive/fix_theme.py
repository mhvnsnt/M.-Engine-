import os

with open('app/src/main/java/com/example/ui/theme/Color.kt', 'w') as f:
    f.write("""package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val PrimaryLight = Color(0xFF101010) // Near Black
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFF4F4F4)
val OnPrimaryContainerLight = Color(0xFF101010)
val SecondaryLight = Color(0xFF6B6B6B)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFF9F9F9)
val OnSecondaryContainerLight = Color(0xFF101010)

val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF101010)
val BackgroundLight = Color(0xFFFAFAFA)
val OnBackgroundLight = Color(0xFF101010)
val SurfaceVariantLight = Color(0xFFF4F4F5)
val OnSurfaceVariantLight = Color(0xFF27272A)
val OutlineLight = Color(0xFFE4E4E7)

val PrimaryDark = Color(0xFFFAFAFA)
val OnPrimaryDark = Color(0xFF101010)
val PrimaryContainerDark = Color(0xFF27272A)
val OnPrimaryContainerDark = Color(0xFFFAFAFA)
val SecondaryDark = Color(0xFFA1A1AA)
val OnSecondaryDark = Color(0xFF101010)
val SecondaryContainerDark = Color(0xFF18181B)
val OnSecondaryContainerDark = Color(0xFFFAFAFA)

val SurfaceDark = Color(0xFF09090B)
val OnSurfaceDark = Color(0xFFFAFAFA)
val BackgroundDark = Color(0xFF09090B)
val OnBackgroundDark = Color(0xFFFAFAFA)
val SurfaceVariantDark = Color(0xFF27272A)
val OnSurfaceVariantDark = Color(0xFFD4D4D8)
val OutlineDark = Color(0xFF3F3F46)
""")

with open('app/src/main/java/com/example/ui/theme/Theme.kt', 'w') as f:
    f.write("""package com.example.ui.theme

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
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamic color by default for a corporate brand look
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

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
""")
