package com.nurtlina.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nurtlina.app.core.notification.NotificationReceiver
import com.nurtlina.app.core.notification.FeedingReminderLaunch
import com.nurtlina.app.domain.repository.RatingPromptRepository
import com.nurtlina.app.ui.navigation.AppViewModel
import com.nurtlina.app.ui.navigation.NurtlinaNavHost
import com.nurtlina.app.ui.theme.NurtlinaTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var ratingPromptRepository: RatingPromptRepository

    private var feedingReminderLaunch by mutableStateOf<FeedingReminderLaunch?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureFeedingReminderLaunch(intent)
        recordRatingPromptLaunchState(intent)
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val nightModeEnabled by appViewModel.nightModeEnabled.collectAsStateWithLifecycle()
            val language by appViewModel.language.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = systemDark || nightModeEnabled

            androidx.compose.runtime.LaunchedEffect(language) {
                val languageTag = when (language) {
                    "zh" -> "zh-CN"
                    "en" -> {
                        // Default — fall back to system locale on first launch
                        val sysLang = java.util.Locale.getDefault().language
                        when (sysLang) {
                            "zh" -> "zh-CN"
                            "es" -> "es"
                            "de" -> "de"
                            "fr" -> "fr"
                            else -> "en"
                        }
                    }
                    "es", "de", "fr" -> language
                    else -> language ?: "en"
                }
                val locales = LocaleListCompat.forLanguageTags(languageTag)
                if (AppCompatDelegate.getApplicationLocales() != locales) {
                    AppCompatDelegate.setApplicationLocales(locales)
                }
            }

            NurtlinaTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NurtlinaNavHost(
                        feedingReminderLaunch = feedingReminderLaunch,
                        onFeedingReminderLaunchConsumed = { launch ->
                            if (feedingReminderLaunch?.token == launch.token) {
                                feedingReminderLaunch = null
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureFeedingReminderLaunch(intent)
        recordRatingPromptLaunchState(intent)
    }

    private fun captureFeedingReminderLaunch(intent: Intent?) {
        if (intent?.getStringExtra(NotificationReceiver.EXTRA_NOTIF_TYPE) !=
            NotificationReceiver.TYPE_NEXT_FEED
        ) return
        val babyId = intent.getStringExtra(NotificationReceiver.EXTRA_BABY_ID) ?: return
        feedingReminderLaunch = FeedingReminderLaunch(babyId = babyId)
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
