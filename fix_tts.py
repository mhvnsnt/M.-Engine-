with open('app/src/main/java/com/example/ai/TTSEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'fun speak(text: String) {',
    'fun speak(text: String, flush: Boolean = true) {\n        if (isInitialized) {\n            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD\n            tts?.speak(text, queueMode, null, "TTS_ID")\n        }\n    }\n    fun speakOld(text: String) {'
)

with open('app/src/main/java/com/example/ai/TTSEngine.kt', 'w') as f:
    f.write(content)
