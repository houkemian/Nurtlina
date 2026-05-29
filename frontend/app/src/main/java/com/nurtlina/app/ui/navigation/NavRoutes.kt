package com.nurtlina.app.ui.navigation

/**
 * Single source of truth for all navigation routes.
 *
 * Onboarding screens form a linear flow; the graph route [OnboardingGraph] is
 * used as a pop-up target so the entire onboarding back-stack is cleared when
 * the user enters the main app for the first time.
 *
 * [BottleDetail.createRoute] builds the concrete route string from a runtime
 * [bottleId], while [BottleDetail.route] is the pattern registered in the NavHost.
 */
sealed class NavRoutes(val route: String) {

    // ── Onboarding graph ────────────────────────────────────────────────────

    /** Nested-navigation graph that wraps the entire onboarding flow. */
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

    // ── Bottle screens ──────────────────────────────────────────────────────

    object NewBottle : NavRoutes("bottle/new")

    object BottleDetail : NavRoutes("bottle/{bottleId}") {

        const val ARG_BOTTLE_ID = "bottleId"

        /** Build the concrete navigation route for a known bottle ID. */
        fun createRoute(bottleId: String): String = "bottle/$bottleId"
    }

    // ── Paywall ─────────────────────────────────────────────────────────────

    object Paywall : NavRoutes("paywall")
}

/** Routes that render with the bottom navigation bar visible. */
val mainTabRoutes: Set<String> = setOf(
    NavRoutes.Today.route,
    NavRoutes.Logs.route,
    NavRoutes.Insights.route,
    NavRoutes.Settings.route,
)
