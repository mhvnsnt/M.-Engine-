import re

with open("app/src/main/java/com/example/ai/capabilities/PhysicalActuators.kt", "r") as f:
    code = f.read()

replacement = """
class ActuatorUnavailableException(message: String) : Exception(message)

/**
 * Real implementation of the AppActuator using Android ADB (Android Debug Bridge).
 * This bridges the M. Engine abstractions to actual physical devices or emulators.
 * Strictly typed to prevent arbitrary LLM string execution via shell.
 */
class AdbPhysicalAppActuator(private val deviceId: String? = null) : AppActuator {
    
    suspend fun checkAvailability(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("adb", "devices").start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            // Check if there's actually a device listed (lines after "List of devices attached")
            val lines = output.lines().filter { it.isNotBlank() }
            if (lines.size <= 1) return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun executeAdb(vararg args: String): String = withContext(Dispatchers.IO) {
        if (!checkAvailability()) throw ActuatorUnavailableException("No physical Android device or emulator connected.")
        
        val command = mutableListOf("adb")
        if (deviceId != null) {
            command.add("-s")
            command.add(deviceId)
        }
        command.addAll(args)
        
        val process = ProcessBuilder(command).start()
        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        
        if (exitCode != 0 || error.contains("error:", ignoreCase = true)) {
            throw Exception("ADB Error ($exitCode): $error $output")
        }
        output
    }
"""

code = code.replace("""/**
 * Real implementation of the AppActuator using Android ADB (Android Debug Bridge).
 * This bridges the M. Engine abstractions to actual physical devices or emulators.
 * Strictly typed to prevent arbitrary LLM string execution via shell.
 */
class AdbPhysicalAppActuator(private val deviceId: String? = null) : AppActuator {

    private suspend fun executeAdb(vararg args: String): String = withContext(Dispatchers.IO) {""", replacement)

# Fix tap, input, swipe, pressBack to return Boolean based on success rather than true
code = code.replace("executeAdb(\"shell\", \"input\", \"tap\", x.toString(), y.toString())\n        return true", "return try { executeAdb(\"shell\", \"input\", \"tap\", x.toString(), y.toString()); true } catch(e: Exception) { false }")
code = code.replace("executeAdb(\"shell\", \"input\", \"text\", text.replace(\" \", \"%s\"))\n        return true", "return try { executeAdb(\"shell\", \"input\", \"text\", text.replace(\" \", \"%s\")); true } catch(e: Exception) { false }")
code = code.replace("executeAdb(\"shell\", \"input\", \"swipe\", startX.toString(), startY.toString(), endX.toString(), endY.toString())\n        return true", "return try { executeAdb(\"shell\", \"input\", \"swipe\", startX.toString(), startY.toString(), endX.toString(), endY.toString()); true } catch(e: Exception) { false }")
code = code.replace("executeAdb(\"shell\", \"input\", \"keyevent\", \"4\")\n        return true", "return try { executeAdb(\"shell\", \"input\", \"keyevent\", \"4\"); true } catch(e: Exception) { false }")


with open("app/src/main/java/com/example/ai/capabilities/PhysicalActuators.kt", "w") as f:
    f.write(code)
