import re

with open("app/src/main/java/com/example/ui/DashboardScreen.kt", "r") as f:
    content = f.read()

# Replace variables
old_vars = """                var loadedDuas by remember { mutableStateOf<List<com.example.ui.ShiaDua>>(emptyList()) }
                LaunchedEffect(Unit) {
                    loadedDuas = com.example.ui.loadDuas(context)
                }"""

new_vars = """                var loadedDuas by remember { mutableStateOf<List<com.example.ui.ShiaDua>>(emptyList()) }
                var loadedAamal by remember { mutableStateOf<List<com.example.ui.AamalItem>>(emptyList()) }
                LaunchedEffect(Unit) {
                    loadedDuas = com.example.ui.loadDuas(context)
                    loadedAamal = com.example.ui.admin.AamalSyncManager.loadAamal(context)
                }"""
content = content.replace(old_vars, new_vars)

old_logic = """                val recIds: List<String>
                val recTitleEng: String
                val recTitleUr: String
                val recDescEng: String
                val recDescUr: String

                when {
                    isThursdayNight -> {
                        recIds = listOf("kumayl", "yastasheer")
                        recTitleEng = "Thursday Night Specials"
                        recTitleUr = "شبِ جمعہ کی مخصوص دعائیں"
                        recDescEng = "Highly recommended for Shab-e-Juma"
                        recDescUr = "شب جمعہ کی برکتوں کے لئے مخصوص"
                    }
                    isFridayMorning -> {
                        recIds = listOf("nudba", "ziyarat_aal_yaseen")
                        recTitleEng = "Friday Blessings"
                        recTitleUr = "جمعہ کے مبارک اذکار"
                        recDescEng = "Nurture your soul on Friday morning"
                        recDescUr = "جمعہ کے بابرکت آغاز کے لئے"
                    }
                    isTuesdayNight -> {
                        recIds = listOf("tawassul", "nade_ali")
                        recTitleEng = "Tuesday Night Supplications"
                        recTitleUr = "منگل کی رات کے اذکار"
                        recDescEng = "Dua Tawassul for fulfilling needs"
                        recDescUr = "حاجات کی برآوری کے لئے بہترین"
                    }
                    isMorning -> {
                        recIds = listOf("sabah", "ahad")
                        recTitleEng = "Morning Devotionals"
                        recTitleUr = "صبح کے بابرکت اذکار"
                        recDescEng = "Start your day with divine light"
                        recDescUr = "اپنے دن کا آغاز نور الہیٰ سے کریں"
                    }
                    isNight -> {
                        recIds = listOf("dua_mashlool", "dua_mujeer")
                        recTitleEng = "Nightly Supplications"
                        recTitleUr = "رات کی مخصوص مناجات"
                        recDescEng = "Find peace before resting"
                        recDescUr = "سونے سے قبل سکونِ قلب کے لئے"
                    }
                    dayOfWeek == java.util.Calendar.SUNDAY -> {
                        recIds = listOf("hadith_kisa", "munajat_imamali")
                        recTitleEng = "Sunday Specials"
                        recTitleUr = "اتوار کے مبارک اذکار"
                        recDescEng = "Aesthetic Sunday blessings"
                        recDescUr = "اتوار کے دن برائے تسکینِ روح"
                    }
                    else -> {
                        recIds = listOf("ashura", "ziyarat_aminullah")
                        recTitleEng = "Daily Recommendations"
                        recTitleUr = "روزمرہ کے منتخب اذکار"
                        recDescEng = "Highly recommended everyday"
                        recDescUr = "روزانہ تلاوت کے لئے بہترین"
                    }
                }

                val recommendedList = loadedDuas.filter { recIds.contains(it.id) }.take(2)"""

new_logic = """                val recIds: List<String>
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
                }

                data class SmartRec(
                    val id: String,
                    val type: String,
                    val title: String,
                    val titleUr: String,
                    val titleAr: String,
                    val category: String,
                    val descEng: String
                )

                val recommendedList = mutableListOf<SmartRec>()
                
                loadedDuas.filter { recIds.contains(it.id) }.take(2).forEach {
                    recommendedList.add(SmartRec(
                        id = it.id,
                        type = "duas",
                        title = it.title,
                        titleUr = it.titleUr ?: it.title,
                        titleAr = it.titleAr ?: it.title,
                        category = it.category,
                        descEng = it.description
                    ))
                }
                
                loadedAamal.filter { recAamalIds.contains(it.id) }.take(2).forEach {
                    recommendedList.add(SmartRec(
                        id = it.id,
                        type = "aamal",
                        title = it.title,
                        titleUr = it.titleUr ?: it.title,
                        titleAr = it.titleAr ?: it.title,
                        category = it.category,
                        descEng = it.description
                    ))
                }
                """

content = content.replace(old_logic, new_logic)

with open("app/src/main/java/com/example/ui/DashboardScreen.kt", "w") as f:
    f.write(content)
