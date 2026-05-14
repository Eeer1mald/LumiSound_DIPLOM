package com.example.lumisound.feature.ratings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors

private val REPORT_REASONS = listOf(
    "Спам или реклама",
    "Оскорбления и грубость",
    "Ненависть и дискриминация",
    "Недостоверная информация",
    "Нарушение авторских прав",
    "Другое"
)

/**
 * Диалог выбора причины жалобы.
 * Вызывается при long-press на комментарий или рецензию.
 */
@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onReport: (reason: String) -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1C), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            if (sent) {
                // Экран подтверждения
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("✅", fontSize = 36.sp)
                    Text(
                        "Ваша жалоба отправлена",
                        color = LocalAppColors.current.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Мы рассмотрим её в ближайшее время",
                        color = LocalAppColors.current.secondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Закрыть", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // Экран выбора причины
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Пожаловаться",
                        color = LocalAppColors.current.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Выберите причину жалобы",
                        color = LocalAppColors.current.secondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    REPORT_REASONS.forEach { reason ->
                        val isSelected = selected == reason
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) GradientStart.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.04f),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) GradientStart.copy(alpha = 0.6f)
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selected = reason }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        if (isSelected) GradientStart else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(9.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                            Text(
                                reason,
                                color = if (isSelected) LocalAppColors.current.onBackground
                                        else LocalAppColors.current.secondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Отмена", color = LocalAppColors.current.secondary, fontSize = 14.sp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    if (selected != null)
                                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(GradientStart, GradientEnd))
                                    else androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = selected != null
                                ) {
                                    selected?.let { reason ->
                                        onReport(reason)
                                        sent = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Отправить",
                                color = if (selected != null) Color.White else LocalAppColors.current.secondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
