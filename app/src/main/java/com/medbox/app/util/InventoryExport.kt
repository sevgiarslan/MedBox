package com.medbox.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Writes an inventory export to cache and opens the share sheet so it can be sent to anyone. */
fun shareInventoryExport(context: Context, json: String) {
    val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val file = File(exportsDir, "medbox_export_$timestamp.json")
    file.writeText(json)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "İlaç listesini paylaş"))
}
