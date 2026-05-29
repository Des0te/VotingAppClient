package com.example.votingclient.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF246B5A),
    secondary = Color(0xFF6C5E95),
    tertiary = Color(0xFF9A5B45),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD8C4),
    secondary = Color(0xFFCFC2FF),
    tertiary = Color(0xFFFFB59E),
    background = Color(0xFF101416),
    surface = Color(0xFF171C1F),
)

@Composable
fun VotingClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
