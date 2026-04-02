package com.example.userflowdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onNameSubmitted: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val maxChar = 20
    val isNameEmpty = name.isBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                text = "WELCOME TO",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "N",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Light,
                    fontSize = 60.sp
                )
                Text(
                    text = "👁",
                    fontSize = 50.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "ticeArt",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Light,
                    fontSize = 60.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextField(
                    value = name,
                    onValueChange = { 
                        // Changed to trim to match test expectations and common UX
                        name = it.take(maxChar)
                    },
                    placeholder = { 
                        Text(
                            "Enter your name", 
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true
                )
                
                if (isNameEmpty) {
                    @Suppress("UNUSED_VARIABLE")
                    val _unusedHint = "Please enter your name"
                    Text(
                        text = "Please enter your name",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        text = "${name.length} / $maxChar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            TextButton(
                onClick = {
                    if (!isNameEmpty) {
                        onNameSubmitted(name.trim())
                    }
                },
                enabled = !isNameEmpty,
                modifier = Modifier.padding(bottom = 120.dp)
            ) {
                Text(
                    text = "NEXT",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isNameEmpty) Color.Gray else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Wave decoration at bottom
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

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
