package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CreatePlaylistModal(
    isOpen: Boolean,
    type: String,
    onClose: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = scaleIn(initialScale = 0.98f, animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
        exit = scaleOut(targetScale = 0.98f, animationSpec = tween(220)) + fadeOut(animationSpec = tween(220))
    ) {
        val focusRequester = remember { FocusRequester() }
        var title by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        LaunchedEffect(isOpen, type) {
            if (isOpen) {
                title = if (type == "collaborative") "Collaborative playlist" 
                        else if (type == "blend") "My Blend"
                        else "My playlist"
                isLoading = false
                delay(100)
                focusRequester.requestFocus()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF404040), Color(0xFF242424), Color(0xFF121212))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    text = if (type == "collaborative") "Name your collaborative playlist" else "Give your playlist a name",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 48.dp)
                )

                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (title.isNotBlank() && !isLoading) {
                                isLoading = true
                                onCreate(title, type)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            innerTextField()
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.8f).height(1.5.dp).background(Color(0xCC737373)))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(56.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x661A1A1A)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && !isLoading) {
                                isLoading = true
                                onCreate(title, type)
                            }
                        },
                        enabled = title.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1DB954),
                            disabledContainerColor = Color(0xFF1DB954).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(if (isLoading) "Creating..." else "Create", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
