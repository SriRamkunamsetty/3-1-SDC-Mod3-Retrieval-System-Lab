package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.Document
import com.example.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class SearchResult(
    val document: Document,
    val score: Float,
    val isMatched: Boolean // based on threshold
)

data class AnchorPoint(
    val document: Document,
    val x: Float, // Similarity to Anchor X
    val y: Float  // Similarity to Anchor Y
)

sealed interface UIState {
    object Idle : UIState
    object Loading : UIState
    data class Success(val results: List<SearchResult>) : UIState
    data class Error(val message: String) : UIState
}

class RetrievalViewModel(application: Application) : AndroidViewModel(application) {

    val isApiKeyConfigured: Boolean
        get() = getApiKey().isNotBlank() && getApiKey().length > 10 && !getApiKey().contains("your_api_key")

    val maskedApiKey: String
        get() {
            val key = getApiKey()
            if (key.length <= 8) return "Active"
            return key.take(4) + "..." + key.takeLast(4)
        }

    private val repository: DocumentRepository
    val allDocuments: StateFlow<List<Document>>

    // Search Settings State
    var searchQuery = MutableStateFlow("")
    var similarityThreshold = MutableStateFlow(0.35f)
    
    // API and Operation Status
    private val _isOperationLoading = MutableStateFlow(false)
    val isOperationLoading = _isOperationLoading.asStateFlow()

    private val _lastOperationResult = MutableStateFlow<String?>(null)
    val lastOperationResult = _lastOperationResult.asStateFlow()

    // Retrieval State
    private val _searchState = MutableStateFlow<UIState>(UIState.Idle)
    val searchState = _searchState.asStateFlow()

    // Anchor Projection State
    var anchorXQuery = MutableStateFlow("Technology")
    var anchorYQuery = MutableStateFlow("Nature")
    
    private val _anchorPoints = MutableStateFlow<List<AnchorPoint>>(emptyList())
    val anchorPoints = _anchorPoints.asStateFlow()

    private val _isAnchorLoading = MutableStateFlow(false)
    val isAnchorLoading = _isAnchorLoading.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DocumentRepository(database.documentDao())
        allDocuments = repository.allDocuments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Automatically pre-populate default documents if the DB is empty
        viewModelScope.launch {
            repository.allDocuments.collect { docs ->
                if (docs.isEmpty()) {
                    prepopulateSampleData()
                } else if (_anchorPoints.value.isEmpty()) {
                    // Calculate projection for existing documents
                    calculateAnchorProjections()
                }
            }
        }
    }

    /**
     * Pre-populates the database with interesting high-quality starter documents.
     */
    private fun prepopulateSampleData() {
        viewModelScope.launch {
            _isOperationLoading.value = true
            _lastOperationResult.value = "Initializing default lab documents..."
            
            val samples = listOf(
                Document(
                    title = "The Wonders of Quantum Computing",
                    content = "Quantum computing utilizes the principles of quantum mechanics, such as superposition and entanglement, to process information. By using qubits, they solve complex optimization and molecular modeling problems exponentially faster than supercomputers.",
                    category = "Technology"
                ),
                Document(
                    title = "Deep Learning and Neural Networks",
                    content = "Deep learning is a subset of machine learning inspired by human brain structures. It uses multi-layered artificial neural networks to learn complex representations from massive datasets, powering image recognition, language translation, and self-driving cars.",
                    category = "Technology"
                ),
                Document(
                    title = "Photosynthesis: Nature's Solar Engine",
                    content = "Photosynthesis is the process where green plants, algae, and some bacteria convert light energy into chemical energy, synthesizing organic sugars from carbon dioxide and water. It releases vital oxygen into the atmosphere.",
                    category = "Nature"
                ),
                Document(
                    title = "Ecosystem of Coral Reefs",
                    content = "Coral reefs are the rainforests of the sea, housing 25% of all marine species despite occupying less than 0.1% of the ocean floor. They protect coasts, provide nourishment, and support local fishing and tourism economies.",
                    category = "Nature"
                ),
                Document(
                    title = "Deep Sleep Consolidates Long-Term Memory",
                    content = "Slow-wave deep sleep is critical for brain recovery and cognitive stability. During deep sleep stages, the hippocampus replays daily events, transferring them into the neocortex for long-term storage while flushing metabolic wastes.",
                    category = "Health"
                ),
                Document(
                    title = "Mindfulness Meditation and Stress Reduction",
                    content = "Mindfulness meditation trains the mind to focus on present-moment awareness, like the breath, without judgment. Clinical studies show regular practice reduces cortisol levels, shrinking the amygdala's response to emotional stress.",
                    category = "Health"
                ),
                Document(
                    title = "The Renaissance: Rebirth of Art and Science",
                    content = "Spanning the 14th to 17th centuries, the European Renaissance was a cultural rebirth bridging the Middle Ages and modernity. It championed humanism, leading to legendary advances in art by Leonardo da Vinci and scientific revelations by Galileo.",
                    category = "History"
                )
            )

            // Generate embeddings for all samples
            val processedSamples = samples.map { doc ->
                val (embedding, retrieved) = repository.getEmbedding(
                    text = "${doc.title} ${doc.content}",
                    apiKey = getApiKey()
                )
                doc.copy(embedding = embedding)
            }

            repository.insertAll(processedSamples)
            _isOperationLoading.value = false
            _lastOperationResult.value = "Pre-populated ${processedSamples.size} documents in database!"
            calculateAnchorProjections()
        }
    }

    /**
     * Executes the query search, calculating Cosine Similarity between query embedding and DB documents.
     */
    fun executeSearch() {
        val query = searchQuery.value.trim()
        if (query.isEmpty()) {
            _searchState.value = UIState.Idle
            return
        }

        viewModelScope.launch {
            _searchState.value = UIState.Loading
            
            // 1. Fetch query embedding
            val apiKey = getApiKey()
            val (queryEmbedding, isOnline) = repository.getEmbedding(query, apiKey)
            
            // 2. Fetch all documents
            val docs = allDocuments.value
            if (docs.isEmpty()) {
                _searchState.value = UIState.Error("No documents found in database. Add some first.")
                return@launch
            }

            // 3. Compute cosine similarity
            val results = docs.map { doc ->
                val docEmbedding = doc.embedding ?: repository.generateFallbackEmbedding("${doc.title} ${doc.content}")
                val score = cosineSimilarity(queryEmbedding, docEmbedding)
                SearchResult(
                    document = doc,
                    score = score,
                    isMatched = score >= similarityThreshold.value
                )
            }.sortedByDescending { it.score }

            val apiSource = if (isOnline) "Gemini API (Online)" else "Keyword Hash (Offline Fallback)"
            _searchState.value = UIState.Success(results)
            _lastOperationResult.value = "Retrieved matches using $apiSource for query: \"$query\""
        }
    }

    /**
     * Adds a new document to the database, fetching its embedding.
     */
    fun addDocument(title: String, content: String, category: String) {
        if (title.isBlank() || content.isBlank() || category.isBlank()) {
            _lastOperationResult.value = "Error: All fields are required."
            return
        }

        viewModelScope.launch {
            _isOperationLoading.value = true
            _lastOperationResult.value = "Generating embedding for document..."
            
            val (embedding, retrieved) = repository.getEmbedding(
                text = "$title $content",
                apiKey = getApiKey()
            )

            val newDoc = Document(
                title = title.trim(),
                content = content.trim(),
                category = category.trim(),
                embedding = embedding
            )

            repository.insertDocument(newDoc)
            _isOperationLoading.value = false
            val sourceStr = if (retrieved) "Gemini API" else "Offline Hash"
            _lastOperationResult.value = "Saved \"$title\" (Embedding sourced from $sourceStr)"
            
            // Recalculate projections for the graph
            calculateAnchorProjections()
        }
    }

    /**
     * Deletes a document.
     */
    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            _lastOperationResult.value = "Deleted document: \"${document.title}\""
            calculateAnchorProjections()
        }
    }

    /**
     * Clears all documents and resets.
     */
    fun resetDatabase() {
        viewModelScope.launch {
            repository.clearAllDocuments()
            _anchorPoints.value = emptyList()
            _searchState.value = UIState.Idle
            prepopulateSampleData()
        }
    }

    /**
     * Projects document embeddings into 2D space based on their Cosine Similarity to Anchor X and Anchor Y.
     */
    fun calculateAnchorProjections() {
        val anchorX = anchorXQuery.value.trim()
        val anchorY = anchorYQuery.value.trim()
        val docs = allDocuments.value

        if (docs.isEmpty()) return

        viewModelScope.launch {
            _isAnchorLoading.value = true
            val apiKey = getApiKey()
            
            val (embX, _) = repository.getEmbedding(anchorX, apiKey)
            val (embY, _) = repository.getEmbedding(anchorY, apiKey)

            val points = docs.map { doc ->
                val docEmbedding = doc.embedding ?: repository.generateFallbackEmbedding("${doc.title} ${doc.content}")
                val scoreX = cosineSimilarity(docEmbedding, embX)
                val scoreY = cosineSimilarity(docEmbedding, embY)
                AnchorPoint(doc, scoreX, scoreY)
            }

            _anchorPoints.value = points
            _isAnchorLoading.value = false
        }
    }

    /**
     * Computes the Cosine Similarity between two vectors.
     */
    private fun cosineSimilarity(vectorA: List<Float>, vectorB: List<Float>): Float {
        if (vectorA.size != vectorB.size || vectorA.isEmpty()) return 0f
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0.0) 0f else (dotProduct / denominator).toFloat()
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY ?: ""
    }
}
