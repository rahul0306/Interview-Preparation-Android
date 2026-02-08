package com.example.interviewprep.screens.upload

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.interviewprep.util.ProfileMenuPopup
import com.example.interviewprep.util.ResumePreviewDialog
import com.google.firebase.auth.FirebaseAuth

@Suppress("UNCHECKED_CAST")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UploadScreen(
    modifier: Modifier = Modifier,
    onQuestionReady: (List<String>) -> Unit,
    onRecordingList: () -> Unit,
    onLogout: () -> Unit
) {
    val viewmodel = remember { UploadViewModel() }
    val ui by viewmodel.ui.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var resumeLabel by remember { mutableStateOf<String?>(null) }
    var showPreview by remember { mutableStateOf(false) }

    var menuExpanded by remember { mutableStateOf(false) }
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val userName = remember {
        user?.displayName ?: user?.email?.substringBefore("@") ?: "Guest"
    }
    val userEmail = remember { user?.email ?: "" }

    val pickerMimes = remember {
        arrayOf(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
        )
    }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {

                pickedUri?.let {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
                pickedUri = uri
                viewmodel.setSelected(uri)
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                resumeLabel = resolveDisplayName(context, uri) ?: "My Resume"
            }
        }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Interview Preparation",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true}) {
                        Icon(Icons.Filled.Menu, "Menu")
                    }
                })
            if (menuExpanded) {
                ProfileMenuPopup(
                    name = userName,
                    email = userEmail,
                    onDismiss = { menuExpanded = false },
                    onRecordingList = {
                        menuExpanded = false
                        onRecordingList()
                    },
                    onLogout = {
                        menuExpanded = false
                        onLogout()
                    }
                )
            }
        },
        floatingActionButton = {
            if (pickedUri == null) {
                FloatingActionButton(
                    onClick = { docPicker.launch(pickerMimes) },
                    shape = CircleShape
                ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Upload") }
            } else {
                FloatingActionButton(
                    onClick = {
                        if (!ui.isScanning && pickedUri != null) {
                            viewmodel.uploadToBackend(context = context, uri = pickedUri!!, onQuestionsReady = onQuestionReady as (List<String>) -> Unit)
                        }
                    },
                    shape = CircleShape,
                ) { Icon(Icons.Default.PlayArrow, contentDescription = null) }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 17.dp)
        ) {
            if (pickedUri == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 20.dp)
                ) {
                    Text("No resume uploaded yet.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Tap the button below to add your first resume.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (pickedUri != null && resumeLabel != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    onClick = { if (!ui.isScanning) showPreview = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = resumeLabel!!,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { docPicker.launch(pickerMimes) },
                            enabled = !ui.isScanning
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Change resume")
                        }

                        IconButton(
                            onClick = {
                                pickedUri?.let {
                                    runCatching {
                                        context.contentResolver.releasePersistableUriPermission(
                                            it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }
                                }
                                pickedUri = null
                                resumeLabel = null
                            },
                            enabled = !ui.isScanning
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove resume")
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 24.dp)
            ) {

                if (ui.isScanning) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Uploading resume and generating questions",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ui.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (showPreview && pickedUri != null) {
                ResumePreviewDialog(
                    uri = pickedUri!!,
                    title = resumeLabel ?: "Document",
                    onDismiss = { showPreview = false },
                    onOpenExternally = {
                        openExternally(context, pickedUri!!)
                    }
                )
            }
        }
    }
}

private fun resolveDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        val c: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        c?.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) it.getString(idx) else null
        }
    }.getOrNull()
}

private fun openExternally(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = uri
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.getType(uri)?.let { type = it }
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Open with"))
    }.onFailure {
        android.widget.Toast.makeText(
            context,
            "No app available to open this file.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

