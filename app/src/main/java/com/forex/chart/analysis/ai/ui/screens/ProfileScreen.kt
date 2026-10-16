package com.forex.chart.analysis.ai.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.forex.chart.analysis.ai.data.manager.AdMobManager
import com.forex.chart.analysis.ai.data.manager.BillingManager
import com.forex.chart.analysis.ai.navigation.NavRoute
import com.forex.chart.analysis.ai.ui.components.PremiumPlanDialog // ✅ Imported from components
import com.forex.chart.analysis.ai.ui.screens.profile.ProfileViewModel
import com.forex.chart.analysis.ai.ui.theme.*

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Billing Setup
    val billingManager = remember { BillingManager(context) }
    val isProUser by billingManager.isProUser.collectAsState(initial = false)
    val prices by billingManager.productPrices.collectAsState(initial = emptyMap())
    val adMobManager = remember { AdMobManager(context) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    // পেন্ডিং পেমেন্ট প্ল্যান ট্র্যাক করার জন্য
    var pendingPlanId by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                viewModel.linkGoogleAccount(credential)

                if (pendingPlanId != null) {
                    val activity = context as? Activity
                    if (activity != null) {
                        // ✅ NEW: Yearly plan check added
                        when {
                            pendingPlanId!!.contains("weekly") -> billingManager.purchaseWeeklyPlan(activity)
                            pendingPlanId!!.contains("monthly") -> billingManager.purchaseMonthlyPlan(activity)
                            pendingPlanId!!.contains("yearly") -> billingManager.purchaseYearlyPlan(activity)
                        }
                    }
                    pendingPlanId = null
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        val authListener = FirebaseAuth.AuthStateListener { viewModel.updateUserStateFromFirebase() }
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(authListener) }
    }

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(containerColor = AppBackground) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ButtonGold)
                Text("Syncing Account Data...", modifier = Modifier.padding(top = 80.dp), color = TextWhite)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                // ✅ Updated Header (Shows Pro Badge)
                ProfileHeader(
                    userName = if (uiState.isAnonymous) "Guest Trader" else uiState.user?.displayName ?: "Pro Trader",
                    email = if (uiState.isAnonymous) "Sign in to save data" else uiState.user?.email ?: uiState.user?.uid ?: "",
                    isAnonymous = uiState.isAnonymous,
                    isProUser = isProUser // Pass Billing Status
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Show Premium Banner ONLY if NOT Pro
                if (!isProUser) {
                    PremiumBanner(onClick = { showPremiumDialog = true })
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    // Show "Pro Member Active" Card
                    ProMemberStatusCard()
                    Spacer(modifier = Modifier.height(24.dp))
                }

                SectionTitle("General")
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column {
                        ProfileMenuItem(Icons.Default.Star, "Rate Us", onClick = { openPlayStore(context) })
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        ProfileMenuItem(Icons.Default.Share, "Share App", onClick = { shareApp(context) })
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        ProfileMenuItem(Icons.Default.Email, "Contact Support", "radhedigitalsolutionofficial@gmail.com", onClick = { sendEmail(context) })
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        ProfileMenuItem(
                            icon = Icons.Default.PrivacyTip,
                            title = "Privacy Policy",
                            onClick = {
                                navController.navigate(NavRoute.createWebViewRoute("Privacy Policy", Uri.encode("https://radhedigitalsolution.in/privacy-policy/forexchart-ai-privacy-policy.html")))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("Account")
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column {
                        if (uiState.isAnonymous) {
                            ProfileMenuItem(
                                icon = Icons.Default.Link,
                                title = "Connect Google",
                                subtitle = "Sync your analysis history",
                                iconTint = ButtonGold,
                                textColor = ButtonGold,
                                onClick = {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken("754581553046-lq7chl8ehae2cru0hapj59158rkge0sm.apps.googleusercontent.com")
                                        .requestEmail()
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                }
                            )
                        } else {
                            ProfileMenuItem(
                                icon = Icons.AutoMirrored.Filled.ExitToApp,
                                title = "Log Out",
                                onClick = { showLogoutDialog = true }
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        ProfileMenuItem(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Account",
                            iconTint = Color(0xFFFF5252),
                            textColor = Color(0xFFFF5252),
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // ✅ Premium Plan Dialog Integration
    if (showPremiumDialog) {
        PremiumPlanDialog(
            weeklyPrice = prices[billingManager.weeklyPlanId] ?: "Loading...",
            monthlyPrice = prices[billingManager.monthlyPlanId] ?: "Loading...",
            yearlyPrice = prices[billingManager.yearlyPlanId] ?: "Loading...", // ✅ NEW: Yearly Price Passed
            isAnonymous = uiState.isAnonymous,
            onDismiss = {
                showPremiumDialog = false
                val activity = context as? Activity
                if (activity != null) {
                    adMobManager.showInterstitialAd(activity, isProUser) {}
                }
            },

            // ✅ যদি গেস্ট হয় -> লগইন রিকোয়েস্ট
            onLoginRequest = { planId ->
                pendingPlanId = planId
                showPremiumDialog = false

                // Google Login Launch
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("754581553046-lq7chl8ehae2cru0hapj59158rkge0sm.apps.googleusercontent.com")
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(client.signInIntent)

                Toast.makeText(context, "Please login to subscribe", Toast.LENGTH_SHORT).show()
            },

            // ✅ যদি লগইন থাকে -> সরাসরি কিনবে
            onSubscribe = { planId ->
                val activity = context as? Activity
                if (activity != null) {
                    // ✅ NEW: when block for all 3 plans
                    when {
                        planId.contains("weekly") -> billingManager.purchaseWeeklyPlan(activity)
                        planId.contains("monthly") -> billingManager.purchaseMonthlyPlan(activity)
                        planId.contains("yearly") -> billingManager.purchaseYearlyPlan(activity)
                    }
                }
                showPremiumDialog = false
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?", color = TextWhite) },
            text = { Text("Are you sure you want to log out?", color = TextGray) },
            containerColor = CardBackground,
            confirmButton = { TextButton(onClick = { viewModel.logout(); showLogoutDialog = false }) { Text("Log Out", color = Color(0xFFFF5252)) } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = TextWhite) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?", color = TextWhite) },
            text = { Text("This action cannot be undone. All history will be lost.", color = TextGray) },
            containerColor = CardBackground,
            confirmButton = { TextButton(onClick = { viewModel.deleteAccount { Toast.makeText(context, "Account Deleted", Toast.LENGTH_SHORT).show() }; showDeleteDialog = false }) { Text("Delete", color = Color(0xFFFF5252)) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = TextWhite) } }
        )
    }
}

// =================== UI COMPONENT DEFINITIONS ===================

@Composable
fun ProMemberStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, ButtonGold)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Verified, null, tint = ButtonGold, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("PRO MEMBER ACTIVE", color = ButtonGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Enjoy unlimited analysis & chat.", color = TextGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PremiumBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(brush = Brush.horizontalGradient(colors = listOf(Color(0xFFDAA520), Color(0xFFFFD700), Color(0xFFFFA000))))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Upgrade to Pro", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Unlock unlimited AI features", color = Color.Black.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ✅ Profile Header with Pro Badge Logic
@Composable
fun ProfileHeader(userName: String, email: String, isAnonymous: Boolean, isProUser: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF005F73), AppBackground)))
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Profile Border Color: Gold if Pro, else Gray
            val borderColor = if (isProUser) ButtonGold else if (isAnonymous) TextGray else TextWhite

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(2.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = borderColor, modifier = Modifier.size(50.dp))
            }

            // ✅ Pro Badge or Anonymous Badge
            if (isProUser) {
                Box(modifier = Modifier.offset(x = 6.dp, y = 6.dp).background(ButtonGold, CircleShape).padding(4.dp)) {
                    Icon(Icons.Default.Verified, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            } else if (isAnonymous) {
                Box(modifier = Modifier.offset(x = 6.dp, y = 6.dp).background(CardBackground, CircleShape).padding(4.dp)) {
                    Icon(Icons.Default.NoAccounts, null, tint = TextGray, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Name & Badge Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(userName, style = MaterialTheme.typography.headlineMedium.copy(color = TextWhite, fontWeight = FontWeight.Bold))
            if(isProUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(color = ButtonGold, shape = RoundedCornerShape(4.dp)) {
                    Text("PRO", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        Text(email, style = MaterialTheme.typography.bodyMedium.copy(color = TextGray))
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium.copy(color = TextGray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    )
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = false,
    iconTint: Color = TextWhite,
    textColor: Color = TextWhite,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = TextGray, fontSize = 12.sp)
        }
        if (showArrow) Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = TextGray, modifier = Modifier.size(16.dp))
    }
}

fun sendEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:radhedigitalsolutionofficial@gmail.com")
        putExtra(Intent.EXTRA_SUBJECT, "Support Request - Trade Chart AI")
        putExtra(Intent.EXTRA_TEXT, "Hello Support Team,\n\nI have a question regarding...\n\n")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Check out this amazing AI Trading Chart Analysis App! [Link]")
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

fun openPlayStore(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
    }
}