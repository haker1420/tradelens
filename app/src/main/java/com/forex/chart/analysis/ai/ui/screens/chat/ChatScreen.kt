package com.forex.chart.analysis.ai.ui.screens.chat

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.forex.chart.analysis.ai.data.manager.AdMobManager
import com.forex.chart.analysis.ai.data.manager.BillingManager
import com.forex.chart.analysis.ai.data.model.AnalysisResponse
import com.forex.chart.analysis.ai.data.model.ChatMessage
import com.forex.chart.analysis.ai.ui.screens.premium_analysis.AnalysisResultModel // ✅ PRO Model Import
import com.forex.chart.analysis.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val msgCount by viewModel.messageCount.collectAsState()
    val currentAnalysis by viewModel.currentAnalysis.collectAsState()
    val currentProAnalysis by viewModel.currentProAnalysis.collectAsState() // ✅ NEW

    var inputText by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val listState = rememberLazyListState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                selectedBitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val billingManager = remember { BillingManager(context) }
    val isProUser by billingManager.isProUser.collectAsState(initial = false)
    val adMobManager = remember { AdMobManager(context) }
    val activity = context as? Activity

    val onBackWithAd: () -> Unit = {
        if (activity != null) {
            adMobManager.showInterstitialAd(activity, isProUser) {
                navController.popBackStack()
            }
        } else {
            navController.popBackStack()
        }
    }

    BackHandler {
        onBackWithAd()
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (currentProAnalysis != null) "Quantum Pro AI" else "AI Analyst", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Online • ${maxOf(0, 5 - msgCount)} messages left",
                            color = if (msgCount >= 5) Color(0xFFFF5252) else ButtonGold,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBackWithAd() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                modifier = Modifier.border(width = 1.dp, color = Color.White.copy(alpha = 0.05f))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBackground)
        ) {
            // ✅ Free Context Card
            currentAnalysis?.let { analysis ->
                ContextCard(analysis = analysis)
            }

            // ✅ NEW: Pro Context Card
            currentProAnalysis?.let { proAnalysis ->
                ProContextCard(analysis = proAnalysis)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { msg -> ChatBubble(message = msg) }
                if (isLoading) { item { TypingIndicator() } }
            }

            // Input area
            Box(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    AnimatedVisibility(visible = selectedBitmap != null) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                selectedBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, ButtonGold, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { selectedBitmap = null },
                                        modifier = Modifier.offset(x = 6.dp, y = (-6).dp).size(18.dp).align(Alignment.TopEnd).background(Color.Red, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Image attached", color = ButtonGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (msgCount >= 5) {
                        Text("Message limit reached (5/5).", color = Color(0xFFFF5252), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = msgCount < 5
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = ButtonGold)
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f).heightIn(min = 50.dp, max = 120.dp),
                            placeholder = { Text("Ask follow-up questions...", color = TextGray.copy(alpha = 0.5f), fontSize = 14.sp) },
                            textStyle = TextStyle(color = TextWhite, fontSize = 15.sp, letterSpacing = 0.5.sp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent, cursorColor = ButtonGold
                            ),
                            enabled = msgCount < 5
                        )

                        val isSendEnabled = !isLoading && msgCount < 5 && (inputText.isNotBlank() || selectedBitmap != null)

                        IconButton(
                            onClick = {
                                if (isSendEnabled) {
                                    viewModel.sendMessage(inputText, selectedBitmap)
                                    inputText = ""
                                    selectedBitmap = null
                                }
                            },
                            enabled = isSendEnabled,
                            modifier = Modifier.padding(start = 4.dp).background(if (isSendEnabled) ButtonBlue else Color.White.copy(alpha = 0.1f), CircleShape).size(44.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (isSendEnabled) Color.White else TextGray.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ✅ NEW: Pro Context Card UI
@Composable
fun ProContextCard(analysis: AnalysisResultModel) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121824)),
        shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFD1A054).copy(alpha = 0.3f), RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp))
    ) {
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFD1A054), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pro AI Context", color = Color(0xFFD1A054), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = Color(0xFFD1A054).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text("Score: ${analysis.confidenceScore}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFFD1A054), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextGray)
            }
            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFFD1A054).copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Signal: ${analysis.tradeSignal} | Timeframe: ${analysis.timeframe}", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Entry: ${analysis.entryPrice} | Target: ${analysis.takeProfit}", color = TextGray, fontSize = 13.sp)
                }
            }
        }
    }
}

// ... Remaining composables (ChatBubble, FormattedMessageContent, TypingIndicator, ContextCard) ...
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.padding(top = 4.dp).size(28.dp).background(brush = Brush.linearGradient(listOf(ButtonGold, Color(0xFFFFAB00))), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = if (isUser) RoundedCornerShape(18.dp, 18.dp, 2.dp, 18.dp) else RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            colors = CardDefaults.cardColors(containerColor = if (isUser) ButtonBlue else Color(0xFF253145)),
            modifier = Modifier.widthIn(min = 40.dp, max = if (isUser) 320.dp else 360.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                message.image?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Sent Image",
                        modifier = Modifier.height(150.dp).widthIn(max = 220.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.2f)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (message.text.isNotEmpty()) {
                    if (isUser) {
                        Text(text = message.text, color = TextWhite, fontSize = 15.sp, lineHeight = 22.sp)
                    } else {
                        FormattedMessageContent(text = message.text, textColor = TextWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedMessageContent(text: String, textColor: Color) {
    val lines = text.split("\n")
    Column {
        lines.forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("*") || trimmedLine.startsWith("-")) {
                Row(modifier = Modifier.padding(bottom = 6.dp, top = 2.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp).padding(top = 3.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    SelectionContainer {
                        Text(text = formatBoldText(trimmedLine.removePrefix("*").removePrefix("-").trim()), color = textColor, fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp, fontFamily = FontFamily.SansSerif)
                    }
                }
            } else {
                if (trimmedLine.isNotEmpty()) {
                    SelectionContainer {
                        Text(text = formatBoldText(trimmedLine), color = textColor, fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        }
    }
}

fun formatBoldText(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = ButtonGold)) { append(part) }
            } else {
                append(part)
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp, top = 16.dp)) {
        Text("AI is thinking...", color = TextGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = ButtonGold)
    }
}

@Composable
fun ContextCard(analysis: AnalysisResponse) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp))
    ) {
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Analysis Context", color = ButtonGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = if((analysis.data?.prediction?.getScoreInt() ?: 0) >= 70) Color.Green.copy(alpha = 0.2f) else Color.Yellow.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text("Score: ${analysis.data?.prediction?.getScoreInt() ?: 0}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = if((analysis.data?.prediction?.getScoreInt() ?: 0) >= 70) Color.Green else Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextGray)
            }
            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Signal: ${analysis.data?.prediction?.signal}", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Game Plan: ${analysis.data?.gamePlan ?: "N/A"}", color = TextGray, fontSize = 13.sp, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}