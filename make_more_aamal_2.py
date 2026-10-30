import json
import os

os.makedirs('app/src/main/assets/Supplications/Aamal', exist_ok=True)

def write_json(name, data):
    with open(f"app/src/main/assets/Supplications/Aamal/{name}.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

aamal_umme_dawood = {
  "id": "aamal_umme_dawood",
  "title": "Aamal-e-Umme Dawood",
  "titleUr": "اعمال ام داؤد",
  "titleAr": "أعمال أم داوود",
  "category": "Rajab",
  "categoryUr": "رجب",
  "categoryAr": "رجب",
  "description": "Very significant Aamal for fulfilling desires and relieving difficulties, usually performed on the 15th of Rajab.",
  "descriptionUr": "حاجات کی تکمیل اور مشکلات دور کرنے کے لیے انتہائی اہم اعمال، جو عام طور پر 15 رجب کو کیے جاتے ہیں۔",
  "descriptionAr": "أعمال عظيمة الشأن لقضاء الحوائج وكشف الكرب، وتؤدى في النصف من رجب.",
  "relevance": "15th of Rajab",
  "acts": [
    {
      "actNumber": 1,
      "title": "Fasting",
      "titleUr": "روزہ",
      "titleAr": "الصيام",
      "description": "Fast on the 13th, 14th, and 15th of Rajab (Ayyam al-Bid).",
      "descriptionUr": "13، 14 اور 15 رجب کو روزہ رکھیں۔",
      "descriptionAr": "صيام الأيام البيض: الثالث عشر والرابع عشر والخامس عشر من رجب."
    },
    {
      "actNumber": 2,
      "title": "Ghusl",
      "titleUr": "غسل",
      "titleAr": "الغسل",
      "description": "Perform Ghusl (ritual bath) around noon on the 15th of Rajab.",
      "descriptionUr": "15 رجب کو دوپہر کے قریب غسل کریں۔",
      "descriptionAr": "الاغتسال عند زوال الشمس يوم الخامس عشر من رجب."
    },
    {
      "actNumber": 3,
      "title": "Recitation of Quran",
      "titleUr": "تلاوت قرآن",
      "titleAr": "تلاوة القرآن",
      "description": "After Zuhr and Asr prayers, recite Surah al-Fatihah, Surah Ikhlas, Ayatul Kursi, and several other specific Surahs like Surah Yaseen, Surah al-Rahman, Surah al-Mulk, etc.",
      "descriptionUr": "ظہر اور عصر کی نماز کے بعد سورہ فاتحہ، سورہ اخلاص، آیت الکرسی، اور دیگر مخصوص سورتیں جیسے سورہ یٰسین، سورہ رحمٰن، اور سورہ ملک وغیرہ پڑھیں۔",
      "descriptionAr": "بعد صلاة الظهر والعصر تقرأ سورة الحمد والتوحيد وآية الكرسي وعدة سور أخرى مثل يس، والرحمن، والملك، وغيرها."
    },
    {
      "actNumber": 4,
      "title": "Dua Umme Dawood",
      "titleUr": "دعا ام داؤد",
      "titleAr": "دعاء أم داوود",
      "description": "Recite the specific Dua known as Dua Umme Dawood after the Quranic recitations.",
      "descriptionUr": "قرآن کی تلاوت کے بعد دعا ام داؤد پڑھیں۔",
      "descriptionAr": "قراءة دعاء أم داوود المعروف بعد الانتهاء من تلاوة السور.",
      "arabicRecitation": "صَدَقَ اللّٰهُ الْعَظِيمُ الَّذِي لَا إِلٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...",
      "translation": "Allah the Almighty has spoken the truth, there is no god but He, the Ever-Living, the Self-Subsisting...",
      "urduTranslation": "عظیم اللہ نے سچ فرمایا ہے، جس کے سوا کوئی معبود نہیں، وہ ہمیشہ زندہ اور قائم رہنے والا ہے...",
      "transliteration": "Sadaqallahul-'aliyyul-'azeemul-ladhi la ilaha illa huwal-hayyul-qayyum..."
    }
  ]
}

namaz_wahshat = {
  "id": "namaz_wahshat",
  "title": "Namaz-e-Wahshat-e-Qabr",
  "titleUr": "نماز وحشت قبر",
  "titleAr": "صلاة ليلة الدفن",
  "category": "Occasions",
  "categoryUr": "مواقع",
  "categoryAr": "المناسبات",
  "description": "A prayer offered for a deceased person on the first night of their burial to ease the difficulties of the grave.",
  "descriptionUr": "میت کو دفن کرنے کی پہلی رات اس کی قبر کی وحشت دور کرنے کے لیے پڑھی جانے والی نماز۔",
  "descriptionAr": "صلاة تُصلى للميت في أول ليلة من دفنه لرفع وحشة القبر عنه.",
  "relevance": "First night of burial",
  "acts": [
    {
      "actNumber": 1,
      "title": "First Rak'at",
      "titleUr": "پہلی رکعت",
      "titleAr": "الركعة الأولى",
      "description": "In the first Rak'at, after Surah al-Fatihah, recite Ayatul Kursi once.",
      "descriptionUr": "پہلی رکعت میں سورہ فاتحہ کے بعد ایک بار آیت الکرسی پڑھیں۔",
      "descriptionAr": "يقرأ في الركعة الأولى سورة الفاتحة مرة وآية الكرسي مرة."
    },
    {
      "actNumber": 2,
      "title": "Second Rak'at",
      "titleUr": "دوسری رکعت",
      "titleAr": "الركعة الثانية",
      "description": "In the second Rak'at, after Surah al-Fatihah, recite Surah al-Qadr 10 times.",
      "descriptionUr": "دوسری رکعت میں سورہ فاتحہ کے بعد 10 بار سورہ قدر پڑھیں۔",
      "descriptionAr": "يقرأ في الركعة الثانية سورة الفاتحة مرة وسورة القدر عشر مرات."
    },
    {
      "actNumber": 3,
      "title": "Dua After Prayer",
      "titleUr": "نماز کے بعد دعا",
      "titleAr": "الدعاء بعد الصلاة",
      "description": "After finishing the prayer, send blessings on Muhammad (PBUH) and his progeny, and send the reward of the prayer to the grave of the deceased.",
      "descriptionUr": "نماز کے بعد محمد اور آل محمد پر درود بھیجیں، اور نماز کا ثواب میت کی قبر کو ہدیہ کریں۔",
      "descriptionAr": "بعد الصلاة يسلم ويقول: اللّهم صل على محمد وآل محمد وابعث ثوابها إلى قبر فلان (ويذكر اسم الميت).",
      "arabicRecitation": "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَآلِ مُحَمَّدٍ وَابْعَثْ ثَوَابَهَا إِلَى قَبْرِ فُلَانٍ",
      "translation": "O Allah, send blessings on Muhammad and the progeny of Muhammad, and send its reward to the grave of [Name of deceased].",
      "urduTranslation": "اے اللہ، محمد اور آل محمد پر درود نازل فرما اور اس کا ثواب فلاں (میت کا نام) کی قبر میں بھیج دے۔",
      "transliteration": "Allahumma salli 'ala Muhammadin wa Aali Muhammad wab'ath thawabaha ila qabri [Name of deceased]."
    }
  ]
}

aamal_zilhajj = {
  "id": "aamal_zilhajj",
  "title": "Aamal of Dhu al-Hijjah",
  "titleUr": "ماہ ذوالحجہ کے اعمال",
  "titleAr": "أعمال شهر ذي الحجة",
  "category": "Monthly",
  "categoryUr": "ماہانہ",
  "categoryAr": "شهري",
  "description": "Highly recommended acts for the first ten days of Dhu al-Hijjah.",
  "descriptionUr": "ماہ ذوالحجہ کے پہلے دس دنوں کے انتہائی مستحب اعمال۔",
  "descriptionAr": "الأعمال المستحبة في العشر الأوائل من شهر ذي الحجة.",
  "relevance": "First 10 days of Dhu al-Hijjah",
  "acts": [
    {
      "actNumber": 1,
      "title": "Fasting",
      "titleUr": "روزہ رکھنا",
      "titleAr": "الصيام",
      "description": "Fasting in the first nine days of Dhu al-Hijjah carries the reward of fasting for a lifetime.",
      "descriptionUr": "ذوالحجہ کے پہلے نو دنوں میں روزہ رکھنے کا ثواب زندگی بھر کے روزوں کے برابر ہے۔",
      "descriptionAr": "صيام الأيام التسعة الأولى من شهر ذي الحجة يعادل صيام العمر كله."
    },
    {
      "actNumber": 2,
      "title": "Prayer Between Maghrib and Isha",
      "titleUr": "مغرب اور عشاء کے درمیان نماز",
      "titleAr": "الصلاة بين العشائين",
      "description": "During the first ten nights, perform a 2 Rak'at prayer between Maghrib and Isha. In each Rak'at, after Surah al-Fatihah and Surah al-Ikhlas, recite verse 142 of Surah al-A'raf.",
      "descriptionUr": "پہلی دس راتوں میں مغرب اور عشاء کے درمیان دو رکعت نماز پڑھیں۔ ہر رکعت میں سورہ فاتحہ اور سورہ اخلاص کے بعد سورہ اعراف کی آیت 142 پڑھیں۔",
      "descriptionAr": "أن يصلي بين فريضتي المغرب والعشاء في كل ليلة من ليالي العشر الأوائل ركعتين يقرأ في كل ركعة الفاتحة والتوحيد وهذه الآية:",
      "arabicRecitation": "وَوَاعَدْنَا مُوسَىٰ ثَلَاثِينَ لَيْلَةً وَأَتْمَمْنَاهَا بِعَشْرٍ فَتَمَّ مِيقَاتُ رَبِّهِ أَرْبَعِينَ لَيْلَةً...",
      "translation": "And We made an appointment with Moses for thirty nights and perfected them by [the addition of] ten; so the term of his Lord was completed as forty nights...",
      "urduTranslation": "اور ہم نے موسیٰ سے تیس راتوں کا وعدہ کیا اور دس کے ذریعے سے انہیں پورا کیا، پس ان کے رب کا مقررہ وقت چالیس راتوں میں مکمل ہوا...",
      "transliteration": "Wa wa'adna musa thalathina laylatan wa atmamnaha bi-'ashrin fatamma miqatu rabbihi arba'ina laylatan..."
    },
    {
      "actNumber": 3,
      "title": "Reciting Tahlil",
      "titleUr": "تہلیل پڑھنا",
      "titleAr": "التهليل",
      "description": "Recite the special Tahlil (La ilaha illallah...) taught by Imam Ali (AS) ten times daily.",
      "descriptionUr": "امام علی (ع) کی سکھائی ہوئی مخصوص تہلیل روزانہ دس مرتبہ پڑھیں۔",
      "descriptionAr": "قراءة التهليلات المروية عن أمير المؤمنين (ع) في كل يوم من هذه العشر عشر مرات.",
      "arabicRecitation": "لَا إِلٰهَ إِلَّا اللّٰهُ عَدَدَ اللَّيَالِي وَالدُّهُورِ...",
      "translation": "There is no god but Allah, corresponding to the number of nights and eras...",
      "urduTranslation": "اللہ کے سوا کوئی معبود نہیں، راتوں اور زمانوں کی تعداد کے برابر...",
      "transliteration": "La ilaha illallahu 'adadal-layali wad-duhoor..."
    }
  ]
}

write_json("aamal_umme_dawood", aamal_umme_dawood)
write_json("namaz_wahshat", namaz_wahshat)
write_json("aamal_zilhajj", aamal_zilhajj)

