package com.cue.presentation.onboarding.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.cue.domain.model.StudyPlace
import com.cue.presentation.components.OnboardingComponents
import com.cue.presentation.components.OnboardingComponents.StepIndicator
import com.cue.presentation.theme.surface_container_highest
import com.cue.presentation.theme.surface_container_low

@Composable
fun StudyLocationScreen(
    studyPlaces: List<StudyPlace>,
    isAnchoringPlace: Boolean,
    anchorError: String?,
    onAddPlace: (StudyLocation, String) -> Unit,
    onRemovePlace: (StudyPlace) -> Unit,
    onDismissAnchorError: () -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showAddDialog by remember { mutableStateOf<StudyLocation?>(null) }
    var tempLabel by remember { mutableStateOf("") }
    var previousPlaceCount by remember { mutableIntStateOf(studyPlaces.size) }

    LaunchedEffect(studyPlaces.size, isAnchoringPlace, anchorError, showAddDialog) {
        if (
            showAddDialog != null &&
            !isAnchoringPlace &&
            anchorError == null &&
            studyPlaces.size > previousPlaceCount
        ) {
            showAddDialog = null
        }
        previousPlaceCount = studyPlaces.size
    }
    
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
            StepIndicator(currentStep = 1)
            
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Build your",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "academic ",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "sanctuary",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Add anchored locations where you focus best. We'll use these to detect when you're in a study zone.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Category Selection
            Text(
                text = "SELECT TYPE TO ADD ANCHOR",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlaceTypeChip("Home", Icons.Default.Home, Modifier.weight(1f)) {
                    onDismissAnchorError()
                    showAddDialog = StudyLocation.HOME
                    tempLabel = "Home"
                }
                PlaceTypeChip("Cafe", Icons.Default.LocationOn, Modifier.weight(1f)) {
                    onDismissAnchorError()
                    showAddDialog = StudyLocation.CAFE
                    tempLabel = "My Local Cafe"
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlaceTypeChip("Library", Icons.Default.Place, Modifier.weight(1f)) {
                    onDismissAnchorError()
                    showAddDialog = StudyLocation.LIBRARY
                    tempLabel = "University Library"
                }
                PlaceTypeChip("Other", Icons.Default.MoreVert, Modifier.weight(1f)) {
                    onDismissAnchorError()
                    showAddDialog = StudyLocation.OTHER
                    tempLabel = "Quiet Spot"
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Saved Anchors List
            Text(
                text = "SAVED ANCHORS",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (studyPlaces.isEmpty()) {
                EmptyAnchorsView()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    studyPlaces.forEach { place ->
                        SavedPlaceCard(place, onRemovePlace)
                    }
                }
            }
        }

        // Add Place Dialog
        if (showAddDialog != null) {
            AlertDialog(
                onDismissRequest = {
                    onDismissAnchorError()
                    showAddDialog = null
                },
                title = { Text("Anchor this location?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Give this study sanctuary a name. We'll capture your current coarse location as the anchor.")
                        TextField(
                            value = tempLabel,
                            onValueChange = { tempLabel = it },
                            label = { Text("Label") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (anchorError != null) {
                            Text(
                                text = anchorError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isAnchoringPlace,
                        onClick = {
                            onAddPlace(showAddDialog!!, tempLabel)
                        }
                    ) {
                        Text(if (isAnchoringPlace) "ANCHORING..." else "CONFIRM ANCHOR")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        onDismissAnchorError()
                        showAddDialog = null
                    }) {
                        Text("CANCEL")
                    }
                }
            )
        }

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
            OnboardingComponents.GradientButton("Continue", Icons.AutoMirrored.Filled.ArrowForward, onContinue, Modifier.fillMaxWidth().height(64.dp))
        }
    }
}

@Composable
fun PlaceTypeChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = surface_container_low,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SavedPlaceCard(place: StudyPlace, onRemove: (StudyPlace) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surface_container_highest.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when(place.category) {
                        StudyLocation.HOME -> Icons.Default.Home
                        StudyLocation.CAFE -> Icons.Default.LocationOn
                        StudyLocation.LIBRARY -> Icons.Default.Place
                        StudyLocation.OTHER -> Icons.Default.MoreVert
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(place.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Radius: ${place.radiusMeters}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onRemove(place) }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun EmptyAnchorsView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No anchors added yet.", color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Medium)
        }
    }
}
