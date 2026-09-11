package com.maya.assistant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MayaDarkColors = darkColorScheme(
    primary = Color(0xFF8E5CF0),
    background = Color(0xFF0A0812),
    surface = Color(0xFF120E1E),
    onPrimary = Color.White,
    onBackground = Color.White,
)

@Composable
fun MayaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MayaDarkColors,
        content = content,
    )
}
