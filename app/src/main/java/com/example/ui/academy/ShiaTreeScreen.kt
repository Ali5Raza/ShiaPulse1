package com.example.ui.academy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ShiaTreeScreen(
    onBack: () -> Unit,
    currentLang: String,
    progress: AcademyProgress
) {
    val leaves = progress.completedLessons.size + progress.readDuas.size
    val flowers = progress.kindnessDates.size
    val fruits = progress.completedGames.size + progress.quizAttempts
    val birds = progress.currentStreak

    Scaffold(
        topBar = {
            AcademyTopBar(
                title = tr(currentLang, "My Growing Tree", "میرا بڑھتا درخت", "شجرتي النامية"),
                onBack = onBack,
                accent = Color(0xFF1B5E20),
                currentLang = currentLang
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF81D4FA), Color(0xFFE1F5FE), Color(0xFFAED581)))
            ).padding(padding)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    TreeStatItem(tr(currentLang, "Leaves", "پتے", "أوراق"), leaves, Color(0xFF2E7D32), tr(currentLang, "Learning", "سیکھنا", "التعلم"))
                    TreeStatItem(tr(currentLang, "Flowers", "پھول", "زهور"), flowers, Color(0xFFC2185B), tr(currentLang, "Kindness", "مہربانی", "اللطف"))
                    TreeStatItem(tr(currentLang, "Fruits", "پھل", "ثمار"), fruits, Color(0xFFEF6C00), tr(currentLang, "Challenges", "چیلنج", "التحديات"))
                    TreeStatItem(tr(currentLang, "Birds", "پرندے", "طيور"), birds, Color(0xFF0288D1), tr(currentLang, "Streak", "اسٹریک", "السلسلة"))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                TreeCanvas(
                    leaves = leaves,
                    flowers = flowers,
                    fruits = fruits,
                    birds = birds,
                    modifier = Modifier.fillMaxSize()
                )
                if (leaves + flowers + fruits == 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            tr(currentLang, "Complete a lesson, read a dua or do a kindness to help your tree grow.", "درخت بڑھانے کے لیے سبق مکمل کریں، دعا پڑھیں یا مہربانی کریں۔", "أكمل درساً أو اقرأ دعاءً أو افعل لطفاً لتنمو شجرتك."),
                            modifier = Modifier.padding(18.dp),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Surface(modifier = Modifier.fillMaxWidth().padding(12.dp), color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(18.dp)) {
                Text(
                    tr(currentLang, "Every number is real: leaves = lessons & duas • flowers = days of kindness • fruits = quizzes & games • birds = current streak.", "ہر عدد حقیقی ہے: پتے = اسباق و دعائیں • پھول = مہربانی کے دن • پھل = کوئز و کھیل • پرندے = موجودہ اسٹریک۔", "كل رقم حقيقي: الأوراق = الدروس والأدعية • الزهور = أيام اللطف • الثمار = الاختبارات والألعاب • الطيور = السلسلة الحالية."),
                    modifier = Modifier.padding(14.dp),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF33691E)
                )
            }
        }
    }
}

@Composable
fun TreeStatItem(label: String, count: Int, color: Color, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 82.dp)) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Text("$count", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
        Text(subtitle, fontSize = 10.sp, color = Color.DarkGray, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
fun TreeCanvas(leaves: Int, flowers: Int, fruits: Int, birds: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val rootX = width / 2
        val rootY = height * 0.94f
        val totalGrowth = leaves + flowers + fruits
        val depth = when {
            totalGrowth == 0 -> 3
            totalGrowth < 5 -> 4
            totalGrowth < 15 -> 5
            else -> 6
        }
        val trunkLength = height * when (depth) {
            3 -> 0.23f
            4 -> 0.27f
            5 -> 0.30f
            else -> 0.32f
        }

        val grassPath = Path().apply {
            moveTo(0f, height * 0.86f)
            quadraticBezierTo(width / 2, height * 0.77f, width, height * 0.86f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(grassPath, Color(0xFF8BC34A))

        var leafBudget = leaves.coerceAtMost(70)
        var flowerBudget = flowers.coerceAtMost(18)
        var fruitBudget = fruits.coerceAtMost(18)

        fun drawBranch(startX: Float, startY: Float, length: Float, angle: Float, remainingDepth: Int) {
            if (remainingDepth == 0) return
            val endX = startX + length * sin(angle)
            val endY = startY - length * cos(angle)
            drawLine(
                color = Color(0xFF5D4037),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = (remainingDepth * 4.5f).coerceAtLeast(3f)
            )
            if (remainingDepth <= 3 && leafBudget > 0) {
                drawCircle(Color(0xFF43A047).copy(alpha = 0.88f), radius = 16f + remainingDepth * 3f, center = Offset(endX, endY))
                leafBudget--
                if (flowerBudget > 0 && remainingDepth == 2) {
                    drawCircle(Color(0xFFE91E63), radius = 9f, center = Offset(endX + 13f, endY - 12f))
                    drawCircle(Color(0xFFF8BBD0), radius = 4f, center = Offset(endX + 13f, endY - 12f))
                    flowerBudget--
                }
                if (fruitBudget > 0 && remainingDepth == 1) {
                    drawCircle(Color(0xFFFF9800), radius = 11f, center = Offset(endX - 10f, endY + 9f))
                    fruitBudget--
                }
            }
            drawBranch(endX, endY, length * 0.69f, angle - 0.52f, remainingDepth - 1)
            drawBranch(endX, endY, length * 0.69f, angle + 0.52f, remainingDepth - 1)
            if (remainingDepth > 3) drawBranch(endX, endY, length * 0.55f, angle, remainingDepth - 1)
        }

        drawBranch(rootX, rootY, trunkLength, 0f, depth)

        repeat(birds.coerceIn(0, 4)) { index ->
            val x = width * (0.16f + index * 0.21f)
            val y = height * (0.12f + (index % 2) * 0.09f)
            val birdPath = Path().apply {
                moveTo(x - 15f, y)
                quadraticBezierTo(x - 7f, y - 10f, x, y)
                quadraticBezierTo(x + 7f, y - 10f, x + 15f, y)
            }
            drawPath(birdPath, Color(0xFF424242), style = Stroke(width = 3f))
        }
    }
}
