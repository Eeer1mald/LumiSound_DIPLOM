package com.example.lumisound.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors

typealias NavItem = String

// Чёрный полупрозрачный фон
private val NavBarBackground = Color(0xFF000000).copy(alpha = 0.6f)
private val NavBarBorder = Color(0xFF1E1E1E).copy(alpha = 0.5f)

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItemData("home",    Icons.Default.Home,   "Домой"),
        NavItemData("search",  Icons.Default.Search, "Поиск"),
        NavItemData("ratings", Icons.Default.Star,   "Рецензии"),
        NavItemData("profile", Icons.Default.Person, "Профиль")
    )

    // Полупрозрачный тёмно-синий контейнер
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(NavBarBackground)
            .navigationBarsPadding()
            .testTag("bottom_navigation")
    ) {
        // Тонкая линия сверху
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(height = 1.dp, width = 0.dp)
                .background(NavBarBorder)
                .align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                NavBarItem(
                    item = item,
                    isActive = currentRoute == item.id,
                    onClick = { onNavigate(item.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItemData,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            // Активный — непрозрачный цветной квадратик с иконкой
            Box(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GradientStart) // полностью непрозрачный
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            // Неактивный — просто иконка
            Box(
                modifier = Modifier.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = LocalAppColors.current.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private data class NavItemData(
    val id: String,
    val icon: ImageVector,
    val label: String
)
