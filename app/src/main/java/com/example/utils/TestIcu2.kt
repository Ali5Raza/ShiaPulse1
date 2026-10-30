package com.example.utils
import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.util.Date

object TestIcu2 {
    fun test() {
        val cal = IslamicCalendar(TimeZone.getDefault(), ULocale.getDefault())
        cal.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA)
        cal.time = Date()
    }
}
