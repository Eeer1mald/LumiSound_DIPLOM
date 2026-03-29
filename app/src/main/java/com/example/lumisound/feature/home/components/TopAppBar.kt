package com.example.lumisound.feature.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumisound.R
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    userName: String = "Пользователь",
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val logoId = remember { context.resources.getIdentifier("logo", "drawable", context.packageName) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 0.dp)
    ) {
        // Logo - увеличен в 4 раза от оригинала
        Box(
            modifier = Modifier
                .size(400.dp, 112.dp) // Увеличено еще в 2 раза: было 200x56, стало 400x112
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        ) {
            if (logoId != 0) {
                Image(
                    painter = painterResource(id = logoId),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = stringResource(R.string.app_name),
                    color = ColorOnBackground,
                    fontSize = 64.sp, // Увеличено еще в 2 раза
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



