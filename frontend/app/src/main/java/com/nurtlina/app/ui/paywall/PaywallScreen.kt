package com.nurtlina.app.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nurtlina.app.R
import com.nurtlina.app.ui.theme.NurtlinaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onClose: () -> Unit,
    onSubscribeMonthly: () -> Unit,
    onSubscribeYearly: () -> Unit,
    onBuyLifetime: () -> Unit,
    onRestorePurchases: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTerms: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // Features
            ProFeaturesList()

            Spacer(Modifier.height(24.dp))

            // Pricing options
            var selectedPlan by remember { mutableStateOf("yearly") }
            PricingOption(
                isSelected = selectedPlan == "monthly",
                title = stringResource(R.string.paywall_monthly_title),
                price = stringResource(R.string.paywall_monthly_price),
                badge = null,
                onClick = { selectedPlan = "monthly" },
            )
            Spacer(Modifier.height(8.dp))
            PricingOption(
                isSelected = selectedPlan == "yearly",
                title = stringResource(R.string.paywall_yearly_title),
                price = stringResource(R.string.paywall_yearly_price),
                badge = stringResource(R.string.paywall_best_value),
                onClick = { selectedPlan = "yearly" },
            )
            Spacer(Modifier.height(8.dp))
            PricingOption(
                isSelected = selectedPlan == "lifetime",
                title = stringResource(R.string.paywall_lifetime_title),
                price = stringResource(R.string.paywall_lifetime_price),
                badge = null,
                onClick = { selectedPlan = "lifetime" },
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    when (selectedPlan) {
                        "monthly" -> onSubscribeMonthly()
                        "yearly" -> onSubscribeYearly()
                        "lifetime" -> onBuyLifetime()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    stringResource(R.string.paywall_subscribe_cta),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRestorePurchases) {
                Text(stringResource(R.string.paywall_restore))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.paywall_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onPrivacyPolicy) {
                    Text(stringResource(R.string.settings_privacy), style = MaterialTheme.typography.bodySmall)
                }
                Text(" · ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
                TextButton(onClick = onTerms) {
                    Text(stringResource(R.string.settings_terms), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProFeaturesList() {
    val features = listOf(
        Icons.Default.Block to R.string.paywall_no_ads,
        Icons.Default.FamilyRestroom to R.string.paywall_multi_baby,
        Icons.Default.History to R.string.paywall_full_history,
        Icons.Default.Download to R.string.paywall_export,
        Icons.Default.Cloud to R.string.paywall_backup,
        Icons.Default.Notifications to R.string.paywall_custom_reminders,
        Icons.Default.Widgets to R.string.paywall_widget_themes,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            features.forEach { (icon, labelRes) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.weight(0.1f))
                    Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PricingOption(
    isSelected: Boolean,
    title: String,
    price: String,
    badge: String?,
    onClick: () -> Unit,
) {
    val cardColors = if (isSelected) {
        CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.outlinedCardColors()
    }
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    if (badge != null) {
                        Spacer(Modifier.weight(0.05f))
                        Badge { Text(badge, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Text(price, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaywallPreview() {
    NurtlinaTheme {
        PaywallScreen(
            onClose = {},
            onSubscribeMonthly = {},
            onSubscribeYearly = {},
            onBuyLifetime = {},
            onRestorePurchases = {},
            onPrivacyPolicy = {},
            onTerms = {},
        )
    }
}
