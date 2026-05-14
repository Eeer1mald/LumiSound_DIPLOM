package com.example.lumisound.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumisound.BuildConfig
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
            .statusBarsPadding()
    ) {
        // Хедер
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalAppColors.current.background)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Назад",
                    tint = LocalAppColors.current.onBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "О приложении",
                color = LocalAppColors.current.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 68.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Версия
            AboutInfoRow(
                icon = Icons.Default.Info,
                label = "Версия приложения",
                value = BuildConfig.VERSION_NAME
            )

            // Авторские права на музыку
            AboutBlock(
                icon = Icons.Default.MusicNote,
                title = "Авторские права на музыку",
                text = "Все треки, исполнители и обложки предоставлены платформой Audius — " +
                    "децентрализованной музыкальной сетью. Авторские права на контент принадлежат " +
                    "соответствующим правообладателям и исполнителям. LumiSound использует открытый " +
                    "API Audius исключительно в некоммерческих целях."
            )

            // Политика конфиденциальности
            AboutBlock(
                icon = Icons.Default.Lock,
                title = "Политика конфиденциальности",
                text = "LumiSound собирает минимально необходимые данные: email для авторизации, " +
                    "имя пользователя и аватар для профиля. Данные хранятся в защищённой базе " +
                    "Supabase и не передаются третьим лицам. История прослушиваний и оценки " +
                    "используются только для персонализации рекомендаций внутри приложения."
            )

            // Пользовательское соглашение
            AboutBlock(
                icon = Icons.Default.Description,
                title = "Пользовательское соглашение",
                text = "Используя LumiSound, вы соглашаетесь не нарушать авторские права, " +
                    "не публиковать оскорбительный контент и не использовать приложение в " +
                    "коммерческих целях без разрешения. Нарушение правил может привести к " +
                    "блокировке аккаунта."
            )

            // Правила сообщества
            AboutBlock(
                icon = Icons.Default.Gavel,
                title = "Правила сообщества",
                text = "Уважайте других пользователей в комментариях и рецензиях. " +
                    "Запрещены оскорбления, спам и дискриминация. " +
                    "За нарушения предусмотрено удаление контента и бан аккаунта. " +
                    "При первом нарушении — предупреждение, при повторных — временная или постоянная блокировка."
            )

            // Копирайт
            Text(
                "© 2025 LumiSound. Все права защищены.\n" +
                    "Музыкальный контент © Audius и соответствующие правообладатели.",
                color = LocalAppColors.current.secondary.copy(alpha = 0.5f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AboutInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = GradientStart, modifier = Modifier.size(20.dp))
        }
        Text(
            label,
            color = LocalAppColors.current.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(value, color = LocalAppColors.current.secondary, fontSize = 13.sp)
    }
}

@Composable
private fun AboutBlock(icon: ImageVector, title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = GradientStart, modifier = Modifier.size(20.dp))
            }
            Text(
                title,
                color = LocalAppColors.current.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text,
            color = LocalAppColors.current.secondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}
