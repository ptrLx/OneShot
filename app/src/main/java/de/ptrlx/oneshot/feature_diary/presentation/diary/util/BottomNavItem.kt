package de.ptrlx.oneshot.feature_diary.presentation.diary.util

import androidx.compose.ui.graphics.painter.Painter

data class BottomNavItem(
    val name: String,
    val route: String,
    val painter: Painter
)
