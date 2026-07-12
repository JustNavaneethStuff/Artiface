package com.artiface.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.artiface.app.ui.SplashRoute
import com.artiface.feature.camera.presentation.CameraRoute
import com.artiface.feature.gallery.presentation.GalleryRoute
import com.artiface.feature.onboarding.presentation.OnboardingRoute
import com.artiface.feature.preview.presentation.PreviewRoute
import com.artiface.feature.processing.presentation.ProcessingRoute
import com.artiface.feature.processing.presentation.StyleSelectionRoute
import com.artiface.feature.result.presentation.ResultRoute
import com.artiface.feature.settings.presentation.SettingsRoute

object ArtifaceDestinations {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Camera = "camera"
    const val Preview = "preview/{selfieId}"
    const val Style = "style/{selfieId}"
    const val Processing = "processing/{jobId}"
    const val Result = "result/{resultId}"
    const val Gallery = "gallery"
    const val Settings = "settings"

    fun preview(selfieId: String) = "preview/$selfieId"
    fun style(selfieId: String) = "style/$selfieId"
    fun processing(jobId: String) = "processing/$jobId"
    fun result(resultId: String) = "result/$resultId"
}

@Composable
fun ArtifaceNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ArtifaceDestinations.Splash,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(ArtifaceDestinations.Splash) {
            SplashRoute(
                onFinished = { hasCompletedOnboarding ->
                    val destination = if (hasCompletedOnboarding) {
                        ArtifaceDestinations.Camera
                    } else {
                        ArtifaceDestinations.Onboarding
                    }
                    navController.navigate(destination) {
                        popUpTo(ArtifaceDestinations.Splash) { inclusive = true }
                    }
                },
            )
        }
        composable(ArtifaceDestinations.Onboarding) {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(ArtifaceDestinations.Camera) {
                        popUpTo(ArtifaceDestinations.Onboarding) { inclusive = true }
                    }
                },
            )
        }
        composable(ArtifaceDestinations.Camera) {
            CameraRoute(
                onCaptured = { selfieId ->
                    navController.navigate(ArtifaceDestinations.preview(selfieId))
                },
                onOpenGallery = {
                    navController.navigate(ArtifaceDestinations.Gallery)
                },
            )
        }
        composable(
            route = ArtifaceDestinations.Preview,
            arguments = listOf(navArgument("selfieId") { type = NavType.StringType }),
        ) { entry ->
            val selfieId = entry.arguments?.getString("selfieId").orEmpty()
            PreviewRoute(
                selfieId = selfieId,
                onRetake = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(ArtifaceDestinations.style(selfieId))
                },
            )
        }
        composable(
            route = ArtifaceDestinations.Style,
            arguments = listOf(navArgument("selfieId") { type = NavType.StringType }),
        ) { entry ->
            val selfieId = entry.arguments?.getString("selfieId").orEmpty()
            StyleSelectionRoute(
                selfieId = selfieId,
                onStyleSelected = { styleId ->
                    // Phase 4 will create a real job id from style + selfie.
                    navController.navigate(ArtifaceDestinations.processing("job-$styleId"))
                },
            )
        }
        composable(
            route = ArtifaceDestinations.Processing,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
        ) { entry ->
            val jobId = entry.arguments?.getString("jobId").orEmpty()
            ProcessingRoute(
                jobId = jobId,
                onCompleted = { resultId ->
                    navController.navigate(ArtifaceDestinations.result(resultId)) {
                        popUpTo(ArtifaceDestinations.Camera)
                    }
                },
                onFailed = { navController.popBackStack() },
            )
        }
        composable(
            route = ArtifaceDestinations.Result,
            arguments = listOf(navArgument("resultId") { type = NavType.StringType }),
        ) { entry ->
            val resultId = entry.arguments?.getString("resultId").orEmpty()
            ResultRoute(
                resultId = resultId,
                onTryAnotherStyle = { navController.popBackStack() },
                onCreateAnother = {
                    navController.navigate(ArtifaceDestinations.Camera) {
                        popUpTo(ArtifaceDestinations.Camera) { inclusive = true }
                    }
                },
                onOpenGallery = {
                    navController.navigate(ArtifaceDestinations.Gallery)
                },
            )
        }
        composable(ArtifaceDestinations.Gallery) {
            GalleryRoute(
                onOpenResult = { resultId ->
                    navController.navigate(ArtifaceDestinations.result(resultId))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(ArtifaceDestinations.Settings) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
