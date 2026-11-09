package com.resqlink.ai.guides

import org.junit.Assert.*
import org.junit.Test

class EmergencyGuideTest {

    @Test
    fun testAllGuides_containsBilingualPairsForEveryCategory() {
        val guides = EmergencyGuideRepository.getAllGuides()
        assertTrue(guides.isNotEmpty())

        val categories = GuideCategory.values()
        for (category in categories) {
            val enGuide = EmergencyGuideRepository.getGuidesByCategory(category, GuideLanguage.ENGLISH)
            val filGuide = EmergencyGuideRepository.getGuidesByCategory(category, GuideLanguage.FILIPINO)

            assertNotNull("Missing English guide for category: $category", enGuide)
            assertNotNull("Missing Filipino guide for category: $category", filGuide)
            assertTrue(enGuide!!.steps.isNotEmpty())
            assertTrue(filGuide!!.steps.isNotEmpty())
            assertTrue(enGuide.whatNotToDo.isNotEmpty())
            assertTrue(filGuide.whatNotToDo.isNotEmpty())
        }
    }

    @Test
    fun testSearchGuides_findsMatchingKeywords() {
        val resultsEn = EmergencyGuideRepository.searchGuides("compressions", GuideLanguage.ENGLISH)
        assertTrue(resultsEn.isNotEmpty())
        assertEquals(GuideCategory.CPR, resultsEn.first().category)

        val resultsFil = EmergencyGuideRepository.searchGuides("lindol", GuideLanguage.FILIPINO)
        assertTrue(resultsFil.isNotEmpty())
        assertEquals(GuideCategory.EARTHQUAKE, resultsFil.first().category)
    }

    @Test
    fun testPhilippineAgencyAttributions() {
        val quake = EmergencyGuideRepository.getGuidesByCategory(GuideCategory.EARTHQUAKE, GuideLanguage.ENGLISH)
        assertTrue(quake!!.authoritativeSource.contains("PHIVOLCS"))

        val fire = EmergencyGuideRepository.getGuidesByCategory(GuideCategory.FIRE, GuideLanguage.ENGLISH)
        assertTrue(fire!!.authoritativeSource.contains("Bureau of Fire Protection") || fire.authoritativeSource.contains("BFP"))

        val typhoon = EmergencyGuideRepository.getGuidesByCategory(GuideCategory.TYPHOON, GuideLanguage.ENGLISH)
        assertTrue(typhoon!!.authoritativeSource.contains("PAGASA"))
    }
}
