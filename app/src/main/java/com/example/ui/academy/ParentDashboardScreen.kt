package com.example.ui.academy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ParentDashboardScreen(
    onBack: () -> Unit,
    currentLang: String,
    content: AcademyContent,
    repository: AcademyProgressRepository
) {
    var unlocked by remember { mutableStateOf(false) }
    var pinConfigured by remember { mutableStateOf(repository.hasParentPin()) }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }
    val progress by repository.progress.collectAsState()

    fun lock() {
        unlocked = false
        pin = ""
        confirmation = ""
        error = ""
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(tr(currentLang, "Reset all learning progress?", "تمام تعلیمی پیش رفت صاف کریں؟", "إعادة تعيين كل التقدم؟")) },
            text = { Text(tr(currentLang, "Points, lessons, duas, games, badges and streaks will be erased. Your parental PIN will stay unchanged.", "پوائنٹس، اسباق، دعائیں، کھیل، بیج اور اسٹریک صاف ہو جائیں گی۔ والدین کا پن نہیں بدلے گا۔", "ستحذف النقاط والدروس والأدعية والألعاب والشارات والسلسلة، ويبقى رمز الوالدين.")) },
            confirmButton = {
                TextButton(onClick = { repository.resetLearningProgress(); showResetConfirm = false }) {
                    Text(tr(currentLang, "Reset progress", "پیش رفت صاف کریں", "إعادة التعيين"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text(tr(currentLang, "Cancel", "منسوخ", "إلغاء")) } }
        )
    }

    Scaffold(
        topBar = {
            AcademyTopBar(
                title = tr(currentLang, "Parent Dashboard", "والدین کا ڈیش بورڈ", "لوحة الوالدين"),
                onBack = onBack,
                accent = if (unlocked) Color.White else Color(0xFF283593),
                currentLang = currentLang,
                actions = {
                    if (unlocked) {
                        IconButton(onClick = ::lock) {
                            Icon(Icons.Rounded.Lock, contentDescription = tr(currentLang, "Lock parent dashboard", "والدین ڈیش بورڈ بند کریں", "قفل لوحة الوالدين"), tint = Color.White)
                        }
                    }
                }
            )
        },
        containerColor = if (unlocked) Color(0xFF283593) else MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (unlocked) Color(0xFFF5F5F5) else MaterialTheme.colorScheme.background
            ).padding(padding)
        ) {
            if (!unlocked) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF283593), modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (pinConfigured) tr(currentLang, "Parent verification", "والدین کی تصدیق", "التحقق من الوالدين") else tr(currentLang, "Set up a parental PIN", "والدین کا پن بنائیں", "إنشاء رمز للوالدين"),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF283593),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        tr(currentLang, "Ask a parent. Reports and reset controls are protected; the PIN is stored only as a salted hash on this device.", "والدین سے کہیں۔ رپورٹس اور ری سیٹ محفوظ ہیں؛ پن صرف ڈیوائس پر محفوظ ہیش کی صورت میں رہتا ہے۔", "اطلب من أحد الوالدين. التقارير وإعادة التعيين محمية، ويحفظ الرمز كبصمة مشفرة على الجهاز فقط."),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(22.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all { digit -> digit in '0'..'9' }) pin = it },
                        label = { Text(tr(currentLang, "4–6 digit PIN", "4 تا 6 ہندسوں کا پن", "رمز من 4–6 أرقام")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!pinConfigured) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmation,
                            onValueChange = { if (it.length <= 6 && it.all { digit -> digit in '0'..'9' }) confirmation = it },
                            label = { Text(tr(currentLang, "Confirm PIN", "پن کی تصدیق", "تأكيد الرمز")) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (pinConfigured) {
                                if (repository.verifyParentPin(pin)) {
                                    unlocked = true
                                    error = ""
                                    pin = ""
                                } else error = tr(currentLang, "Incorrect PIN. Please try again.", "پن غلط ہے۔ دوبارہ کوشش کریں۔", "الرمز غير صحيح. حاول مجدداً.")
                            } else if (pin.length !in 4..6) {
                                error = tr(currentLang, "Use 4 to 6 digits.", "4 تا 6 ہندسے استعمال کریں۔", "استخدم من 4 إلى 6 أرقام.")
                            } else if (pin != confirmation) {
                                error = tr(currentLang, "The PINs do not match.", "دونوں پن ایک جیسے نہیں۔", "الرمزان غير متطابقين.")
                            } else if (repository.setParentPin(pin)) {
                                pinConfigured = true
                                unlocked = true
                                pin = ""
                                confirmation = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF283593))
                    ) {
                        Text(if (pinConfigured) tr(currentLang, "Unlock", "کھولیں", "فتح") else tr(currentLang, "Create PIN & continue", "پن بنا کر جاری رکھیں", "إنشاء الرمز والمتابعة"))
                    }
                }
            } else {
                val salahPercent = moduleLessonPercent(progress, "salah", content.salah.size)
                val wuduPercent = moduleLessonPercent(progress, "wudu", content.wudu.size)
                val storyPercent = percent(progress.completedLessons.count { it.startsWith("story:") }, content.stories.size)
                val aqaidPercent = percent(progress.completedLessons.count { it.startsWith("aqaid:") }, content.aqaid.size)
                val duaPercent = percent(progress.readDuas.size, content.duas.size)
                val gamePercent = percent(progress.completedGames.size, 4)
                val quizPercent = if (progress.bestQuizTotal > 0) percent(progress.bestQuizScore, progress.bestQuizTotal) else 0
                val badges = earnedBadges(progress, content, currentLang)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(tr(currentLang, "Child's real progress", "بچے کی حقیقی پیش رفت", "التقدم الحقيقي للطفل"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                        Text(tr(currentLang, "Saved on this device and updated by completed activities", "ڈیوائس پر محفوظ اور مکمل سرگرمیوں سے اپڈیٹ", "محفوظ على الجهاز ويتحدث بالأنشطة المكتملة"), fontSize = 13.sp, color = Color.DarkGray)
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                SummaryRow(Icons.Rounded.Star, tr(currentLang, "Total Shia Points", "کل شیعہ پوائنٹس", "مجموع النقاط"), "${progress.points}", Color(0xFFEF6C00))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                SummaryRow(Icons.Rounded.LocalFireDepartment, tr(currentLang, "Daily Streak", "روزانہ اسٹریک", "السلسلة اليومية"), "${progress.currentStreak} " + tr(currentLang, "days", "دن", "أيام"), Color(0xFF3F51B5))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                SummaryRow(Icons.Rounded.CalendarMonth, tr(currentLang, "Active learning days", "فعال تعلیمی دن", "أيام التعلم النشطة"), "${progress.activityDates.size}", Color(0xFF00838F))
                            }
                        }
                    }
                    item {
                        Text(tr(currentLang, "Module progress", "ماڈیول پیش رفت", "تقدم الوحدات"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                                StatBar(tr(currentLang, "Learn Salah", "نماز", "الصلاة"), salahPercent, Color(0xFF7B1FA2))
                                StatBar(tr(currentLang, "Learn Wudu", "وضو", "الوضوء"), wuduPercent, Color(0xFF0288D1))
                                StatBar(tr(currentLang, "Daily Duas", "دعائیں", "الأدعية") + " (${progress.readDuas.size}/${content.duas.size})", duaPercent, Color(0xFFC2185B))
                                StatBar(tr(currentLang, "Ahlul Bayt Stories", "اہل بیت کہانیاں", "قصص أهل البيت") + " (${progress.completedLessons.count { it.startsWith("story:") }}/${content.stories.size})", storyPercent, Color(0xFF2E7D32))
                                StatBar(tr(currentLang, "Aqaid", "عقائد", "العقائد") + " (${progress.completedLessons.count { it.startsWith("aqaid:") }}/${content.aqaid.size})", aqaidPercent, Color(0xFFEF6C00))
                                StatBar(tr(currentLang, "Mini Games", "کھیل", "الألعاب") + " (${progress.completedGames.size}/4)", gamePercent, Color(0xFFC62828))
                                StatBar(tr(currentLang, "Quiz best score", "بہترین کوئز اسکور", "أفضل نتيجة اختبار") + if (progress.bestQuizTotal > 0) " (${progress.bestQuizScore}/${progress.bestQuizTotal})" else "", quizPercent, Color(0xFF00838F))
                            }
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(tr(currentLang, "Today's missions", "آج کے مشن", "مهام اليوم"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A237E))
                                Spacer(Modifier.height(8.dp))
                                ParentMissionRow(tr(currentLang, "Dua", "دعا", "دعاء"), progress.daily.dua, currentLang)
                                ParentMissionRow(tr(currentLang, "Quiz", "کوئز", "اختبار"), progress.daily.quiz, currentLang)
                                ParentMissionRow(tr(currentLang, "Lesson or game", "سبق یا کھیل", "درس أو لعبة"), progress.daily.learning, currentLang)
                                ParentMissionRow(tr(currentLang, "Kindness at home", "گھر میں مہربانی", "اللطف في المنزل"), progress.daily.kindness, currentLang)
                            }
                        }
                    }
                    item {
                        Text(tr(currentLang, "Earned badges", "حاصل شدہ بیج", "الشارات المكتسبة"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                    }
                    if (badges.isEmpty()) {
                        item {
                            Text(tr(currentLang, "No badges yet. They will appear only after real achievements.", "ابھی کوئی بیج نہیں۔ حقیقی کامیابی کے بعد ظاہر ہوگا۔", "لا شارات بعد. ستظهر بعد الإنجازات الحقيقية فقط."), color = Color.DarkGray, lineHeight = 21.sp)
                        }
                    } else {
                        items(badges.size) { index -> BadgeIcon(badges[index], Color(0xFF6A1B9A)) }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(tr(currentLang, "Content & safety", "مواد اور حفاظت", "المحتوى والسلامة"), fontWeight = FontWeight.Bold, color = Color(0xFF1A237E), fontSize = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${content.duas.size} " + tr(currentLang, "duas", "دعائیں", "أدعية") + " • ${content.stories.size} " + tr(currentLang, "stories", "کہانیاں", "قصص") + " • ${content.quiz.size} " + tr(currentLang, "quiz questions", "کوئز سوالات", "أسئلة") + "\n" +
                                        tr(currentLang, "English, Urdu and Arabic • no ads or child chat • progress stays on-device. Read-aloud uses an installed device voice. Fiqh lessons are introductory and show their sources; detailed rulings should follow your marja.", "انگریزی، اردو اور عربی • کوئی اشتہار یا بچوں کی چیٹ نہیں • پیش رفت ڈیوائس پر رہتی ہے۔ بلند خوانی ڈیوائس کی آواز استعمال کرتی ہے۔ فقہی اسباق ابتدائی اور ماخذ کے ساتھ ہیں؛ تفصیلی احکام اپنے مرجع سے لیں۔", "إنجليزية وعربية وأردية • لا إعلانات أو دردشة للأطفال • التقدم على الجهاز. القراءة الصوتية تستخدم صوت الجهاز. دروس الفقه تمهيدية بمصادرها، والتفاصيل تتبع المرجع."),
                                    color = Color.DarkGray,
                                    lineHeight = 21.sp,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(tr(currentLang, "Reset learning progress", "تعلیمی پیش رفت صاف کریں", "إعادة تعيين التقدم"))
                        }
                    }
                }
            }
        }
    }
}

private fun moduleLessonPercent(progress: AcademyProgress, id: String, steps: Int): Int =
    if (id in progress.completedLessons) 100 else percent(progress.lessonSteps[id] ?: 0, steps + 1)

private fun percent(completed: Int, total: Int): Int = if (total <= 0) 0 else (completed.coerceIn(0, total) * 100 / total)

@Composable
private fun SummaryRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
fun StatBar(label: String, percentage: Int, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text("$percentage%", fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun ParentMissionRow(label: String, complete: Boolean, currentLang: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (complete) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, contentDescription = null, tint = if (complete) Color(0xFF2E7D32) else Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), color = Color.DarkGray)
        Text(if (complete) tr(currentLang, "Done", "مکمل", "مكتمل") else tr(currentLang, "Pending", "باقی", "معلق"), fontSize = 12.sp, color = if (complete) Color(0xFF2E7D32) else Color.Gray)
    }
}

@Composable
fun BadgeIcon(label: String, color: Color) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        }
    }
}

private fun earnedBadges(progress: AcademyProgress, content: AcademyContent, currentLang: String): List<String> = buildList {
    if ("wudu" in progress.completedLessons) add(tr(currentLang, "Wudu Master", "وضو ماسٹر", "بطل الوضوء"))
    if ("salah" in progress.completedLessons) add(tr(currentLang, "Prayer Hero", "نماز ہیرو", "بطل الصلاة"))
    content.aqaid.forEach { lesson ->
        if ("aqaid:${lesson.id}" in progress.completedLessons) add(lesson.badge.get(currentLang))
    }
    content.stories.forEach { story ->
        if ("story:${story.id}" in progress.completedLessons) add(story.badge.get(currentLang))
    }
    if (progress.completedLessons.count { it.startsWith("aqaid:") } >= content.aqaid.size) add(tr(currentLang, "Aqaid Explorer", "عقائد ایکسپلورر", "مستكشف العقائد"))
    if (progress.completedLessons.count { it.startsWith("story:") } >= content.stories.size) add(tr(currentLang, "Ahlul Bayt Story Collector", "اہل بیت اسٹوری کلیکٹر", "جامع قصص أهل البيت"))
    if (progress.readDuas.size >= 5) add(tr(currentLang, "Dua Learner", "دعا سیکھنے والا", "متعلم الأدعية"))
    if (progress.bestQuizTotal > 0 && progress.bestQuizScore * 100 / progress.bestQuizTotal >= 80) add(tr(currentLang, "Quiz Champion", "کوئز چیمپیئن", "بطل الاختبار"))
    if (progress.completedGames.size >= 4) add(tr(currentLang, "Learning Games Star", "تعلیمی کھیلوں کا ستارہ", "نجمة الألعاب التعليمية"))
    if (progress.currentStreak >= 7) add(tr(currentLang, "Seven-Day Streak", "سات روزہ اسٹریک", "سلسلة سبعة أيام"))
}
