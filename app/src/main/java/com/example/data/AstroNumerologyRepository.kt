package com.example.data

import kotlinx.coroutines.flow.Flow

class AstroNumerologyRepository(
    private val astroDao: AstroDao
) {
    val astroProfileFlow: Flow<AstroProfile?> = astroDao.getAstroProfileFlow()

    suspend fun updateAstroProfile(profile: AstroProfile) {
        astroDao.insertAstroProfile(profile)
    }

    // Simplified numerology calculation
    fun calculateLifePath(birthDateStr: String): Int {
        // Assume format YYYY-MM-DD
        val digits = birthDateStr.filter { it.isDigit() }
        var sum = digits.map { it.toString().toInt() }.sum()
        while (sum > 9 && sum != 11 && sum != 22 && sum != 33) {
            sum = sum.toString().map { it.toString().toInt() }.sum()
        }
        return sum
    }
    
    fun getZodiacSign(month: Int, day: Int): String {
        return when (month) {
            1 -> if (day <= 19) "Capricorn" else "Aquarius"
            2 -> if (day <= 18) "Aquarius" else "Pisces"
            3 -> if (day <= 20) "Pisces" else "Aries"
            4 -> if (day <= 19) "Aries" else "Taurus"
            5 -> if (day <= 20) "Taurus" else "Gemini"
            6 -> if (day <= 20) "Gemini" else "Cancer"
            7 -> if (day <= 22) "Cancer" else "Leo"
            8 -> if (day <= 22) "Leo" else "Virgo"
            9 -> if (day <= 22) "Virgo" else "Libra"
            10 -> if (day <= 22) "Libra" else "Scorpio"
            11 -> if (day <= 21) "Scorpio" else "Sagittarius"
            12 -> if (day <= 21) "Sagittarius" else "Capricorn"
            else -> "Unknown"
        }
    }
    
    fun getApproximateMoonPhase(year: Int, month: Int, day: Int): String {
        // Simple approximate moon phase calculation
        var c = 0
        var e = 0.0
        var jd = 0.0
        var b = 0.0
        
        if (month < 3) {
            val y = year - 1
            val m = month + 12
            c = y / 100
            val c1 = 2 - c + (c / 4)
            jd = (365.25 * (y + 4716)).toLong() + (30.6001 * (m + 1)).toLong() + day + c1 - 1524.5
        } else {
            c = year / 100
            val c1 = 2 - c + (c / 4)
            jd = (365.25 * (year + 4716)).toLong() + (30.6001 * (month + 1)).toLong() + day + c1 - 1524.5
        }
        
        val daysSinceNew = jd - 2451549.5
        val newMoons = daysSinceNew / 29.53
        val phase = newMoons - newMoons.toLong()
        
        return when {
            phase < 0.03 || phase > 0.97 -> "New Moon"
            phase < 0.22 -> "Waxing Crescent"
            phase < 0.28 -> "First Quarter"
            phase < 0.47 -> "Waxing Gibbous"
            phase < 0.53 -> "Full Moon"
            phase < 0.72 -> "Waning Gibbous"
            phase < 0.78 -> "Last Quarter"
            else -> "Waning Crescent"
        }
    }

    fun getCurrentTransitsContext(): String {
        val calendar = java.util.Calendar.getInstance()
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val year = calendar.get(java.util.Calendar.YEAR)
        
        val currentSign = getZodiacSign(month, day)
        val moonPhase = getApproximateMoonPhase(year, month, day)
        
        val digits = "$year${String.format("%02d", month)}${String.format("%02d", day)}"
        var sum = digits.map { it.toString().toInt() }.sum()
        while (sum > 9 && sum != 11 && sum != 22 && sum != 33) {
            sum = sum.toString().map { it.toString().toInt() }.sum()
        }
        
        return "Current Astrological Weather: Sun in $currentSign. Moon Phase: $moonPhase. Universal Numerology Day: $sum."
    }
}
