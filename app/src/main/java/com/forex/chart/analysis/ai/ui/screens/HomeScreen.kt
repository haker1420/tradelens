package com.forex.chart.analysis.ai.ui.screens

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.forex.chart.analysis.ai.R
import com.forex.chart.analysis.ai.data.manager.AdMobManager
import com.forex.chart.analysis.ai.navigation.NavRoute
import com.forex.chart.analysis.ai.ui.components.AiPoweredBanner
import com.forex.chart.analysis.ai.ui.components.PremiumPlanDialog
import com.forex.chart.analysis.ai.ui.screens.analysis.AnalysisResultScreen
import com.forex.chart.analysis.ai.ui.screens.chat.ChatViewModel
import com.forex.chart.analysis.ai.ui.screens.home.HomeViewModel
import com.forex.chart.analysis.ai.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    chatViewModel: ChatViewModel,
    newlyCapturedImageUri: String? = null,
    onImageHandled: () -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val adMobManager = remember { AdMobManager(context) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                viewModel.onGoogleLoginSuccess(credential, context as Activity)
            } catch (e: ApiException) {
                Toast.makeText(context, "Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val handleProAction = { viewModel.requestProDialog() }

    LaunchedEffect(uiState.showAdTrigger) {
        if (uiState.showAdTrigger) {
            adMobManager.showRewardedAd(context as Activity) { viewModel.onAdWatched() }
        }
    }

    LaunchedEffect(uiState.analysisResult) { onToggleBottomBar(uiState.analysisResult == null) }
    BackHandler(enabled = uiState.analysisResult != null) { viewModel.resetAnalysis() }
    BackHandler(enabled = uiState.selectedImages.isNotEmpty() && uiState.analysisResult == null) { viewModel.clearImages() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris -> if (uris.isNotEmpty()) viewModel.addImages(uris) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val onCameraClick = {
        if (cameraPermissionState.status.isGranted) navController.navigate(NavRoute.CAMERA_SCREEN)
        else cameraPermissionState.launchPermissionRequest()
    }
    val onGalleryClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    LaunchedEffect(newlyCapturedImageUri) {
        newlyCapturedImageUri?.let { viewModel.addImages(listOf(Uri.parse(it))); onImageHandled() }
    }

    when {
        uiState.isUploading -> { AnalysisLoaderScreen() }

        uiState.analysisResult != null -> {
            AnalysisResultScreen(
                data = uiState.analysisResult!!,
                localImageUri = uiState.selectedImages.firstOrNull(),
                isChatLocked = uiState.isChatLocked,
                weeklyPrice = uiState.weeklyPrice,
                monthlyPrice = uiState.monthlyPrice,
                yearlyPrice = uiState.yearlyPrice,
                showRatingDialog = uiState.showRatingDialog,
                onDismissRatingDialog = { viewModel.dismissRatingDialog() },
                onMarkUserAsRated = { viewModel.markUserAsRated() },
                onBackClicked = { viewModel.resetAnalysis() },
                onChatClicked = {
                    viewModel.onChatClicked {
                        chatViewModel.startSession(uiState.analysisResult!!, newSession = true)
                        navController.navigate(NavRoute.CHAT_SESSION)
                    }
                },
                onBuyPro = { planId ->
                    val activity = context as? Activity
                    activity?.let { viewModel.triggerPurchase(it, planId) }
                }
            )
        }

        uiState.selectedImages.isNotEmpty() -> {
            SelectedImagesScreen(
                images = uiState.selectedImages,
                isProUser = uiState.isProUser,
                onRemove = { viewModel.removeImage(it) },
                onAddFromGallery = onGalleryClick,
                onAddFromCamera = onCameraClick,
                onAnalyzeClick = { viewModel.startAnalysisProcess() },
                onBannerProClick = {
                    if (!uiState.isProUser) {
                        viewModel.dismissAnalysisChoicePopup()
                        handleProAction()
                    }
                }
            )
        }

        else -> {
            EmptyHomeScreen(
                isProUser = uiState.isProUser,
                freeProScanCount = uiState.freeProScanCount,
                onImageUploadClicked = onGalleryClick,
                onCameraCaptureClicked = onCameraClick,
                onBannerProClick = { handleProAction() },
                onProAnalysisClicked = { navController.navigate(NavRoute.PREMIUM_ANALYSIS) }
            )
        }
    }

    if (uiState.showAnalysisChoicePopup) {
        AnalysisChoiceDialog(
            onProClick = {
                viewModel.dismissAnalysisChoicePopup()
                handleProAction()
            },
            onAdClick = viewModel::onChoiceAdClicked,
            onDismiss = {
                viewModel.dismissAnalysisChoicePopup()
                val activity = context as? Activity
                if (activity != null) {
                    adMobManager.showInterstitialAd(activity, uiState.isProUser) {}
                }
            }
        )
    }

    if (uiState.showPremiumPopup) {
        PremiumPlanDialog(
            weeklyPrice = uiState.weeklyPrice,
            monthlyPrice = uiState.monthlyPrice,
            yearlyPrice = uiState.yearlyPrice,
            isAnonymous = uiState.isAnonymous,
            onDismiss = {
                viewModel.dismissPremiumPopup()
                val activity = context as? Activity
                if (activity != null) {
                    adMobManager.showInterstitialAd(activity, uiState.isProUser) {}
                }
            },
            onLoginRequest = { planId ->
                viewModel.pendingPlanId = planId
                viewModel.dismissPremiumPopup()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("754581553046-lq7chl8ehae2cru0hapj59158rkge0sm.apps.googleusercontent.com")
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(client.signInIntent)
            },
            onSubscribe = { planId ->
                val activity = context as? Activity
                activity?.let { viewModel.triggerPurchase(it, planId) }
                viewModel.dismissPremiumPopup()
            }
        )
    }
}

// ==========================================
// Custom Premium Rating UI
// ==========================================
@Composable
fun PremiumRatingDialog(onDismiss: () -> Unit, onRateSubmit: (Int) -> Unit) {
    var selectedStars by remember { mutableIntStateOf(5) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
            border = BorderStroke(1.dp, ButtonGold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(ButtonGold.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, ButtonGold.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = ButtonGold, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Rate Your Experience",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "If our AI helped you in trading, please support us with a 5-star rating. It keeps the app free!",
                    color = TextGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        val isSelected = i <= selectedStars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (isSelected) ButtonGold else Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { selectedStars = i }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { onRateSubmit(selectedStars) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGold),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Submit Rating",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text("Maybe Later", color = TextGray, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
// ==========================================

@Composable
fun EmptyHomeScreen(
    isProUser: Boolean,
    freeProScanCount: Int,
    onImageUploadClicked: () -> Unit,
    onCameraCaptureClicked: () -> Unit,
    onBannerProClick: () -> Unit,
    onProAnalysisClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.name_logo),
            contentDescription = "App Logo",
            modifier = Modifier.height(40.dp).fillMaxWidth(),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        if (isProUser) ProUserBanner() else AiPoweredBanner(onProClick = onBannerProClick)

        Spacer(modifier = Modifier.height(30.dp))

        if (isProUser || freeProScanCount > 0) {
            ProAnalysisMainCard(
                onClick = onProAnalysisClicked,
                isTrial = !isProUser && freeProScanCount > 0
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Standard AI Analysis",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                UploadInstructionCard(
                    onImageUploadClicked = onImageUploadClicked,
                    onCameraCaptureClicked = onCameraCaptureClicked
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onBannerProClick,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, ButtonGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = ButtonGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock Pro AI Analysis", color = ButtonGold)
                }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun ProAnalysisMainCard(onClick: () -> Unit, isTrial: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    val borderGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF00D4FF), Color(0xFF007BFF), Color(0xFF00D4FF))
    )
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF060A14), Color(0xFF0A1224))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .scale(scale)
            .clickable { onClick() }
            .border(1.5.dp, borderGradient, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTrial) {
                    Surface(color = Color(0xFFFFD700), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "🎁 1 FREE PRO SCAN LEFT",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFF00D4FF).copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, Color(0xFF00D4FF).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Upload Chart",
                        tint = Color(0xFF00D4FF),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Quantum AI Analysis",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Institutional grade SMC & price action scanner",
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF000000).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScanStepItem(icon = Icons.Default.Search, text = "Deep scanning chart structures")
                    ScanStepItem(icon = Icons.Default.Memory, text = "Processing institutional order flow")
                    ScanStepItem(icon = Icons.Default.GpsFixed, text = "Mapping precision target zones")
                }

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF00D4FF),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upload chart to begin",
                            style = TextStyle(
                                color = Color(0xFF060A14),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFF060A14),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScanStepItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00D4FF).copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun ProUserBanner() {
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp).clickable(enabled = false) {},
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF00C853), Color(0xFF69F0AE)))),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Verified, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Pro Trader Active", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("You have unlimited access.", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
            }
        }
    }
}

// Preserved side-by-side box layout design
@Composable
fun UploadInstructionCard(onImageUploadClicked: () -> Unit, onCameraCaptureClicked: () -> Unit) {
    val cardGradient = Brush.verticalGradient(colors = listOf(Color(0xFF00202A), Color(0xFF10313B)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardGradient)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(vertical = 32.dp, horizontal = 20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color(0xFF050B13)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.BarChart, null, tint = TextWhite, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("AI Chart Analysis", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 18.sp))

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clickable { onImageUploadClicked() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ButtonBlue.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, ButtonBlue.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = ButtonBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Gallery", color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clickable { onCameraCaptureClicked() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ButtonBlue.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, ButtonBlue.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ButtonBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Camera", color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// Upload button and layout matching chart design
@Composable
fun SelectedImagesScreen(
    images: List<Uri>,
    isProUser: Boolean,
    onRemove: (Uri) -> Unit,
    onAddFromGallery: () -> Unit,
    onAddFromCamera: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onBannerProClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.name_logo),
            contentDescription = null,
            modifier = Modifier.height(40.dp).fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isProUser) ProUserBanner() else AiPoweredBanner(onProClick = onBannerProClick)
        Spacer(modifier = Modifier.height(24.dp))

        ImagePreviewSection(
            selectedImages = images,
            onRemoveImage = onRemove,
            onAddFromGallery = onAddFromGallery,
            onAddFromCamera = onAddFromCamera
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAnalyzeClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGold),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color.Black)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Analyze Charts",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 18.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "AI will generate prediction based on all charts.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun ImagePreviewSection(selectedImages: List<Uri>, onRemoveImage: (Uri) -> Unit, onAddFromGallery: () -> Unit, onAddFromCamera: () -> Unit) {
    Column {
        selectedImages.forEach { uri ->
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(uri).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(onClick = { onRemoveImage(uri) }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row {
            Button(onClick = onAddFromGallery, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))) { Text("Add More") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onAddFromCamera, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))) { Text("Camera") }
        }
    }
}

@Composable
fun AnalysisLoaderScreen() {
    val currentMessageIndex = remember { Animatable(0f) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_chart))
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            currentMessageIndex.animateTo((currentMessageIndex.value + 1) % 4, animationSpec = keyframes { durationMillis = 500 })
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(AppBackground.copy(alpha = 0.95f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(200.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text("Analyzing Chart...", style = TextStyle(color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 18.sp))
        }
    }
}

@Composable
fun AnalysisChoiceDialog(onProClick: () -> Unit, onAdClick: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.TrendingUp, null, tint = ButtonGold, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Daily Free Limit Reached", style = MaterialTheme.typography.headlineSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("You have used your free daily analysis quota.", textAlign = TextAlign.Center, color = TextGray)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onProClick, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ButtonGold), shape = RoundedCornerShape(14.dp)) { Text("Get Pro Access (Unlimited)", color = Color.Black, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAdClick, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue), shape = RoundedCornerShape(14.dp)) { Text("Continue Free with Ad", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Cancel", color = TextGray) }
            }
        }
    }
}