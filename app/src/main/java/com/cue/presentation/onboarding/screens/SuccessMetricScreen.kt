package com.cue.presentation.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cue.domain.model.SuccessMetric
import com.cue.presentation.components.Buttons

@Composable
fun SuccessMetricScreen(
    selectedMetric: SuccessMetric?,
    onMetricSelect: (SuccessMetric) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 140.dp)
        ) {
            StepIndicator(currentStep = 3)

            Spacer(modifier = Modifier.height(48.dp))

            // Headline
            Text(
                text = "What does study",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "success ",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "mean for",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )


            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "you?",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Define your personal milestone so we can tailor your focus recommendations.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Metrics Selection
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(
                    metric = SuccessMetric.COMPLETING_TASK,
                    icon = Icons.Default.CheckCircle,
                    isSelected = selectedMetric == SuccessMetric.COMPLETING_TASK,
                    onClick = { onMetricSelect(SuccessMetric.COMPLETING_TASK) }
                )
                MetricCard(
                    metric = SuccessMetric.TIME_DURATION,
                    icon = Icons.Default.Lock,
                    isSelected = selectedMetric == SuccessMetric.TIME_DURATION,
                    onClick = { onMetricSelect(SuccessMetric.TIME_DURATION) }
                )
                MetricCard(
                    metric = SuccessMetric.NO_DISTRACTION,
                    icon = Icons.Default.Clear,
                    isSelected = selectedMetric == SuccessMetric.NO_DISTRACTION,
                    onClick = { onMetricSelect(SuccessMetric.NO_DISTRACTION) }
                )
            }
        }

        // Fixed Footer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f
                    )
                )
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            Buttons.GradientButton("Complete Setup", Icons.AutoMirrored.Filled.ArrowForward, onComplete, Modifier.fillMaxWidth().height(64.dp))
        }
    }
}

@Composable
fun MetricCard(
    metric: SuccessMetric,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                    else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f)
            )
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(24.dp)
            .size(100.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(
                modifier = Modifier

                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metric.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Radio Indicator
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(24.dp)
                    .border(
                        2.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
