package com.forex.chart.analysis.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forex.chart.analysis.ai.ui.theme.BannerTealEnd
import com.forex.chart.analysis.ai.ui.theme.BannerTealStart
import com.forex.chart.analysis.ai.ui.theme.ButtonGold

@Composable
fun AiPoweredBanner(
    onProClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(BannerTealStart, BannerTealEnd)
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ButtonGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI-Powered Chart Sense",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Smart Analysis for\nBetter Outcomes",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp,
                        lineHeight = 26.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                //  Replaced Surface/Box with actionable Button
                Button(
                    onClick = onProClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGold),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Deep Analysis",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = ButtonGold,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}