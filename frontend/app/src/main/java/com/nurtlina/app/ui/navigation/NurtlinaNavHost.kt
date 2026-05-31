package com.nurtlina.app.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nurtlina.app.R
import com.nurtlina.app.core.notification.FeedReminderConfig
import com.nurtlina.app.data.billing.EntitlementManager
import com.nurtlina.app.data.billing.ProStatus
import com.nurtlina.app.domain.model.BottleTransition
import com.nurtlina.app.domain.model.DiaperType
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.TodaySummary
import com.nurtlina.app.domain.model.UnitType
import com.nurtlina.app.domain.model.UserSettings
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.ui.bottle.BottleDetailScreen
import com.nurtlina.app.ui.bottle.BottleDetailUiState
import com.nurtlina.app.ui.bottle.BottleUiEvent
import com.nurtlina.app.ui.bottle.BottleViewModel
import com.nurtlina.app.ui.bottle.NewBottleSheet
import com.nurtlina.app.ui.bottle.NewBottleUiState
import com.nurtlina.app.ui.insights.InsightsDateRange
import com.nurtlina.app.ui.insights.InsightsScreen
import com.nurtlina.app.ui.logs.LogEntry
import com.nurtlina.app.ui.logs.LogItem
import com.nurtlina.app.ui.logs.LogsScreen
import com.nurtlina.app.ui.logs.LogsViewModel
import com.nurtlina.app.ui.onboarding.OnboardingScreen
import com.nurtlina.app.ui.onboarding.OnboardingViewModel
import com.nurtlina.app.ui.auth.SignInScreen
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

// ── App-level ViewModel ──────────────────────────────────────────────────────

/**
 * Lightweight ViewModel scoped to the activity that resolves the start
 * destination and exposes app-wide Pro / sync state before the NavHost
 * is first composed.
 *
 * Emits `null` for [onboardingComplete] while the settings DataStore is
 * still loading, which causes [NurtlinaNavHost] to render an empty
 * placeholder rather than flash the wrong screen.
 */
@HiltViewModel
internal class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    entitlementManager: EntitlementManager,
) : ViewModel() {

    /** `null` = loading, `true` = completed, `false` = not yet completed. */
    val onboardingComplete: StateFlow<Boolean?> = settingsRepository
        .observe()
        .map { it.onboardingCompleted }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    /** `true` when the user holds an active Pro subscription or lifetime purchase. */
    val isPro: StateFlow<Boolean> = entitlementManager.proStatus
        .map { it != ProStatus.FREE && it != ProStatus.UNKNOWN }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false,
        )
}

// ── Navigation host ──────────────────────────────────────────────────────────

private const val PRIVACY_URL = "https://nurtlina.app/privacy"
private const val TERMS_URL = "https://nurtlina.app/terms"
private const val PLAY_STORE_SUBS_URL =
    "https://play.google.com/store/account/subscriptions?package=com.nurtlina.app"

/**
 * Root composable that owns the [NavHostController] and the app-level
 * [Scaffold].  The bottom navigation bar is visible only while the user is
 * on a main-tab destination (Today / Logs / Insights / Settings).
 *
 * Screen composables are wired here; each delegates its own ViewModel
 * retrieval to [hiltViewModel] so each back-stack entry gets a correctly
 * scoped instance.
 */
@Composable
fun NurtlinaNavHost(modifier: Modifier = Modifier) {

    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val onboardingComplete by appViewModel.onboardingComplete.collectAsStateWithLifecycle()
    val isPro by appViewModel.isPro.collectAsStateWithLifecycle()

    // Hold off rendering until settings are loaded to avoid a destination flash.
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

            // ── Bottle screens ───────────────────────────────────────────

            composable(NavRoutes.NewBottle.route) {
                NewBottleRoute(navController)
            }

            composable(
                route = NavRoutes.BottleDetail.route,
                arguments = listOf(
                    navArgument(NavRoutes.BottleDetail.ARG_BOTTLE_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                BottleDetailRoute(navController)
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
            // Permission prompting is handled later; core onboarding remains non-blocking.
        },
    )
}

@Composable
private fun TodayRoute(navController: NavController, isPro: Boolean) {
    val viewModel: TodayViewModel = hiltViewModel()
    val babies by viewModel.babies.collectAsStateWithLifecycle()
    val selectedBaby by viewModel.selectedBaby.collectAsStateWithLifecycle()
    val activeBottles by viewModel.activeBottles.collectAsStateWithLifecycle()
    val latestFeed by viewModel.latestFeed.collectAsStateWithLifecycle()
    val todaySummary by viewModel.todaySummary.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(Instant.now()) }
    val activeBottle = activeBottles.firstOrNull()
    val summary = todaySummary ?: emptyTodaySummary()
    val unitType = UnitType.ML

    LaunchedEffect(Unit) {
        while (true) {
            delay(if (FeedReminderConfig.DEBUG_NEXT_FEED_REMINDER_ENABLED) 1_000L else 60_000L)
            now = Instant.now()
        }
    }

    LaunchedEffect(latestFeed?.id, latestFeed?.startedAt) {
        latestFeed?.let(viewModel::scheduleNextFeedReminder)
    }

    TodayScreen(
        state = TodayUiState(
            babies = babies,
            selectedBaby = selectedBaby,
            feedingStatus = buildFeedingStatus(
                babyName = selectedBaby?.name,
                latestFeed = latestFeed,
                summary = summary,
                unitType = unitType,
                now = now,
            ),
            activeBottle = activeBottle,
            countdownText = formatRemaining(activeBottle?.expiresAt),
            isExpiringSoon = isExpiringSoon(activeBottle?.expiresAt),
            todaySummary = summary,
            showAds = !isPro,
            nightModeEnabled = false,
            unitType = unitType,
        ),
        onSelectBaby = { viewModel.selectBaby(it.id) },
        onAddBaby = {
            if (isPro) {
                // Pro: navigate directly to baby creation (future screen)
            } else {
                navController.navigate(NavRoutes.Paywall.route)
            }
        },
        onNewBottle = { navController.navigate(NavRoutes.NewBottle.route) },
        onStartFeeding = { viewModel.transitionBottle(it, BottleTransition.StartFeeding) },
        onRefrigerate = { viewModel.transitionBottle(it, BottleTransition.Refrigerate) },
        onDiscard = { viewModel.transitionBottle(it, BottleTransition.Discard) },
        onMarkFed = { viewModel.transitionBottle(it, BottleTransition.MarkFed) },
        onBottleDetail = { navController.navigate(NavRoutes.BottleDetail.createRoute(it.id)) },
        onQuickFeed = { amountMl -> viewModel.quickLogFeed(amountMl) },
        onQuickDiaper = { viewModel.quickLogDiaper(DiaperType.WET) },
        onQuickSleep = {
            if (todaySummary?.activeSleepStartedAt == null) {
                viewModel.startSleep()
            } else {
                viewModel.endSleep()
            }
        },
    )
}

@Composable
private fun NewBottleRoute(navController: NavController) {
    val bottleViewModel: BottleViewModel = hiltViewModel()
    val todayViewModel: TodayViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val selectedBaby by todayViewModel.selectedBaby.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var sheetState by remember { mutableStateOf(NewBottleUiState()) }

    LaunchedEffect(bottleViewModel) {
        bottleViewModel.uiEvents.collect { event ->
            when (event) {
                is BottleUiEvent.NavigateToDetail -> {
                    navController.navigate(NavRoutes.BottleDetail.createRoute(event.bottleId)) {
                        popUpTo(NavRoutes.NewBottle.route) { inclusive = true }
                    }
                }
                is BottleUiEvent.ShowError -> Unit
            }
        }
    }

    Box(Modifier.fillMaxSize())
    NewBottleSheet(
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
            val baby = selectedBaby ?: return@NewBottleSheet
            bottleViewModel.createBottle(
                babyId = baby.id,
                milkType = sheetState.milkType,
                amountMl = sheetState.amountMl,
                preparedAt = if (sheetState.isJustNow) Instant.now() else sheetState.preparedAt,
                guidelineRegion = settings?.guidelineRegion ?: GuidelineRegion.US,
                note = sheetState.note.ifBlank { null },
            )
        },
        onDismiss = { navController.popBackStack() },
    )
}

@Composable
private fun BottleDetailRoute(navController: NavController) {
    val viewModel: BottleViewModel = hiltViewModel()
    val bottle by viewModel.bottle.collectAsStateWithLifecycle()

    BottleDetailScreen(
        state = BottleDetailUiState(
            bottle = bottle,
            countdownText = formatRemaining(bottle?.expiresAt),
            elapsedText = "",
            isExpired = bottle?.expiresAt?.isBefore(Instant.now()) == true,
            isExpiringSoon = isExpiringSoon(bottle?.expiresAt),
            guidelineSourceName = guidelineSourceName(bottle?.guidelineRegion),
            unitType = UnitType.ML,
        ),
        onBack = { navController.popBackStack() },
        onStartFeeding = { viewModel.transitionBottle(BottleTransition.StartFeeding) },
        onRefrigerate = { viewModel.transitionBottle(BottleTransition.Refrigerate) },
        onMarkFed = { viewModel.transitionBottle(BottleTransition.MarkFed) },
        onDiscard = { viewModel.transitionBottle(BottleTransition.Discard) },
        onEditPreparedTime = {},
    )
}

@Composable
private fun LogsRoute() {
    val viewModel: LogsViewModel = hiltViewModel()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val logItems by viewModel.logItems.collectAsStateWithLifecycle()

    LogsScreen(
        selectedDate = selectedDate,
        entries = logItems.map { it.toLogEntry() },
        activeFilter = selectedFilter,
        useOz = false,
        onPrevDay = viewModel::goToPreviousDay,
        onNextDay = viewModel::goToNextDay,
        onPickDate = {},
        onFilterSelected = viewModel::setFilter,
        onEntryClick = {},
        onEntryDelete = { entry ->
            when (entry) {
                is LogEntry.Bottle -> viewModel.deleteBottle(entry.id)
                is LogEntry.Feed -> viewModel.deleteFeedLog(entry.id)
                is LogEntry.Diaper -> viewModel.deleteDiaperLog(entry.id)
                is LogEntry.Sleep -> viewModel.deleteSleepLog(entry.id)
            }
        },
    )
}

@Composable
private fun InsightsRoute(navController: NavController, isPro: Boolean) {
    val todayViewModel: TodayViewModel = hiltViewModel()
    val todaySummary by todayViewModel.todaySummary.collectAsStateWithLifecycle()
    var selectedRange by remember { mutableStateOf(InsightsDateRange.SEVEN) }

    InsightsScreen(
        todaySummary = todaySummary ?: emptyTodaySummary(),
        isPro = isPro,
        useOz = false,
        selectedRange = selectedRange,
        proData = null,
        onRangeSelected = { selectedRange = it },
        onUpgradeTapped = { navController.navigate(NavRoutes.Paywall.route) },
    )
}

@Composable
private fun SettingsRoute(navController: NavController, isPro: Boolean) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val babies by viewModel.babies.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentSettings = settings ?: UserSettings()
    val selectedBaby = babies.firstOrNull { it.id == currentSettings.selectedBabyId }
        ?: babies.firstOrNull()

    SettingsScreen(
        baby = selectedBaby,
        settings = currentSettings,
        isPro = isPro,
        currentUser = currentUser,
        appVersion = "1.0.0",
        onEditBaby = {},
        onUnitChanged = viewModel::updateUnitType,
        onGuidelineRegionChanged = viewModel::updateGuidelineRegion,
        onLanguageClick = {},
        onNotificationsToggled = viewModel::updateNotificationEnabled,
        onReminderTimingChanged = {},
        onNightModeToggled = {},
        onThemeChanged = viewModel::updateTheme,
        onManageSubscription = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_SUBS_URL))
            )
        },
        onRestorePurchases = { viewModel.restorePurchases() },
        onUpgradeTapped = { navController.navigate(NavRoutes.Paywall.route) },
        onExportCsv = {},
        onBackupClick = { viewModel.requestFullSync() },
        onSignInClick = { navController.navigate(NavRoutes.SignIn.route) },
        onSignOutClick = { viewModel.signOut() },
        onFaqClick = {},
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
    is LogItem.BottleLog -> LogEntry.Bottle(bottle)
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

// Soft home-screen prompt for interval awareness; this is not a safety or medical rule.
private val feedAttentionInterval: Duration
    get() = FeedReminderConfig.nextFeedAttentionInterval

private fun buildFeedingStatus(
    babyName: String?,
    latestFeed: FeedLog?,
    summary: TodaySummary,
    unitType: UnitType,
    now: Instant,
): FeedingStatusUiState {
    val elapsedSinceLastFeed = latestFeed?.startedAt?.let { startedAt ->
        val elapsed = Duration.between(startedAt, now)
        if (elapsed.isNegative) Duration.ZERO else elapsed
    }
    val nextFeedRemaining = elapsedSinceLastFeed
        ?.takeIf { it < feedAttentionInterval }
        ?.let { feedAttentionInterval.minus(it) }

    return FeedingStatusUiState(
        babyName = babyName.orEmpty(),
        lastFeedTimeText = latestFeed?.startedAt?.let(::formatTime),
        lastFeedAgoText = elapsedSinceLastFeed?.let(::formatCompactDuration),
        lastFeedAmountText = latestFeed?.amountMl?.let { formatAmount(it, unitType) },
        todayFeedCount = summary.totalFeedCount,
        todayAmountText = formatAmount(summary.totalAmountMl, unitType),
        nextFeedInMillis = nextFeedRemaining?.toMillis(),
        isNextFeedDue = elapsedSinceLastFeed?.let { it >= feedAttentionInterval } == true,
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

private fun formatRemaining(expiresAt: Instant?): String {
    if (expiresAt == null) return ""
    val remaining = Duration.between(Instant.now(), expiresAt)
    if (remaining.isNegative || remaining.isZero) return "Expired"
    val hours = remaining.toHours()
    val minutes = remaining.minusHours(hours).toMinutes().coerceAtLeast(1)
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun isExpiringSoon(expiresAt: Instant?): Boolean {
    if (expiresAt == null) return false
    val remaining = Duration.between(Instant.now(), expiresAt)
    return !remaining.isNegative && remaining <= Duration.ofMinutes(15)
}

private fun guidelineSourceName(region: GuidelineRegion?): String = when (region) {
    GuidelineRegion.US -> "CDC"
    GuidelineRegion.UK -> "NHS"
    GuidelineRegion.CUSTOM -> "Custom"
    null -> ""
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
                        // Pop up to the start destination to avoid building up a
                        // large back stack when re-selecting tabs.
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
