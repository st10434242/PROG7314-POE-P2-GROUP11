package com.example.runway.domain.model

import com.example.runway.domain.model.ColourRelationship.ANALOGOUS
import com.example.runway.domain.model.ColourRelationship.COMPLEMENTARY
import com.example.runway.domain.model.ColourRelationship.NEUTRAL
import com.example.runway.domain.model.ColourRelationship.TONAL
import com.example.runway.domain.model.ColourRelationship.TRIADIC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// SCRUM-143: the hue-angle rules behind the smart colour matcher.
// Pairs were picked well away from the 30/90/150 degree boundaries.
class ColourMatcherTest {

    @Test
    fun `anything with a neutral is neutral`() {
        assertEquals(NEUTRAL, ColourMatcher.relate(ItemColour.BLACK, ItemColour.RED))
        assertEquals(NEUTRAL, ColourMatcher.relate(ItemColour.RED, ItemColour.BEIGE))
        assertEquals(NEUTRAL, ColourMatcher.relate(ItemColour.NAVY, ItemColour.WHITE))
    }

    @Test
    fun `two shades of the same hue are tonal`() {
        // About 4 degrees apart.
        assertEquals(TONAL, ColourMatcher.relate(ItemColour.BLUE, ItemColour.LIGHT_BLUE))
    }

    @Test
    fun `neighbours on the wheel are analogous`() {
        // About 41 degrees apart.
        assertEquals(ANALOGOUS, ColourMatcher.relate(ItemColour.BLUE, ItemColour.TEAL))
    }

    @Test
    fun `a third of the way round is triadic`() {
        // About 130 degrees apart.
        assertEquals(TRIADIC, ColourMatcher.relate(ItemColour.GREEN, ItemColour.PURPLE))
    }

    @Test
    fun `opposites are complementary`() {
        // About 179 degrees apart.
        assertEquals(COMPLEMENTARY, ColourMatcher.relate(ItemColour.RED, ItemColour.TEAL))
    }

    @Test
    fun `the gap is measured the short way round the wheel`() {
        // Red is near 354 and orange near 27, so they're 33 degrees apart, not 327.
        assertEquals(ANALOGOUS, ColourMatcher.relate(ItemColour.RED, ItemColour.ORANGE))
    }

    @Test
    fun `the order of the two colours doesn't matter`() {
        ItemColour.entries.forEach { a ->
            ItemColour.entries.forEach { b ->
                assertEquals(ColourMatcher.relate(a, b), ColourMatcher.relate(b, a))
            }
        }
    }

    @Test
    fun `easier pairings score higher`() {
        val scores = listOf(NEUTRAL, TONAL, ANALOGOUS, TRIADIC, COMPLEMENTARY).map { it.score }
        assertTrue(scores.zipWithNext().all { (a, b) -> a > b })
    }
}
