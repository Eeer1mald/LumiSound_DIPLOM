package com.example.lumisound.feature.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lumisound.R
import com.example.lumisound.feature.auth.components.GradientButton
import com.example.lumisound.ui.theme.LocalAppColors

@Composable
fun VerifyEmailScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToProfileSetup: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.verify_email_title),
                style = MaterialTheme.typography.headlineSmall,
                color = LocalAppColors.current.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.verify_email_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = LocalAppColors.current.onBackground.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            GradientButton(
                text = "Войти",
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "После подтверждения email войдите с теми же данными, и вы попадете на экран настройки профиля",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = LocalAppColors.current.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}