package com.cue.presentation.onboarding.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cue.domain.model.StudyLocation
import com.cue.presentation.components.Buttons
import com.cue.presentation.theme.surface_container_highest
import com.cue.presentation.theme.surface_container_low

@Composable
fun StudyLocationScreen(
    selectedLocations: List<StudyLocation>,
    onLocationToggle: (StudyLocation) -> Unit,
    onContinue: () -> Unit
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
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 140.dp)
                .verticalScroll(scrollState),
        ) {
            // Step indicator
            StepIndicator(currentStep = 1)
            
            Spacer(modifier = Modifier.height(48.dp))

            // Headline - Intentional Asymmetry
            Text(
                text = "Where do you",
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
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
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
                text = "We'll tailor your academic sanctuary based on your preferred environment.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Choice Grid - Adaptive Layout
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)

            ) {

                
                // Adaptive Row for Home and Cafe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LocationCard(
                        title = "Home",
                        description = "Comfortable and private.",
                        icon = Icons.Default.Home,
                        isSelected = selectedLocations.contains(StudyLocation.HOME),
                        onClick = { onLocationToggle(StudyLocation.HOME) },
                        modifier = Modifier.weight(1f)
                    )
                    LocationCard(
                        title = "Cafe",
                        description = "Energetic and lively.",
                        icon = Icons.Default.LocationOn,
                        isSelected = selectedLocations.contains(StudyLocation.CAFE),
                        onClick = { onLocationToggle(StudyLocation.CAFE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Option: Other (Full Width)
                    LocationCard(
                        title = "Other",
                        description = "Any other sanctuary.",
                        icon = Icons.Default.MoreVert,
                        isSelected = selectedLocations.contains(StudyLocation.OTHER),
                        onClick = { onLocationToggle(StudyLocation.OTHER) },
                        modifier = Modifier.weight(1f)
                    )

                    // Option: Library (Full Width Anchor)
                    LocationCard(
                        title = "Library",
                        description = "Quiet and peaceful.",
                        icon = Icons.Default.Place,
                        isSelected = selectedLocations.contains(StudyLocation.LIBRARY),
                        onClick = { onLocationToggle(StudyLocation.LIBRARY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Fixed Footer Action with Gradient Background for readability
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
fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index + 1 <= currentStep) MaterialTheme.colorScheme.primary 
                        else surface_container_highest
                    )
            )
        }
    }
}

@Composable
fun LocationCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else Color.Transparent
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(surface_container_low.copy(alpha = 0.4f))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else surface_container_highest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseOnSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Custom Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

