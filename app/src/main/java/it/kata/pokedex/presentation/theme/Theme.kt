package it.kata.pokedex.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AccentRed,
    onPrimary = Color.White,
    background = SheetLight,
    onBackground = InkLight,
    surface = SheetLight,
    onSurface = InkLight,
    surfaceVariant = FieldLight,
    onSurfaceVariant = InkMutedLight,
    outlineVariant = DividerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentRedDark,
    onPrimary = Color.Black,
    background = SheetDark,
    onBackground = InkDark,
    surface = SheetDark,
    onSurface = InkDark,
    surfaceVariant = FieldDark,
    onSurfaceVariant = InkMutedDark,
    outlineVariant = DividerDark,
)

/**
 * Dynamic colour is deliberately off: the app has its own identity and the type chips already
 * carry all the colour the screen can take. It also keeps screenshots consistent across devices.
 */
@Composable
fun PokedexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
