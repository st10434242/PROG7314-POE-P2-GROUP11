package com.example.runway.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.runway.ui.itemdetail.ItemDetailRoute
import com.example.runway.ui.items.ItemsRoute

@Composable
fun RunwayNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.ITEMS,
        modifier = modifier
    ) {
        composable(Routes.ITEMS) {
            ItemsRoute(
                onItemClick = { id -> navController.navigate(Routes.itemDetail(id)) }
            )
        }

        composable(
            route = Routes.ITEM_DETAIL,
            arguments = listOf(
                navArgument(Routes.ARG_ITEM_ID) { type = NavType.LongType }
            )
        ) {
            ItemDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}
