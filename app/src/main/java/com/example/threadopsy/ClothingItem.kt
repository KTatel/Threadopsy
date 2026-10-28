package com.example.threadopsy

import com.google.firebase.firestore.Exclude

data class ClothingItem(
    val id: String = "",
    val garmentName: String = "Unidentified Garment",
    val rawTagText: String = "",
    val fabricBreakdown: String = "Unknown Composition",

    // --- RPG Armor Card Stats (SDG 12 & SDG 14) ---
    val armorClassTitle: String = "Mystery Rag",
    val landfillYears: Int = 1,
    val microplasticShedRate: String = "LOW", // ZERO (BIO-SAFE), MODERATE, HIGH, CRITICAL
    val shedDangerScore: Int = 0,             // 0 to 100 (for UI progress bars)
    val breathabilityBuff: Int = 50,          // 0 to 100 RPG comfort stat
    val ecoAlignment: String = "True Neutral",

    // --- Solutions: Wash Protocol & Cost-Per-Wear ---
    val washProtocol: List<String> = emptyList(),
    val purchasePrice: Double = 500.0,
    val timesWorn: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Calculated on the fly for the UI; excluded from Firestore storage
    @get:Exclude
    val costPerWear: Double
        get() = if (timesWorn > 0) purchasePrice / timesWorn else purchasePrice

    @get:Exclude
    val isCostSustainable: Boolean
        get() = timesWorn >= 30 // The SDG 12 "30-Wear Benchmark" against fast fashion
}