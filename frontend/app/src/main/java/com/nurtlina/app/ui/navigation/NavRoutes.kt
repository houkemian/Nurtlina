package com.nurtlina.app.ui.navigation

/**
 * Single source of truth for all navigation routes.
 */
sealed class NavRoutes(val route: String) {

    // ── Onboarding graph ────────────────────────────────────────────────────

    object OnboardingGraph : NavRoutes("onboarding_graph")

    object Welcome : NavRoutes("onboarding/welcome")
    object CreateBaby : NavRoutes("onboarding/create_baby")
    object GuidelineSelect : NavRoutes("onboarding/guideline_select")
    object Disclaimer : NavRoutes("onboarding/disclaimer")
    object NotificationPermission : NavRoutes("onboarding/notification_permission")

    // ── Main bottom-tab destinations ────────────────────────────────────────

    object Today : NavRoutes("main/today")
    object Logs : NavRoutes("main/logs")
    object Insights : NavRoutes("main/insights")
    object Settings : NavRoutes("main/settings")

    // ── Feed screens ────────────────────────────────────────────────────────

    object NewFeed : NavRoutes("feed/new")

    // ── Paywall ─────────────────────────────────────────────────────────────

    object Paywall : NavRoutes("paywall")

    // ── Sign-in ──────────────────────────────────────────────────────────────

    object SignIn : NavRoutes("auth/sign_in")
}

/** Routes that render with the bottom navigation bar visible. */
val mainTabRoutes: Set<String> = setOf(
    NavRoutes.Today.route,
    NavRoutes.Logs.route,
    NavRoutes.Insights.route,
    NavRoutes.Settings.route,
)
