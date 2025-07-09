package com.dailydevchallenge.devstreaks.features.navigation

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dailydevchallenge.devstreaks.features.routes.Routes


@Composable
fun BottomNavBar(navController: NavController) {
    data class BottomNavItem(
        val route: String,
        val icon: ImageVector,
        val label: String
    )

    val items = listOf(
        BottomNavItem(Routes.HomeScreen, Icons.Default.Home, "Home"),
        BottomNavItem(Routes.Paths, Icons.Default.Explore, "Paths"),
        BottomNavItem(Routes.Journal, Icons.Default.Insights, "My Day"),
        BottomNavItem(Routes.Progress, Icons.Default.Leaderboard, "Progress"),
        BottomNavItem(Routes.Profile, Icons.Default.Person, "Profile")
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: ""

    NavigationBar(
        modifier = Modifier.shadow(4.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            val isSelected = currentRoute.startsWith(item.route)

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(text = item.label) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.HomeScreen) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

