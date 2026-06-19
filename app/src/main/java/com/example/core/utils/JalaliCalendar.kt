package com.example.core.utils

import java.util.*

object JalaliCalendar {

    data class JalaliDate(val year: Int, val month: Int, val day: Int) {
        override fun toString(): String {
            return "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
        }
    }

    fun getTodayJalali(): JalaliDate {
        val calendar = Calendar.getInstance()
        return g2d(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getYesterdayJalali(): JalaliDate {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return g2d(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun d2g(jy: Int, jm: Int, jd: Int): Calendar {
        val jY = jy - 979
        val jM = jm - 1
        val jD = jd - 1

        var jDayNo = 365 * jY + (jY / 33) * 8 + ((jY % 33 + 3) / 4)
        for (i in 0 until jM) {
            jDayNo += jMonthDays[i]
        }
        jDayNo += jD

        var gDayNo = jDayNo + 79

        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        var i = 0
        while (i < 12) {
            val num = if (i == 1 && leap) 29 else gMonthDays[i]
            if (gDayNo >= num) {
                gDayNo -= num
            } else {
                break
            }
            i++
        }
        val gm = i + 1
        val gd = gDayNo + 1

        return Calendar.getInstance().apply {
            set(gy, gm - 1, gd)
        }
    }

    fun g2d(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gY = gy - 1600
        val gM = gm - 1
        val gD = gd - 1

        var gDayNo = 365 * gY + (gY + 3) / 4 - (gY + 99) / 100 + (gY + 399) / 400
        for (i in 0 until gM) {
            gDayNo += gMonthDays[i]
        }
        if (gM > 1 && ((gY % 4 == 0 && gY % 100 != 0) || (gY % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gD

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var i = 0
        while (i < 11 && jDayNo >= jMonthDays[i]) {
            jDayNo -= jMonthDays[i]
            i++
        }
        val jm = i + 1
        val jd = jDayNo + 1

        return JalaliDate(jy, jm, jd)
    }

    private val gMonthDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    private val jMonthDays = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

    fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        return "$hour:$minute"
    }
}
