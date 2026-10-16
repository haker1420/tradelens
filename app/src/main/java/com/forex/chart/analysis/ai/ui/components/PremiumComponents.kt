package com.forex.chart.analysis.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.forex.chart.analysis.ai.ui.theme.ButtonGold
import com.forex.chart.analysis.ai.ui.theme.TextGray
import com.forex.chart.analysis.ai.ui.theme.TextWhite

@Composable
fun PremiumPlanDialog(
    weeklyPrice: String,
    monthlyPrice: String,
    yearlyPrice: String,
    isAnonymous: Boolean,
    onDismiss: () -> Unit,
    onLoginRequest: (String) -> Unit,
    onSubscribe: (String) -> Unit
) {
    // Pre-select 'year' by default to encourage yearly subscription
    var selectedPlan by remember { mutableStateOf("year") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Width styling
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)), // Dark Premium Background
            modifier = Modifier
                .fillMaxWidth(0.92f) // Take 92% of screen width
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()), // Scrollable on smaller screens
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (icon and title placed side by side to save height)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = ButtonGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upgrade to Premium",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ NEW: Updated compact feature list
                Column(modifier = Modifier.fillMaxWidth()) {
                    FeatureItem("Unlimited Deep Chart Analysis")
                    FeatureItem("AI Entry & Exit Suggestions")
                    FeatureItem("No Ads — 100% Focus")
                    FeatureItem("Expert-Level Market Insights")
                    FeatureItem("Priority Support")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ✅ Weekly Plan
                PlanOption(
                    title = "Weekly",
                    price = weeklyPrice,
                    originalPrice = "₹349", // Original strike-through price
                    selected = selectedPlan == "week",
                    onClick = { selectedPlan = "week" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ✅ Monthly Plan
                PlanOption(
                    title = "Monthly",
                    price = monthlyPrice,
                    originalPrice = "₹999", // Original strike-through price
                    selected = selectedPlan == "month",
                    onClick = { selectedPlan = "month" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ✅ Yearly Plan (BEST VALUE)
                PlanOption(
                    title = "Yearly",
                    price = yearlyPrice,
                    originalPrice = "₹6999", // Original strike-through price
                    badge = "🔥 BEST VALUE",
                    selected = selectedPlan == "year",
                    onClick = { selectedPlan = "year" }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Subscribe Button
                Button(
                    onClick = {
                        val planId = when (selectedPlan) {
                            "year" -> "yearly_pro_plan"
                            "month" -> "monthly_pro_plan"
                            else -> "weekly_pro_plan"
                        }
                        if (isAnonymous) {
                            onLoginRequest(planId)
                        } else {
                            onSubscribe(planId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGold),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Continue",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dismiss Button
                TextButton(onClick = onDismiss, modifier = Modifier.height(40.dp)) {
                    Text("Maybe Later", color = TextGray, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// Helper Composable for Features
@Composable
fun FeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Reduced padding for compact layout
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = TextWhite.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PlanOption(
    title: String,
    price: String,
    originalPrice: String,
    badge: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) ButtonGold else Color.White.copy(alpha = 0.08f)
    val bgColor = if (selected) ButtonGold.copy(alpha = 0.1f) else Color(0xFF0B101A) // Darker default BG

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = ButtonGold,
                    unselectedColor = TextGray.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFFC107).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFC107))
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFFFFC107),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = price,
                        color = ButtonGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = originalPrice,
                        color = TextGray.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }
    }
}