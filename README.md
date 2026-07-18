# Retrieval System Lab 🧪

An educational, fully interactive Android sandbox built with **Kotlin** and **Jetpack Compose** that demonstrates how document retrieval systems work using high-dimensional text embeddings and semantic vector calculations.

---

## 🚀 Key Features

-   **Semantic Vector Search**: Retrieve documents based on conceptual meaning rather than exact string matching using Google's `text-embedding-004` model.
-   **Local Document persistence**: Added document database built with **Room Database** (SQLite) to store custom documents and pre-calculated vectors.
-   **Interactive Similarity Slider**: Adjust the matching threshold ($0.0 \rightarrow 1.0$) dynamically to control precision and recall under real-time conditions.
-   **2D Vector Space Projection**: Projects 768-dimensional embeddings onto a 2D canvas using custom **Anchor Projection** (measuring vector angles against two dynamic concepts like *Technology* and *Nature*).
-   **Robust Offline Fallback**: Features a deterministic **Feature Hashing Vectorizer** that normalizes vocabularies into unit-length vectors offline so that the lab is fully functional even without network or API key access.
-   **Detailed Math Overviews**: Toggle visual breakdowns to inspect the raw float coordinates of retrieved embeddings and understand the dot-product similarity mathematics behind the hood.

---

## 🧮 Mathematical Engine

### 1. Cosine Similarity
Retrieval relevance is computed using the Cosine Similarity formula between the Query Vector $\mathbf{Q}$ and the Document Vector $\mathbf{D}$:

$$\text{Similarity}(\mathbf{Q}, \mathbf{D}) = \frac{\mathbf{Q} \cdot \mathbf{D}}{\|\mathbf{Q}\| \|\mathbf{D}\|} = \frac{\sum_{i=1}^{n} Q_i D_i}{\sqrt{\sum_{i=1}^{n} Q_i^2} \sqrt{\sum_{i=1}^{n} D_i^2}}$$

When vectors are normalized to unit magnitude ($\|\mathbf{V}\| = 1$), Cosine Similarity simplifies to the direct **dot product** $\mathbf{Q} \cdot \mathbf{D}$, representing the cosine of the angle between them in hyperspace.

### 2. Feature Hashing Fallback (Offline)
When offline, text is tokenized into word vectors of dimension 768 using a deterministic hashing strategy:

-   Each non-stop word $w$ is hashed deterministically into three coordinates in 768-D space:
    -   $idx_1 = |(hash(w) \oplus \text{0x55555555}) \pmod{768}|$
    -   $idx_2 = |(hash(w) \oplus \text{0xAAAAAAAA}) \pmod{768}|$
    -   $idx_3 = |(hash(w) \cdot 31) \pmod{768}|$
-   Vectors are then fully normalized to unit length so that Cosine Similarity works identically to online embeddings, representing word overlap.

---

## 🛠️ Configuration & Secret Management

The app uses the **Secrets Gradle Plugin** to read environmental keys without hardcoding them in the source.

### Setup Instructions:
1.  **Open AI Studio Secrets Panel**: Enter your Gemini API key under the secret key `GEMINI_API_KEY`.
2.  At compile time, the platform automatically injects the key into `BuildConfig.GEMINI_API_KEY` via `.env`.
3.  *Fallback*: If the key is not configured, the app automatically transitions to **Feature Hashing Offline Mode** so you can continue exploring the vector calculations and 2D charts safely.

---

## 🎨 Visual Identity & Styling

Adhering to strict **Material Design 3 (M3)** guidelines:
-   **Edge-to-Edge Canvas**: Edge-to-edge support with custom Scaffold spacing.
-   **Responsive Layouts**: Generous paddings (8dp grid spacing) and scroll constraints suited for both mobile portrait and landscape.
-   **Interactive Custom Canvas**: Draw interactive, clickable scatter plot graphs with custom touch-target hit detection.
-   **Accessible Elements**: Custom action chips and contrast metrics with touch fields of at least 48dp.
