package com.example.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.utils.LocalizationUtility
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizard(
    viewModel: PrayerViewModel,
    onComplete: () -> Unit,
    onRequestLocation: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(1) }
    val totalSteps = 4

    val appLanguage by viewModel.appLanguage.collectAsState()
    val emailFlow by viewModel.userEmail.collectAsState()
    val locationPreset by viewModel.selectedLocation.collectAsState()
    val isConfirmed by viewModel.isLocationConfirmed.collectAsState()

    var emailInput by rememberSaveable { mutableStateOf(emailFlow ?: "") }
    LaunchedEffect(emailFlow) {
        if (!emailFlow.isNullOrBlank() && emailInput.isBlank()) emailInput = emailFlow ?: ""
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val layoutDirection = if (appLanguage == "ur" || appLanguage == "ar") {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    fun text(key: String): String = LocalizationUtility.get(key, appLanguage)

    BackHandler(enabled = step > 1) { step-- }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..totalSteps) {
                            Box(
                                modifier = Modifier
                                    .size(if (i == step) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i <= step) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                            if (i < totalSteps) Spacer(modifier = Modifier.width(6.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = String.format(Locale.getDefault(), text("onboarding_step"), step, totalSteps),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Keep navigation reachable on small screens while allowing long localized
                    // content and the location dropdown to scroll independently.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (step) {
                            1 -> {
                                Text(
                                    text("onboarding_language_title"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val languages = listOf("en" to "English", "ar" to "العربية", "ur" to "اردو")
                                languages.forEach { (code, name) ->
                                    OutlinedButton(
                                        onClick = { viewModel.setAppLanguage(code) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (appLanguage == code) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            contentColor = if (appLanguage == code) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Text(
                                            name,
                                            fontSize = 16.sp,
                                            fontWeight = if (appLanguage == code) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            2 -> {
                                var dropdownExpanded by remember { mutableStateOf(false) }

                                Text(
                                    text("onboarding_location_title"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text("onboarding_location_description"),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onRequestLocation,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.LocationOn, text("onboarding_grant_location"))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text("onboarding_grant_location"), textAlign = TextAlign.Center)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text("onboarding_or"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { dropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text("onboarding_select_city"))
                                    }
                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        viewModel.locations.forEach { preset ->
                                            DropdownMenuItem(
                                                text = { Text(preset.city) },
                                                onClick = {
                                                    viewModel.setLocation(preset, context)
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isConfirmed) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                text("onboarding_selected"),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = String.format(
                                                    Locale.getDefault(),
                                                    text("onboarding_selected"),
                                                    locationPreset.city
                                                ),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                } else {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text("onboarding_location_required"),
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(12.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            3 -> {
                                Text(
                                    text("onboarding_account_title"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text("onboarding_account_description"),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                if (emailInput.isBlank()) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val googleIdOption = GetGoogleIdOption.Builder()
                                                        .setFilterByAuthorizedAccounts(false)
                                                        .setServerClientId("692524830790-d0acko25ljbhlf5qd7ddil20osqcgmfb.apps.googleusercontent.com")
                                                        .setAutoSelectEnabled(true)
                                                        .build()

                                                    val request = GetCredentialRequest.Builder()
                                                        .addCredentialOption(googleIdOption)
                                                        .build()

                                                    val result = credentialManager.getCredential(context = context, request = request)
                                                    val credential = result.credential
                                                    if (
                                                        credential is androidx.credentials.CustomCredential &&
                                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                                    ) {
                                                        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                                        val email = googleCredential.id
                                                        emailInput = email
                                                        viewModel.loginWithGoogle(
                                                            email,
                                                            googleCredential.displayName ?: email,
                                                            googleCredential.idToken
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("GoogleSignIn", "Failed to sign in", e)
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        text("onboarding_sign_in_error"),
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.Email, text("onboarding_continue_google"))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text("onboarding_continue_google"), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Email,
                                                text("onboarding_email"),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text("onboarding_connected"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    emailInput,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                Text(
                                    text("onboarding_ready_title"),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        val languageName = when (appLanguage) {
                                            "ar" -> "العربية"
                                            "ur" -> "اردو"
                                            else -> "English"
                                        }
                                        Text("${text("onboarding_language")}: $languageName", fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("${text("onboarding_location")}: ${locationPreset.city}", fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "${text("onboarding_email")}: ${emailInput.ifBlank { text("onboarding_not_provided") }}",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 1) {
                            TextButton(onClick = { step-- }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = text("action_back"),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text("action_back"))
                            }
                        } else {
                            Spacer(modifier = Modifier.width(80.dp))
                        }

                        if (step == 3) {
                            TextButton(onClick = { step++ }) { Text(text("action_skip")) }
                        }

                        if (step < 4) {
                            Button(
                                onClick = { step++ },
                                enabled = step != 2 || isConfirmed
                            ) {
                                Text(text("action_next"))
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = text("action_next"),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Button(onClick = onComplete) {
                                Text("🚀 ${text("onboarding_get_started")}")
                            }
                        }
                    }
                }
            }
        }
    }
}
