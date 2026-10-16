package com.forex.chart.analysis.ai.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.forex.chart.analysis.ai.data.manager.BillingManager
import com.forex.chart.analysis.ai.data.manager.AdMobManager
import com.forex.chart.analysis.ai.data.model.AnalysisResponse
import com.forex.chart.analysis.ai.navigation.NavRoute

// ✅ Alias imports
import com.forex.chart.analysis.ai.ui.screens.analysis.AnalysisResultScreen as StandardResultScreen
import com.forex.chart.analysis.ai.ui.screens.premium_analysis.AnalysisResultModel
import com.forex.chart.analysis.ai.ui.screens.premium_analysis.PremiumAnalysisViewModel

import com.forex.chart.analysis.ai.ui.screens.chat.ChatViewModel
import com.forex.chart.analysis.ai.ui.screens.history.HistoryViewModel
import com.forex.chart.analysis.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = viewModel(),
    premiumViewModel: PremiumAnalysisViewModel,
    chatViewModel: ChatViewModel,
    onToggleBottomBar: (Boolean) -> Unit
) {
    val historyList by viewModel.historyList.collectAsState()
    val proHistoryList by viewModel.proHistoryList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val billingManager = remember { BillingManager(context) }
    val adMobManager = remember { AdMobManager(context) }

    // ✅ Pro Status চেক
    val isProUser by billingManager.isProUser.collectAsState(initial = false)
    val prices by billingManager.productPrices.collectAsState(initial = emptyMap())

    val weeklyPrice = prices["weekly_pro_plan"] ?: "Loading..."
    val monthlyPrice = prices["monthly_pro_plan"] ?: "Loading..."
    val yearlyPrice = prices["yearly_pro_plan"] ?: "Loading..." // ✅ NEW: ইয়ারলি প্রাইস অ্যাড করা হলো

    // ফ্রি ইউজারদের জন্য
    var selectedAnalysis by remember { mutableStateOf<AnalysisResponse?>(null) }

    LaunchedEffect(selectedAnalysis) {
        onToggleBottomBar(selectedAnalysis == null)
    }

    // ==========================================
    // ফ্রি ইউজারদের ইনলাইন রেজাল্ট স্ক্রিন
    // ==========================================
    if (selectedAnalysis != null && !isProUser) {
        BackHandler { selectedAnalysis = null }

        StandardResultScreen(
            data = selectedAnalysis!!,
            localImageUri = null,
            isChatLocked = true, // ফ্রি ইউজার তাই চ্যাট লক থাকবে
            weeklyPrice = weeklyPrice,
            monthlyPrice = monthlyPrice,
            yearlyPrice = yearlyPrice, // ✅ NEW: ডায়ালগে পাঠানোর জন্য ইয়ারলি প্রাইস দেওয়া হলো
            onChatClicked = {
                selectedAnalysis?.let { analysis ->
                    chatViewModel.startSession(analysis, newSession = true)
                    navController.navigate(NavRoute.CHAT_SESSION)
                }
            },
            onBackClicked = { selectedAnalysis = null },
            onBuyPro = { planId ->
                val activity = context as? Activity
                if (activity != null) {
                    // ✅ NEW: when ব্লক দিয়ে সব প্ল্যান কেনার লজিক সাজানো হলো
                    when {
                        planId.contains("weekly") -> billingManager.purchaseWeeklyPlan(activity)
                        planId.contains("monthly") -> billingManager.purchaseMonthlyPlan(activity)
                        planId.contains("yearly") -> billingManager.purchaseYearlyPlan(activity)
                    }
                }
            }
        )

    } else {
        // ==========================================
        // মেইন হিস্ট্রি স্ক্রিন (লিস্ট ভিউ)
        // ==========================================
        Column(
            modifier = Modifier.fillMaxSize().background(AppBackground).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // প্রো হলে আইকন ও টেক্সট সাইবার-ব্লু হবে, ফ্রি হলে গোল্ডেন
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = if (isProUser) Color(0xFF00D4FF) else ButtonGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isProUser) "Pro Scans History" else "Analysis History",
                    style = MaterialTheme.typography.headlineSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (isProUser) Color(0xFF00D4FF) else ButtonGold)
                }
            } else if (isProUser) {
                // ==========================================
                // প্রো ইউজার লিস্ট (Cyber-Blue)
                // ==========================================
                if (proHistoryList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No pro scans yet.", color = Color.Gray) }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(proHistoryList) { proItem ->
                            ProHistoryItemCard(item = proItem) {
                                val activity = context as? Activity
                                if (activity != null) {
                                    adMobManager.showInterstitialAd(activity, isProUser) {
                                        premiumViewModel.setHistoryResult(proItem)
                                        navController.navigate(NavRoute.PREMIUM_RESULT)
                                    }
                                } else {
                                    premiumViewModel.setHistoryResult(proItem)
                                    navController.navigate(NavRoute.PREMIUM_RESULT)
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // ফ্রি ইউজার লিস্ট (Golden)
                // ==========================================
                if (historyList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No history yet.", color = Color.Gray) }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(historyList) { item ->
                            HistoryItemCard(item = item) {
                                val activity = context as? Activity
                                if (activity != null) {
                                    adMobManager.showInterstitialAd(activity, isProUser) {
                                        selectedAnalysis = item
                                    }
                                } else {
                                    selectedAnalysis = item
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ফ্রি ইউজারদের হিস্ট্রি কার্ড (আগের মতোই থাকবে)
// ==========================================
@Composable
fun HistoryItemCard(item: AnalysisResponse, onClick: () -> Unit) {
    val prediction = item.data?.prediction
    val score = prediction?.getScoreInt() ?: 0
    val signal = prediction?.signal ?: "N/A"
    val dateString = remember(item.createdAt) { item.createdAt?.let { SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(it) } ?: "Just now" }
    val statusColor = when { score >= 70 -> Color(0xFF00C853); score >= 40 -> Color(0xFFFFAB00); else -> Color(0xFFD50000) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        score >= 60 -> Icons.AutoMirrored.Filled.TrendingUp
                        score <= 40 -> Icons.Default.TrendingDown
                        else -> Icons.Default.TrendingFlat
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = signal, style = MaterialTheme.typography.titleMedium.copy(color = TextWhite, fontWeight = FontWeight.Bold), maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = dateString, style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "$score%", style = MaterialTheme.typography.titleLarge.copy(color = statusColor, fontWeight = FontWeight.Bold))
                Text(text = "Score", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp))
            }
        }
    }
}

// ==========================================
// প্রো ইউজারদের হিস্ট্রি কার্ড (Cyber-Blue Look)
// ==========================================
@Composable
fun ProHistoryItemCard(item: AnalysisResultModel, onClick: () -> Unit) {
    val signal = item.tradeSignal.uppercase()
    val statusColor = when (signal) {
        "BUY" -> Color(0xFF00E676)
        "SELL" -> Color(0xFFFF3D00)
        else -> Color(0xFFFFC107)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1224)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00D4FF).copy(alpha = 0.3f), RoundedCornerShape(16.dp)).clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (signal) {
                        "BUY" -> Icons.AutoMirrored.Filled.TrendingUp
                        "SELL" -> Icons.Default.TrendingDown
                        else -> Icons.Default.TrendingFlat
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "$signal SIGNAL", style = MaterialTheme.typography.titleMedium.copy(color = statusColor, fontWeight = FontWeight.Bold), maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Quantum AI • ${item.timeframe}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF00D4FF).copy(alpha = 0.8f)))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${item.confidenceScore}%", style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
                Text(text = "Confidence", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp))
            }
        }
    }
}