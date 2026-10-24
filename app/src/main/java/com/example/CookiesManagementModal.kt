package com.example

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CookieRepository
import com.example.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookiesManagementModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val cookieConfig by CookieRepository.cookieConfig.collectAsState()

    var cookieText by remember { mutableStateOf(cookieConfig.rawCookie) }
    var userAgentText by remember { mutableStateOf(cookieConfig.customUserAgent) }
    var isEnabled by remember { mutableStateOf(cookieConfig.isEnabled) }
    var speedBooster by remember { mutableStateOf(cookieConfig.speedBoosterEnabled) }
    var bufferChunkKb by remember { mutableIntStateOf(cookieConfig.bufferChunkSizeKb) }
    var isPasswordMasked by remember { mutableStateOf(true) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf(cookieConfig.lastTestStatus) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161618),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF555555)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF26262A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Cookies",
                        tint = GreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Authorized Cookies",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage session credentials securely",
                        color = Color(0xFFA7A7A7),
                        fontSize = 13.sp
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GreenPrimary,
                        uncheckedThumbColor = Color(0xFF888888),
                        uncheckedTrackColor = Color(0xFF333333)
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Status Banner
            val hasCookies = cookieText.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hasCookies && isEnabled) Color(0xFF142E1F) else Color(0xFF242428))
                    .border(
                        1.dp,
                        if (hasCookies && isEnabled) GreenPrimary.copy(alpha = 0.5f) else Color(0xFF3A3A40),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasCookies && isEnabled) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (hasCookies && isEnabled) GreenPrimary else Color(0xFFAAAAAA),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (hasCookies && isEnabled) "Cookies Active & Applied" else if (!isEnabled) "Cookies Disabled" else "No Cookies Configured",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (testResultText.isNotBlank()) testResultText else "Applied to Media3 player & downloads",
                            color = if (testSuccess == false) Color(0xFFFF5252) else Color(0xFFA0A0A0),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cookie Input Label + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cookie String",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Row {
                    TextButton(
                        onClick = {
                            val clipboardText = clipboardManager.getText()?.text
                            if (!clipboardText.isNullOrBlank()) {
                                cookieText = clipboardText
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = GreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste", color = GreenPrimary, fontSize = 12.sp)
                    }

                    if (cookieText.isNotEmpty()) {
                        TextButton(
                            onClick = { cookieText = "" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Clear", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = cookieText,
                onValueChange = { cookieText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 160.dp),
                placeholder = {
                    Text(
                        "Paste your authorized cookies here (e.g. SID=...; HSID=...; SAPISID=...;)",
                        color = Color(0xFF6E6E73),
                        fontSize = 13.sp
                    )
                },
                visualTransformation = if (isPasswordMasked) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    IconButton(onClick = { isPasswordMasked = !isPasswordMasked }) {
                        Icon(
                            imageVector = if (isPasswordMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordMasked) "Show cookies" else "Hide cookies",
                            tint = Color(0xFFA0A0A0)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = Color(0xFF333338),
                    focusedContainerColor = Color(0xFF1E1E22),
                    unfocusedContainerColor = Color(0xFF1E1E22),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Preset Chips
            Text(
                text = "Quick Presets / Templates:",
                color = Color(0xFF8E8E93),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        cookieText = "VISITOR_INFO1_LIVE=auth_sample; SID=sample_sid_auth; HSID=sample_hsid; LOGIN_INFO=sample_login;"
                        Toast.makeText(context, "Filled YouTube Music template", Toast.LENGTH_SHORT).show()
                    },
                    label = { Text("YouTube Template", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF26262A),
                        labelColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                FilterChip(
                    selected = false,
                    onClick = {
                        cookieText = "session_token=usr_auth_stream; cdn_token=media_access_verified;"
                        Toast.makeText(context, "Filled Media CDN template", Toast.LENGTH_SHORT).show()
                    },
                    label = { Text("Media CDN", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF26262A),
                        labelColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Speed Booster & Buffer Size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E22))
                    .clickable { speedBooster = !speedBooster }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (speedBooster) GreenPrimary else Color(0xFF888888),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download Speed Booster",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Multi-connection pooling & 64KB high-speed stream buffering",
                        color = Color(0xFFA0A0A0),
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = speedBooster,
                    onCheckedChange = { speedBooster = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GreenPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Advanced options toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showAdvanced) "Hide Advanced Options" else "Show Advanced Network Options",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93)
                )
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Custom User-Agent (Optional)",
                        color = Color(0xFFA0A0A0),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = userAgentText,
                        onValueChange = { userAgentText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Default Android Mobile User-Agent",
                                color = Color(0xFF6E6E73),
                                fontSize = 12.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color(0xFF333338),
                            focusedContainerColor = Color(0xFF1E1E22),
                            unfocusedContainerColor = Color(0xFF1E1E22),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Buffer Chunk Size (KB)",
                        color = Color(0xFFA0A0A0),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(32, 64, 128, 256).forEach { size ->
                            FilterChip(
                                selected = bufferChunkKb == size,
                                onClick = { bufferChunkKb = size },
                                label = { Text("${size} KB", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF26262A),
                                    labelColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Test Connection Button
            OutlinedButton(
                onClick = {
                    isTestingConnection = true
                    testResultText = "Testing connection with headers..."
                    scope.launch {
                        // Temporarily apply cookie config for test
                        CookieRepository.saveCookies(
                            rawCookie = cookieText,
                            customUserAgent = userAgentText,
                            isEnabled = isEnabled,
                            speedBooster = speedBooster,
                            bufferSizeKb = bufferChunkKb
                        )
                        val (success, status) = CookieRepository.testConnection()
                        isTestingConnection = false
                        testSuccess = success
                        testResultText = status
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444448))
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = GreenPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Verifying headers...")
                } else {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Connection & Ping", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save & Apply Button
            Button(
                onClick = {
                    CookieRepository.saveCookies(
                        rawCookie = cookieText,
                        customUserAgent = userAgentText,
                        isEnabled = isEnabled,
                        speedBooster = speedBooster,
                        bufferSizeKb = bufferChunkKb
                    )
                    Toast.makeText(context, "Authorized cookies applied successfully!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save & Apply Cookies",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
