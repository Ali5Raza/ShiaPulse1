import json
import os

os.makedirs('app/src/main/assets/Supplications/Aamal', exist_ok=True)

aamal_jumuah = {
  "id": "aamal_jumuah",
  "title": "Aamal of Friday (Jumu'ah)",
  "titleUr": "جمعہ کے اعمال",
  "titleAr": "أعمال يوم الجمعة",
  "category": "Weekly",
  "categoryUr": "ہفتہ وار",
  "categoryAr": "أسبوعي",
  "description": "Recommended acts to be performed on the day of Friday.",
  "descriptionUr": "جمعہ کے دن انجام دیے جانے والے مستحب اعمال۔",
  "descriptionAr": "الأعمال المستحبة في يوم الجمعة.",
  "relevance": "Every Friday",
  "acts": [
    {
      "actNumber": 1,
      "title": "Ghusl of Friday",
      "titleUr": "غسل جمعہ",
      "titleAr": "غسل الجمعة",
      "description": "It is highly recommended to perform Ghusl on Friday. The time is from dawn to noon.",
      "descriptionUr": "جمعہ کا غسل کرنا بہت مستحب ہے۔ اس کا وقت فجر سے ظہر تک ہے۔",
      "descriptionAr": "يستحب استحباباً مؤكداً غسل الجمعة ووقته من طلوع الفجر إلى الزوال."
    },
    {
      "actNumber": 2,
      "title": "Clipping Nails and Trimming Moustache",
      "titleUr": "ناخن اور مونچھیں تراشنا",
      "titleAr": "تقليم الأظافر وأخذ الشارب",
      "description": "It increases sustenance and prevents many diseases.",
      "descriptionUr": "یہ رزق میں اضافہ کرتا ہے اور کئی بیماریوں سے بچاتا ہے۔",
      "descriptionAr": "يزيد في الرزق ويأمن من أمراض كثيرة."
    },
    {
      "actNumber": 3,
      "title": "Applying Perfume (Itr)",
      "titleUr": "خوشبو (عطر) لگانا",
      "titleAr": "التطيب",
      "description": "It is recommended to wear clean clothes and apply perfume.",
      "descriptionUr": "صاف کپڑے پہننا اور خوشبو لگانا مستحب ہے۔",
      "descriptionAr": "يستحب لبس الثياب النظيفة والتطيب."
    },
    {
      "actNumber": 4,
      "title": "Reciting Salawat",
      "titleUr": "صلوات پڑھنا",
      "titleAr": "الصلاة على محمد وآل محمد",
      "description": "Send blessings on Prophet Muhammad (PBUH) and his progeny as much as possible, at least 100 times.",
      "descriptionUr": "نبی اکرم (ص) اور ان کی آل پر زیادہ سے زیادہ صلوات بھیجیں، کم از کم 100 مرتبہ۔",
      "descriptionAr": "الإكثار من الصلاة على محمد وآله، وأقله مائة مرة.",
      "arabicRecitation": "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَآلِ مُحَمَّدٍ وَعَجِّلْ فَرَجَهُمْ",
      "translation": "O Allah, send blessings on Muhammad and the family of Muhammad and hasten their relief.",
      "urduTranslation": "اے اللہ، محمد اور آل محمد پر درود بھیج اور ان کے ظہور میں جلدی فرما۔",
      "transliteration": "Allahumma salli 'ala Muhammadin wa Aali Muhammad wa 'ajjil farajahum"
    },
    {
      "actNumber": 5,
      "title": "Ziyarat of Imam al-Mahdi (ATFS)",
      "titleUr": "امام مہدی (عج) کی زیارت",
      "titleAr": "زيارة الإمام المهدي (عج)",
      "description": "Friday is the day of Imam al-Mahdi (ATFS), and it is the day his reappearance is expected. Recite his Ziyarat.",
      "descriptionUr": "جمعہ کا دن امام مہدی (عج) کا دن ہے اور اسی دن ان کے ظہور کی توقع ہے۔ ان کی زیارت پڑھیں۔",
      "descriptionAr": "يوم الجمعة هو يوم الإمام المهدي (عج) وفيه يتوقع ظهوره. تقرأ زيارته."
    }
  ]
}

aamal_shabe_qadr = {
  "id": "aamal_shabe_qadr",
  "title": "Aamal of Shab-e-Qadr (Laylatul Qadr)",
  "titleUr": "شب قدر کے اعمال",
  "titleAr": "أعمال ليلة القدر",
  "category": "Ramadan",
  "categoryUr": "رمضان",
  "categoryAr": "رمضان",
  "description": "Common acts for the 19th, 21st, and 23rd nights of Ramadan.",
  "descriptionUr": "رمضان کی 19ویں، 21ویں اور 23ویں راتوں کے مشترکہ اعمال۔",
  "descriptionAr": "الأعمال المشتركة لليالي 19 و 21 و 23 من شهر رمضان.",
  "relevance": "19th, 21st, and 23rd of Ramadan",
  "acts": [
    {
      "actNumber": 1,
      "title": "Ghusl",
      "titleUr": "غسل",
      "titleAr": "الغسل",
      "description": "Perform Ghusl, preferably at sunset so one can pray Maghrib and Isha with Ghusl.",
      "descriptionUr": "غسل کریں، بہتر ہے کہ غروب آفتاب کے وقت کریں تاکہ مغرب اور عشاء کی نماز غسل کے ساتھ پڑھی جا سکے۔",
      "descriptionAr": "الغسل، والأفضل أن يكون عند غروب الشمس ليصلي العشائين به."
    },
    {
      "actNumber": 2,
      "title": "Two Rak'at Prayer",
      "titleUr": "دو رکعت نماز",
      "titleAr": "صلاة ركعتين",
      "description": "Pray a 2 Rak'at prayer. In each Rak'at, after Surah al-Fatihah, recite Surah al-Ikhlas 7 times. After finishing the prayer, recite 'Astaghfirullaaha wa atoobu ilayh' 70 times.",
      "descriptionUr": "دو رکعت نماز پڑھیں۔ ہر رکعت میں سورہ فاتحہ کے بعد 7 مرتبہ سورہ اخلاص پڑھیں۔ نماز ختم کرنے کے بعد 70 مرتبہ 'استغفراللہ ربی و اتوب الیہ' پڑھیں۔",
      "descriptionAr": "صلاة ركعتين، يقرأ في كل ركعة بعد الحمد التوحيد سبع مرات، ويقول بعد الفراغ سبعين مرة: أستغفر الله وأتوب إليه."
    },
    {
      "actNumber": 3,
      "title": "Opening the Quran",
      "titleUr": "قرآن مجید کھول کر دعا",
      "titleAr": "فتح المصحف",
      "description": "Open the Holy Quran, place it in front of you, and recite the specific Dua for it.",
      "descriptionUr": "قرآن مجید کھولیں، اسے اپنے سامنے رکھیں اور اس کے لیے مخصوص دعا پڑھیں۔",
      "descriptionAr": "تأخذ المصحف وتنشره وتضعه بين يديك وتقول الدعاء المخصوص.",
      "arabicRecitation": "اللَّهُمَّ إِنِّي أَسْأَلُكَ بِكِتَابِكَ الْمُنْزَلِ وَمَا فِيهِ...",
      "translation": "O Allah, I ask You by Your revealed Book and what is in it...",
      "urduTranslation": "اے اللہ! میں تجھ سے سوال کرتا ہوں تیری نازل کردہ کتاب کے واسطے سے اور جو کچھ اس میں ہے...",
      "transliteration": "Allahumma inni as'aluka bikitabikal-munzali wa ma fihi..."
    },
    {
      "actNumber": 4,
      "title": "Placing the Quran on the Head",
      "titleUr": "قرآن سر پر رکھنا",
      "titleAr": "وضع المصحف على الرأس",
      "description": "Place the Holy Quran on your head and recite the prescribed Dua, invoking Allah by the names of the 14 Infallibles (AS).",
      "descriptionUr": "قرآن مجید اپنے سر پر رکھیں اور 14 معصومین (ع) کے ناموں کا واسطہ دے کر مخصوص دعا پڑھیں۔",
      "descriptionAr": "تأخذ المصحف وتدعه على رأسك وتقول الدعاء المخصوص بالتوسل بأسماء المعصومين (ع)."
    },
    {
      "actNumber": 5,
      "title": "Ziyarat of Imam Husayn (AS)",
      "titleUr": "امام حسین (ع) کی زیارت",
      "titleAr": "زيارة الإمام الحسين (ع)",
      "description": "Recite the specific Ziyarat of Imam Husayn (AS) for Laylatul Qadr.",
      "descriptionUr": "شب قدر کے لیے امام حسین (ع) کی مخصوص زیارت پڑھیں۔",
      "descriptionAr": "زيارة الإمام الحسين (ع) المخصوصة بليلة القدر."
    },
    {
      "actNumber": 6,
      "title": "Supplication of Jawshan Kabir",
      "titleUr": "دعا جوشن کبیر",
      "titleAr": "دعاء الجوشن الكبير",
      "description": "It is highly recommended to recite Dua Jawshan Kabir on these nights.",
      "descriptionUr": "ان راتوں میں دعا جوشن کبیر کا پڑھنا بہت مستحب ہے۔",
      "descriptionAr": "يستحب قراءة دعاء الجوشن الكبير في هذه الليالي."
    }
  ]
}

aamal_ashura = {
  "id": "aamal_ashura",
  "title": "Aamal of Ashura (10th Muharram)",
  "titleUr": "عاشورہ کے اعمال (10 محرم)",
  "titleAr": "أعمال يوم عاشوراء",
  "category": "Muharram",
  "categoryUr": "محرم",
  "categoryAr": "محرم",
  "description": "Acts and rituals for the day of Ashura, the day of mourning for Imam Husayn (AS).",
  "descriptionUr": "عاشورہ کے دن کے اعمال و رسومات، امام حسین (ع) کا سوگ منانے کا دن۔",
  "descriptionAr": "أعمال ومراسم يوم عاشوراء، يوم العزاء للإمام الحسين (ع).",
  "relevance": "10th of Muharram",
  "acts": [
    {
      "actNumber": 1,
      "title": "Mourning and Expressing Grief",
      "titleUr": "عزاداری اور غم کا اظہار",
      "titleAr": "إقامة العزاء وإظهار الحزن",
      "description": "It is the day of immense grief for the Ahl al-Bayt. One should abandon worldly tasks, mourn, and weep for Imam Husayn (AS).",
      "descriptionUr": "یہ اہل بیت کے لیے شدید غم کا دن ہے۔ انسان کو دنیاوی کام ترک کر کے امام حسین (ع) پر رونا اور عزاداری کرنی چاہیے۔",
      "descriptionAr": "إنه يوم حزن شديد لأهل البيت. ينبغي ترك الأعمال الدنيوية وإقامة العزاء والبكاء على الإمام الحسين (ع)."
    },
    {
      "actNumber": 2,
      "title": "Condoling One Another",
      "titleUr": "ایک دوسرے کو تعزیت پیش کرنا",
      "titleAr": "تعزية المؤمنين لبعضهم البعض",
      "description": "Condole other believers upon meeting them.",
      "descriptionUr": "ملاقات کے وقت دوسرے مومنین کو تعزیت پیش کریں۔",
      "descriptionAr": "أن يعزي المؤمنون بعضهم بعضاً عند اللقاء.",
      "arabicRecitation": "عَظَّمَ اللّٰهُ أُجُورَنَا بِمُصَابِنَا بِالْحُسَيْنِ عَلَيْهِ السَّلَامُ",
      "translation": "May Allah make our reward great for our grief for Husayn (AS).",
      "urduTranslation": "اللہ ہمارے اجر کو زیادہ کرے، اس مصیبت پر جو ہمیں حسین (ع) کی وجہ سے پہنچی ہے۔",
      "transliteration": "A'zzamallahu ujoorana bimusabina bil-Husayni 'alayhis-salam."
    },
    {
      "actNumber": 3,
      "title": "Ziyarat Ashura",
      "titleUr": "زیارت عاشورہ",
      "titleAr": "زيارة عاشوراء",
      "description": "Recite Ziyarat Ashura with its 100 curses and 100 salutations.",
      "descriptionUr": "زیارت عاشورہ کو اس کی 100 لعنتوں اور 100 سلاموں کے ساتھ پڑھیں۔",
      "descriptionAr": "قراءة زيارة عاشوراء المشهورة مع اللعن والسلام مائة مرة."
    },
    {
      "actNumber": 4,
      "title": "Fasting (Imsak) until Asr",
      "titleUr": "عصر تک فاقہ (امساک)",
      "titleAr": "الإمساك إلى العصر",
      "description": "It is disliked to fast completely, but one should abstain from food and drink until the time of Asr, and then break it with simple food.",
      "descriptionUr": "مکمل روزہ رکھنا مکروہ ہے، لیکن عصر کے وقت تک کھانے پینے سے پرہیز کرنا چاہیے اور پھر سادہ کھانے سے اسے توڑنا چاہیے۔",
      "descriptionAr": "يكره الصوم الكامل، ولكن ينبغي الإمساك عن الطعام والشراب إلى وقت العصر ثم الإفطار بطعام بسيط كطعام أهل المصائب."
    },
    {
      "actNumber": 5,
      "title": "Reciting Surah al-Ikhlas 1000 times",
      "titleUr": "1000 مرتبہ سورہ اخلاص پڑھنا",
      "titleAr": "قراءة سورة التوحيد ألف مرة",
      "description": "Reciting Surah al-Ikhlas 1000 times carries great reward.",
      "descriptionUr": "1000 مرتبہ سورہ اخلاص پڑھنے کا بہت بڑا ثواب ہے۔",
      "descriptionAr": "قراءة سورة التوحيد ألف مرة لها ثواب عظيم."
    }
  ]
}

def write_json(name, data):
    with open(f"app/src/main/assets/Supplications/Aamal/{name}.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

write_json("aamal_jumuah", aamal_jumuah)
write_json("aamal_shabe_qadr", aamal_shabe_qadr)
write_json("aamal_ashura", aamal_ashura)

