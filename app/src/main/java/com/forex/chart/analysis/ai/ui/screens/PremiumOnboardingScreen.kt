package com.forex.chart.analysis.ai.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forex.chart.analysis.ai.R
import com.forex.chart.analysis.ai.data.manager.AdMobManager
import com.forex.chart.analysis.ai.ui.screens.home.HomeViewModel
import com.forex.chart.analysis.ai.ui.theme.AppBackground
import com.forex.chart.analysis.ai.ui.theme.ButtonGold
import com.forex.chart.analysis.ai.ui.theme.TextGray
import com.forex.chart.analysis.ai.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun PremiumOnboardingScreen(
    viewModel: HomeViewModel,
    adMobManager: AdMobManager,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // --- ৫ সেকেন্ড টাইমার এবং প্রগ্রেস স্টেট ---
    var timeLeft by remember { mutableIntStateOf(5) }
    var progress by remember { mutableFloatStateOf(1f) }
    val isTimerFinished = timeLeft <= 0

    // টাইমার লজিক: ৫ সেকেন্ড কাউন্টডাউন
    LaunchedEffect(Unit) {
        val totalTime = 5000L // ৫ সেকেন্ড
        val step = 50L // প্রতি ৫০ মিলিসেকেন্ডে প্রগ্রেস আপডেট হবে
        var elapsed = 0L

        while (elapsed < totalTime) {
            delay(step)
            elapsed += step
            // প্রগ্রেস ১.০ থেকে ০.০ এর দিকে কমবে
            progress = 1f - (elapsed.toFloat() / totalTime)
            // সেকেন্ড আপডেট (৫, ৪, ৩...)
            timeLeft = (5 - (elapsed / 1000)).toInt()
        }
        timeLeft = 0
        progress = 0f
    }

    // ইউজার প্রো হলে সরাসরি হোমে পাঠিয়ে দেওয়া
    LaunchedEffect(uiState.isProUser) {
        if (uiState.isProUser) {
            onNavigateToHome()
        }
    }

    var selectedPlanId by remember { mutableStateOf(viewModel.pendingPlanId ?: "yearly_pro_plan") }

    val selectedPrice = when (selectedPlanId) {
        "weekly_pro_plan" -> uiState.weeklyPrice
        "monthly_pro_plan" -> uiState.monthlyPrice
        else -> uiState.yearlyPrice
    }

    val planDurationText = when (selectedPlanId) {
        "weekly_pro_plan" -> "week"
        "monthly_pro_plan" -> "month"
        else -> "year"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- ১. টপ বার (টাইমার এবং ক্লোজ বাটন) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable(enabled = isTimerFinished) {
                        if (isTimerFinished) {
                            activity?.let {
                                adMobManager.showInterstitialAd(it, uiState.isProUser) {
                                    onNavigateToHome()
                                }
                            } ?: onNavigateToHome()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!isTimerFinished) {
                    // গোল প্রগ্রেস বার
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = ButtonGold,
                        strokeWidth = 2.dp,
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )
                    // বাকি থাকা সেকেন্ড
                    Text(
                        text = "$timeLeft",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // ৫ সেকেন্ড শেষ হলে 'X' আইকন দেখাবে
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ২. শিরোনাম (Titles) ---
        Text(
            text = buildAnnotatedString {
                append("Maximize your ")
                withStyle(style = SpanStyle(color = ButtonGold)) {
                    append("Profit")
                }
            },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                fontSize = 28.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Subscribe to get unlimited\nchart analysis.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = TextWhite.copy(alpha = 0.8f),
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- ৩. অ্যাপ প্রিভিউ ইমেজ ---
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.mobile),
                contentDescription = "App Preview",
                modifier = Modifier.fillMaxHeight(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ৪. প্রাইসিং প্ল্যানস (Horizontal Scroll) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlanCardOnboarding(
                title = "Weekly",
                price = uiState.weeklyPrice,
                isSelected = selectedPlanId == "weekly_pro_plan",
                onClick = { selectedPlanId = "weekly_pro_plan" }
            )
            PlanCardOnboarding(
                title = "Monthly",
                price = uiState.monthlyPrice,
                isSelected = selectedPlanId == "monthly_pro_plan",
                onClick = { selectedPlanId = "monthly_pro_plan" }
            )
            PlanCardOnboarding(
                title = "Yearly",
                price = uiState.yearlyPrice,
                isSelected = selectedPlanId == "yearly_pro_plan",
                onClick = { selectedPlanId = "yearly_pro_plan" }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- ৫. পেমেন্ট টেক্সট ---
        Text(
            text = "Billed $selectedPrice per $planDurationText",
            color = TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- ৬. কন্টিনিউ বাটন ---
        Button(
            onClick = {
                activity?.let { viewModel.triggerPurchase(it, selectedPlanId) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGold),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Continue",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun PlanCardOnboarding(
    title: String,
    price: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) ButtonGold else Color.White.copy(alpha = 0.05f)
    val bgColor = if (isSelected) Color(0xFF1C222D) else Color(0xFF12161E)

    Card(
        modifier = Modifier
            .width(115.dp)
            .height(90.dp)
            .clickable { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = price,
                color = TextWhite.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}