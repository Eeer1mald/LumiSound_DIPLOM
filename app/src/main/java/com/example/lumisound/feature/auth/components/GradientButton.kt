package com.example.lumisound.feature.auth.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LumiSoundTheme

@Composable
fun GradientButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(50), // Уменьшено в 2 раза: было 100, стало 50
        label = "button_scale"
    )

    // Оптимизация: используем remember для градиента
    val gradient = remember {
        Brush.linearGradient(
            colors = listOf(GradientStart, GradientEnd),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }
    val disabledGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(
                androidx.compose.ui.graphics.Color(0xFF7B6DFF).copy(alpha = 0.5f),
                androidx.compose.ui.graphics.Color(0xFFFF5C6C).copy(alpha = 0.5f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = if (enabled) gradient else disabledGradient
            )
            .scale(scale)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            )
            .then(if (testTag != null) Modifier.semantics { contentDescription = testTag } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (text.isNotEmpty()) {
            if (leadingIcon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Preview(showBackground = false, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GradientButtonPreview() {
    LumiSoundTheme {
        GradientButton(
            text = "Войти",
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

