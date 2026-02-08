package com.example.interviewprep.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object RecordingStore {
    private const val PREF = "recordings_pref"
    private const val KEY = "recordings_json"

    fun getAll(context: Context): List<RecordingItem> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<RecordingItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                RecordingItem(
                    id = o.getString("id"),
                    filePath = o.getString("filePath"),
                    createdAt = o.getLong("createdAt")
                )
            )
        }
        // newest first
        return list.sortedByDescending { it.createdAt }
    }

    fun add(context: Context, file: File, createdAt: Long = System.currentTimeMillis()) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray(sp.getString(KEY, "[]") ?: "[]")

        val obj = JSONObject().apply {
            put("id", file.nameWithoutExtension)
            put("filePath", file.absolutePath)
            put("createdAt", createdAt)
        }

        val newArr = JSONArray().put(obj)
        for (i in 0 until arr.length()) newArr.put(arr.getJSONObject(i))

        sp.edit().putString(KEY, newArr.toString()).apply()
    }

    fun delete(context: Context, item: RecordingItem) {

        runCatching { File(item.filePath).delete() }

        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray(sp.getString(KEY, "[]") ?: "[]")
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getString("filePath") != item.filePath) newArr.put(o)
        }
        sp.edit().putString(KEY, newArr.toString()).apply()
    }
}