package com.app.forgefocus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.app.forgefocus.core.presentation.theme.ForgeFocusTheme
import com.app.forgefocus.features.mountains.presentation.navigation.DashboardRoute
import com.app.forgefocus.features.mountains.presentation.navigation.mountainsGraph
import org.jetbrains.compose.resources.painterResource

import forgefocus.shared.generated.resources.Res
import forgefocus.shared.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
    ForgeFocusTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            mountainsGraph(navController = navController)
        }
    }
}