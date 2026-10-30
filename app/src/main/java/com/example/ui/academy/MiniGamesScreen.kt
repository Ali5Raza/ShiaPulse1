package com.example.ui.academy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun MiniGamesScreen(
    onBack: () -> Unit,
    currentLang: String,
    content: AcademyContent,
    repository: AcademyProgressRepository
) {
    var activeGame by rememberSaveable { mutableStateOf("") }
    val progress by repository.progress.collectAsState()

    fun handleBack() {
        if (activeGame.isNotEmpty()) activeGame = "" else onBack()
    }
    BackHandler { handleBack() }

    Scaffold(
        topBar = {
            AcademyTopBar(
                title = tr(currentLang, "Mini Games", "تعلیمی کھیل", "ألعاب تعليمية"),
                onBack = ::handleBack,
                accent = Color(0xFFC62828),
                currentLang = currentLang
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            when (activeGame) {
                "memory" -> MemoryGame(onFinish = { activeGame = "" }, currentLang = currentLang, repository = repository)
                "puzzle" -> WuduPuzzleGame(onFinish = { activeGame = "" }, currentLang = currentLang, content = content, repository = repository)
                "drag" -> SalahOrderGame(onFinish = { activeGame = "" }, currentLang = currentLang, content = content, repository = repository)
                "truefalse" -> TrueFalseGame(onFinish = { activeGame = "" }, currentLang = currentLang, content = content, repository = repository)
                else -> GamesListContent(
                    currentLang = currentLang,
                    completedGames = progress.completedGames,
                    onGameClick = { activeGame = it }
                )
            }
        }
    }
}

@Composable
fun GamesListContent(currentLang: String, completedGames: Set<String>, onGameClick: (String) -> Unit) {
    val games = listOf(
        GameInfo("memory", tr(currentLang, "Memory Match", "میموری میچ", "لعبة الذاكرة"), Icons.Rounded.Dashboard),
        GameInfo("puzzle", tr(currentLang, "Wudu Puzzle", "وضو پزل", "ترتيب الوضوء"), Icons.Rounded.WaterDrop),
        GameInfo("drag", tr(currentLang, "Drag & Drop Salah", "نماز کی ترتیب", "ترتيب الصلاة"), Icons.Rounded.Mosque),
        GameInfo("truefalse", tr(currentLang, "True or False", "صحیح یا غلط", "صح أم خطأ"), Icons.Rounded.Quiz)
    )
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            tr(currentLang, "Four games. Every one is ready to play!", "چاروں کھیل تیار ہیں!", "أربع ألعاب جاهزة للعب!"),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFC62828),
            lineHeight = 33.sp
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(games.size) { index ->
                val game = games[index]
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 175.dp).clickable { onGameClick(game.id) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(62.dp).clip(CircleShape).background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(game.icon, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(34.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(game.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828), textAlign = TextAlign.Center, lineHeight = 24.sp)
                        if (game.id in completedGames) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(tr(currentLang, "Completed", "مکمل", "مكتمل"), fontSize = 12.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class GameInfo(val id: String, val title: String, val icon: ImageVector)

@Composable
fun MemoryGame(onFinish: () -> Unit, currentLang: String, repository: AcademyProgressRepository) {
    var deckCsv by rememberSaveable { mutableStateOf(shuffledDeck()) }
    val deck = remember(deckCsv) { deckCsv.split(",").mapNotNull { it.toIntOrNull() } }
    var matchedMask by rememberSaveable { mutableStateOf(0) }
    var firstIndex by rememberSaveable { mutableStateOf(-1) }
    var secondIndex by rememberSaveable { mutableStateOf(-1) }
    var moves by rememberSaveable { mutableStateOf(0) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var pointsAwarded by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(secondIndex) {
        if (secondIndex >= 0 && firstIndex >= 0) {
            delay(650)
            val newMask = if (deck[firstIndex] == deck[secondIndex]) {
                matchedMask or (1 shl firstIndex) or (1 shl secondIndex)
            } else matchedMask
            matchedMask = newMask
            firstIndex = -1
            secondIndex = -1
            if (newMask == 0xFF && !completed) {
                pointsAwarded = repository.recordGame("memory")
                completed = true
            }
        }
    }

    if (completed) {
        GameResult(
            title = tr(currentLang, "Every pair matched!", "تمام جوڑے مل گئے!", "طابقت كل الأزواج!"),
            message = tr(currentLang, "Moves", "حرکتیں", "الحركات") + ": $moves",
            pointsAwarded = pointsAwarded,
            currentLang = currentLang,
            onReplay = {
                deckCsv = shuffledDeck(); matchedMask = 0; firstIndex = -1; secondIndex = -1
                moves = 0; pointsAwarded = 0; completed = false
            },
            onFinish = onFinish
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(tr(currentLang, "Find the matching pairs", "ایک جیسے جوڑے تلاش کریں", "اعثر على الأزواج المتشابهة"), fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(tr(currentLang, "Moves", "حرکتیں", "الحركات") + ": $moves", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(deck.size) { index ->
                val isMatched = matchedMask and (1 shl index) != 0
                val isFaceUp = isMatched || index == firstIndex || index == secondIndex
                MemoryCardView(
                    icon = memoryIcon(deck[index]),
                    isFaceUp = isFaceUp,
                    isMatched = isMatched,
                    contentDescription = if (isFaceUp) {
                        tr(currentLang, "Card", "کارڈ", "بطاقة") + " ${index + 1}: " + memoryLabel(deck[index], currentLang)
                    } else {
                        tr(currentLang, "Card", "کارڈ", "بطاقة") + " ${index + 1}: " + tr(currentLang, "hidden", "چھپا ہوا", "مخفية")
                    },
                    onClick = {
                        if (secondIndex < 0 && !isMatched && index != firstIndex) {
                            if (firstIndex < 0) firstIndex = index else {
                                secondIndex = index
                                moves++
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun shuffledDeck(): String = (listOf(0, 1, 2, 3) + listOf(0, 1, 2, 3)).shuffled().joinToString(",")

private fun memoryIcon(id: Int): ImageVector = when (id) {
    0 -> Icons.Rounded.Star
    1 -> Icons.Rounded.Favorite
    2 -> Icons.Rounded.WbSunny
    else -> Icons.Rounded.Mosque
}

private fun memoryLabel(id: Int, currentLang: String): String = when (id) {
    0 -> tr(currentLang, "star", "ستارہ", "نجمة")
    1 -> tr(currentLang, "heart", "دل", "قلب")
    2 -> tr(currentLang, "sun", "سورج", "شمس")
    else -> tr(currentLang, "mosque", "مسجد", "مسجد")
}

@Composable
fun MemoryCardView(icon: ImageVector, isFaceUp: Boolean, isMatched: Boolean, contentDescription: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.25f).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isMatched -> Color(0xFFC8E6C9)
                isFaceUp -> MaterialTheme.colorScheme.surface
                else -> Color(0xFFE53935)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                if (isFaceUp) icon else Icons.Rounded.HelpOutline,
                contentDescription = contentDescription,
                tint = when {
                    isMatched -> Color(0xFF2E7D32)
                    isFaceUp -> Color(0xFFC62828)
                    else -> Color.White
                },
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
fun WuduPuzzleGame(onFinish: () -> Unit, currentLang: String, content: AcademyContent, repository: AcademyProgressRepository) {
    var remainingCsv by rememberSaveable { mutableStateOf(content.wudu.map { it.id }.shuffled().joinToString(",")) }
    val remaining = remainingCsv.split(",").mapNotNull { it.toIntOrNull() }
    var wrongAttempt by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var pointsAwarded by rememberSaveable { mutableStateOf(0) }
    val expectedNumber = content.wudu.size - remaining.size + 1

    if (completed) {
        GameResult(
            title = tr(currentLang, "Wudu order completed!", "وضو کی ترتیب مکمل!", "اكتمل ترتيب الوضوء!"),
            message = tr(currentLang, "You placed every step in its correct position.", "آپ نے ہر مرحلہ درست جگہ رکھا۔", "وضعت كل خطوة في مكانها الصحيح."),
            pointsAwarded = pointsAwarded,
            currentLang = currentLang,
            onReplay = {
                remainingCsv = content.wudu.map { it.id }.shuffled().joinToString(",")
                wrongAttempt = false; completed = false; pointsAwarded = 0
            },
            onFinish = onFinish
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(tr(currentLang, "Tap the steps in Wudu order", "وضو کی ترتیب سے مراحل دبائیں", "اضغط خطوات الوضوء بالترتيب"), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00796B), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(tr(currentLang, "Find step", "مرحلہ تلاش کریں", "ابحث عن الخطوة") + " $expectedNumber / ${content.wudu.size}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            if (wrongAttempt) {
                Text(tr(currentLang, "That step comes later. Try another one.", "یہ مرحلہ بعد میں آتا ہے۔ دوسرا آزمائیں۔", "هذه الخطوة تأتي لاحقاً. جرب غيرها."), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
        items(remaining, key = { it }) { id ->
            val step = content.wudu.first { it.id == id }
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (id == expectedNumber) {
                        wrongAttempt = false
                        val updated = remaining - id
                        remainingCsv = updated.joinToString(",")
                        if (updated.isEmpty()) {
                            pointsAwarded = repository.recordGame("puzzle")
                            completed = true
                        }
                    } else wrongAttempt = true
                },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Color(0xFF00796B))
                    Spacer(Modifier.width(14.dp))
                    Text(step.title.get(currentLang), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                }
            }
        }
    }
}

@Composable
fun SalahOrderGame(onFinish: () -> Unit, currentLang: String, content: AcademyContent, repository: AcademyProgressRepository) {
    val steps = remember(content) {
        listOf(3, 4, 6, 7, 8, 9).mapNotNull { id -> content.salah.firstOrNull { it.id == id } }
    }
    var orderCsv by rememberSaveable { mutableStateOf(nonSortedOrder(steps.map { it.id })) }
    var draggedId by remember { mutableStateOf(-1) }
    var dragDistance by remember { mutableStateOf(0f) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var pointsAwarded by rememberSaveable { mutableStateOf(0) }
    val order = orderCsv.split(",").mapNotNull { it.toIntOrNull() }
    val rowDistancePx = with(LocalDensity.current) { 92.dp.toPx() }

    fun moveStep(id: Int, targetIndex: Int) {
        val mutable = order.toMutableList()
        val oldIndex = mutable.indexOf(id)
        if (oldIndex < 0) return
        mutable.removeAt(oldIndex)
        mutable.add(targetIndex.coerceIn(0, mutable.size), id)
        orderCsv = mutable.joinToString(",")
        showError = false
    }

    if (completed) {
        GameResult(
            title = tr(currentLang, "Salah order completed!", "نماز کی ترتیب مکمل!", "اكتمل ترتيب الصلاة!"),
            message = tr(currentLang, "You ordered the main prayer actions correctly.", "آپ نے نماز کے اہم افعال درست ترتیب دی۔", "رتبت أفعال الصلاة الأساسية بشكل صحيح."),
            pointsAwarded = pointsAwarded,
            currentLang = currentLang,
            onReplay = {
                orderCsv = nonSortedOrder(steps.map { it.id }); showError = false; completed = false; pointsAwarded = 0
            },
            onFinish = onFinish
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr(currentLang, "Put the Salah actions in order", "نماز کے افعال ترتیب دیں", "رتب أفعال الصلاة"), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6A1B9A), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(tr(currentLang, "Drag a card, or use its up and down buttons.", "کارڈ گھسیٹیں یا اوپر نیچے کے بٹن استعمال کریں۔", "اسحب البطاقة أو استعمل زري الصعود والنزول."), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        order.forEachIndexed { index, id ->
            val step = steps.first { it.id == id }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .zIndex(if (draggedId == id) 1f else 0f)
                    .graphicsLayer { translationY = if (draggedId == id) dragDistance else 0f }
                    .pointerInput(orderCsv, id) {
                        var totalDrag = 0f
                        detectDragGestures(
                            onDragStart = {
                                draggedId = id
                                totalDrag = 0f
                                dragDistance = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                totalDrag += amount.y
                                dragDistance = totalDrag
                            },
                            onDragEnd = {
                                val shift = (totalDrag / rowDistancePx).roundToInt()
                                if (shift != 0) moveStep(id, index + shift)
                                draggedId = -1
                                dragDistance = 0f
                            },
                            onDragCancel = { draggedId = -1; dragDistance = 0f }
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = if (draggedId == id) Color(0xFFE1BEE7) else Color(0xFFF3E5F5))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DragIndicator, contentDescription = tr(currentLang, "Drag to reorder", "ترتیب کے لیے گھسیٹیں", "اسحب لإعادة الترتيب"), tint = Color(0xFF6A1B9A))
                    Spacer(Modifier.width(8.dp))
                    Text(step.title.get(currentLang), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A), fontSize = 16.sp, lineHeight = 22.sp)
                    IconButton(onClick = { moveStep(id, index - 1) }, enabled = index > 0) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = tr(currentLang, "Move up", "اوپر کریں", "تحريك لأعلى"))
                    }
                    IconButton(onClick = { moveStep(id, index + 1) }, enabled = index < order.lastIndex) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = tr(currentLang, "Move down", "نیچے کریں", "تحريك لأسفل"))
                    }
                }
            }
        }
        if (showError) {
            Text(tr(currentLang, "Not quite. Check Takbir, Ruku, Sujud and the final Salam.", "ابھی درست نہیں۔ تکبیر، رکوع، سجدہ اور آخری سلام دیکھیں۔", "ليس بعد. راجع التكبير والركوع والسجود والسلام الأخير."), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
        }
        Button(
            onClick = {
                if (order == steps.map { it.id }) {
                    pointsAwarded = repository.recordGame("drag")
                    completed = true
                } else showError = true
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
        ) { Text(tr(currentLang, "Check my order", "میری ترتیب جانچیں", "تحقق من الترتيب")) }
    }
}

private fun nonSortedOrder(ids: List<Int>): String {
    if (ids.size < 2) return ids.joinToString(",")
    var shuffled = ids.shuffled()
    var attempts = 0
    while (shuffled == ids && attempts < 5) {
        shuffled = ids.shuffled()
        attempts++
    }
    if (shuffled == ids) shuffled = ids.reversed()
    return shuffled.joinToString(",")
}

@Composable
fun TrueFalseGame(onFinish: () -> Unit, currentLang: String, content: AcademyContent, repository: AcademyProgressRepository) {
    var seed by rememberSaveable { mutableStateOf(Random.nextInt()) }
    val questions = remember(seed, content) { content.trueFalse.shuffled(Random(seed)).take(8) }
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    var score by rememberSaveable { mutableStateOf(0) }
    var selectedAnswer by rememberSaveable { mutableStateOf(-1) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var pointsAwarded by rememberSaveable { mutableStateOf(0) }

    if (completed) {
        GameResult(
            title = tr(currentLang, "True or False complete!", "صحیح یا غلط مکمل!", "اكتمل صح أم خطأ!"),
            message = tr(currentLang, "Score", "اسکور", "النتيجة") + ": $score / ${questions.size}",
            pointsAwarded = pointsAwarded,
            currentLang = currentLang,
            onReplay = {
                seed = Random.nextInt(); currentIndex = 0; score = 0; selectedAnswer = -1
                completed = false; pointsAwarded = 0
            },
            onFinish = onFinish
        )
        return
    }

    val question = questions[currentIndex]
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(tr(currentLang, "Statement", "جملہ", "العبارة") + " ${currentIndex + 1} / ${questions.size}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(question.statement.get(currentLang), modifier = Modifier.padding(26.dp), fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 34.sp)
        }
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { if (selectedAnswer < 0) selectedAnswer = 1 },
                modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                enabled = selectedAnswer < 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
            ) { Text(tr(currentLang, "True", "صحیح", "صح"), fontSize = 18.sp) }
            Button(
                onClick = { if (selectedAnswer < 0) selectedAnswer = 0 },
                modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                enabled = selectedAnswer < 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) { Text(tr(currentLang, "False", "غلط", "خطأ"), fontSize = 18.sp) }
        }
        if (selectedAnswer >= 0) {
            val isCorrect = (selectedAnswer == 1) == question.answer
            Spacer(Modifier.height(18.dp))
            Text(
                if (isCorrect) tr(currentLang, "Correct!", "درست!", "صحيح!") else tr(currentLang, "Not this time—the right answer is shown.", "اس بار نہیں — صحیح جواب دیکھیں۔", "ليس هذه المرة؛ انظر الإجابة الصحيحة."),
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Text(
                tr(currentLang, "Correct answer: ", "درست جواب: ", "الإجابة الصحيحة: ") + if (question.answer) tr(currentLang, "True", "صحیح", "صح") else tr(currentLang, "False", "غلط", "خطأ"),
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = {
                val newScore = score + if (isCorrect) 1 else 0
                score = newScore
                selectedAnswer = -1
                if (currentIndex < questions.lastIndex) currentIndex++ else {
                    pointsAwarded = repository.recordGame("truefalse")
                    completed = true
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                Text(if (currentIndex == questions.lastIndex) tr(currentLang, "See result", "نتیجہ دیکھیں", "عرض النتيجة") else tr(currentLang, "Next statement", "اگلا جملہ", "العبارة التالية"))
            }
        }
    }
}

@Composable
fun GameResult(
    title: String,
    message: String,
    pointsAwarded: Int,
    currentLang: String,
    onReplay: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828), textAlign = TextAlign.Center, lineHeight = 39.sp)
        Spacer(Modifier.height(10.dp))
        Text(message, fontSize = 18.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(18.dp))
        Text(
            if (pointsAwarded > 0) "+$pointsAwarded " + tr(currentLang, "Shia Points", "شیعہ پوائنٹس", "نقاط شيعة") else tr(currentLang, "Completion saved • reward already collected", "مکمل محفوظ • انعام پہلے مل چکا", "تم الحفظ • المكافأة محصلة"),
            fontWeight = FontWeight.Bold,
            color = if (pointsAwarded > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))
        Button(onClick = onReplay, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(tr(currentLang, "Play again", "دوبارہ کھیلیں", "العب مجدداً")) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(tr(currentLang, "Games menu", "کھیلوں کا مینو", "قائمة الألعاب")) }
    }
}
