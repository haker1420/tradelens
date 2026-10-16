package com.forex.chart.analysis.ai.ui.screens.analysis

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.forex.chart.analysis.ai.data.model.AnalysisResponse
import com.forex.chart.analysis.ai.data.model.DetailItem
import com.forex.chart.analysis.ai.data.manager.AdMobManager
import com.forex.chart.analysis.ai.data.manager.InAppReviewManager
import com.forex.chart.analysis.ai.ui.components.AiPoweredBanner
import com.forex.chart.analysis.ai.ui.components.PremiumPlanDialog
import com.forex.chart.analysis.ai.ui.screens.PremiumRatingDialog
import com.forex.chart.analysis.ai.ui.theme.*

@Composable
fun AnalysisResultScreen(
    data: AnalysisResponse,
    localImageUri: Uri?,
    isChatLocked: Boolean,
    weeklyPrice: String,
    monthlyPrice: String,
    yearlyPrice: String,
    showRatingDialog: Boolean = false,
    onDismissRatingDialog: () -> Unit = {},
    onMarkUserAsRated: () -> Unit = {},
    onChatClicked: () -> Unit,
    onBackClicked: () -> Unit,
    onBuyPro: (String) -> Unit
) {
    var isImageVisible by remember { mutableStateOf(true) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showAdClarificationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    val isAnonymous = remember { FirebaseAuth.getInstance().currentUser?.isAnonymous ?: true }

    val adMobManager = remember { AdMobManager(context) }
    val inAppReviewManager = remember { InAppReviewManager(context) }

    val isPremium = !isChatLocked

    val imageModel = remember(localImageUri, data.imageUrl) {
        if (localImageUri != null && localImageUri != Uri.EMPTY && localImageUri.toString().isNotEmpty()) {
            localImageUri
        } else if (!data.imageUrl.isNullOrEmpty()) {
            data.imageUrl
        } else {
            null
        }
    }

    BackHandler {
        if (activity != null) {
            adMobManager.showInterstitialAd(activity, isPremium) {
                onBackClicked()
            }
        } else {
            onBackClicked()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            Surface(color = AppBackground, shadowElevation = 10.dp) {
                if (isChatLocked) {
                    // ✅ Free User: Blue button animated like a heartbeat ("animate this button like heard")
                    val infiniteTransition = rememberInfiniteTransition(label = "ButtonHeartbeatAnimation")
                    val buttonScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ButtonScale"
                    )

                    Button(
                        onClick = { showAdClarificationDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                            .graphicsLayer {
                                scaleX = buttonScale
                                scaleY = buttonScale
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonBlue
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Free Chat Expert",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // ✅ Pro User: Instant Chat Access
                    Button(
                        onClick = { onChatClicked() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat with AI Assistant", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                if(isChatLocked) {
                    AiPoweredBanner(onProClick = { showPremiumDialog = true })
                } else {
                    Spacer(modifier = Modifier.height(40.dp))
                }

                IconButton(
                    onClick = {
                        if (activity != null) {
                            adMobManager.showInterstitialAd(activity, isPremium) {
                                onBackClicked()
                            }
                        } else {
                            onBackClicked()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-8).dp)
                        .background(AppBackground.copy(alpha = 0.7f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
            }

            // Image Preview Section
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isImageVisible = !isImageVisible }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isImageVisible) "Hide Chart" else "View Chart",
                        color = ButtonGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isImageVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ButtonGold
                    )
                }

                AnimatedVisibility(visible = isImageVisible, enter = expandVertically(), exit = shrinkVertically()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(250.dp)
                            .border(1.dp, ButtonGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        if (imageModel != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Uploaded Chart",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = TextGray, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Chart image not available", color = TextGray)
                                }
                            }
                        }
                    }
                }
            }

            // Score Meter Logic
            data.data?.prediction?.let { prediction ->
                val score = prediction.getScoreInt()
                val sig = prediction.signal.lowercase()
                val risk = prediction.riskLevel.lowercase()
                val conf = prediction.confidenceLevel.lowercase()

                val signalColor = when {
                    sig.contains("buy") -> Color(0xFF00E676)
                    sig.contains("sell") -> Color(0xFFFF3D00)
                    else -> Color(0xFFFFC400)
                }
                val riskColor = when {
                    risk.contains("low") -> Color(0xFF00E676)
                    risk.contains("medium") || risk.contains("moderate") -> Color(0xFFFFC400)
                    else -> Color(0xFFFF3D00)
                }
                val confColor = when {
                    conf.contains("high") -> Color(0xFF00E676)
                    conf.contains("medium") -> Color(0xFFFFC400)
                    else -> Color(0xFFFF3D00)
                }

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI CONFIDENCE SCORE", style = TextStyle(color = TextWhite.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp))
                    Spacer(modifier = Modifier.height(12.dp))

                    PredictionMeter(score = score, signal = prediction.signal, signalColor = signalColor)

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        BadgeInfo("Risk: ${prediction.riskLevel}", riskColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        BadgeInfo("Conf: ${prediction.confidenceLevel}", confColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Key Insights
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionHeader("KEY INSIGHTS", Icons.Default.Analytics)
                Spacer(modifier = Modifier.height(16.dp))
                val insights = data.data?.keyInsights

                Row(Modifier.fillMaxWidth()) {
                    val trendColor = if (insights?.trend?.lowercase()?.contains("bull") == true) Color(0xFF00E676) else if (insights?.trend?.lowercase()?.contains("bear") == true) Color(0xFFFF3D00) else ButtonGold
                    GridItem(Modifier.weight(1f), "Trend", insights?.trend, Icons.AutoMirrored.Filled.TrendingUp, trendColor)
                    Spacer(Modifier.width(10.dp))
                    GridItem(Modifier.weight(1f), "Momentum", insights?.momentumStrength, Icons.Default.Speed, Color(0xFF00E5FF))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    GridItem(Modifier.weight(1f), "Liq. Bias", insights?.liquidityBias, Icons.Default.WaterDrop, Color(0xFF76FF03))
                    Spacer(Modifier.width(10.dp))
                    GridItem(Modifier.weight(1f), "Sentiment", insights?.marketSentiment, Icons.Default.Psychology, Color(0xFFFF4081))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Game Plan
            InfoSection("AI GAME PLAN", Icons.Default.SportsScore, data.data?.gamePlan ?: "Analyzing...", ButtonGold)

            // Risk Management
            data.data?.riskManagement?.let { risk ->
                InfoCardGroup("RISK MANAGEMENT", Icons.Default.Shield) {
                    InfoRow("RR Ratio", risk.rrRatio, Color(0xFF69F0AE))
                    InfoRow("Stop Loss", risk.slZone, Color(0xFFFF5252))
                    InfoRow("Position Size", risk.sizing, Color(0xFF00E5FF))
                }
            }

            // Multi-Timeframe
            data.data?.multiTimeframe?.let { mtf ->
                InfoCardGroup("MULTI-TIMEFRAME", Icons.Default.Schedule) {
                    InfoRow("Weekly", mtf.weekly, ButtonGold)
                    InfoRow("Daily", mtf.daily, ButtonGold)
                    InfoRow("4H Struct", mtf.h4, Color(0xFFFF4081))
                    InfoRow("1H Struct", mtf.h1, Color(0xFFFFA000))
                }
            }

            // Institutional
            data.data?.institutional?.let { smc ->
                InfoCardGroup("INSTITUTIONAL (SMC)", Icons.Default.AccountBalance) {
                    InfoRow("FVG", smc.fvg, Color(0xFF00E5FF))
                    InfoRow("Bullish OB", smc.orderBlocks?.bullish ?: "-", Color(0xFF76FF03))
                    InfoRow("Bearish OB", smc.orderBlocks?.bearish ?: "-", Color(0xFFFF5252))
                    InfoRow("Buy-Side Liq", smc.liquidity?.buySide ?: "-", TextGray)
                    InfoRow("Sell-Side Liq", smc.liquidity?.sellSide ?: "-", TextGray)
                }
            }

            // Detailed Analysis
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader("DETAILED BREAKDOWN", Icons.AutoMirrored.Filled.ListAlt)
                Spacer(modifier = Modifier.height(16.dp))

                val icons = listOf(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    Icons.Default.Layers,
                    Icons.Default.BarChart,
                    Icons.Default.CandlestickChart,
                    Icons.Default.Speed
                )

                data.data?.detailedAnalysis?.forEachIndexed { index, item ->
                    val icon = icons.getOrElse(index) { Icons.Default.Info }
                    ExpandableDetailCard(item = item, icon = icon)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ✅ NEW: Disclaimer Section Added Here
                DisclaimerSection()

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showPremiumDialog) {
        PremiumPlanDialog(
            weeklyPrice = weeklyPrice,
            monthlyPrice = monthlyPrice,
            yearlyPrice = yearlyPrice,
            isAnonymous = isAnonymous,
            onDismiss = {
                showPremiumDialog = false
                if (activity != null) {
                    adMobManager.showInterstitialAd(activity, isPremium) {}
                }
            },
            onLoginRequest = { planId ->
                onBuyPro(planId)
                showPremiumDialog = false
            },
            onSubscribe = { planId ->
                onBuyPro(planId)
                showPremiumDialog = false
            }
        )
    }

    if (showRatingDialog) {
        PremiumRatingDialog(
            onDismiss = { onDismissRatingDialog() },
            onRateSubmit = { stars ->
                onMarkUserAsRated()
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

    if (showAdClarificationDialog) {
        WatchAdClarificationDialog(
            onDismiss = { showAdClarificationDialog = false },
            onWatchAdClicked = {
                showAdClarificationDialog = false
                if (activity != null) {
                    adMobManager.showRewardedAd(activity, isPremium = false) {
                        onChatClicked()
                    }
                } else {
                    onChatClicked()
                }
            },
            onUpgradeProClicked = {
                showAdClarificationDialog = false
                showPremiumDialog = true
            }
        )
    }
}

// ==========================================
// ✅ Disclaimer Component
// ==========================================
@Composable
fun DisclaimerSection() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0C0C)), // Subtle dark red tint
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "DISCLAIMER: This AI analysis is for educational purposes only and does not constitute financial advice. Trading involves high risk. Always do your own research.",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

// ... Helper Functions
@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ButtonGold, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, style = TextStyle(color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.2.sp))
    }
}

@Composable
fun PredictionMeter(
    score: Int,
    signal: String,
    signalColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 1500), label = "Gauge"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.size(170.dp)) {
            Canvas(modifier = Modifier.size(160.dp)) {
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 25f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = signalColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 25f, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    style = TextStyle(
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "/100",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = TextWhite.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = signalColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, signalColor.copy(alpha = 0.6f))
        ) {
            Text(
                text = signal.uppercase(),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = TextStyle(
                    color = signalColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
fun OverviewItem(title: String, value: String, icon: @Composable () -> Unit, color: Color = ButtonBlue, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), modifier = modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { icon() }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title.uppercase(), style = TextStyle(color = TextWhite.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = value, style = TextStyle(color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
            }
        }
    }
}

@Composable
fun ExpandableDetailCard(item: DetailItem, icon: ImageVector) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).clickable { expanded = !expanded }.animateContentSize()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = ButtonGold, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = item.title, style = TextStyle(color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
                }
                Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextWhite.copy(alpha = 0.7f))
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = formatAnalysisText(item.description),
                    style = TextStyle(color = Color(0xFFF5F5F5), lineHeight = 24.sp, fontSize = 15.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal)
                )
            }
        }
    }
}

@Composable
fun GridItem(modifier: Modifier, title: String, value: String?, icon: ImageVector, color: Color) {
    OverviewItem(title = title, value = value ?: "N/A", icon = { Icon(icon, null, tint = color) }, color = color, modifier = modifier)
}

@Composable
fun BadgeInfo(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.4f))) {
        Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = TextStyle(color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
    }
}

@Composable
fun InfoSection(title: String, icon: ImageVector, content: String, color: Color) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        SectionHeader(title, icon)
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
            Text(
                text = formatAnalysisText(content),
                modifier = Modifier.padding(20.dp),
                style = TextStyle(color = TextWhite, lineHeight = 26.sp, fontSize = 16.sp, letterSpacing = 0.3.sp, fontWeight = FontWeight.Normal)
            )
        }
    }
}

@Composable
fun InfoCardGroup(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        SectionHeader(title, icon)
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
            Column(Modifier.padding(20.dp)) { content() }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = TextStyle(color = TextWhite.copy(alpha = 0.6f), fontSize = 15.sp))
        Text(value, style = TextStyle(color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp), maxLines = 1)
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
}

fun formatAnalysisText(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val words = text.split(" ")
        words.forEach { word ->
            val cleanWord = word.replace(Regex("[^A-Za-z0-9-]"), "")
            val color = when {
                cleanWord.equals("Buy", true) || cleanWord.equals("Long", true) || cleanWord.equals("Bullish", true) || cleanWord.equals("Profit", true) || cleanWord.equals("Target", true) -> Color(0xFF00E676)
                cleanWord.equals("Sell", true) || cleanWord.equals("Short", true) || cleanWord.equals("Bearish", true) || cleanWord.equals("Stop", true) || cleanWord.equals("Loss", true) -> Color(0xFFFF5252)
                cleanWord.equals("Entry", true) || cleanWord.equals("Wait", true) || cleanWord.equals("Neutral", true) -> ButtonGold
                else -> Color.White
            }
            withStyle(style = SpanStyle(color = color, fontWeight = if(color != Color.White) FontWeight.Bold else FontWeight.Normal)) {
                append(word)
            }
            append(" ")
        }
    }
}

// ==========================================
// ✅ Watch Ad Clarification Dialog for Free Chat Expert
// ==========================================
@Composable
fun WatchAdClarificationDialog(
    onDismiss: () -> Unit,
    onWatchAdClicked: () -> Unit,
    onUpgradeProClicked: () -> Unit
) {
    val skyBlue = Color(0xFF00D4FF)
    val cardBg = Color(0xFF0B1322)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.2.dp, Brush.verticalGradient(listOf(skyBlue.copy(alpha = 0.6f), Color.Transparent))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon Container (Cyber Sky-Blue)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(skyBlue.copy(alpha = 0.12f), CircleShape)
                        .border(1.5.dp, skyBlue.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI Expert",
                        tint = skyBlue,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Free Chat Expert",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Watch a quick sponsored video ad to unlock real-time chat with our AI Analyst for this session.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFA0AABF),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Benefit Cards
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF070D18)),
                    border = BorderStroke(1.dp, skyBlue.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClarificationPerkItem(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            text = "5 AI Chat requests in this session",
                            tint = skyBlue
                        )
                        ClarificationPerkItem(
                            icon = Icons.Default.TrendingUp,
                            text = "Instant chart targets & trade confirmation",
                            tint = skyBlue
                        )
                        ClarificationPerkItem(
                            icon = Icons.Default.LockOpen,
                            text = "100% Free with rewarded ad",
                            tint = skyBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sky Blue Action Button ("button will be blue type sky")
                Button(
                    onClick = onWatchAdClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = skyBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF070E1A),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Ad & Start Chat",
                        color = Color(0xFF070E1A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Maybe Later", color = Color.Gray, fontSize = 13.sp)
                    }

                    TextButton(onClick = onUpgradeProClicked) {
                        Text("Get Pro (Ad-free)", color = skyBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClarificationPerkItem(icon: ImageVector, text: String, tint: Color = Color(0xFF00D4FF)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}