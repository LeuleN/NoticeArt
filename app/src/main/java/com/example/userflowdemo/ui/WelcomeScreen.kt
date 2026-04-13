package com.example.userflowdemo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.userflowdemo.R

@Composable
fun WelcomeScreen(
    onNameSubmitted: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val maxChar = 20
    
    // Derived states
    val isNameEmpty = name.isEmpty()
    val isNameBlank = name.isBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Reverted "WELCOME TO" style to original bold/headlineLarge
            Text(
                text = "WELCOME TO",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            // NoticeArt logo (Replaced with Image)
            Image(
                painter = painterResource(id = R.drawable.noticeart_logo),
                contentDescription = "NoticeArt Logo",
                modifier = Modifier
                    .padding(top = 16.dp)
                    .height(80.dp)
                    .fillMaxWidth(),
                alignment = Alignment.Center
            )

            // Tagline
            Text(
                text = "A space for your observations",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1.2f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "What should we call you?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextField(
                    value = name,
                    onValueChange = { 
                        if (it.length <= maxChar) {
                            name = it
                            if (showError && it.isNotBlank()) showError = false
                        }
                    },
                    placeholder = { 
                        Text(
                            "Enter your name", 
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.LightGray,
                                fontWeight = FontWeight.Normal
                            )
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF7E9AFE),
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.6f),
                        errorIndicatorColor = MaterialTheme.colorScheme.error,
                        cursorColor = Color(0xFF7E9AFE)
                    ),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    if (showError) {
                        Text(
                            text = "Please enter your name",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                    
                    Text(
                        text = "${name.length} / $maxChar",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF6B6B6B)
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isNameBlank) {
                        showError = true
                    } else {
                        onNameSubmitted(name.trim())
                    }
                },
                enabled = !isNameEmpty,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7E9AFE),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFF0F0F0),
                    disabledContentColor = Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp,
                    disabledElevation = 0.dp
                )
            ) {
                Text(
                    text = "Next",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(110.dp))
        }

        WaveDecoration(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
        )
    }
}

@Composable
fun WaveDecoration(modifier: Modifier = Modifier) {
    val waveShape1 = GenericShape { size, _ ->
        moveTo(0f, size.height * 0.7f)
        cubicTo(
            size.width * 0.2f, size.height * 0.5f,
            size.width * 0.5f, size.height * 0.9f,
            size.width, size.height * 0.6f
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }

    val waveShape2 = GenericShape { size, _ ->
        moveTo(0f, size.height * 0.8f)
        cubicTo(
            size.width * 0.3f, size.height * 0.9f,
            size.width * 0.7f, size.height * 0.6f,
            size.width, size.height * 0.85f
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD0E2FF).copy(alpha = 0.5f),
                            Color(0xFFD0E2FF)
                        )
                    ),
                    shape = waveShape1
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFC0CAFF).copy(alpha = 0.6f),
                            Color(0xFFA5B4FF)
                        )
                    ),
                    shape = waveShape2
                )
        )
    }
}
