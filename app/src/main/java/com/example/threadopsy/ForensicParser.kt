package com.example.threadopsy

import java.util.UUID
import kotlin.math.roundToInt

object ForensicParser {

    private enum class FiberCategory {
        SYNTHETIC,
        SEMI_SYNTHETIC,
        NATURAL_PLANT,
        NATURAL_ANIMAL
    }

    private data class FiberProfile(
        val displayName: String,
        val category: FiberCategory,
        val landfillYears: Int,
        val shedScore: Int,
        val breathability: Int
    )

    private val fiberDatabase = mapOf(
        "polyester" to FiberProfile("Polyester", FiberCategory.SYNTHETIC, 200, 95, 25),
        "nylon" to FiberProfile("Nylon", FiberCategory.SYNTHETIC, 40, 85, 30),
        "polyamide" to FiberProfile("Polyamide", FiberCategory.SYNTHETIC, 40, 85, 30),
        "acrylic" to FiberProfile("Acrylic", FiberCategory.SYNTHETIC, 200, 100, 15),
        "spandex" to FiberProfile("Spandex", FiberCategory.SYNTHETIC, 100, 75, 40),
        "elastane" to FiberProfile("Elastane", FiberCategory.SYNTHETIC, 100, 75, 40),
        "fleece" to FiberProfile("Fleece", FiberCategory.SYNTHETIC, 200, 100, 20),

        "rayon" to FiberProfile("Rayon", FiberCategory.SEMI_SYNTHETIC, 2, 15, 70),
        "viscose" to FiberProfile("Viscose", FiberCategory.SEMI_SYNTHETIC, 2, 15, 70),
        "modal" to FiberProfile("Modal", FiberCategory.SEMI_SYNTHETIC, 1, 10, 80),
        "lyocell" to FiberProfile("Lyocell", FiberCategory.SEMI_SYNTHETIC, 1, 5, 85),

        "cotton" to FiberProfile("Cotton", FiberCategory.NATURAL_PLANT, 1, 0, 90),
        "linen" to FiberProfile("Linen", FiberCategory.NATURAL_PLANT, 1, 0, 100),
        "hemp" to FiberProfile("Hemp", FiberCategory.NATURAL_PLANT, 1, 0, 95),
        "bamboo" to FiberProfile("Bamboo", FiberCategory.NATURAL_PLANT, 2, 5, 85),

        "wool" to FiberProfile("Wool", FiberCategory.NATURAL_ANIMAL, 3, 0, 85),
        "cashmere" to FiberProfile("Cashmere", FiberCategory.NATURAL_ANIMAL, 2, 0, 90),
        "silk" to FiberProfile("Silk", FiberCategory.NATURAL_ANIMAL, 4, 0, 80)
    )

    private val percentageRegex = Regex("""(\d{1,3})\s*%\s*([a-zA-Z]+)""")

    fun hasValidPercentage(rawText: String): Boolean {
        val normalized = rawText.lowercase()
        return percentageRegex.findAll(normalized).any { match ->
            val word = match.groupValues[2]
            fiberDatabase.containsKey(word)
        }
    }

    fun analyzeTag(
        rawText: String,
        customName: String = "",
        price: Double = 500.0
    ): ClothingItem {
        val normalizedText = rawText.lowercase()
        val detectedParts = mutableListOf<Pair<Int, FiberProfile>>()

        for (match in percentageRegex.findAll(normalizedText)) {
            val percentage = match.groupValues[1].toIntOrNull()?.coerceIn(1, 100) ?: continue
            val word = match.groupValues[2]
            val profile = fiberDatabase[word] ?: continue
            detectedParts.add(percentage to profile)
        }

        if (detectedParts.isEmpty()) {
            return ClothingItem(
                id = UUID.randomUUID().toString(),
                garmentName = customName.ifBlank { "---" },
                rawTagText = "---",
                fabricBreakdown = "---",
                armorClassTitle = "---",
                landfillYears = -1,
                microplasticShedRate = "---",
                shedDangerScore = 0,
                breathabilityBuff = 0,
                ecoAlignment = "---",
                washProtocol = listOf("---"),
                purchasePrice = price,
                timesWorn = 1
            )
        }

        val totalWeight = detectedParts.sumOf { it.first }.coerceAtLeast(1)
        val maxLandfillYears = detectedParts.maxOf { it.second.landfillYears }

        val weightedShedScore = detectedParts.sumOf { (pct, profile) ->
            (pct.toDouble() / totalWeight) * profile.shedScore
        }.roundToInt().coerceIn(0, 100)

        val weightedBreathability = detectedParts.sumOf { (pct, profile) ->
            (pct.toDouble() / totalWeight) * profile.breathability
        }.roundToInt().coerceIn(0, 100)

        val syntheticPercentage = detectedParts
            .filter { it.second.category == FiberCategory.SYNTHETIC }
            .sumOf { (it.first * 100) / totalWeight }

        val shedRateLabel = when {
            weightedShedScore == 0 -> "None (Plastic-Free)"
            weightedShedScore in 1..25 -> "Low"
            weightedShedScore in 26..65 -> "High"
            else -> "Very High"
        }

        val (fabricTypeTitle, ecoRating) = when {
            syntheticPercentage >= 80 -> "Synthetic Plastic Fiber" to "High Ocean Impact"
            syntheticPercentage in 30..79 -> "Mixed Fiber Blend" to "Moderate Impact"
            syntheticPercentage in 1..29 -> "Mostly Natural Blend" to "Low Impact"
            else -> "100% Natural Fiber" to "Eco-Friendly"
        }

        val breakdownString = detectedParts.joinToString(", ") { (pct, profile) ->
            "$pct% ${profile.displayName}"
        }

        val autoName = customName.ifBlank {
            val dominantFiber = detectedParts.maxByOrNull { it.first }?.second?.displayName ?: "Item"
            "$dominantFiber Top"
        }

        val careSteps = buildSimpleCareGuide(
            normalizedText = normalizedText,
            syntheticPercentage = syntheticPercentage,
            categories = detectedParts.map { it.second.category }.toSet()
        )

        return ClothingItem(
            id = UUID.randomUUID().toString(),
            garmentName = autoName,
            rawTagText = rawText.trim(),
            fabricBreakdown = breakdownString,
            armorClassTitle = fabricTypeTitle,
            landfillYears = maxLandfillYears,
            microplasticShedRate = shedRateLabel,
            shedDangerScore = weightedShedScore,
            breathabilityBuff = weightedBreathability,
            ecoAlignment = ecoRating,
            washProtocol = careSteps,
            purchasePrice = price,
            timesWorn = 1
        )
    }

    private fun buildSimpleCareGuide(
        normalizedText: String,
        syntheticPercentage: Int,
        categories: Set<FiberCategory>
    ): List<String> {
        val tips = mutableListOf<String>()

        if (syntheticPercentage > 0) {
            tips.add("🌊 Ocean Shield: Wash inside a microfiber-catching bag to stop plastic threads from entering waterways.")
            tips.add("❄️ Cold & Gentle: Wash at 30°C on a low spin so synthetic fibers don't snap.")
        } else {
            tips.add("🌱 100% Plastic-Free: Sheds zero microplastics! Wash in cold water to save energy and prevent shrinking.")
        }

        if (FiberCategory.NATURAL_ANIMAL in categories) {
            tips.add("🧶 Delicate Wool/Silk: Use mild soap and dry flat to keep its natural shape.")
        }
        if (FiberCategory.SEMI_SYNTHETIC in categories) {
            tips.add("✨ Soft Cellulose Care: Place in a mesh laundry bag to protect delicate wet fibers.")
        }
        if (normalizedText.contains("do not tumble") || syntheticPercentage >= 50) {
            tips.add("☀️ Air Dry Only: Hang on a rack instead of using a hot dryer to make this piece last twice as long.")
        }
        if (normalizedText.contains("hand wash")) {
            tips.add("🫧 Gentle Soak: Hand wash in cool water for 10 minutes instead of machine spinning.")
        }

        return tips
    }

    val sampleTestTags = listOf(
        "65% Polyester, 35% Cotton. Machine wash cold. Do not tumble dry.",
        "100% Cotton. Wash cold, line dry.",
        "92% Nylon, 8% Spandex. Hand wash cold.",
        "Unclear blurry tag without numbers"
    )
}