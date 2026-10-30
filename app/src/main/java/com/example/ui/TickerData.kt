package com.example.ui

import java.util.Calendar

enum class TickerCategory {
    HISTORY,
    RELIGIOUS,
    COMMUNITY,
    EDUCATIONAL,
    APP,
    ALERT;

    fun getLabel(lang: String): String {
        return when (this) {
            HISTORY -> when (lang) {
                "ur" -> "تاریخی مناسبتیں"
                "ar" -> "المناسبات التاريخية"
                "fa" -> "مناسبت‌های تاریخی"
                "hi" -> "ऐतिहासिक घटनाएँ"
                else -> "Historical Events"
            }
            RELIGIOUS -> when (lang) {
                "ur" -> "دینی اعلانات"
                "ar" -> "الإعلانات الدينية"
                "fa" -> "اطلاعیه‌های دینی"
                "hi" -> "धार्मिक घोषणाएँ"
                else -> "Religious Notices"
            }
            COMMUNITY -> when (lang) {
                "ur" -> "علاقائی پروگرام"
                "ar" -> "النشاطات المجتمعية"
                "fa" -> "فعالیت‌های محلی"
                "hi" -> "सामुदायिक कार्यक्रम"
                else -> "Community Events"
            }
            EDUCATIONAL -> when (lang) {
                "ur" -> "بابرکت احادیث"
                "ar" -> "الأحاديث الشريفة"
                "fa" -> "احادیث مبارکه"
                "hi" -> "शैक्षिक हदीस"
                else -> "Hadith & Lessons"
            }
            APP -> when (lang) {
                "ur" -> "ایپ اپڈیٹس"
                "ar" -> "تحديثات التطبيق"
                "fa" -> "بروزرسانی‌های اپ"
                "hi" -> "एप अपडेट"
                else -> "App Updates"
            }
            ALERT -> when (lang) {
                "ur" -> "ہنگامی الرٹ"
                "ar" -> "تنبيهات عاجلة"
                "fa" -> "هشدارهای اضطراری"
                "hi" -> "आपातकालीन सूचना"
                else -> "Urgent Alert"
            }
        }
    }
}

data class TickerItem(
    val id: String,
    val text: String,
    val category: TickerCategory,
    val fullTitle: String = "",
    val fullDetails: String = "",
    val imageUrl: String = "",
    val source: String = "Internal",
    val dateString: String = "",
    val bulletPoints: List<String> = emptyList(),
    val isEmergency: Boolean = false,
    val address: String = "",
    val contact: String = "",
    val isNew: Boolean = true
)

object TickerLocalData {
    fun getLocalEvents(lang: String): List<TickerItem> {
        val list = mutableListOf<TickerItem>()

        // Historical events local
        list.add(
            TickerItem(
                id = "hist_ghadeer",
                text = when (lang) {
                    "ur" -> "📅 عید الغدیر: ۱۸ ذوالحجہ۔ تاجپوشیِ مولا امیر المومنین (ع)۔"
                    "ar" -> "📅 عيد الغدير الأغر: ١٨ ذو الحجة۔ تنصيب الإمام علي (ع) أميراً للمؤمنين۔"
                    "fa" -> "📅 عید سعید غدیر خم: ۱۸ ذی‌الحجه۔ ولایت امیرالمؤمنین (ع)۔"
                    else -> "📅 Eid al-Ghadeer: 18th Dhul-Hijjah. Declaration of Imam Ali (as) Mastership."
                },
                category = TickerCategory.HISTORY,
                fullTitle = when (lang) {
                    "ur" -> "عید الغدیر: الٰہی اعلانِ ولایت"
                    "ar" -> "عيد الغدير الأغر: إعلان الولاية الإلهية"
                    else -> "Eid al-Ghadeer: Divine Declaration of Wilayah"
                },
                fullDetails = when (lang) {
                    "ur" -> "پیغمبر اکرم (ص) نے حجۃ الوداع سے واپسی پر غدیر خم کے مقام پر حضرت علی علیہ السلام کی ولایت و امامت کا باقاعدہ اعلان فرمایا۔ تمام مسلمانوں کو مبارک باد!"
                    "ar" -> "أعلن رسول الله (ص) ولاية وإمامة أمير المؤمنين علي بن أبي طالب (ع) في غدير خم أثناء عودته من حجة الوداع بأمر الله تعالى۔"
                    else -> "Historically occurred on the 18th of Dhul Hijjah 10 AH, where Prophet Muhammad (saws) officially declared Imam Ali (as) as his successor and Master of all believers after him, at Ghadir Khum."
                },
                source = "Mafatih al-Jinan & Bihaar al-Anwar",
                bulletPoints = listOf(
                    "Date: 18th Dhul-Hijjah 10 AH",
                    "Location: Ghadir Khum, near Miqat al-Juhfah",
                    "Declaration: 'Of whomsoever I am Master, this Ali is his Master'",
                    "Quranic Revelation: Surah Al-Ma'idah Verses 3 & 67"
                )
            )
        )

        list.add(
            TickerItem(
                id = "hist_ali_martyrdom",
                text = when (lang) {
                    "ur" -> "🕌 شہادت امام علی علیہ السلام: ۲۱ رمضان المبارک، محرابِ کوفہ۔"
                    "ar" -> "🕌 شهادة أمير المؤمنين الإمام علي (ع): ٢١ رمضان المبارك، محراب الكوفة۔"
                    "fa" -> "🕌 شهادت حضرت علی (ع): ۲۱ رمضان المبارک در محراب مسجد کوفه۔"
                    else -> "🕌 Martyrdom of Imam Ali (as): 21st Ramadan, Mihrab of Kufa."
                },
                category = TickerCategory.HISTORY,
                fullTitle = when (lang) {
                    "ur" -> "شہادتِ مولائے کائنات امام علی علیہ السلام"
                    "ar" -> "شهادة يعسوب الدين الإمام علي (ع)"
                    else -> "Martyrdom of Commander of Faithful Imam Ali (as)"
                },
                fullDetails = when (lang) {
                    "ur" -> "۱۹ رمضان کی صبح ابن ملجم لعین نے سجدہ کی حالت میں مولا علی (ع) کے سر مبارک پر وار کیا، جس کے بعد آپ ۲۱ رمضان کو درجۂ شہادت پر فائز ہوئے۔"
                    "ar" -> "في محراب مسجد الكوفة المعظم، ضُرب الإمام علي (ع) بسيف ابن ملجم المرادي المسموم في ليلة ١٩ رمضان المبارك واستشهد ليلة ٢١ من الشهر۔"
                    else -> "Imam Ali (as) was struck on the head with a poisoned sword by Ibn Muljam during the morning Fajr prayers in the Great Mosque of Kufa on the 19th of Ramadan and attained martyrdom on the 21st."
                },
                source = "Nahjul Balagha & Tarikh al-Tabari",
                bulletPoints = listOf(
                    "Date: 21st Ramadan 40 AH",
                    "Location: Masood Kufa Mosque, Iraq",
                    "Burial Site: Najaf al-Ashraf, Iraq",
                    "Last Words: 'Fuztu wa Rabbil Ka'bah' (By the Lord of the Ka'bah, I have succeeded)"
                )
            )
        )

        // Educational reminders
        list.add(
            TickerItem(
                id = "edu_hadith_knowledge",
                text = when (lang) {
                    "ur" -> "✨ علم مومن کی گمشدہ میراث ہے، جہاں سے ملے حاصل کرے: امام علی (ع)۔"
                    "ar" -> "✨ العلم ضالة المؤمن، فخذوه ولو من أيدي المشركين: الإمام علي (ع)۔"
                    "fa" -> "✨ حکمت گمشده مومن است، آن را فراگیرید حتی از منافقان: امام علی (ع)۔"
                    else -> "✨ 'Knowledge is the lost property of the believer, seek it even from polytheists': Imam Ali (as)."
                },
                category = TickerCategory.EDUCATIONAL,
                fullTitle = when (lang) {
                    "ur" -> "تحصیلِ علم کی اہمیت"
                    "ar" -> "أهمية العلم والتحصيل"
                    else -> "Value of Acquiring Knowledge"
                },
                fullDetails = when (lang) {
                    "ur" -> "امیر المومنین علی علیہ السلام فرماتے ہیں کہ حکمت اور علم مومن کی گمشدہ میراث ہے، اسے حاصل کرو خواہ تمہیں باطل سے ہی کیوں نہ لینا پڑے۔"
                    "ar" -> "من وصايا أمير المؤمنين (ع) في فضل طلب العلم وشرف أصحابه، وأن العلم يحمي صاحبه بينما المال يحتاج إلى رعاية۔"
                    else -> "A golden instruction from Imam Ali (as) emphasizing that a Shia must constantly seek intellectual, theological, and scientific growth because wisdom and knowledge belong inherently to the faithful."
                },
                source = "Ghurar al-Hikam, Vol 1, p. 110",
                bulletPoints = listOf(
                    "Speaker: Imam Ali ibn Abi Talib (as)",
                    "Focus: Lifetime intellectual pursuits and Shia theology",
                    "Quote Code: Ghurar-1029",
                    "Actionable reminder: Dedicate 15 minutes today to read a reliable Islamic book"
                )
            )
        )

        list.add(
            TickerItem(
                id = "edu_hadith_prayer",
                text = when (lang) {
                    "ur" -> "✨ نماز دین کا ستون ہے، اگر قبول ہوئی تو سب قبول: امام جعفر صادق (ع)۔"
                    "ar" -> "✨ الصلاة عمود الدين، إن قبلت قبل ما سواها وإن ردت رد ما سواها۔"
                    "fa" -> "✨ نماز ستون دین است، اگر قبول شود سایر اعمال پذیرفته می‌شود: امام صادق (ع)۔"
                    else -> "✨ 'Prayer is the pillar of religion; if accepted, all else is accepted': Imam Al-Sadiq (as)."
                },
                category = TickerCategory.EDUCATIONAL,
                fullTitle = when (lang) {
                    "ur" -> "نماز: قرب الٰہی کا ذریعہ"
                    "ar" -> "الصلاة: عمود الدين ومعراجه"
                    else -> "Salat: The Pillar of Faith"
                },
                fullDetails = when (lang) {
                    "ur" -> "امام جعفر صادق علیہ السلام نے اپنی آخری وصیت میں تاکید فرمائی کہ ہماری شفاعت اس شخص تک نہیں پہنچ سکتی جو اپنی نمازوں کو ہلکا اور غیر اہم سمجھے۔"
                    "ar" -> "عن الإمام الصادق (ع) إن شفاعتنا لا تنال مستخفاً بالصلاة۔ أهمية أداء الصلوات المكتوبة في أوقات فضيلتها بنوافلها۔"
                    else -> "Under Ja'fari jurisprudence, preserving and performing the five obligatory daily prayers at their premium priority فضيلة times is the foundational bridge of a Shia's relationship with Allah."
                },
                source = "Al-Kafi, Vol 3, Chapter of Salat",
                bulletPoints = listOf(
                    "Teacher of Fiqh: Imam Ja'far al-Sadiq (as)",
                    "Severe Warning: Light treatment of Salat excludes one from Ahlul Bayt's special intercession",
                    "Practical Advice: Use our Prayer Times dynamic notification to pray on time"
                )
            )
        )

        // Community Alert or religious notices local fallback
        list.add(
            TickerItem(
                id = "rel_moon_notice",
                text = when (lang) {
                    "ur" -> "📢 چاند نظر آنے کا شرعی اعلان اور پہلی شرعی تاریخ کی ہلال مہم۔"
                    "ar" -> "📢 إعلان الهلال الشرعي ومتابعة رصد الشهور الهجرية المباركة۔"
                    "fa" -> "📢 اطلاعیه رصد هلال ماه نو و آغاز ماه‌های قمری۔"
                    else -> "📢 Crescent Moon Sighting Notice: Follow official announcements of the Mujtahideen."
                },
                category = TickerCategory.RELIGIOUS,
                fullTitle = when (lang) {
                    "ur" -> "ہلالِ نو کی رویت اور شرعی ثبوت"
                    "ar" -> "طرق ثبوت هلال الشهر الجديد"
                    else -> "Crescent Moon Verification in Shia Jurisprudence"
                },
                fullDetails = when (lang) {
                    "ur" -> "شرعی طور پر نیا قمری مہینہ ثابت ہونے کے لیے عادل گواہوں کی رویت یا مراجع عظام کا باضابطہ تصدیق شدہ اعلان ضروری ہے۔"
                    "ar" -> "ثبوت الهلال طبقاً لفتاوى المراجع العظام يتطلب الرؤية بالعين المجردة أو المسلحة حسب مباني التقليد الفقهية۔"
                    else -> "In Shia Ja'fari Fiqh, a new Islamic month is proven by sighting the crescent moon with the naked eye (or optical aid based on your Marja's verdict), the testimony of two just witnesses, or a formal decree by the supreme religious authority."
                },
                source = "Minhaj al-Salihin (Seyyid Sistani / Seyyid Khamenei)",
                bulletPoints = listOf(
                    "Marja Ruling: Optical aids vs. Naked eye requirements",
                    "Rule: Sighting in Eastern hemisphere usually suffices for Western horizons if shared path is found",
                    "Sighting: Check our Moon phase countdown panel on the dashboard for estimated dates"
                )
            )
        )

        list.add(
            TickerItem(
                id = "app_new_features",
                text = when (lang) {
                    "ur" -> "📱 نیا فیچر: اعمالِ روزانہ ٹریکر اب مکمل طور پر لائیو ہے!"
                    "ar" -> "📱 ميزة جديدة: متابع الأعمال اليومية مفعل الآن مع سجلات التزام۔"
                    "fa" -> "📱 ویژگی جدید: بخش اعمال روزانه را اکنون امتحان کنید۔"
                    else -> "📱 New Feature: Daily Worship Tracker with streaks is now fully live!"
                },
                category = TickerCategory.APP,
                fullTitle = "Aamal-e-Rozana Integration",
                fullDetails = "We have added a custom local database tracker in this update. Keep track of daily prayers, nawafil, key supplications, and customized spiritual tasks to cultivate dynamic spiritual mindfulness and see your streaks glow!",
                source = "Shia Pulse Release Notes v3.4.2",
                bulletPoints = listOf(
                    "Feature: Aamal-e-Rozana screen",
                    "Feature: Custom user activities and reflections",
                    "Feature: Automatic streak calculators",
                    "UI: Modern light/dark adaptive layout"
                )
            )
        )

        return list
    }
}
