package com.nurtlina.app.ui.onboarding

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nurtlina.app.R
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.ui.theme.NurtlinaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val AvatarColorOptions = listOf(
    "TEAL" to Color(0xFF009FA6),
    "PEACH" to Color(0xFFDB5189),
    "LAVENDER" to Color(0xFF9C7EC4),
    "AMBER" to Color(0xFFE8A020),
    "SAGE" to Color(0xFF4D9664),
)

private enum class OnboardingStep { WELCOME, CREATE_BABY, GUIDELINE, DISCLAIMER, NOTIFICATIONS }

private val OnboardingStep.displayIndex: Int get() = ordinal + 1
private const val TOTAL_STEPS = 5

/** Transient state for the baby creation step. */
data class OnboardingBabyInput(
    val name: String = "",
    val birthDate: LocalDate? = null,
    val avatarColor: String = "TEAL",
)

/**
 * Multi-step onboarding flow.
 *
 * Stateless in terms of persistence — callers (ViewModel / NavGraph) must persist
 * [OnboardingBabyInput] and [GuidelineRegion] on completion.
 *
 * @param onComplete Called when the user finishes all steps. Receives the baby
 *   input and selected region so the caller can persist them.
 * @param onRequestNotificationPermission Called when the user taps "Allow Notifications"
 *   on the final step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (babyInput: OnboardingBabyInput, region: GuidelineRegion) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    var babyInput by remember { mutableStateOf(OnboardingBabyInput()) }
    var selectedRegion by rememberSaveable { mutableStateOf(GuidelineRegion.US) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            if (step != OnboardingStep.WELCOME) {
                StepIndicator(
                    currentStepIndex = step.ordinal,
                    totalSteps = TOTAL_STEPS,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
                modifier = Modifier.weight(1f),
                label = "onboarding_step_transition",
            ) { currentStep ->
                when (currentStep) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        onGetStarted = { step = OnboardingStep.CREATE_BABY },
                    )
                    OnboardingStep.CREATE_BABY -> CreateBabyStep(
                        input = babyInput,
                        onInputChange = { babyInput = it },
                        onNext = { step = OnboardingStep.GUIDELINE },
                        onBack = { step = OnboardingStep.WELCOME },
                    )
                    OnboardingStep.GUIDELINE -> GuidelineRegionStep(
                        selected = selectedRegion,
                        onSelect = { selectedRegion = it },
                        onNext = { step = OnboardingStep.DISCLAIMER },
                        onBack = { step = OnboardingStep.CREATE_BABY },
                    )
                    OnboardingStep.DISCLAIMER -> DisclaimerStep(
                        onAccept = { step = OnboardingStep.NOTIFICATIONS },
                        onBack = { step = OnboardingStep.GUIDELINE },
                    )
                    OnboardingStep.NOTIFICATIONS -> NotificationPermissionStep(
                        onAllow = {
                            onRequestNotificationPermission()
                            onComplete(babyInput, selectedRegion)
                        },
                        onNotNow = { onComplete(babyInput, selectedRegion) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentStepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStepIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isActive) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

// --------------------------------------------------------------------------
// Step 1 – Welcome
// --------------------------------------------------------------------------

@Composable
private fun WelcomeStep(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = stringResource(R.string.onboarding_get_started),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

// --------------------------------------------------------------------------
// Step 2 – Create Baby
// --------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBabyStep(
    input: OnboardingBabyInput,
    onInputChange: (OnboardingBabyInput) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_create_baby_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_create_baby_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = input.name,
            onValueChange = { onInputChange(input.copy(name = it)) },
            label = { Text(stringResource(R.string.onboarding_name_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_name_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        Spacer(Modifier.height(16.dp))

        val birthDateText = input.birthDate?.toString() ?: ""
        OutlinedTextField(
            value = birthDateText,
            onValueChange = {},
            label = { Text(stringResource(R.string.onboarding_birth_date_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_birth_date_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            readOnly = true,
            enabled = false,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = stringResource(R.string.onboarding_birth_date_label),
                    )
                }
            },
        )
        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.onboarding_pick_color),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AvatarColorOptions.forEach { (key, color) ->
                val isSelected = input.avatarColor == key
                val cdLabel = stringResource(R.string.cd_avatar_color, key)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    shape = CircleShape,
                                )
                            } else Modifier,
                        )
                        .clickable { onInputChange(input.copy(avatarColor = key)) }
                        .semantics { contentDescription = cdLabel },
                ) {}
            }
        }

        Spacer(Modifier.height(40.dp))
        OnboardingNavButtons(
            onBack = onBack,
            onNext = onNext,
            nextEnabled = input.name.isNotBlank(),
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = input.birthDate
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onInputChange(input.copy(birthDate = date))
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

// --------------------------------------------------------------------------
// Step 3 – Guideline Region
// --------------------------------------------------------------------------

@Composable
private fun GuidelineRegionStep(
    selected: GuidelineRegion,
    onSelect: (GuidelineRegion) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    data class RegionOption(
        val region: GuidelineRegion,
        val titleRes: Int,
        val descRes: Int,
    )

    val options = remember {
        listOf(
            RegionOption(GuidelineRegion.US, R.string.onboarding_region_us, R.string.onboarding_region_us_desc),
            RegionOption(GuidelineRegion.UK, R.string.onboarding_region_uk, R.string.onboarding_region_uk_desc),
            RegionOption(GuidelineRegion.CUSTOM, R.string.onboarding_region_custom, R.string.onboarding_region_custom_desc),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_region_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_region_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options.forEach { option ->
                val isSelected = selected == option.region
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(option.region) },
                            role = Role.RadioButton,
                        ),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(option.titleRes),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(option.descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        OnboardingNavButtons(onBack = onBack, onNext = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

// --------------------------------------------------------------------------
// Step 4 – Disclaimer
// --------------------------------------------------------------------------

@Composable
private fun DisclaimerStep(
    onAccept: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_disclaimer_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text(
                text = stringResource(R.string.disclaimer_full_text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(20.dp),
            )
        }
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(stringResource(R.string.onboarding_accept))
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_back))
        }
        Spacer(Modifier.height(24.dp))
    }
}

// --------------------------------------------------------------------------
// Step 5 – Notification Permission
// --------------------------------------------------------------------------

@Composable
private fun NotificationPermissionStep(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_notification_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_notification_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(56.dp))
        Button(
            onClick = onAllow,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = stringResource(R.string.onboarding_allow_notifications),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onNotNow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_not_now))
        }
    }
}

// --------------------------------------------------------------------------
// Shared nav buttons row
// --------------------------------------------------------------------------

@Composable
private fun OnboardingNavButtons(
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(stringResource(R.string.onboarding_back))
        }
        Button(
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier
                .weight(2f)
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(stringResource(R.string.onboarding_next))
        }
    }
}

// --------------------------------------------------------------------------
// Previews
// --------------------------------------------------------------------------

@Preview(name = "Onboarding – Welcome (Light)", showBackground = true)
@Composable
private fun PreviewOnboardingLight() {
    NurtlinaTheme {
        OnboardingScreen(
            onComplete = { _, _ -> },
            onRequestNotificationPermission = {},
        )
    }
}

@Preview(
    name = "Onboarding – Welcome (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewOnboardingDark() {
    NurtlinaTheme(darkTheme = true) {
        OnboardingScreen(
            onComplete = { _, _ -> },
            onRequestNotificationPermission = {},
        )
    }
}
