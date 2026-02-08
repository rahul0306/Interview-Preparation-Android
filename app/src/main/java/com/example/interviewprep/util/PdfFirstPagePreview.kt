package com.example.interviewprep.util

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PdfFirstPagePreview(
    uri: Uri,
    maxHeightFraction: Float = 0.96f,
    maxRenderWidthPxCap: Int = 3000
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val maxPreviewHeight: Dp = screenHeightDp * maxHeightFraction
    val density = LocalDensity.current

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val availableWidthDp = maxWidth

        LaunchedEffect(uri, availableWidthDp) {
            error = null
            bitmap = null

            val targetWidthPx = with(density) {
                availableWidthDp.toPx().toInt().coerceAtMost(maxRenderWidthPxCap)
            }

            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (renderer.pageCount == 0) return@use
                        renderer.openPage(0).use { page ->
                            val scale = targetWidthPx.toFloat() / page.width.toFloat()
                            val targetHeightPx = (page.height * scale).toInt()

                            val bmp = createBitmap(
                                targetWidthPx.coerceAtLeast(1),
                                targetHeightPx.coerceAtLeast(1)
                            )
                            page.render(
                                bmp,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            bitmap = bmp
                        }
                    }
                } ?: throw IllegalStateException("Unable to open file")
            }.onFailure {
                error = "Unable to render preview: ${it.message ?: "unknown error"}"
            }
        }


        Card(shape = MaterialTheme.shapes.extraSmall) {
            when {
                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                bitmap == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxPreviewHeight)
                            .verticalScroll(scrollState)
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "PDF preview",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}