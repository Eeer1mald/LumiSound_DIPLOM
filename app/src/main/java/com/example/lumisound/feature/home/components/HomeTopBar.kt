package com.example.lumisound.feature.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lumisound.R

@Composable
fun HomeTopBar(modifier: Modifier = Modifier, onProfileClick: () -> Unit) {
    val context = LocalContext.current
    val logoId = remember { context.resources.getIdentifier("logo", "drawable", context.packageName) }

    Surface(tonalElevation = 0.dp, shadowElevation = 0.dp, modifier = modifier.height(56.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(56.dp)
                .semantics { contentDescription = "home_topbar" }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = if (logoId != 0) logoId else R.drawable.ic_logo_foreground),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_myplaces),
                contentDescription = stringResource(id = R.string.home_profile_cd),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onProfileClick() }
            )
        }
    }
}


