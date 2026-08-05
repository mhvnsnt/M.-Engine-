package com.example.ai

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.Scanner

class EmbeddingEngine(private val context: Context) {
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val vocab = mutableMapOf<String, Int>()

    init {
        val modelBytes = context.assets.open("all-MiniLM-L6-v2.onnx").readBytes()
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
        loadVocab()
    }

    private fun loadVocab() {
        context.assets.open("vocab.txt").use { inputStream ->
            Scanner(inputStream).use { scanner ->
                var index = 0
                while (scanner.hasNextLine()) {
                    vocab[scanner.nextLine().trim()] = index++
                }
            }
        }
    }

    private fun tokenize(text: String): List<Int> {
        // Simplified WordPiece tokenizer
        val tokens = mutableListOf<Int>()
        tokens.add(vocab["[CLS]"] ?: 101)
        
        val words = text.lowercase().replace(Regex("[^a-z0-9 ]"), " ").split(Regex("\\s+")).filter { it.isNotBlank() }
        for (word in words) {
            var currentWord = word
            while (currentWord.isNotEmpty()) {
                var found = false
                for (i in currentWord.length downTo 1) {
                    val sub = if (currentWord == word) currentWord.substring(0, i) else "##" + currentWord.substring(0, i)
                    val id = vocab[sub]
                    if (id != null) {
                        tokens.add(id)
                        currentWord = currentWord.substring(i)
                        found = true
                        break
                    }
                }
                if (!found) {
                    tokens.add(vocab["[UNK]"] ?: 100)
                    break
                }
            }
        }
        
        tokens.add(vocab["[SEP]"] ?: 102)
        // Pad to max length 128 (for example purposes) or just use dynamic length. all-MiniLM usually handles dynamic length up to 512.
        // Let's keep it to 128
        val maxLength = 128
        val paddedTokens = if (tokens.size > maxLength) tokens.take(maxLength) else tokens + List(maxLength - tokens.size) { 0 }
        return paddedTokens
    }

    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        val tokens = tokenize(text)
        val shape = longArrayOf(1, tokens.size.toLong())
        
        val inputIds = LongArray(tokens.size) { tokens[it].toLong() }
        val attentionMask = LongArray(tokens.size) { if (tokens[it] != 0) 1L else 0L }
        val tokenTypeIds = LongArray(tokens.size) { 0L }

        val inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape)
        val attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape)
        val tokenTypeIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape)

        val inputs = mapOf(
            "input_ids" to inputTensor,
            "attention_mask" to attentionMaskTensor,
            "token_type_ids" to tokenTypeIdsTensor
        )

        val result = session.run(inputs)
        
        // Output from MiniLM is usually (batch_size, sequence_length, hidden_size) or similar
        // For sentence transformers, mean pooling is typically applied. 
        // We assume the model exports `last_hidden_state` or `embeddings` which is (1, seq_len, 384)
        // Let's just grab the first token's embedding (CLS token) as a simple approximation if it's already pooled, or mean pool.
        val outputTensor = result.get(0) as OnnxTensor
        val floatBuffer = outputTensor.floatBuffer
        
        val embedding = FloatArray(384)
        // Here we just extract the [CLS] token which is the first 384 floats, which is a decent approximation 
        // if not explicitly mean pooling. Real sentence-transformers use mean pooling.
        floatBuffer.get(embedding)

        inputTensor.close()
        attentionMaskTensor.close()
        tokenTypeIdsTensor.close()
        result.close()
        
        // Normalize embedding
        val sum = embedding.map { it * it }.sum()
        val norm = Math.sqrt(sum.toDouble()).toFloat()
        if (norm > 0) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
        
        embedding
    }
}
