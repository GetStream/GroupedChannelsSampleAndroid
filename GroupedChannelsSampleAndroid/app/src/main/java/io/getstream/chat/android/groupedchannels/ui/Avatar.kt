package io.getstream.chat.android.groupedchannels.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun Avatar(
    seed: String,
    initials: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
) {
    val palette = remember(seed) { avatarGradient(seed) }
    Box(
        modifier = modifier.background(brush = palette, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val AvatarPalettes: List<Pair<Color, Color>> = listOf(
    Color(0xFF7F7FD5) to Color(0xFF86A8E7),
    Color(0xFFFF6A88) to Color(0xFFFFB199),
    Color(0xFF11998E) to Color(0xFF38EF7D),
    Color(0xFFFF9A9E) to Color(0xFFFAD0C4),
    Color(0xFF4776E6) to Color(0xFF8E54E9),
    Color(0xFFF7971E) to Color(0xFFFFD200),
)

private fun avatarGradient(seed: String): Brush {
    val (start, end) = AvatarPalettes[(seed.hashCode().toUInt() % AvatarPalettes.size.toUInt()).toInt()]
    return Brush.linearGradient(listOf(start, end))
}

fun String.initials(): String {
    val parts = split('_', '-', ' ').filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
