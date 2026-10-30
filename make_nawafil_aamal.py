import json
import os

os.makedirs('app/src/main/assets/Supplications/Aamal', exist_ok=True)

def write_json(name, data):
    with open(f"app/src/main/assets/Supplications/Aamal/{name}.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

aamal_ayat = {
  "id": "aamal_ayat",
  "title": "Salat al-Ayat (Signs)",
  "titleUr": "نماز آیات",
  "titleAr": "صلاة الآيات",
  "category": "Occasions",
  "categoryUr": "مواقع",
  "categoryAr": "المناسبات",
  "description": "Obligatory prayer performed during solar/lunar eclipses, earthquakes, severe thunderstorms, or any natural event that causes widespread fear.",
  "descriptionUr": "یہ نماز سورج یا چاند گرہن، زلزلہ، شدید گرج چمک، طوفان، یا کسی ایسی قدرتی آفت کے وقت واجب ہوتی ہے جس سے لوگوں میں خوف پھیل جائے۔",
  "descriptionAr": "صلاة واجبة عند كسوف الشمس، خسوف القمر، الزلزلة، والآيات المخوفة كالرعد والبرق الشديدين والعواصف.",
  "relevance": "Eclipses, Earthquakes, Thunderstorms",
  "acts": [
    {
      "actNumber": 1,
      "title": "Method of Salat al-Ayat",
      "titleUr": "نماز آیات کا طریقہ",
      "titleAr": "كيفية صلاة الآيات",
      "description": "It consists of 2 Rak'ats, and there are 5 Ruku's in each Rak'at. In each Rak'at, recite Surah al-Fatihah followed by another Surah (or divide a Surah into 5 parts, one before each Ruku').",
      "descriptionUr": "یہ دو رکعت پر مشتمل ہے، اور ہر رکعت میں 5 رکوع ہوتے ہیں۔ ہر رکعت میں سورہ فاتحہ کے بعد ایک اور سورت پڑھیں (یا ایک سورت کو 5 حصوں میں تقسیم کر کے ہر رکوع سے پہلے پڑھیں)۔",
      "descriptionAr": "وهي ركعتان، في كل ركعة خمسة ركوعات. يقرأ الفاتحة وسورة (أو يوزع سورة على الركوعات الخمسة)."
    },
    {
      "actNumber": 2,
      "title": "Qunut",
      "titleUr": "قنوت",
      "titleAr": "القنوت",
      "description": "It is recommended to recite Qunut before the 2nd, 4th, 6th, 8th, and 10th Ruku', but reciting one Qunut before the 10th Ruku' is sufficient.",
      "descriptionUr": "دوسرے، چوتھے، چھٹے، آٹھویں اور دسویں رکوع سے پہلے قنوت پڑھنا مستحب ہے، لیکن دسویں رکوع سے پہلے ایک قنوت بھی کافی ہے۔",
      "descriptionAr": "يستحب القنوت قبل الركوع الثاني والرابع والسادس والثامن والعاشر، ويكفي قنوت واحد قبل الركوع العاشر."
    }
  ]
}

aamal_rain = {
  "id": "aamal_rain",
  "title": "Supplication at the Time of Rain",
  "titleUr": "بارش کے وقت کی دعا",
  "titleAr": "دعاء عند نزول المطر",
  "category": "Occasions",
  "categoryUr": "مواقع",
  "categoryAr": "المناسبات",
  "description": "Recommended prayers and supplications to recite when it rains, seeking blessings and protection.",
  "descriptionUr": "بارش کے وقت پڑھی جانے والی مستحب دعائیں، رحمت اور حفاظت کے حصول کے لیے۔",
  "descriptionAr": "الأدعية المستحبة عند نزول المطر لطلب البركة والحفظ.",
  "relevance": "During Rain",
  "acts": [
    {
      "actNumber": 1,
      "title": "Dua for Rain",
      "titleUr": "بارش کی دعا",
      "titleAr": "دعاء المطر",
      "description": "Recite this short supplication when it starts to rain.",
      "descriptionUr": "بارش شروع ہوتے وقت یہ مختصر دعا پڑھیں۔",
      "descriptionAr": "يقرأ هذا الدعاء القصير عند بدء المطر.",
      "arabicRecitation": "اللَّهُمَّ صَيِّباً نَافِعاً",
      "translation": "O Allah, make it a beneficial rain.",
      "urduTranslation": "اے اللہ، اسے نفع بخش بارش بنا۔",
      "transliteration": "Allahumma sayyiban nafi'an"
    },
    {
      "actNumber": 2,
      "title": "Thanksgiving after Rain",
      "titleUr": "بارش کے بعد شکر",
      "titleAr": "الشكر بعد المطر",
      "description": "After the rain, say thanks to Allah for His bounty.",
      "descriptionUr": "بارش کے بعد اللہ کی اس نعمت پر شکر ادا کریں۔",
      "descriptionAr": "بعد المطر اشكر الله على نعمته.",
      "arabicRecitation": "مُطِرْنَا بِفَضْلِ اللَّهِ وَرَحْمَتِهِ",
      "translation": "We have been given rain by the grace and mercy of Allah.",
      "urduTranslation": "ہمیں اللہ کے فضل اور اس کی رحمت سے بارش ملی۔",
      "transliteration": "Mutirna bifadhlillahi wa rahmatihi"
    }
  ]
}

aamal_rajab = {
  "id": "aamal_rajab",
  "title": "Aamal of Rajab",
  "titleUr": "ماہ رجب کے اعمال",
  "titleAr": "أعمال شهر رجب",
  "category": "Monthly",
  "categoryUr": "ماہانہ",
  "categoryAr": "شهري",
  "description": "Recommended daily acts for the holy month of Rajab.",
  "descriptionUr": "ماہ رجب کے مقدس مہینے کے لیے روزانہ کے مستحب اعمال۔",
  "descriptionAr": "الأعمال اليومية المستحبة في شهر رجب الأصب.",
  "relevance": "Month of Rajab",
  "acts": [
    {
      "actNumber": 1,
      "title": "Daily Supplication",
      "titleUr": "روزانہ کی دعا",
      "titleAr": "الدعاء اليومي",
      "description": "Recite this dua daily after obligatory prayers in Rajab.",
      "descriptionUr": "رجب میں ہر واجب نماز کے بعد یہ دعا روزانہ پڑھیں۔",
      "descriptionAr": "قراءة هذا الدعاء يومياً بعد الصلوات الواجبة في رجب.",
      "arabicRecitation": "يَا مَنْ أَرْجُوهُ لِكُلِّ خَيْرٍ، وَآمَنُ سَخَطَهُ عِنْدَ كُلِّ شَرٍّ...",
      "translation": "O He from whom I hope for all good, and from whose anger I feel safe at every evil...",
      "urduTranslation": "اے وہ جس سے میں ہر بھلائی کی امید رکھتا ہوں اور جس کے غضب سے ہر برائی کے وقت محفوظ رہتا ہوں...",
      "transliteration": "Ya man arjuhu likulli khayr, wa amanu sakhatahu 'inda kulli sharr..."
    },
    {
      "actNumber": 2,
      "title": "Fasting",
      "titleUr": "روزہ رکھنا",
      "titleAr": "الصيام",
      "description": "Fasting even for one day in Rajab carries immense rewards and closes the doors of Hell.",
      "descriptionUr": "رجب میں ایک دن کا روزہ رکھنا بھی بہت بڑا ثواب رکھتا ہے اور جہنم کے دروازے بند کر دیتا ہے۔",
      "descriptionAr": "صوم يوم واحد من رجب يوجب رضوان الله الأكبر، ويغلق باباً من أبواب النار."
    },
    {
      "actNumber": 3,
      "title": "Istighfar",
      "titleUr": "استغفار",
      "titleAr": "الاستغفار",
      "description": "Say 'Astaghfirullah wa as'aluhut tawbah' 70 times in the morning and 70 times in the evening.",
      "descriptionUr": "صبح اور شام 70 مرتبہ 'استغفراللہ و اسئلہ التوبۃ' پڑھیں۔",
      "descriptionAr": "أن يقول في كل صباح 70 مرة وفي كل مساء 70 مرة: أستغفر الله وأسأله التوبة."
    }
  ]
}

aamal_shaban = {
  "id": "aamal_shaban",
  "title": "Aamal of Sha'ban",
  "titleUr": "ماہ شعبان کے اعمال",
  "titleAr": "أعمال شهر شعبان",
  "category": "Monthly",
  "categoryUr": "ماہانہ",
  "categoryAr": "شهري",
  "description": "General recommended acts for the month of Sha'ban.",
  "descriptionUr": "ماہ شعبان کے لیے عمومی مستحب اعمال۔",
  "descriptionAr": "الأعمال العامة المستحبة في شهر شعبان.",
  "relevance": "Month of Sha'ban",
  "acts": [
    {
      "actNumber": 1,
      "title": "Salawat",
      "titleUr": "صلوات",
      "titleAr": "الصلوات",
      "description": "Reciting Salawat is the best deed in this month.",
      "descriptionUr": "اس مہینے میں صلوات پڑھنا بہترین عمل ہے۔",
      "descriptionAr": "أفضل الأعمال في هذا الشهر الإكثار من الصلاة على محمد وآل محمد."
    },
    {
      "actNumber": 2,
      "title": "Daily Istighfar",
      "titleUr": "روزانہ استغفار",
      "titleAr": "الاستغفار اليومي",
      "description": "Say 70 times daily: 'Astaghfirullahal-ladhi la ilaha illa huwar-Rahmanur-Rahim...'",
      "descriptionUr": "روزانہ 70 مرتبہ کہیں: 'استغفراللہ الذی لا الہ الا ھو الرحمن الرحیم...'",
      "descriptionAr": "أن يقول في كل يوم سبعين مرة: أستغفر الله الذي لا إله إلا هو الرحمن الرحيم الحي القيوم وأتوب إليه.",
      "arabicRecitation": "أَسْتَغْفِرُ اللّٰهَ الَّذِي لَا إِلٰهَ إِلَّا هُوَ الرَّحْمٰنُ الرَّحِيمُ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ",
      "translation": "I seek forgiveness of Allah, there is no god but He, the Beneficent, the Merciful, the Ever-Living, the Self-Subsisting, and I turn to Him in repentance.",
      "urduTranslation": "میں اللہ سے بخشش مانگتا ہوں جس کے سوا کوئی معبود نہیں، جو رحمن، رحیم، ہمیشہ زندہ اور قائم رہنے والا ہے اور اسی کی طرف توبہ کرتا ہوں۔",
      "transliteration": "Astaghfirullahal-ladhi la ilaha illa huwar-Rahmanur-Rahimul-Hayyul-Qayyum wa atubu ilayh."
    },
    {
      "actNumber": 3,
      "title": "Salawat of Sha'ban (Shabaniyah)",
      "titleUr": "صلوات شعبانیہ",
      "titleAr": "الصلوات الشعبانية",
      "description": "Recite the special Salawat taught by Imam Sajjad (AS) every day at noon.",
      "descriptionUr": "ہر روز دوپہر کے وقت امام سجاد (ع) کی سکھائی ہوئی مخصوص صلوات پڑھیں۔",
      "descriptionAr": "قراءة الصلوات الشعبانية المروية عن الإمام السجاد (ع) عند زوال كل يوم."
    }
  ]
}

aamal_eid_ghadir = {
  "id": "aamal_eid_ghadir",
  "title": "Aamal of Eid al-Ghadir",
  "titleUr": "عید غدیر کے اعمال",
  "titleAr": "أعمال عيد الغدير",
  "category": "Special Days",
  "categoryUr": "خاص دن",
  "categoryAr": "أيام خاصة",
  "description": "Rituals for the greatest Eid (Eid al-Akbar), celebrating the appointment of Imam Ali (AS).",
  "descriptionUr": "عید غدیر (سب سے بڑی عید) کی رسومات، امام علی (ع) کی ولایت کا جشن۔",
  "descriptionAr": "أعمال عيد الله الأكبر عيد الغدير الأغر، يوم تنصيب أمير المؤمنين (ع).",
  "relevance": "18th of Dhu al-Hijjah",
  "acts": [
    {
      "actNumber": 1,
      "title": "Fasting",
      "titleUr": "روزہ رکھنا",
      "titleAr": "الصيام",
      "description": "Fasting on this day is equivalent to fasting for the entire lifespan of the world.",
      "descriptionUr": "اس دن روزہ رکھنا دنیا کی پوری زندگی کے روزوں کے برابر ہے۔",
      "descriptionAr": "صيام هذا اليوم يعدل صيام عمر الدنيا، ويعدل مائة حجة ومائة عمرة مبرورات متقبلات."
    },
    {
      "actNumber": 2,
      "title": "Ghusl",
      "titleUr": "غسل",
      "titleAr": "الغسل",
      "description": "Perform Ghusl, preferably before noon.",
      "descriptionUr": "غسل کریں، بہتر ہے کہ دوپہر سے پہلے کیا جائے۔",
      "descriptionAr": "الغسل، والأفضل أن يكون قبيل الزوال."
    },
    {
      "actNumber": 3,
      "title": "Ziyarat of Aminullah",
      "titleUr": "زیارت امین اللہ",
      "titleAr": "زيارة أمين الله",
      "description": "Recite the Ziyarat of Aminullah for Imam Ali (AS).",
      "descriptionUr": "امام علی (ع) کے لیے زیارت امین اللہ پڑھیں۔",
      "descriptionAr": "زيارة أمير المؤمنين (ع) بزيارة أمين الله."
    },
    {
      "actNumber": 4,
      "title": "Saying Congratulations",
      "titleUr": "مبارکباد دینا",
      "titleAr": "التهنئة",
      "description": "Congratulate believers when you meet them.",
      "descriptionUr": "مومنین سے ملاقات کے وقت انہیں مبارکباد دیں۔",
      "descriptionAr": "تهنئة المؤمنين عند اللقاء.",
      "arabicRecitation": "الْحَمْدُ لِلّٰهِ الَّذِي جَعَلَنَا مِنَ الْمُتَمَسِّكِينَ بِوِلَايَةِ أَمِيرِ الْمُؤْمِنِينَ وَالْأَئِمَّةِ عَلَيْهِمُ السَّلَامُ",
      "translation": "Praise be to Allah, who has made us among those who hold fast to the Wilayah of the Commander of the Faithful and the Imams, peace be upon them.",
      "urduTranslation": "تمام تعریفیں اللہ کے لیے ہیں جس نے ہمیں امیر المومنین اور آئمہ (علیہم السلام) کی ولایت سے تمسک کرنے والوں میں سے قرار دیا۔",
      "transliteration": "Alhamdu lillahil-ladhi ja'alana minal-mutamassikina bi-wilayati Amiril-mu'minina wal-a'immah 'alayhimus-salam."
    }
  ]
}

aamal_eid_fitr = {
  "id": "aamal_eid_fitr",
  "title": "Aamal of Eid al-Fitr",
  "titleUr": "عید الفطر کے اعمال",
  "titleAr": "أعمال عيد الفطر",
  "category": "Special Days",
  "categoryUr": "خاص دن",
  "categoryAr": "أيام خاصة",
  "description": "Acts recommended for the Day of Eid al-Fitr.",
  "descriptionUr": "عید الفطر کے دن مستحب اعمال۔",
  "descriptionAr": "الأعمال المستحبة في يوم عيد الفطر المبارك.",
  "relevance": "1st of Shawwal",
  "acts": [
    {
      "actNumber": 1,
      "title": "Zakat al-Fitrah",
      "titleUr": "زکوۃ الفطرہ",
      "titleAr": "زكاة الفطرة",
      "description": "Pay Zakat al-Fitrah before the Eid prayer.",
      "descriptionUr": "نماز عید سے پہلے زکوۃ الفطرہ ادا کریں۔",
      "descriptionAr": "إخراج زكاة الفطرة قبل صلاة العيد."
    },
    {
      "actNumber": 2,
      "title": "Ghusl",
      "titleUr": "غسل",
      "titleAr": "الغسل",
      "description": "Take a bath (Ghusl) on Eid day.",
      "descriptionUr": "عید کے دن غسل کریں۔",
      "descriptionAr": "الاغتسال في يوم العيد."
    },
    {
      "actNumber": 3,
      "title": "Eid Prayer",
      "titleUr": "نماز عید",
      "titleAr": "صلاة العيد",
      "description": "Perform the Eid prayer in congregation.",
      "descriptionUr": "نماز عید باجماعت ادا کریں۔",
      "descriptionAr": "أداء صلاة العيد في جماعة."
    }
  ]
}

aamal_ramadan = {
  "id": "aamal_ramadan",
  "title": "Aamal of Ramadan (General)",
  "titleUr": "ماہ رمضان کے اعمال",
  "titleAr": "أعمال شهر رمضان",
  "category": "Monthly",
  "categoryUr": "ماہانہ",
  "categoryAr": "شهري",
  "description": "Daily recommended acts for the Holy month of Ramadan.",
  "descriptionUr": "ماہ رمضان کے مقدس مہینے کے لیے روزانہ کے مستحب اعمال۔",
  "descriptionAr": "الأعمال اليومية المستحبة في شهر رمضان المبارك.",
  "relevance": "Month of Ramadan",
  "acts": [
    {
      "actNumber": 1,
      "title": "Recitation of the Quran",
      "titleUr": "تلاوت قرآن",
      "titleAr": "تلاوة القرآن",
      "description": "Ramadan is the spring of the Quran. Recite at least one Juz' daily.",
      "descriptionUr": "رمضان قرآن کی بہار ہے۔ روزانہ کم از کم ایک پارہ تلاوت کریں۔",
      "descriptionAr": "رمضان ربيع القرآن، يستحب تلاوة جزء واحد يومياً على الأقل."
    },
    {
      "actNumber": 2,
      "title": "Dua Iftitah",
      "titleUr": "دعا افتتاح",
      "titleAr": "دعاء الافتتاح",
      "description": "Recite Dua Iftitah every night of Ramadan.",
      "descriptionUr": "رمضان کی ہر رات دعا افتتاح پڑھیں۔",
      "descriptionAr": "قراءة دعاء الافتتاح في كل ليلة من ليالي شهر رمضان."
    },
    {
      "actNumber": 3,
      "title": "Dua Ya Aliyyu Ya Azeem",
      "titleUr": "دعا یا علی یا عظیم",
      "titleAr": "دعاء يا علي يا عظيم",
      "description": "Recite after every obligatory prayer.",
      "descriptionUr": "ہر واجب نماز کے بعد پڑھیں۔",
      "descriptionAr": "يُقرأ بعد كل فريضة.",
      "arabicRecitation": "يَا عَلِيُّ يَا عَظِيمُ، يَا غَفُورُ يَا رَحِيمُ...",
      "translation": "O High, O Great, O Forgiving, O Merciful...",
      "urduTranslation": "اے بلند، اے عظیم، اے بخشنے والے، اے رحم کرنے والے...",
      "transliteration": "Ya Aliyyu ya Azeem, ya Ghafooru ya Raheem..."
    }
  ]
}

write_json("aamal_ayat", aamal_ayat)
write_json("aamal_rain", aamal_rain)
write_json("aamal_rajab", aamal_rajab)
write_json("aamal_shaban", aamal_shaban)
write_json("aamal_ramadan", aamal_ramadan)
write_json("aamal_eid_ghadir", aamal_eid_ghadir)
write_json("aamal_eid_fitr", aamal_eid_fitr)
