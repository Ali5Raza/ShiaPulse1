import re

with open("app/src/main/java/com/example/ui/DashboardScreen.kt", "r") as f:
    content = f.read()

old_logic = """                val isThursdayNight = (dayOfWeek == java.util.Calendar.THURSDAY && hourOfDay >= 17) || (dayOfWeek == java.util.Calendar.FRIDAY && hourOfDay < 5)
                val isFridayMorning = dayOfWeek == java.util.Calendar.FRIDAY && hourOfDay in 5..16
                val isTuesdayNight = (dayOfWeek == java.util.Calendar.TUESDAY && hourOfDay >= 17) || (dayOfWeek == java.util.Calendar.WEDNESDAY && hourOfDay < 5)
                val isMorning = hourOfDay in 5..11
                val isNight = hourOfDay >= 18 || hourOfDay < 5
                
                val recIds: List<String>
                val recAamalIds: List<String>
                val recTitleEng: String
                val recTitleUr: String
                val recDescEng: String
                val recDescUr: String

                when {
                    isThursdayNight -> {
                        recIds = listOf("kumayl")
                        recAamalIds = listOf("aamal_jumuah")
                        recTitleEng = "Thursday Night Specials"
                        recTitleUr = "شبِ جمعہ کی مخصوص دعائیں"
                        recDescEng = "Highly recommended for Shab-e-Juma"
                        recDescUr = "شب جمعہ کی برکتوں کے لئے مخصوص"
                    }
                    isFridayMorning -> {
                        recIds = listOf("nudba")
                        recAamalIds = listOf("aamal_jumuah")
                        recTitleEng = "Friday Blessings"
                        recTitleUr = "جمعہ کے مبارک اذکار"
                        recDescEng = "Nurture your soul on Friday morning"
                        recDescUr = "جمعہ کے بابرکت آغاز کے لئے"
                    }
                    isTuesdayNight -> {
                        recIds = listOf("tawassul", "nade_ali")
                        recAamalIds = emptyList()
                        recTitleEng = "Tuesday Night Supplications"
                        recTitleUr = "منگل کی رات کے اذکار"
                        recDescEng = "Dua Tawassul for fulfilling needs"
                        recDescUr = "حاجات کی برآوری کے لئے بہترین"
                    }
                    isMorning -> {
                        recIds = listOf("sabah", "ahad")
                        recAamalIds = emptyList()
                        recTitleEng = "Morning Devotionals"
                        recTitleUr = "صبح کے بابرکت اذکار"
                        recDescEng = "Start your day with divine light"
                        recDescUr = "اپنے دن کا آغاز نور الہیٰ سے کریں"
                    }
                    isNight -> {
                        recIds = listOf("dua_mashlool", "dua_mujeer")
                        recAamalIds = emptyList()
                        recTitleEng = "Nightly Supplications"
                        recTitleUr = "رات کی مخصوص مناجات"
                        recDescEng = "Find peace before resting"
                        recDescUr = "سونے سے قبل سکونِ قلب کے لئے"
                    }
                    dayOfWeek == java.util.Calendar.SUNDAY -> {
                        recIds = listOf("hadith_kisa")
                        recAamalIds = listOf("aamal_ayat")
                        recTitleEng = "Sunday Specials"
                        recTitleUr = "اتوار کے مبارک اذکار"
                        recDescEng = "Aesthetic Sunday blessings"
                        recDescUr = "اتوار کے دن برائے تسکینِ روح"
                    }
                    else -> {
                        recIds = listOf("ashura", "ziyarat_aminullah")
                        recAamalIds = emptyList()
                        recTitleEng = "Daily Recommendations"
                        recTitleUr = "روزمرہ کے منتخب اذکار"
                        recDescEng = "Highly recommended everyday"
                        recDescUr = "روزانہ تلاوت کے لئے بہترین"
                    }
                }"""

new_logic = """                val isThursdayNight = (dayOfWeek == java.util.Calendar.THURSDAY && hourOfDay >= 17) || (dayOfWeek == java.util.Calendar.FRIDAY && hourOfDay < 5)
                val isFridayMorning = dayOfWeek == java.util.Calendar.FRIDAY && hourOfDay in 5..16
                val isTuesdayNight = (dayOfWeek == java.util.Calendar.TUESDAY && hourOfDay >= 17) || (dayOfWeek == java.util.Calendar.WEDNESDAY && hourOfDay < 5)
                val isMorning = hourOfDay in 5..11
                val isNight = hourOfDay >= 18 || hourOfDay < 5
                
                val hijriDateForRec = com.example.utils.HijriCalendarHelper.convertGregorianToHijri(currentCal)
                val hMonth = hijriDateForRec.month
                
                val isRajab = hMonth == 7
                val isShaban = hMonth == 8
                val isRamadan = hMonth == 9
                val isZilhajj = hMonth == 12
                val isMuharramSafar = hMonth == 1 || hMonth == 2
                
                val recIds: List<String>
                val recAamalIds: List<String>
                val recTitleEng: String
                val recTitleUr: String
                val recDescEng: String
                val recDescUr: String

                when {
                    isThursdayNight -> {
                        recIds = listOf("kumayl")
                        recAamalIds = listOf("aamal_jumuah")
                        recTitleEng = "Thursday Night Specials"
                        recTitleUr = "شبِ جمعہ کی مخصوص دعائیں"
                        recDescEng = "Highly recommended for Shab-e-Juma"
                        recDescUr = "شب جمعہ کی برکتوں کے لئے مخصوص"
                    }
                    isFridayMorning -> {
                        recIds = listOf("nudba")
                        recAamalIds = listOf("aamal_jumuah")
                        recTitleEng = "Friday Blessings"
                        recTitleUr = "جمعہ کے مبارک اذکار"
                        recDescEng = "Nurture your soul on Friday morning"
                        recDescUr = "جمعہ کے بابرکت آغاز کے لئے"
                    }
                    isRamadan -> {
                        recIds = listOf("iftitah")
                        recAamalIds = listOf("aamal_ramadan", "aamal_shabe_qadr")
                        recTitleEng = "Ramadan Blessings"
                        recTitleUr = "رمضان کے اذکار"
                        recDescEng = "Holy month of fasting"
                        recDescUr = "رمضان کی برکتیں"
                    }
                    isRajab -> {
                        recIds = listOf("dua_mashlool")
                        recAamalIds = listOf("aamal_rajab", "aamal_umme_dawood")
                        recTitleEng = "Rajab Devotions"
                        recTitleUr = "رجب کے اعمال"
                        recDescEng = "Recommended acts for Rajab"
                        recDescUr = "رجب کے مستحب اعمال"
                    }
                    isShaban -> {
                        recIds = listOf("hadith_kisa")
                        recAamalIds = listOf("aamal_shaban")
                        recTitleEng = "Shaban Blessings"
                        recTitleUr = "شعبان کے اعمال"
                        recDescEng = "Recommended acts for Shaban"
                        recDescUr = "شعبان کے مستحب اعمال"
                    }
                    isZilhajj -> {
                        recIds = listOf("dua_mashlool")
                        recAamalIds = listOf("aamal_zilhajj", "aamal_eid_ghadir")
                        recTitleEng = "Dhu al-Hijjah Acts"
                        recTitleUr = "ذی الحجہ کے اعمال"
                        recDescEng = "Blessings of the sacred month"
                        recDescUr = "حج کے مہینے کی برکتیں"
                    }
                    isMuharramSafar -> {
                        recIds = listOf("ashura", "ziyarat_arbaeen")
                        recAamalIds = listOf("aamal_ashura")
                        recTitleEng = "Days of Mourning"
                        recTitleUr = "ایامِ عزا کے اذکار"
                        recDescEng = "Ziyarat for Muharram & Safar"
                        recDescUr = "محرم اور صفر کی زیارات"
                    }
                    isTuesdayNight -> {
                        recIds = listOf("tawassul", "nade_ali")
                        recAamalIds = emptyList()
                        recTitleEng = "Tuesday Night Supplications"
                        recTitleUr = "منگل کی رات کے اذکار"
                        recDescEng = "Dua Tawassul for fulfilling needs"
                        recDescUr = "حاجات کی برآوری کے لئے بہترین"
                    }
                    isMorning -> {
                        recIds = listOf("sabah", "ahad")
                        recAamalIds = emptyList()
                        recTitleEng = "Morning Devotionals"
                        recTitleUr = "صبح کے بابرکت اذکار"
                        recDescEng = "Start your day with divine light"
                        recDescUr = "اپنے دن کا آغاز نور الہیٰ سے کریں"
                    }
                    isNight -> {
                        recIds = listOf("dua_mashlool", "dua_mujeer")
                        recAamalIds = emptyList()
                        recTitleEng = "Nightly Supplications"
                        recTitleUr = "رات کی مخصوص مناجات"
                        recDescEng = "Find peace before resting"
                        recDescUr = "سونے سے قبل سکونِ قلب کے لئے"
                    }
                    dayOfWeek == java.util.Calendar.SUNDAY -> {
                        recIds = listOf("hadith_kisa")
                        recAamalIds = listOf("aamal_ayat")
                        recTitleEng = "Sunday Specials"
                        recTitleUr = "اتوار کے مبارک اذکار"
                        recDescEng = "Aesthetic Sunday blessings"
                        recDescUr = "اتوار کے دن برائے تسکینِ روح"
                    }
                    else -> {
                        recIds = listOf("ashura", "ziyarat_aminullah")
                        recAamalIds = emptyList()
                        recTitleEng = "Daily Recommendations"
                        recTitleUr = "روزمرہ کے منتخب اذکار"
                        recDescEng = "Highly recommended everyday"
                        recDescUr = "روزانہ تلاوت کے لئے بہترین"
                    }
                }"""

content = content.replace(old_logic, new_logic)

with open("app/src/main/java/com/example/ui/DashboardScreen.kt", "w") as f:
    f.write(content)
