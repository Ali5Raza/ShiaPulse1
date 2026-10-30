package com.example.utils

object LocalizationUtility {

    private val translations = mapOf(
        "en" to mapOf(
            // Shared actions and first-run experience
            "action_back" to "Back",
            "action_next" to "Next",
            "action_skip" to "Skip",
            "action_cancel" to "Cancel",
            "action_save" to "Save",
            "onboarding_step" to "Step %1${'$'}d of %2${'$'}d",
            "onboarding_language_title" to "Choose Your Language",
            "onboarding_location_title" to "Allow Location Access",
            "onboarding_location_description" to "We use your location to provide accurate prayer times, Qibla direction, nearby mosques, and regional Islamic occasions.",
            "onboarding_grant_location" to "Grant Location Permission",
            "onboarding_or" to "OR",
            "onboarding_select_city" to "Select City Manually",
            "onboarding_selected" to "Selected: %1${'$'}s",
            "onboarding_location_required" to "Please grant location permission or select a city to continue.",
            "onboarding_account_title" to "Connect Your Account",
            "onboarding_account_description" to "Link your Google account to back up and sync your preferences. You can skip this step.",
            "onboarding_continue_google" to "Continue with Google",
            "onboarding_connected" to "Connected",
            "onboarding_sign_in_error" to "Sign-in failed. Please try again.",
            "onboarding_ready_title" to "You're All Set! 🎉",
            "onboarding_language" to "Language",
            "onboarding_location" to "Location",
            "onboarding_email" to "Email",
            "onboarding_not_provided" to "Not provided",
            "onboarding_get_started" to "Get Started",

            // AI companion
            "ai_companion_title" to "AI Companion",
            "ai_greeting" to "Salaam! I am your ShiaPulse AI Companion. Ask about Islamic teachings, Ahlul Bayt (as), history, or daily practices. How can I help?",
            "ai_disclaimer" to "AI can make mistakes. Verify Qur'an, hadith, and religious rulings with authentic sources and a qualified Shia scholar.",
            "ai_input_hint" to "Ask something...",
            "ai_send" to "Send",
            "ai_cooldown" to "Please wait a few seconds before sending another message.",
            "ai_no_response" to "No response was received. Please try again.",
            "ai_unavailable" to "The AI service is not configured on this build.",
            "ai_rate_limit" to "The service is busy. Please try again in a minute.",
            "ai_http_error" to "Could not connect to the AI service (code %1${'$'}d). Please try again later.",
            "ai_network_error" to "Unable to connect. Check your internet connection and try again.",

            // Calendar and history
            "calendar_history" to "Calendar & History",
            "calendar_grid_subtitle" to "Interactive Gregorian & Hijri Grid",
            "calendar_no_events" to "No major Shia historical or international events are logged for this date.",
            "calendar_shia_events" to "Shia Islamic Events",
            "calendar_international_events" to "International Events",
            "calendar_previous_month" to "Previous Month",
            "calendar_next_month" to "Next Month",
            "calendar_joyous" to "Joyous Celebration",
            "calendar_solemn" to "Solemn Commemoration",
            "calendar_international" to "International",
            "calendar_today" to "Today",
            "calendar_hijri_notice" to "Hijri dates are calculated estimates and may differ by one day with local moon sighting.",
            "calendar_search_events" to "Search events or descriptions",
            "calendar_all_events" to "All",
            "calendar_wiladat" to "Wiladat",
            "calendar_shahadat" to "Shahadat",
            "calendar_no_matching_events" to "No events match this search and filter.",

            // Frequently used settings labels
            "settings_profile_subtitle" to "Preferences & Cloud Synchronization",
            "settings_account_cloud" to "Account & Cloud",
            "settings_personalization" to "Personalization",
            "settings_notifications" to "Notifications & Reminders",
            "settings_support" to "Support & Legal",
            "settings_faq" to "Frequently Asked Questions",
            "settings_sign_in" to "Sign in with Google",
            "settings_sign_out" to "Sign Out",
            "settings_backup" to "Backup Now",
            "settings_restore" to "Restore Now",
            "settings_interface_language" to "Interface Language",
            "settings_theme" to "App Color Theme",
            "settings_dark" to "Dark",
            "settings_light" to "Light",
            "settings_system" to "System",
            "settings_share" to "Share Application",

            "slogan" to "Comprehensive Shia Companion Platform",
            "time_format" to "Use 24-Hour Format",
            "verse_text" to "“And my success is not but through Allah.”",
            "verse_source" to "Surah Hud, Verse 88",
            "guidance_title" to "Ahlul Bayt (as) Guidance",
            "ask_gemini" to "Ask Gemini Topic",
            "imsak" to "Imsak",
            "fajr" to "Fajr",
            "sunrise" to "Sunrise",
            "dhuhr" to "Dhuhr",
            "asr" to "Asr",
            "sunset" to "Sunset",
            "maghrib" to "Maghrib",
            "isha" to "Isha",
            "midnight" to "Midnight (Nisfe Shab)",
            "quran" to "Quran",
            "mafatih" to "Mafatih",
            "ahkam" to "Ahkam",
            "meet" to "Meet",
            "calendar" to "Calendar",
            "hussainiyah" to "Hussainiyah",
            "library" to "Library",
            "tasbeeh" to "Tasbeeh",
            "tracker" to "Prayer Tracker & Qada",
            "exact_location" to "Astronomical Shia Offsets",
            "exact_location_sub" to "Fajr begins 16° twilight, Asr calculations utilize Shadow rules, and Maghrib is precisely 4 degrees past sunset.",
            "settings" to "Settings & Profile",
            "language" to "Change Language",
            "adhan" to "Adhan Notifications",
            "notifications_enabled" to "Adhan Sound Alerts Enabled",
            "notifications_disabled" to "Adhan Sound Alerts Disabled",
            "share" to "Share Application",
            "faq" to "Frequently Asked Questions (FAQ)",
            "back" to "Return",
            "log_in_google" to "Sign In with Google",
            "log_out" to "Sign Out",
            "google_logged_in" to "Signed in with Google Securely",
            "sim_login" to "Simulating Secure Login Sync...",
            "show_guidance_startup" to "Show Ahlul Bayt Guidance on Startup",
            "show_guidance_startup_sub" to "Toggle to show or hide the Ahlul Bayt (as) Guidance pop-up upon application launch.",
            "guidance_language" to "Guidance Language Setting",
            "guidance_language_sub" to "Choose to display start-up guidance and Shia quotes in English or Urdu directly.",
            "masoomeen_title" to "14 Infallible Masoomeen (as)",
            "masoomeen_sub" to "Explore the sacred biographies and lives of Ahlul Bayt",
            "born" to "Born",
            "martyred" to "Martyred",
            "burial" to "Burial Shrine",
            "close" to "Close",
            
            // Legacy/Modern Buttons UI Localizer
            "tile_dua" to "Dua",
            "tile_calculator" to "Calculator",
            "tile_qibla" to "Qibla",
            "tile_calendar" to "Calendar",
            "tile_mosque" to "Mosque Map",
            "tile_tasbeeh" to "Tasbeeh",
            "tile_tracker" to "Tracker",
            "tile_aichat" to "AI Chat",
            "tile_masoomeen" to "Masoomeen",
            "tile_names_allah" to "99 Names",
            "tile_names_muhammad" to "Prophet Names",
            "tile_youtube" to "Videos",
            "tile_ramadan" to "Ramadan Tracker",

            // Qibla Compass specific
            "qibla_compass" to "QIBLA COMPASS",
            "aligned_kaaba" to "ALIGNED WITH KAABA",
            "aligned_success" to "Successfully aligned for Prayer!",
            "slow_rotate" to "Slowly rotate your device left or right until pointer turns Green",
            "compass_dial" to "Compass Dial",
            "map_pin_view" to "Map Pin View",
            "to_kaaba" to "to Kaaba",
            "geographic_calcs" to "GEOGRAPHIC CALCULATIONS",
            "selected_reference" to "Selected Reference",
            "kaaba_direction" to "Kaaba Direction (Ghadir angle)",
            "east_north" to "True East-North",
            "global_qibla_map" to "GLOBAL QIBLA PIN MAP",
            "mecca_kaaba" to "Mecca (Kaaba)",
            "reference_pin" to "Reference Pin",
            "holy_kaaba" to "Holy Kaaba",
            "mecca_sa" to "Mecca, SA",
            "gps_sensor_warning" to "Turn on GPS / place machine flat away from electronic laptops, charging adapters, or magnets to ensure clean sensor feedback.",

            // Custom Calculators & Khums labels
            "islamic_calculators" to "Islamic Calculators",
            "select_calc_sub" to "Select a calculator to continue",
            "khums_calc" to "Khums Calculator",
            "khums_calc_desc" to "Calculate 20% Islamic tax on surplus wealth.",
            "zakat_calc" to "Zakat Calculator",
            "zakat_calc_desc" to "Calculate Wajib and Mustahab Zakat on assets.",
            "fitrana_calc" to "Fitrana (Zakat al-Fitr)",
            "fitrana_calc_desc" to "Calculate Fitrana for your family.",
            "musafir_calc" to "Musafir Calculator",
            "musafir_calc_desc" to "Calculate travel distance and prayer rules.",
            "inheritance_calc" to "Inheritance Calculator",
            "inheritance_calc_desc" to "Shia Fiqh based inheritance distribution.",
            "date_conv_calc" to "Date Converter",
            "greg_hijri_desc" to "Convert between Islamic Hijri & Gregorian dates.",
            "khums_info_text" to "Khums is a 20% tax on annual surplus wealth (savings leftover after necessary daily yearly expenses). In Shia jurisprudence, it is split equally between Sehm-e-Imam and Sehm-e-Sadat.",
            "annual_income" to "Total Annual Income/Savings",
            "allowable_expenses" to "Allowable Yearly Expenses",
            "total_surplus" to "Total Surplus",
            "khums_due" to "Khums Due",
            "sehm_imam_label" to "Sehm-e-Imam (50%)",
            "sehm_sadat_label" to "Sehm-e-Sadat (50%)",

            // Duas & Ziyarats Screen
            "duas_ziyarats" to "Shi'ite Duas & Ziyarat",
            "duas_ziyarats_sub" to "Authentic prayers with Audio",
            "search_duas" to "Search Supplications / Ziyarats...",
            "cat_all" to "All",
            "cat_famed" to "Famed",
            "cat_weekly" to "Weekly",
            "cat_daily" to "Daily",
            "cat_ziyarats" to "Ziyarats",
            "lunar_phase_tracker" to "Lunar Phase Tracker",
            "masoomeen_remembrance_calendar" to "Islamic Events Calendar",
            "lunar_phase_tracker_sub" to "Islamic Date & Moon Phases",
            "next_event" to "NEXT EVENT",
            "wiladat" to "WILADAT (BIRTH)",
            "shahadat" to "SHAHADAT",
            "days_remaining" to "Days Remaining",
            "upcoming_anniversary" to "Upcoming Lunar Anniversary Schedule",
            "today" to "Today",
            "tomorrow" to "Tomorrow",
            "in_days" to "In %d days",
            "gregorian_label" to "• Gregorian: ",
            "saved_items" to "Saved Items",
            "my_bookmarks" to "My Bookmarks",
            "daily_quotes" to "Daily Quotes",
            "tab_duas" to "Duas",
            "tab_ziyarats" to "Ziyarats",
            "tab_all" to "All",
            "no_saved_duas" to "No saved Duas or Ziyarats yet.",
            "bookmarked_duas" to "Bookmarked Duas",
            "bookmarked_ziyarats" to "Bookmarked Ziyarats",
            "no_saved_quotes" to "No saved quotes yet.",
            "ahlul_bayt" to "Ahlul Bayt",
            "lunar_date" to "Lunar date:"
        ),
        "ar" to mapOf(
            "action_back" to "رجوع",
            "action_next" to "التالي",
            "action_skip" to "تخطٍ",
            "action_cancel" to "إلغاء",
            "action_save" to "حفظ",
            "onboarding_step" to "الخطوة %1${'$'}d من %2${'$'}d",
            "onboarding_language_title" to "اختر لغتك",
            "onboarding_location_title" to "السماح بالوصول إلى الموقع",
            "onboarding_location_description" to "نستخدم موقعك لتوفير أوقات صلاة دقيقة واتجاه القبلة والمساجد القريبة والمناسبات الإسلامية الإقليمية.",
            "onboarding_grant_location" to "منح إذن الموقع",
            "onboarding_or" to "أو",
            "onboarding_select_city" to "اختر المدينة يدويًا",
            "onboarding_selected" to "المحدد: %1${'$'}s",
            "onboarding_location_required" to "يرجى منح إذن الموقع أو اختيار مدينة للمتابعة.",
            "onboarding_account_title" to "ربط حسابك",
            "onboarding_account_description" to "اربط حساب Google للنسخ الاحتياطي ومزامنة تفضيلاتك. يمكنك تخطي هذه الخطوة.",
            "onboarding_continue_google" to "المتابعة باستخدام Google",
            "onboarding_connected" to "تم الاتصال",
            "onboarding_sign_in_error" to "فشل تسجيل الدخول. يرجى المحاولة مرة أخرى.",
            "onboarding_ready_title" to "كل شيء جاهز! 🎉",
            "onboarding_language" to "اللغة",
            "onboarding_location" to "الموقع",
            "onboarding_email" to "البريد الإلكتروني",
            "onboarding_not_provided" to "غير متوفر",
            "onboarding_get_started" to "ابدأ",
            "ai_companion_title" to "رفيق الذكاء الاصطناعي",
            "ai_greeting" to "السلام عليكم! أنا رفيق ShiaPulse الذكي. يمكنك السؤال عن التعاليم الإسلامية وأهل البيت (ع) والتاريخ والممارسات اليومية. كيف يمكنني مساعدتك؟",
            "ai_disclaimer" to "قد يخطئ الذكاء الاصطناعي. تحقق من القرآن والحديث والأحكام الشرعية من المصادر الموثوقة وعالم شيعي مؤهل.",
            "ai_input_hint" to "اسأل عن شيء...",
            "ai_send" to "إرسال",
            "ai_cooldown" to "يرجى الانتظار بضع ثوان قبل إرسال رسالة أخرى.",
            "ai_no_response" to "لم يتم تلقي رد. يرجى المحاولة مرة أخرى.",
            "ai_unavailable" to "خدمة الذكاء الاصطناعي غير مهيأة في هذا الإصدار.",
            "ai_rate_limit" to "الخدمة مشغولة حاليًا. يرجى المحاولة بعد دقيقة.",
            "ai_http_error" to "تعذر الاتصال بخدمة الذكاء الاصطناعي (الرمز %1${'$'}d). يرجى المحاولة لاحقًا.",
            "ai_network_error" to "تعذر الاتصال. تحقق من اتصال الإنترنت وحاول مرة أخرى.",
            "calendar_history" to "التقويم والتاريخ",
            "calendar_grid_subtitle" to "تقويم تفاعلي ميلادي وهجري",
            "calendar_no_events" to "لا توجد مناسبات شيعية تاريخية أو دولية رئيسية مسجلة في هذا التاريخ.",
            "calendar_shia_events" to "المناسبات الإسلامية الشيعية",
            "calendar_international_events" to "المناسبات الدولية",
            "calendar_previous_month" to "الشهر السابق",
            "calendar_next_month" to "الشهر التالي",
            "calendar_joyous" to "مناسبة مباركة",
            "calendar_solemn" to "إحياء ذكرى",
            "calendar_international" to "دولي",
            "calendar_today" to "اليوم",
            "calendar_hijri_notice" to "التواريخ الهجرية تقديرية حسابياً وقد تختلف يوماً واحداً بحسب رؤية الهلال المحلية.",
            "calendar_search_events" to "البحث في المناسبات والأوصاف",
            "calendar_all_events" to "الكل",
            "calendar_wiladat" to "الولادات",
            "calendar_shahadat" to "الشهادات",
            "calendar_no_matching_events" to "لا توجد مناسبات تطابق هذا البحث والتصفية.",
            "settings_profile_subtitle" to "التفضيلات والمزامنة السحابية",
            "settings_account_cloud" to "الحساب والسحابة",
            "settings_personalization" to "التخصيص",
            "settings_notifications" to "الإشعارات والتذكيرات",
            "settings_support" to "الدعم والمعلومات القانونية",
            "settings_faq" to "الأسئلة الشائعة",
            "settings_sign_in" to "تسجيل الدخول باستخدام Google",
            "settings_sign_out" to "تسجيل الخروج",
            "settings_backup" to "نسخ احتياطي الآن",
            "settings_restore" to "استعادة الآن",
            "settings_interface_language" to "لغة الواجهة",
            "settings_theme" to "سمة ألوان التطبيق",
            "settings_dark" to "داكن",
            "settings_light" to "فاتح",
            "settings_system" to "النظام",
            "settings_share" to "مشاركة التطبيق",

            "slogan" to "منصة رفيق الشيعة المتميزة",
            "time_format" to "استخدم نظام 24 ساعة",
            "verse_text" to "“وَمَا تَوْفِيقِي إِلَّا بِاللَّهِ”",
            "verse_source" to "سورة هود، الآية ٨٨",
            "guidance_title" to "إرشاد أهل البيت (ع)",
            "ask_gemini" to "اسأل جيمني عن موضوع",
            "imsak" to "الإمساك",
            "fajr" to "الفجر",
            "sunrise" to "الشروق",
            "dhuhr" to "الظهر",
            "asr" to "العصر",
            "sunset" to "الغروب",
            "maghrib" to "المغرب",
            "isha" to "العشاء",
            "midnight" to "منتصف الليل (نصف شب)",
            "quran" to "القرآن الكريم",
            "mafatih" to "مفاتيح الجنان",
            "ahkam" to "خمس وأحكام",
            "meet" to "قبلة ومساجد",
            "calendar" to "تقويم قمري",
            "hussainiyah" to "الحسينية",
            "library" to "مكتبة شيعية",
            "tasbeeh" to "مسبحة الذكر",
            "tracker" to "متابع الصلوات",
            "exact_location" to "تفاصيل التوقيت الشرعي شيعي",
            "exact_location_sub" to "يبدأ الفجر عند غسق 16°، وتعتمد صلاة العصر على قواعد الظل الفقهية، والمغرب عند زوال الحمرة الشرقية (بعد 4 درجات من الغروب).",
            "settings" to "الإعدادات والحساب",
            "language" to "تغيير اللغة",
            "adhan" to "تنبيهات الأذان الشرعي",
            "notifications_enabled" to "صوت الأذان مفعل",
            "notifications_disabled" to "صوت الأذان معطل",
            "share" to "مشاركة التطبيق",
            "faq" to "الأسئلة الشائعة والشرعية",
            "back" to "رجوع",
            "log_in_google" to "تسجيل الدخول بجوجل",
            "log_out" to "تسجيل الخروج",
            "google_logged_in" to "تم تسجيل الدخول بجوجل بأمان",
            "sim_login" to "جاري فحص الجلسة عبر مصلحة الحسابات...",
            "show_guidance_startup" to "إظهار إرشاد أهل البيت عند بدء التشغيل",
            "show_guidance_startup_sub" to "إظهار أو إخفاء نافذة إرشاد أهل البيت (ع) التوجيهية عند فتح التطبيق.",
            "guidance_language" to "لغة الإرشاد المفضلة",
            "guidance_language_sub" to "اختر ما إذا كانت حكمة أهل البيت (ع) تظهر بالإنجليزية أو الأوردو افتراضيًا.",
            "masoomeen_title" to "١٤ المعصومين (عليهم السلام)",
            "masoomeen_sub" to "اضغط لاستكشاف السيرة العطرة لأهل البيت (ع)",
            "born" to "الولادة",
            "martyred" to "الشهادة",
            "burial" to "المرقد الشريف",
            "close" to "إغلاق",

            // Legacy/Modern Buttons UI Localizer
            "tile_dua" to "الأدعية والزيارات",
            "tile_calculator" to "حاسبة الخمس",
            "tile_qibla" to "القبلة الشرعية",
            "tile_calendar" to "التقويم الهجري",
            "tile_mosque" to "خريطة المساجد",
            "tile_tasbeeh" to "مسبحة الذكر",
            "tile_tracker" to "متابع الصلوات",
            "tile_aichat" to "الدردشة الذكية",
            "tile_masoomeen" to "١٤ معصوم",
            "tile_names_allah" to "٩٩ اسم الله",
            "tile_names_muhammad" to "أسماء محمدؐ",
            "tile_youtube" to "مقاطع فيديو",
            "tile_ramadan" to "متابع رمضان",

            // Qibla Compass specific
            "qibla_compass" to "بوصلة القبلة",
            "aligned_kaaba" to "متجه نحو الكعبة",
            "aligned_success" to "تم التوجيه بنجاح للصلاة!",
            "slow_rotate" to "يرجى تدوير الجهاز ببطء حتى يتغير لون المؤشر إلى الأخضر",
            "compass_dial" to "قرص البوصلة",
            "map_pin_view" to "عرض الخريطة",
            "to_kaaba" to "نحو الكعبة",
            "geographic_calcs" to "الحسابات الجغرافية",
            "selected_reference" to "المرجع المحدد",
            "kaaba_direction" to "اتجاه الكعبة (زاوية الغدير)",
            "east_north" to "الشرق-الشمال الحقيقي",
            "global_qibla_map" to "خريطة القبلة العالمية",
            "mecca_kaaba" to "مكة (الكعبة)",
            "reference_pin" to "دبوس المرجع",
            "holy_kaaba" to "الكعبة المشرفة",
            "mecca_sa" to "مكة، السعودية",
            "gps_sensor_warning" to "يرجى تشغيل نظام GPS ووضع الجهاز بشكل مستوٍ بعيدًا عن الأجهزة الإلكترونية والشواحن الكهربائية والمغناطیس للحصول على قراءة دقيقة للمستشعر.",

            // Custom Calculators & Khums labels
            "islamic_calculators" to "الحاسبات الشرعية",
            "select_calc_sub" to "يرجى اختيار حاسبة من الأسفل للمتابعة",
            "khums_calc" to "حاسبة خمس",
            "khums_calc_desc" to "احسب ضريبة الخمس السنوية بنسبة 20٪ من الفائض المالي.",
            "zakat_calc" to "حاسبة زكاة المال",
            "zakat_calc_desc" to "حساب الزكاة المستحبة والواجبة على أصولك.",
            "fitrana_calc" to "زكاة الفطرة",
            "fitrana_calc_desc" to "حساب زكاة الفطرة لعائلتك.",
            "musafir_calc" to "حاسبة المسافر الشرعية",
            "musafir_calc_desc" to "حساب مسافة السفر وتقصير الصلوات والإفطار.",
            "inheritance_calc" to "حاسبة تقسيم المواريث",
            "inheritance_calc_desc" to "توزيع المواريث والتركات والوصايا وفق الفقه الخمس.",
            "date_conv_calc" to "محول التقويم الهجري",
            "greg_hijri_desc" to "التحويل المتبادل بين التاريخ الهجري والميلادي العيسوي.",
            "khums_info_text" to "الخمس هو ضريبة عبادية بنسبة 20% على الفاضل من المؤونة السنوية (المتبقي من المدخرات بعد المصاريف السنوية الضرورية). ويجمع في الفقه الجعفري بالتساوي بين سہم الإمام (ع) وسہم السادة الكرام.",
            "annual_income" to "إجمالي الدخل / المدخرات السنوية",
            "allowable_expenses" to "المصاريف السنوية المستثناة",
            "total_surplus" to "إجمالي الفائض المشمول بالخمس",
            "khums_due" to "الخمس الواجب",
            "sehm_imam_label" to "سهم الإمام (ع) (50%)",
            "sehm_sadat_label" to "سهم السادة (50%)",

            // Duas & Ziyarats Screen
            "duas_ziyarats" to "الأدعية والزيارات الشرعية",
            "duas_ziyarats_sub" to "مجموعة مأثورة مع صوت عذب",
            "search_duas" to "ابحث في الأدعية والزيارات الكريمة...",
            "cat_all" to "الكل",
            "cat_famed" to "الأدعية الشهيرة",
            "cat_weekly" to "الأدعية الأسبوعية",
            "cat_daily" to "الأدعية اليومية",
            "cat_ziyarats" to "الزيارات",
            "lunar_phase_tracker" to "متتبع مراحل القمر",
            "masoomeen_remembrance_calendar" to "تقويم المناسبات الإسلامية",
            "lunar_phase_tracker_sub" to "التاريخ الإسلامي ومنازل القمر",
            "next_event" to "المناسبة القادمة",
            "wiladat" to "ولادة",
            "shahadat" to "شهادة",
            "days_remaining" to "أيام متبقية",
            "upcoming_anniversary" to "الجدول القادم للمناسبات القمرية",
            "today" to "اليوم",
            "tomorrow" to "غداً",
            "in_days" to "بعد %d أيام",
            "gregorian_label" to "• الميلادي: ",
            "saved_items" to "العناصر المحفوظة",
            "my_bookmarks" to "إشاراتي المرجعية",
            "daily_quotes" to "اقتباسات يومية",
            "tab_duas" to "أدعية",
            "tab_ziyarats" to "زيارات",
            "tab_all" to "الكل",
            "no_saved_duas" to "لا توجد أدعية أو زيارات محفوظة بعد.",
            "bookmarked_duas" to "الأدعية المحفوظة",
            "bookmarked_ziyarats" to "الزيارات المحفوظة",
            "no_saved_quotes" to "لا توجد اقتباسات محفوظة بعد.",
            "ahlul_bayt" to "أهل البيت",
            "lunar_date" to "التاريخ الهجري:"
        ),
        "ur" to mapOf(
            "action_back" to "واپس",
            "action_next" to "اگلا",
            "action_skip" to "چھوڑ دیں",
            "action_cancel" to "منسوخ کریں",
            "action_save" to "محفوظ کریں",
            "onboarding_step" to "مرحلہ %1${'$'}d از %2${'$'}d",
            "onboarding_language_title" to "اپنی زبان منتخب کریں",
            "onboarding_location_title" to "مقام کی اجازت دیں",
            "onboarding_location_description" to "ہم آپ کے مقام کا استعمال نماز کے درست اوقات، قبلے کی سمت، قریب کی مساجد اور علاقائی اسلامی مناسبتوں کے لیے کرتے ہیں۔",
            "onboarding_grant_location" to "مقام کی اجازت دیں",
            "onboarding_or" to "یا",
            "onboarding_select_city" to "شہر دستی طور پر منتخب کریں",
            "onboarding_selected" to "منتخب شدہ: %1${'$'}s",
            "onboarding_location_required" to "جاری رکھنے کے لیے مقام کی اجازت دیں یا شہر منتخب کریں۔",
            "onboarding_account_title" to "اپنا اکاؤنٹ جوڑیں",
            "onboarding_account_description" to "اپنی ترجیحات کا بیک اپ اور سنک کرنے کے لیے گوگل اکاؤنٹ جوڑیں۔ آپ یہ مرحلہ چھوڑ بھی سکتے ہیں۔",
            "onboarding_continue_google" to "گوگل کے ساتھ جاری رکھیں",
            "onboarding_connected" to "منسلک ہے",
            "onboarding_sign_in_error" to "سائن اِن ناکام رہا۔ براہ کرم دوبارہ کوشش کریں۔",
            "onboarding_ready_title" to "آپ تیار ہیں! 🎉",
            "onboarding_language" to "زبان",
            "onboarding_location" to "مقام",
            "onboarding_email" to "ای میل",
            "onboarding_not_provided" to "فراہم نہیں کیا گیا",
            "onboarding_get_started" to "شروع کریں",
            "ai_companion_title" to "اے آئی رفیق",
            "ai_greeting" to "السلام علیکم! میں آپ کا ShiaPulse اے آئی رفیق ہوں۔ آپ اسلامی تعلیمات، اہل بیتؑ، تاریخ یا روزمرہ اعمال کے بارے میں پوچھ سکتے ہیں۔ میں کیسے مدد کروں؟",
            "ai_disclaimer" to "اے آئی سے غلطی ممکن ہے۔ قرآن، حدیث اور شرعی احکام کو مستند ماخذ اور مستند شیعہ عالم سے ضرور جانچیں۔",
            "ai_input_hint" to "کچھ پوچھیں...",
            "ai_send" to "بھیجیں",
            "ai_cooldown" to "اگلا پیغام بھیجنے سے پہلے چند سیکنڈ انتظار کریں۔",
            "ai_no_response" to "کوئی جواب موصول نہیں ہوا۔ براہ کرم دوبارہ کوشش کریں۔",
            "ai_unavailable" to "اس ورژن میں اے آئی سروس کنفیگر نہیں ہے۔",
            "ai_rate_limit" to "فی الحال سروس مصروف ہے۔ براہ کرم ایک منٹ بعد کوشش کریں۔",
            "ai_http_error" to "اے آئی سروس سے رابطہ نہیں ہو سکا (کوڈ %1${'$'}d)۔ براہ کرم بعد میں کوشش کریں۔",
            "ai_network_error" to "رابطہ ممکن نہیں۔ اپنا انٹرنیٹ کنکشن چیک کر کے دوبارہ کوشش کریں۔",
            "calendar_history" to "کیلنڈر اور تاریخ",
            "calendar_grid_subtitle" to "تفاعلی عیسوی اور ہجری کیلنڈر",
            "calendar_no_events" to "اس تاریخ کے لیے کوئی اہم شیعہ تاریخی یا بین الاقوامی واقعہ درج نہیں ہے۔",
            "calendar_shia_events" to "شیعہ اسلامی مناسبتیں",
            "calendar_international_events" to "بین الاقوامی مناسبتیں",
            "calendar_previous_month" to "پچھلا مہینہ",
            "calendar_next_month" to "اگلا مہینہ",
            "calendar_joyous" to "مسرت کی مناسبت",
            "calendar_solemn" to "سوگواری کی یادگار",
            "calendar_international" to "بین الاقوامی",
            "calendar_today" to "آج",
            "calendar_hijri_notice" to "ہجری تواریخ حسابی تخمینہ ہیں اور مقامی رؤیتِ ہلال کے مطابق ایک دن کا فرق ممکن ہے۔",
            "calendar_search_events" to "مناسبتیں یا تفصیل تلاش کریں",
            "calendar_all_events" to "تمام",
            "calendar_wiladat" to "ولادتیں",
            "calendar_shahadat" to "شہادتیں",
            "calendar_no_matching_events" to "اس تلاش اور فلٹر سے کوئی مناسبت نہیں ملتی۔",
            "settings_profile_subtitle" to "ترجیحات اور کلاؤڈ سنکرونائزیشن",
            "settings_account_cloud" to "اکاؤنٹ اور کلاؤڈ",
            "settings_personalization" to "ذاتی ترتیبات",
            "settings_notifications" to "اطلاعات اور یاد دہانیاں",
            "settings_support" to "مدد اور قانونی معلومات",
            "settings_faq" to "اکثر پوچھے گئے سوالات",
            "settings_sign_in" to "گوگل سے سائن اِن کریں",
            "settings_sign_out" to "سائن آؤٹ",
            "settings_backup" to "ابھی بیک اپ لیں",
            "settings_restore" to "ابھی بحال کریں",
            "settings_interface_language" to "انٹرفیس کی زبان",
            "settings_theme" to "ایپ کے رنگ کی تھیم",
            "settings_dark" to "ڈارک",
            "settings_light" to "لائٹ",
            "settings_system" to "سسٹم",
            "settings_share" to "ایپ شیئر کریں",

            "slogan" to "جامع شیعہ رفیقِ نماز و عبادات",
            "time_format" to "24 گھنٹے کا وقت استعمال کریں",
            "verse_text" to "“اور میری توفیق صرف اللہ کی مدد سے ہے۔”",
            "verse_source" to "سورہ ہود، آیت ۸۸",
            "guidance_title" to "رہنمائی اہلبیت (ع)",
            "ask_gemini" to "جیمنی سے سوال پوچھیں",
            "imsak" to "امساک",
            "fajr" to "فجر",
            "sunrise" to "طلوعِ آفتاب",
            "dhuhr" to "ظہر",
            "asr" to "عصر",
            "sunset" to "غروبِ آفتاب",
            "maghrib" to "مغرب",
            "isha" to "عشاء",
            "midnight" to "نصف شب",
            "quran" to "قرآن مجید",
            "mafatih" to "مفاتیح الجنان",
            "ahkam" to "احکام الشرعیہ / خمس",
            "meet" to "مساجد اور قبلہ نما",
            "calendar" to "ہجری قمری کلینڈر",
            "hussainiyah" to "حسینیہ آڈیو و اذان",
            "library" to "معلومات کتب خانہ",
            "tasbeeh" to "تسبیحات و ذکر",
            "tracker" to "نماز ٹریکر و قضا",
            "exact_location" to "شیعہ فلکیاتی اوقات کی معلومات",
            "exact_location_sub" to "فجر کی نماز کا آغاز سنہری پٹی (16° twilight) سے ہوتا ہے، عصر کے لیے مخصوص شیعہ شیڈو قوانین نافذ ہیں، اور مغرب غروبِ آفتاب کے ٹھیک 4 ڈگری بعد ہوتا ہے۔",
            "settings" to "ترتیبات اور پروفائل",
            "language" to "زبان تبدیل کریں",
            "adhan" to "انداز الرٹس ترتیبات",
            "notifications_enabled" to "اذان مخصوص آڈیو الرٹ فعال ہے",
            "notifications_disabled" to "اذان الرٹ غیر فعال ہے",
            "share" to "ایپ شیئر کریں",
            "faq" to "عام فقہی و تکنیکی سوالات (FAQ)",
            "back" to "واپس جائیں",
            "log_in_google" to "گوگل کے ساتھ لاگ ان کریں",
            "log_out" to "لاگ آؤٹ کریں",
            "google_logged_in" to "گوگل ہولڈر کے طور پر کامیابی سے لاگ ان ہوا",
            "sim_login" to "انکرپٹڈ گوگل کلاؤڈ سیشن لوڈ ہو رہا ہے...",
            "show_guidance_startup" to "اسٹارٹ اپ پر رہنمائی اہلبیت پاپ اپ دکھائیں",
            "show_guidance_startup_sub" to "ایپ شروع ہونے پر رہنمائی اہلبیت (ع) کا متبرک پاپ اپ دکھانے یا چھپانے کے لیے منتخب کریں۔",
            "guidance_language" to "رہنمائی اہلبیت کی زبان",
            "guidance_language_sub" to "منتخب کریں کہ روزانہ رہنمائی اہلبیت (ع) کلام اردو میں ظاہر ہو یا انگریزی میں۔",
            "masoomeen_title" to "14 معصومین علیہم السلام",
            "masoomeen_sub" to "اہلبیت علیہم السلام کی پاکیزہ زندگی اور سوانح حیات جاننے کے لیے معلومات",
            "born" to "ولادت",
            "martyred" to "شہادت",
            "burial" to "مزارِ مبارک",
            "close" to "بند کریں",

            // Legacy/Modern Buttons UI Localizer
            "tile_dua" to "دعا اور زیارات",
            "tile_calculator" to "حساب خمس",
            "tile_qibla" to "قبلہ نما",
            "tile_calendar" to "کلینڈر",
            "tile_mosque" to "مساجد",
            "tile_tasbeeh" to "تسبیح",
            "tile_tracker" to "ٹریکر",
            "tile_aichat" to "اے آئی چیٹ",
            "tile_masoomeen" to "14 معصومین",
            "tile_names_allah" to "اللہ کے 99 نام",
            "tile_names_muhammad" to "اسمائے محمدؐ",
            "tile_youtube" to "ویڈیوز",
            "tile_ramadan" to "شعبان وہ رمضان",

            // Qibla Compass specific
            "qibla_compass" to "قبلہ نما",
            "aligned_kaaba" to "قبلہ کی طرف رخ ہے",
            "aligned_success" to "نماز پڑھنے کے لیے درست رخ منتخب ہوا!",
            "slow_rotate" to "موبائل کو آہستہ سے گھمائیں تاکہ اشارہ سبز رنگ کا ہو جائے",
            "compass_dial" to "قبلہ نما ڈائل",
            "map_pin_view" to "خارطہ قبلہ اثر",
            "to_kaaba" to "متصل کعبہ",
            "geographic_calcs" to "جغرافیائی تفصیلات",
            "selected_reference" to "منتخب مقام",
            "kaaba_direction" to "جغرافیائی سمت (زاویہ غدیر)",
            "east_north" to "حقیقی شمال-مشرق",
            "global_qibla_map" to "عالمی قبلہ نما نقشہ",
            "mecca_kaaba" to "مکہ (کعبہ)",
            "reference_pin" to "مقامِ مرکز",
            "holy_kaaba" to "خانہ کعبہ",
            "mecca_sa" to "مکہ، سعودی عرب",
            "gps_sensor_warning" to "سینسرز کے درست الرٹ کے لیے موبائل کو ہموار سطح پر رکھیں اور برقی مقناطیسی چیزوں جیسے لیپ ٹاپ یا چارجر سے دور رکھیں۔",

            // Custom Calculators & Khums labels
            "islamic_calculators" to "خمس و شرعی حساب",
            "select_calc_sub" to "جاری رکھنے کے لیے کسی ایک شعبہ کا انتخاب کریں",
            "khums_calc" to "خمس کیلکولیٹر",
            "khums_calc_desc" to "اپنے سالانہ اضافی مال و بچت پر 20 فیصد خمس واجب کا حساب لگائیں۔",
            "zakat_calc" to "زکوٰۃ کیلکولیٹر",
            "zakat_calc_desc" to "اپنے سونے اور تجارتی مال پر زکوٰۃ کا حساب لگائیں۔",
            "fitrana_calc" to "فطرانہ کیلکولیٹر",
            "fitrana_calc_desc" to "اپنے خاندان کے لئے فطرانہ کا حساب لگائیں۔",
            "musafir_calc" to "نمازِ مسافر معلومات",
            "musafir_calc_desc" to "شرعی سفر کے فاصلے اور مسافر نماز و روزے کے قوانین جانیں۔",
            "inheritance_calc" to "تقسیمِ میراث حاسب",
            "inheritance_calc_desc" to "شیعہ فقہ جعفریہ کی روشنی میں وراثت کی شرعی تقسیم جانیں۔",
            "date_conv_calc" to "قمری تاریخ متبدل",
            "greg_hijri_desc" to "ہجری قمری اور عیسوی تاریخوں کو تبدیل کریں۔",
            "khums_info_text" to "خمس سالانہ اضافی مال و بچت پر 20٪ واجب ٹیکس ہے۔ شیعہ فقہ میں یہ برابر حصوں میں سہمِ امامؑ اور سہمِ سادات میں تقسیم ہوتا ہے۔",
            "annual_income" to "کل سالانہ آمدنی / بچت",
            "allowable_expenses" to "ضروری سالانہ اخراجات",
            "total_surplus" to "کل اضافی رقم",
            "khums_due" to "خمس واجب",
            "sehm_imam_label" to "سہمِ امام علیہ السلام (50%)",
            "sehm_sadat_label" to "سہمِ سادات (50%)",

            // Duas & Ziyarats Screen
            "duas_ziyarats" to "دعائیں اور زیارات",
            "duas_ziyarats_sub" to "مستند دعائیں اور خوبصورت آڈیو",
            "search_duas" to "مطلوبہ دعا یا زیارت تلاش کریں...",
            "cat_all" to "تمام",
            "cat_famed" to "خاص دعائیں",
            "cat_weekly" to "ہفتہ وار دعائیں",
            "cat_daily" to "روزانہ کی دعائیں",
            "cat_ziyarats" to "زیارتیں",
            "lunar_phase_tracker" to "چاند کے مراحل کا ٹریکر",
            "masoomeen_remembrance_calendar" to "اسلامی تقریبات کا کیلنڈر",
            "lunar_phase_tracker_sub" to "اسلامی تاریخ اور چاند کے مراحل",
            "next_event" to "اگلی برسی",
            "wiladat" to "ولادت مبارک",
            "shahadat" to "شہادت",
            "days_remaining" to "دن باقی",
            "upcoming_anniversary" to "آنے والی قمری تواریخ",
            "today" to "آج",
            "tomorrow" to "کل",
            "in_days" to "%d دنوں میں",
            "gregorian_label" to "• عیسوی: ",
            "saved_items" to "محفوظ شدہ اشیاء",
            "my_bookmarks" to "میری بک مارکس",
            "daily_quotes" to "روزانہ فرامین",
            "tab_duas" to "دعائیں",
            "tab_ziyarats" to "زیارات",
            "tab_all" to "سب",
            "no_saved_duas" to "کوئی بک مارک محفوظ نہیں ہے",
            "bookmarked_duas" to "محفوظ شدہ دعائیں",
            "bookmarked_ziyarats" to "محفوظ شدہ زیارات",
            "no_saved_quotes" to "کوئی محفوظ شدہ کلمہ نہیں ہے",
            "ahlul_bayt" to "اہل بیت",
            "lunar_date" to "قمری:"
        )
    )

    fun get(key: String, lang: String): String {
        val english = translations.getValue("en")
        val dict = translations[lang] ?: english
        return dict[key] ?: english[key] ?: key
    }

    // --- DYNAMIC TRANSLATION HELPERS ---
    
    fun translateMasoomName(name: String, lang: String): String {
        if (lang == "en" || lang.isEmpty()) return name
        val isAr = lang == "ar"
        
        // Since Imam Muhammad ibn Ali is repeated for Baqir and Jawad, 
        // we'll try to use a specific string replacement approach or just use the map. 
        // The display will be fine being Baqir/Jawad generic or we can do a fallback.
        // Actually, in the UI it's not a big issue if Jawad is just Imam Muhammad ibn Ali
        
        val words = mapOf(
            "Prophet Muhammad (saws)" to if(isAr) "النبي محمد (ص)" else "حضرت محمد (ص)",
            "Imam Ali ibn Abi Talib (as)" to if(isAr) "الإمام علي بن أبي طالب (ع)" else "امام علی ابن ابی طالب (ع)",
            "Lady Fatima al-Zahra (sa)" to if(isAr) "السيدة فاطمة الزهراء (س)" else "بی بی فاطمتہ الزہرا (س)",
            "Imam Hasan al-Mujtaba (as)" to if(isAr) "الإمام الحسن المجتبى (ع)" else "امام حسن مجتبیٰ (ع)",
            "Imam Hussain ibn Ali (as)" to if(isAr) "الإمام الحسين بن علي (ع)" else "امام حسین ابن علی (ع)",
            "Imam Ali ibn al-Hussain (as)" to if(isAr) "الإمام علي بن الحسين (ع)" else "امام علی زین العابدین (ع)",
            "Imam Muhammad ibn Ali (as)" to if(isAr) "الإمام محمد بن علي (ع)" else "امام محمد بن علی (ع)",
            "Imam Ja'far ibn Muhammad (as)" to if(isAr) "الإمام جعفر بن محمد الصادق (ع)" else "امام جعفر صادق (ع)",
            "Imam Musa ibn Ja'far (as)" to if(isAr) "الإمام موسى بن جعفر الكاظم (ع)" else "امام موسی کاظم (ع)",
            "Imam Ali ibn Musa (as)" to if(isAr) "الإمام علي بن موسى الرضا (ع)" else "امام علی رضا (ع)",
            "Imam Ali ibn Muhammad (as)" to if(isAr) "الإمام علي بن محمد الهادي (ع)" else "امام علی نقی الہادی (ع)",
            "Imam Hasan ibn Ali (as)" to if(isAr) "الإمام الحسن بن علي العسكري (ع)" else "امام حسن عسکری (ع)",
            "Imam Muhammad ibn al-Hasan (ajtf)" to if(isAr) "الإمام محمد بن الحسن المهدي (عج)" else "امام مہدی (عج)"
        )
        
        return words[name] ?: name
    }

    fun formatTimeUI(timeStr: String, use24h: Boolean): String {
        if (use24h) return timeStr
        try {
            val parts = timeStr.split(":")
            if (parts.size != 2) return timeStr
            val hstr = parts[0].trim()
            val mstr = parts[1].trim().take(2)
            val h = hstr.toIntOrNull() ?: return timeStr
            val m = mstr.toIntOrNull() ?: return timeStr
            
            val isPm = h >= 12
            val h12 = if (h % 12 == 0) 12 else h % 12
            val amPm = if (isPm) "PM" else "AM"
            return String.format("%02d:%02d %s", h12, m, amPm)
        } catch (e: Exception) {
            return timeStr
        }
    }

    fun formatGregorianDate(cal: java.util.Calendar, languageCode: String): String {
        return when (languageCode) {
            "ur" -> {
                val month = when (cal.get(java.util.Calendar.MONTH)) {
                    java.util.Calendar.JANUARY -> "جنوری"
                    java.util.Calendar.FEBRUARY -> "فروری"
                    java.util.Calendar.MARCH -> "مارچ"
                    java.util.Calendar.APRIL -> "اپریل"
                    java.util.Calendar.MAY -> "مئی"
                    java.util.Calendar.JUNE -> "جون"
                    java.util.Calendar.JULY -> "جولائی"
                    java.util.Calendar.AUGUST -> "اگست"
                    java.util.Calendar.SEPTEMBER -> "ستمبر"
                    java.util.Calendar.OCTOBER -> "اکتوبر"
                    java.util.Calendar.NOVEMBER -> "نومبر"
                    java.util.Calendar.DECEMBER -> "دسمبر"
                    else -> ""
                }
                val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val year = cal.get(java.util.Calendar.YEAR)
                "$day $month $year"
            }
            "ar" -> {
                val month = when (cal.get(java.util.Calendar.MONTH)) {
                    java.util.Calendar.JANUARY -> "يناير"
                    java.util.Calendar.FEBRUARY -> "فبراير"
                    java.util.Calendar.MARCH -> "مارس"
                    java.util.Calendar.APRIL -> "أبريل"
                    java.util.Calendar.MAY -> "مايو"
                    java.util.Calendar.JUNE -> "يونيو"
                    java.util.Calendar.JULY -> "يوليو"
                    java.util.Calendar.AUGUST -> "أغسطس"
                    java.util.Calendar.SEPTEMBER -> "سبتمبر"
                    java.util.Calendar.OCTOBER -> "أكتوبر"
                    java.util.Calendar.NOVEMBER -> "نوفمبر"
                    java.util.Calendar.DECEMBER -> "ديسمبر"
                    else -> ""
                }
                val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val year = cal.get(java.util.Calendar.YEAR)
                "$day $month $year"
            }
            else -> {
                val sdf = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.ENGLISH)
                sdf.format(cal.time)
            }
        }
    }

    fun formatHijriDate(hijriDate: HijriCalendarHelper.HijriDate, languageCode: String): String {
        return when (languageCode) {
            "ur" -> {
                val urMonthName = when (hijriDate.monthName) {
                    "Muharram" -> "محرم"
                    "Safar" -> "صفر"
                    "Rabi' al-Awwal" -> "ربیع الاول"
                    "Rabi' al-Thani" -> "ربیع الثانی"
                    "Jumada al-Awwal" -> "جمادی الاول"
                    "Jumada al-Thani" -> "جمادی الثانی"
                    "Rajab" -> "رجب"
                    "Sha'ban" -> "شعبان"
                    "Ramadan" -> "رمضان"
                    "Shawwal" -> "شوال"
                    "Dhu al-Qa'dah" -> "ذی القعدہ"
                    "Dhu al-Hijjah" -> "ذی الحجہ"
                    else -> hijriDate.monthName
                }
                "${hijriDate.day} $urMonthName ${hijriDate.year}ھ"
            }
            "ar" -> {
                val arMonthName = when (hijriDate.monthName) {
                    "Muharram" -> "محرم"
                    "Safar" -> "صفر"
                    "Rabi' al-Awwal" -> "ربيع الأول"
                    "Rabi' al-Thani" -> "ربيع الثاني"
                    "Jumada al-Awwal" -> "جمادى الأولى"
                    "Jumada al-Thani" -> "جمادى الآخرة"
                    "Rajab" -> "رجب"
                    "Sha'ban" -> "شعبان"
                    "Ramadan" -> "رمضان"
                    "Shawwal" -> "شوال"
                    "Dhu al-Qa'dah" -> "ذو القعدة"
                    "Dhu al-Hijjah" -> "ذو الحجة"
                    else -> hijriDate.monthName
                }
                "${hijriDate.day} $arMonthName ${hijriDate.year} هـ"
            }
            else -> {
                "${hijriDate.day} ${hijriDate.monthName} ${hijriDate.year} AH"
            }
        }
    }

    fun translateHijriDateString(dateStr: String, languageCode: String): String {
        if (languageCode != "ur" && languageCode != "ar") return dateStr

        if (dateStr.contains("/")) {
            return dateStr.split("/").joinToString(" / ") { segment ->
                translateHijriDateString(segment.trim(), languageCode)
            }
        }

        var result = dateStr
        if (languageCode == "ur") {
            result = result
                .replace("Muharram", "محرم")
                .replace("Safar", "صفر")
                .replace("Rabi' al-Awwal", "ربیع الاول")
                .replace("Rabi' al-Thani", "ربیع الثانی")
                .replace("Jumada al-Awwal", "جمادی الاول")
                .replace("Jumada al-Thani", "جمادی الثانی")
                .replace("Rajab", "رجب")
                .replace("Sha'ban", "شعبان")
                .replace("Ramadan", "رمضان")
                .replace("Shawwal", "شوال")
                .replace("Dhu al-Qa'dah", "ذی القعدہ")
                .replace("Dhu al-Hijjah", "ذی الحجہ")
                .replace("(Ashura)", "(عاشورہ)")
                .replace("(Arba'een)", "(اربعین)")
        } else if (languageCode == "ar") {
            result = result
                .replace("Muharram", "محرم")
                .replace("Safar", "صفر")
                .replace("Rabi' al-Awwal", "ربيع الأول")
                .replace("Rabi' al-Thani", "ربيع الثاني")
                .replace("Jumada al-Awwal", "جمادى الأولى")
                .replace("Jumada al-Thani", "جمادى الآخرة")
                .replace("Rajab", "رجب")
                .replace("Sha'ban", "شعبان")
                .replace("Ramadan", "رمضان")
                .replace("Shawwal", "شوال")
                .replace("Dhu al-Qa'dah", "ذو القعدة")
                .replace("Dhu al-Hijjah", "ذو الحجة")
                .replace("(Ashura)", "(عاشوراء)")
                .replace("(Arba'een)", "(الأربعين)")
        }
        return result
    }
}
