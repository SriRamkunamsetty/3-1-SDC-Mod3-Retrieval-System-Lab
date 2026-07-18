package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Document
import com.example.viewmodel.AnchorPoint
import com.example.viewmodel.RetrievalViewModel
import com.example.viewmodel.SearchResult
import com.example.viewmodel.UIState
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetrievalLabApp(
    viewModel: RetrievalViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        "Lab Search" to Icons.Default.Science,
        "2D Vector Space" to Icons.Default.QueryStats,
        "Documents Database" to Icons.Default.Storage
    )

    val lastOpResult by viewModel.lastOperationResult.collectAsState()
    val isOpLoading by viewModel.isOperationLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Toast-like feedback for database operations
    LaunchedEffect(lastOpResult) {
        lastOpResult?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Troubleshoot,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                "Retrieval Lab",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                "Embeddings Engine v2.4",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    val isApiKeyActive = viewModel.isApiKeyConfigured
                    val maskedKey = viewModel.maskedApiKey

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isApiKeyActive) Color(0xFF4ADE80) else Color(0xFF9CA3AF))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isApiKeyActive) "API ACTIVE: $maskedKey" else "OFFLINE ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    if (isOpLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            icon = { Icon(icon, contentDescription = label) },
                            modifier = Modifier.testTag("nav_tab_$index"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> LabSearchTab(viewModel)
                1 -> VectorSpaceTab(viewModel)
                2 -> DocumentsDatabaseTab(viewModel)
            }
        }
    }
}

@Composable
fun LabSearchTab(viewModel: RetrievalViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val threshold by viewModel.similarityThreshold.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    var expandedResultId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Lab Intro Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Lab Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "What is semantic search?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Unlike simple keyword search, semantic search converts your query and documents into high-dimensional embedding vectors using Gemini and measures their angle in space. Closer vectors indicate highly relevant content!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            // Search Input Panel
            ElevatedCard(
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "1. Query the Embedding Space",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("e.g. artificial intelligence in medicine") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_query_input"),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear query")
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "2. Set Similarity Threshold: ${"%.2f".format(threshold)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Documents with a Cosine Similarity score above this value are retrieved as 'matches'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Slider(
                        value = threshold,
                        onValueChange = { viewModel.similarityThreshold.value = it },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("similarity_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.executeSearch() },
                        enabled = searchQuery.isNotBlank() && allDocs.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("retrieve_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SavedSearch, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retrieve Relevant Documents")
                    }
                }
            }
        }

        // Search Results Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOP RETRIEVALS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                
                val apiSource = if (viewModel.isApiKeyConfigured) "ONLINE MODEL" else "OFFLINE HASHING"
                Text(
                    text = apiSource,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }

        when (val state = searchState) {
            UIState.Idle -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Troubleshoot,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (allDocs.isEmpty()) "Add documents in the Database tab first!" else "Type a query above to test the retrieval lab.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
            UIState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Querying embedding model & matching vectors...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            is UIState.Success -> {
                if (state.results.isEmpty()) {
                    item {
                        Text("No documents found in database.")
                    }
                } else {
                    items(state.results, key = { it.document.id }) { result ->
                        val doc = result.document
                        val isMatched = result.score >= threshold

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isMatched) 1.5.dp else 0.dp,
                                    color = if (isMatched) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMatched) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        DocumentCategoryChip(category = doc.category)
                                    }
                                    
                                    // Similarity Score Circle Badge
                                    Column(horizontalAlignment = Alignment.End) {
                                        val scorePercentage = (result.score * 100).coerceIn(0f, 100f)
                                        Text(
                                            text = "${"%.1f".format(scorePercentage)}%",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = getScoreColor(result.score)
                                            )
                                        )
                                        Text(
                                            text = if (isMatched) "RETRIEVED" else "EXCLUDED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isMatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = doc.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Similarity Meter (Progress Bar)
                                val meterProgress by animateFloatAsState(
                                    targetValue = result.score.coerceIn(0f, 1f),
                                    animationSpec = tween(durationMillis = 800)
                                )
                                LinearProgressIndicator(
                                    progress = { meterProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = getScoreColor(result.score),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Expanded Vector Detail
                                val isExpanded = expandedResultId == doc.id
                                TextButton(
                                    onClick = { expandedResultId = if (isExpanded) null else doc.id },
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isExpanded) "Hide Embedding Math" else "Show Embedding Math")
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            "Lab Analysis: Vector Space Details",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "• Vector dimensions: 768 float values",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "• Cosine Similarity formula: DotProduct(Q, D) / (|Q| * |D|)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "• Mathematical similarity score: ${"%.5f".format(result.score)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Vector values preview (First 10 dimensions):",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        val vectorPreview = doc.embedding?.take(10) ?: emptyList()
                                        if (vectorPreview.isNotEmpty()) {
                                            Text(
                                                text = "[ " + vectorPreview.joinToString(", ") { "%.4f".format(it) } + " ... ]",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        MaterialTheme.colorScheme.background,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(6.dp)
                                            )
                                        } else {
                                            Text("No vector values available.", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is UIState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VectorSpaceTab(viewModel: RetrievalViewModel) {
    val anchorX by viewModel.anchorXQuery.collectAsState()
    val anchorY by viewModel.anchorYQuery.collectAsState()
    val anchorPoints by viewModel.anchorPoints.collectAsState()
    val isLoading by viewModel.isAnchorLoading.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    var selectedPoint by remember { mutableStateOf<AnchorPoint?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Visualizer Info",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "What is Anchor Projection?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Since 768 dimensions can't be drawn in 2D, we project documents by measuring their similarity to two 'anchor concepts' on the X and Y axes. This shows semantic clustering visually!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Configure Concept Axes (Anchors)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = anchorX,
                            onValueChange = { viewModel.anchorXQuery.value = it },
                            label = { Text("X-Axis (Anchor X)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("anchor_x_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = anchorY,
                            onValueChange = { viewModel.anchorYQuery.value = it },
                            label = { Text("Y-Axis (Anchor Y)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("anchor_y_input"),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.calculateAnchorProjections() },
                        enabled = anchorX.isNotBlank() && anchorY.isNotBlank() && allDocs.isNotEmpty() && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("project_button")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compute 2D Projection Space")
                    }
                }
            }
        }

        item {
            Text(
                "Embedding 2D Scatter Space",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Click any point on the canvas below to inspect the document.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        item {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Projecting embeddings to concept space...")
                    }
                }
            } else if (anchorPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No points to visualize. Click Project above.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                // Render the interactive 2D Scatter Canvas
                ScatterPlotCanvas(
                    points = anchorPoints,
                    selectedPoint = selectedPoint,
                    anchorX = anchorX,
                    anchorY = anchorY,
                    onPointSelected = { selectedPoint = it }
                )
            }
        }

        // Selected point card details
        selectedPoint?.let { point ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = point.document.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                DocumentCategoryChip(category = point.document.category)
                            }
                            IconButton(onClick = { selectedPoint = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close details")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = point.document.content,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "X Coordinate",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "Similarity to \"$anchorX\"",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "${"%.4f".format(point.x)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Y Coordinate",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "Similarity to \"$anchorY\"",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "${"%.4f".format(point.y)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScatterPlotCanvas(
    points: List<AnchorPoint>,
    selectedPoint: AnchorPoint?,
    anchorX: String,
    anchorY: String,
    onPointSelected: (AnchorPoint) -> Unit
) {
    val localDensity = LocalDensity.current

    // Padding inside the canvas for axis text, titles, etc.
    val paddingDp = 48.dp
    val paddingPx = with(localDensity) { paddingDp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        val colorPrimary = MaterialTheme.colorScheme.primary
        val colorOutline = MaterialTheme.colorScheme.outline
        val colorOnSurface = MaterialTheme.colorScheme.onSurface

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        // Determine canvas working area
                        val workingWidth = size.width - (paddingPx * 2f)
                        val workingHeight = size.height - (paddingPx * 2f)

                        var nearestPoint: AnchorPoint? = null
                        var minDistance = Float.MAX_VALUE

                        for (pt in points) {
                            // Convert point coordinate (0f..1f) to pixel position
                            // Map score ranges roughly from min similarity to max similarity.
                            // To make visualization dynamic, we auto-scale or plot from -0.2f to 1.0f (since similarity can be negative or positive)
                            // But usually, positive text embeddings are between 0f and 1f. Let's assume 0.0 to 1.0.
                            val scaleX = paddingPx + (pt.x * workingWidth)
                            val scaleY = size.height - paddingPx - (pt.y * workingHeight)

                            val distance = sqrt((offset.x - scaleX) * (offset.x - scaleX) + (offset.y - scaleY) * (offset.y - scaleY))
                            // 30px touch threshold
                            if (distance < 30f && distance < minDistance) {
                                minDistance = distance
                                nearestPoint = pt
                            }
                        }

                        nearestPoint?.let {
                            onPointSelected(it)
                        }
                    }
                }
        ) {
            val workingWidth = size.width - (paddingPx * 2f)
            val workingHeight = size.height - (paddingPx * 2f)

            // Draw Background grid lines and numbers
            val gridLinesCount = 5
            for (i in 0..gridLinesCount) {
                val ratio = i.toFloat() / gridLinesCount.toFloat()
                
                // Draw vertical grid
                val gx = paddingPx + (ratio * workingWidth)
                drawLine(
                    color = colorOutline.copy(alpha = 0.15f),
                    start = Offset(gx, paddingPx),
                    end = Offset(gx, size.height - paddingPx),
                    strokeWidth = 1f
                )

                // Draw horizontal grid
                val gy = size.height - paddingPx - (ratio * workingHeight)
                drawLine(
                    color = colorOutline.copy(alpha = 0.15f),
                    start = Offset(paddingPx, gy),
                    end = Offset(size.width - paddingPx, gy),
                    strokeWidth = 1f
                )
            }

            // Draw Axes lines
            drawLine(
                color = colorOutline.copy(alpha = 0.7f),
                start = Offset(paddingPx, size.height - paddingPx),
                end = Offset(size.width - paddingPx, size.height - paddingPx),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = colorOutline.copy(alpha = 0.7f),
                start = Offset(paddingPx, paddingPx),
                end = Offset(paddingPx, size.height - paddingPx),
                strokeWidth = 2.dp.toPx()
            )

            // Plot all points
            points.forEach { pt ->
                // Coordinate positioning
                val px = paddingPx + (pt.x * workingWidth)
                val py = size.height - paddingPx - (pt.y * workingHeight)

                val dotColor = getCategoryColor(pt.document.category)
                val isSelected = selectedPoint?.document?.id == pt.document.id

                // Draw pulsing selection ring
                if (isSelected) {
                    drawCircle(
                        color = colorPrimary.copy(alpha = 0.4f),
                        radius = 16.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = colorPrimary,
                        radius = 16.dp.toPx(),
                        center = Offset(px, py),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Draw filled document node
                drawCircle(
                    color = dotColor,
                    radius = 8.dp.toPx(),
                    center = Offset(px, py)
                )
                // Draw node border for contrast
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(px, py),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Overlay axis names and titles manually via Compose Layouts to avoid Canvas font rendering issues
        // Y-Axis Anchor Label (rotated or just placed vertically)
        Text(
            text = "Y: $anchorY",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
        )

        // X-Axis Anchor Label
        Text(
            text = "X: $anchorX",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
        )

        // 0.0 Corner Label
        Text(
            text = "0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
        )

        // 1.0 Top-Right Label
        Text(
            text = "1.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp, top = 24.dp)
        )
    }
}

@Composable
fun DocumentsDatabaseTab(viewModel: RetrievalViewModel) {
    val documents by viewModel.allDocuments.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Technology") }

    val categories = listOf("Technology", "Nature", "Health", "History", "General")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Documents DB (${documents.size})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                
                Row {
                    IconButton(
                        onClick = { viewModel.resetDatabase() },
                        modifier = Modifier.testTag("reset_db_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Database",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_doc_fab_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Document")
                    }
                }
            }
        }

        if (documents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Database empty. Click Reset/Pre-populate button to load lab files.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(documents, key = { it.id }) { doc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                DocumentCategoryChip(category = doc.category)
                            }
                            
                            IconButton(
                                onClick = { viewModel.deleteDocument(doc) },
                                modifier = Modifier
                                    .size(48.dp) // Accessibility guideline touch target
                                    .testTag("delete_doc_${doc.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Document",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = doc.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (doc.embedding != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "768-D Vector Baked",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Document Dialog
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Add Document to Retrieval DB",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Its vector embedding will be calculated dynamically using the embedding model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_title_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("add_content_input"),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat
                            SuggestionChip(
                                onClick = { category = cat },
                                label = { Text(cat) },
                                colors = if (isSelected) {
                                    SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        labelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    SuggestionChipDefaults.suggestionChipColors()
                                },
                                modifier = Modifier.testTag("cat_chip_$cat")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.addDocument(title, content, category)
                                showAddDialog = false
                                title = ""
                                content = ""
                                category = "Technology"
                            },
                            enabled = title.isNotBlank() && content.isNotBlank(),
                            modifier = Modifier.testTag("save_doc_btn")
                        ) {
                            Text("Save & Vectorize")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCategoryChip(category: String) {
    Box(
        modifier = Modifier
            .background(
                color = getCategoryColor(category).copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = category.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = getCategoryColor(category)
            ),
            fontSize = 9.sp
        )
    }
}

fun getScoreColor(score: Float): Color {
    return when {
        score >= 0.6f -> Color(0xFF4CAF50) // Green
        score >= 0.4f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFF9E9E9E)          // Grey
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "technology" -> Color(0xFF2196F3) // Blue
        "nature" -> Color(0xFF4CAF50)     // Green
        "health" -> Color(0xFFE91E63)     // Pink/Red
        "history" -> Color(0xFFFF9800)    // Orange
        else -> Color(0xFF9C27B0)         // Purple
    }
}
