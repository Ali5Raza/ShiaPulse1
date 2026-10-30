package com.example.api

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object UrduTranslations {
    private var cachedQuotes: List<ShiaQuote>? = null
    private var cachedAyats: List<ShiaQuote>? = null

    private val moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private fun loadJsonList(context: Context, filename: String): List<ShiaQuote>? {
        return try {
            val jsonStr = context.assets.open(filename).bufferedReader().use { it.readText() }
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ShiaQuoteJson::class.java)
            val adapter = moshi.adapter<List<ShiaQuoteJson>>(listType)
            val parsedList = adapter.fromJson(jsonStr)
            parsedList?.map { ShiaQuote(arabic = it.arabic, english = it.english, source = it.source, urdu = it.urdu) }
        } catch (e: Exception) {
            Log.e("UrduTranslations", "Error loading JSON from Assets: $filename", e)
            null
        }
    }

    private val translationMap = mapOf(
        "I am leaving among you two weighty things" to "میں تمہارے درمیان دو گراں قدر چیزیں چھوڑے جا رہا ہوں: اللہ کی کتاب اور میری عترت یعنی میرے اہلِ بیت۔ یقیناً اگر تم ان دونوں کو مضبوطی سے تھامے رہو گے تو کبھی گمراہ نہ ہو گے۔",
        "Associate with people in such a manner" to "لوگوں کے ساتھ اس طرح ملو اور زندگی بسر کرو کہ اگر تم مر جاؤ تو وہ تم پر روئیں، اور اگر تم زندہ رہو تو وہ تمہاری صحبت کے دلدادہ ہوں۔",
        "Allah made faith a purification for you" to "اللہ تعالیٰ نے ایمان کو تمہارے لیے شرک سے پاکیزگی کا ذریعہ بنایا، اور نماز کو تکبر سے نجات دلانے کے لیے فرض کیا۔",
        "Indeed, the most beautiful of beauties is an excellent character." to "بے شک تمام خوبیوں اور اچھائیوں میں سب سے خوبصورت خوبی اچھے اخلاق ہیں۔",
        "Indeed, I do not see death but as happiness" to "یقیناً میں موت کو سعادت اور ظالموں کے ساتھ زندگی گزارنے کو رنج و ملال (ذلت) کے سوا کچھ نہیں دیکھتا۔",
        "The right of your prayer is that you know" to "تمہاری نماز کا حق یہ ہے کہ تم جانو کہ یہ اللہ کی بارگاہ میں سفر اور حاضری ہے اور تم اس کے حضور کھڑے ہو۔",
        "Our Shia are only those who fear Allah and obey Him." to "ہمارے شیعہ صرف وہی لوگ ہیں جو اللہ سے ڈرتے ہیں اور اس کی مکمل اطاعت کرتے ہیں۔",
        "Our intercession will not reach those who take the prayer lightly." to "ہماری شفاعت اس شخص تک ہرگز نہیں پہنچے گی جو نماز کو ہلکا (معمولی) اور غیر اہم سمجھے۔",
        "He is not of us who does not hold himself to account every day." to "وہ ہم میں سے نہیں ہے جو ہر روز اپنے نفس کا محاسبہ اور حساب کتاب نہ کرے۔",
        "Your assistance to the weak is more meritorious than giving charity." to "کسی کمزور اور ناتواں شخص کی مدد کرنا تمہارے خدا کی راہ میں صدقہ دینے سے بھی زیادہ افضل ہے۔",
        "Trust in Allah is the price of everything" to "اللہ تعالیٰ پر بھروسہ ہر قیمتی چیز کی قیمت ہے اور ہر اس منزل تک پہنچنے کا زینہ ہے جو بلند اور عزیمت والی ہے۔",
        "The world is a market in which some people gain and others lose." to "دنیا ایک ایسا بازار ہے جس میں کچھ لوگوں نے حقیقی منافع کمایا اور دوسرے خود کو سراسر نقصان اور خسارے میں لے گئے۔",
        "There are two qualities such that there is nothing superior to them" to "دو خصلتیں ایسی ہیں جن سے بڑھ کر کوئی چیز بلند نہیں: اللہ تعالیٰ پر ایمان لانا اور اپنے دینی بھائیوں کو نفع پہنچانا۔",
        "Pray more for the acceleration of the reappearance" to "دشمنوں سے نجات اور ظہور کی تعجیل کے لیے کثرت سے دعا کرو، کیونکہ اسی میں تمہاری اپنی کشادگی اور نجات ہے۔",
        "Patience is of two kinds: patience over what pains you, and patience against what you covet." to "صبر کی دو قسمیں ہیں: ناپسندیدہ چیز پر صبر کرنا، اور اپنی نفسانی خواہشات کے خلاف گناہوں سے بچنے پر صبر کرنا۔",
        "When a person prays for their brother in their absence" to "جب کوئی انسان اپنے بھائی کے لیے غائبانہ دعا کرتا ہے تو عرشِ الٰہی سے آواز گونجتی ہے: 'تمہارے لیے اس سے ایک لاکھ گنا زیادہ ہو۔'",
        "The worth of every person is in accordance with what they excel in." to "ہر انسان کی حقیقی قیمت اور قدر اس ہنر، کمال اور علم میں ہے جسے وہ خوبصورتی سے انجام دے سکتا ہے۔",
        "Hearts have been naturally created to love those who act kindly towards them." to "دلوں کو فطرتی طور پر ان لوگوں سے محبت کرنے کے لیے پیدا کیا گیا ہے جو ان کے ساتھ احسان اور حسنِ سلوک کرتے ہیں۔",
        "Whoever restrains their anger from people" to "جو شخص لوگوں سے اپنے غصے کو روکے رکھے گا، اللہ تعالیٰ بروزِ قیامت اس سے اپنے عذاب کو روک لے گا۔",
        "The best of deeds is to perform the prayer at its proper time." to "اعمال میں سب سے زیادہ فضیلت والا اور پسندیدہ ترین عمل نماز کو اس کے اول وقت پر ادا کرنا ہے۔"
    )

    fun getUrdu(quote: ShiaQuote, context: Context? = null): String {
        if (quote.urdu.isNotEmpty()) {
            return quote.urdu
        }

        // Try to query loaded JSON lists first if we have the context
        if (context != null) {
            try {
                if (cachedQuotes == null) {
                    cachedQuotes = com.example.ui.admin.ContentSyncManager.loadContent(context, "quotes.json", "quotes_cached.json")
                }
                if (cachedAyats == null) {
                    cachedAyats = com.example.ui.admin.ContentSyncManager.loadContent(context, "ayats.json", "ayats_cached.json")
                }

                // 1. Search in quotes.json
                val quoteMatch = cachedQuotes?.firstOrNull {
                    it.arabic.trim() == quote.arabic.trim() ||
                    it.english.equals(quote.english, ignoreCase = true) ||
                    (quote.english.isNotEmpty() && it.english.contains(quote.english, ignoreCase = true)) ||
                    (it.english.isNotEmpty() && quote.english.contains(it.english, ignoreCase = true))
                }
                if (quoteMatch != null && quoteMatch.urdu.isNotEmpty()) {
                    return quoteMatch.urdu
                }

                // 2. Search in ayats.json
                val ayatMatch = cachedAyats?.firstOrNull {
                    it.arabic.trim() == quote.arabic.trim() ||
                    it.english.equals(quote.english, ignoreCase = true) ||
                    (quote.english.isNotEmpty() && it.english.contains(quote.english, ignoreCase = true)) ||
                    (it.english.isNotEmpty() && quote.english.contains(it.english, ignoreCase = true))
                }
                if (ayatMatch != null && ayatMatch.urdu.isNotEmpty()) {
                    return ayatMatch.urdu
                }
            } catch (e: Exception) {
                Log.e("UrduTranslations", "Failed to resolve from asset JSON, falling back", e)
            }
        }

        for ((englishKey, urduText) in translationMap) {
            if (quote.english.contains(englishKey, ignoreCase = true) || englishKey.contains(quote.english, ignoreCase = true)) {
                return urduText
            }
        }
        
        // Search in LocalQuotes for populated Urdu text
        val localQuoteMatch = LocalQuotes.quotes.firstOrNull {
            it.arabic.trim() == quote.arabic.trim() ||
            it.english.equals(quote.english, ignoreCase = true) ||
            (quote.english.isNotEmpty() && it.english.contains(quote.english, ignoreCase = true)) ||
            (it.english.isNotEmpty() && quote.english.contains(it.english, ignoreCase = true))
        }
        if (localQuoteMatch != null && localQuoteMatch.urdu.isNotEmpty()) {
            return localQuoteMatch.urdu
        }

        // Search in LocalAyats for populated Urdu text
        val localAyatMatch = LocalAyats.ayats.firstOrNull {
            it.arabic.trim() == quote.arabic.trim() ||
            it.english.equals(quote.english, ignoreCase = true) ||
            (quote.english.isNotEmpty() && it.english.contains(quote.english, ignoreCase = true)) ||
            (it.english.isNotEmpty() && quote.english.contains(it.english, ignoreCase = true))
        }
        if (localAyatMatch != null && localAyatMatch.urdu.isNotEmpty()) {
            return localAyatMatch.urdu
        }

        return "اہلِ بیت (ع) فرماتے ہیں: ہمیشہ مشکلات میں صبر کرو، اللہ کو یاد رکھو اور نماز کو قائم کرو، بے شک اللہ تعالی صبر کرنے والوں کے ساتھ ہے۔"
    }
}
