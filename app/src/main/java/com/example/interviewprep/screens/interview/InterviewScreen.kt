package com.example.interviewprep.screens.interview

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.interviewprep.data.RecordingStore
import java.io.File

@SuppressLint("MissingPermission")
@Composable
fun InterviewScreen(
    modifier: Modifier = Modifier,
    questions: List<String>,
    onFinish: (Uri?, List<Int>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current


    var hasCamera by remember { mutableStateOf(false) }
    var hasMic by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { map ->
            hasCamera = map[Manifest.permission.CAMERA] == true
            hasMic = map[Manifest.permission.RECORD_AUDIO] == true
        }
    )


    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }

    val questionStartSeconds = remember { mutableListOf(0) }


    var idx by remember { mutableStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var seconds by remember { mutableStateOf(0) }
    var lastSavedUri by remember { mutableStateOf<Uri?>(null) }
    var shouldFinishAfterFinalize by remember { mutableStateOf(false) }
    val lastIndex = (questions.size - 1).coerceAtLeast(0)
    idx = idx.coerceIn(0, lastIndex)

    var currentCacheFile by remember { mutableStateOf<File?>(null) }


    LaunchedEffect(Unit) {
        permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }


    LaunchedEffect(isRecording, idx) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                seconds++
            }
        } else {
            seconds = 0
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                currentRecording?.stop()
                currentRecording = null
                val provider = cameraProviderFuture.get()
                provider.unbindAll()
            } catch (_: Exception) {  }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {

        AndroidView(
            factory = { ctx ->
                val pv = PreviewView(ctx).apply {
                    layoutParams =
                        android.widget.FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val previewUseCase =
                    Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                val qualitySelector = QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
                val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
                val videoUseCase = VideoCapture.withOutput(recorder)

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewUseCase,
                        videoUseCase
                    )
                    videoCapture = videoUseCase
                } catch (e: Exception) {
                    Log.e("InterviewScreen", "Bind failed", e)
                }
                pv
            },
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(20.dp))
        )


        AnimatedVisibility(
            visible = isRecording,
            modifier = Modifier
                .align(alignment = Alignment.TopCenter)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp)
                    .background(
                        Color(0xCC000000),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = questions.getOrNull(idx) ?: "-",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Text(
            text = formatTime(seconds),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .background(
                    Color(0x66000000),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )

        val canNext   = isRecording && questions.isNotEmpty() && idx < lastIndex
        val canFinish = isRecording || lastSavedUri != null


        FloatingActionButton(
            onClick = {
                if (!hasCamera || !hasMic) {
                    permLauncher.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                    return@FloatingActionButton
                }
                val vc = videoCapture ?: return@FloatingActionButton

                if (!isRecording && currentRecording == null) {

                    val name = "mock_interview_${System.currentTimeMillis()}.mp4"
                    val file = File(context.cacheDir, name)
                    currentCacheFile = file
                    val outputOptions = FileOutputOptions.Builder(file).build()

                    currentRecording = vc.output
                        .prepareRecording(context, outputOptions)
                        .apply { if (hasMic) withAudioEnabled() }
                        .start(ContextCompat.getMainExecutor(context)) { event ->
                            when (event) {
                                is VideoRecordEvent.Start -> {
                                    isRecording = true
                                    seconds = 0
                                    questionStartSeconds.clear()
                                    questionStartSeconds.add(0)

                                }

                                is VideoRecordEvent.Finalize -> {
                                    isRecording = false

                                    val cacheFile = currentCacheFile
                                    val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }

                                    val finalFile = if (cacheFile != null && cacheFile.exists()) {
                                        val out = File(recordingsDir, cacheFile.name)
                                        runCatching {
                                            cacheFile.copyTo(out, overwrite = true)
                                            cacheFile.delete()
                                        }
                                        out
                                    } else {
                                        null
                                    }

                                    val uri = (finalFile?.toUri()) ?: event.outputResults.outputUri

                                    if (finalFile != null) {
                                        RecordingStore.add(context, finalFile)
                                    }

                                    lastSavedUri = uri

                                    if (shouldFinishAfterFinalize) {
                                        shouldFinishAfterFinalize = false
                                        onFinish(uri, questionStartSeconds.toList())
                                    }

                                }

                                else -> Unit
                            }
                        }
                }

            },

            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            containerColor = Color(0xCC000000),
            contentColor = Color.White
        ) {
            if (isRecording) {
                Box(
                    Modifier
                        .padding(18.dp)
                        .background(
                            Color.Red,
                            androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .size(18.dp)
                )
            } else {
                Box(
                    Modifier
                        .padding(16.dp)
                        .size(22.dp)
                        .background(
                            Color.Red,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }


        OutlinedButton(
            enabled = (questions.isEmpty() && canFinish) || canNext || (idx == lastIndex && canFinish),
            onClick = {
                if (questions.isNotEmpty() && idx < lastIndex) {
                    idx++
                    questionStartSeconds.add(seconds)
                } else {

                    if (isRecording) {
                        shouldFinishAfterFinalize = true
                        currentRecording?.stop()
                        currentRecording = null
                    } else {
                        val uri = lastSavedUri
                        if (uri == null) {
                            onFinish(null, questionStartSeconds.toList())
                        } else {
                            onFinish(uri, questionStartSeconds.toList())
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .background(
                    Color(0x66000000),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                )
        ) {
            Text(if (idx < lastIndex) "Next" else "Finish")
        }
    }
}

fun formatTime(total: Int): String {
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}


