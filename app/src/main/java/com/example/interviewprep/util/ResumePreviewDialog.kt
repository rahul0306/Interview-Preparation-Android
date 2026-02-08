package com.example.interviewprep.util

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ResumePreviewDialog(
    uri: Uri,
    title: String,
    onDismiss: () -> Unit,
    onOpenExternally:() -> Unit
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val maxWidth = (config.screenWidthDp * 0.96f).dp
    val maxHeight = (config.screenHeightDp * 0.96).dp
    val mime = remember(uri) { context.contentResolver.getType(uri) ?: "" }
    val scroll = rememberScrollState()

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
                .heightIn(max = maxHeight)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(12.dp))

                when {
                    mime.equals("application/pdf", ignoreCase = true) -> {
                        PdfFirstPagePreview(uri = uri,
                            maxHeightFraction = 0.9f, maxRenderWidthPxCap = 3000)
                    }

                    else -> {
                        Text(
                            "In-app preview not available for this file type.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Close")
                }
            }
        }
    }
}
