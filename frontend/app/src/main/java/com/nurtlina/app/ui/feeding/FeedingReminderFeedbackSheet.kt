package com.nurtlina.app.ui.feeding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nurtlina.app.R
import com.nurtlina.app.domain.model.FeedbackType
import com.nurtlina.app.ui.theme.NurtlinaTheme

/**
 * Shown after a feeding reminder notification is tapped.
 * Lets the user tell us whether the window timing was helpful.
 */
@Composable
fun FeedingReminderFeedbackSheet(
    onFeedback: (FeedbackType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.ThumbUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.feeding_feedback_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeedbackButton(
                    label = stringResource(R.string.feeding_feedback_too_early),
                    icon = Icons.Outlined.Timer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onFeedback(FeedbackType.TOO_EARLY) },
                )
                FeedbackButton(
                    label = stringResource(R.string.feeding_feedback_just_right),
                    icon = Icons.Outlined.ThumbUp,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onFeedback(FeedbackType.JUST_RIGHT) },
                )
                FeedbackButton(
                    label = stringResource(R.string.feeding_feedback_too_late),
                    icon = Icons.Outlined.Schedule,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onFeedback(FeedbackType.TOO_LATE) },
                )
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    }
}

@Composable
private fun FeedbackButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Preview(name = "Feedback – light", showBackground = true)
@Composable
private fun FeedbackPreview() {
    NurtlinaTheme {
        FeedingReminderFeedbackSheet(
            onFeedback = {},
            onDismiss = {},
        )
    }
}
