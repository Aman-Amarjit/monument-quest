package com.monumentquest.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monumentquest.ui.theme.*

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var isSignUpMode by remember { mutableStateOf(false) } // Default to Sign In
    var currentStep  by remember { mutableIntStateOf(1) } // 1: Email, 2: OTP, 3: Name (SignUp)

    var email        by remember { mutableStateOf("") }
    var otpCode      by remember { mutableStateOf("") }
    var name         by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSendingOtp by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // App Brand Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Surface2)
                    .border(1.dp, Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "MONUMENTQUEST",
                style = MaterialTheme.typography.labelSmall,
                color = Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Text(
                text = when (currentStep) {
                    1 -> if (isSignUpMode) "Create Account" else "Welcome Back"
                    2 -> "Enter Email Code"
                    else -> "Personal Details"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Surface1,
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Mode Toggle (Login vs Sign Up)
                    if (currentStep == 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface2)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isSignUpMode) Surface1 else Color.Transparent)
                                    .clickable {
                                        isSignUpMode = false
                                        errorMessage = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign In",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isSignUpMode) TextPrimary else TextSecondary,
                                    fontSize = 13.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSignUpMode) Surface1 else Color.Transparent)
                                    .clickable {
                                        isSignUpMode = true
                                        errorMessage = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign Up",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSignUpMode) TextPrimary else TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // ── STEP 1: EMAIL INPUT ────────────────────────────
                    if (currentStep == 1) {
                        Text(
                            text = "No password needed! We'll email you a 6-digit security code.",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Gold,
                                unfocusedBorderColor = Border,
                                focusedTextColor     = TextPrimary,
                                unfocusedTextColor   = TextPrimary
                            )
                        )
                    }

                    // ── STEP 2: VERIFY EMAIL OTP ─────────────────────────────
                    if (currentStep == 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0D2240))
                                .padding(12.dp)
                        ) {
                            Text(
                                "🔒 Enter your 6-digit Security PIN for $email",
                                fontSize = 11.5.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it },
                            label = { Text("Enter 6-Digit Security PIN") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Gold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Gold,
                                unfocusedBorderColor = Border,
                                focusedTextColor     = TextPrimary,
                                unfocusedTextColor   = TextPrimary
                            )
                        )
                    }

                    // ── STEP 3: FULL NAME FOR NEW USER ──────────────────
                    if (currentStep == 3) {
                        Text(
                            "Enter Your Name to Complete Signup:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name (e.g. Heritage Explorer)") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Gold,
                                unfocusedBorderColor = Border,
                                focusedTextColor     = TextPrimary,
                                unfocusedTextColor   = TextPrimary
                            )
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = RedAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Action Button
                    Button(
                        onClick = {
                            errorMessage = null
                            when (currentStep) {
                                1 -> {
                                    if (email.contains("@") && email.contains(".")) {
                                        isSendingOtp = true
                                        viewModel.sendOtp(
                                            email = email,
                                            onSuccess = {
                                                isSendingOtp = false
                                                currentStep = 2
                                            },
                                            onError = { msg ->
                                                isSendingOtp = false
                                                errorMessage = msg
                                            }
                                        )
                                    } else {
                                        errorMessage = "Please enter a valid email address."
                                    }
                                }
                                2 -> {
                                    if (otpCode.length == 6) {
                                        // Always test login first: if user already has an account, log them in instantly!
                                        viewModel.loginWithOtp(
                                            email = email,
                                            code = otpCode,
                                            onNeedsSignup = {
                                                isSignUpMode = true
                                                currentStep = 3
                                            },
                                            onError = { msg -> errorMessage = msg }
                                        )
                                    } else {
                                        errorMessage = "Please enter the 6-digit code sent to your email."
                                    }
                                }
                                3 -> {
                                    val cleanName = name.trim()
                                    if (cleanName.matches(Regex("^[0-9]+$"))) {
                                        errorMessage = "Please enter your full name (letters only, e.g. Aman Amarjit)."
                                    } else if (cleanName.length >= 2) {
                                        viewModel.registerWithOtp(
                                            email = email,
                                            code = otpCode,
                                            name = cleanName,
                                            onError = { msg -> errorMessage = msg }
                                        )
                                    } else {
                                        errorMessage = "Please enter a valid name (at least 2 letters)."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        enabled = uiState !is AuthUiState.Loading && !isSendingOtp
                    ) {
                        if (uiState is AuthUiState.Loading || isSendingOtp) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Bg, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = when (currentStep) {
                                    1 -> "Send Verification OTP >"
                                    2 -> "Verify Code & Sign In 🔒"
                                    else -> "Complete Registration & Start Quest 🎉"
                                },
                                color = Bg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    // Back Navigation
                    if (currentStep > 1) {
                        TextButton(
                            onClick = {
                                currentStep -= 1
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("< Back", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                    // Guest Login Option
                    OutlinedButton(
                        onClick = { viewModel.continueAsGuest() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Icon(Icons.Default.Shield, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue as Guest", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
