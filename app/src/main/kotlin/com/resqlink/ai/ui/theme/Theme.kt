/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.resqlink.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ResQNavyBackground = Color(0xFF081226)
val ResQNavySurface = Color(0xFF101C36)
val ResQNavyCard = Color(0xFF162544)
val ResQNavyCardBorder = Color(0xFF263964)
val ResQCrimsonPrimary = Color(0xFFE53935)
val ResQCrimsonGlow = Color(0xFFFF334B)
val ResQTextPearl = Color(0xFFF8FAFC)
val ResQTextMuted = Color(0xFF94A3B8)

private val ResQColorScheme = darkColorScheme(
    primary = ResQCrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = ResQNavyCard,
    onPrimaryContainer = ResQTextPearl,
    secondary = ResQCrimsonGlow,
    onSecondary = Color.White,
    background = ResQNavyBackground,
    onBackground = ResQTextPearl,
    surface = ResQNavySurface,
    onSurface = ResQTextPearl,
    surfaceVariant = ResQNavyCard,
    onSurfaceVariant = ResQTextMuted,
    outline = ResQNavyCardBorder
)

@Composable
fun ResQLinkAITheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ResQColorScheme,
        typography = Typography,
        content = content
    )
}

