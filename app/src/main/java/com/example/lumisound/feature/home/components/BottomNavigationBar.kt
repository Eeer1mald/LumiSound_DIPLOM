package com.example.lumisound.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

typealias NavItem = String

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItemData(
            id = "home",
            icon = Icons.Default.Home,
            label = "Домой"
        ),
        NavItemData(
            id = "search",
            icon = Icons.Default.Search,
            label = "Поиск"
        ),
        NavItemData(
            id = "ratings",
            icon = Icons.Default.Star,
            label = "Рецензии"
        ),
        NavItemData(
            id = "profile",
            icon = Icons.Default.Person,
            label = "Профиль"
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF0F1020).copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF2A2D3E).copy(alpha = 0.4f),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
            .testTag("bottom_navigation")
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            navItems.forEach { item ->
                NavigationItem(
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
private fun NavigationItem(
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
        Box(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isActive) {
                        Brush.horizontalGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    }
                )
                .shadow(
                    elevation = if (isActive) 4.dp else 0.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = if (isActive) GradientEnd.copy(alpha = 0.3f) else Color.Transparent
                )
                .then(if (isActive) Modifier.padding(10.dp) else Modifier.padding(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isActive) Color.White else ColorSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private data class NavItemData(
    val id: String,
    val icon: ImageVector,
    val label: String
)

