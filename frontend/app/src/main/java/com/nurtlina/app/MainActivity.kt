package com.nurtlina.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nurtlina.app.core.notification.NotificationReceiver
import com.nurtlina.app.domain.repository.RatingPromptRepository
import com.nurtlina.app.ui.navigation.AppViewModel
import com.nurtlina.app.ui.navigation.NurtlinaNavHost
import com.nurtlina.app.ui.theme.NurtlinaTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ratingPromptRepository: RatingPromptRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        recordRatingPromptLaunchState(intent)
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val nightModeEnabled by appViewModel.nightModeEnabled.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = systemDark || nightModeEnabled

            NurtlinaTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NurtlinaNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recordRatingPromptLaunchState(intent)
    }

    private fun recordRatingPromptLaunchState(intent: Intent?) {
        lifecycleScope.launch {
            val now = Instant.now()
            ratingPromptRepository.ensureFirstLaunchAt(now)
            if (intent?.hasExtra(NotificationReceiver.EXTRA_NOTIF_TYPE) == true) {
                ratingPromptRepository.recordNotificationOpened(now)
            }
        }
    }
}
