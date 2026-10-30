package com.example.data

import android.content.Context

data class MasoomEvent(
    val title: String,
    val dateStringHijri: String,
    val eventType: EventType,
    val description: String,
    val id: String = "",
    val titleUrdu: String = "",
    val titleArabic: String = "",
    val hijriMonth: Int = 0,
    val hijriDay: Int = 0,
    val masoomIndex: Int = 0,
    val region: String = "ALL", // "ALL", "SOUTH_ASIA", "IRAN_HAWZA"
    val descriptionUrdu: String = "",
    val descriptionArabic: String = "",
    val sourceUrl: String = ""
) {
    fun localizedTitle(languageCode: String): String = when (languageCode) {
        "ur" -> titleUrdu.ifBlank { title }
        "ar" -> titleArabic.ifBlank { title }
        else -> title
    }

    fun localizedDescription(languageCode: String): String = when (languageCode) {
        "ur" -> descriptionUrdu.ifBlank { description }
        "ar" -> descriptionArabic.ifBlank { description }
        else -> description
    }

    /** Numeric JSON fields are canonical; the date string supports older remote records. */
    fun occursOn(day: Int, month: Int, monthName: String): Boolean {
        if (hijriDay in 1..30 && hijriMonth in 1..12) {
            return hijriDay == day && hijriMonth == month
        }
        return dateStringHijri.split("/").any { rawDate ->
            val parts = rawDate.substringBefore("(")
                .trim()
                .replace(Regex("\\s+"), " ")
                .split(" ", limit = 2)
            parts.size == 2 &&
                parts[0].toIntOrNull() == day &&
                parts[1].equals(monthName.trim(), ignoreCase = true)
        }
    }
}

enum class EventType {
    WILADAT,
    SHAHADAT
}

data class MasoomDetails(
    val index: Int,
    val name: String,
    val title: String,
    val birthDate: String,
    val martyrdomDate: String,
    val burialPlace: String,
    val bioSummary: String
)

object MasoomeenData {
    /** All Hijri occasions come from the bundled/admin/Firebase JSON through one manager. */
    fun getAllEvents(context: Context): List<MasoomEvent> =
        IslamicEventManager.getFilteredEvents(context)

    fun getNotableDays(context: Context): List<MasoomEvent> = getAllEvents(context)

    fun getEventsForMasoom(context: Context, masoomIndex: Int): List<MasoomEvent> =
        getAllEvents(context)
            .filter { it.masoomIndex == masoomIndex }
            .sortedWith(compareBy<MasoomEvent> { it.hijriMonth }.thenBy { it.hijriDay })

    val list = listOf(
        MasoomDetails(
            index = 1,
            name = "Prophet Muhammad (saws)",
            title = "Al-Mustafa (The Chosen)",
            birthDate = "17 Rabi' al-Awwal",
            martyrdomDate = "28 Safar",
            burialPlace = "Al-Masjid an-Nabawi, Medina",
            bioSummary = "The final Prophet and Messenger of Allah, who established Islam, received the Holy Qur'an, and set the path of guidance for humanity.",
        ),
        MasoomDetails(
            index = 2,
            name = "Imam Ali ibn Abi Talib (as)",
            title = "Amir al-Mu'minin (Commander of the Faithful)",
            birthDate = "13 Rajab",
            martyrdomDate = "21 Ramadan",
            burialPlace = "Imam Ali Shrine, Najaf, Iraq",
            bioSummary = "The first infallible Imam, born inside the Holy Kaabah, the gate of prophetic knowledge, irreplaceable champion of justice, and standard-bearer of Islam.",
        ),
        MasoomDetails(
            index = 3,
            name = "Lady Fatima al-Zahra (sa)",
            title = "Sayyidat Nisa' al-Alamin (Leader of the Women of the Worlds)",
            birthDate = "20 Jumada al-Thani",
            martyrdomDate = "3 Jumada al-Thani",
            burialPlace = "Medina (exact burial place unknown)",
            bioSummary = "The beloved noble daughter of the Prophet, wife of Imam Ali, mother of Imam Hasan and Imam Hussain, representing the perfect purity of Islam.",
        ),
        MasoomDetails(
            index = 4,
            name = "Imam Hasan al-Mujtaba (as)",
            title = "Al-Mujtaba (The Chosen)",
            birthDate = "15 Ramadan",
            martyrdomDate = "28 Safar / 7 Safar",
            burialPlace = "Jannat al-Baqi, Medina",
            bioSummary = "The second infallible Imam, renowned for his infinite generosity, wisdom, patience, and the historic peace treaty which saved the Muslim community.",
        ),
        MasoomDetails(
            index = 5,
            name = "Imam Hussain ibn Ali (as)",
            title = "Sayyid al-Shuhada (Lord of the Martyrs)",
            birthDate = "3 Sha'ban",
            martyrdomDate = "10 Muharram (Ashura)",
            burialPlace = "Imam Hussain Shrine, Karbala, Iraq",
            bioSummary = "The third infallible Imam, who made the supreme sacrifice in Karbala alongside his family and loyal companions to save truth and justice for eternity.",
        ),
        MasoomDetails(
            index = 6,
            name = "Imam Ali ibn al-Hussain (as)",
            title = "Zayn al-Abidin / Al-Sajjad (Ornament of the Worshippers)",
            birthDate = "5 Sha'ban",
            martyrdomDate = "25 Muharram",
            burialPlace = "Jannat al-Baqi, Medina",
            bioSummary = "The fourth infallible Imam, standard-bearer of Karbala's message through sermons, and author of Al-Sahifa al-Sajjadiyya (the Psalms of Islam).",
        ),
        MasoomDetails(
            index = 7,
            name = "Imam Muhammad ibn Ali (as)",
            title = "Al-Baqir (Splitter / Revealer of Knowledge)",
            birthDate = "1 Rajab",
            martyrdomDate = "7 Dhu al-Hijjah",
            burialPlace = "Jannat al-Baqi, Medina",
            bioSummary = "The fifth infallible Imam, who opened the floodgates of Ahlul Bayt scientific, jurisprudential, and philosophical teachings in Islamic academia.",
        ),
        MasoomDetails(
            index = 8,
            name = "Imam Ja'far ibn Muhammad (as)",
            title = "Al-Sadiq (The Truthful)",
            birthDate = "17 Rabi' al-Awwal",
            martyrdomDate = "25 Shawwal",
            burialPlace = "Jannat al-Baqi, Medina",
            bioSummary = "The sixth infallible Imam, founder of the Ja'fari school of jurisprudence, who trained over 4,000 scholars including Jabir ibn Hayyan and Abu Hanifa.",
        ),
        MasoomDetails(
            index = 9,
            name = "Imam Musa ibn Ja'far (as)",
            title = "Al-Kadhim (The Restrainer of Anger)",
            birthDate = "7 Safar",
            martyrdomDate = "25 Rajab",
            burialPlace = "Al-Kadhimiya Mosque, Baghdad",
            bioSummary = "The seventh infallible Imam, famous for his extreme patience, enduring long imprisonments in Abbassid dungeons but guiding thousands to truth.",
        ),
        MasoomDetails(
            index = 10,
            name = "Imam Ali ibn Musa (as)",
            title = "Al-Ridha (The Pleasing / Appointed One)",
            birthDate = "11 Dhu al-Qa'dah",
            martyrdomDate = "17 Safar / 30 Safar",
            burialPlace = "Imam Reza Shrine, Mashhad, Iran",
            bioSummary = "The eighth infallible Imam, famous for his debates with scholars of other religions, showcasing the supremacy of Islamic intellect.",
        ),
        MasoomDetails(
            index = 11,
            name = "Imam Muhammad ibn Ali (as)",
            title = "Al-Jawad / Al-Taqi (The Generous / God-Conscious)",
            birthDate = "10 Rajab",
            martyrdomDate = "30 Dhu al-Qa'dah",
            burialPlace = "Al-Kadhimiya Mosque, Baghdad",
            bioSummary = "The ninth infallible Imam, the youngest of all Imams (assumed Imamate at age 7), whose debates astounded veteran contemporary scholars.",
        ),
        MasoomDetails(
            index = 12,
            name = "Imam Ali ibn Muhammad (as)",
            title = "Al-Hadi / Al-Naqi (The Guide / The Pure)",
            birthDate = "15 Dhu al-Hijjah",
            martyrdomDate = "3 Rajab",
            burialPlace = "Al-Askari Shrine, Samarra, Iraq",
            bioSummary = "The tenth infallible Imam, who taught Ziyarat al-Jamiah al-Kabirah and guided the Shia through a network of deputies (Wikalah) under fortress confinement.",
        ),
        MasoomDetails(
            index = 13,
            name = "Imam Hasan ibn Ali (as)",
            title = "Al-Askari (of the garrison at Samarra) / Al-Zaki",
            birthDate = "8 Rabi' al-Thani",
            martyrdomDate = "8 Rabi' al-Awwal",
            burialPlace = "Al-Askari Shrine, Samarra, Iraq",
            bioSummary = "The eleventh infallible Imam, father of the Savior, spent his entire life in military surveillance but maintained connections with the global Shia community.",
        ),
        MasoomDetails(
            index = 14,
            name = "Imam Muhammad ibn al-Hasan (ajtf)",
            title = "Al-Mahdi / Al-Qa'im (The Guided / The Risen)",
            birthDate = "15 Sha'ban",
            martyrdomDate = "Alive (In Occultation)",
            burialPlace = "N/A (Active Guidance)",
            bioSummary = "The twelfth and current Infallible Imam, living under divine occultation, who will reappear with Jesus Christ to fill the earth with equity, peace, and absolute justice.",
        )
    )

    // Curated list of English/Gregorian international events
    val gregorianEvents = listOf(
        GregorianEvent("New Year's Day", 1, 1, "The first day of the year in the Gregorian calendar, celebrated globally.", titleUrdu = "نئے سال کا دن", titleArabic = "رأس السنة الميلادية", descriptionUrdu = "عیسوی کیلنڈر کے سال کا پہلا دن، جو دنیا بھر میں منایا جاتا ہے۔", descriptionArabic = "اليوم الأول من السنة الميلادية، ويحتفل به في أنحاء العالم."),
        GregorianEvent("International Women's Day", 8, 3, "A global day celebrating the social, economic, cultural, and political achievements of women.", titleUrdu = "بین الاقوامی یوم خواتین", titleArabic = "اليوم الدولي للمرأة", descriptionUrdu = "خواتین کی سماجی، معاشی، ثقافتی اور سیاسی کامیابیوں کو سراہنے کا عالمی دن۔", descriptionArabic = "يوم عالمي للاحتفاء بإنجازات النساء الاجتماعية والاقتصادية والثقافية والسياسية."),
        GregorianEvent("World Health Day", 7, 4, "Sponsored by the World Health Organization (WHO) to raise global health awareness.", titleUrdu = "عالمی یوم صحت", titleArabic = "اليوم العالمي للصحة", descriptionUrdu = "عالمی ادارہ صحت (WHO) کی جانب سے صحت کے عالمی شعور کے لیے منایا جانے والا دن۔", descriptionArabic = "يوم ترعاه منظمة الصحة العالمية لنشر الوعي الصحي العالمي."),
        GregorianEvent("Earth Day", 22, 4, "An annual event to demonstrate support for environmental protection.", titleUrdu = "یوم زمین", titleArabic = "يوم الأرض", descriptionUrdu = "ماحول کے تحفظ کے لیے حمایت ظاہر کرنے کا سالانہ دن۔", descriptionArabic = "مناسبة سنوية لإظهار دعم حماية البيئة."),
        GregorianEvent("International Mother's Day", 10, 5, "Honoring mothers and celebrating motherhood, maternal bonds, and the influence of mothers in society.", titleUrdu = "بین الاقوامی یوم والدہ", titleArabic = "يوم الأم الدولي", descriptionUrdu = "ماؤں کی قدر اور مامتا، والدہ کے رشتوں اور معاشرے میں ان کے کردار کو سراہنے کا دن۔", descriptionArabic = "يوم لتكريم الأمهات والاحتفاء بالأمومة وروابطها ودور الأمهات في المجتمع."),
        GregorianEvent("World Environment Day", 5, 6, "UN day for encouraging worldwide awareness and action for the protection of our environment.", titleUrdu = "عالمی یوم ماحول", titleArabic = "اليوم العالمي للبيئة", descriptionUrdu = "اقوام متحدہ کا دن، جو ماحول کے تحفظ کے لیے عالمی شعور اور عمل کو فروغ دیتا ہے۔", descriptionArabic = "يوم تابع للأمم المتحدة لتعزيز الوعي والعمل العالميين لحماية البيئة."),
        GregorianEvent("International Father's Day", 21, 6, "Honoring fathers and celebrating fatherhood, paternal bonds, and the influence of fathers in society.", titleUrdu = "بین الاقوامی یوم والد", titleArabic = "يوم الأب الدولي", descriptionUrdu = "والد کی قدر، والدیت اور معاشرے میں ان کے کردار کو سراہنے کا دن۔", descriptionArabic = "يوم لتكريم الآباء والاحتفاء بالأبوة وروابطها ودور الآباء في المجتمع."),
        GregorianEvent("World Blood Donor Day", 14, 6, "Raising awareness of the need for safe blood and thanking voluntary blood donors.", titleUrdu = "عالمی یوم عطیہ خون", titleArabic = "اليوم العالمي للمتبرعين بالدم", descriptionUrdu = "محفوظ خون کی ضرورت کے بارے میں شعور اور رضاکارانہ عطیہ دہندگان کا شکریہ ادا کرنے کا دن۔", descriptionArabic = "يوم لنشر الوعي بالحاجة إلى الدم الآمن وشكر المتبرعين الطوعيين."),
        GregorianEvent("World Parents' Day", 1, 6, "A United Nations day to appreciate all parents for their selfless commitment to children.", titleUrdu = "عالمی یوم والدین", titleArabic = "اليوم العالمي للوالدين", descriptionUrdu = "اقوام متحدہ کا دن، جو بچوں کے لیے والدین کی بے لوث خدمت کی قدر کرتا ہے۔", descriptionArabic = "يوم تابع للأمم المتحدة لتقدير التزام جميع الوالدين بأطفالهم."),
        GregorianEvent("International Day of Peace", 21, 9, "A United Nations-sanctioned holiday dedicated to world peace and non-violence.", titleUrdu = "بین الاقوامی یوم امن", titleArabic = "اليوم الدولي للسلام", descriptionUrdu = "اقوام متحدہ کا منظور شدہ دن، جو عالمی امن اور عدم تشدد کے لیے وقف ہے۔", descriptionArabic = "يوم معتمد من الأمم المتحدة مخصص للسلام العالمي ونبذ العنف."),
        GregorianEvent("World Teachers' Day", 5, 10, "Celebrating teachers worldwide and their crucial role in education and development.", titleUrdu = "عالمی یوم اساتذہ", titleArabic = "اليوم العالمي للمعلمين", descriptionUrdu = "دنیا بھر کے اساتذہ اور تعلیم و ترقی میں ان کے اہم کردار کو سراہنے کا دن۔", descriptionArabic = "يوم للاحتفاء بالمعلمين في أنحاء العالم ودورهم المهم في التعليم والتنمية."),
        GregorianEvent("World Food Day", 16, 10, "Commemorating the founding of the Food and Agriculture Organization (FAO) to tackle global hunger.", titleUrdu = "عالمی یوم خوراک", titleArabic = "اليوم العالمي للأغذية", descriptionUrdu = "خوراک و زراعت کی تنظیم کے قیام کی یادگار اور عالمی بھوک کے خاتمے کے لیے کام کرنے کا دن۔", descriptionArabic = "يوم لإحياء ذكرى تأسيس منظمة الأغذية والزراعة والعمل على القضاء على الجوع العالمي."),
        GregorianEvent("World Children's Day", 20, 11, "Promoting international togetherness, awareness among children worldwide, and improving children's welfare.", titleUrdu = "عالمی یوم اطفال", titleArabic = "اليوم العالمي للطفل", descriptionUrdu = "بچوں میں بین الاقوامی یکجہتی، شعور اور ان کی بہبود کو فروغ دینے کا دن۔", descriptionArabic = "يوم لتعزيز التكاتف الدولي والوعي بين الأطفال وتحسين رفاههم."),
        GregorianEvent("Human Rights Day", 10, 12, "Honoring the United Nations General Assembly's adoption of the Universal Declaration of Human Rights.", titleUrdu = "یوم انسانی حقوق", titleArabic = "يوم حقوق الإنسان", descriptionUrdu = "اقوام متحدہ کی جنرل اسمبلی کی جانب سے عالمی منشور انسانی حقوق کے اعلان کی یادگار کا دن۔", descriptionArabic = "يوم لإحياء ذكرى اعتماد الجمعية العامة للأمم المتحدة الإعلان العالمي لحقوق الإنسان."),
        GregorianEvent("Christmas Day", 25, 12, "An annual festival commemorating the birth of Jesus Christ, observed globally.", titleUrdu = "کرسمس ڈے", titleArabic = "عيد الميلاد", descriptionUrdu = "ییسوع مسیح کی ولادت کی یاد میں منایا جانے والا سالانہ تیوار، جسے دنیا بھر میں منایا جاتا ہے۔", descriptionArabic = "مهرجان سنوي لإحياء ذكرى مولد يسوع المسيح، ويحتفل به في أنحاء العالم.")
    )
}

data class GregorianEvent(
    val title: String,
    val day: Int,
    val month: Int, // 1-indexed (1 = Jan, ..., 12 = Dec)
    val description: String,
    val titleUrdu: String = "",
    val titleArabic: String = "",
    val descriptionUrdu: String = "",
    val descriptionArabic: String = ""
) {
    fun localizedTitle(languageCode: String): String = when (languageCode) {
        "ur" -> titleUrdu.ifBlank { title }
        "ar" -> titleArabic.ifBlank { title }
        else -> title
    }

    fun localizedDescription(languageCode: String): String = when (languageCode) {
        "ur" -> descriptionUrdu.ifBlank { description }
        "ar" -> descriptionArabic.ifBlank { description }
        else -> description
    }
}
