package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(viewModel: DeliveryViewModel) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Logo matching HTML Style
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(HighDensityPrimary, RoundedCornerShape(20.dp))
                    .border(2.dp, HighDensityBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Compass-like enterprise delivery icon
                Text(
                    text = "Ω",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "OmniRoute",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = HighDensityText,
                letterSpacing = (-1).sp
            )
            Text(
                text = "Enterprise Logistics Hub",
                fontSize = 14.sp,
                color = LogiGray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Switch Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HighDensitySecondary, RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isLoginMode) Color.White else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { isLoginMode = true }
                                .padding(vertical = 10.dp)
                                .testTag("login_tab_selector"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                fontWeight = FontWeight.Bold,
                                color = if (isLoginMode) HighDensityPrimary else LogiGray,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (!isLoginMode) Color.White else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { isLoginMode = false }
                                .padding(vertical = 10.dp)
                                .testTag("register_tab_selector"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Register",
                                fontWeight = FontWeight.Bold,
                                color = if (!isLoginMode) HighDensityPrimary else LogiGray,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LogiRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .border(1.dp, LogiRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Error",
                                    tint = LogiRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = it,
                                    color = LogiRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Fields
                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityBorder
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityPrimary,
                            unfocusedBorderColor = HighDensityBorder
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "Hide" else "Show",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityPrimary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityPrimary,
                            unfocusedBorderColor = HighDensityBorder
                        )
                    )

                    if (isLoginMode) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Forgot Password?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { showResetDialog = true }
                                .padding(vertical = 4.dp)
                                .testTag("forgot_password_btn")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Button
                    Button(
                        onClick = {
                            errorMessage = null
                            if (isLoginMode) {
                                viewModel.login(email, password,
                                    onSuccess = {
                                        Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { errorMessage = it }
                                )
                            } else {
                                viewModel.register(email, password, name,
                                    onSuccess = {
                                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { errorMessage = it }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_primary_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isLoginMode) "Sign In Securely" else "Create Account",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Social Logins Section (Google & Apple Styled Beautifully)
            Text(
                text = "OR CONNECT SECURELY WITH Enterprise SSO",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LogiGray,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mock Google SSO Button
                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Connecting Google Identity Session...", Toast.LENGTH_SHORT).show()
                        viewModel.login("driver@omniroute.com", "password", {}, {})
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("google_sso_btn"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HighDensityBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Google",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF444746),
                            fontSize = 13.sp
                        )
                    }
                }

                // Mock Apple SSO Button
                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Connecting Apple iCloud Session...", Toast.LENGTH_SHORT).show()
                        viewModel.login("manager@omniroute.com", "password", {}, {})
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("apple_sso_btn"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HighDensityBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Apple",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF444746),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Demo Bypass accounts for seamless presentation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = HighDensitySecondary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DEMO ACCESS MODE (QUICK BYPASS)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HighDensityPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                email = "driver@omniroute.com"
                                password = "password"
                                viewModel.login(email, password, {}, {})
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bypass_driver_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Driver John", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                email = "manager@omniroute.com"
                                password = "password"
                                viewModel.login(email, password, {}, {})
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bypass_manager_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Manager Jane", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Reset Password Request Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Enterprise Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Please enter your email address. We will simulate sending a password reset payload to your credentials:",
                        fontSize = 13.sp,
                        color = HighDensityText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_email_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityPrimary,
                            unfocusedBorderColor = HighDensityBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPassword(resetEmail,
                            onSuccess = {
                                Toast.makeText(context, "Simulated reset instructions sent to $resetEmail!", Toast.LENGTH_LONG).show()
                                showResetDialog = false
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    modifier = Modifier.testTag("submit_reset_btn")
                ) {
                    Text("Send Recovery Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
