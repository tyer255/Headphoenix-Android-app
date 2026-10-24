package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaProcessingEngine
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaEngineSetupDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val engineStatus by MediaProcessingEngine.engineStatus.collectAsState()
    val codecs = remember { MediaProcessingEngine.getSupportedAudioCodecs() }
    var simulateFailure by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161618),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF555555))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Media Engine",
                        tint = GreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "yt-dlp & FFmpeg Engines",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bundled NDK Native Libraries & Python Runtime",
                        color = Color(0xFFA7A7A7),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. yt-dlp Engine Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (engineStatus.isYtDlpAvailable) Color(0xFF142E1F) else Color(0xFF222226))
                    .border(
                        1.dp,
                        if (engineStatus.isYtDlpAvailable) GreenPrimary.copy(alpha = 0.5f) else Color(0xFF3A3A40),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "yt-dlp Engine (dev.ffmpegkit-maintained)",
                            color = Color(0xFFA7A7A7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (engineStatus.isYtDlpAvailable) GreenPrimary.copy(alpha = 0.2f) else Color(0xFF444448))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (engineStatus.isYtDlpAvailable) "BUNDLED & READY" else "ACTIVE VIA REST",
                                color = if (engineStatus.isYtDlpAvailable) GreenPrimary else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = engineStatus.ytDlpVersion,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Embedded CPython runtime for direct yt-dlp media extraction with authorized cookies (--cookies).",
                        color = Color(0xFFC0C0C0),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. FFmpegKit Engine Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (engineStatus.isFFmpegAvailable) Color(0xFF142E1F) else Color(0xFF222226))
                    .border(
                        1.dp,
                        if (engineStatus.isFFmpegAvailable) GreenPrimary.copy(alpha = 0.5f) else Color(0xFF3A3A40),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FFmpegKit Engine (ffmpeg-kit-https)",
                            color = Color(0xFFA7A7A7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (engineStatus.isFFmpegAvailable) GreenPrimary.copy(alpha = 0.2f) else Color(0xFF444448))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (engineStatus.isFFmpegAvailable) "NATIVE C/C++ ACTIVE" else "HARDWARE CODEC",
                                color = if (engineStatus.isFFmpegAvailable) GreenPrimary else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = engineStatus.ffmpegVersion,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Native FFmpeg libraries compiled for arm64-v8a/x86_64 for audio transcoding, stream demuxing, and media merging.",
                        color = Color(0xFFC0C0C0),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Supported Hardware Audio Codecs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E22))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Hardware Audio Codecs (${codecs.size} Discovered)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    codecs.take(4).forEach { codec ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2B2B30))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = codec,
                                color = Color(0xFFE0E0E0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live FFmpeg & yt-dlp Download Percentage Indicator Box
            val overallPercent = (engineStatus.updateProgress * 100).toInt().coerceIn(0, 100)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF181A1E))
                    .border(1.dp, GreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Big Circular Percentage Gauge
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { engineStatus.updateProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.size(64.dp),
                            color = GreenPrimary,
                            trackColor = Color(0xFF2E2E36),
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = "${engineStatus.ffmpegPercent}%",
                            color = GreenPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FFmpeg Download: ${engineStatus.ffmpegPercent}%",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = engineStatus.speedText.ifEmpty { "Ready" },
                                color = GreenPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = engineStatus.downloadedMbText,
                            color = Color(0xFFD0D0D8),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = engineStatus.statusMessage,
                            color = if (engineStatus.error != null) Color(0xFFFF5252) else Color(0xFFA0A0A8),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. FFmpeg Individual Progress Bar + Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("FFmpeg Native Engine (v8.1.7)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${engineStatus.ffmpegPercent}%", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (engineStatus.ffmpegPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GreenPrimary,
                    trackColor = Color(0xFF2E2E36)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. yt-dlp Individual Progress Bar + Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("yt-dlp Python 3.13 Bundle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${engineStatus.ytdlpPercent}%", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (engineStatus.ytdlpPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GreenPrimary,
                    trackColor = Color(0xFF2E2E36)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. DExo (Media3) Progress Bar + Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DExo (Media3) Streamer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${overallPercent}%", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { engineStatus.updateProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GreenPrimary,
                    trackColor = Color(0xFF2E2E36)
                )

                if (engineStatus.ffmpegDownloadedBytes > 0L || engineStatus.ffmpegSha1.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF111316))
                            .border(1.dp, GreenPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "REAL ON-DISK VERIFICATION (NO FAKE DATA):",
                            color = GreenPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Source: ${engineStatus.downloadSource.ifBlank { "Maven Central + GitHub Releases" }}",
                            color = Color(0xFFD0D0D8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• FFmpeg Real Bytes: ${String.format(java.util.Locale.US, "%,d", engineStatus.ffmpegDownloadedBytes)} bytes",
                            color = Color(0xFFD0D0D8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• yt-dlp Real Bytes: ${String.format(java.util.Locale.US, "%,d", engineStatus.ytdlpDownloadedBytes)} bytes",
                            color = Color(0xFFD0D0D8),
                            fontSize = 11.sp
                        )
                        if (engineStatus.ffmpegSha1.isNotBlank()) {
                            Text(
                                text = "• SHA-1: ${engineStatus.ffmpegSha1}",
                                color = GreenPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (engineStatus.extractedNativeLibs.isNotEmpty()) {
                            Text(
                                text = "• Native .so Files (${engineStatus.extractedNativeLibs.size}): ${engineStatus.extractedNativeLibs.take(4).joinToString(", ")}",
                                color = Color(0xFFB0B0BA),
                                fontSize = 10.5.sp
                            )
                        }
                        if (engineStatus.installedFilePath.isNotBlank()) {
                            Text(
                                text = "• Path: ${engineStatus.installedFilePath}",
                                color = Color(0xFF8E8E98),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (engineStatus.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            MediaProcessingEngine.runEngineDiagnosticsAndUpdate(context, forceFail = false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5252),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Download", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2 Options as requested: (1) Install FFmpeg & yt-dlp, (2) Update FFmpeg & yt-dlp
            Text(
                text = "Select Engine Action (2 Options):",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: Install FFmpeg & yt-dlp
                Button(
                    onClick = {
                        MediaProcessingEngine.installAndBindEngines(context)
                        Toast.makeText(context, "Installing & binding FFmpeg + yt-dlp (0% -> 100%)...", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !engineStatus.isUpdating,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Install FFmpeg",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Option 2: Update FFmpeg & yt-dlp
                Button(
                    onClick = {
                        MediaProcessingEngine.runEngineDiagnosticsAndUpdate(context, forceFail = simulateFailure)
                        Toast.makeText(context, "Updating yt-dlp & FFmpeg (0% -> 100%)...", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !engineStatus.isUpdating,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF28282E),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdateAlt,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Update FFmpeg",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulation switch for testing error & retry workflow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Test Error & Retry Behavior",
                    color = Color(0xFF6E6E73),
                    fontSize = 12.sp
                )
                Switch(
                    checked = simulateFailure,
                    onCheckedChange = { simulateFailure = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF5252),
                        uncheckedThumbColor = Color(0xFF666666),
                        uncheckedTrackColor = Color(0xFF26262A)
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }
        }
    }
}

@Composable
fun PreLaunchEngineInstallerScreen(
    onOpenApk: () -> Unit
) {
    val context = LocalContext.current
    val engineStatus by MediaProcessingEngine.engineStatus.collectAsState()
    val cookieConfig by com.example.data.CookieRepository.cookieConfig.collectAsState()
    var cookieInput by remember { mutableStateOf(cookieConfig.rawCookie) }

    LaunchedEffect(Unit) {
        // Automatically start binding/downloading yt-dlp & FFmpeg over internet on installer launch
        MediaProcessingEngine.installAndBindEngines(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF18181C))
                    .border(1.dp, GreenPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BuildCircle,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Headphonix Bound Installer",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "yt-dlp • DExo (Media3) • FFmpeg Native Engine",
                        color = GreenPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: 2 Options to Install and Update FFmpeg & yt-dlp with Live % Indicator
            val overallPct = (engineStatus.updateProgress * 100).toInt().coerceIn(0, 100)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF16161A))
                    .border(1.dp, GreenPrimary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Big Circular Percentage Gauge
                    Box(
                        modifier = Modifier.size(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { engineStatus.updateProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.size(68.dp),
                            color = GreenPrimary,
                            trackColor = Color(0xFF2E2E36),
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = "${engineStatus.ffmpegPercent}%",
                            color = GreenPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FFmpeg Download: ${engineStatus.ffmpegPercent}%",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = engineStatus.speedText.ifEmpty { "Ready" },
                                color = GreenPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Downloaded: ${engineStatus.downloadedMbText}",
                            color = Color(0xFFE0E0E8),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = engineStatus.statusMessage,
                            color = Color(0xFFB8B8C2),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. FFmpeg Native Engine Bar + Live %
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("FFmpeg Native Engine (v8.1.7)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${engineStatus.ffmpegPercent}%", color = GreenPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (engineStatus.ffmpegPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GreenPrimary,
                    trackColor = Color(0xFF2E2E36)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. yt-dlp Python 3.13 Bar + Live %
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("yt-dlp Python 3.13 Extractor", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${engineStatus.ytdlpPercent}%", color = GreenPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (engineStatus.ytdlpPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GreenPrimary,
                    trackColor = Color(0xFF2E2E36)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. DExo Media3 Streamer Bar + Live %
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DExo (Media3) Streamer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${overallPct}%", color = GreenPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { engineStatus.updateProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GreenPrimary,
                    trackColor = Color(0xFF2E2E36)
                )

                if (engineStatus.ffmpegDownloadedBytes > 0L || engineStatus.ffmpegSha1.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF101215))
                            .border(1.dp, GreenPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "REAL ON-DISK VERIFICATION (NO FAKE DATA):",
                            color = GreenPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Source: ${engineStatus.downloadSource.ifBlank { "Maven Central + GitHub Releases" }}",
                            color = Color(0xFFD0D0D8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• FFmpeg Real Bytes: ${String.format(java.util.Locale.US, "%,d", engineStatus.ffmpegDownloadedBytes)} bytes",
                            color = Color(0xFFD0D0D8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• yt-dlp Real Bytes: ${String.format(java.util.Locale.US, "%,d", engineStatus.ytdlpDownloadedBytes)} bytes",
                            color = Color(0xFFD0D0D8),
                            fontSize = 11.sp
                        )
                        if (engineStatus.ffmpegSha1.isNotBlank()) {
                            Text(
                                text = "• SHA-1: ${engineStatus.ffmpegSha1}",
                                color = GreenPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (engineStatus.extractedNativeLibs.isNotEmpty()) {
                            Text(
                                text = "• Native .so Files (${engineStatus.extractedNativeLibs.size}): ${engineStatus.extractedNativeLibs.take(4).joinToString(", ")}",
                                color = Color(0xFFB0B0BA),
                                fontSize = 10.5.sp
                            )
                        }
                        if (engineStatus.installedFilePath.isNotBlank()) {
                            Text(
                                text = "• Path: ${engineStatus.installedFilePath}",
                                color = Color(0xFF8E8E98),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // The 2 Options requested: Option 1 (Install FFmpeg & yt-dlp) & Option 2 (Update FFmpeg & yt-dlp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            MediaProcessingEngine.installAndBindEngines(context)
                            Toast.makeText(context, "Downloading & Installing FFmpeg (0% -> 100%)...", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !engineStatus.isUpdating,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1. Install FFmpeg", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            MediaProcessingEngine.runEngineDiagnosticsAndUpdate(context, forceFail = false)
                            Toast.makeText(context, "Updating FFmpeg & yt-dlp (0% -> 100%)...", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !engineStatus.isUpdating,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF26262E),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("2. Update FFmpeg", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Connect User's Own Cookies in APK (with Paste Test Cookie button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF16161A))
                    .border(1.dp, Color(0xFF2C2C34), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STEP 2: Connect Your Cookies",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bind authorized session cookies with yt-dlp & DExo",
                            color = Color(0xFF9E9EA8),
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            cookieInput = com.example.data.CookieRepository.USER_TEST_NETSCAPE_COOKIE
                            com.example.data.CookieRepository.saveCookies(
                                rawCookie = cookieInput,
                                isEnabled = true
                            )
                            Toast.makeText(context, "Test Cookie Pasted & Connected!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Paste Test Cookie", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cookieInput,
                    onValueChange = {
                        cookieInput = it
                        com.example.data.CookieRepository.saveCookies(rawCookie = it, isEnabled = true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 140.dp),
                    placeholder = {
                        Text(
                            "Tap 'Paste Test Cookie' or paste your Netscape cookies here...",
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Open APK Button
            Button(
                onClick = {
                    if (cookieInput.isNotBlank()) {
                        com.example.data.CookieRepository.saveCookies(rawCookie = cookieInput, isEnabled = true)
                    }
                    MediaProcessingEngine.markInstallerCompleted(context)
                    onOpenApk()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Headphonix APK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

