package com.example.interviewprep.screens.review

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.interviewprep.di.buildTranscriptionRepository
import com.example.interviewprep.util.copyVideo
import kotlinx.coroutines.launch

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@OptIn(UnstableApi::class)
@Composable
fun ReviewScreen(
    modifier: Modifier = Modifier,
    videoUri: Uri?,
    questions: List<String>,
    questionStartSeconds: List<Int>,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transcriptionRepo = remember { buildTranscriptionRepository() }

    var isTranscribing by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf<String?>(null) }
    var transcriptError by remember { mutableStateOf<String?>(null) }

    val saveDoc =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { dest ->
            if (dest != null && videoUri != null) {
                copyVideo(context, videoUri, dest)
                Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show()
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(insets = WindowInsets.systemBars)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDone) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Session Review",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
        }


        if (videoUri != null) {
            val player = remember(videoUri) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(videoUri))
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    prepare()
                }
            }
            DisposableEffect(player) { onDispose { player.release() } }

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = true
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                controllerShowTimeoutMs = 2500
                            }
                        },
                        update = { it.player = player }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (transcript == null) {
                Button(
                    onClick = {
                        if (!isTranscribing && videoUri != null) {
                            isTranscribing = true
                            transcriptError = null
                            scope.launch {
                                val result = transcriptionRepo.transcribeFromUri(context, videoUri)
                                result
                                    .onSuccess { text ->
                                        val lines = questionStartSeconds
                                            .distinct()
                                            .sorted()
                                            .mapIndexed { i, sec ->
                                                val q = questions.getOrNull(i) ?: "-"

                                                "${formatMmSs(sec)}  Q${i + 1}: $q"

                                            }

                                        val header = if (lines.isNotEmpty()) {
                                            lines.joinToString("\n") + "\n\n"
                                        } else {
                                            ""
                                        }

                                        transcript = header + text
                                    }
                                    .onFailure { e ->
                                        transcriptError = e.message ?: "Transcription failed. Please try again."
                                    }
                                isTranscribing = false
                            }
                        }
                    },
                    enabled = !isTranscribing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (isTranscribing) "Transcribing..." else "Generate Transcript",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }


            Spacer(Modifier.height(12.dp))

            when {
                isTranscribing -> {
                    Text(
                        "Transcribing your answer...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                transcriptError != null -> {
                    Text(
                        text = transcriptError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                transcript != null -> {
                    Text(
                        text = "Transcript",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                            .verticalScroll(rememberScrollState())
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = transcript ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            Text(
                "No recording to review",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

private fun formatMmSs(total: Int): String {
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
