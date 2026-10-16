package com.forex.chart.analysis.ai.ui.screens.news

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.forex.chart.analysis.ai.data.model.NewsModel
import com.forex.chart.analysis.ai.navigation.NavRoute
import com.forex.chart.analysis.ai.ui.theme.AppBackground
import com.forex.chart.analysis.ai.ui.theme.ButtonGold
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NewsScreen(
    navController: NavController,
    viewModel: NewsViewModel = viewModel()
) {
    val newsList by viewModel.newsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = 16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Article,
                contentDescription = null,
                tint = ButtonGold,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Market Intelligence",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ButtonGold)
            }
        } else if (newsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No latest news available.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
            ) {
                items(newsList) { news ->
                    NewsCard(news = news) {
                        val route = NavRoute.createWebViewRoute(news.title, news.url)
                        navController.navigate(route)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. NEWS CARD UI
// ==========================================
@Composable
fun NewsCard(news: NewsModel, onClick: () -> Unit) {
    val dateString = news.publishedAt?.let {
        SimpleDateFormat("dd MMM • hh:mm a", Locale.US).format(it)
    } ?: "Just Now"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(ButtonGold.copy(0.4f), Color.Transparent)))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(115.dp)) {
                if (news.imageUrl.isNotEmpty()) {
                    AsyncImage(model = news.imageUrl, contentDescription = "News Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF253145)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Feed, contentDescription = null, tint = ButtonGold, modifier = Modifier.size(36.dp))
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF161F2E).copy(alpha = 0.95f)), startY = 70f)))
            }

            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)) {
                Text(text = news.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = news.description, color = Color(0xFFA0AABF), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = ButtonGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = dateString, color = ButtonGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Read Article", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = ButtonGold, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}