package com.example.userflowdemo.ui

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

private fun createAudioUri(context: Context): Uri {
    val audioDir = context.getExternalFilesDir("Music")
        ?: throw IllegalStateException("Music directory is unavailable")

    val audioFile = File(
        audioDir,
        "recorded_${System.currentTimeMillis()}.m4a"
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        audioFile
    )
}

private fun uriToFile(context: Context, uri: Uri): File {
    val audioDir = context.getExternalFilesDir("Music")
        ?: throw IllegalStateException("Music directory is unavailable")

    return File(audioDir, uri.lastPathSegment ?: "recorded_audio.m4a")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordAudioScreen(
    onConfirm: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var hasPermission by rememberSaveable { mutableStateOf(false) }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var recordedAudioUri by rememberSaveable { mutableStateOf<String?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    fun startRecording() {
        val uri = createAudioUri(context)
        val file = uriToFile(context, uri)

        val mediaRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

        @Suppress("DEPRECATION")
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = mediaRecorder
        recordedAudioUri = uri.toString()
        isRecording = true
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        recorder?.release()
        recorder = null
        isRecording = false
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.release()
            } catch (_: Exception) {
            }
            recorder = null
        }
    }

    BackHandler {
        if (isRecording) {
            stopRecording()
        }
        onBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Record Audio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                stopRecording()
                            }
                            onBack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(48.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .border(
                            4.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            RoundedCornerShape(48.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (isRecording) {
                                    stopRecording()
                                } else {
                                    startRecording()
                                }
                            }
                        },
                        modifier = Modifier.size(180.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Record",
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isRecording) "TAP TO STOP" else "TAP TO RECORD",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = when {
                        isRecording -> "Recording..."
                        recordedAudioUri != null -> "Recording ready to save"
                        else -> "Tap the mic to start recording"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        }
                        recordedAudioUri = null
                    },
                    enabled = recordedAudioUri != null || isRecording,
                    modifier = Modifier
                        .size(64.dp)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Redo",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        }
                        recordedAudioUri?.let { onConfirm(it) }
                    },
                    enabled = recordedAudioUri != null && !isRecording,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            if (recordedAudioUri != null && !isRecording) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Confirm recording",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}