package com.example.runway.domain.model

// Clothing sizes and what they mean for fit (IIE, 2026).
// One ordered scale for tops and bottoms, so the gap between the size a garment
// is and the size the user usually wears can be read as a number of steps.

enum class ClothingSize(val label: String, val step: Int) {
    XXS("XXS", 0),
    XS("XS", 1),
    S("S", 2),
    M("M", 3),
    L("L", 4),
    XL("XL", 5),
    XXL("XXL", 6),
    XXXL("3XL", 7);

    companion object {
        // Exact match on the stored name, for values the app itself wrote.
        fun from(value: String?): ClothingSize? = entries.firstOrNull { it.name == value }

        // Lenient match for whatever the user typed on the item, since the size
        // field on a garment is free text: "Medium", "med", "m" and "32" all arrive.
        fun parse(value: String?): ClothingSize? {
            val cleaned = value?.trim()?.uppercase()?.replace(" ", "") ?: return null
            if (cleaned.isEmpty()) return null

            entries.firstOrNull { it.name == cleaned || it.label == cleaned }?.let { return it }

            WORDS[cleaned]?.let { return it }

            // A waist measurement in inches, which is how most trousers are labelled.
            cleaned.filter { it.isDigit() }.toIntOrNull()?.let { number ->
                if (number in WAIST_RANGE) return waistToSize(number)
            }
            return null
        }

        // Approximate, and deliberately so: waist-to-letter conversion differs by
        // brand and by country, so this is a starting point the user can correct.
        private fun waistToSize(waistInches: Int): ClothingSize = when {
            waistInches <= 27 -> XS
            waistInches <= 31 -> S
            waistInches <= 34 -> M
            waistInches <= 38 -> L
            waistInches <= 42 -> XL
            else -> XXL
        }

        private val WORDS = mapOf(
            "EXTRASMALL" to XS, "XSMALL" to XS,
            "SMALL" to S,
            "MEDIUM" to M, "MED" to M,
            "LARGE" to L,
            "EXTRALARGE" to XL, "XLARGE" to XL,
        )

        private val WAIST_RANGE = 22..50
    }
}

// How a garment of one size is likely to sit on someone who usually wears another.
enum class FitVerdict {
    TIGHT,
    SNUG,
    TRUE_TO_SIZE,
    LOOSE,
    OVERSIZED;

    companion object {
        // Null when either size is unknown: saying nothing is better than guessing.
        fun compare(garment: ClothingSize?, usual: ClothingSize?): FitVerdict? {
            if (garment == null || usual == null) return null
            return when (garment.step - usual.step) {
                in Int.MIN_VALUE..-2 -> TIGHT
                -1 -> SNUG
                0 -> TRUE_TO_SIZE
                1 -> LOOSE
                else -> OVERSIZED
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
