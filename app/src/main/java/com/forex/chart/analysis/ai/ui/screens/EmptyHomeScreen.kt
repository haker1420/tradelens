package com.forex.chart.analysis.ai.ui.screens.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forex.chart.analysis.ai.R
import com.forex.chart.analysis.ai.ui.components.AiPoweredBanner
import com.forex.chart.analysis.ai.ui.theme.*

@Composable
fun EmptyHomeScreen(
    isProUser: Boolean, // ✅ Pro Status Received
    onImageUploadClicked: () -> Unit,
    onCameraCaptureClicked: () -> Unit,
    onBannerProClick: () -> Unit
) {
    val context = LocalContext.current

    // ✅ SharedPreferences ইনিশিয়ালাইজ করা হলো
    val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    // ✅ মেমরি থেকে চেক করবে। প্রথমবার true পাবে, ক্লিক করার পর false হয়ে যাবে।
    var showReviewCard by remember {
        mutableStateOf(sharedPreferences.getBoolean("show_review_card", true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .verticalScroll(rememberScrollState()), // Scrollable Container
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.name_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        Spacer(modifier = Modifier.height(35.dp))

        // ✅ Updated Banner Logic
        if (isProUser) {
            // Pro User হলে এটা দেখাবে
            ProUserBanner()
        } else {
            // Free User হলে আগের ব্যানার দেখাবে (ক্লিক করলে পেমেন্ট পপআপ)
            AiPoweredBanner(onProClick = onBannerProClick)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome Text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Text(
                text = "Turn Charts into Profit with AI",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "👋", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Upload Card
        UploadInstructionCard(
            onImageUploadClicked = onImageUploadClicked,
            onCameraCaptureClicked = onCameraCaptureClicked
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Add Review Card Section (মেমরি লজিক সহ)
        if (showReviewCard) {
            AppReviewCard(
                onAddReviewClick = {
                    sharedPreferences.edit().putBoolean("show_review_card", false).apply()
                    showReviewCard = false

                    try {
                        val playStoreUrl = "https://play.google.com/store/apps/details?id=com.forex.chart.analysis.ai"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)).apply {
                            setPackage("com.android.vending")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.forex.chart.analysis.ai"))
                        context.startActivity(intent)
                    }
                },
                onMaybeLaterClick = {
                    sharedPreferences.edit().putBoolean("show_review_card", false).apply()
                    showReviewCard = false
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ✅ Fixed: Added extra space at bottom so buttons are not hidden by Nav Bar
        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ==========================================
// ✅ App Review Card Component
// ==========================================
@Composable
fun AppReviewCard(
    onAddReviewClick: () -> Unit,
    onMaybeLaterClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, ButtonGold.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Review",
                tint = ButtonGold,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enjoying AI Chart Analysis?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your feedback helps us improve\nand serve you better!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextGray,
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onAddReviewClick,
                    modifier = Modifier.height(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Add Review",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Maybe later",
                    color = ButtonBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onMaybeLaterClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ✅ New Component: Banner for Pro Users
@Composable
fun ProUserBanner() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable {
                Toast.makeText(context, "You are a Pro Trader! Enjoy unlimited access.", Toast.LENGTH_SHORT).show()
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00C853), Color(0xFF69F0AE)) // Green Gradient for Pro
                )
            ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Pro User",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Pro Analyst Active",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Unlimited AI Chats & Signals",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun UploadInstructionCard(
    onImageUploadClicked: () -> Unit,
    onCameraCaptureClicked: () -> Unit
) {
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF16293E),
            Color(0xFF0A1625)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(brush = cardGradient)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(vertical = 32.dp, horizontal = 20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF050B13)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Upload your chart image",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite,
                    fontSize = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Supported formats: PNG, JPG, JPEG",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextGray
                )
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onImageUploadClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Upload from Gallery",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCameraCaptureClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ButtonBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = ButtonBlue
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Snap with Camera",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}