package com.forex.chart.analysis.ai.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.forex.chart.analysis.ai.navigation.NavRoute

// Deep Blue-Black Mix
val BottomNavColor = Color(0xFF001333)

@Composable
fun AppBottomNavigation(navController: NavController) {

    NavigationBar(
        containerColor = BottomNavColor,
        tonalElevation = 0.dp,
        modifier = Modifier.height(75.dp)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        NavRoute.bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(

                modifier = Modifier.padding(top = 8.dp),

                icon = {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = item.label,

                        modifier = Modifier
                            .size(28.dp)
                            .padding(bottom = 4.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(

                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,


                    unselectedIconColor = Color.White.copy(alpha = 0.5f),
                    unselectedTextColor = Color.White.copy(alpha = 0.5f),


                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}