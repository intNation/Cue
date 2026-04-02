package com.cue.presentation.onboarding.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cue.domain.model.DaySchedule
import com.cue.presentation.components.Buttons

@Composable
fun StudyScheduleScreen(
    weeklySchedule: List<DaySchedule>,
    onScheduleChange: (List<DaySchedule>) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    
    // Local state to track expanded cards for editing times
    var expandedDay by remember { mutableStateOf<Int?>(null) }

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
            StepIndicator(currentStep = 2)
            
            Spacer(modifier = Modifier.height(48.dp))

            // Headline
            Text(
                text = "When do you",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "usually ",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "study?",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Set custom hours for specific days, or keep it flexible. We'll adjust focus recommendations accordingly.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Study Days Multi-select
            Text(
                text = "STUDY DAYS",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { index, day ->
                    val dayNum = index + 1
                    val isSelected = weeklySchedule.any { it.dayOfWeek == dayNum }
                    
                    DayButton(
                        text = day,
                        isSelected = isSelected,
                        onClick = {
                            val newList = if (isSelected) {
                                weeklySchedule.filter { it.dayOfWeek != dayNum }
                            } else {
                                weeklySchedule + DaySchedule(dayOfWeek = dayNum)
                            }
                            onScheduleChange(newList)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Weekly Schedule List
            Text(
                text = "WEEKLY SCHEDULE",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (weeklySchedule.isEmpty()) {
                EmptySchedulePlaceholder()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    weeklySchedule.sortedBy { it.dayOfWeek }.forEach { schedule ->
                        ScheduleDayCard(
                            schedule = schedule,
                            isExpanded = expandedDay == schedule.dayOfWeek,
                            onExpandToggle = {
                                expandedDay = if (expandedDay == schedule.dayOfWeek) null else schedule.dayOfWeek
                            },
                            onTimeChange = { start, end ->
                                val newList = weeklySchedule.map { 
                                    if (it.dayOfWeek == schedule.dayOfWeek) it.copy(startTime = start, endTime = end, isFlexible = false) 
                                    else it 
                                }
                                onScheduleChange(newList)
                            },
                            onMakeFlexible = {
                                val newList = weeklySchedule.map { 
                                    if (it.dayOfWeek == schedule.dayOfWeek) it.copy(isFlexible = true, startTime = null, endTime = null) 
                                    else it 
                                }
                                onScheduleChange(newList)
                            }
                        )
                    }
                }
            }
        }

        // Fixed Footer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.9f), MaterialTheme.colorScheme.background),
                        startY = 0f
                    )
                )
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            Buttons.GradientButton("Continue", Icons.AutoMirrored.Filled.ArrowForward, onContinue, Modifier.fillMaxWidth().height(64.dp))
        }
    }
}

@Composable
fun DayButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .clickable(onClick = onClick)
            .then(
                if (!isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ScheduleDayCard(
    schedule: DaySchedule,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onTimeChange: (String, String) -> Unit,
    onMakeFlexible: () -> Unit
) {
    val dayName = when(schedule.dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
            .border(1.dp, if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (schedule.isFlexible) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName.take(1),
                        fontWeight = FontWeight.Bold,
                        color = if (schedule.isFlexible) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = dayName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (schedule.isFlexible) "Anytime / Flexible" else "${schedule.startTime} – ${schedule.endTime}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (schedule.isFlexible) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        fontWeight = if (schedule.isFlexible) FontWeight.Normal else FontWeight.Medium
                    )
                }
            }

            TextButton(
                onClick = { onExpandToggle() },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = if (schedule.isFlexible) "ADD TIMES" else "EDIT",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (schedule.isFlexible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = if (schedule.isFlexible) Icons.Default.Add else if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TimeInputField(
                        label = "START TIME",
                        value = schedule.startTime ?: "08:00",
                        modifier = Modifier.weight(1f),
                        onValueChange = { onTimeChange(it, schedule.endTime ?: "12:00") }
                    )
                    TimeInputField(
                        label = "END TIME",
                        value = schedule.endTime ?: "12:00",
                        modifier = Modifier.weight(1f),
                        onValueChange = { onTimeChange(schedule.startTime ?: "08:00", it) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Make Flexible",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onMakeFlexible(); onExpandToggle() }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )
        // Simplified time input for the prototype
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptySchedulePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select study days above to build your schedule",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontStyle = FontStyle.Italic
        )
    }
}
