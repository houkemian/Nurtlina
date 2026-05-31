package com.nurtlina.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nurtlina.app.R
import com.nurtlina.app.domain.model.*import com.nurtlina.app.ui.theme.NurtlinaTheme

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/**
 * Stateless Settings screen. State hoisted to ViewModel.
 *
 * @param baby              Currently selected baby. Null if none added yet.
 * @param settings          Current [UserSettings] values.
 * @param isPro             Whether the user has an active Pro subscription.
 * @param currentUser       Currently signed-in account. Null while loading.
 * @param appVersion        Displayed in the About section.
 * @param onEditBaby        Navigate to baby profile edit.
 * @param onUnitChanged     User changed volume unit.
 * @param onGuidelineRegionChanged User changed guideline region.
 * @param onLanguageClick   Navigate to language picker.
 * @param onNotificationsToggled User toggled notification enable/disable.
 * @param onReminderTimingChanged User changed reminder lead-time.
 * @param onNightModeToggled User toggled night mode.
 * @param onThemeChanged    User selected a theme.
 * @param onManageSubscription Navigate to Play Store subscription management.
 * @param onRestorePurchases Trigger restore flow.
 * @param onUpgradeTapped   Navigate to paywall.
 * @param onExportCsv       Trigger CSV export.
 * @param onBackupClick     Navigate to backup settings.
 * @param onSignInClick     Navigate to sign-in screen.
 * @param onSignOutClick    Sign out the current user.
 * @param onFaqClick        Navigate to FAQ.
 * @param onPrivacyPolicyClick Open privacy policy URL.
 * @param onTermsClick      Open terms URL.
 * @param onContactSupportClick Open support email/form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    baby: Baby?,
    settings: UserSettings,
    isPro: Boolean,
    currentUser: UserAccount?,
    appVersion: String,
    onEditBaby: () -> Unit,
    onUnitChanged: (UnitType) -> Unit,
    onGuidelineRegionChanged: (GuidelineRegion) -> Unit,
    onLanguageClick: () -> Unit,
    onNotificationsToggled: (Boolean) -> Unit,
    onReminderTimingChanged: (Int) -> Unit,
    onNightModeToggled: (Boolean) -> Unit,
    onThemeChanged: (ThemeType) -> Unit,
    onManageSubscription: () -> Unit,
    onRestorePurchases: () -> Unit,
    onUpgradeTapped: () -> Unit,
    onExportCsv: () -> Unit,
    onBackupClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onFaqClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSourcesPage by remember { mutableStateOf(false) }

    if (showSourcesPage) {
        SafeSourcesPage(onBack = { showSourcesPage = false })
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ---- Baby profile ----
            SettingsSectionHeader(stringResource(R.string.settings_section_baby_profile))
            BabyProfileRow(baby = baby, onEditBaby = onEditBaby)

            SettingsDivider()

            // ---- Preferences ----
            SettingsSectionHeader(stringResource(R.string.settings_section_preferences))
            UnitPickerRow(unit = settings.unit, onUnitChanged = onUnitChanged)
            GuidelineRegionRow(
                region = settings.guidelineRegion,
                onChanged = onGuidelineRegionChanged,
            )
            SettingsClickRow(
                icon = Icons.Outlined.Language,
                label = stringResource(R.string.settings_language_label),
                value = settings.language.uppercase(),
                onClick = onLanguageClick,
            )

            SettingsDivider()

            // ---- Notifications ----
            SettingsSectionHeader(stringResource(R.string.settings_section_notifications))
            SettingsToggleRow(
                icon = Icons.Outlined.Notifications,
                label = stringResource(R.string.settings_notifications_enabled_label),
                checked = settings.notificationEnabled,
                onCheckedChange = onNotificationsToggled,
            )
            if (settings.notificationEnabled) {
                ReminderTimingRow(
                    currentMinutes = settings.reminderBeforeExpiryMinutes,
                    onChanged = onReminderTimingChanged,
                )
            }
            SettingsToggleRow(
                icon = Icons.Outlined.DarkMode,
                label = stringResource(R.string.settings_night_mode_label),
                subtitle = stringResource(R.string.settings_night_mode_desc),
                checked = settings.nightModeEnabled,
                onCheckedChange = onNightModeToggled,
            )

            SettingsDivider()

            // ---- Theme ----
            SettingsSectionHeader(stringResource(R.string.settings_section_theme))
            ThemePickerRow(theme = settings.theme, onChanged = onThemeChanged)

            SettingsDivider()

            // ---- Pro & Billing ----
            SettingsSectionHeader(stringResource(R.string.settings_section_billing))
            SettingsInfoRow(
                icon = if (isPro) Icons.Outlined.Stars else Icons.Outlined.WorkspacePremium,
                label = stringResource(R.string.settings_pro_status_label),
                value = if (isPro) {
                    stringResource(R.string.settings_pro_status_pro)
                } else {
                    stringResource(R.string.settings_pro_status_free)
                },
            )
            if (isPro) {
                SettingsClickRow(
                    icon = Icons.Outlined.CreditCard,
                    label = stringResource(R.string.settings_manage_subscription),
                    onClick = onManageSubscription,
                )
            } else {
                SettingsClickRow(
                    icon = Icons.Outlined.Star,
                    label = stringResource(R.string.settings_upgrade_to_pro),
                    labelColor = MaterialTheme.colorScheme.primary,
                    onClick = onUpgradeTapped,
                )
            }
            SettingsClickRow(
                icon = Icons.Outlined.Refresh,
                label = stringResource(R.string.settings_restore_purchases),
                onClick = onRestorePurchases,
            )

            SettingsDivider()

            // ---- Account ----
            SettingsSectionHeader(stringResource(R.string.settings_section_account))
            AccountRow(
                currentUser = currentUser,
                onSignInClick = onSignInClick,
                onSignOutClick = onSignOutClick,
            )

            SettingsDivider()

            // ---- Data ----
            SettingsSectionHeader(stringResource(R.string.settings_section_data))
            SettingsClickRow(
                icon = Icons.Outlined.Download,
                label = stringResource(R.string.settings_export_csv),
                onClick = onExportCsv,
            )
            SettingsClickRow(
                icon = Icons.Outlined.Backup,
                label = stringResource(R.string.settings_backup_label),
                subtitle = stringResource(R.string.settings_backup_desc),
                onClick = onBackupClick,
            )

            SettingsDivider()

            // ---- About ----
            SettingsSectionHeader(stringResource(R.string.settings_section_about))
            SettingsClickRow(
                icon = Icons.Outlined.HelpOutline,
                label = stringResource(R.string.settings_faq),
                onClick = onFaqClick,
            )
            SettingsClickRow(
                icon = Icons.Outlined.Shield,
                label = stringResource(R.string.settings_safety_sources),
                onClick = { showSourcesPage = true },
            )
            SettingsClickRow(
                icon = Icons.Outlined.PrivacyTip,
                label = stringResource(R.string.settings_privacy_policy),
                onClick = onPrivacyPolicyClick,
            )
            SettingsClickRow(
                icon = Icons.Outlined.Gavel,
                label = stringResource(R.string.settings_terms),
                onClick = onTermsClick,
            )
            SettingsClickRow(
                icon = Icons.Outlined.Email,
                label = stringResource(R.string.settings_contact_support),
                onClick = onContactSupportClick,
            )
            SettingsInfoRow(
                icon = Icons.Outlined.Info,
                label = "",
                value = stringResource(R.string.settings_app_version, appVersion),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Account row
// ---------------------------------------------------------------------------

@Composable
private fun AccountRow(
    currentUser: UserAccount?,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    val isSignedIn = currentUser != null && !currentUser.isAnonymous

    if (isSignedIn && currentUser != null) {
        // Show signed-in account info
        val displayLabel = currentUser.displayName
            ?: currentUser.email
            ?: stringResource(R.string.settings_account_signed_in)

        SettingsInfoRow(
            icon = Icons.Outlined.AccountCircle,
            label = stringResource(R.string.settings_account_signed_in_label),
            value = displayLabel,
        )
        SettingsClickRow(
            icon = Icons.Outlined.Logout,
            label = stringResource(R.string.settings_account_sign_out),
            labelColor = MaterialTheme.colorScheme.error,
            onClick = onSignOutClick,
        )
    } else {
        // Show sign-in prompt
        SettingsClickRow(
            icon = Icons.Outlined.AccountCircle,
            label = stringResource(R.string.settings_account_sign_in),
            subtitle = stringResource(R.string.settings_account_sign_in_desc),
            onClick = onSignInClick,
        )
    }
}

// ---------------------------------------------------------------------------
// Baby profile row
// ---------------------------------------------------------------------------

@Composable
private fun BabyProfileRow(baby: Baby?, onEditBaby: () -> Unit) {
    val editCd = stringResource(R.string.settings_edit_baby_cd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditBaby)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = editCd },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ChildCare,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_baby_name_label),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (baby != null) {
                Text(
                    text = baby.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Unit picker
// ---------------------------------------------------------------------------

@Composable
private fun UnitPickerRow(unit: UnitType, onUnitChanged: (UnitType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = when (unit) {
        UnitType.ML -> stringResource(R.string.settings_unit_ml)
        UnitType.OZ -> stringResource(R.string.settings_unit_oz)
    }

    SettingsClickRow(
        icon = Icons.Outlined.Scale,
        label = stringResource(R.string.settings_unit_label),
        value = currentLabel,
        onClick = { expanded = true },
    )

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        UnitType.entries.forEach { unitType ->
            val label = when (unitType) {
                UnitType.ML -> stringResource(R.string.settings_unit_ml)
                UnitType.OZ -> stringResource(R.string.settings_unit_oz)
            }
            DropdownMenuItem(
                text = { Text(label) },
                onClick = { onUnitChanged(unitType); expanded = false },
                leadingIcon = if (unit == unitType) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Guideline region picker
// ---------------------------------------------------------------------------

@Composable
private fun GuidelineRegionRow(
    region: GuidelineRegion,
    onChanged: (GuidelineRegion) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = region.displayLabel()

    SettingsClickRow(
        icon = Icons.Outlined.Public,
        label = stringResource(R.string.settings_guideline_region_label),
        value = currentLabel,
        onClick = { expanded = true },
    )

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        GuidelineRegion.entries.forEach { r ->
            DropdownMenuItem(
                text = { Text(r.displayLabel()) },
                onClick = { onChanged(r); expanded = false },
                leadingIcon = if (region == r) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )
        }
    }
}

@Composable
private fun GuidelineRegion.displayLabel() = when (this) {
    GuidelineRegion.US -> stringResource(R.string.settings_region_us)
    GuidelineRegion.UK -> stringResource(R.string.settings_region_uk)
    GuidelineRegion.CUSTOM -> stringResource(R.string.settings_region_custom)
}

// ---------------------------------------------------------------------------
// Reminder timing picker
// ---------------------------------------------------------------------------

@Composable
private fun ReminderTimingRow(currentMinutes: Int, onChanged: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(5 to R.string.settings_reminder_5min, 10 to R.string.settings_reminder_10min,
        15 to R.string.settings_reminder_15min, 30 to R.string.settings_reminder_30min)
    val currentLabel = options.firstOrNull { it.first == currentMinutes }?.second
        ?: R.string.settings_reminder_15min

    SettingsClickRow(
        icon = Icons.Outlined.Timer,
        label = stringResource(R.string.settings_reminder_timing_label),
        value = stringResource(currentLabel),
        onClick = { expanded = true },
    )

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (min, labelRes) ->
            DropdownMenuItem(
                text = { Text(stringResource(labelRes)) },
                onClick = { onChanged(min); expanded = false },
                leadingIcon = if (currentMinutes == min) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Theme picker
// ---------------------------------------------------------------------------

@Composable
private fun ThemePickerRow(theme: ThemeType, onChanged: (ThemeType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = theme.displayLabel()

    SettingsClickRow(
        icon = Icons.Outlined.Palette,
        label = stringResource(R.string.settings_theme_label),
        value = currentLabel,
        onClick = { expanded = true },
    )

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        ThemeType.entries.forEach { t ->
            DropdownMenuItem(
                text = { Text(t.displayLabel()) },
                onClick = { onChanged(t); expanded = false },
                leadingIcon = if (theme == t) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )
        }
    }
}

@Composable
private fun ThemeType.displayLabel() = when (this) {
    ThemeType.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeType.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeType.DARK -> stringResource(R.string.settings_theme_dark)
}

// ---------------------------------------------------------------------------
// Reusable row building blocks
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 56.dp, top = 16.dp, bottom = 4.dp, end = 16.dp),
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
}

@Composable
private fun SettingsClickRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String? = null,
    value: String? = null,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Safety Sources sub-page
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SafeSourcesPage(onBack: () -> Unit) {
    val context = LocalContext.current

    data class Source(val labelRes: Int, val urlRes: Int)

    val sources = listOf(
        Source(R.string.settings_source_cdc_label, R.string.settings_source_cdc_url),
        Source(R.string.settings_source_aap_label, R.string.settings_source_aap_url),
        Source(R.string.settings_source_nhs_label, R.string.settings_source_nhs_url),
        Source(R.string.settings_source_cdc_bm_label, R.string.settings_source_cdc_bm_url),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_sources_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Intro text
            Text(
                text = stringResource(R.string.settings_sources_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Source cards
            sources.forEach { source ->
                val label = stringResource(source.labelRes)
                val url = stringResource(source.urlRes)
                val openCd = stringResource(R.string.settings_source_open_link_cd, label)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                            .padding(16.dp)
                            .semantics { contentDescription = openCd },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            HorizontalDivider()

            // Disclaimer block
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.common_not_medical_advice),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_sources_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private fun fakeSettings() = UserSettings()

@Preview(name = "Settings – light", showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    NurtlinaTheme {
        SettingsScreen(
            baby = null,
            settings = fakeSettings(),
            isPro = false,
            currentUser = null,
            appVersion = "1.0.0",
            onEditBaby = {}, onUnitChanged = {}, onGuidelineRegionChanged = {},
            onLanguageClick = {}, onNotificationsToggled = {}, onReminderTimingChanged = {},
            onNightModeToggled = {}, onThemeChanged = {}, onManageSubscription = {},
            onRestorePurchases = {}, onUpgradeTapped = {}, onExportCsv = {},
            onBackupClick = {}, onSignInClick = {}, onSignOutClick = {},
            onFaqClick = {}, onPrivacyPolicyClick = {},
            onTermsClick = {}, onContactSupportClick = {},
        )
    }
}

@Preview(name = "Settings – pro dark", showBackground = true)
@Composable
private fun SettingsScreenProDarkPreview() {
    NurtlinaTheme(darkTheme = true) {
        SettingsScreen(
            baby = null,
            settings = fakeSettings().copy(theme = ThemeType.DARK, notificationEnabled = true),
            isPro = true,
            currentUser = UserAccount(uid = "uid1", email = "parent@example.com", isAnonymous = false, familyId = "fam1", isProActive = true),
            appVersion = "1.0.0",
            onEditBaby = {}, onUnitChanged = {}, onGuidelineRegionChanged = {},
            onLanguageClick = {}, onNotificationsToggled = {}, onReminderTimingChanged = {},
            onNightModeToggled = {}, onThemeChanged = {}, onManageSubscription = {},
            onRestorePurchases = {}, onUpgradeTapped = {}, onExportCsv = {},
            onBackupClick = {}, onSignInClick = {}, onSignOutClick = {},
            onFaqClick = {}, onPrivacyPolicyClick = {},
            onTermsClick = {}, onContactSupportClick = {},
        )
    }
}

@Preview(name = "Safety Sources page – light", showBackground = true)
@Composable
private fun SafeSourcesPagePreview() {
    NurtlinaTheme {
        SafeSourcesPage(onBack = {})
    }
}
