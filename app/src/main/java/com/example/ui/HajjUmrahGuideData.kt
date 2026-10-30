package com.example.ui

/** Localized guide content kept independent of Compose so its references can be unit tested. */
data class LocalizedGuideText(
    val en: String,
    val ur: String,
    val ar: String
) {
    fun get(languageCode: String): String = when (languageCode) {
        "ur" -> ur
        "ar" -> ar
        else -> en
    }
}

enum class GuideRequirement {
    OBLIGATORY,
    RECOMMENDED,
    CONDITIONAL
}

enum class GuideReferenceType {
    QURAN,
    HADITH,
    IMAM,
    FIQH
}

data class GuideReference(
    val type: GuideReferenceType,
    val citation: LocalizedGuideText,
    val summary: LocalizedGuideText,
    val url: String,
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null
)

data class GuideDua(
    val title: LocalizedGuideText,
    val body: LocalizedGuideText,
    val citation: LocalizedGuideText,
    val url: String,
    val arabic: String? = null
)

data class GuideStep(
    /** Stable Room checklist key. Existing IDs 1-4 (Umrah) and 1-6 (Hajj) retain their meanings. */
    val id: Int,
    val title: LocalizedGuideText,
    val description: LocalizedGuideText,
    val requirement: GuideRequirement = GuideRequirement.OBLIGATORY,
    val note: LocalizedGuideText? = null,
    val references: List<GuideReference>,
    val duas: List<GuideDua> = emptyList()
)

private fun tr(en: String, ur: String, ar: String) = LocalizedGuideText(en, ur, ar)

private fun quran(
    verse: String,
    summary: LocalizedGuideText
): GuideReference {
    val surahNumber = verse.substringBefore(":").toInt()
    val ayahNumber = verse.substringAfter(":").takeWhile { it.isDigit() }.toInt()
    return GuideReference(
        type = GuideReferenceType.QURAN,
        citation = tr("Qur’an $verse", "قرآن $verse", "القرآن $verse"),
        summary = summary,
        url = "",
        surahNumber = surahNumber,
        ayahNumber = ayahNumber
    )
}

private fun fiqh(
    citation: LocalizedGuideText,
    summary: LocalizedGuideText,
    url: String
) = GuideReference(
    type = GuideReferenceType.FIQH,
    citation = citation,
    summary = summary,
    url = url
)

private const val SISTANI_TAMATTU = "https://www.sistani.org/english/book/47/2089/"
private const val SISTANI_UMRAH_TYPES = "https://www.sistani.org/english/book/47/2086/"
private const val SISTANI_TAWAF_RULES = "https://www.sistani.org/english/book/47/2097/"
private const val SISTANI_TAWAF_PRAYER = "https://www.sistani.org/english/book/47/2100/"
private const val SISTANI_SAI = "https://www.sistani.org/english/book/47/2101/"
private const val SISTANI_TAQSIR = "https://www.sistani.org/english/book/47/2103/"
private const val SISTANI_HAJJ_IHRAM = "https://www.sistani.org/english/book/47/2104/"
private const val SISTANI_MUZDALIFAH = "https://www.sistani.org/english/book/47/2106/"
private const val SISTANI_HALQ = "https://www.sistani.org/english/book/47/2109/"
private const val SISTANI_TAWAF_NISA = "https://www.sistani.org/english/book/47/2110/"
private const val SISTANI_MINA_STAY = "https://www.sistani.org/english/book/47/2111/"
private const val SISTANI_RAMI = "https://www.sistani.org/english/book/47/2112/"
private const val SISTANI_RECOMMENDED = "https://www.sistani.org/english/book/47/2114/"
private const val DUA_ARAFAH = "https://www.duas.org/dua-arafah-imam-husain.html"
private const val ARAFAH_AMAAL = "https://www.duas.org/zilhajj/arafa.htm"

private val recommendedCitation = tr(
    "Ayatullah Sistani — Recommended acts & supplications",
    "آیت اللہ سیستانی — مستحب اعمال و دعائیں",
    "السيستاني — الأعمال والأدعية المستحبة"
)

private val talbiyahArabic =
    "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ"

private val talbiyahDua = GuideDua(
    title = tr("Talbiyah", "تلبیہ", "التلبية"),
    arabic = talbiyahArabic,
    body = tr(
        "Here I am, O Allah, here I am. Here I am, You have no partner, here I am. All praise, favour and sovereignty belong to You; You have no partner.",
        "میں حاضر ہوں اے اللہ! میں حاضر ہوں۔ میں حاضر ہوں، تیرا کوئی شریک نہیں، میں حاضر ہوں۔ تمام تعریف، نعمت اور بادشاہی تیرے ہی لیے ہے؛ تیرا کوئی شریک نہیں۔",
        "يستحب عقد الإحرام بالتلبية وتكرارها وفق آداب المناسك."
    ),
    citation = recommendedCitation,
    url = SISTANI_RECOMMENDED
)

private val tawafDua = GuideDua(
    title = tr(
        "Between Rukn al-Yamani and the Black Stone",
        "رکن یمانی اور حجر اسود کے درمیان",
        "بين الركن اليماني والحجر الأسود"
    ),
    arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
    body = tr(
        "Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire. — Qur’an 2:201",
        "اے ہمارے رب! ہمیں دنیا میں بھلائی اور آخرت میں بھلائی عطا فرما، اور ہمیں آگ کے عذاب سے بچا۔ — قرآن 2:201",
        "دعاء قرآني من سورة البقرة، الآية 201."
    ),
    citation = tr(
        "Qur’an 2:201; placement in Sistani’s recommended Tawaf acts",
        "قرآن 2:201؛ سیستانی کے مستحب اعمالِ طواف میں مقام",
        "القرآن 2:201؛ وموضعه في الأعمال المستحبة للطواف عند السيستاني"
    ),
    url = SISTANI_RECOMMENDED
)

private val ramiDua = GuideDua(
    title = tr("With every pebble", "ہر کنکری کے ساتھ", "مع كل حصاة"),
    arabic = "اللَّهُ أَكْبَرُ",
    body = tr(
        "Allah is the Greatest. The cited manual also recommends asking Allah to accept the Hajj and forgive one’s sins.",
        "اللہ سب سے بڑا ہے۔ مذکورہ رہنما میں حج کی قبولیت اور مغفرت کی دعا بھی مستحب بتائی گئی ہے۔",
        "الله أكبر، ويستحب أيضاً سؤال الله قبول الحج ومغفرة الذنوب كما في الدليل المذكور."
    ),
    citation = recommendedCitation,
    url = SISTANI_RECOMMENDED
)

private val umrahSteps = listOf(
    GuideStep(
        id = 1,
        title = tr("Miqat & Ihram", "میقات اور احرام", "الميقات والإحرام"),
        description = tr(
            "At your appointed Miqat, make the intention for Umrah and enter Ihram with Talbiyah. Ghusl is recommended; observe every Ihram prohibition from this point.",
            "اپنے مقررہ میقات پر عمرہ کی نیت کریں اور تلبیہ کے ساتھ احرام باندھیں۔ غسل مستحب ہے؛ اس مقام سے احرام کی تمام پابندیوں کا خیال رکھیں۔",
            "عند الميقات المحدد، انوِ العمرة وأحرم بالتلبية. يستحب الغسل، والتزم جميع محرمات الإحرام من هذه النقطة."
        ),
        note = tr(
            "Miqats include Dhu al-Hulayfah, al-Juhfah, Qarn al-Manazil, Yalamlam and Dhat Irq, according to the route. The applicable Miqat and any special case should be confirmed with your Marja.",
            "راستے کے مطابق میقات میں ذوالحلیفہ، جحفہ، قرن المنازل، یلملم اور ذات عرق شامل ہیں۔ اپنا میقات اور کوئی خاص صورت اپنے مرجع سے تصدیق کر کے طے کریں۔",
            "تشمل المواقيت ذا الحليفة والجحفة وقرن المنازل ويلملم وذات عرق بحسب الطريق. أكّد الميقات والحالات الخاصة مع مرجعك."
        ),
        references = listOf(
            quran(
                "2:196",
                tr(
                    "The Qur’an commands completing Hajj and Umrah for Allah; the detailed Miqat and Ihram procedure comes from the cited fiqh.",
                    "قرآن میں اللہ کے لیے حج اور عمرہ مکمل کرنے کا حکم ہے؛ میقات اور احرام کی تفصیلی ترتیب مذکورہ فقہی ماخذ سے ملتی ہے۔",
                    "يأمر القرآن بإتمام الحج والعمرة لله، أما تفاصيل الميقات والإحرام فتؤخذ من المصدر الفقهي المذكور."
                )
            ),
            fiqh(
                tr("Ayatullah Sistani — Umrah al-Tamattu, Rule 148", "آیت اللہ سیستانی — عمرہ تمتع، حکم 148", "السيستاني — عمرة التمتع، الحكم 148"),
                tr(
                    "Ihram from Miqat is the first of the five required acts of Umrah al-Tamattu.",
                    "میقات سے احرام عمرہ تمتع کے پانچ لازمی اعمال میں پہلا ہے۔",
                    "الإحرام من الميقات هو أول الأعمال الخمسة الواجبة في عمرة التمتع."
                ),
                SISTANI_TAMATTU
            )
        ),
        duas = listOf(talbiyahDua)
    ),
    GuideStep(
        id = 2,
        title = tr("Tawaf — seven circuits", "طواف — سات چکر", "الطواف — سبعة أشواط"),
        description = tr(
            "With the intention of nearness to Allah and the required purity, begin at the Black Stone, complete seven continuous circuits with the Kaaba on your left, and go around Hijr Ismail without entering it.",
            "قربتِ الٰہی کی نیت اور مطلوبہ پاکیزگی کے ساتھ حجر اسود سے شروع کریں، کعبہ کو بائیں جانب رکھتے ہوئے مسلسل سات چکر مکمل کریں، اور حطیمِ اسماعیل میں داخل ہوئے بغیر اس کے گرد چکر لگائیں۔",
            "بنية القربة والطهارة المطلوبة، ابدأ من الحجر الأسود وأتم سبعة أشواط متتابعة والكعبة عن يسارك، وطُف حول حجر إسماعيل من دون الدخول فيه."
        ),
        references = listOf(
            quran(
                "22:29",
                tr(
                    "The verse includes circumambulating the Ancient House among the pilgrimage rites.",
                    "آیت میں مناسک کے حصے کے طور پر بیت العتیق کے طواف کا ذکر ہے۔",
                    "تتضمن الآية الطواف بالبيت العتيق ضمن مناسك الحج."
                )
            ),
            fiqh(
                tr("Ayatullah Sistani — Obligations of Tawaf", "آیت اللہ سیستانی — واجباتِ طواف", "السيستاني — واجبات الطواف"),
                tr(
                    "Details the Black Stone start, seven rounds, continuity, the Kaaba on the left, and Hijr Ismail.",
                    "حجر اسود سے آغاز، سات چکر، تسلسل، کعبہ کا بائیں جانب ہونا اور حطیمِ اسماعیل کی تفصیل۔",
                    "يفصل البدء من الحجر الأسود والأشواط السبعة والتوالي والكعبة عن اليسار وحجر إسماعيل."
                ),
                SISTANI_TAWAF_RULES
            )
        ),
        duas = listOf(tawafDua)
    ),
    GuideStep(
        // New step; placed after Tawaf without changing the saved IDs of Sa'i (3) or Taqsir (4).
        id = 5,
        title = tr("Two Rak‘ahs of Tawaf", "نمازِ طواف — دو رکعت", "ركعتا الطواف"),
        description = tr(
            "Immediately after Tawaf, pray two Rak‘ahs near and behind Maqam Ibrahim as able. It is recommended to recite Surah al-Tawhid in the first and Surah al-Kafirun in the second after al-Fatihah.",
            "طواف کے فوراً بعد استطاعت کے مطابق مقام ابراہیم کے قریب اور پیچھے دو رکعت نماز پڑھیں۔ مستحب ہے سورہ فاتحہ کے بعد پہلی رکعت میں سورہ توحید اور دوسری میں سورہ کافرون پڑھیں۔",
            "بعد الطواف مباشرة، صلِّ ركعتين قريباً من مقام إبراهيم وخلفه بحسب الإمكان. يستحب قراءة سورة التوحيد في الأولى وسورة الكافرون في الثانية بعد الفاتحة."
        ),
        note = tr(
            "If the usual place is inaccessible because of crowding or inability, the linked Tawaf prayer rules explain the alternatives.",
            "اگر بھیڑ یا ناتوانی کے سبب معمول کی جگہ میسر نہ ہو تو مربوط نمازِ طواف کے احکام میں متبادل صورتیں بیان ہیں۔",
            "إذا تعذر المكان المعتاد بسبب الزحام أو غيره، فأحكام صلاة الطواف المرفقة تبين البدائل."
        ),
        references = listOf(
            fiqh(
                tr("Ayatullah Sistani — Tawaf prayer", "آیت اللہ سیستانی — نمازِ طواف", "السيستاني — صلاة الطواف"),
                tr(
                    "The two Rak‘ahs are the third required act of Umrah al-Tamattu and normally follow Tawaf immediately.",
                    "دو رکعت نماز عمرہ تمتع کا تیسرا لازمی عمل ہے اور معمول کے مطابق طواف کے فوراً بعد پڑھی جاتی ہے۔",
                    "الركعتان هما العمل الثالث الواجب في عمرة التمتع وتؤديان عادة بعد الطواف مباشرة."
                ),
                SISTANI_TAWAF_PRAYER
            ),
            GuideReference(
                type = GuideReferenceType.HADITH,
                citation = tr("Imam al-Sadiq (as) — reported post-prayer supplication", "امام صادقؑ — نماز کے بعد کی منقول دعا", "الإمام الصادق (ع) — الدعاء المنقول بعد الصلاة"),
                summary = tr(
                    "The manual reports a supplication of confession and seeking forgiveness in sajdah after the Tawaf prayer.",
                    "رہنما میں امام صادقؑ سے نمازِ طواف کے بعد سجدے میں گناہوں کا اعتراف اور مغفرت کی دعا منقول ہے۔",
                    "ينقل الدليل عن الإمام الصادق (ع) دعاء في السجود بعد صلاة الطواف يتضمن الاعتراف بالذنوب وطلب المغفرة."
                ),
                url = SISTANI_RECOMMENDED
            )
        ),
        duas = listOf(
            GuideDua(
                title = tr("After the prayer", "نماز کے بعد", "بعد الصلاة"),
                body = tr(
                    "Praise Allah, send blessings on Muhammad and his Progeny, and ask for your worship to be accepted; then use the linked supplication reported from Imam al-Sadiq (as).",
                    "اللہ کی حمد کریں، محمد و آلِ محمد پر درود بھیجیں، عبادت کی قبولیت مانگیں، پھر امام صادقؑ سے منقول دعا کے لیے ماخذ کھولیں۔",
                    "احمد الله وصلِّ على محمد وآله واسأل قبول العمل، ثم استخدم الدعاء المنقول عن الإمام الصادق (ع) في المصدر."
                ),
                citation = recommendedCitation,
                url = SISTANI_RECOMMENDED
            ),
            GuideDua(
                title = tr("Before Sa‘i — Zamzam", "سعی سے قبل — آبِ زمزم", "قبل السعي — ماء زمزم"),
                body = tr(
                    "It is recommended to drink Zamzam and ask Allah for beneficial knowledge, abundant sustenance and relief from illness.",
                    "آبِ زمزم پینا اور اللہ سے علمِ نافع، کشادہ رزق اور بیماری سے شفا مانگنا مستحب ہے۔",
                    "يستحب شرب ماء زمزم وسؤال الله العلم النافع والرزق الواسع والشفاء من الأمراض."
                ),
                citation = recommendedCitation,
                url = SISTANI_RECOMMENDED
            )
        )
    ),
    GuideStep(
        id = 3,
        title = tr("Sa‘i — Safa to Marwah", "سعی — صفا تا مروہ", "السعي — من الصفا إلى المروة"),
        description = tr(
            "After Tawaf and its prayer, complete seven journeys beginning at Safa and ending at Marwah. Safa to Marwah is one journey; Marwah to Safa is the next.",
            "طواف اور اس کی نماز کے بعد صفا سے شروع اور مروہ پر ختم ہوتے ہوئے سات چکر مکمل کریں۔ صفا سے مروہ ایک چکر، مروہ سے صفا اگلا چکر ہے۔",
            "بعد الطواف وصلاته، أكمل سبعة أشواط تبدأ من الصفا وتنتهي بالمروة. من الصفا إلى المروة شوط، ومن المروة إلى الصفا شوط آخر."
        ),
        references = listOf(
            quran(
                "2:158",
                tr(
                    "Safa and Marwah are described as being among the symbols and rites of Allah.",
                    "صفا اور مروہ کو اللہ کی نشانیوں اور مناسک میں شمار کیا گیا ہے۔",
                    "تصف الآية الصفا والمروة بأنهما من شعائر الله."
                )
            ),
            GuideReference(
                type = GuideReferenceType.IMAM,
                citation = tr("Imam Ali (as) — reported Safa supplication", "امام علیؑ — صفا پر منقول دعا", "الإمام علي (ع) — الدعاء المنقول على الصفا"),
                summary = tr(
                    "The recommended-acts manual reports Imam Ali’s supplication for forgiveness and mercy upon ascending Safa.",
                    "مستحب اعمال کے ماخذ میں صفا پر چڑھنے کے وقت امام علیؑ سے مغفرت اور رحمت کی دعا منقول ہے۔",
                    "ينقل دليل المستحبات عن الإمام علي (ع) دعاء المغفرة والرحمة عند الصعود على الصفا."
                ),
                url = SISTANI_RECOMMENDED
            ),
            fiqh(
                tr("Ayatullah Sistani — Obligations of Sa‘i, Rule 333", "آیت اللہ سیستانی — واجباتِ سعی، حکم 333", "السيستاني — واجبات السعي، الحكم 333"),
                tr(
                    "Sa‘i follows Tawaf and its prayer; performing it before them requires the prescribed correction.",
                    "سعی طواف اور اس کی نماز کے بعد ہے؛ ان سے پہلے کرنے پر مقررہ اصلاح ضروری ہے۔",
                    "السعي بعد الطواف وصلاته، وأداؤه قبلهما يستلزم التصحيح المقرر."
                ),
                SISTANI_SAI
            )
        ),
        duas = listOf(
            GuideDua(
                title = tr("At Safa and Marwah", "صفا اور مروہ پر", "على الصفا والمروة"),
                arabic = "اللَّهُ أَكْبَرُ، الْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ",
                body = tr(
                    "At Safa, the manual recommends each of these phrases seven times, followed by Salawat and supplication. Repeat the remembrance at Marwah; Imam Ali’s reported Safa dua is in the same source.",
                    "ماخذ کے مطابق صفا پر ہر ذکر سات مرتبہ، پھر درود اور دعا مستحب ہے۔ مروہ پر بھی ذکر کریں؛ امام علیؑ کی منقول دعا اسی ماخذ میں ہے۔",
                    "يستحب في الدليل قول كل ذكر منها سبع مرات على الصفا، ثم الصلاة على محمد وآله والدعاء، ويكرر الذكر على المروة. ودعاء الإمام علي (ع) في المصدر نفسه."
                ),
                citation = recommendedCitation,
                url = SISTANI_RECOMMENDED
            )
        )
    ),
    GuideStep(
        id = 4,
        title = tr("Taqsir — or Halq in Mufradah", "تقصیر — مفردہ میں حلق کا اختیار", "التقصير — أو الحلق في المفردة"),
        description = tr(
            "After Sa‘i, cut some hair with the intention of nearness to Allah. In Umrah al-Tamattu, Taqsir is required and shaving the head is not permitted. In Umrah Mufradah, men may choose Halq or Taqsir; women perform Taqsir.",
            "سعی کے بعد قربتِ الٰہی کی نیت سے کچھ بال کاٹیں۔ عمرہ تمتع میں تقصیر واجب اور سر مونڈنا جائز نہیں۔ عمرہ مفردہ میں مرد حلق یا تقصیر کا انتخاب کر سکتے ہیں؛ خواتین تقصیر کریں۔",
            "بعد السعي، قصّر بعض الشعر بنية القربة. في عمرة التمتع يتعين التقصير ولا يجوز حلق الرأس. وفي العمرة المفردة يخير الرجل بين الحلق والتقصير، وتقصّر المرأة."
        ),
        note = tr(
            "Taqsir has detailed rules concerning hair and nails. Use the linked rulings rather than treating a nail-only shortcut as universally sufficient.",
            "تقصیر میں بالوں اور ناخنوں کے تفصیلی احکام ہیں۔ صرف ناخن کاٹنے کو ہر صورت میں کافی سمجھنے کے بجائے مربوط احکام دیکھیں۔",
            "للتقصير أحكام تفصيلية في الشعر والأظافر؛ راجع الأحكام ولا تعتبر قص الأظافر وحده كافياً في كل الحالات."
        ),
        references = listOf(
            quran(
                "48:27",
                tr(
                    "The Qur’an mentions entering the Sacred Mosque with shaved heads or shortened hair; the exact Umrah rules remain fiqh-specific.",
                    "قرآن میں مسجد الحرام میں داخلے کے ساتھ سر مونڈنے یا بال کم کرنے کا ذکر ہے؛ عمرہ کے دقیق احکام فقہ سے معلوم ہوتے ہیں۔",
                    "يذكر القرآن دخول المسجد الحرام محلقين أو مقصرين، أما أحكام العمرة الدقيقة فتؤخذ من الفقه."
                )
            ),
            fiqh(
                tr("Ayatullah Sistani — Taqsir, Rules 350–357", "آیت اللہ سیستانی — تقصیر، احکام 350–357", "السيستاني — التقصير، الأحكام 350–357"),
                tr(
                    "Explains the required Taqsir of Umrah al-Tamattu and that Tawaf al-Nisa is not obligatory in that Umrah.",
                    "عمرہ تمتع کے لازمی تقصیر اور اس میں طواف النساء کے واجب نہ ہونے کی وضاحت۔",
                    "يبين وجوب التقصير في عمرة التمتع وعدم وجوب طواف النساء فيها."
                ),
                SISTANI_TAQSIR
            ),
            fiqh(
                tr("Ayatullah Sistani — Various kinds of Umrah, Rule 139", "آیت اللہ سیستانی — اقسامِ عمرہ، حکم 139", "السيستاني — أنواع العمرة، الحكم 139"),
                tr(
                    "Distinguishes Mufradah from Tamattu, including Mufradah’s Halq option and obligatory Tawaf al-Nisa.",
                    "مفردہ اور تمتع کا فرق، بشمول مفردہ میں حلق کے اختیار اور طواف النساء کا واجب ہونا۔",
                    "يميز المفردة عن التمتع، بما في ذلك خيار الحلق في المفردة ووجوب طواف النساء."
                ),
                SISTANI_UMRAH_TYPES
            )
        )
    ),
    GuideStep(
        id = 6,
        title = tr("Tawaf al-Nisa — Umrah Mufradah", "طواف النساء — صرف عمرہ مفردہ", "طواف النساء — للعمرة المفردة"),
        description = tr(
            "For Umrah Mufradah, Tawaf al-Nisa and its two Rak‘ahs are obligatory for men and women. It is not obligatory for Umrah al-Tamattu.",
            "عمرہ مفردہ میں طواف النساء اور اس کی دو رکعت نماز مرد اور عورت دونوں پر واجب ہے۔ عمرہ تمتع میں یہ واجب نہیں۔",
            "في العمرة المفردة يجب طواف النساء وركعتاه على الرجال والنساء، وهو غير واجب في عمرة التمتع."
        ),
        requirement = GuideRequirement.CONDITIONAL,
        note = tr(
            "This step affects the permissibility of marital relations. Rules for inability, forgetfulness and representation must be taken from your Marja.",
            "یہ مرحلہ زوجین کے تعلقات کی جواز سے متعلق ہے۔ ناتوانی، بھول یا نیابت کی صورتیں اپنے مرجع کے احکام سے معلوم کریں۔",
            "ترتبط هذه الخطوة بجواز العلاقة الزوجية. تؤخذ أحكام العجز والنسيان والنيابة من مرجعك."
        ),
        references = listOf(
            quran(
                "22:29",
                tr(
                    "The general Qur’anic reference to Tawaf accompanies the specific juristic requirement explained below.",
                    "طواف کا یہ عمومی قرآنی حوالہ ہے، جبکہ خاص شرعی حکم نیچے دیے گئے فقہی ماخذ میں ہے۔",
                    "هذا هو الأصل القرآني العام للطواف، أما الحكم الفقهي الخاص ففي المصدر التالي."
                )
            ),
            fiqh(
                tr("Ayatullah Sistani — Various kinds of Umrah, Rule 139", "آیت اللہ سیستانی — اقسامِ عمرہ، حکم 139", "السيستاني — أنواع العمرة، الحكم 139"),
                tr(
                    "Explicitly distinguishes Umrah Mufradah, in which Tawaf al-Nisa is obligatory, from Umrah al-Tamattu.",
                    "عمرہ مفردہ، جس میں طواف النساء واجب ہے، اور عمرہ تمتع کے درمیان واضح فرق بیان کرتا ہے۔",
                    "يميز صراحة العمرة المفردة التي يجب فيها طواف النساء عن عمرة التمتع."
                ),
                SISTANI_UMRAH_TYPES
            ),
            fiqh(
                tr("Ayatullah Sistani — Tawaf al-Nisa, Rules 417–425", "آیت اللہ سیستانی — طواف النساء، احکام 417–425", "السيستاني — طواف النساء، الأحكام 417–425"),
                tr(
                    "Covers its obligation on men and women, its prayer, and when marital relations may resume.",
                    "مرد و عورت پر اس کا واجب ہونا، اس کی نماز اور زوجین کے تعلقات کی بحالی کے احکام۔",
                    "يتناول وجوبه على الرجال والنساء وصلاته ووقت جواز استئناف العلاقة الزوجية."
                ),
                SISTANI_TAWAF_NISA
            )
        ),
        duas = listOf(
            GuideDua(
                title = tr("Tawaf remembrance", "ذکرِ طواف", "أذكار الطواف"),
                body = tr(
                    "The same reported Tawaf supplications and post-prayer Salawat may be used; choose the relevant intention for Tawaf al-Nisa.",
                    "وہی منقول دعائیں اور نماز کے بعد درود پڑھی جا سکتی ہیں؛ نیت طواف النساء کے مطابق رکھیں۔",
                    "يمكن استخدام أدعية الطواف المنقولة والصلاة على محمد وآله بعد الركعتين، مع نية طواف النساء."
                ),
                citation = recommendedCitation,
                url = SISTANI_RECOMMENDED
            )
        )
    )
)

private val hajjSteps = listOf(
    GuideStep(
        id = 1,
        title = tr("8 Dhu al-Hijjah — Ihram and Mina", "8 ذوالحجہ — احرام اور منیٰ", "8 ذي الحجة — الإحرام والتوجه إلى منى"),
        description = tr(
            "For Hajj al-Tamattu, enter Ihram from Makkah, preferably around zawaal on the day of Tarwiyah, then proceed to Mina for the prayers; spending the night of Arafah in Mina and its devotions are recommended.",
            "حج تمتع کے لیے مکہ سے احرام باندھیں، یوم الترویہ کو زوال کے قریب افضل ہے، پھر منیٰ جا کر نمازیں پڑھیں؛ راتِ عرفہ منیٰ میں قیام اور عبادت مستحب ہے۔",
            "لحج التمتع، أحرم من مكة، ويستحب قرب الزوال يوم التروية، ثم توجه إلى منى للصلوات؛ ويستحب المبيت ليلة عرفة في منى والعبادة فيها."
        ),
        references = listOf(
            quran(
                "22:27",
                tr(
                    "The command to proclaim Hajj to the people is the call answered by the pilgrim’s Talbiyah.",
                    "لوگوں کو حج کی دعوت کا حکم اس پکار کا ماخذ ہے جس کا جواب حاجی تلبیہ سے دیتا ہے۔",
                    "الأمر بالأذان في الناس بالحج هو النداء الذي يجيبه الحاج بالتلبية."
                )
            ),
            fiqh(
                tr("Ayatullah Sistani — Ihram for Hajj", "آیت اللہ سیستانی — حج کا احرام", "السيستاني — الإحرام للحج"),
                tr(
                    "Covers Ihram from Makkah, the preferred time on 8 Dhu al-Hijjah, and exceptions for earlier Ihram.",
                    "مکہ سے احرام، 8 ذوالحجہ کا افضل وقت اور پہلے احرام کی استثنائی صورتوں کی وضاحت۔",
                    "يتناول الإحرام من مكة ووقته المستحب في 8 ذي الحجة وحالات التقديم."
                ),
                SISTANI_HAJJ_IHRAM
            )
        ),
        duas = listOf(talbiyahDua)
    ),
    GuideStep(
        id = 2,
        title = tr("9 Dhu al-Hijjah — Wuquf at Arafat", "9 ذوالحجہ — وقوفِ عرفات", "9 ذي الحجة — الوقوف بعرفات"),
        description = tr(
            "Remain within Arafat with the intention of worship from zawaal until sunset, as required by the cited ruling. Pray Dhuhr and Asr, and devote the time to dhikr, repentance and supplication.",
            "مذکورہ حکم کے مطابق زوال سے غروب تک عبادت کی نیت کے ساتھ عرفات کی حدود میں قیام کریں۔ ظہر و عصر پڑھیں اور وقت کو ذکر، توبہ اور دعا میں صرف کریں۔",
            "ابقَ داخل عرفات بنية العبادة من الزوال إلى الغروب وفق الحكم المذكور، وصلِّ الظهر والعصر واجعل الوقت للذكر والتوبة والدعاء."
        ),
        note = tr(
            "Ghusl at zawaal and joining Dhuhr and Asr with one adhan and two iqamahs are listed among the recommended etiquettes.",
            "زوال پر غسل اور ایک اذان و دو اقامتوں کے ساتھ ظہر و عصر جمع پڑھنا مستحب آداب میں شامل ہے۔",
            "من الآداب المستحبة الغسل عند الزوال والجمع بين الظهر والعصر بأذان واحد وإقامتين."
        ),
        references = listOf(
            quran(
                "2:199",
                tr(
                    "The pilgrimage passage connects departure from Arafat with remembrance at al-Mash‘ar al-Haram.",
                    "مناسک کے اس مقطع میں عرفات سے روانگی کو مشعر الحرام میں ذکر سے جوڑا گیا ہے۔",
                    "يربط مقطع المناسك بين الإفاضة من عرفات والذكر عند المشعر الحرام."
                )
            ),
            GuideReference(
                type = GuideReferenceType.HADITH,
                citation = tr("Imam al-Sadiq (as) — reported guidance for Arafat", "امام صادقؑ — عرفات کی منقول رہنمائی", "الإمام الصادق (ع) — الهدي المنقول لعرفات"),
                summary = tr(
                    "The report urges pilgrims to hasten to prayer and gather for supplication because it is a day of prayer and dua.",
                    "روایت میں نماز کی طرف جلدی اور دعا کے لیے جمع ہونے کی تاکید ہے کیونکہ یہ نماز اور دعا کا دن ہے۔",
                    "تحض الرواية على المبادرة إلى الصلاة والاجتماع للدعاء لأنه يوم صلاة ودعاء."
                ),
                url = SISTANI_RECOMMENDED
            )
        ),
        duas = listOf(
            GuideDua(
                title = tr("Dua Arafah — Imam Hussain (as)", "دعائے عرفہ — امام حسینؑ", "دعاء عرفة — الإمام الحسين (ع)"),
                body = tr(
                    "Recite the complete supplication from the linked text rather than relying on a shortened quotation.",
                    "مکمل دعا دیے گئے متن سے پڑھیں، مختصر اقتباس پر اکتفا نہ کریں۔",
                    "اقرأ الدعاء كاملاً من النص المرفق بدلاً من الاكتفاء باقتباس مختصر."
                ),
                citation = tr("Imam Hussain (as) — Duas.org", "امام حسینؑ — Duas.org", "الإمام الحسين (ع) — Duas.org"),
                url = DUA_ARAFAH
            ),
            GuideDua(
                title = tr("Sahifa al-Sajjadiya — Supplication 47", "صحیفہ سجادیہ — دعا 47", "الصحيفة السجادية — الدعاء 47"),
                body = tr(
                    "The Arafah Aamal collection also includes Imam Zayn al-Abidin’s supplication for this day.",
                    "اعمالِ عرفہ کے مجموعے میں امام زین العابدینؑ کی اس دن کی دعا بھی شامل ہے۔",
                    "يتضمن مجموع أعمال عرفة أيضاً دعاء الإمام زين العابدين (ع) لهذا اليوم."
                ),
                citation = tr("Imam Zayn al-Abidin (as) — Arafah Aamal", "امام زین العابدینؑ — اعمالِ عرفہ", "الإمام زين العابدين (ع) — أعمال عرفة"),
                url = ARAFAH_AMAAL
            )
        )
    ),
    GuideStep(
        id = 3,
        title = tr("Night of 10th — Muzdalifah / Mash‘ar", "10 کی رات — مزدلفہ / مشعر الحرام", "ليلة العاشر — مزدلفة / المشعر الحرام"),
        description = tr(
            "After sunset, leave Arafat for Muzdalifah. Delay Maghrib and Isha until arrival and pray them together; remain for the required night and dawn-to-sunrise Wuquf.",
            "غروب کے بعد عرفات سے مزدلفہ روانہ ہوں۔ مغرب و عشاء وہاں پہنچ کر جمع پڑھیں، اور رات کے مقررہ حصے اور فجر تا طلوعِ آفتاب وقوف مکمل کریں۔",
            "بعد الغروب، اتجه من عرفات إلى مزدلفة، وأخر المغرب والعشاء إلى الوصول واجمع بينهما، وأتم الوقوف الليلي المقرر ووقوف الفجر إلى الشروق."
        ),
        note = tr(
            "Collect pebbles for the required Rami. The manual lists collecting seventy among the recommended acts; it is not a universal statement of the obligatory count for every pilgrim.",
            "واجب رمی کے لیے کنکریاں جمع کریں۔ رہنما میں ستر کنکریاں جمع کرنا مستحب اعمال میں ہے؛ یہ ہر حاجی کے واجب شمار کا عمومی حکم نہیں۔",
            "اجمع الحصى للرمي الواجب. ذكر الدليل جمع سبعين حصاة ضمن المستحبات، وليس ذلك عدداً واجباً موحداً لكل حاج."
        ),
        references = listOf(
            quran(
                "2:199",
                tr(
                    "After departure from Arafat, the verse commands remembrance of Allah at al-Mash‘ar al-Haram.",
                    "عرفات سے روانگی کے بعد آیت مشعر الحرام میں اللہ کے ذکر کا حکم دیتی ہے۔",
                    "بعد الإفاضة من عرفات تأمر الآية بذكر الله عند المشعر الحرام."
                )
            ),
            fiqh(
                tr("Ayatullah Sistani — Wuquf at Muzdalifah, Rules 372–376", "آیت اللہ سیستانی — وقوفِ مزدلفہ، احکام 372–376", "السيستاني — الوقوف بمزدلفة، الأحكام 372–376"),
                tr(
                    "Explains the night stay, the obligatory dawn-to-sunrise Wuquf, intention, exemptions and remedial timing.",
                    "رات کے قیام، فجر تا طلوعِ آفتاب وقوف، نیت، استثناء اور متبادل وقت کے احکام۔",
                    "يبين المبيت والوقوف الواجب من الفجر إلى الشروق والنية والإعفاءات والوقت البديل."
                ),
                SISTANI_MUZDALIFAH
            )
        ),
        duas = listOf(
            GuideDua(
                title = tr("Mash‘ar supplication and dhikr", "مشعر الحرام میں دعا و ذکر", "الدعاء والذكر في المشعر"),
                body = tr(
                    "The manual recommends night worship, Salawat and the reported supplication beginning ‘O Allah, Lord of the Holy Mash‘ar’, along with combining Maghrib and Isha using one adhan and two iqamahs.",
                    "رہنما میں رات کی عبادت، درود، اور ‘اے اللہ! اے مشعر الحرام کے رب’ سے شروع ہونے والی دعا، نیز ایک اذان و دو اقامتوں کے ساتھ مغرب و عشاء جمع پڑھنا مستحب ہے۔",
                    "يستحب في الدليل إحياء الليل بالعبادة والصلاة على محمد وآله والدعاء المنقول «اللهم يا رب المشعر الحرام»، والجمع بين المغرب والعشاء بأذان واحد وإقامتين."
                ),
                citation = recommendedCitation,
                url = SISTANI_RECOMMENDED
            )
        )
    ),
    GuideStep(
        id = 4,
        title = tr("10 Dhu al-Hijjah — Rami, Hady, Halq/Taqsir", "10 ذوالحجہ — رمی، قربانی، حلق/تقصیر", "10 ذي الحجة — الرمي والهدي والحلق/التقصير"),
        description = tr(
            "At Mina, perform the acts in order: stone Jamrat al-Aqabah with seven pebbles, offer the required Hady, then shave or perform Taqsir. Women perform Taqsir rather than Halq.",
            "منیٰ میں یہ ترتیب رکھیں: جمرہ عقبہ پر سات کنکریاں ماریں، واجب قربانی کریں، پھر حلق یا تقصیر کریں۔ خواتین حلق کے بجائے تقصیر کریں۔",
            "في منى، راعِ الترتيب: ارمِ جمرة العقبة بسبع حصيات، ثم قدم الهدي الواجب، ثم احلق أو قصّر. وتقصّر المرأة ولا تحلق."
        ),
        note = tr(
            "These acts lift some restrictions, but the Makkah rites and Tawaf al-Nisa remain. Rules for inability, representation and the animal’s conditions require the detailed rulings.",
            "ان اعمال سے بعض پابندیاں ختم ہوتی ہیں، لیکن مکہ کے مناسک اور طواف النساء باقی ہیں۔ ناتوانی، نیابت اور قربانی کے جانور کی شرائط کے لیے تفصیلی احکام دیکھیں۔",
            "ترفع هذه الأعمال بعض القيود، لكن مناسك مكة وطواف النساء تبقى. راجع الأحكام التفصيلية للعجز والنيابة وشروط الهدي."
        ),
        references = listOf(
            quran(
                "22:36–37",
                tr(
                    "The sacrificial animals are described as rites of Allah, with the emphasis on devotion rather than flesh or blood.",
                    "قربانی کے جانوروں کو اللہ کی شعائر میں قرار دیا گیا ہے، اور اصل مقصد گوشت و خون نہیں تقویٰ بتایا گیا ہے۔",
                    "يعد البدن من شعائر الله، ويؤكد أن المقصود التقوى لا اللحوم والدماء."
                )
            ),
            fiqh(
                tr("Ayatullah Sistani — Hajj al-Tamattu, Rule 149", "آیت اللہ سیستانی — حج تمتع، حکم 149", "السيستاني — حج التمتع، الحكم 149"),
                tr(
                    "Lists the thirteen obligations and their sequence, including Rami, Hady, Halq/Taqsir and the later Makkah rites.",
                    "تیرہ واجبات اور ان کی ترتیب بیان کرتا ہے، بشمول رمی، قربانی، حلق/تقصیر اور بعد کے مناسکِ مکہ۔",
                    "يعدد الواجبات الثلاثة عشر وترتيبها، بما فيها الرمي والهدي والحلق/التقصير ومناسك مكة اللاحقة."
                ),
                SISTANI_TAMATTU
            ),
            fiqh(
                tr("Ayatullah Sistani — Shaving and Taqsir, Rules 403–404", "آیت اللہ سیستانی — حلق و تقصیر، احکام 403–404", "السيستاني — الحلق والتقصير، الحكمان 403–404"),
                tr(
                    "Women perform Taqsir; men may choose Halq or Taqsir, with Halq preferred.",
                    "خواتین تقصیر کریں؛ مرد حلق یا تقصیر کا انتخاب کر سکتے ہیں اور حلق افضل ہے۔",
                    "تقصّر المرأة، ويخير الرجل بين الحلق والتقصير، والحلق أفضل."
                ),
                SISTANI_HALQ
            )
        ),
        duas = listOf(
            ramiDua,
            GuideDua(
                title = tr("At the sacrifice — Qur’an 6:162", "قربانی کے وقت — قرآن 6:162", "عند الهدي — القرآن 6:162"),
                arabic = "إِنَّ صَلَاتِي وَنُسُكِي وَمَحْيَايَ وَمَمَاتِي لِلَّهِ رَبِّ الْعَالَمِينَ",
                body = tr(
                    "Indeed, my prayer, my sacrifice, my living and my dying are for Allah, Lord of the worlds. This verse is included in the manual’s recommended Hady supplication.",
                    "بے شک میری نماز، میری قربانی، میرا جینا اور میرا مرنا اللہ رب العالمین کے لیے ہے۔ رہنما کی مستحب دعائے قربانی میں یہ آیت شامل ہے۔",
                    "تتضمن دعاء الهدي المستحب في الدليل هذه الآية القرآنية."
                ),
                citation = tr(
                    "Qur’an 6:162; Sistani’s recommended Hady supplication",
                    "قرآن 6:162؛ سیستانی کی مستحب دعائے قربانی",
                    "القرآن 6:162؛ ودعاء الهدي المستحب عند السيستاني"
                ),
                url = SISTANI_RECOMMENDED
            ),
            GuideDua(
                title = tr("When shaving", "حلق کے وقت", "عند الحلق"),
                body = tr(
                    "The recommended supplication asks Allah to grant light on the Day of Judgement for every hair.",
                    "مستحب دعا میں اللہ سے ہر بال کے بدلے قیامت کے دن نور مانگا گیا ہے۔",
                    "يتضمن الدعاء المستحب سؤال الله نوراً يوم القيامة عن كل شعرة."
                ),
                citation = recommendedCitation,
                url = SISTANI_RECOMMENDED
            )
        )
    ),
    GuideStep(
        id = 5,
        title = tr("Makkah — Tawaf, Prayer, Sa‘i and Tawaf al-Nisa", "مکہ — طواف، نماز، سعی اور طواف النساء", "مكة — الطواف والصلاة والسعي وطواف النساء"),
        description = tr(
            "Return to Makkah for Tawaf al-Hajj and its two Rak‘ahs, then Sa‘i, then Tawaf al-Nisa and its two Rak‘ahs. Marital relations resume only after Tawaf al-Nisa and its prayer, according to Rule 425.",
            "مکہ واپس جا کر طواف حج اور اس کی دو رکعت، پھر سعی، پھر طواف النساء اور اس کی دو رکعت نماز ادا کریں۔ حکم 425 کے مطابق زوجین کے تعلقات صرف طواف النساء اور اس کی نماز کے بعد بحال ہوتے ہیں۔",
            "عد إلى مكة لطواف الحج وركعتيه، ثم السعي، ثم طواف النساء وركعتيه. وبحسب الحكم 425 لا تستأنف العلاقة الزوجية إلا بعد طواف النساء وصلاته."
        ),
        note = tr(
            "Tawaf al-Nisa and its prayer are obligatory, though the cited manual clarifies that they are not among the arkan of Hajj. They must not be treated as optional.",
            "طواف النساء اور اس کی نماز واجب ہیں، اگرچہ ماخذ واضح کرتا ہے کہ یہ حج کے ارکان میں شامل نہیں۔ انہیں اختیاری نہ سمجھیں۔",
            "طواف النساء وصلاته واجبان، وإن أوضح الدليل أنهما ليسا من أركان الحج؛ فلا يجوز اعتبارهما اختياريين."
        ),
        references = listOf(
            quran(
                "22:29",
                tr(
                    "The verse includes Tawaf of the Ancient House among the pilgrimage rites.",
                    "آیت میں مناسک کے حصے کے طور پر بیت العتیق کے طواف کا ذکر ہے۔",
                    "تتضمن الآية الطواف بالبيت العتيق ضمن المناسك."
                )
            ),
            quran(
                "2:158",
                tr(
                    "Safa and Marwah are identified as symbols of Allah in the Qur’anic passage on pilgrimage.",
                    "مناسک کے قرآنی مقطع میں صفا و مروہ کو اللہ کی نشانیوں میں قرار دیا گیا ہے۔",
                    "تجعل الآية الصفا والمروة من شعائر الله في مقطع المناسك."
                )
            ),
            fiqh(
                tr("Ayatullah Sistani — Tawaf al-Nisa, Rules 417–425", "آیت اللہ سیستانی — طواف النساء، احکام 417–425", "السيستاني — طواف النساء، الأحكام 417–425"),
                tr(
                    "Explains its obligation, its prayer, its difference from the arkan, and its effect on marital relations.",
                    "اس کا واجب ہونا، نماز، ارکان سے فرق اور زوجین کے تعلقات پر اثر کے احکام۔",
                    "يبين وجوبه وصلاته وفرقه عن الأركان وأثره في العلاقة الزوجية."
                ),
                SISTANI_TAWAF_NISA
            )
        ),
        duas = listOf(
            GuideDua(
                title = tr("Tawaf and Sa‘i adhkar", "طواف اور سعی کے اذکار", "أذكار الطواف والسعي"),
                body = tr(
                    "Use the same reported Tawaf and Sa‘i supplications listed in the Umrah steps, with the correct intention for each Hajj rite, and send Salawat after the prayers.",
                    "ہر رکن کی صحیح نیت کے ساتھ عمرہ کے مراحل میں دی گئی طواف و سعی کی منقول دعائیں پڑھیں، اور نمازوں کے بعد درود بھیجیں۔",
                    "استخدم أدعية الطواف والسعي المنقولة في خطوات العمرة مع النية الصحيحة لكل ركن من أركان الحج، وصلِّ على محمد وآله بعد الصلوات."
                ),
                citation = recommendedCitation,
                url = SISTANI_RECOMMENDED
            )
        )
    ),
    GuideStep(
        id = 6,
        title = tr("11–12 Dhu al-Hijjah — Mina and the three Jamarat", "11–12 ذوالحجہ — منیٰ اور تین جمرات", "11–12 ذي الحجة — منى والجمرات الثلاث"),
        description = tr(
            "Spend the required nights in Mina. On both days, stone Jamrat al-Ula, then al-Wusta, then al-Aqabah with seven pebbles each during the valid daytime period.",
            "منیٰ میں مقررہ راتیں گزاریں۔ دونوں دن صحیح وقتِ روز میں پہلے جمرہ اولیٰ، پھر وسطیٰ، پھر عقبہ پر سات سات کنکریاں ماریں۔",
            "ابت في منى الليالي المقررة، وفي كلا اليومين ارمِ جمرة الأولى ثم الوسطى ثم العقبة بسبع حصيات لكل منها في الوقت النهاري الصحيح."
        ),
        note = tr(
            "The linked rules explain when the night of the 13th and its Rami become necessary, including remaining in Mina until nightfall and certain Ihram violations.",
            "مربوط احکام میں 13 کی رات اور اس کی رمی کب لازم ہے بیان ہے، بشمول منیٰ میں رات تک ٹھہرنے اور بعض احرام کی خلاف ورزیوں کی صورتیں۔",
            "تبين الأحكام المرفقة متى تجب ليلة الثالث عشر ورميها، بما في ذلك البقاء في منى إلى الليل وبعض مخالفات الإحرام."
        ),
        references = listOf(
            quran(
                "2:203",
                tr(
                    "The verse commands remembrance of Allah during the numbered days.",
                    "آیت گنے ہوئے دنوں میں اللہ کے ذکر کا حکم دیتی ہے۔",
                    "تأمر الآية بذكر الله في أيام معدودات."
                )
            ),
            fiqh(
                tr("Ayatullah Sistani — Wuquf at Mina, Rules 426–430", "آیت اللہ سیستانی — قیامِ منیٰ، احکام 426–430", "السيستاني — المبيت بمنى، الأحكام 426–430"),
                tr(
                    "Covers the required nights, departure on the 12th, the night of the 13th, and exemptions.",
                    "واجب راتیں، 12 کو روانگی، 13 کی رات اور استثناء کے احکام۔",
                    "يتناول الليالي الواجبة والخروج يوم الثاني عشر وليلة الثالث عشر والإعفاءات."
                ),
                SISTANI_MINA_STAY
            ),
            fiqh(
                tr("Ayatullah Sistani — Rami of Jamarat, Rules 431–437", "آیت اللہ سیستانی — رمی جمرات، احکام 431–437", "السيستاني — رمي الجمرات، الأحكام 431–437"),
                tr(
                    "Covers the Ula–Wusta–Aqabah order, daytime Rami, the 13th when applicable, and qadha or representation.",
                    "اولیٰ–وسطیٰ–عقبہ کی ترتیب، دن میں رمی، صورتِ لزوم میں 13 کی رمی، قضا اور نیابت کے احکام۔",
                    "يتناول ترتيب الأولى فالوسطى فالعقبة والرمي نهاراً ورمي الثالث عشر عند الاقتضاء والقضاء والنيابة."
                ),
                SISTANI_RAMI
            )
        ),
        duas = listOf(
            ramiDua,
            GuideDua(
                title = tr("Takbir during the days of Tashriq", "ایام تشریق کے تکبیرات", "تكبيرات أيام التشريق"),
                arabic = "اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ، لَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، وَلِلَّهِ الْحَمْدُ",
                body = tr(
                    "Allah is the Greatest, Allah is the Greatest. There is no god but Allah, and Allah is the Greatest. Allah is the Greatest, and all praise belongs to Allah. The manual recommends Takbir after the prescribed prayers from Dhuhr on the 10th.",
                    "اللہ سب سے بڑا، اللہ سب سے بڑا ہے۔ اللہ کے سوا کوئی معبود نہیں، اور اللہ سب سے بڑا ہے۔ اللہ سب سے بڑا ہے، اور تمام تعریف اللہ کے لیے ہے۔ رہنما میں 10 کو ظہر سے مقررہ نمازوں کے بعد تکبیر مستحب ہے۔",
                    "الله أكبر الله أكبر، لا إله إلا الله، والله أكبر، الله أكبر، ولله الحمد. يستحب في الدليل التكبير بعد الصلوات المقررة ابتداءً من ظهر يوم العاشر."
                ),
                citation = recommendedCitation,
                url = SISTANI_RECOMMENDED
            )
        )
    ),
    GuideStep(
        id = 7,
        title = tr("Farewell Tawaf and Ziyarat — recommended", "طواف وداع اور زیارت — مستحب", "طواف الوداع والزيارة — مستحب"),
        description = tr(
            "Before leaving Makkah, the manual recommends a farewell Tawaf, remembrance and supplication. It also strongly recommends visiting Madinah for the Prophet, Lady Fatimah and the Imams of Baqi.",
            "مکہ سے روانگی سے قبل رہنما میں طواف وداع، ذکر اور دعا مستحب ہے۔ مدینہ میں رسولؐ، سیدہ فاطمہؑ اور ائمہِ بقیعؑ کی زیارت بھی بہت تاکید کے ساتھ مستحب بتائی گئی ہے۔",
            "قبل مغادرة مكة، يستحب في الدليل طواف الوداع والذكر والدعاء. ويستحب بشدة زيارة المدينة للنبي (ص) والسيدة فاطمة (ع) وأئمة البقيع (ع)."
        ),
        requirement = GuideRequirement.RECOMMENDED,
        note = tr(
            "This recommended step is not one of the thirteen obligatory acts of Hajj al-Tamattu.",
            "یہ مستحب مرحلہ حج تمتع کے تیرہ واجب اعمال میں شامل نہیں۔",
            "هذه الخطوة المستحبة ليست من الأعمال الثلاثة عشر الواجبة في حج التمتع."
        ),
        references = listOf(
            GuideReference(
                type = GuideReferenceType.IMAM,
                citation = tr("Recommended Ziyarat — Prophet, Lady Fatimah and the Imams of Baqi", "مستحب زیارت — رسولؐ، سیدہ فاطمہؑ اور ائمہِ بقیعؑ", "الزيارة المستحبة — النبي والسيدة فاطمة وأئمة البقيع (ع)"),
                summary = tr(
                    "The closing section of Sistani’s recommended-acts manual strongly encourages these Ziyarats in Madinah after Hajj.",
                    "سیستانی کے مستحب اعمال کے آخری حصے میں حج کے بعد مدینہ میں ان زیارتوں کی قوی تاکید ہے۔",
                    "يؤكد القسم الأخير من دليل المستحبات عند السيستاني على هذه الزيارات في المدينة بعد الحج."
                ),
                url = SISTANI_RECOMMENDED
            ),
            fiqh(
                tr("Ayatullah Sistani — Farewell Tawaf", "آیت اللہ سیستانی — طواف وداع", "السيستاني — طواف الوداع"),
                summary = tr(
                    "Lists the farewell Tawaf and its supplications among recommended, not obligatory, acts.",
                    "طواف وداع اور اس کی دعاؤں کو واجب کے بجائے مستحب اعمال میں بیان کرتا ہے۔",
                    "يذكر طواف الوداع وأدعيته ضمن المستحبات لا الواجبات."
                ),
                url = SISTANI_RECOMMENDED
            )
        ),
        duas = listOf(
            GuideDua(
                title = tr("At departure", "روانگی کے وقت", "عند المغادرة"),
                body = tr(
                    "Thank and praise Allah, send Salawat on Muhammad and his Progeny, pray for acceptance, and ask Allah to grant you another visit.",
                    "اللہ کا شکر اور حمد کریں، محمد و آلِ محمد پر درود بھیجیں، قبولیت کی دعا کریں اور دوبارہ زیارت کی توفیق مانگیں۔",
                    "احمد الله واشكره، وصلِّ على محمد وآله، واسأل قبول المناسك وأن يرزقك زيارة أخرى."
                ),
                citation = recommendedCitation,
                url = SISTANI_RECOMMENDED
            )
        )
    )
)

data class HajjJourneyStop(
    val order: Int,
    val location: LocalizedGuideText,
    val date: LocalizedGuideText,
    val ritual: LocalizedGuideText
)

private val journeyStops = listOf(
    HajjJourneyStop(
        order = 1,
        location = tr("Makkah", "مکہ", "مكة"),
        date = tr("8 Dhu al-Hijjah", "8 ذوالحجہ", "8 ذو الحجة"),
        ritual = tr(
            "Enter Ihram for Hajj al-Tamattu and begin Talbiyah.",
            "حج تمتع کا احرام باندھیں اور تلبیہ شروع کریں۔",
            "أحرم لحج التمتع وابدأ التلبية."
        )
    ),
    HajjJourneyStop(
        order = 2,
        location = tr("Mina", "منیٰ", "منى"),
        date = tr("8 Dhu al-Hijjah", "8 ذوالحجہ", "8 ذو الحجة"),
        ritual = tr(
            "Pray and prepare for Arafat; the night’s worship in Mina is recommended.",
            "نماز اور عرفہ کی تیاری؛ منیٰ میں رات کی عبادت مستحب ہے۔",
            "صلِّ واستعد لعرفة؛ وتستحب عبادة الليل في منى."
        )
    ),
    HajjJourneyStop(
        order = 3,
        location = tr("Arafat", "عرفات", "عرفات"),
        date = tr("9 Dhu al-Hijjah", "9 ذوالحجہ", "9 ذو الحجة"),
        ritual = tr(
            "Wuquf from zawaal to sunset, Dhuhr and Asr, repentance and Dua Arafah.",
            "زوال تا غروب وقوف، ظہر و عصر، توبہ اور دعائے عرفہ۔",
            "الوقوف من الزوال إلى الغروب، والظهر والعصر، والتوبة ودعاء عرفة."
        )
    ),
    HajjJourneyStop(
        order = 4,
        location = tr("Muzdalifah — al-Mash‘ar al-Haram", "مزدلفہ — مشعر الحرام", "مزدلفة — المشعر الحرام"),
        date = tr("Night of the 10th", "10 کی رات", "ليلة العاشر"),
        ritual = tr(
            "Maghrib and Isha, the required Wuquf, remembrance and pebbles for Rami.",
            "مغرب و عشاء، واجب وقوف، ذکر اور رمی کے لیے کنکریاں۔",
            "المغرب والعشاء، والوقوف الواجب، والذكر وجمع الحصى للرمي."
        )
    ),
    HajjJourneyStop(
        order = 5,
        location = tr("Mina", "منیٰ", "منى"),
        date = tr("10 Dhu al-Hijjah", "10 ذوالحجہ", "10 ذو الحجة"),
        ritual = tr(
            "Jamrat al-Aqabah → Hady → Halq or Taqsir, in that order.",
            "جمرہ عقبہ ← قربانی ← حلق یا تقصیر، اسی ترتیب سے۔",
            "جمرة العقبة ← الهدي ← الحلق أو التقصير، بهذا الترتيب."
        )
    ),
    HajjJourneyStop(
        order = 6,
        location = tr("Makkah", "مکہ", "مكة"),
        date = tr("After the Mina rites", "منیٰ کے اعمال کے بعد", "بعد أعمال منى"),
        ritual = tr(
            "Tawaf al-Hajj and prayer → Sa‘i → Tawaf al-Nisa and prayer.",
            "طواف حج و نماز ← سعی ← طواف النساء و نماز۔",
            "طواف الحج وصلاته ← السعي ← طواف النساء وصلاته."
        )
    ),
    HajjJourneyStop(
        order = 7,
        location = tr("Return to Mina", "منیٰ واپسی", "العودة إلى منى"),
        date = tr("11–12; 13th when required", "11–12؛ ضرورت ہو تو 13", "11–12؛ والثالث عشر عند اللزوم"),
        ritual = tr(
            "Required nights, the three Jamarat in order, and Takbir during Tashriq.",
            "واجب راتیں، ترتیب سے تینوں جمرات، اور ایام تشریق کے تکبیرات۔",
            "الليالي الواجبة، والجمرات الثلاث بالترتيب، وتكبيرات التشريق."
        )
    )
)

fun hajjJourneyStops(): List<HajjJourneyStop> = journeyStops

fun umrahGuideSteps(): List<GuideStep> = umrahSteps

fun hajjGuideSteps(): List<GuideStep> = hajjSteps
