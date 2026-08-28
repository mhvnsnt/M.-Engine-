package com.example.ai.capabilities

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real implementation of the AppActuator using Android ADB (Android Debug Bridge).
 * This bridges the M. Engine abstractions to actual physical devices or emulators.
 * Strictly typed to prevent arbitrary LLM string execution via shell.
 */
class AdbPhysicalAppActuator(private val deviceId: String? = null) : AppActuator {
    
    private suspend fun executeAdb(vararg args: String): String = withContext(Dispatchers.IO) {
        val command = mutableListOf("adb")
        if (deviceId != null) {
            command.add("-s")
            command.add(deviceId)
        }
        command.addAll(args)
        
        val process = ProcessBuilder(command).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        output
    }

    override suspend fun launch(packageName: String): Boolean {
        require(packageName.matches(Regex("^[a-zA-Z0-9_.]+$"))) { "Invalid package name security check" }
        val result = executeAdb("shell", "monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1")
        return !result.contains("error", ignoreCase = true)
    }

    override suspend fun tap(x: Int, y: Int): Boolean {
        return try { executeAdb("shell", "input", "tap", x.toString(), y.toString()); true } catch(e: Exception) { false }
    }

    override suspend fun inputText(text: String): Boolean {
        // Simple sanitization for space replacement
        return try { executeAdb("shell", "input", "text", text.replace(" ", "%s")); true } catch(e: Exception) { false }
    }

    override suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        return try { executeAdb("shell", "input", "swipe", startX.toString(), startY.toString(), endX.toString(), endY.toString()); true } catch(e: Exception) { false }
    }

    override suspend fun pressBack(): Boolean {
        return try { executeAdb("shell", "input", "keyevent", "4"); true } catch(e: Exception) { false }
    }

    override suspend fun recordVideo(durationMs: Long, outputPath: String): Boolean {
        require(outputPath.endsWith(".mp4")) { "Output must be mp4" }
        val seconds = (durationMs / 1000).coerceAtMost(180) // ADB max is 3 minutes
        val devicePath = "/sdcard/video_${System.currentTimeMillis()}.mp4"
        executeAdb("shell", "screenrecord", "--time-limit", seconds.toString(), devicePath)
        executeAdb("pull", devicePath, outputPath)
        executeAdb("shell", "rm", devicePath)
        return File(outputPath).exists()
    }

    override suspend fun dumpUi(outputPath: String): String {
        require(outputPath.endsWith(".xml")) { "Output must be xml" }
        val devicePath = "/sdcard/window_dump_${System.currentTimeMillis()}.xml"
        executeAdb("shell", "uiautomator", "dump", devicePath)
        executeAdb("pull", devicePath, outputPath)
        executeAdb("shell", "rm", devicePath)
        val file = File(outputPath)
        return if (file.exists()) file.readText() else ""
    }

    override suspend fun observe(): ScreenObservation {
        val timestamp = System.currentTimeMillis()
        val screenshotPath = "/sdcard/screen_$timestamp.png"
        val localPath = "build/reports/screen_$timestamp.png"
        
        executeAdb("shell", "screencap", "-p", screenshotPath)
        executeAdb("pull", screenshotPath, localPath)
        executeAdb("shell", "rm", screenshotPath)
        
        val localXml = "build/reports/dump_$timestamp.xml"
        val uiTree = dumpUi(localXml)
        
        return ScreenObservation(timestamp, localPath, uiTree)
    }

    override suspend fun captureSession(durationMs: Long, actions: List<InteractionEvent>): VideoSessionTrace {
        val timestamp = System.currentTimeMillis()
        val localVideoPath = "build/reports/video_$timestamp.mp4"
        
        recordVideo(durationMs, localVideoPath)
        
        return VideoSessionTrace(durationMs, localVideoPath, emptyList())
    }

    override suspend fun terminate(packageName: String) {
        require(packageName.matches(Regex("^[a-zA-Z0-9_.]+$"))) { "Invalid package name" }
        executeAdb("shell", "am", "force-stop", packageName)
    }
}
