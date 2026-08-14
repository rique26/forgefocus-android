package com.app.forgefocus.features.mountains.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.app.forgefocus.features.mountains.presentation.screens.DashboardScreen
import com.app.forgefocus.features.mountains.presentation.screens.GoalDetailScreen

fun NavGraphBuilder.mountainsGraph(
    navController: NavHostController
) {

    composable<DashboardRoute> {
        DashboardScreen(
            onNavigateToDetail = { id ->
                navController.navigate(GoalDetailRoute(goalId = id))
            }
        )
    }


    composable<GoalDetailRoute> { backStackEntry ->
        val route: GoalDetailRoute = backStackEntry.toRoute()

        GoalDetailScreen(
            goalId = route.goalId,
            onBackClick = { navController.popBackStack() }
        )
    }
}