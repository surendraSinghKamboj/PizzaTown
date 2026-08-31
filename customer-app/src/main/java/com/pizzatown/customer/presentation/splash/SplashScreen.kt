package com.pizzatown.customer.presentation.splash

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pizzatown.customer.R

/**
 * Branded splash: plays the Pizza Town Lottie animation once, then moves
 * on to the correct destination once BOTH the animation has had a chance
 * to play AND the auth state has resolved — whichever finishes last,
 * without adding any artificial extra delay.
 *
 * Respects reduced-motion: if the system animator scale is 0 (Settings >
 * Accessibility > Remove animations), we show a static logo instead of
 * playing the full animation, and proceed immediately once auth resolves.
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMenu: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 1f,
        isPlaying = !reduceMotion
    )

    val animationFinished = reduceMotion || progress >= 1f

    LaunchedEffect(animationFinished, isSignedIn) {
        if (animationFinished && isSignedIn != null) {
            if (isSignedIn == true) onNavigateToMenu() else onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .semantics { contentDescription = "Pizza Town" },
        contentAlignment = Alignment.Center
    ) {
        if (reduceMotion || composition == null) {
            // Static fallback: brand background + fade-free logo, no motion.
            Image(
                painter = painterResource(id = R.drawable.pizza_town_logo),
                contentDescription = "Pizza Town logo",
                modifier = Modifier.size(220.dp)
            )
        } else {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(280.dp)
            )
        }
    }
}
