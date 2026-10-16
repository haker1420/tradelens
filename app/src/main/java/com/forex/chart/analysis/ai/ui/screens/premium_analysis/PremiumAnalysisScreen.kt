package com.forex.chart.analysis.ai.ui.screens.premium_analysis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun PremiumAnalysisScreen(
    viewModel: PremiumAnalysisViewModel,
    onAnalysisCompleted: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Multiple image selection state (up to 3)
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(currentStep) {
        if (currentStep != AnalysisStep.IDLE) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (currentStep == AnalysisStep.COMPLETED) {
            onAnalysisCompleted()
        }
    }

    // Multiple image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newImages = (selectedImages + uris).take(3) // Limit to maximum 3 images
        selectedImages = newImages
    }

    // Cyber-Blue background theme
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF060A14), Color(0xFF0A1224))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // === Header Section ===
            Icon(
                imageVector = Icons.Default.DocumentScanner,
                contentDescription = "Scanner",
                tint = Color(0xFF00D4FF),
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Quantum AI Scanner",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Institutional grade SMC & price action",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // === Main Content (Upload or Processing) ===
            if (currentStep == AnalysisStep.IDLE) {
                // 1. Image Preview Section
                if (selectedImages.isNotEmpty()) {
                    ImagePreviewSection(
                        images = selectedImages,
                        onRemove = { uri -> selectedImages = selectedImages.filter { it != uri } }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 2. Upload Box (visible when less than 3 images)
                if (selectedImages.size < 3) {
                    UploadBox { galleryLauncher.launch("image/*") }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 3. Submit Button (visible when at least 1 image is selected)
                AnimatedVisibility(visible = selectedImages.isNotEmpty()) {
                    Button(
                        onClick = {
                            // Send first image to backend API for analysis
                            selectedImages.firstOrNull()?.let { uri ->
                                viewModel.startProAnalysis(uri.toString())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(54.dp),
                        elevation = ButtonDefaults.buttonElevation(8.dp)
                    ) {
                        Text(
                            text = "START INSTITUTIONAL SCAN",
                            color = Color(0xFF060A14),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                // 4. Processing Animation
                ProcessingChecklistCard(currentStep)
            }
        }
    }
}

// ==========================================
// Image preview component
// ==========================================
@Composable
fun ImagePreviewSection(images: List<Uri>, onRemove: (Uri) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(images) { uri ->
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF00D4FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected Chart",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Remove image button
                IconButton(
                    onClick = { onRemove(uri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// Glowing upload box
// ==========================================
@Composable
fun UploadBox(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .scale(scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF000000).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00D4FF), Color.Transparent)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF00D4FF).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF00D4FF), modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Add technical chart", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Select up to 3 clear images", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

// ==========================================
// High-tech checklist animation
// ==========================================
@Composable
fun ProcessingChecklistCard(currentStep: AnalysisStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF000000).copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF00D4FF).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF00D4FF))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Quantum Neural Processing...", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Analysis processing steps
            ChecklistItem("Deep scanning chart structures", currentStep >= AnalysisStep.SCANNING_VISION, currentStep > AnalysisStep.SCANNING_VISION)
            ChecklistItem("Processing institutional order flow", currentStep >= AnalysisStep.ANALYZING_DEEPSEEK, currentStep > AnalysisStep.ANALYZING_DEEPSEEK)
            ChecklistItem("Mapping precision target zones", currentStep >= AnalysisStep.GENERATING_CHART, currentStep > AnalysisStep.GENERATING_CHART)

            // Final step and status message
            ChecklistItem("Finalizing strategy & risk profile", currentStep >= AnalysisStep.FINALIZING, currentStep > AnalysisStep.FINALIZING)

            Spacer(modifier = Modifier.height(20.dp))

            // Note: Analysis may take a moment...
            AnimatedVisibility(visible = currentStep >= AnalysisStep.FINALIZING) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00D4FF).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00D4FF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This final step takes some time because the AI is crafting the best possible high-probability institutional setup for you.",
                            color = Color(0xFF00D4FF),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistItem(text: String, isVisible: Boolean, isCompleted: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(500)),
        exit = fadeOut()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()
        ) {
            if (isCompleted) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
            } else {
                CircularProgressIndicator(color = Color(0xFF00D4FF), strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = if (isCompleted) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}