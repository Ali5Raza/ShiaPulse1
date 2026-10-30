fun main() {
    val helper = com.example.utils.HijriCalendarHelper
    for (i in 1..400) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, i)
        val h = helper.convertGregorianToHijri(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
        if (h.day == 30) {
            println("Found 30 on ${h.monthName} ${h.year} (Gregorian: ${cal.time})")
        }
    }
}
