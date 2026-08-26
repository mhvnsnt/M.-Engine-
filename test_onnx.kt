package com.example.ai

import java.io.File
import java.io.FileOutputStream
import android.content.Context

fun copyAsset(context: Context) {
    val file = File(context.cacheDir, "all-MiniLM-L6-v2.onnx")
    context.assets.open("all-MiniLM-L6-v2.onnx").use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
}
