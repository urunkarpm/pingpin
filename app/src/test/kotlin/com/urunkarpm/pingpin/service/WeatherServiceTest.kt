package com.urunkarpm.pingpin.service

import com.urunkarpm.pingpin.data.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherServiceTest {

    private val mockContext: android.content.Context = org.mockito.Mockito.mock(android.content.Context::class.java)
    private val dummyService = WeatherService(mockContext)


    @Test
    fun testParseWmoWeatherCode_MapsCorrectly() {
        assertEquals(WeatherCondition.SUNNY, dummyService.parseWmoWeatherCode(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, dummyService.parseWmoWeatherCode(1))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, dummyService.parseWmoWeatherCode(2))
        assertEquals(WeatherCondition.CLOUDY, dummyService.parseWmoWeatherCode(3))
        assertEquals(WeatherCondition.CLOUDY, dummyService.parseWmoWeatherCode(45))
        assertEquals(WeatherCondition.RAINY, dummyService.parseWmoWeatherCode(51))
        assertEquals(WeatherCondition.RAINY, dummyService.parseWmoWeatherCode(63))
        assertEquals(WeatherCondition.HEAVY_RAIN, dummyService.parseWmoWeatherCode(65))
        assertEquals(WeatherCondition.RAINY, dummyService.parseWmoWeatherCode(80))
        assertEquals(WeatherCondition.HEAVY_RAIN, dummyService.parseWmoWeatherCode(82))
        assertEquals(WeatherCondition.THUNDERSTORM, dummyService.parseWmoWeatherCode(95))
        assertEquals(WeatherCondition.THUNDERSTORM, dummyService.parseWmoWeatherCode(99))
    }

    @Test
    fun testGenerateTravelInsight_SevereRainAlert() {
        val insight = dummyService.generateTravelInsight(
            currentCondition = WeatherCondition.THUNDERSTORM,
            currentTemp = 25,
            morningRainChance = 80,
            eveningRainChance = 60,
            maxRainChance = 80
        )

        assertEquals("Storm Warning", insight.headline)
        assertTrue(insight.umbrellaNeeded)
        assertEquals("Storm Warning", insight.travelSafetyScore)

        val heavyRainInsight = dummyService.generateTravelInsight(
            currentCondition = WeatherCondition.HEAVY_RAIN,
            currentTemp = 23,
            morningRainChance = 75,
            eveningRainChance = 40,
            maxRainChance = 75
        )
        assertEquals("Heavy Rain Alert", heavyRainInsight.headline)
        assertTrue(heavyRainInsight.umbrellaNeeded)
        assertEquals("Heavy Rain", heavyRainInsight.travelSafetyScore)
    }

    @Test
    fun testGenerateTravelInsight_EveningRainAlert() {
        val insight = dummyService.generateTravelInsight(
            currentCondition = WeatherCondition.PARTLY_CLOUDY,
            currentTemp = 27,
            morningRainChance = 20,
            eveningRainChance = 50,
            maxRainChance = 50
        )

        assertEquals("Evening Commute Rain Alert", insight.headline)
        assertTrue(insight.umbrellaNeeded)
        assertEquals("Umbrella Advised", insight.travelSafetyScore)
    }

    @Test
    fun testGenerateTravelInsight_HighHeatWarning() {
        val insight = dummyService.generateTravelInsight(
            currentCondition = WeatherCondition.SUNNY,
            currentTemp = 36,
            morningRainChance = 10,
            eveningRainChance = 15,
            maxRainChance = 15
        )

        assertEquals("High Temperature Warning", insight.headline)
        assertFalse(insight.umbrellaNeeded)
        assertEquals("High Heat", insight.travelSafetyScore)
    }

    @Test
    fun testGenerateTravelInsight_OptimalConditions() {
        val insight = dummyService.generateTravelInsight(
            currentCondition = WeatherCondition.SUNNY,
            currentTemp = 26,
            morningRainChance = 10,
            eveningRainChance = 15,
            maxRainChance = 15
        )

        assertEquals("Optimal Commute Conditions", insight.headline)
        assertFalse(insight.umbrellaNeeded)
        assertEquals("Clear Travel", insight.travelSafetyScore)
    }

    @Test
    fun testCommuteStatus_EvaluationGood() {
        val state = com.urunkarpm.pingpin.data.model.WeatherState(
            condition = WeatherCondition.SUNNY,
            rainChancePercent = 15,
            currentTempC = 25
        )
        assertEquals(com.urunkarpm.pingpin.data.model.CommuteStatus.GOOD, state.commuteStatus)
        assertEquals("Low Risk", state.trafficRisk)
    }

    @Test
    fun testCommuteStatus_EvaluationConsiderDelaying() {
        val stateRainy = com.urunkarpm.pingpin.data.model.WeatherState(
            condition = WeatherCondition.RAINY,
            rainChancePercent = 50,
            currentTempC = 24
        )
        assertEquals(com.urunkarpm.pingpin.data.model.CommuteStatus.CONSIDER_DELAYING, stateRainy.commuteStatus)
        assertEquals("Moderate Risk", stateRainy.trafficRisk)

        val stateHeat = com.urunkarpm.pingpin.data.model.WeatherState(
            condition = WeatherCondition.SUNNY,
            rainChancePercent = 10,
            currentTempC = 36
        )
        assertEquals(com.urunkarpm.pingpin.data.model.CommuteStatus.CONSIDER_DELAYING, stateHeat.commuteStatus)
    }

    @Test
    fun testCommuteStatus_EvaluationDifficult() {
        val stateStorm = com.urunkarpm.pingpin.data.model.WeatherState(
            condition = WeatherCondition.THUNDERSTORM,
            rainChancePercent = 85,
            currentTempC = 23
        )
        assertEquals(com.urunkarpm.pingpin.data.model.CommuteStatus.DIFFICULT, stateStorm.commuteStatus)
        assertEquals("High Risk", stateStorm.trafficRisk)
    }

    @Test
    fun testWeatherState_ErrorStateHandling() {
        val errorState = com.urunkarpm.pingpin.data.model.WeatherState(
            isError = true,
            errorMessage = "Network timeout"
        )
        assertTrue(errorState.isError)
        assertEquals("Network timeout", errorState.errorMessage)
    }
}

