package com.example.utils
import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.util.Date

object TestIcu {
    fun test() {
        val cal = IslamicCalendar(TimeZone.getDefault(), ULocale.getDefault())
        cal.time = Date()
    }
}
