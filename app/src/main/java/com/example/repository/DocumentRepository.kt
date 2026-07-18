package com.example.repository

import android.util.Log
import com.example.api.EmbedContent
import com.example.api.EmbedPart
import com.example.api.EmbedRequest
import com.example.api.RetrofitClient
import com.example.data.Document
import com.example.data.DocumentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

class DocumentRepository(private val documentDao: DocumentDao) {

    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()

    suspend fun getDocumentById(id: Long): Document? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id)
    }

    suspend fun insertDocument(document: Document) = withContext(Dispatchers.IO) {
        documentDao.insertDocument(document)
    }

    suspend fun deleteDocument(document: Document) = withContext(Dispatchers.IO) {
        documentDao.deleteDocument(document)
    }

    suspend fun clearAllDocuments() = withContext(Dispatchers.IO) {
        documentDao.deleteAll()
    }

    suspend fun insertAll(documents: List<Document>) = withContext(Dispatchers.IO) {
        documentDao.insertAll(documents)
    }

    /**
     * Tries to get the real embedding from Gemini API.
     * If it fails (no API key, offline, timeout), it returns a deterministic fallback embedding.
     */
    suspend fun getEmbedding(text: String, apiKey: String): Pair<List<Float>, Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("DocumentRepository", "API key is placeholder or empty. Using fallback embedding.")
            return@withContext Pair(generateFallbackEmbedding(text), false)
        }

        try {
            val request = EmbedRequest(
                content = EmbedContent(parts = listOf(EmbedPart(text = text)))
            )
            val response = RetrofitClient.service.embedContent(apiKey, request)
            val values = response.embedding.values
            if (values.isNotEmpty()) {
                Pair(values, true) // True means successfully retrieved from API
            } else {
                Pair(generateFallbackEmbedding(text), false)
            }
        } catch (e: Exception) {
            Log.e("DocumentRepository", "Gemini Embedding API call failed: ${e.message}. Using fallback.", e)
            Pair(generateFallbackEmbedding(text), false)
        }
    }

    /**
     * Generates a deterministic high-dimensional vector based on keyword feature hashing.
     * This acts as an extremely elegant offline fallback, producing higher cosine similarity
     * scores for documents sharing similar vocabularies.
     */
    fun generateFallbackEmbedding(text: String): List<Float> {
        val vector = FloatArray(768) { 0f }
        // Simple tokenization
        val words = text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && it !in STOP_WORDS }

        if (words.isEmpty()) {
            vector[0] = 1f
            return vector.toList()
        }

        for (word in words) {
            val hash = word.hashCode()
            // Map each word to 3 distinct coordinates deterministically
            val idx1 = abs((hash xor 0x55555555) % 768)
            vector[idx1] += 1.0f

            val idx2 = abs(((hash.toLong() xor 0xAAAAAAAA) % 768).toInt())
            vector[idx2] += 0.6f

            val idx3 = abs((hash * 31) % 768)
            vector[idx3] += 0.3f
        }

        // Normalize to unit length
        var sumSq = 0f
        for (v in vector) {
            sumSq += v * v
        }
        val magnitude = sqrt(sumSq.toDouble()).toFloat()
        if (magnitude > 0f) {
            for (i in vector.indices) {
                vector[i] /= magnitude
            }
        } else {
            vector[0] = 1f
        }

        return vector.toList()
    }

    companion object {
        private val STOP_WORDS = setOf(
            "the", "and", "are", "for", "you", "but", "not", "this", "that", "with",
            "from", "they", "will", "your", "has", "have", "had", "was", "were", "been"
        )
    }
}
