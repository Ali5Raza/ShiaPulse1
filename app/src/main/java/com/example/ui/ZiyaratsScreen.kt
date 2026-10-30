package com.example.ui
import com.example.data.MyCollection
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import com.example.data.MasoomeenData
import com.example.data.ZiyaratItem
import com.example.ui.theme.AmiriFont
import com.example.ui.theme.NotoNaskhArabicFont
import com.example.ui.theme.NotoNastaliqUrduFont

data class ZiyaratLocation(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val nameUr: String,
    val lat: Double,
    val lon: Double,
    val descEn: String,
    val descAr: String,
    val descUr: String,
    val cityEn: String,
    val cityAr: String,
    val cityUr: String,
    val tipsEn: String,
    val tipsAr: String,
    val tipsUr: String,
    val masoomeenIndices: List<Int> = emptyList()
)

val ziyaratLocations = listOf(
    ZiyaratLocation(
        id = "medina_prophet",
        nameEn = "Al-Masjid an-Nabawi (Holy Prophet s.a.w.w.)",
        nameAr = "المسجد النبوي الشريف (الرسول الأكرم ص)",
        nameUr = "مسجد نبوی شریف (خاتم المرسلین حضرت محمد مصطفیٰ صلی اللہ علیہ وآلہ وسلم)",
        lat = 24.4672,
        lon = 39.6111,
        cityEn = "Medina, Saudi Arabia",
        cityAr = "المدينة المنورة، السعودية",
        cityUr = "مدینہ منورہ، سعودی عرب",
        descEn = "The magnificent Prophet's Mosque containing the holy shrine of the Savior of Humanity, Prophet Muhammad (s.a.w.w.), the 1st of the 14 Infallibles (Masoomeen). It is the second holiest site in Islam.",
        descAr = "المسجد النبوي الشريف الذي يضم المرقد الطاهر لخاتم الأنبياء والمرسلين نبي الرحمة محمد (ص)، أول المعصومين الأربعة عشر.",
        descUr = "مسجدِ نبوی شریف جہاں تمام انسانیت کے نجات دہندہ، خاتم النبیین حضرت محمد مصطفیٰ صلی اللہ علیہ وآلہ وسلم کا روضہ اطہر موجود ہے، جو کہ چودہ معصومینؑ میں سب سے پہلے ہیں۔",
        tipsEn = "Recommended attire: Plain, formal conservative clothing. Women must cover their heads completely. Best time to visit: Late evening (after Isha) or third portion of the night. Local etiquette: Enter with extreme humility (Khushu), keep your voice lowered, recite Ziyarat with peace, and avoid taking large cameras.",
        tipsAr = "الزي الموصى به: الملابس البسيطة والساترة جداً مع تغطية الرأس بالكامل للنساء. أفضل وقت للزيارة: في أواخر الليل أو بعد صلاة العشاء. الآداب المحلية: الدخول بخشوع تام، خفض الصوت عند قبر النبي الكريم، وتجنب التصوير الصاخب.",
        tipsUr = "تجویز کردہ لباس: نہایت سادہ، صاف اور باوقار شرعی لباس پہنیں۔ زیارت کا بہترین وقت: نمازِ عشا کے بعد یا رات کا آخری پہر۔ مقامی آداب: انتہائی عاجزی اور خشوع کے ساتھ داخل ہوں، آواز کو دھیما رکھیں، اور بغیر کسی شور کے صلوٰۃ و سلام پیش کریں۔",
        masoomeenIndices = listOf(1)
    ),
    ZiyaratLocation(
        id = "medina_zahra",
        nameEn = "Lady Fatima al-Zahra (s.a.) — Medina Ziyarat",
        nameAr = "مقام زيارة السيدة فاطمة الزهراء (ع) في المدينة",
        nameUr = "حضرت فاطمہ زہرا سلام اللہ علیہا — مدینہ میں مقامِ زیارت",
        lat = 24.4676,
        lon = 39.6115,
        cityEn = "Medina, Saudi Arabia",
        cityAr = "المدينة المنورة، السعودية",
        cityUr = "مدینہ منورہ، سعودی عرب",
        descEn = "A spiritual ziyarat point in Medina for the beloved daughter of Prophet Muhammad (s.a.w.w.), wife of Imam Ali (a.s.), and mother of the Infallible Imams. Her exact grave is unknown; reports mention her house/al-Rawdah and Jannat al-Baqi as possible locations. This map pin does not identify a confirmed grave.",
        descAr = "مقام روحي للزيارة في المدينة المنورة لبضعة الرسول الأكرم، السيدة فاطمة الزهراء (ع)، زوجة أمير المؤمنين وأم الأئمة الأطهار. قبرها الشريف مجهول المكان، وقد وردت روايات عن احتمال دفنها في بيتها أو الروضة الشريفة أو جنة البقيع. لا يحدد هذا المؤشر على الخريطة قبراً مؤكداً.",
        descUr = "سرورِ کائناتؐ کی لختِ جگر، امیر المومنینؑ کی شریکِ حیات اور معصوم ائمہؑ کی والدہ حضرت فاطمہ زہرا سلام اللہ علیہا کے لیے مدینہ میں روحانی زیارت کا مقام۔ آپ کی قبرِ اقدس کی درست جگہ معلوم نہیں؛ روایات میں آپ کے گھر/ریاض الجنہ اور جنت البقیع جیسے امکانی مقامات مذکور ہیں۔ نقشے کا یہ نشان کسی تصدیق شدہ قبر کی نشاندہی نہیں کرتا۔",
        tipsEn = "Recommended attire: Strictly modest conservative attire. Best time to visit: During times of prayer when Masjid an-Nabawi is open. Local etiquette: Focus on deep internal reflection, pray inside Riyadhul Jannah (between the Prophet's house and his pulpit), and recite the unique Ziyarat of Lady Zahra (s.a.) quietly.",
        tipsAr = "الزي الموصى به: الملابس المحتشمة والساترة جداً. أفضل وقت للزيارة: أوقات الصلوات المكتوبة. الآداب المحلية: استحضار عظمة الصديقة الطاهرة، والدعاء بخشوع في الروضة الشريفة (ما بين بيت النبي ومنبره) وقراءة زيارتها المأثورة بهدوء.",
        tipsUr = "تجویز کردہ لباس: مکمل باوقار اور شرعی پردہ لازمی ہے۔ زیارت کا بہترین وقت: نمازوں کے اوقات کے دوران۔ مقامی آداب: خاتونِ جنت کی عظمت کا دل میں احترام قائم کریں، ریاض الجنہ (مبارک منبر اور کاشانہ رسولؐ کے درمیان) میں نماز ادا کریں اور نہایت راز داری و سکون سے زیارت پڑھیں۔",
        masoomeenIndices = listOf(3)
    ),
    ZiyaratLocation(
        id = "najaf",
        nameEn = "Shrine of Imam Ali (a.s.)",
        nameAr = "مرقد الإمام علي بن أبي طالب (ع)",
        nameUr = "روضہ مبارک حضرت امام علی علیہ السلام",
        lat = 31.99611,
        lon = 44.31417,
        cityEn = "Najaf, Iraq",
        cityAr = "النجف الأشرف، العراق",
        cityUr = "نجف اشرف، عراق",
        descEn = "The holy sanctuary of the Commander of the Faithful, Imam Ali ibn Abi Talib (a.s.), the cousin and son-in-law of Prophet Muhammad (s.a.w.w.) and the 3rd Masoom (1st Imam). It is a major center of Shia scholarship and spirituality.",
        descAr = "المرقد المقدس لأمير المؤمنين الإمام علي بن أبي طالب (ع)، وهو مركز رئيسي للعلوم الدينية والروحانية وجهاد الأئمة المعصومين.",
        descUr = "امیر المومنین حضرت علی ابن ابی طالب علیہ السلام کا روضہ مبارک، جو علم و معرفت اور روحانیت کا عظیم مرکز ہے۔",
        tipsEn = "Recommended attire: Modest traditional clothing (Abaya for women). Best time to visit: Early morning (after Fajr) or late at night (after 10 PM) to avoid extreme heat and crowds. Local etiquette: Perform wudhu (ablution) before entering, enter with your right foot first, maintain silence, and follow directions from local guides.",
        tipsAr = "الزي الموصى به: الملابس التقليدية المحتشمة (العباءة والچادر للنساء). أفضل وقت للزيارة: الصباح الباكر (بعد الفجر) أو في وقت متأخر من الليل (بعد الساعة 10 مساءً) لتجنب الحرارة والزحام الشديد. الآداب المحلية: الوضوء قبل الدخول، الدخول بالقدم اليمنى، التزام الهدوء، واتباع إرشادات اللجان المنظمة.",
        tipsUr = "تجویز کردہ لباس: باوقار اور روایتی لباس (خواتین کے لیے عبایا یا چادر لازمی ہے)۔ زیارت کا بہترین وقت: صبح سویرے (نمازِ فجر کے بعد) یا دیر رات (رات 10 بجے کے بعد) تاور گرمی اور ہجوم سے بچا جا سکے۔ مقامی آداب: داخلے سے پہلے وضو کریں، دایاں پاؤں پہلے رکھیں، خاموشی برقرار رکھیں، اور خدام کی ہدایات پر عمل کریں۔",
        masoomeenIndices = listOf(2)
    ),
    ZiyaratLocation(
        id = "medina_baqi",
        nameEn = "Jannat al-Baqi (4 Holy Imams)",
        nameAr = "جنة البقيع (الأئمة الأربعة ع)",
        nameUr = "جنت البقیع (ائمہ اربعہ علیہم السلام)",
        lat = 24.46694,
        lon = 39.61639,
        cityEn = "Medina, Saudi Arabia",
        cityAr = "المدينة المنورة، السعودية",
        cityUr = "مدینہ منورہ، سعودی عرب",
        descEn = "The oldest and most sacred Islamic cemetery containing the vanished/demolished shrines of four holy Imams of Ahlul Bayt: Imam Hasan al-Mujtaba (4th Masoom, 2nd Imam), Imam Sajjad (6th Masoom, 4th Imam), Imam Baqir (7th Masoom, 5th Imam), and Imam Sadiq (8th Masoom, 6th Imam). It also holds Lady Umm ul-Banin (s.a.) and the Prophet's close family.",
        descAr = "مقبرة البقيع المقدسة وتضم المقامات للأئمة الأربعة المعصومين: الأسباط الحسن بن علي، علي السجاد، محمد الباقر، وجعفر الصادق (ع)، ومقام السيدة أم البنين (ع) والهاشميين.",
        descUr = "مدینہ منورہ کا تاریخی قبرستان جہاں چودہ معصومینؑ میں سے چار معصوم ائمہ: امام حسن مجتبیٰؑ (چوتھے معصوم)، امام زین العابدینؑ (چھٹے معصوم)، امام محمد باقرؑ (ساتویں معصوم)، اور امام جعفر صادقؑ (آٹھویں معصوم) آرام فرما ہیں۔ یہاں جناب ام البنینؑ اور دیگر اہلبیتؑ کی قبریں بھی ہیں۔",
        tipsEn = "Recommended attire: Plain, conservative attire. Best time to visit: Right after Fajr prayer or after Asr prayer when the main gates are opened for a short duration. Local etiquette: Strictly no photography or videography is allowed (including mobile phones). Do not raise your voice, and pray in silent contemplation from the boundary fence.",
        tipsAr = "الزي الموصى به: الملابس البسيطة والساترة للجميع. أفضل وقت للزيارة: بعد صلاة الفجر مباشرة أو الأسابيع العادية بعد صلاة العصر. الآداب المحلية: يمنع منعاً باتاً التصوير بالهواتف النقالة، والابتعاد عن رفع الصوت والدعاء بتوقير ووقار وهدوء خلف السياج.",
        tipsUr = "تجویز کردہ لباس: انتہائی سادہ اور باوقار شرعی لباس پہنیں۔ زیارت کا بہترین وقت: نمازِ فجر یا نمازِ عصر کے فوراً بعد جب مرکزی دروازہ مختصر وقت کے لیے کھولا جاتا ہے۔ مقامی آداب: کیمرے یا موبائل فون سے تصویر کھینچنے کی سخت ممانعت ہے۔ اونچی آواز میں کچھ نہ پڑھیں، بلکہ جنگلے کے باہر نہایت خاموشی اور احترام سے سلام پیش کریں۔",
        masoomeenIndices = listOf(4, 6, 7, 8)
    ),
    ZiyaratLocation(
        id = "karbala_hussain",
        nameEn = "Shrine of Imam Hussain (a.s.)",
        nameAr = "مرقد الإمام الحسين بن علي (ع)",
        nameUr = "روضہ مبارک حضرت امام حسین علیہ السلام",
        lat = 32.616361,
        lon = 44.032314,
        cityEn = "Karbala, Iraq",
        cityAr = "كربلاء المقدسة، العراق",
        cityUr = "کربلائے معلیٰ، عراق",
        descEn = "The sacred resting place of Imam Hussain (a.s.), the grandson of Prophet Muhammad (s.a.w.w.) and the 5th Masoom (3rd Imam). He was martyred in the tragic Event of Karbala in 61 AH alongside his loyal companions, defending truth and justice. His shrine also holds Hazrat Ali Akbar (a.s.) and Ali Asghar (a.s.).",
        descAr = "مرقد سيد الشهداء الإمام الحسين بن علي (ع) - المعصوم الخامس وثالث الأئمة، الذي استشهد مع أهل بيته وأصحابه الأوفياء في واقعة الطف الأليمة عام ٦١ هـ دفاعاً عن شريعة جده المصطفى.",
        descUr = "نواسے رسولؐ، سید الشہداء حضرت امام حسین علیہ السلام کا روضہ مبارک، جو پانچویں معصوم اور تیسرے امام ہیں۔ آپ نے ۶۱ ہجری میں حق و صداقت کی بالادستی کے لیے معصوم اہل بیتؑ اور جانثار اصحابؑ کے همراہ لازوال قربانی پیش کی۔",
        tipsEn = "Recommended attire: Conservative black or dark-colored clothing as a symbol of mourning. Best time to visit: Between midnight and Fajr for a peaceful experience. Local etiquette: Cooperate fully during several security checkpoints, avoid carrying large bags or professional cameras, and recite the prescribed Ziyarat quietly.",
        tipsAr = "الزي الموصى به: الملابس الداكنة أو السوداء الموقرة تعبيراً عن الحزن والعزاء. أفضل وقت للزيارة: ما بين منتصف الليل وصلاة الفجر لأجواء أكثر هدوءاً وروحانية. الآداب المحلية: التعاون التام مع الحرس عند نقاط التفتيش، وعدم حمل الحقائب الكبيرة، وقراءة الزيارة المأثورة بصوت منخفض.",
        tipsUr = "تجویز کردہ لباس: احتراماً سیاہ یا گہرے رنگ کا سادہ شرعی لباس زیب تن کریں۔ زیارت کا بہترین وقت: آدھی رات سے فجر کے درمیان کا وقت پرسکون زیارت کے لیے موزوں ترین ہے۔ مقامی آداب: متعدد سیکیورٹی چیک پوائنٹس پر مکمل تعاون کریں، بڑا سامان یا کیمرے اپنے ساتھ نہ لے جائیں، اور نہایت خشوع کے ساتھ زیارت پڑھ ہیں۔",
        masoomeenIndices = listOf(5)
    ),
    ZiyaratLocation(
        id = "karbala_abbas",
        nameEn = "Shrine of Hazrat Abbas (a.s.)",
        nameAr = "مرقد أبي الفضل العباس (ع)",
        nameUr = "روضہ مبارک حضرت عباس علمدار علیہ السلام",
        lat = 32.6171889,
        lon = 44.0362167,
        cityEn = "Karbala, Iraq",
        cityAr = "كربلاء المقدسة، العراق",
        cityUr = "کربلائے معلیٰ، عراق",
        descEn = "The shrine of Al-Abbas ibn Ali (a.s.), the loyal brother and standard-bearer of Imam Hussain (a.s.), renowned globally for his peerless loyalty, bravery, and chivalry. He is a primary figure of Ahlul Bayt.",
        descAr = "مرقد قمر بني هاشم أبي الفضل العباس بن علي (ع) - حامل لواء الإمام الحسين، المعروف بوفائه وشجاعته الفائقة وجهاده الخالد في كربلاء.",
        descUr = "علمدارِ کربلا حضرت ابو الفضل العباس علیہ السلام کا روضہ مبارک، جو وفاداری، ایثار اور شجاعت کا بے مثال استعارہ ہیں۔",
        tipsEn = "Recommended attire: Modest dark attire. Best time to visit: Mid-morning on weekdays or late hours. Local etiquette: Enter with deep respect recognizing the supreme loyalty of Al-Abbas (a.s.), wait patiently in queues, and follow the entry/exit flows.",
        tipsAr = "الزي الموصى به: ملابس ساترة داكنة اللون. أفضل وقت للزيارة: الضحى في أيام الأسبوع العادية لتقليل الانتظار. الآداب المحلية: استحضار الوفاء العظيم لأبي الفضل العباس (ع)، الالتزام بالانتظار المنظم في الممرات، واتباع اتجاهات السير المحددة.",
        tipsUr = "تجویز کردہ لباس: باوقار اور گہرے رنگ کے کپڑے۔ زیارت کا بہترین وقت: منگل اور بدھ کو دن کا درمیانی حصہ یا رات کا آخری پہر۔ مقامی آداب: سیدنا عباس علمدارؑ کی بے مثال وفاداری کا تصور ذہن میں رکھ کر انتہائی احترام سے داخل ہوں، قطار میں صبر سے کام لیں اور آمد و رفت کے راستوں کا خیال رکھیں۔"
    ),
    ZiyaratLocation(
        id = "baghdad_kadhim",
        nameEn = "Al-Kadhimiya Shrine (2 Holy Imams)",
        nameAr = "العتبة الكاظمية الشريفة (الإمام الكاظم والإمام الجواد ع)",
        nameUr = "روضہ مبارک کاظمین شریفین (حضرت امام موسیٰ کاظمؑ اور حضرت امام محمد تقیؑ)",
        lat = 33.38,
        lon = 44.33806,
        cityEn = "Baghdad, Iraq",
        cityAr = "الكاظمية، بغداد، العراق",
        cityUr = "کاظمین، بغداد، عراق",
        descEn = "The holy sanctuary of Imam Musa al-Kadhim (9th Masoom, 7th Imam) and Imam Muhammad al-Jawad (11th Masoom, 9th Imam) of Ahlul Bayt. It is an architectural and spiritual marvel located in Baghdad.",
        descAr = "العتبة الكاظمية المقدسة، وتضم مرقدي الإمام موسى الكاظم (ع) - المعصوم التاسع وسابع الأئمة، والإمام محمد الجواد (ع) - المعصوم الحادي عشر وتاسع الأئمة.",
        descUr = "کاظمین شریفین، جہاں دو معصوم ائمہ: حضرت امام موسیٰ کاظم علیہ السلام (نویں معصوم اور ساتویں امام ہوئے) اور حضرت امام محمد تقی علیہ السلام (گیارہویں معصوم اور نویں امام) آرام فرما ہیں۔",
        tipsEn = "Recommended attire: Clean conservative dress. Best time to visit: Early mornings or after Isha prayers. Local etiquette: Maintain a highly prayerful state, keep track of designated entry gates for men and women, and cooperate with security personnel doing thorough checks.",
        tipsAr = "الزي الموصى به: الملابس الطاهرة والساترة والمحتشمة. أفضل وقت للزيارة: ساعات الصباح الأولى أو بعد صلاة العشاء. الآداب المحلية: الاستغفار المستمر واستحضار الخشوع، الانتباه والمحافظة على المداخل المنفصلة للرجال والنساء، والتعاون مع رجال الأمن للحفاظ على سلامة الجميع.",
        tipsUr = "تجویز کردہ لباس: پاک و صاف اور سادہ لباس پہنیں۔ زیارت کا بہترین وقت: صبح سویرے کے وقت یا نمازِ عشا کے بعد۔ مقامی آداب: ذکر و استغفار کرتے ہوئے داخل ہوں، مردوں اور خواتین کے لیے الگ داخلی راستوں کا دھیان رکھیں، اور سیکیورٹی اہلکاروں کی تفصیلی چیکنگ کے دوران مکمل تعاون۔",
        masoomeenIndices = listOf(9, 11)
    ),
    ZiyaratLocation(
        id = "mashhad_reza",
        nameEn = "Shrine of Imam Reza (a.s.)",
        nameAr = "مرقد الإمام علي الرضا (ع)",
        nameUr = "روضہ مبارک حضرت امام علی رضا علیہ السلام",
        lat = 36.288,
        lon = 59.6157,
        cityEn = "Mashhad, Iran",
        cityAr = "مشهد، إيران",
        cityUr = "مشہد مقدس، ایران",
        descEn = "The grand mausoleum of Imam Ali al-Reza (a.s.), the 10th Masoom (8th Imam). It is one of the world's largest shrine complexes and welcomes millions of pilgrims annually.",
        descAr = "المرقد الشريف للإمام الثامن علي بن موسى الرضا (ع) - المعصوم العاشر، ويعد من أكبر المساجد والمزارات في العالم حيث يحتضن الملايين من الزوار سنوياً.",
        descUr = "آٹھویں تاجدارِ امامت حضرت امام علی رضا علیہ السلام کا شاندار روضہ، جو دسویں معصوم اور آٹھویں امام ہیں۔ یہ دنیا کی بڑی زیارت گاہوں میں سے ایک ہے جہاں سالانہ لاکھوں زائرین حاضری دیتے ہیں۔",
        tipsEn = "Recommended attire: Chador is mandatory for females and is provided for free at the main shrine entrances. Best time to visit: Afternoon hours or late night. Local etiquette: Keep your shoes at the designated 'Kafshdari' counters, obtain a free guide book, and silence your mobile phones.",
        tipsAr = "الزي الموصى به: ارتداء 'الچادر' ضروري جداً للنساء ومتوفر مجاناً عند مداخل الحرم الشريف. أفضل وقت للزيارة: ساعات العصر أو أواخر الليل. الآداب المحلية: إيداع الأحذية في الأماكن المخصصة (الكفشداري)، الحصول على كتيبات الزيارة المجانية، وإغلاق الهواتف النقالة.",
        tipsUr = "تجویز کردہ لباس: خواتین کے لیے چادر اوڑھنا لازمی ہے جو تمام داخلی راستوں پر مفت ملتی ہے۔ زیارت کا بہترین وقت: دوپہر کے اوقات یا دیر رات کا وقت۔ مقامی آداب: اپنے جوتے مخصوص 'کفش داری' کاؤنٹرز پر جمع کروائیں، وہاں موجود رہنمائی کے مراکز سے زائرین کا گائیڈ کارڈ حاصل کریں اور موبائل فون بند یا سائلنٹ رکھیں۔",
        masoomeenIndices = listOf(10)
    ),
    ZiyaratLocation(
        id = "samarra_askari",
        nameEn = "Al-Askariyain Shrine (2 Holy Imams)",
        nameAr = "العتبة العسكرية المقدسة (الإمام الهادي والإمام العسكري ع)",
        nameUr = "روضہ مبارک عسکریین شریفین (حضرت امام علی نقیؑ اور حضرت امام حسن عسکریؑ)",
        lat = 34.1989,
        lon = 43.8735,
        cityEn = "Samarra, Iraq",
        cityAr = "سامراء، العراق",
        cityUr = "سامرا، عراق",
        descEn = "The holy resting place of Imam Ali al-Hadi (12th Masoom, 10th Imam) and Imam Hasan al-Askari (13th Masoom, 11th Imam) of Ahlul Bayt, located in Samarra, north of Baghdad.",
        descAr = "العتبة العسكرية المقدسة في سامراء، مرقد الإمامين علي الهادي (المعصوم ١٢) والحسن العسكري (المعصوم ١٣) عليهما السلام.",
        descUr = "سامرا میں عسکریین شریفین کا روضہ، جہاں دسویں اور گیارہویں امام حضرت امام علی نقیؑ (بارہویں معصوم) اور حضرت امام حسن عسکریؑ (تیرہویں معصوم) آرام فرما ہیں۔",
        tipsEn = "Recommended attire: Standard modest pilgrim clothing. Best time to visit: It is safest and highly recommended to travel in organized daytime groups/pilgrim buses leaving from Baghdad. Local etiquette: Be patient at numerous highway checkpoints, pray in the holy cellar (Sardab), and follow the guidance of tour leaders.",
        tipsAr = "الزي الموصى به: ملابس الزيارة المعتادة والساترة. أفضل وقت للزيارة: نهاراً عبر القوافل والحافلات المنسقة التي تسير من بغداد وتعود قبل الغروب. الآداب المحلية: التحلي بالصبر عند نقاط التفتيش المنتشرة على الطرق السريعة، الصلاة والزيارة في سرداب الغيبة المقدس، واتباع توجيهات مسؤولي الحملة.",
        tipsUr = "تجویز کردہ لباس: عام باوقار سفری و شرعی لباس۔ زیارت کا بہترین وقت: سیکیورٹی وجوہات کی بنا پر بغداد سے دن کے وقت روانہ ہونے والے منظم قافلوں یا سرکاری بسوں کے ذریعے سفر کریں۔ مقامی آداب: شاہراہ پر موجود متعدد چیک پوسٹوں پر تحمل کا مظاہرہ کریں، سردابِ مقدس میں نماز ادا کریں، اور اپنے گروپ لیڈر کی ہدایات پر عمل کریں۔",
        masoomeenIndices = listOf(12, 13)
    ),
    ZiyaratLocation(
        id = "samarra_mahdi",
        nameEn = "Sardab al-Ghaybah (Imam al-Mahdi a.t.f.s.)",
        nameAr = "سرداب الغيبة المقدسة (الإمام المهدي عج)",
        nameUr = "سردابِ غیبت (حضرت امام مہدی عجل اللہ تعالیٰ فرجہ الشریف)",
        lat = 34.1993,
        lon = 43.8724,
        cityEn = "Samarra, Iraq",
        cityAr = "سامراء، العراق",
        cityUr = "سامرا، عراق",
        descEn = "The sacred cellar where the 14th Masoom, the Savior of Humanity, Imam al-Mahdi (a.t.f.s.) lived and began his Minor Occultation. It is a highly respected place of prayer and direct connection with the Imam of Our Time.",
        descAr = "سرداب الغيبة في العتبة العسكرية المقدسة، حيث كان منزلاً ومصلى للإمام المهدي المنتظر (عج) - المعصوم الرابع عشر والأخير، ومركز الدعاء بتعجيل فرجه الشريف.",
        descUr = "عسکریین شریفین کے احاطے میں موجود وہ مبارک تہہ خانہ (سرداب) جہاں آخری معصوم (چودھویں معصوم)، منجیِ بشریت حضرت صاحب الزماں امامِ مہدی (عج) نے زندگی گزاری اور جہاں سے غیبتِ صغریٰ کا آغاز ہوا۔",
        tipsEn = "Recommended attire: Devotional modest attire. Best time to visit: Midday during secure group visits. Local etiquette: Recite Dua Nudba or Ziyarat Al-Yasin inside the Sardab with immense humility, present your written letters of supplication (Ariza) with pure intent, and stay calm.",
        tipsAr = "الزي الموصى به: ملابس التقديس والساترة. أفضل وقت للزيارة: نهاراً مع الوفود. الآداب المحلية: قراءة زيارة آل ياسين ودعاء الندبة بإنكسار وخشوع، والدعاء بتعجيل فرج الإمام المهدي (عج) تحت قبة السرداب الشريف.",
        tipsUr = "تجویز کردہ لباس: باوضو اور پاکیزہ لباس۔ زیارت کا بہترین وقت: دن کے پرسکون اوقات میں۔ مقامی آداب: سرداب کے اندر نہایت خشوع سے زیارتِ آلِ یاسین یا دعائے ندبہ پڑھیں، امامِ عصرؑ کے حضور اپنی حاجات اور سلام پیش کریں اور سکوت قائم رکھیں۔",
        masoomeenIndices = listOf(14)
    ),
    ZiyaratLocation(
        id = "damascus_zainab",
        nameEn = "Shrine of Sayyida Zainab (s.a.)",
        nameAr = "مرقد العقيلة السيدة زينب (ع)",
        nameUr = "روضہ مبارک حضرت سیدہ زینب سلام اللہ علیہا",
        lat = 33.44444,
        lon = 36.34083,
        cityEn = "Damascus, Syria",
        cityAr = "دمشق، سوريا",
        cityUr = "دمشق، شام",
        descEn = "The holy tomb of Sayyida Zainab bint Ali (s.a.), the heroic daughter of Imam Ali (a.s.) and Lady Fatima (s.a.), who kept the message of Karbala alive through her majestic sermons.",
        descAr = "المرقد المقدس للعقيلة السيدة زينب بنت علي (ع)، بطلة كربلاء وابنة أمير المؤمنين والزهراء (ع)، التي خلدت النهضة الحسينية.",
        descUr = "ثانیِ زہرا حضرت سیدہ زینب سلام اللہ علیہا کا روضہ مبارک، جنہوں نے کوفہ و شام کے درباروں میں اپنے بے مثل خطبات سے کربلا کے پیغام کو زندہ رکھا.",
        tipsEn = "Recommended attire: Traditional modest black abaya for women. Best time to visit: Cooler afternoon or evening hours. Local etiquette: Carry your passport or legal ID cards as they are routinely checked before entry, show extreme deference, and support the local service initiatives.",
        tipsAr = "الزي الموصى به: العباءة السوداء التقليدية الساترة كاملة للنساء. أفضل وقت للزيارة: ساعات العصر الأقل حرارة أو في المساء. الآداب المحلية: الاحتفاظ بجواز السفر أو بطاقة الهوية الرسمية لفحصها بشكل روتيني عند المداخل، التحلي بأعلى درجات الوقار والأدب، ومساعدة الفقراء المحيطين بالمنطقة.",
        tipsUr = "تجویز کردہ لباس: خواتین کے لیے روایتی اور باوقار سیاہ عبایا لازمی ہے۔ زیارت کا بہترین وقت: سہ پہر یا شام کے اوقات جب موسم معتدل ہوتا ہے۔ مقامی آداب: داخلے کے وقت شناختی چیکنگ کی جاتی ہے اس لیے اپنا پاسپورٹ اپنے ساتھ رکھیں، بے پناہ ادب کا مظاہرہ کریں، اور مقامی خدمت گاروں سے تعاون کریں۔"
    ),
    ZiyaratLocation(
        id = "damascus_ruqayya",
        nameEn = "Shrine of Sayyida Ruqayya (s.a.)",
        nameAr = "مرقد السيدة رقية بنت الحسين (ع)",
        nameUr = "روضہ مبارک حضرت سیدہ رقیہ سلام اللہ علیہا",
        lat = 33.51333,
        lon = 36.30722,
        cityEn = "Damascus, Syria",
        cityAr = "دمشق، سوريا",
        cityUr = "دمشق، شام",
        descEn = "The shrine attributed to Sayyida Ruqayya, a young daughter of Imam Hussain (a.s.). It is a place of mourning and remembrance for the captivity and suffering of Ahlul Bayt after Karbala.",
        descAr = "المرقد المنسوب إلى السيدة رقية، ابنة الإمام الحسين (ع)، وهو مكان للعزاء وإحياء ذكرى سبي أهل البيت ومآسيهم بعد كربلاء.",
        descUr = "یہ روضہ حضرت امام حسین علیہ السلام کی کم سن بیٹی سیدہ رقیہ سلام اللہ علیہا سے منسوب ہے۔ یہاں کربلا کے بعد اہلبیتؑ کی اسیری اور مصائب کو یاد کر کے عزا و سوگواری کی جاتی ہے۔",
        tipsEn = "Recommended attire: Dark conservative clothing. Best time to visit: Morning hours before the heat rises. Local etiquette: Enter with total empathy for the orphans of Hussein, recite the specific Ziyarat with a soft low voice, and cooperate at the security doors.",
        tipsAr = "الزي الموصى به: العباءة الشرعية للنساء والملابس المحتشمة للرجال. أفضل وقت للزيارة: الساعات الصباحية الهادئة. الآداب المحلية: استحضار مأساة يتامى أهل البيت (ع) والبكاء بوقار وصمت، والتعاون التام مع خدمة المقام الشريف عند الأبواب.",
        tipsUr = "تجویز کردہ لباس: نہایت باوقار اور گہرے رنگ کے کپڑے۔ زیارت کا بہترین وقت: سورج نکلنے کے بعد صبح کے پرسکون اوقات۔ مقامی آداب: مظلوم کربلا کے کمسن بچوں اور یتیموں کی صعوبتوں کو یاد رکھ کر گریہ و زاری کریں، شور شرابے سے دور رہیں، اور سیکیورٹی اہلکاروں کی ہدایات کی پابندی کریں۔"
    ),
    ZiyaratLocation(
        id = "qom_masuma",
        nameEn = "Shrine of Sayyida Fatima Masuma (s.a.)",
        nameAr = "مرقد السيدة فاطمة المعصومة (ع)",
        nameUr = "روضہ مبارک حضرت سیدہ فاطمہ معصومہ سلام اللہ علیہا",
        lat = 34.6417,
        lon = 50.879,
        cityEn = "Qom, Iran",
        cityAr = "قم، إيران",
        cityUr = "قم مقدس، ایران",
        descEn = "The holy sanctuary of Lady Fatima Masuma (s.a.), the noble sister of Imam al-Reza (a.s.). Qom has grown into a premier global center for Islamic theology around her shrine.",
        descAr = "المرقد المقدس لكريمة أهل البيت السيدة فاطمة المعصومة (ع)، أخت الإمام الرضا (ع)، وهي منارة كبرى للحوزة العلمية في قم المقدسة.",
        descUr = "قم میں کریمہ اہلبیت حضرت فاطمہ معصومہ سلام اللہ علیہا کا روضہ مبارک، جن کے وجود مسعود کی برکت سے قم علم و فقہ کا عظیم عالمی مرکز بنا۔",
        tipsEn = "Recommended attire: A Chador is mandatory for women visitors and can be obtained at the main entrances. Best time to visit: Weekdays between 8 AM and 11 AM when it is highly peaceful. Local etiquette: Show respect towards the seminary students, avoid loud discussions, and utilize informational guides.",
        tipsAr = "الزي الموصى به: ارتداء 'الچادر' كامل للمرأة وهو إلزامي لدخول المزار (متوفر عند الأبواب). أفضل وقت للزيارة: الصباح بين الساعة 8 و 11 لضمان الخلو والهدوء. الآداب المحلية: توقير طلاب الحوزة والعلماء، تجنب الأحاديث الجانبية والصاخبة، والاستفادة من النشرات الإرشادية.",
        tipsUr = "تجویز کردہ لباس: خواتین زائرین کے لیے چادر اوڑھنا لازمی ہے (داخلی دروازوں پر فراہم کی جاتی ہے)۔ زیارت کا بہترین وقت: ہفتے کے عام دنوں میں صبح 8 سے 11 بجے کا وقت، جب وہاں خاموشی اور سکون ہوتا ہے۔ مقامی آداب: حوزہ علمیہ کے طلبہ اور اساتذہ کا احترام کریں، بلند آواز سے گفتگو سے پرہیز کریں، اور مفت معلوماتی لٹریچر سے استفادہ کریں۔"
    ),
    ZiyaratLocation(
        id = "mecca_mualla",
        nameEn = "Jannat al-Mu'alla (Lady Khadija s.a.)",
        nameAr = "مقبرة جنة المعلاة (السيدة خديجة الكبرى ع)",
        nameUr = "قبرستان جنت المعلیٰ (حضرت سیدہ خدیجہ کبریٰ سلام اللہ علیہا)",
        lat = 21.436991,
        lon = 39.829213,
        cityEn = "Mecca, Saudi Arabia",
        cityAr = "مكة المكرمة، السعودية",
        cityUr = "مکہ مکرمہ، سعودی عرب",
        descEn = "The sacred graveyard of Mecca containing the holy graves of Lady Khadija (s.a.), the first wife of the Prophet and mother of Lady Fatima (s.a.), alongside Hazrat Abu Talib (a.s.) and the grandfather of the Prophet, Abdul Muttalib (a.s.).",
        descAr = "مقبرة الحجون الشريفة في مكة المكرمة وتضم الضريح المهدم لأم المؤمنين السيدة خديجة الكبرى (ع) - المعصومة الروحية وسند الإسلام - وجد النبي عبد المطلب وأبي طالب (ع).",
        descUr = "مکہ مکرمہ کا تاریخی اور مقدس ترین قبرستان جہاں ام المومنین حضرت خدیجہ کبریٰ سلام اللہ علیہا (سرتاجِ اولِ رسولؐ اور مادرِ سیدہ زہراؑ)، محسنِ اسلام حضرت ابوطالب علیہ السلام، اور دادائے رسولؐ حضرت عبدالمطلب علیہ السلام آسودہ خاک ہیں۔",
        tipsEn = "Recommended attire: Plain conservative Hajj / Umrah clothing. Best time to visit: Mid-morning or after Sunset on regular days. Local etiquette: Approach with absolute respect, keep phones secure, do not shout or perform loud ritual practices, and pray from the designated sidewalks quietly.",
        tipsAr = "الزي الموصى به: ملابس الإحرام البسيطة أو الثوب والعباءة الساترة. أفضل وقت للزيارة: بعد صلاة العصر أو أثناء النهار. الآداب المحلية: السلام على أم المؤمنين خديجة الكبرى (ع) بخشوع وحزن، عدم رفع الأصوات لضمان احترام القوانين المحلية.",
        tipsUr = "تجویز کردہ لباس: سادہ شرعی لباس یا سفید شلوار قمیض۔ زیارت کا بہترین وقت: دوپہر کے علاوہ صبح و شام کے اوقات۔ مقامی آداب: محسنۂ اسلام جنابِ خدیجۃ الکبریٰؑ کے درجات پر سلام پیش کریں، بلند آواز سے گریہ و زاری سے بچیں، اور خاموشی سے سورہ فاتحہ اور صلوٰۃ پیش کریں۔"
    ),
    ZiyaratLocation(
        id = "medina_uhud",
        nameEn = "Uhud Cemetery (Hazrat Hamza a.s.)",
        nameAr = "مقبرة شهداء أحد (جبل أحد وسيد الشهداء حمزة ع)",
        nameUr = "قبرستان شہدائے احد (سید الشہداء حضرت حمزہ علیہ السلام)",
        lat = 24.50361,
        lon = 39.61194,
        cityEn = "Medina, Saudi Arabia",
        cityAr = "المدينة المنورة، السعودية",
        cityUr = "مدینہ منورہ، سعودی عرب",
        descEn = "The battleground of Uhud and cemetery containing the holy resting place of the uncle of the Prophet, Hazrat Hamza ibn Abdul Muttalib (a.s.), 'Sayyid al-Shuhada' of his time, who was martyred protecting Islam.",
        descAr = "مقبرة شهداء معركة أحد المقدسة في المدينة المنورة، وتضم مرقد عم النبي الأكرم وسيد الشهداء حمزة بن عبد المطلب (ع) والصحابة المخلصين.",
        descUr = "مدینہ منورہ میں غزوۂ احد کا تاریخی میدان اور وہ مقدس قبرستان جہاں حضورؐ کے باوفا چچا، شیرِ خدا و شیرِ رسولؐ حضرت حمزہ بن عبدالمطلب علیہ السلام اور دیگر ستر شہدائے احد آرام فرما ہیں۔",
        tipsEn = "Recommended attire: Comfortable walking modest clothing. Best time to visit: Early morning when temperatures are cool. Local etiquette: Recite the unique Ziyarat of the Uhud martyrs, stand quietly opposite the graves, and climb the Archers' hill with care.",
        tipsAr = "الزي الموصى به: ملابس مريحة ومحتشمة للمشي الطويل. أفضل وقت للزيارة: ساعات الصباح الباكر لبرودة الجو. الآداب المحلية: الزيارة والدعاء لعم الرسول حمزة (ع) والشهداء الأبرار، والمحافظة على النظافة العامة.",
        tipsUr = "تجویز کردہ لباس: باوقار اور آرام دہ سفری لباس۔ زیارت کا بہترین وقت: صبح سویرے جب دھوپ تیز نہیں ہوتی۔ مقامی آداب: جنابِ حمزہؑ اور شہدائے احد کے لیے صلوٰۃ و سلام کا ہدیہ پیش کریں، نہایت احترام سے الوداعی سلام پڑھیں، اور پہاڑی پر چڑھتے وقت احتیاط بڑھائیں۔"
    ),
    ZiyaratLocation(
        id = "balad_muhammad",
        nameEn = "Shrine of Sayyid Muhammad (a.s.)",
        nameAr = "مرقد السيد محمد بن الإمام الهادي (ع)",
        nameUr = "روضہ مبارک حضرت سید محمد بن امام علی نقی علیہ السلام",
        lat = 34.0135,
        lon = 44.1481,
        cityEn = "Balad, Iraq",
        cityAr = "بلد، العراق",
        cityUr = "بلد، عراق",
        descEn = "The grand shrine of Sayyid Muhammad (a.s.), the eldest son of Imam Ali al-Hadi (a.s.) and brother of Imam Hasan al-Askari (a.s.). He is universally revered for his intense purity, deep knowledge, and powerful spiritual presence.",
        descAr = "مرقد سبع الدجيل، السيد محمد بن الإمام علي الهادي (ع)، وهو شقيق الإمام الحسن العسكري، المعروف بعلو مقامه، وكراماته العظيمة المشهورة وجلالة روحه الطاهرة.",
        descUr = "حضرت سید محمد علیہ السلام (سبع الدجیل) کا پروقار مزارِ اقدس، جو دسویں امام حضرت علی نقیؑ کے سب سے بڑے فرزند اور گیارہویں امام حضرت حسن عسکریؑ کے سگے بھائی ہیں۔ آپ اپنی جلالتِ علمی، بے پناہ طہارت اور کراماتِ الٰہی کے لیے زائرین میں بے حد مشہور ہیں۔",
        tipsEn = "Recommended attire: Plain traditional pilgrim dress. Best time to visit: On the way heading towards or coming back from Samarra. Local etiquette: Be fully prepared for extensive security screens, wait patiently at the gates, and read the Ziyarat of the noble son of Imamate.",
        tipsAr = "الزي الموصى به: الملابس الشرعية والتقليدية المحتشمة. أفضل وقت للزيارة: أثناء التوجه إلى سامراء أو العودة منها. الآداب المحلية: التعاون عند البوابة الأمنية، وطلب كرامات السيد بن الإمام الهادي، وقراءة زيارته بنية صادقة وخالص لله.",
        tipsUr = "تجویز کردہ لباس: پاک و صاف روایتی یا سلیس شلوار قمیض۔ زیارت کا بہترین وقت: سامرا جاتے ہوئے یا وہاں سے بغداد واپس لوٹتے ہوئے راستے کا قیام۔ مقامی آداب: بیرونی گیٹ پر تفصیلی تلاشی کے دوران سیکیورٹی پر مامور خدام سے تعاون کریں، نہایت مودب ہو کر دربار پہ سلام پیش کریں اور دلی خلوص سے دعا مانگیں۔"
    ),
    ZiyaratLocation(
        id = "hillah_sharifa",
        nameEn = "Shrine of Lady Sharifa (s.a.)",
        nameAr = "مرقد العلوية الشريفة بنت الإمام الحسن (ع)",
        nameUr = "روضہ مبارک جناب سیدہ شریفہ بنت امام حسن مجتبیٰ علیہا السلام",
        lat = 32.4411,
        lon = 44.4152,
        cityEn = "Hillah, Iraq",
        cityAr = "الحلة، العراق",
        cityUr = "ہلہ، عراق",
        descEn = "The holy sanctuary of Lady Sharifa (s.a.), the noble daughter of Imam Hasan al-Mujtaba (a.s.). Celebrated globally for her miraculous healing intercessions (Bab al-Hawaij) and comforting presence.",
        descAr = "مرقد طبيبة معاصي أهل البيت العلوية الشريفة بنت الإمام الحسن السبط (ع)- في منطقة الحلة، مقصد المرضى وطالبي الحوائج ببركتها الطاهرة.",
        descUr = "حضرت امام حسن مجتبیٰ علیہ السلام کی صاحبزادی حضرت سیدہ شریفہ سلام اللہ علیہا کا پربرکت روضہ مبارک، جو عراق کے تاریخی شہر ہلہ میں واقع ہے۔ آپ بیماروں کی شفا یابی اور باب الحوائج ہونے کی وجہ سے دنیا بھر کے زائرین کی امیدوں کا مرکز ہیں۔",
        tipsEn = "Recommended attire: Conservative traditional attire (chador/abaya to show respect). Best time to visit: Afternoon on weekdays. Local etiquette: Present your prayers for health with deep faith, respect the massive influx of patients, and assist in distributing charity items if possible.",
        tipsAr = "الزي الموصى به: العباءة الچادر الساتر للنساء. أفضل وقت للزيارة: في منتصف الأسبوع نهاراً. الآداب المحلية: التوسل إلى الله بشفائها المبارك للمرضى، إظهار التعاطف مع كثرة الزوار المرضى، والمساهمة في الموائد الخيرية.",
        tipsUr = "تجویز کردہ لباس: خواتین کے لیے عبایا اور پردے کا مکمل انتظام۔ زیارت کا بہترین وقت: ہفتے کے درمیانی ایام میں دن کا پچھلا حصہ۔ مقامی آداب: شفا یابی اور حاجات کی برآوری کے لیے پختہ یقین کے ساتھ الٰہی بارگاہ میں گڑگڑائیں، وہاں موجود علیل زائرین کا احترام و خیال رکھیں اور لنگر و خیرات میں حصہ لیں۔"
    ),
    ZiyaratLocation(
        id = "kufa_muslim",
        nameEn = "Shrine of Muslim ibn Aqeel (a.s.)",
        nameAr = "مرقد مسلم بن عقيل (ع)",
        nameUr = "روضہ مبارک حضرت مسلم بن عقیل علیہ السلام",
        lat = 32.02861,
        lon = 44.40083,
        cityEn = "Kufa, Iraq",
        cityAr = "الكوفة، العراق",
        cityUr = "کوفہ، عراق",
        descEn = "The sacred shrine inside Majid al-Kufa holding the holy graves of Hazrat Muslim ibn Aqeel (a.s.), the cousin and ambassador of Imam Hussain (a.s.), together with his loyal host Hani ibn Urwah (a.s.).",
        descAr = "المرقد المقدس لمسلم بن عقيل (ع) سفير أبي عبد الله الحسين في الكوفة، ومعه هانئ بن عروة والمختار الثقفي بطل الإسلام.",
        descUr = "کوفہ میں حضرت مسلم بن عقیل علیہ السلام اور حضرت ہانی بن عروہ کا مزار اقدس جو کہ مسجد کوفہ کے احاطے میں ہے اور بہت ہی عظیم الشان زیارت ہے۔",
        tipsEn = "Recommended attire: Modest traditional clothing. Best time to visit: Afternoon, combined with performing Masjid al-Kufa holy A'maal prayers. Local etiquette: Chant greetings with extreme reverence, do not block the paths, and recite the specific Ziyarat of Hazrat Muslim.",
        tipsAr = "الزي الموصى به: العباءة للنساء والثوب للرجال. أفضل وقت للزيارة: فترة الضحى أو العصر مع أداء أعمال مسجد الكوفة التاريخية. الآداب المحلية: قراءة زيارة مسلم بن عقيل بخشوع ودموع طاهرة.",
        tipsUr = "تجویز کردہ لباس: باوقار اور سادہ کپڑے۔ زیارت کا بہترین وقت: دن کا پچھلا حصہ جب مسجد کوفہ کے اعمال کرنے میں آسانی ہو۔ مقامی آداب: دربار میں داخل ہو کر گریہ و زاری کریں، وہاں کی مخصوص زیارت پڑھیں اور زائرین کے حقوق کا خیال رکھیں۔"
    ),
    ZiyaratLocation(
        id = "musayyib_tiflan",
        nameEn = "Shrine of Tiflan-e-Muslim (a.s.)",
        nameAr = "مرقد طفلي مسلم بن عقيل (ع)",
        nameUr = "مزار طفلان مسلم علیہ السلام",
        lat = 32.785271,
        lon = 44.312849,
        cityEn = "Musayyib, Iraq",
        cityAr = "المسيب، العراق",
        cityUr = "مسیب، عراق",
        descEn = "The holy resting place of Ibrahim and Muhammad, the two young, pure sons of Hazrat Muslim ibn Aqeel (a.s.), brutally martyred while in captivity of the oppressors near the Euphrates river.",
        descAr = "المزار الشريف لولدي مسلم بن عقيل إبراهيم ومحمد عليهما السلام، اللذين قتلا مظلومين في ريعان طفولتهما بعد واقعة كربلاء.",
        descUr = "حضرت مسلم بن عقیلؑ کے دو چاند جیسے معصوم فرزندوں، محمد اور ابراہیم کا روضہ، جنہیں کربلا کے بعد قید خانے سے نکل کر فرات کے کنارے انتہائی بیدردی سے شہید کیا گیا تھا۔",
        tipsEn = "Recommended attire: Plain pilgrim attire. Best time to visit: Early afternoons. Local etiquette: Direct your thoughts towards children's hardships, pray for the orphan children of the world, and maintain a quiet mourning posture.",
        tipsAr = "الزي الموصى به: ملابس طاهرة وساترة. أفضل وقت للزيارة: بعد الظهر نهاراً. الآداب المحلية: تذكر ظمأ وعطش الأطفال الأبرياء في كربلاء، وقراءة التعازي بهدوء تام.",
        tipsUr = "تجویز کردہ لباس: دھلی ہوئی پاک و صاف شلوار قمیض۔ زیارت کا بہترین وقت: دوپہر کے وقت۔ مقامی آداب: مصیبت زدہ بچوں اور یتیموں کی پیاس اور بھوک کو یاد کر کے سلام پیش کریں اور دربار پر ادب و احترام کا مکمل التزام کریں۔"
    )
)

val leafletHtml = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        html, body, #map {
            height: 100%;
            margin: 0;
            padding: 0;
            background-color: #0F172A;
        }
        .leaflet-popup-content-wrapper {
            background-color: #1E293B !important;
            color: #FFFFFF !important;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.5);
            font-family: system-ui, -apple-system, sans-serif;
            border: 1px solid rgba(255,255,255,0.1);
        }
        .leaflet-popup-tip {
            background-color: #1E293B !important;
        }
        .popup-title {
            font-weight: bold;
            font-size: 15px;
            color: #38BDF8;
            margin-bottom: 4px;
        }
        .popup-city {
            font-size: 11px;
            font-weight: 600;
            color: #34D399;
            text-transform: uppercase;
            margin-bottom: 8px;
        }
        .popup-desc {
            font-size: 12px;
            line-height: 1.5;
            color: #E2E8F0;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map', {
            zoomControl: false
        }).setView([32.5, 45.0], 4);
        
        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
            subdomains: 'abcd',
            maxZoom: 20
        }).addTo(map);

        L.control.zoom({
            position: 'topright'
        }).addTo(map);

        var markers = {};

        function addMarker(id, lat, lon, title, city, desc) {
            var popupContent = '<div class="popup-title">' + title + '</div>' +
                               '<div class="popup-city">📍 ' + city + '</div>' +
                               '<div class="popup-desc">' + desc + '</div>';
            
            var marker = L.marker([lat, lon]).addTo(map)
                .bindPopup(popupContent);
            
            markers[id] = marker;
        }

        function focusLocation(id, lat, lon) {
            map.flyTo([lat, lon], 12, {
                animate: true,
                duration: 1.5
            });
            setTimeout(function() {
                if (markers[id]) {
                    markers[id].openPopup();
                }
            }, 1600);
        }

        // Dynamically injected markers:
        %MARKERS%
    </script>
</body>
</html>
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZiyaratsScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit,
    languageCode: String = "en",
    modifier: Modifier = Modifier,
    initialSelectedZiyaratId: String? = null,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {},
    onOpenZiyaratsMap: (() -> Unit)? = null,
    supplicationZiyarats: List<com.example.ui.ShiaDua> = emptyList(),
    onOpenDua: ((com.example.ui.ShiaDua) -> Unit)? = null
) {
    val allZiyarats by viewModel.allZiyarats.collectAsState()
    val allCollections by viewModel.allCollections.collectAsState()
    var activeZiyaratForCollectionDialog by remember { mutableStateOf<ZiyaratItem?>(null) }
    
    val context = LocalContext.current
    val eventsRevision by com.example.data.IslamicEventManager.eventsRevision.collectAsState()
    val eventsByMasoom = remember(context, eventsRevision) {
        MasoomeenData.getAllEvents(context).groupBy { it.masoomIndex }
    }
    var searchQuery by remember { mutableStateOf("") }
    val prefs = remember { context.getSharedPreferences("recent_searches", android.content.Context.MODE_PRIVATE) }
    var recentSearches by remember { 
        mutableStateOf(prefs.getStringSet("ziyarat_searches", emptySet<String>())?.toList()?.reversed() ?: emptyList<String>()) 
    }
    
    fun saveRecentSearch(query: String) {
        if (query.isNotBlank()) {
            val updated = (listOf(query) + recentSearches).distinct().take(5)
            recentSearches = updated
            prefs.edit().putStringSet("ziyarat_searches", updated.toSet()).apply()
        }
    }

    var selectedCategory by remember { mutableStateOf("All") } // "All", "Prophet", "Imams", "Lady Fatima", "Favorites"
    var currentSortOption by remember { mutableStateOf("Most Popular") }
    var activeReadingZiyarat by remember { mutableStateOf<ZiyaratItem?>(null) }
    
    BackHandler(enabled = activeReadingZiyarat != null) {
        activeReadingZiyarat = null
    }

    // Auto-select starting Ziyarat if passed as initial argument
    LaunchedEffect(initialSelectedZiyaratId, allZiyarats) {
        if (initialSelectedZiyaratId != null && allZiyarats.isNotEmpty()) {
            val matched = allZiyarats.find { it.id == initialSelectedZiyaratId }
            if (matched != null) {
                activeReadingZiyarat = matched
            }
        }
    }

    // Filtering & Sorting system
    val filteredZiyarats = remember(allZiyarats, searchQuery, selectedCategory, currentSortOption) {
        val filtered = allZiyarats.filter { item ->
            // Search criteria
            val matchesSearch = item.titleEn.contains(searchQuery, ignoreCase = true) ||
                    item.titleAr.contains(searchQuery) ||
                    item.titleUr.contains(searchQuery, ignoreCase = true) ||
                    item.arText.contains(searchQuery) ||
                    item.enText.contains(searchQuery, ignoreCase = true) ||
                    item.urText.contains(searchQuery, ignoreCase = true)

            // Category classification
            val matchesCategory = when (selectedCategory) {
                "Prophet" -> item.masoomIndex == 1
                "Lady Fatima" -> item.masoomIndex == 3
                "Imams" -> item.masoomIndex in listOf(2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
                "Favorites" -> item.isFavorite
                else -> true
            }

            matchesSearch && matchesCategory
        }

        when (currentSortOption) {
            "Alphabetical" -> filtered.sortedBy { if (languageCode == "ur") it.titleUr else it.titleEn }
            "Shortest Duration" -> filtered.sortedBy { it.arText.length }
            "Longest Duration" -> filtered.sortedByDescending { it.arText.length }
            "Most Popular" -> {
                val popularIds = mapOf(
                    "ziyarat_ashura" to 100,
                    "ziyarat_aminullah" to 95,
                    "ziyarat_prophet" to 90,
                    "ziyarat_fatimah" to 85,
                    "ziyarat_waritha" to 80,
                    "ziyarat_hasan" to 75
                )
                filtered.sortedWith(
                    compareByDescending<ZiyaratItem> { it.isFavorite }
                        .thenByDescending { popularIds[it.id] ?: 0 }
                        .thenBy { if (languageCode == "ur") it.titleUr else it.titleEn }
                )
            }
            else -> filtered
        }
    }

    Scaffold(
        modifier = modifier
            .background(Color(0xFFFAF6EE))
            .testTag("ziyarats_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAF6EE))
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Elegant Spiritual Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F2E27), Color(0xFF1B4D3E))
                        )
                    )
            ) {
                // Subtle Custom Islamic Dome-Arch Canvas Drawing
                Canvas(modifier = Modifier.matchParentSize()) {
                    val path = Path()
                    val width = size.width
                    val height = size.height
                    
                    path.moveTo(0f, height)
                    path.lineTo(0f, height * 0.7f)
                    path.cubicTo(
                        width * 0.25f, height * 0.65f,
                        width * 0.35f, height * 0.3f,
                        width * 0.5f, height * 0.2f
                    )
                    path.cubicTo(
                        width * 0.65f, height * 0.3f,
                        width * 0.75f, height * 0.65f,
                        width, height * 0.7f
                    )
                    path.lineTo(width, height)
                    path.close()
                    
                    drawPath(
                        path = path,
                        color = Color(0xFFD4AF37).copy(alpha = 0.08f)
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFFD4AF37).copy(alpha = 0.15f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar with Back Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("ziyarat_back")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFFAF6EE)
                            )
                        }

                        // Title Column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (languageCode == "ur") "مقدس زیارات" else "Sacred Ziyarats",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD4AF37),
                                fontFamily = NotoNastaliqUrduFont,
                                modifier = Modifier.offset(y = if (languageCode == "ur") (-4).dp else 0.dp)
                            )
                            Text(
                                text = if (languageCode == "ur") "۱۴ معصومین کے روضہ مبارک کی تلاوتیں" else "Holy visitations of the 14 Infallible Masoomeen (as)",
                                fontSize = 11.sp,
                                color = Color(0xFFFAF6EE).copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.width(40.dp)) // Visual balance
                    }

                    // Glass-morphic Beautiful Search Bar
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF1B4D3E),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { saveRecentSearch(searchQuery.trim()) }),
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F2E27),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = if (languageCode == "ur") "زیارت یا عربی متن تلاش کریں..." else "Search Ziyarats & Arabic text...",
                                                color = Color(0xFF0F2E27).copy(alpha = 0.5f),
                                                fontSize = 14.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF0F2E27).copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(visible = searchQuery.isEmpty() && recentSearches.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.forEach { search ->
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable { searchQuery = search }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = "Recent",
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = search,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom Styled Category Filters Row
            val tabs = listOf(
                "All" to "سب",
                "Prophet" to "رسول اکرمؐ",
                "Lady Fatima" to "سیدہ زہراؑ",
                "Imams" to "ائمہ معصومینؑ",
                "Favorites" to "پسندیدہ"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tabs.forEach { (catId, catUr) ->
                    val selected = catId == selectedCategory
                    val displayText = if (languageCode == "ur") catUr else catId
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) Color(0xFF0F2E27) else Color(0xFF0F2E27).copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (selected) Color(0xFFD4AF37) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedCategory = catId }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = displayText,
                            color = if (selected) Color(0xFFFAF6EE) else Color(0xFF0F2E27),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = if (languageCode == "ur") NotoNastaliqUrduFont else FontFamily.Default
                        )
                    }
                }
            }

            // Empty State
            AnimatedVisibility(
                visible = filteredZiyarats.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Empty",
                            tint = Color(0xFF0F2E27).copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (languageCode == "ur") "کوئی مقدس زیارت دستیاب نہیں ہے" else "No matching Ziyarats found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2E27).copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (languageCode == "ur") "اپنے الفاظ یا تلاش کا معیار تبدیل کریں۔" else "Try searching with a different keyword or category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF0F2E27).copy(alpha = 0.45f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Core Scrollable List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("ziyarat_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive Ziyarats Map Banner
                if (onOpenZiyaratsMap != null && (selectedCategory == "All" || selectedCategory == "Favorites")) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(24.dp))
                                .clickable { onOpenZiyaratsMap() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF0F2E27), Color(0xFF1B4D3E))
                                        )
                                    )
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = "Map Icon",
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (languageCode == "ur") "مقدس مقامات کا نقشہ" else "Interactive Ziyarats Map",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD4AF37)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (languageCode == "ur") "دنیا بھر کے مقدس روضوں کی تاریخی تفصیلات نقشے پر دیکھیں۔" else "Explore holy visitation sites with live map & guides.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFAF6EE).copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Navigate Map",
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Beautiful, modern Sort Dropdown Row for Ziyarats
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (languageCode == "ur") "ترتیب دیں:" else "Sort Ziyarats:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2E27).copy(alpha = 0.7f)
                        )
                        
                        var expanded by remember { mutableStateOf(false) }
                        val sortOptions = listOf("Most Popular", "Alphabetical", "Shortest Duration", "Longest Duration")
                        
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F2E27).copy(alpha = 0.05f))
                                    .border(1.dp, Color(0xFF0F2E27).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { expanded = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Icon",
                                    tint = Color(0xFF0F2E27),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when (currentSortOption) {
                                        "Most Popular" -> if (languageCode == "ur") "سب سے مقبول" else "Most Popular"
                                        "Alphabetical" -> if (languageCode == "ur") "حروف تہجی کے لحاظ سے" else "Alphabetical"
                                        "Shortest Duration" -> if (languageCode == "ur") "سب سے مختصر" else "Shortest Duration"
                                        "Longest Duration" -> if (languageCode == "ur") "سب سے طویل" else "Longest Duration"
                                        else -> currentSortOption
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F2E27)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color(0xFF0F2E27),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                sortOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = when (option) {
                                                    "Most Popular" -> if (languageCode == "ur") "سب سے مقبول" else "Most Popular"
                                                    "Alphabetical" -> if (languageCode == "ur") "حروف تہجی کے لحاظ سے" else "Alphabetical"
                                                    "Shortest Duration" -> if (languageCode == "ur") "سب سے مختصر" else "Shortest Duration"
                                                    "Longest Duration" -> if (languageCode == "ur") "سب سے طویل" else "Longest Duration"
                                                    else -> option
                                                },
                                                color = Color(0xFF0F2E27),
                                                fontWeight = if (option == currentSortOption) FontWeight.Bold else FontWeight.Normal
                                            ) 
                                        },
                                        onClick = {
                                            currentSortOption = option
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                items(filteredZiyarats, key = { it.id }) { ziyarat ->
                    val linkedDateText = eventsByMasoom[ziyarat.masoomIndex].orEmpty()
                        .sortedWith(compareBy<com.example.data.MasoomEvent> { it.hijriMonth }.thenBy { it.hijriDay })
                        .joinToString(" / ") {
                            com.example.utils.LocalizationUtility.translateHijriDateString(it.dateStringHijri, languageCode)
                        }
                        .ifBlank {
                            when (languageCode) {
                                "ur" -> "عمومی مستحب اوقات"
                                "ar" -> "الأوقات المستحبة العامة"
                                else -> "General recommended occasions"
                            }
                        }
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.22f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(24.dp))
                            .clickable { 
                                saveRecentSearch(searchQuery.trim())
                                activeReadingZiyarat = ziyarat 
                            }
                            .testTag("ziyarat_card_${ziyarat.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header: Masoom Badge and Favorite Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val masoomName = remember(ziyarat.masoomIndex) {
                                    MasoomeenData.list.find { it.index == ziyarat.masoomIndex }?.name ?: "Holy Infallible (as)"
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF0F2E27).copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF0F2E27).copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF0F2E27)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = ziyarat.masoomIndex.toString(),
                                                    color = Color(0xFFD4AF37),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = masoomName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F2E27)
                                            )
                                        }
                                    }

                                    // Estimated Read Time Badge for Ziyarats
                                    val totalWords = ziyarat.arText.split("\\s+".toRegex()).size
                                    val readTimeMins = (totalWords / 45).coerceAtLeast(1)
                                    val readTimeText = if (languageCode == "ur") "$readTimeMins منٹ مطالعہ" else "$readTimeMins min read"
                                    Surface(
                                        color = Color(0xFFD4AF37).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = "Estimated Read Time",
                                                tint = Color(0xFF8B7355),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = readTimeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8B7355)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { activeZiyaratForCollectionDialog = ziyarat },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreateNewFolder,
                                            contentDescription = "Add to Collection",
                                            tint = Color(0xFF0F2E27).copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleZiyaratFavorite(ziyarat) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (ziyarat.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (ziyarat.isFavorite) Color(0xFFE94057) else Color(0xFF0F2E27).copy(alpha = 0.4f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            // Ziyarat Title displaying correct translations
                            Text(
                                text = if (languageCode == "ur") ziyarat.titleUr else if (languageCode == "ar") ziyarat.titleAr else ziyarat.titleEn,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F2E27),
                                fontFamily = if (languageCode == "ur") NotoNastaliqUrduFont else FontFamily.Default,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            // Arabic Preview snippet with beautiful Amiri font
                            Text(
                                text = ziyarat.arText.take(130) + "...",
                                fontSize = 20.sp,
                                fontFamily = if (context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE).getString("arabic_font_type", "Indo-Pak") == "Indo-Pak") com.example.ui.theme.NoorehudaFont else AmiriFont,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B4D3E),
                                textAlign = TextAlign.Right,
                                lineHeight = 34.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )

                            // Connected Lunar Calendar Date Label
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F2E27).copy(alpha = 0.04f))
                                    .border(0.5.dp, Color(0xFF0F2E27).copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Calendar",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF1B4D3E)
                                )
                                Text(
                                    text = when (languageCode) {
                                        "ur" -> "منسلک قمری تواریخ: $linkedDateText"
                                        "ar" -> "التواريخ الهجرية المرتبطة: $linkedDateText"
                                        else -> "Linked Hijri dates: $linkedDateText"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F2E27)
                                )
                            }

                            // Quick action reading trigger
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F2E27).copy(alpha = 0.08f))
                                        .clickable { 
                                            saveRecentSearch(searchQuery.trim())
                                            activeReadingZiyarat = ziyarat 
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Read",
                                        tint = Color(0xFF0F2E27),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (languageCode == "ur") "زیارت پڑھیں" else "Read Ziyarah",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F2E27),
                                        fontFamily = if (languageCode == "ur") NotoNastaliqUrduFont else FontFamily.Default
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Extra Supplication Ziyarats (that are not already in ZiyaratItems)
                val existingIds = allZiyarats.map { it.id }
                val mappedIds = allZiyarats.map { z -> 
                    when (z.id) {
                        "ziyarat_ashura" -> "ashura"
                        "ziyarat_fatimah" -> "ziyarat_fatima"
                        "ziyarat_yasin" -> "ziyarat_aal_yaseen"
                        else -> z.id
                    }
                }
                val extraZiyarats = supplicationZiyarats.filter { dua -> 
                    !existingIds.contains(dua.id) && !mappedIds.contains(dua.id)
                }
                
                if (extraZiyarats.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (languageCode == "ur") "مزید زیارات" else "More Ziyarats",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2E27),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    items(extraZiyarats, key = { it.id }) { dua ->
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.22f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(3.dp, RoundedCornerShape(24.dp))
                                .clickable { 
                                    saveRecentSearch(searchQuery.trim())
                                    onOpenDua?.invoke(dua)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFF0F2E27).copy(alpha = 0.06f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Color(0xFF0F2E27),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (languageCode == "ur") dua.title else dua.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F2E27)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (languageCode == "ur") dua.category else dua.description,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = "Read",
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen Dialog
    activeReadingZiyarat?.let { ziyarat ->
        val contextForDuas = LocalContext.current
        val allLoadedDuas = remember { com.example.ui.loadDuas(contextForDuas) }
        
        val mappedId = when (ziyarat.id) {
            "ziyarat_ashura" -> "ashura"
            "ziyarat_fatimah" -> "ziyarat_fatima"
            "ziyarat_yasin" -> "ziyarat_aal_yaseen"
            else -> ziyarat.id
        }
        val matchingLineByLineZiyarat = allLoadedDuas.find { it.id == mappedId || it.id == ziyarat.id }

        if (matchingLineByLineZiyarat != null) {
            DuaReadingView(
                viewModel = viewModel,
                dua = matchingLineByLineZiyarat,
                onBack = { activeReadingZiyarat = null },
                onPlaybackEnded = {
                    val currentIndex = filteredZiyarats.indexOf(ziyarat)
                    if (currentIndex != -1 && currentIndex < filteredZiyarats.size - 1) {
                        activeReadingZiyarat = filteredZiyarats[currentIndex + 1]
                    }
                },
                onEditCard = onEditCard
            )
        } else {
            ZiyaratDetailDialog(
                ziyarat = ziyarat,
                languageCode = languageCode,
                viewModel = viewModel,
                onDismiss = { activeReadingZiyarat = null },
                onPlaybackEnded = {
                    val currentIndex = filteredZiyarats.indexOf(ziyarat)
                    if (currentIndex != -1 && currentIndex < filteredZiyarats.size - 1) {
                        activeReadingZiyarat = filteredZiyarats[currentIndex + 1]
                    }
                },
                onEditCard = onEditCard
            )
        }
    }

    val collectionZiyarat = activeZiyaratForCollectionDialog
    if (collectionZiyarat != null) {
        AddToCollectionDialog(
            duaId = null,
            ziyaratId = collectionZiyarat.id,
            allCollections = allCollections,
            onAdd = { collection: MyCollection -> viewModel.addZiyaratToCollection(collection, collectionZiyarat.id) },
            onRemove = { collection: MyCollection -> viewModel.removeZiyaratFromCollection(collection, collectionZiyarat.id) },
            onCreateCollection = { name: String, desc: String -> viewModel.createCollection(name, desc) },
            onDismiss = { activeZiyaratForCollectionDialog = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZiyaratDetailDialog(
    ziyarat: ZiyaratItem,
    languageCode: String,
    viewModel: PrayerViewModel,
    onDismiss: () -> Unit,
    onPlaybackEnded: () -> Unit = {},
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {}
) {
    val context = LocalContext.current
    val eventsRevision by com.example.data.IslamicEventManager.eventsRevision.collectAsState()
    var viewingMode by remember { mutableIntStateOf(0) } // 0 = Bilingual, 1 = Arabic Only, 2 = Translation Only
    val ziyaratBookmarks by viewModel.ziyaratBookmarks.collectAsState()
    
    // Fetch font scale preferences
    val fontScale = remember {
        val prefs = context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE)
        prefs.getFloat("arabic_urdu_font_scale", 1.0f)
    }

    // Recitation Repeat Counter State
    var repeatCount by remember { mutableIntStateOf(0) }

    // Dynamic sizing states
    var arabicFontSize by remember { mutableStateOf(18f * fontScale) }
    var translationFontSize by remember { mutableStateOf(12f * fontScale) }
    var showSettings by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(false) }

    var showEnglishTranslation by remember(languageCode) { mutableStateOf(languageCode != "ur") }
    var showUrduTranslation by remember(languageCode) { mutableStateOf(languageCode == "ur" || ziyarat.enText.isBlank()) }

    val prefs = remember { context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE) }
    var readingTheme by remember { mutableStateOf(prefs.getString("reading_theme", "Cream") ?: "Cream") }
    var arabicFontType by remember { mutableStateOf(prefs.getString("arabic_font_type", "Indo-Pak") ?: "Indo-Pak") }

    val bgThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFFF4ECD8)
        "Dark" -> Color(0xFF121212)
        else -> Color(0xFFFAF6EE)
    }
    val textThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFF433422)
        "Dark" -> Color(0xFFE0E0E0)
        else -> Color(0xFF0F2E27)
    }
    val arabicThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFF5D4037)
        "Dark" -> Color(0xFF81C784)
        else -> Color(0xFF1B4D3E)
    }
    val cardBgThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFFEFE5CD)
        "Dark" -> Color(0xFF1E1E1E)
        else -> Color.White
    }
    val cardBorderThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFFD4BFA7).copy(alpha = 0.5f)
        "Dark" -> Color(0xFF333333)
        else -> Color(0xFF0F2E27).copy(alpha = 0.08f)
    }
    val activeSelectorBgColor = when (readingTheme) {
        "Sepia" -> Color(0xFF433422)
        "Dark" -> Color(0xFFD4AF37)
        else -> Color(0xFF0F2E27)
    }
    val activeSelectorTextColor = when (readingTheme) {
        "Sepia" -> Color(0xFFFAF6EE)
        "Dark" -> Color(0xFF121212)
        else -> Color(0xFFFAF6EE)
    }

    var currentPlaybackPosition by remember { mutableStateOf(0L) }
    var currentPlaybackDuration by remember { mutableStateOf(0L) }
    
    val savedOffset = ziyaratBookmarks[ziyarat.id] ?: 0
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        initialFirstVisibleItemScrollOffset = savedOffset
    )

    val scrollProgress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems <= 1) {
                0f
            } else {
                val visibleItems = listState.layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) {
                    0f
                } else {
                    val lastVisibleItem = visibleItems.last()
                    if (lastVisibleItem.index == totalItems - 1 && 
                        lastVisibleItem.offset + lastVisibleItem.size <= listState.layoutInfo.viewportEndOffset) {
                        1f
                    } else {
                        val firstVisibleItem = listState.firstVisibleItemIndex
                        val firstVisibleItemOffset = listState.firstVisibleItemScrollOffset.toFloat()
                        val firstVisibleItemSize = visibleItems.first().size.toFloat()
                        val fraction = if (firstVisibleItemSize > 0f) firstVisibleItemOffset / firstVisibleItemSize else 0f
                        val progress = (firstVisibleItem + fraction) / (totalItems - 1)
                        progress.coerceIn(0f, 1f)
                    }
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { offset ->
                if (offset > 0 || (savedOffset != 0 && offset == 0)) {
                    viewModel.setZiyaratBookmark(ziyarat.id, offset)
                }
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(bgThemeColor),
            color = bgThemeColor
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (ziyarat.audioUrl.isNotBlank()) {
                        Box(
                            modifier = if (isFocusMode) Modifier.height(0.dp).alpha(0f) else Modifier
                        ) {
                            com.example.ui.components.AudioPlayerComponent(
                                audioUrl = ziyarat.audioUrl,
                                onProgressUpdate = { pos, dur ->
                                    currentPlaybackPosition = pos
                                    currentPlaybackDuration = dur
                                },
                                onPlaybackEnded = onPlaybackEnded,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                },
                topBar = {
                    androidx.compose.animation.AnimatedVisibility(visible = !isFocusMode) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = if (languageCode == "ur") ziyarat.titleUr else ziyarat.titleEn,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textThemeColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = if (languageCode == "ur") NotoNastaliqUrduFont else FontFamily.Default
                                    )
                                    val masoomName = remember(ziyarat.masoomIndex) {
                                        MasoomeenData.list.find { it.index == ziyarat.masoomIndex }?.name ?: ""
                                    }
                                    Text(
                                        text = masoomName,
                                        fontSize = 11.sp,
                                        color = Color(0xFFD4AF37),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.testTag("ziyarat_detail_back")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Close",
                                        tint = textThemeColor
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { isFocusMode = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Focus Mode",
                                        tint = textThemeColor
                                    )
                                }
                                IconButton(onClick = { showSettings = !showSettings }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = textThemeColor
                                    )
                                }
                                // Share / Card Maker export
                                IconButton(onClick = {
                                    val includeArabic = viewingMode == 0 || viewingMode == 1
                                    val includeTranslations = viewingMode == 0 || viewingMode == 2
                                    onEditCard(
                                        com.example.api.ShiaQuote(
                                            arabic = if (includeArabic) ziyarat.arText.take(1000) else "",
                                            english = if (includeTranslations) ziyarat.enText.take(1000) else "",
                                            urdu = if (includeTranslations) ziyarat.urText.take(1000) else "",
                                            source = ziyarat.titleEn
                                        )
                                    )
                                    onDismiss()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ColorLens,
                                        contentDescription = "Create Quote Card",
                                        tint = textThemeColor
                                    )
                                }

                                // Favorite Toggle
                                IconButton(onClick = { viewModel.toggleZiyaratFavorite(ziyarat) }) {
                                    Icon(
                                        imageVector = if (ziyarat.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (ziyarat.isFavorite) Color(0xFFE94057) else textThemeColor
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = bgThemeColor)
                        )
                    }
                },
                floatingActionButton = {
                    androidx.compose.animation.AnimatedVisibility(visible = isFocusMode) {
                        FloatingActionButton(
                            onClick = { isFocusMode = false },
                            containerColor = textThemeColor,
                            contentColor = bgThemeColor
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Focus Mode"
                            )
                        }
                    }
                }
            ) { overlayPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(overlayPadding)
                        .background(bgThemeColor)
                ) {
                    androidx.compose.animation.AnimatedVisibility(visible = showSettings) {
                    Column {
                    // Custom Glass-morphic Language Selector Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(textThemeColor.copy(alpha = 0.05f))
                            .padding(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val options = listOf("Bilingual", "Arabic Only", "Translations")
                            options.forEachIndexed { index, opt ->
                                val active = viewingMode == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) activeSelectorBgColor else Color.Transparent)
                                        .clickable { viewingMode = index }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) activeSelectorTextColor else textThemeColor
                                    )
                                }
                            }
                        }
                    }

                    // Custom Glass-morphic Translation Filter Chips Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(textThemeColor.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Filters:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textThemeColor.copy(alpha = 0.7f)
                            )

                            FilterChip(
                                selected = showEnglishTranslation,
                                onClick = { showEnglishTranslation = !showEnglishTranslation },
                                label = { Text("English", fontSize = 11.sp, color = if (showEnglishTranslation) Color.Black else textThemeColor) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD4AF37),
                                    selectedLabelColor = Color.Black
                                )
                            )

                            FilterChip(
                                selected = showUrduTranslation,
                                onClick = { showUrduTranslation = !showUrduTranslation },
                                label = { Text("اردو", fontSize = 11.sp, fontFamily = NotoNastaliqUrduFont, color = if (showUrduTranslation) Color.Black else textThemeColor) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD4AF37),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    // Custom Glass-morphic Theme Selector Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(textThemeColor.copy(alpha = 0.05f))
                            .padding(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Theme:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textThemeColor.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                            )
                            
                            val themes = listOf(
                                "Cream" to Color(0xFFFAF6EE),
                                "Sepia" to Color(0xFFF4ECD8),
                                "Dark" to Color(0xFF121212)
                            )
                            themes.forEach { (tName, tColor) ->
                                val isSelected = readingTheme == tName
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) activeSelectorBgColor else Color.Transparent)
                                        .clickable {
                                            readingTheme = tName
                                            prefs.edit().putString("reading_theme", tName).apply()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(tColor)
                                                .border(0.5.dp, Color.Gray, CircleShape)
                                        )
                                        Text(
                                            text = if (tName == "Dark") "Night" else tName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) activeSelectorTextColor else textThemeColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Professional Custom Font Size Sliders Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (readingTheme == "Dark") Color(0xFF1E1E1E) else textThemeColor.copy(alpha = 0.04f)),
                        border = BorderStroke(1.dp, if (readingTheme == "Sepia") Color(0xFFD4BFA7).copy(alpha = 0.5f) else if (readingTheme == "Dark") Color(0xFF333333) else Color(0xFF0F2E27).copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = "Font Sizer",
                                    tint = textThemeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Adjust Readability",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textThemeColor
                                )
                            }

                            // Arabic Font Size Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (languageCode == "ur") "عربی" else "Arabic",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textThemeColor.copy(alpha = 0.6f),
                                    modifier = Modifier.width(60.dp)
                                )
                                Slider(
                                    value = arabicFontSize,
                                    onValueChange = { arabicFontSize = it },
                                    valueRange = 18f..40f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFD4AF37),
                                        activeTrackColor = Color(0xFF0F2E27),
                                        inactiveTrackColor = textThemeColor.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                )
                                Text(
                                    text = "${arabicFontSize.toInt()}sp",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textThemeColor,
                                    modifier = Modifier.width(35.dp),
                                    textAlign = TextAlign.End
                                )
                            }

                            // Translation Font Size Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (languageCode == "ur") "ترجمہ" else "Meaning",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textThemeColor.copy(alpha = 0.6f),
                                    modifier = Modifier.width(60.dp)
                                )
                                Slider(
                                    value = translationFontSize,
                                    onValueChange = { translationFontSize = it },
                                    valueRange = 11f..25f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFD4AF37),
                                        activeTrackColor = Color(0xFF0F2E27),
                                        inactiveTrackColor = textThemeColor.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                )
                                Text(
                                    text = "${translationFontSize.toInt()}sp",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textThemeColor,
                                    modifier = Modifier.width(35.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                    
                    // Arabic Font Toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(textThemeColor.copy(alpha = 0.05f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Arabic Script: $arabicFontType", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textThemeColor)
                            Switch(
                                checked = (arabicFontType == "Indo-Pak"),
                                onCheckedChange = {
                                    val newType = if(it) "Indo-Pak" else "Uthmani"
                                    arabicFontType = newType
                                    prefs.edit().putString("arabic_font_type", newType).apply()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = activeSelectorBgColor, checkedTrackColor = textThemeColor.copy(alpha=0.3f))
                            )
                        }
                    }
                    }
                    }

                    // Recitation Repeat Counter Capsule
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (readingTheme == "Dark") Color(0xFFD4AF37).copy(alpha = 0.15f) else Color(0xFFD4AF37).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .clickable { repeatCount++ }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Recitation",
                                tint = textThemeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Recitation count: $repeatCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textThemeColor
                            )
                        }

                        if (repeatCount > 0) {
                            Text(
                                text = "Reset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textThemeColor.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clickable { repeatCount = 0 }
                                    .padding(8.dp)
                            )
                        }
                    }

                    // Text Body LazyColumn
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            val cardScale by animateFloatAsState(targetValue = if (isFocusMode) 1.01f else 1f, label = "cardScale")
                            val cardElevation by animateDpAsState(targetValue = if (isFocusMode) 8.dp else 2.dp, label = "cardElevation")
                            
                            Card(
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBgThemeColor),
                                border = BorderStroke(if (isFocusMode) 2.dp else 1.dp, if (isFocusMode) Color(0xFF4CAF50).copy(alpha = 0.5f) else cardBorderThemeColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(cardScale)
                                    .shadow(cardElevation, RoundedCornerShape(28.dp))
                            ) {

                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Arabic Script Section
                                    if (viewingMode == 0 || viewingMode == 1) {
                                        Text(
                                            text = ziyarat.arText,
                                            fontSize = arabicFontSize.sp,
                                            fontFamily = if (arabicFontType == "Indo-Pak") com.example.ui.theme.NoorehudaFont else AmiriFont,
                                            fontWeight = FontWeight.Bold,
                                            color = arabicThemeColor,
                                            lineHeight = (arabicFontSize * 1.6f).sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("ziyarat_arabic_text")
                                                .clickable {
                                                    isFocusMode = !isFocusMode
                                                }
                                        )
                                    }

                                    if (viewingMode == 0) {
                                        HorizontalDivider(
                                            color = Color(0xFFD4AF37).copy(alpha = 0.15f),
                                            thickness = 1.dp
                                        )
                                    }

                                    // Translations Section
                                    if (viewingMode == 0 || viewingMode == 2) {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            // English Section
                                            if (showEnglishTranslation && ziyarat.enText.isNotBlank()) {
                                                Column {
                                                    Text(
                                                        text = "ENGLISH TRANSLATION",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFD4AF37),
                                                        letterSpacing = 1.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = ziyarat.enText,
                                                        fontSize = translationFontSize.sp,
                                                        color = textThemeColor.copy(alpha = 0.85f),
                                                        lineHeight = (translationFontSize * 1.45f).sp,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                isFocusMode = !isFocusMode
                                                            }
                                                    )
                                                }
                                            }

                                            if (showEnglishTranslation && showUrduTranslation && ziyarat.enText.isNotBlank() && ziyarat.urText.isNotBlank()) {
                                                HorizontalDivider(
                                                    color = textThemeColor.copy(alpha = 0.05f),
                                                    thickness = 0.5.dp
                                                )
                                            }

                                            // Urdu Section
                                            if (showUrduTranslation && ziyarat.urText.isNotBlank()) {
                                                Column {
                                                    Text(
                                                        text = "اردو ترجمہ",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFD4AF37),
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        fontFamily = NotoNastaliqUrduFont
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = ziyarat.urText,
                                                        fontSize = (translationFontSize * 1.15f).sp,
                                                        color = textThemeColor,
                                                        fontFamily = NotoNastaliqUrduFont,
                                                        lineHeight = (translationFontSize * 1.8f).sp,
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                isFocusMode = !isFocusMode
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Historical Context Timeline Event Card
                        item {
                            val matchesEvents = remember(ziyarat.masoomIndex, eventsRevision) {
                                MasoomeenData.getEventsForMasoom(context, ziyarat.masoomIndex)
                            }

                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = textThemeColor.copy(alpha = 0.03f)),
                                border = BorderStroke(1.dp, textThemeColor.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(0.dp, RoundedCornerShape(24.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = when (languageCode) {
                                            "ur" -> "🕌 تاریخی پس منظر اور مقدس تواریخ"
                                            "ar" -> "🕌 السياق التاريخي والتواريخ المقدسة"
                                            else -> "🕌 HISTORICAL CONTEXT & HOLY DATES"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textThemeColor,
                                        letterSpacing = 1.sp
                                    )

                                    if (matchesEvents.isEmpty()) {
                                        Text(
                                            text = when (languageCode) {
                                                "ur" -> "یہ زیارتِ مبارکہ اہم اسلامی تواریخ، برسیوں اور جمعرات کو معصوم (ع) کی خدمت میں درود و سلام بھیجنے کے لیے پڑھنی انتہائی مستحب ہے۔"
                                                "ar" -> "تُستحب قراءة هذه الزيارة المباركة في المناسبات الإسلامية والذكرى السنوية وأيام الخميس لإهداء السلام إلى المعصوم (ع)."
                                                else -> "This sacred Ziyarah is highly recommended for recitation on major Islamic dates, anniversaries, and on Thursdays to convey spiritual salutations to the Holy Infallible (as)."
                                            },
                                            fontSize = 12.sp,
                                            color = textThemeColor.copy(alpha = 0.8f),
                                            lineHeight = 18.sp
                                        )
                                    } else {
                                        matchesEvents.forEach { ev ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(cardBgThemeColor.copy(alpha = 0.7f))
                                                    .border(0.5.dp, textThemeColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (ev.eventType == com.example.data.EventType.WILADAT) Icons.Default.Star else Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = if (ev.eventType == com.example.data.EventType.WILADAT) Color(0xFF00BFA5) else Color(0xFFE94057),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = ev.localizedTitle(languageCode),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textThemeColor
                                                    )
                                                    Text(
                                                        text = when (languageCode) {
                                                            "ur" -> "قمری مناسبت: ${com.example.utils.LocalizationUtility.translateHijriDateString(ev.dateStringHijri, languageCode)}"
                                                            "ar" -> "مناسبة قمرية: ${com.example.utils.LocalizationUtility.translateHijriDateString(ev.dateStringHijri, languageCode)}"
                                                            else -> "Lunar Calendar Event: ${ev.dateStringHijri}"
                                                        },
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFD4AF37),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Subtle Progress Indicator at the bottom of the long Ziyarat
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgThemeColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reading Progress",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textThemeColor.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${(scrollProgress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (readingTheme == "Dark") Color(0xFF81C784) else Color(0xFF1B4D3E)
                                )
                            }
                            // Progress track & bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(textThemeColor.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(scrollProgress)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFFD4AF37), arabicThemeColor)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

