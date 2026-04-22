package com.example.lumisound.feature.auth.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.lumisound.R
import com.example.lumisound.feature.auth.components.GradientButton
import com.example.lumisound.feature.auth.components.OutlinedGradientButton
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = LocalContext.current
    val logoId = remember { context.resources.getIdentifier("logo", "drawable", context.packageName) }
    val logoPlaceholderId = remember { context.resources.getIdentifier("ic_logo_foreground", "drawable", context.packageName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = when {
                    logoId != 0 -> logoId
                    logoPlaceholderId != 0 -> logoPlaceholderId
                    else -> android.R.drawable.ic_menu_gallery
                }),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .clip(RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Твоя музыка.",
                    color = LocalAppColors.current.onBackground,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Твой вкус.",
                    color = LocalAppColors.current.onBackground,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Твоя оценка.",
                    color = GradientStart,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            GradientButton(
                text = stringResource(R.string.login),
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "welcome_login"
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedGradientButton(
                text = stringResource(R.string.register),
                onClick = onRegisterClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "welcome_register"
            )
        }
    }
}
