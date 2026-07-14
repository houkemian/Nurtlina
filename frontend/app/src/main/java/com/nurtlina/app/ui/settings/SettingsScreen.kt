package com.nurtlina.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import com.nurtlina.app.domain.model.*
import com.nurtlina.app.ui.NurtlinaDialog
import com.nurtlina.app.ui.theme.NurtlinaTheme
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/**
 * Stateless Settings screen. State hoisted to ViewModel.
 *
 * @param baby                    Currently selected baby. Null if none added yet.
 * @param settings                Current [UserSettings] values.
 * @param isPro                   Whether the user has an active Pro subscription.
 * @param currentUser             Currently signed-in account. Null while loading.
 * @param notificationPermissionGranted Whether Android allows this app to post notifications.
 * @param appVersion              Displayed in the About section.
 * @param onEditBaby              Reserved for future navigation to a dedicated baby-profile screen.
 * @param onBabyUpdated           User saved baby name and/or birth date from the inline dialog.
 * @param onUnitChanged           User changed volume unit.
 * @param onGuidelineRegionChanged User changed guideline region.
 * @param onLanguageSelected      User selected a language code (e.g. "en", "zh").
 * @param onNotificationsToggled  User toggled notification enable/disable.
 * @param onReminderTimingChanged User changed reminder lead-time in minutes.
 * @param onFeedIntervalChanged   User changed the feeding reminder interval.
 * @param onNightModeToggled      User toggled night mode.
 * @param onManageSubscription    Navigate to Play Store subscription management.
 * @param onUpgradeTapped         Navigate to paywall.
 * @param onExportCsv             Trigger CSV export.
 * @param onBackupClick           Navigate to backup settings.
 * @param onSignInClick           Navigate to sign-in screen.
 * @param onSignOutClick          Sign out the current user.
 * @param onFaqClick              Navigate to FAQ.
 * @param onPrivacyPolicyClick    Open privacy policy URL.
 * @param onTermsClick            Open terms URL.
 * @param onContactSupportClick   Open support email/form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    baby: Baby?,
    settings: UserSettings,
    isPro: Boolean,
    currentUser: UserAccount?,
    notificationPermissionGranted: Boolean = true,
    appVersion: String,
    onEditBaby: () -> Unit = {},
    onBabyUpdated: (name: String, birthDate: LocalDate?) -> Unit,
    onUnitChanged: (UnitType) -> Unit,
    onGuidelineRegionChanged: (GuidelineRegion) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onNotificationsToggled: (Boolean) -> Unit,
    onReminderTimingChanged: (Int) -> Unit,
    onFeedIntervalChanged: (Int) -> Unit,
    onNightModeToggled: (Boolean) -> Unit,
    onManageSubscription: () -> Unit,
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
    var showEditBabyDialog by remember { mutableStateOf(false) }

    if (showSourcesPage) {
        SafeSourcesPage(onBack = { showSourcesPage = false })
        return
    }

    if (showEditBabyDialog) {
        EditBabyDialog(
            currentName = baby?.name.orEmpty(),
            currentBirthDate = baby?.birthDate,
            onSave = { name, birthDate ->
                onBabyUpdated(name, birthDate)
                showEditBabyDialog = false
            },
            onDismiss = { showEditBabyDialog = false },
        )
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
            // ---- Account ----
            SettingsSectionHeader(stringResource(R.string.settings_section_account))
            AccountRow(
                currentUser = currentUser,
                onSignInClick = onSignInClick,
                onSignOutClick = onSignOutClick,
            )

            SettingsDivider()

            // ---- Baby profile ----
            SettingsSectionHeader(stringResource(R.string.settings_section_baby_profile))
            BabyProfileRow(baby = baby, onEditBaby = { showEditBabyDialog = true })

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

            // ---- Preferences ----
            SettingsSectionHeader(stringResource(R.string.settings_section_preferences))
            UnitPickerRow(unit = settings.unit, onUnitChanged = onUnitChanged)
            GuidelineRegionRow(
                region = settings.guidelineRegion,
                onChanged = onGuidelineRegionChanged,
            )
            LanguageRow(
                currentLanguage = settings.language,
                onLanguageSelected = onLanguageSelected,
            )

            SettingsDivider()

            // ---- Notifications ----
            SettingsSectionHeader(stringResource(R.string.settings_section_notifications))
            SettingsToggleRow(
                icon = Icons.Outlined.Notifications,
                label = stringResource(R.string.settings_notifications_enabled_label),
                subtitle = if (settings.notificationEnabled && !notificationPermissionGranted) {
                    stringResource(R.string.settings_notifications_permission_needed)
                } else {
                    null
                },
                checked = settings.notificationEnabled,
                onCheckedChange = onNotificationsToggled,
            )
            if (settings.notificationEnabled && !notificationPermissionGranted) {
                SettingsInfoRow(
                    icon = Icons.Outlined.NotificationsOff,
                    label = stringResource(R.string.settings_notifications_system_status_label),
                    value = stringResource(R.string.settings_notifications_system_status_off),
                )
            }
            if (settings.notificationEnabled) {
                ReminderTimingRow(
                    currentMinutes = settings.reminderBeforeExpiryMinutes,
                    onChanged = onReminderTimingChanged,
                )
                FeedIntervalRow(
                    currentMinutes = settings.feedReminderIntervalMinutes,
                    onChanged = onFeedIntervalChanged,
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

            // ---- About ----
            SettingsSectionHeader(stringResource(R.string.settings_section_about))
            SettingsClickRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
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
    val signedInUser = currentUser?.takeUnless { it.isAnonymous }

    if (signedInUser != null) {
        val displayLabel = signedInUser.displayName
            ?: signedInUser.email
            ?: stringResource(R.string.settings_account_signed_in)

        SettingsInfoRow(
            icon = Icons.Outlined.AccountCircle,
            label = stringResource(R.string.settings_account_signed_in_label),
            value = displayLabel,
        )
        SettingsClickRow(
            icon = Icons.AutoMirrored.Outlined.Logout,
            label = stringResource(R.string.settings_account_sign_out),
            labelColor = MaterialTheme.colorScheme.error,
            onClick = onSignOutClick,
        )
    } else {
        SettingsClickRow(
            icon = Icons.Outlined.AccountCircle,
            label = stringResource(R.string.settings_account_sign_in),
            subtitle = stringResource(R.string.settings_account_sign_in_desc),
            onClick = onSignInClick,
        )
    }
}

// ---------------------------------------------------------------------------
// Baby profile row + edit dialog
// ---------------------------------------------------------------------------

@Composable
private fun BabyProfileRow(baby: Baby?, onEditBaby: () -> Unit) {
    val editCd = stringResource(R.string.settings_edit_baby_cd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
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
                baby.birthDate?.let { bd ->
                    Text(
                        text = stringResource(
                            R.string.settings_baby_birth_date_value,
                            bd.format(java.time.format.DateTimeFormatter.ofLocalizedDate(
                                java.time.format.FormatStyle.MEDIUM
                            )),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBabyDialog(
    currentName: String,
    currentBirthDate: LocalDate?,
    onSave: (name: String, birthDate: LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var birthDate by remember { mutableStateOf(currentBirthDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    NurtlinaDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Outlined.ChildCare,
        title = stringResource(R.string.settings_baby_name_label),
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), birthDate) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_baby_name_label)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Birth date row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = birthDate?.let { bd ->
                        bd.format(java.time.format.DateTimeFormatter.ofLocalizedDate(
                            java.time.format.FormatStyle.MEDIUM
                        ))
                    } ?: stringResource(R.string.settings_baby_birth_date_not_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (birthDate != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = birthDate?.atStartOfDay(java.time.ZoneId.systemDefault())
                    ?.toInstant()?.toEpochMilli(),
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            birthDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }) {
                        Text(stringResource(android.R.string.ok))
                    }
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
    }
}

// ---------------------------------------------------------------------------
// Unit picker
// ---------------------------------------------------------------------------

@Composable
private fun UnitPickerRow(unit: UnitType, onUnitChanged: (UnitType) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val entries = UnitType.entries
    val labels = entries.map { type ->
        when (type) {
            UnitType.ML -> stringResource(R.string.settings_unit_ml)
            UnitType.OZ -> stringResource(R.string.settings_unit_oz)
        }
    }
    val selectedIndex = entries.indexOf(unit).coerceAtLeast(0)

    SettingsClickRow(
        icon = Icons.Outlined.Scale,
        label = stringResource(R.string.settings_unit_label),
        value = labels[selectedIndex],
        onClick = { showDialog = true },
    )

    if (showDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_unit_label),
            options = labels,
            selectedIndex = selectedIndex,
            onSelect = { idx -> onUnitChanged(entries[idx]); showDialog = false },
            onDismiss = { showDialog = false },
            icon = Icons.Outlined.Scale,
        )
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
    var showDialog by remember { mutableStateOf(false) }
    val entries = GuidelineRegion.entries
    val labels = entries.map { it.displayLabel() }
    val selectedIndex = entries.indexOf(region).coerceAtLeast(0)

    SettingsClickRow(
        icon = Icons.Outlined.Public,
        label = stringResource(R.string.settings_guideline_region_label),
        value = labels[selectedIndex],
        onClick = { showDialog = true },
    )

    if (showDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_guideline_region_label),
            options = labels,
            selectedIndex = selectedIndex,
            onSelect = { idx -> onChanged(entries[idx]); showDialog = false },
            onDismiss = { showDialog = false },
            icon = Icons.Outlined.Public,
        )
    }
}

@Composable
private fun GuidelineRegion.displayLabel() = when (this) {
    GuidelineRegion.US -> stringResource(R.string.settings_region_us)
    GuidelineRegion.UK -> stringResource(R.string.settings_region_uk)
    GuidelineRegion.CUSTOM -> stringResource(R.string.settings_region_custom)
}

// ---------------------------------------------------------------------------
// Language picker
// ---------------------------------------------------------------------------

private val supportedLanguages: List<Pair<String, String>> = listOf(
    "en" to "English",
    "es" to "Español",
    "zh" to "中文（简体）",
    "de" to "Deutsch",
    "fr" to "Français",
)

@Composable
private fun LanguageRow(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedIndex = supportedLanguages
        .indexOfFirst { it.first == currentLanguage }
        .coerceAtLeast(0)
    val currentLabel = supportedLanguages[selectedIndex].second

    SettingsClickRow(
        icon = Icons.Outlined.Language,
        label = stringResource(R.string.settings_language_label),
        value = currentLabel,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_language_label),
            options = supportedLanguages.map { it.second },
            selectedIndex = selectedIndex,
            onSelect = { idx ->
                onLanguageSelected(supportedLanguages[idx].first)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            icon = Icons.Outlined.Language,
        )
    }
}

// ---------------------------------------------------------------------------
// Reminder timing picker
// ---------------------------------------------------------------------------

@Composable
private fun ReminderTimingRow(currentMinutes: Int, onChanged: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val options = listOf(
        5 to R.string.settings_reminder_5min,
        10 to R.string.settings_reminder_10min,
        15 to R.string.settings_reminder_15min,
        30 to R.string.settings_reminder_30min,
    )
    val labels = options.map { stringResource(it.second) }
    val selectedIndex = options.indexOfFirst { it.first == currentMinutes }.coerceAtLeast(0)

    SettingsClickRow(
        icon = Icons.Outlined.Timer,
        label = stringResource(R.string.settings_reminder_timing_label),
        value = labels[selectedIndex],
        onClick = { showDialog = true },
    )

    if (showDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_reminder_timing_label),
            options = labels,
            selectedIndex = selectedIndex,
            onSelect = { idx -> onChanged(options[idx].first); showDialog = false },
            onDismiss = { showDialog = false },
            icon = Icons.Outlined.Timer,
        )
    }
}

// ---------------------------------------------------------------------------
// Feed interval picker
// ---------------------------------------------------------------------------

@Composable
private fun FeedIntervalRow(currentMinutes: Int, onChanged: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val options = listOf(
        120 to R.string.settings_feed_interval_120min,
        150 to R.string.settings_feed_interval_150min,
        160 to R.string.settings_feed_interval_160min,
        180 to R.string.settings_feed_interval_180min,
        210 to R.string.settings_feed_interval_210min,
        240 to R.string.settings_feed_interval_240min,
    )
    val labels = options.map { stringResource(it.second) }
    val selectedIndex = options.indexOfFirst { it.first == currentMinutes }.coerceAtLeast(0)

    SettingsClickRow(
        icon = Icons.Outlined.Schedule,
        label = stringResource(R.string.settings_feed_interval_label),
        value = labels[selectedIndex],
        onClick = { showDialog = true },
    )

    if (showDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_feed_interval_label),
            options = labels,
            selectedIndex = selectedIndex,
            onSelect = { idx -> onChanged(options[idx].first); showDialog = false },
            onDismiss = { showDialog = false },
            icon = Icons.Outlined.Schedule,
        )
    }
}

// ---------------------------------------------------------------------------
// Reusable: centered single-choice dialog
// ---------------------------------------------------------------------------

/**
 * Single-choice picker dialog built on [NurtlinaDialog].
 *
 * The selected option is highlighted with [ColorScheme.primaryContainer] and a
 * trailing check-mark. Tapping an option immediately confirms and closes the dialog.
 *
 * @param icon  Icon shown in the header bubble above the title.
 */
@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    NurtlinaDialog(
        onDismissRequest = onDismiss,
        icon = icon,
        title = title,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
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
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
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
            .defaultMinSize(minHeight = 48.dp)
            .clickable { onCheckedChange(!checked) }
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_cancel),
                        )
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
            Text(
                text = stringResource(R.string.settings_sources_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
            onBabyUpdated = { _, _ -> }, onUnitChanged = {}, onGuidelineRegionChanged = {},
            onLanguageSelected = {}, onNotificationsToggled = {}, onReminderTimingChanged = {},
            onFeedIntervalChanged = {}, onNightModeToggled = {}, onManageSubscription = {},
            onUpgradeTapped = {}, onExportCsv = {},
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
            settings = fakeSettings().copy(notificationEnabled = true),
            isPro = true,
            currentUser = UserAccount(uid = "uid1", email = "parent@example.com", isAnonymous = false, familyId = "fam1", isProActive = true),
            appVersion = "1.0.0",
            onBabyUpdated = { _, _ -> }, onUnitChanged = {}, onGuidelineRegionChanged = {},
            onLanguageSelected = {}, onNotificationsToggled = {}, onReminderTimingChanged = {},
            onFeedIntervalChanged = {}, onNightModeToggled = {}, onManageSubscription = {},
            onUpgradeTapped = {}, onExportCsv = {},
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
