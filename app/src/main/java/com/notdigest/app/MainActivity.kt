package com.notdigest.app

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.core.Constants
import com.notdigest.app.data.system.AppIconLoader
import com.notdigest.app.ui.AppViewModel
import com.notdigest.app.ui.LocalAppIconLoader
import com.notdigest.app.ui.LocalIs24Hour
import com.notdigest.app.ui.navigation.NotDigestNavHost
import com.notdigest.app.ui.theme.NotDigestTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appIconLoader: AppIconLoader

    private val deepLinkRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeDeepLink(intent)

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val state by appViewModel.uiState.collectAsStateWithLifecycle()
            splash.setKeepOnScreenCondition { state.loading }

            val is24Hour = remember { DateFormat.is24HourFormat(this) }

            NotDigestTheme(themeMode = state.themeMode, dynamicColor = state.dynamicColor) {
                CompositionLocalProvider(
                    LocalIs24Hour provides is24Hour,
                    LocalAppIconLoader provides appIconLoader,
                ) {
                    if (!state.loading) {
                        NotDigestNavHost(
                            startDestination = state.startDestination,
                            deepLinkRoute = deepLinkRoute.value,
                            onDeepLinkConsumed = { deepLinkRoute.value = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeDeepLink(intent)
    }

    private fun consumeDeepLink(intent: Intent?) {
        if (intent?.action == Constants.ACTION_OPEN_ROUTE) {
            intent.getStringExtra(Constants.EXTRA_ROUTE)?.let { deepLinkRoute.value = it }
        }
    }
}
