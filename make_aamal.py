import json

aamal_ashura = {
  "id": "aamal_ashura",
  "title": "Aamal of Ashura",
  "category": "Muharram",
  "description": "The recommended acts for the Day of Ashura.",
  "relevance": "Recited on the 10th of Muharram.",
  "audioUrl": "",
  "lines": [
    {
      "arabic": "اَلسَّلامُ عَلَيْكَ يا اَبا عَبْدِاللهِ",
      "translation": "Peace be upon you, O Abu Abdullah.",
      "transliteration": "As-salamu alayka ya Aba Abdillah.",
      "urduTranslation": "سلام ہو آپ پر اے ابا عبداللہ۔",
      "farsiTranslation": "سلام بر تو ای ابا عبدالله."
    },
    {
      "arabic": "لَعَنَ اللهُ قاتِلِيكَ",
      "translation": "May Allah curse those who killed you.",
      "transliteration": "La'anallahu qatilika.",
      "urduTranslation": "اللہ آپ کے قاتلوں پر لعنت کرے۔",
      "farsiTranslation": "خدا لعنت کند قاتلان تو را."
    }
  ]
}

aamal_shab_e_qadr = {
  "id": "aamal_shab_e_qadr",
  "title": "Aamal of Laylatul Qadr",
  "category": "Ramadan",
  "description": "Common acts for the nights of Qadr.",
  "relevance": "Recited on the 19th, 21st, and 23rd of Ramadan.",
  "audioUrl": "",
  "lines": [
    {
      "arabic": "بِكَ يا اَللهُ",
      "translation": "By You, O Allah.",
      "transliteration": "Bika ya Allah.",
      "urduTranslation": "تیرے واسطے سے اے اللہ۔",
      "farsiTranslation": "به تو ای خدا."
    },
    {
      "arabic": "بِمُحَمَّد",
      "translation": "By Muhammad.",
      "transliteration": "Bi Muhammadin.",
      "urduTranslation": "محمد کے واسطے سے۔",
      "farsiTranslation": "به محمد."
    }
  ]
}

with open("app/src/main/assets/Supplications/Aamal/aamal_ashura.json", "w", encoding="utf-8") as f:
    json.dump(aamal_ashura, f, ensure_ascii=False, indent=2)

with open("app/src/main/assets/Supplications/Aamal/aamal_shab_e_qadr.json", "w", encoding="utf-8") as f:
    json.dump(aamal_shab_e_qadr, f, ensure_ascii=False, indent=2)

