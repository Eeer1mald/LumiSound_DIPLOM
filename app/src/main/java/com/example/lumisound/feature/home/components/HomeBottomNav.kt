package com.example.lumisound.feature.home.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lumisound.R

@Composable
fun HomeBottomNav(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar(modifier = Modifier.height(64.dp).testTag("home_bottomnav")) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { onNavigate("home") },
            icon = { Icon(painterResource(id = android.R.drawable.ic_menu_view), contentDescription = stringResource(R.string.home_tab_home)) },
            label = { Text(stringResource(R.string.home_tab_home)) }
        )
        NavigationBarItem(
            selected = currentRoute == "ratings",
            onClick = { onNavigate("ratings") },
            icon = { Icon(painterResource(id = android.R.drawable.star_big_on), contentDescription = stringResource(R.string.home_tab_ratings)) },
            label = { Text(stringResource(R.string.home_tab_ratings)) }
        )
        NavigationBarItem(
            selected = currentRoute == "search",
            onClick = { onNavigate("search") },
            icon = { Icon(painterResource(id = android.R.drawable.ic_menu_search), contentDescription = stringResource(R.string.home_tab_search)) },
            label = { Text(stringResource(R.string.home_tab_search)) }
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { onNavigate("profile") },
            icon = { Icon(painterResource(id = android.R.drawable.ic_menu_myplaces), contentDescription = stringResource(R.string.home_tab_profile)) },
            label = { Text(stringResource(R.string.home_tab_profile)) }
        )
    }
}


