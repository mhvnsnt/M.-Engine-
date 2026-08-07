package com.example.ai

class LlamaEngine {
    init {
        try {
            System.loadLibrary("mengine_llama")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    external fun stringFromJNI(): String
    
    // Future methods:
    // external fun loadModel(path: String)
    // external fun generateText(prompt: String): String
}
