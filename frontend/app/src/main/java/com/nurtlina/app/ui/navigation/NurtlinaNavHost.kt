package com.nurtlina.app.ui.navigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.review.ReviewManagerFactory
import com.nurtlina.app.R
import com.nurtlina.app.core.notification.FeedReminderConfig
import com.nurtlina.app.data.billing.EntitlementManager
import com.nurtlina.app.data.billing.ProStatus
import com.nurtlina.app.domain.model.DiaperType
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedingPrediction
import com.nurtlina.app.domain.model.TodaySummary
import com.nurtlina.app.domain.model.UnitType
import com.nurtlina.app.domain.model.UserSettings
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.ui.auth.SignInScreen
import com.nurtlina.app.ui.feed.NewFeedSheet
import com.nurtlina.app.ui.feed.NewFeedUiState
import com.nurtlina.app.ui.insights.InsightsDateRange
import com.nurtlina.app.ui.insights.InsightsScreen
import com.nurtlina.app.ui.insights.InsightsViewModel
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.model.NursingSide
import com.nurtlina.app.ui.logs.DiaperEditState
import com.nurtlina.app.ui.logs.FeedEditState
import com.nurtlina.app.ui.logs.LogEditSheet
import com.nurtlina.app.ui.logs.LogEditTarget
import com.nurtlina.app.ui.logs.LogEntry
import com.nurtlina.app.ui.logs.LogItem
import com.nurtlina.app.ui.logs.LogsScreen
import com.nurtlina.app.ui.logs.LogsViewModel
import com.nurtlina.app.ui.logs.SleepEditState
import com.nurtlina.app.ui.onboarding.OnboardingScreen
import com.nurtlina.app.ui.onboarding.OnboardingViewModel
import com.nurtlina.app.ui.paywall.PaywallScreen
import com.nurtlina.app.ui.paywall.PaywallViewModel
import com.nurtlina.app.ui.settings.SettingsScreen
import com.nurtlina.app.ui.settings.SettingsViewModel
import com.nurtlina.app.ui.today.FeedingStatusUiState
import com.nurtlina.app.ui.today.TodayScreen
import com.nurtlina.app.ui.today.TodayUiState
import com.nurtlina.app.ui.today.TodayViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

// ── App-level ViewModel ──────────────────────────────────────────────────────

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    entitlementManager: EntitlementManager,
) : ViewModel() {

    val onboardingComplete: StateFlow<Boolean?> = settingsRepository
        .observe()
        .map { it.onboardingCompleted }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    val isPro: StateFlow<Boolean> = entitlementManager.proStatus
        .map { it != ProStatus.FREE && it != ProStatus.UNKNOWN }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false,
        )

    val nightModeEnabled: StateFlow<Boolean> = settingsRepository
        .observe()
        .map { it.nightModeEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false,
        )

    val language: StateFlow<String?> = settingsRepository
        .observe()
        .map { it.language }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )
}

// ── Navigation host ──────────────────────────────────────────────────────────

private const val PRIVACY_URL = "https://nurtlina.app/privacy"
private const val TERMS_URL = "https://nurtlina.app/terms"
private const val PLAY_STORE_SUBS_URL =
    "https://play.google.com/store/account/subscriptions?package=com.nurtlina.app"
private const val PLAY_STORE_APP_URL = "market://details?id=com.nurtlina.app"
private const val PLAY_STORE_APP_WEB_URL =
    "https://play.google.com/store/apps/details?id=com.nurtlina.app"

@Composable
fun NurtlinaNavHost(modifier: Modifier = Modifier) {

    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val onboardingComplete by appViewModel.onboardingComplete.collectAsStateWithLifecycle()
    val isPro by appViewModel.isPro.collectAsStateWithLifecycle()

    if (onboardingComplete == null) {
        Box(modifier.fillMaxSize())
        return
    }

    val startDestination = if (onboardingComplete == true) {
        NavRoutes.Today.route
    } else {
        NavRoutes.OnboardingGraph.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in mainTabRoutes

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NurtlinaBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                )
            }
        },
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {

            // ── Onboarding flow ──────────────────────────────────────────

            navigation(
                startDestination = NavRoutes.Welcome.route,
                route = NavRoutes.OnboardingGraph.route,
            ) {
                composable(NavRoutes.Welcome.route) {
                    OnboardingRoute(navController)
                }
                composable(NavRoutes.CreateBaby.route) {
                    OnboardingRoute(navController)
                }
                composable(NavRoutes.GuidelineSelect.route) {
                    OnboardingRoute(navController)
                }
                composable(NavRoutes.Disclaimer.route) {
                    OnboardingRoute(navController)
                }
                composable(NavRoutes.NotificationPermission.route) {
                    OnboardingRoute(navController)
                }
            }

            // ── Main tabs ────────────────────────────────────────────────

            composable(NavRoutes.Today.route) {
                TodayRoute(navController, isPro)
            }

            composable(NavRoutes.Logs.route) {
                LogsRoute()
            }

            composable(NavRoutes.Insights.route) {
                InsightsRoute(navController, isPro)
            }

            composable(NavRoutes.Settings.route) {
                SettingsRoute(navController, isPro)
            }

            // ── Feed screens ─────────────────────────────────────────────

            composable(NavRoutes.NewFeed.route) {
                NewFeedRoute(navController)
            }

            // ── Paywall ──────────────────────────────────────────────────
            composable(NavRoutes.Paywall.route) {
                PaywallRoute(navController)
            }

            // ── Sign-in ──────────────────────────────────────────────────
            composable(NavRoutes.SignIn.route) {
                SignInScreen(
                    onBack = { navController.popBackStack() },
                    onSignInSuccess = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun OnboardingRoute(navController: NavController) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* granted or denied — either way, proceed */ }

    OnboardingScreen(
        onComplete = { babyInput, region ->
            viewModel.setBabyName(babyInput.name)
            viewModel.setBirthDate(babyInput.birthDate)
            viewModel.setAvatarColor(babyInput.avatarColor)
            viewModel.setGuidelineRegion(region)
            viewModel.completeOnboarding {
                navController.navigate(NavRoutes.Today.route) {
                    popUpTo(NavRoutes.OnboardingGraph.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )
}

@Composable
private fun TodayRoute(navController: NavController, isPro: Boolean) {
    val viewModel: TodayViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val babies by viewModel.babies.collectAsStateWithLifecycle()
    val selectedBaby by viewModel.selectedBaby.collectAsStateWithLifecycle()
    val latestFeed by viewModel.latestFeed.collectAsStateWithLifecycle()
    val feedingPrediction by viewModel.feedingPrediction.collectAsStateWithLifecycle()
    val todaySummary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val showRatingPrompt by viewModel.showRatingPrompt.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(Instant.now()) }
    val context = LocalContext.current
    val summary = todaySummary ?: emptyTodaySummary()
    val currentSettings = settings ?: UserSettings()
    val unitType = currentSettings.unit

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = Instant.now()
        }
    }

    LaunchedEffect(latestFeed?.id, latestFeed?.startedAt) {
        latestFeed?.let(viewModel::scheduleNextFeedReminder)
    }

    LaunchedEffect(
        summary.totalFeedCount,
        currentSettings.nightModeEnabled,
    ) {
        delay(7_000L)
        viewModel.maybeShowRatingPrompt(
            nightModeEnabled = currentSettings.nightModeEnabled,
        )
    }

    val windowStartTime = feedingPrediction?.windowStart?.let { time ->
        java.time.format.DateTimeFormatter
            .ofLocalizedTime(java.time.format.FormatStyle.SHORT)
            .withZone(java.time.ZoneId.systemDefault())
            .format(time)
    }

    TodayScreen(
        state = TodayUiState(
            babies = babies,
            selectedBaby = selectedBaby,
            feedingStatus = buildFeedingStatus(
                babyName = selectedBaby?.name,
                latestFeed = latestFeed,
                prediction = feedingPrediction,
                summary = summary,
                unitType = unitType,
                now = now,
                windowLaterMessage = windowStartTime?.let {
                    stringResource(R.string.today_feeding_window_later, it)
                },
                windowSoonMessage = stringResource(R.string.today_feeding_window_approaching),
                recentPatternMessage = stringResource(R.string.today_feeding_window_recent_pattern),
            ),
            todaySummary = summary,
            showAds = !isPro && !currentSettings.nightModeEnabled,
            nightModeEnabled = currentSettings.nightModeEnabled,
            unitType = unitType,
            showRatingPrompt = showRatingPrompt,
        ),
        onSelectBaby = { viewModel.selectBaby(it.id) },
        onAddBaby = {
            if (isPro) {
                // Pro: navigate directly to baby creation (future screen)
            } else {
                navController.navigate(NavRoutes.Paywall.route)
            }
        },
        onNewFeed = { navController.navigate(NavRoutes.NewFeed.route) },
        onQuickFeedAmount = { ml -> viewModel.quickLogFeed(ml) },
        onQuickDiaper = { viewModel.quickLogDiaper(DiaperType.WET) },
        onQuickSleep = {
            if (todaySummary?.activeSleepStartedAt == null) {
                viewModel.startSleep()
            } else {
                viewModel.endSleep()
            }
        },
        onRatingPromptRate = {
            viewModel.markRatingPromptRateClicked()
            launchInAppReviewOrStore(context)
        },
        onRatingPromptMaybeLater = viewModel::dismissRatingPromptForMaybeLater,
        onRatingPromptNoThanks = viewModel::dismissRatingPromptPermanently,
    )
}

@Composable
private fun NewFeedRoute(navController: NavController) {
    val todayViewModel: TodayViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val selectedBaby by todayViewModel.selectedBaby.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var sheetState by remember { mutableStateOf(NewFeedUiState()) }

    Box(Modifier.fillMaxSize())
    NewFeedSheet(
        state = sheetState.copy(unitType = settings?.unit ?: UnitType.ML),
        onMilkTypeChange = { sheetState = sheetState.copy(milkType = it) },
        onAmountChange = { sheetState = sheetState.copy(amountMl = it) },
        onPreparedAtChange = { sheetState = sheetState.copy(preparedAt = it, isJustNow = false) },
        onIsJustNowChange = {
            sheetState = sheetState.copy(
                isJustNow = it,
                preparedAt = if (it) Instant.now() else sheetState.preparedAt,
            )
        },
        onNoteChange = { sheetState = sheetState.copy(note = it) },
        onCreate = {
            selectedBaby ?: return@NewFeedSheet
            todayViewModel.logFeed(
                milkType = sheetState.milkType,
                amountMl = sheetState.amountMl,
                startedAt = if (sheetState.isJustNow) Instant.now() else sheetState.preparedAt,
                note = sheetState.note.ifBlank { null },
                onLogged = { navController.popBackStack() },
            )
        },
        titleRes = R.string.new_feed_title,
        createButtonRes = R.string.action_save_feed,
        disclaimerRes = R.string.new_feed_disclaimer,
        onDismiss = { navController.popBackStack() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogsRoute() {
    val viewModel: LogsViewModel = hiltViewModel()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val logItems by viewModel.logItems.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var editingTarget by remember { mutableStateOf<LogEditTarget?>(null) }

    LogsScreen(
        selectedDate = selectedDate,
        entries = logItems.map { it.toLogEntry() },
        activeFilter = selectedFilter,
        useOz = false,
        onPrevDay = viewModel::goToPreviousDay,
        onNextDay = viewModel::goToNextDay,
        onPickDate = { showDatePicker = true },
        onFilterSelected = viewModel::setFilter,
        onEntryClick = { entry -> editingTarget = entry.toEditTarget() },
        onEntryDelete = { entry ->
            when (entry) {
                is LogEntry.Feed -> viewModel.deleteFeedLog(entry.id)
                is LogEntry.Diaper -> viewModel.deleteDiaperLog(entry.id)
                is LogEntry.Sleep -> viewModel.deleteSleepLog(entry.id)
            }
        },
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.goToDate(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    editingTarget?.let { target ->
        LogEditSheet(
            target = target,
            useOz = false,
            onDismiss = { editingTarget = null },
            onSave = { updated ->
                viewModel.updateEntry(updated)
                editingTarget = null
            },
            onDelete = {
                viewModel.deleteEntry(target)
                editingTarget = null
            },
        )
    }
}

@Composable
private fun InsightsRoute(navController: NavController, isPro: Boolean) {
    val todayViewModel: TodayViewModel = hiltViewModel()
    val insightsViewModel: InsightsViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val todaySummary by todayViewModel.todaySummary.collectAsStateWithLifecycle()
    val weeklySummary by insightsViewModel.weeklySummary.collectAsStateWithLifecycle()
    val selectedRange by insightsViewModel.selectedRange.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val useOz = settings?.unit == UnitType.OZ

    InsightsScreen(
        todaySummary = todaySummary ?: emptyTodaySummary(),
        isPro = isPro,
        useOz = useOz,
        selectedRange = selectedRange,
        weeklyData = weeklySummary,
        proData = null, // Extended-range Pro data (14d/30d) — future enhancement
        onRangeSelected = { insightsViewModel.setRange(it) },
        onUpgradeTapped = { navController.navigate(NavRoutes.Paywall.route) },
    )
}

@Composable
private fun SettingsRoute(navController: NavController, isPro: Boolean) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val babies by viewModel.babies.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val notificationPermissionGranted by viewModel.notificationPermissionGranted.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshNotificationPermission()
        viewModel.updateNotificationEnabled(granted)
    }

    val showFaq by viewModel.showFaq.collectAsStateWithLifecycle()
    val currentSettings = settings ?: UserSettings()
    val selectedBaby = babies.firstOrNull { it.id == currentSettings.selectedBabyId }
        ?: babies.firstOrNull()

    if (showFaq) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFaq() },
            title = { Text(stringResource(R.string.faq_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FaqItem(R.string.faq_q1, R.string.faq_a1)
                    FaqItem(R.string.faq_q2, R.string.faq_a2)
                    FaqItem(R.string.faq_q3, R.string.faq_a3)
                    FaqItem(R.string.faq_q4, R.string.faq_a4)
                    FaqItem(R.string.faq_q5, R.string.faq_a5)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissFaq() }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }

    LaunchedEffect(settings?.notificationEnabled) {
        val persistedSettings = settings ?: return@LaunchedEffect
        val hasSystemPermission = viewModel.refreshNotificationPermission()
        if (persistedSettings.notificationEnabled && !hasSystemPermission) {
            viewModel.updateNotificationEnabled(false)
        }
    }

    SettingsScreen(
        baby = selectedBaby,
        settings = currentSettings,
        isPro = isPro,
        currentUser = currentUser,
        notificationPermissionGranted = notificationPermissionGranted,
        appVersion = "1.0.0",
        onBabyUpdated = { name, birthDate ->
            selectedBaby?.let { viewModel.updateBaby(it.id, name, birthDate) }
        },
        onUnitChanged = viewModel::updateUnitType,
        onGuidelineRegionChanged = viewModel::updateGuidelineRegion,
        onLanguageSelected = viewModel::updateLanguage,
        onNotificationsToggled = { enabled ->
            if (
                enabled &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !notificationPermissionGranted
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.updateNotificationEnabled(enabled)
            }
        },
        onReminderTimingChanged = viewModel::updateReminderBeforeExpiryMinutes,
        onFeedIntervalChanged = viewModel::updateFeedReminderInterval,
        onNightModeToggled = viewModel::updateNightModeEnabled,
        onManageSubscription = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_SUBS_URL))
            )
        },
        onUpgradeTapped = { navController.navigate(NavRoutes.Paywall.route) },
        onExportCsv = {
            viewModel.exportCsv(context)
        },
        onBackupClick = { viewModel.requestFullSync() },
        onSignInClick = { navController.navigate(NavRoutes.SignIn.route) },
        onSignOutClick = { viewModel.signOut() },
        onFaqClick = { viewModel.showFaq() },
        onPrivacyPolicyClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
        },
        onTermsClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL)))
        },
        onContactSupportClick = {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@nurtlina.app")
                }
            )
        },
    )
}

@Composable
private fun PaywallRoute(navController: NavController) {
    val viewModel: PaywallViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity: Activity? = context.findActivity()

    PaywallScreen(
        onClose = { navController.popBackStack() },
        onSubscribeMonthly = { activity?.let { act: Activity -> viewModel.subscribe(act, "monthly") } },
        onSubscribeYearly = { activity?.let { act: Activity -> viewModel.subscribe(act, "yearly") } },
        onBuyLifetime = { activity?.let { act: Activity -> viewModel.subscribe(act, "lifetime") } },
        onRestorePurchases = { viewModel.restorePurchases() },
        onPrivacyPolicy = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
        },
        onTerms = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL)))
        },
    )
}

private fun LogItem.toLogEntry(): LogEntry = when (this) {
    is LogItem.FeedLogItem -> LogEntry.Feed(feedLog)
    is LogItem.DiaperLogItem -> LogEntry.Diaper(diaperLog)
    is LogItem.SleepLogItem -> LogEntry.Sleep(sleepLog)
}

private fun emptyTodaySummary(): TodaySummary = TodaySummary(
    totalFeedCount = 0,
    totalAmountMl = 0.0,
    diaperCount = 0,
    sleepDurationMillis = 0L,
    activeSleepStartedAt = null,
)

private fun buildFeedingStatus(
    babyName: String?,
    latestFeed: FeedLog?,
    prediction: FeedingPrediction?,
    summary: TodaySummary,
    unitType: UnitType,
    now: Instant,
    windowLaterMessage: String? = null,
    windowSoonMessage: String? = null,
    recentPatternMessage: String? = null,
): FeedingStatusUiState {
    val elapsedSinceLastFeed = latestFeed?.startedAt?.let { startedAt ->
        val elapsed = Duration.between(startedAt, now)
        if (elapsed.isNegative) Duration.ZERO else elapsed
    }

    // Build feeding window text from prediction
    val windowStartText: String? = prediction?.windowStart?.let(::formatTime)
    val windowEndText: String? = prediction?.windowEnd?.let(::formatTime)
    val windowMessage: String? = when {
        prediction == null || prediction.isLearning -> null
        now.isBefore(prediction.windowStart) -> windowLaterMessage
        now.isBefore(prediction.windowEnd) -> windowSoonMessage
        else -> recentPatternMessage
    }

    return FeedingStatusUiState(
        babyName = babyName.orEmpty(),
        lastFeedTimeText = latestFeed?.startedAt?.let(::formatTime),
        lastFeedAgoText = elapsedSinceLastFeed?.let(::formatCompactDuration),
        lastFeedAmountText = latestFeed?.amountMl?.let { formatAmount(it, unitType) },
        todayFeedCount = summary.totalFeedCount,
        todayAmountText = formatAmount(summary.totalAmountMl, unitType),
        feedingWindowStartText = windowStartText,
        feedingWindowEndText = windowEndText,
        feedingWindowMessage = windowMessage,
        isLearning = prediction?.isLearning == true,
    )
}

private fun formatTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun formatCompactDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes().coerceAtLeast(if (hours > 0) 0L else 1L)
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatAmount(amountMl: Double, unitType: UnitType): String = when (unitType) {
    UnitType.ML -> "%.0f ml".format(amountMl)
    UnitType.OZ -> "%.1f oz".format(amountMl / 29.5735)
}

private fun launchInAppReviewOrStore(context: Context) {
    val activity = context.findActivity()
    if (activity == null) {
        openPlayStorePage(context)
        return
    }

    val reviewManager = ReviewManagerFactory.create(context)
    reviewManager.requestReviewFlow()
        .addOnSuccessListener { reviewInfo ->
            reviewManager.launchReviewFlow(activity, reviewInfo)
                .addOnFailureListener { openPlayStorePage(context) }
        }
        .addOnFailureListener { openPlayStorePage(context) }
}

private fun openPlayStorePage(context: Context) {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_APP_URL)).apply {
        setPackage("com.android.vending")
    }
    runCatching { context.startActivity(marketIntent) }
        .onFailure {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_APP_WEB_URL)))
        }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ── Bottom navigation bar ────────────────────────────────────────────────────

private data class BottomNavItem(
    val route: String,
    val labelResId: Int,
    val icon: ImageVector,
    val contentDescriptionResId: Int,
)

private val bottomNavItems: List<BottomNavItem> = listOf(
    BottomNavItem(
        route = NavRoutes.Today.route,
        labelResId = R.string.nav_today,
        icon = Icons.Default.Home,
        contentDescriptionResId = R.string.nav_today_cd,
    ),
    BottomNavItem(
        route = NavRoutes.Logs.route,
        labelResId = R.string.nav_logs,
        icon = Icons.Default.Book,
        contentDescriptionResId = R.string.nav_logs_cd,
    ),
    BottomNavItem(
        route = NavRoutes.Insights.route,
        labelResId = R.string.nav_insights,
        icon = Icons.Default.BarChart,
        contentDescriptionResId = R.string.nav_insights_cd,
    ),
    BottomNavItem(
        route = NavRoutes.Settings.route,
        labelResId = R.string.nav_settings,
        icon = Icons.Default.Settings,
        contentDescriptionResId = R.string.nav_settings_cd,
    ),
)

@Composable
private fun NurtlinaBottomBar(
    navController: NavController,
    currentRoute: String?,
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.contentDescriptionResId),
                    )
                },
                label = { Text(stringResource(item.labelResId)) },
            )
        }
    }
}

// ── LogEntry → LogEditTarget converter ─────────────────────────────────────────

private fun LogEntry.toEditTarget(): LogEditTarget = when (this) {
    is LogEntry.Feed -> {
        val log = this.log
        LogEditTarget.Feed(
            original = log,
            draft = FeedEditState(
                feedType = log.feedType,
                amountMl = log.amountMl?.let { "%.0f".format(it) } ?: "",
                nursingSide = log.side,
                time = log.startedAt,
                note = log.note ?: "",
            ),
        )
    }
    is LogEntry.Diaper -> {
        val log = this.log
        LogEditTarget.Diaper(
            original = log,
            draft = DiaperEditState(
                diaperType = log.diaperType,
                time = log.changedAt,
                note = log.note ?: "",
            ),
        )
    }
    is LogEntry.Sleep -> {
        val log = this.log
        LogEditTarget.Sleep(
            original = log,
            draft = SleepEditState(
                startedAt = log.startedAt,
                endedAt = log.endedAt,
                note = log.note ?: "",
            ),
        )
    }
}

// ── FAQ item composable ───────────────────────────────────────────────────────

@Composable
private fun FaqItem(@StringRes questionRes: Int, @StringRes answerRes: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(questionRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(answerRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
