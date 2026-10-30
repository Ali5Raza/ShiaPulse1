import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import java.util.Calendar

fun main() {
    val cal = IslamicCalendar(TimeZone.getDefault(), java.util.Locale.getDefault())
    // ... wait, this won't compile without android SDK
}
