package com.forex.chart.analysis.ai.ui.screens.premium_analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.forex.chart.analysis.ai.data.manager.InAppReviewManager
import com.forex.chart.analysis.ai.ui.screens.PremiumRatingDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    viewModel: PremiumAnalysisViewModel,
    onBack: () -> Unit,
    onOpenChat: () -> Unit // ✅ NEW: Callback parameter to open chat
) {
    val result by viewModel.analysisResult.collectAsState()
    val showRatingDialog by viewModel.showRatingDialog.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val inAppReviewManager = remember { InAppReviewManager(context) }

    if (result == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B101E)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFFD1A054), strokeWidth = 3.dp, modifier = Modifier.size(50.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Decoding Institutional Data...", color = Color(0xFFD1A054), fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    val data = result!!

    val glowColor = when (data.tradeSignal.uppercase()) {
        "BUY" -> Color(0xFF00E676)
        "SELL" -> Color(0xFFFF3D00)
        else -> Color(0xFFFFC107)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PRO INTELLIGENCE", color = Color(0xFFD1A054), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF080B12))
            )
        },
        containerColor = Color(0xFF080B12),
        // ✅ NEW: Added floating action button to bottom right
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenChat() },
                containerColor = Color(0xFFD1A054), // Gold accent color
                contentColor = Color(0xFF080B12),   // Dark text color
                icon = { Icon(Icons.Default.SmartToy, contentDescription = "Pro Chat") },
                text = { Text("PRO CHAT", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Dynamic background glow effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Brush.verticalGradient(listOf(glowColor.copy(alpha = 0.15f), Color.Transparent)))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Analyzed Chart Image (Premium Border)
                Box(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = data.analyzedImageUrl,
                        contentDescription = "Analyzed Chart",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFFD1A054), Color.Transparent)), RoundedCornerShape(16.dp))
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // 2. Main Signal Card
                    TradeSignalCard(data.tradeSignal, data.confidenceScore, data.timeframe, glowColor)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        RiskProfileBadge(data.riskProfile)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121824)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD1A054).copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Balance, null, tint = Color(0xFFD1A054), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("R/R: ${data.riskRewardRatio}", color = Color(0xFFD1A054), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Entry and Target Zones
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PriceActionCard("ENTRY", data.entryPrice, Icons.Default.Login, Color(0xFF00E676), Modifier.weight(1f))
                        PriceActionCard("TARGET", data.takeProfit, Icons.Default.Flag, Color(0xFFD1A054), Modifier.weight(1f))
                        PriceActionCard("STOP LOSS", data.stopLoss, Icons.Default.GppBad, Color(0xFFFF3D00), Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // 4. Professional Metrics Table
                    SectionHeader(icon = Icons.Default.Analytics, title = "Institutional Metrics")
                    Spacer(modifier = Modifier.height(12.dp))
                    ProMetricsTable(data.detailedMetricsTable)

                    Spacer(modifier = Modifier.height(30.dp))

                    // 5. Tradechart Pro Breakdown
                    SectionHeader(icon = Icons.Default.Psychology, title = "AI Logic Breakdown")
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        data.aiAnalysisReport.forEach { section ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121824)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.width(4.dp).height(IntrinsicSize.Max).background(Color(section.titleColor)))

                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = section.title.uppercase(),
                                            color = Color(section.titleColor),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = section.description,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                    DisclaimerCard()
                    Spacer(modifier = Modifier.height(80.dp)) // ✅ NEW: Added bottom spacing for floating chat button
                }
            }
        }
    }

    if (showRatingDialog) {
        PremiumRatingDialog(
            onDismiss = { viewModel.dismissRatingDialog() },
            onRateSubmit = { stars ->
                viewModel.markUserAsRated()
                if (activity != null) {
                    inAppReviewManager.launchReviewFlow(activity) { success ->
                        if (!success) {
                            inAppReviewManager.openPlayStore(activity)
                        }
                    }
                } else {
                    inAppReviewManager.openPlayStore(null)
                }
            }
        )
    }
}

// ... Remaining components (ProMetricsTable, TradeSignalCard, etc.) ...

@Composable
fun ProMetricsTable(metrics: List<MetricRow>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121824)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFD1A054).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFD1A054).copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("METRIC", color = Color(0xFFD1A054), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("RESULT", color = Color(0xFFD1A054), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
            }
            HorizontalDivider(color = Color(0xFFD1A054).copy(alpha = 0.2f), thickness = 1.dp)

            metrics.forEachIndexed { index, row ->
                val bgColor = if (index % 2 == 0) Color.Transparent else Color(0xFF0F141E)
                Row(
                    modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = row.metricName, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        text = row.resultValue,
                        color = if (row.isHighlight) Color(0xFF00E676) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (row.isHighlight) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1.5f)
                    )
                }
                if (index < metrics.size - 1) HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 1.dp)
            }
        }
    }
}

@Composable
fun TradeSignalCard(signal: String, confidence: Int, timeframe: String, glowColor: Color) {
    val signalIcon = when (signal.uppercase()) {
        "BUY" -> Icons.Default.TrendingUp
        "SELL" -> Icons.Default.TrendingDown
        else -> Icons.Default.Remove
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = glowColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, glowColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(signalIcon, contentDescription = null, tint = glowColor, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("${signal.uppercase()} SIGNAL", color = glowColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Timeframe: $timeframe", color = Color.LightGray, fontSize = 13.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFFD1A054), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("$confidence%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun RiskProfileBadge(risk: String) {
    val (bgColor, textColor, icon) = when {
        risk.contains("Low", ignoreCase = true) -> Triple(Color(0xFF00E676).copy(alpha = 0.2f), Color(0xFF00E676), Icons.Default.Shield)
        risk.contains("High", ignoreCase = true) -> Triple(Color(0xFFFF3D00).copy(alpha = 0.2f), Color(0xFFFF3D00), Icons.Default.Warning)
        else -> Triple(Color(0xFFFFC107).copy(alpha = 0.2f), Color(0xFFFFC107), Icons.Default.Security)
    }

    Card(colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(risk.uppercase(), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun PriceActionCard(title: String, price: String, icon: ImageVector, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121824)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.6f), Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(price, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFFD1A054), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DisclaimerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121824).copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Info, contentDescription = "Disclaimer", tint = Color.Gray.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Disclaimer: The analysis provided by Tradechart Pro Breakdown is for educational and informational purposes only and does not constitute financial advice. Cryptocurrency and stock trading involve significant risk. Always conduct your own research before making any trading decisions.",
                color = Color.Gray.copy(alpha = 0.8f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}