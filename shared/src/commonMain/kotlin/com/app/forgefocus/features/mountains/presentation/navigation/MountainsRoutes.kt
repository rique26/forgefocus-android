package com.app.forgefocus.features.mountains.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
data class GoalDetailRoute(val goalId: Long)