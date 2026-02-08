package com.example.interviewprep.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream

fun copyVideo(context: Context, src: Uri, dest: Uri) {
    try {
        val input = when (src.scheme) {
            "file" -> FileInputStream(File(requireNotNull(src.path)))
            else -> context.contentResolver.openInputStream(src)
        }

        input.use { i ->
            context.contentResolver.openOutputStream(dest).use { o ->
                if (i != null && o != null) i.copyTo(o)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}