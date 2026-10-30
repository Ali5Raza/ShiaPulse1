package com.example.ui

import android.graphics.Bitmap
import android.graphics.Picture
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.api.ShiaQuote
import com.example.ui.theme.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.content.Context
import com.example.utils.ShareService
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.example.R

import androidx.compose.runtime.Immutable

@Immutable
sealed class CardLayer {
    abstract val id: String
    abstract val offset: Offset
    abstract val scale: Float
    abstract val rotation: Float
    abstract val isLocked: Boolean
    abstract val flipX: Boolean
    abstract val flipY: Boolean
}

@Immutable
data class TextLayer(
    override val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val color: Color = Color.Black,
    val fontName: String = "Roboto",
    val fontSize: Float = 20f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val hasShadow: Boolean = false,
    val shadowColor: Color = Color.Black,
    val shadowBlur: Float = 4f,
    val shadowDistance: Float = 4f,
    val shadowOpacity: Float = 0.5f,
    val hasOutline: Boolean = false,
    val outlineColor: Color = Color.White,
    val outlineWidth: Float = 2f,
    val isCurved: Boolean = false,
    val curveAngle: Float = 180f,
    val textShape: String = "NORMAL",
    
    
    val alignment: TextAlign = TextAlign.Center,
    override val offset: Offset = Offset.Zero,
    override val scale: Float = 1f,
    override val rotation: Float = 0f,
    override val isLocked: Boolean = false,
    override val flipX: Boolean = false,
    override val flipY: Boolean = false
) : CardLayer()

@Immutable
data class ErasedPath(
    val points: List<Offset>,
    val brushSize: Float
)

@Immutable
data class ImageLayer(
    override val id: String = UUID.randomUUID().toString(),
    val resId: Int,
    val erasedPaths: List<ErasedPath> = emptyList(),
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    override val offset: Offset = Offset.Zero,
    override val scale: Float = 1f,
    override val rotation: Float = 0f,
    override val isLocked: Boolean = false,
    override val flipX: Boolean = false,
    override val flipY: Boolean = false
) : CardLayer()

@Immutable
sealed class CardBackground {
    data class Solid(val color: Color) : CardBackground()
    data class Gradient(val colors: List<Color>) : CardBackground()
    data class CustomImage(
        val uri: Uri,
        val brightness: Float = 0f, // -255f to 255f offset
        val contrast: Float = 1f,   // 0f to 10f multiplier
        val grayscale: Float = 0f   // 0f (color) to 1f (gray)
    ) : CardBackground()
    data class PresetImage(
        val resId: Int,
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val grayscale: Float = 0f
    ) : CardBackground()
}

fun getBackgroundImageFilter(brightness: Float, contrast: Float, grayscale: Float): androidx.compose.ui.graphics.ColorFilter? {
    if (brightness == 0f && contrast == 1f && grayscale == 0f) return null
    val scale = contrast
    val translate = brightness * 255f
    val androidCm1 = android.graphics.ColorMatrix()
    androidCm1.setSaturation(1f - grayscale)
    val androidCm2 = android.graphics.ColorMatrix(floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    ))
    androidCm1.postConcat(androidCm2)
    return androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(androidCm1.array))
}

fun getFontByName(name: String): FontFamily {
    return when(name) {
        "Amiri" -> AmiriFont
        "Noto Naskh", "Noto Naskh Arabic" -> NotoNaskhArabicFont
        "Nastaliq", "Nafees Nastaleeq", "Jameel Noori", "Urdu Typesetting" -> NotoNastaliqUrduFont
        "Noorehuda", "Indo-Pak" -> NoorehudaFont
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }
}

val predefinedColors = listOf(
    Color.Black, Color.White, Color(0xFF065F46), Color(0xFF0F172A),
    Color(0xFFB55D00), Color(0xFF1E293B), Color(0xFFD4AF37), Color(0xFFE2C275),
    Color(0xFF8B0000), Color(0xFF4B0082), Color(0xFF1565C0), Color(0xFF2E7D32),
    Color(0xFFC62828), Color(0xFFF9A825), Color(0xFF6A1B9A), Color(0xFF00838F),
    Color(0xFF4E342E), Color(0xFF37474F)
)

val predefinedBackgrounds = listOf(
    CardBackground.PresetImage(R.drawable.islamic_bg_blue),
    CardBackground.Gradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
    CardBackground.Gradient(listOf(Color(0xFFD4AF37), Color(0xFFFFF2CD))),
    CardBackground.Gradient(listOf(Color(0xFF000428), Color(0xFF004e92))),
    CardBackground.Gradient(listOf(Color(0xFF0B3D2E), Color(0xFF176451), Color(0xFF2D6A4F))),
    CardBackground.Gradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))),
    CardBackground.Gradient(listOf(Color(0xFFFFF9EE), Color(0xFFF1E2C3))),
    CardBackground.Gradient(listOf(Color(0xFF3C1518), Color(0xFF691B2B)))
)

private val cardAspectRatios = listOf(
    "1:1" to 1f,
    "9:16" to 9f / 16f,
    "4:5" to 4f / 5f,
    "16:9" to 16f / 9f
)

private data class CardTextStylePreset(
    val title: String,
    val fontName: String,
    val fontSize: Float,
    val color: Color,
    val isBold: Boolean = false,
    val hasShadow: Boolean = false
)

private val cardTextStylePresets = listOf(
    CardTextStylePreset("Classic", "Serif", 20f, Color(0xFF0F172A)),
    CardTextStylePreset("Arabic", "Amiri", 30f, Color(0xFF065F46), isBold = true),
    CardTextStylePreset("Nastaliq", "Nastaliq", 26f, Color(0xFF1E293B)),
    CardTextStylePreset("Gold", "Serif", 22f, Color(0xFFD4AF37), isBold = true, hasShadow = true)
)

private data class CardTemplate(
    val id: String,
    val titleKey: String,
    val background: CardBackground.Gradient,
    val accentColor: Color,
    val titleColor: Color,
    val bodyColor: Color,
    val aspectRatio: Float,
    val useShadow: Boolean = true
)

// Four code-based designs: no template screenshots, image packs, or duplicated assets.
private val cardTemplates = listOf(
    CardTemplate(
        id = "jumma_emerald",
        titleKey = "jumma",
        background = CardBackground.Gradient(listOf(Color(0xFF062B24), Color(0xFF0B4F3D))),
        accentColor = Color(0xFFD4AF37),
        titleColor = Color(0xFFF4D47C),
        bodyColor = Color(0xFFF8FAFC),
        aspectRatio = 4f / 5f
    ),
    CardTemplate(
        id = "wiladat_noor",
        titleKey = "wiladat",
        background = CardBackground.Gradient(listOf(Color(0xFFFFFDF6), Color(0xFFFFF3D6), Color(0xFFEAD3A2))),
        accentColor = Color(0xFFB8860B),
        titleColor = Color(0xFF065F46),
        bodyColor = Color(0xFF334155),
        aspectRatio = 1f,
        useShadow = false
    ),
    CardTemplate(
        id = "shahadat_remembrance",
        titleKey = "shahadat",
        background = CardBackground.Gradient(listOf(Color(0xFF17090D), Color(0xFF3C1518), Color(0xFF691B2B))),
        accentColor = Color(0xFFC6A15B),
        titleColor = Color(0xFFF3DFC0),
        bodyColor = Color(0xFFF8FAFC),
        aspectRatio = 9f / 16f
    ),
    CardTemplate(
        id = "dua_serenity",
        titleKey = "dua",
        background = CardBackground.Gradient(listOf(Color(0xFF0F172A), Color(0xFF16213E), Color(0xFF0F3460))),
        accentColor = Color(0xFF93C5FD),
        titleColor = Color(0xFFE0F2FE),
        bodyColor = Color(0xFFF8FAFC),
        aspectRatio = 4f / 5f
    )
)

private fun createCardExportBitmap(picture: Picture, longEdge: Int = 2048): Bitmap {
    val sourceWidth = picture.width.takeIf { it > 0 } ?: 1080
    val sourceHeight = picture.height.takeIf { it > 0 } ?: 1080
    val scale = longEdge.toFloat() / maxOf(sourceWidth, sourceHeight).coerceAtLeast(1)
    val outputWidth = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
    val outputHeight = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.scale(scale, scale)
        canvas.drawPicture(picture)
    }
}

private fun cardAspectRatioLabel(ratio: Float): String =
    cardAspectRatios.firstOrNull { it.second == ratio }?.first ?: "Custom"

private fun cardMakerText(key: String, languageCode: String): String {
    val values = when (languageCode) {
        "ur" -> mapOf(
            "title" to "کارڈ میکر",
            "subtitle" to "خوبصورت ڈیزائن · ایچ ڈی ایکسپورٹ",
            "draft" to "ڈرافٹ",
            "export" to "ایکسپورٹ",
            "share" to "شیئر",
            "autosave" to "خودکار محفوظ",
            "layers" to "لیئرز",
            "text" to "متن",
            "design" to "ڈیزائن",
            "stickers" to "اسٹیکرز",
            "tools" to "ٹولز",
            "quick_styles" to "فوری ٹیکسٹ اسٹائلز",
            "branding" to "ShiaPulse واٹرمارک",
            "branding_sub" to "حتمی کارڈ پر لوگو اور نام دکھائیں",
            "no_layers" to "ابھی کوئی لیئر نہیں۔ متن یا اسٹیکر شامل کریں۔",
            "hd_saved" to "کارڈ ایچ ڈی میں گیلری میں محفوظ ہو گیا!",
            "templates" to "ٹیمپلیٹس",
            "apply_template" to "ٹیمپلیٹ لگائیں؟",
            "template_confirm" to "موجودہ لیئرز اور پس منظر تبدیل ہوں گے۔ آپ Undo سے واپس لا سکتے ہیں۔",
            "apply" to "لگائیں",
            "cancel" to "منسوخ",
            "template_applied" to "ٹیمپلیٹ لگ گیا",
            "template_body" to "اپنا پیغام یہاں لکھیں",
            "jumma" to "جمعہ",
            "wiladat" to "ولادت",
            "shahadat" to "شہادت",
            "dua" to "دعا"
        )
        "ar" -> mapOf(
            "title" to "صانع البطاقات",
            "subtitle" to "صمّم بجمال · تصدير عالي الدقة",
            "draft" to "مسودة",
            "export" to "تصدير",
            "share" to "مشاركة",
            "autosave" to "الحفظ التلقائي مفعّل",
            "layers" to "الطبقات",
            "text" to "النص",
            "design" to "التصميم",
            "stickers" to "الملصقات",
            "tools" to "الأدوات",
            "quick_styles" to "أنماط نص سريعة",
            "branding" to "علامة ShiaPulse",
            "branding_sub" to "إظهار الشعار والاسم على البطاقة النهائية",
            "no_layers" to "لا توجد طبقات بعد. أضف نصاً أو ملصقاً.",
            "hd_saved" to "تم حفظ البطاقة بدقة عالية في المعرض!",
            "templates" to "القوالب",
            "apply_template" to "تطبيق القالب؟",
            "template_confirm" to "سيتم استبدال الطبقات والخلفية الحاليين. يمكنك التراجع عن هذا التغيير.",
            "apply" to "تطبيق",
            "cancel" to "إلغاء",
            "template_applied" to "تم تطبيق القالب",
            "template_body" to "أضف رسالتك هنا",
            "jumma" to "الجمعة",
            "wiladat" to "الولادة",
            "shahadat" to "الشهادة",
            "dua" to "الدعاء"
        )
        else -> mapOf(
            "title" to "Card Maker",
            "subtitle" to "Design beautifully · Export in HD",
            "draft" to "Draft",
            "export" to "Export",
            "share" to "Share",
            "autosave" to "Auto-save on",
            "layers" to "layers",
            "text" to "Text",
            "design" to "Design",
            "stickers" to "Stickers",
            "tools" to "Tools",
            "quick_styles" to "Quick text styles",
            "branding" to "ShiaPulse watermark",
            "branding_sub" to "Show the logo and app name on the final card",
            "no_layers" to "No layers yet. Add text or a sticker to begin.",
            "hd_saved" to "Card saved to gallery in HD!",
            "templates" to "Templates",
            "apply_template" to "Apply template?",
            "template_confirm" to "Current layers and background will be replaced. You can undo this change.",
            "apply" to "Apply",
            "cancel" to "Cancel",
            "template_applied" to "Template applied",
            "template_body" to "Add your message here",
            "jumma" to "Jumma",
            "wiladat" to "Wiladat",
            "shahadat" to "Shahadat",
            "dua" to "Dua"
        )
    }
    return values[key] ?: key
}

private fun createCardTemplateLayers(template: CardTemplate, languageCode: String): List<CardLayer> {
    val fontName = when (languageCode) {
        "ur" -> "Nastaliq"
        "ar" -> "Amiri"
        else -> "Serif"
    }
    val titleSize = if (languageCode == "en") 30f else 34f
    val bodySize = if (languageCode == "en") 18f else 22f
    return listOf(
        TextLayer(
            text = cardMakerText(template.titleKey, languageCode),
            color = template.titleColor,
            fontName = fontName,
            fontSize = titleSize,
            isBold = true,
            hasShadow = template.useShadow,
            shadowColor = Color.Black,
            shadowOpacity = 0.35f,
            shadowBlur = 6f,
            shadowDistance = 2f,
            offset = Offset(0f, -50f)
        ),
        TextLayer(
            text = cardMakerText("template_body", languageCode),
            color = template.bodyColor,
            fontName = fontName,
            fontSize = bodySize,
            hasShadow = template.useShadow,
            shadowColor = Color.Black,
            shadowOpacity = 0.25f,
            shadowBlur = 4f,
            shadowDistance = 1f,
            offset = Offset(0f, 45f)
        )
    )
}

@Composable
private fun CardTemplatePreview(
    template: CardTemplate,
    languageCode: String,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 136.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(template.background.colors))
            .border(1.dp, template.accentColor.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, template.accentColor.copy(alpha = 0.5f), RoundedCornerShape(11.dp))
        )
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val previewFont = getFontByName(
                when (languageCode) {
                    "ur" -> "Nastaliq"
                    "ar" -> "Amiri"
                    else -> "Serif"
                }
            )
            Text(
                text = cardMakerText(template.titleKey, languageCode),
                color = template.titleColor,
                fontFamily = previewFont,
                fontSize = if (languageCode == "en") 17.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = cardMakerText("template_body", languageCode),
                color = template.bodyColor.copy(alpha = 0.8f),
                fontFamily = previewFont,
                fontSize = if (languageCode == "en") 8.sp else 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

private fun generatedColorBackground(seed: String): CardBackground.Gradient {
    val palettes = listOf(
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
        listOf(Color(0xFF16222A), Color(0xFF3A6073)),
        listOf(Color(0xFF1E3C32), Color(0xFF2D6A4F), Color(0xFF40916C)),
        listOf(Color(0xFF241734), Color(0xFF4B0082)),
        listOf(Color(0xFF2B1720), Color(0xFF6B2737)),
        listOf(Color(0xFF000428), Color(0xFF004E92))
    )
    val paletteIndex = Math.floorMod(seed.trim().hashCode(), palettes.size)
    return CardBackground.Gradient(palettes[paletteIndex])
}

val predefinedStickers = listOf(
    R.drawable.ic_tasbeeh_custom,
    R.drawable.ic_duas_custom,
    R.drawable.ic_knowledge_custom
)

fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable) {
        if (drawable.bitmap != null) {
            return drawable.bitmap
        }
    }
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
    } else {
        android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
    }
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@Composable
fun ErasedImage(
    resId: Int,
    erasedPaths: List<ErasedPath>,
    cropLeft: Float = 0f,
    cropTop: Float = 0f,
    cropRight: Float = 1f,
    cropBottom: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageBitmap = remember(resId) {
        val drawable = androidx.core.content.res.ResourcesCompat.getDrawable(context.resources, resId, null)
        drawable?.let { drawableToBitmap(it).asImageBitmap() }
    }

    if (imageBitmap != null) {
        androidx.compose.foundation.Canvas(modifier = modifier) {
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                }
                
                // We create an offscreen layer to draw the image and clear paths on it
                val saveCount = canvas.nativeCanvas.saveLayer(
                    0f, 0f, size.width, size.height, null
                )

                if (cropLeft > 0f || cropTop > 0f || cropRight < 1f || cropBottom < 1f) {
                    val scaleX = 1f / (cropRight - cropLeft).coerceAtLeast(0.01f)
                    val scaleY = 1f / (cropBottom - cropTop).coerceAtLeast(0.01f)
                    canvas.nativeCanvas.scale(scaleX, scaleY)
                    canvas.nativeCanvas.translate(-cropLeft * size.width, -cropTop * size.height)
                }

                // Draw the image scaled to fit the Canvas size
                val srcRect = android.graphics.Rect(0, 0, imageBitmap.width, imageBitmap.height)
                val dstRect = android.graphics.RectF(0f, 0f, size.width, size.height)
                
                canvas.nativeCanvas.drawBitmap(
                    imageBitmap.asAndroidBitmap(),
                    srcRect,
                    dstRect,
                    paint
                )

                // Setup eraser paint
                val eraserPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                }

                // Draw each path
                erasedPaths.forEach { erasedPath ->
                    if (erasedPath.points.size > 1) {
                        val path = android.graphics.Path()
                        val first = erasedPath.points.first()
                        path.moveTo(first.x * size.width, first.y * size.height)
                        for (i in 1 until erasedPath.points.size) {
                            val pt = erasedPath.points[i]
                            path.lineTo(pt.x * size.width, pt.y * size.height)
                        }
                        eraserPaint.strokeWidth = erasedPath.brushSize
                        canvas.nativeCanvas.drawPath(path, eraserPaint)
                    } else if (erasedPath.points.size == 1) {
                        val pt = erasedPath.points.first()
                        val dotPaint = android.graphics.Paint(eraserPaint).apply {
                            style = android.graphics.Paint.Style.FILL
                        }
                        canvas.nativeCanvas.drawCircle(pt.x * size.width, pt.y * size.height, erasedPath.brushSize / 2, dotPaint)
                    }
                }

                canvas.nativeCanvas.restoreToCount(saveCount)
            }
        }
    } else {
        // Fallback
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = modifier
        )
    }
}

@Composable
fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawWithCache {
                onDrawBehind {
                    val cellSize = 12.dp.toPx()
                    val numCols = (size.width / cellSize).toInt() + 1
                    val numRows = (size.height / cellSize).toInt() + 1
                    for (c in 0 until numCols) {
                        for (r in 0 until numRows) {
                            val color = if ((c + r) % 2 == 0) Color(0xFFECEFF1) else Color(0xFFFFFFFF)
                            drawRect(
                                color = color,
                                topLeft = Offset(c * cellSize, r * cellSize),
                                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            },
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardLayerItem(
    layer: CardLayer,
    isSelected: Boolean,
    onLayerSelected: () -> Unit,
    onLayerDoubleTapped: () -> Unit = {},
    onLayerTransform: (pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onTransformEnd: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = layer.offset.x
                translationY = layer.offset.y
                scaleX = if (layer.flipX) -layer.scale else layer.scale
                scaleY = if (layer.flipY) -layer.scale else layer.scale
                rotationZ = layer.rotation
            }
            .pointerInput(layer.id, layer.isLocked) {
                if (layer.isLocked) return@pointerInput
                detectTransformGestures { _, pan, zoom, rotation ->
                    onLayerTransform(pan, zoom, rotation)
                }
            }
            .pointerInput(layer.id, layer.isLocked) {
                if (layer.isLocked) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        if (!anyPressed) {
                            onTransformEnd()
                        }
                    }
                }
            }
            .combinedClickable(
                onDoubleClick = { if (!layer.isLocked) onLayerDoubleTapped() },
                onClick = { if (!layer.isLocked) onLayerSelected() }
            )
            .border(
                width = if (isSelected) (1.dp / layer.scale) else 0.dp,
                color = if (isSelected) Color.Blue.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        when (layer) {
            is TextLayer -> {
                if (layer.textShape != "NORMAL" || layer.isCurved) {
                    val shape = if (layer.textShape == "NORMAL" && layer.isCurved) "CURVED_UP" else layer.textShape
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    
                    // Determine canvas size based on shape
                    val cw = layer.fontSize * 7
                    val ch = if (shape.startsWith("CIRCLE")) layer.fontSize * 7 else layer.fontSize * 3
                    
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(cw.dp, ch.dp)) {
                        drawIntoCanvas { canvas ->
                            val path = android.graphics.Path()
                            val width = size.width
                            val height = size.height
                            
                            when (shape) {
                                "CURVED_UP" -> {
                                    val rect = android.graphics.RectF(0f, 0f, width, height * 2)
                                    path.addArc(rect, 180f, layer.curveAngle)
                                }
                                "CURVED_DOWN" -> {
                                    val rect = android.graphics.RectF(0f, -height, width, height)
                                    path.addArc(rect, 180f, -layer.curveAngle)
                                }
                                "CIRCLE_OUTER" -> {
                                    path.addCircle(width / 2, height / 2, width / 2.5f, android.graphics.Path.Direction.CW)
                                }
                                "CIRCLE_INNER" -> {
                                    path.addCircle(width / 2, height / 2, width / 2.5f, android.graphics.Path.Direction.CCW)
                                }
                                "WAVY" -> {
                                    path.moveTo(0f, height / 2)
                                    path.quadTo(width / 4, 0f, width / 2, height / 2)
                                    path.quadTo(width * 3 / 4, height, width, height / 2)
                                }
                                "BELL" -> {
                                    path.moveTo(0f, height)
                                    path.cubicTo(width / 4, height, width / 4, 0f, width / 2, 0f)
                                    path.cubicTo(width * 3 / 4, 0f, width * 3 / 4, height, width, height)
                                }
                                "IRREGULAR" -> {
                                    path.moveTo(0f, height / 2)
                                    path.cubicTo(width / 3, 0f, width / 2, height, width, height / 3)
                                }
                                else -> {
                                    val rect = android.graphics.RectF(0f, 0f, width, height * 2)
                                    path.addArc(rect, 180f, layer.curveAngle)
                                }
                            }
                            
                            val paint = android.graphics.Paint().apply {
                                textSize = with(density) { layer.fontSize.sp.toPx() }
                                color = layer.color.toArgb()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                                if (layer.isBold) {
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                if (layer.hasShadow) {
                                    setShadowLayer(
                                        layer.shadowBlur,
                                        layer.shadowDistance,
                                        layer.shadowDistance,
                                        layer.shadowColor.copy(alpha = layer.shadowOpacity).toArgb()
                                    )
                                }
                                if (layer.hasOutline) {
                                    style = android.graphics.Paint.Style.FILL_AND_STROKE
                                    strokeWidth = layer.outlineWidth
                                }
                            }
                            
                            val textLength = paint.measureText(layer.text)
                            val pathMeasure = android.graphics.PathMeasure(path, false)
                            val pathLength = pathMeasure.length
                            val hOffset = (pathLength - textLength) / 2f
                            
                            // If outline is requested, draw stroke first then fill
                            if (layer.hasOutline) {
                                val strokePaint = android.graphics.Paint(paint).apply {
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = layer.outlineWidth * 2
                                    color = layer.outlineColor.toArgb()
                                }
                                canvas.nativeCanvas.drawTextOnPath(layer.text, path, hOffset, 0f, strokePaint)
                            }
                            paint.style = android.graphics.Paint.Style.FILL
                            canvas.nativeCanvas.drawTextOnPath(layer.text, path, hOffset, 0f, paint)
                        }
                    }
                } else { 
                    Text(
                        text = layer.text,
                        color = layer.color,
                        fontSize = layer.fontSize.sp,
                        fontFamily = getFontByName(layer.fontName),
                        fontWeight = if (layer.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (layer.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (layer.isUnderline) TextDecoration.Underline else TextDecoration.None,
                        textAlign = layer.alignment,
                        style = TextStyle(
                            shadow = if (layer.hasShadow) Shadow(color = layer.shadowColor.copy(alpha = layer.shadowOpacity), offset = Offset(layer.shadowDistance, layer.shadowDistance), blurRadius = layer.shadowBlur) else null,
                            drawStyle = if (layer.hasOutline) androidx.compose.ui.graphics.drawscope.Stroke(
                                miter = 10f,
                                width = layer.outlineWidth,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            ) else null
                        )
                    )
                }
            }
            is ImageLayer -> {
                ErasedImage(
                    resId = layer.resId,
                    erasedPaths = layer.erasedPaths,
                    cropLeft = layer.cropLeft,
                    cropTop = layer.cropTop,
                    cropRight = layer.cropRight,
                    cropBottom = layer.cropBottom,
                    modifier = Modifier.size(100.dp)
                )
            }
        }
        
        if (isSelected && !layer.isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-24).dp)
                    .size(24.dp)
                    .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    .border(1.dp, Color.Blue, androidx.compose.foundation.shape.CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { onTransformEnd() },
                            onDragCancel = { onTransformEnd() }
                        ) { change, dragAmount ->
                            change.consume()
                            onLayerTransform(Offset.Zero, 1f, dragAmount.x)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rotate",
                    tint = Color.Blue,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 12.dp)
                    .size(24.dp)
                    .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    .border(1.dp, Color.Blue, androidx.compose.foundation.shape.CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { onTransformEnd() },
                            onDragCancel = { onTransformEnd() }
                        ) { change, dragAmount ->
                            change.consume()
                            val scaleChange = 1f + (dragAmount.x + dragAmount.y) * 0.005f
                            onLayerTransform(Offset.Zero, scaleChange, 0f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.OpenWith, // Need to ensure it's imported
                    contentDescription = "Scale",
                    tint = Color.Blue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsMakerScreen(
    initialQuote: ShiaQuote? = null,
    onBack: () -> Unit,
    currentLang: String = "en"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var layers by remember { mutableStateOf(listOf<CardLayer>()) }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var background by remember { mutableStateOf<CardBackground>(predefinedBackgrounds[0]) }
    var aspectRatio by remember { mutableStateOf(1f) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var captureTrigger by remember { mutableStateOf(false) }
    var captureForSaveTrigger by remember { mutableStateOf(false) }
    var editingTextLayerId by remember { mutableStateOf<String?>(null) }
    var showEraserDialog by remember { mutableStateOf(false) }
    var showOverwriteDraftDialog by remember { mutableStateOf(false) }
    var showWatermark by remember { mutableStateOf(true) }
    var pendingTemplate by remember { mutableStateOf<CardTemplate?>(null) }
    
    val history = remember { mutableStateListOf<Triple<List<CardLayer>, CardBackground, Float>>() }
    var historyIndex by remember { mutableStateOf(-1) }

    val handleBackPress = {
        if (historyIndex > 0) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }
    
    androidx.activity.compose.BackHandler(onBack = handleBackPress)

    fun saveState(newLayers: List<CardLayer>, newBg: CardBackground) {
        if (historyIndex >= 0 && historyIndex < history.size) {
            val (currentLayers, currentBg, currentRatio) = history[historyIndex]
            if (currentLayers == newLayers && currentBg == newBg && currentRatio == aspectRatio) {
                return // Ignore duplicates
            }
        }
        if (historyIndex < history.size - 1) {
            history.removeRange(historyIndex + 1, history.size)
        }
        history.add(Triple(newLayers, newBg, aspectRatio))
        if (history.size > 20) {
            history.removeAt(0)
        } else {
            historyIndex++
        }
        
        // Draft auto-save is handled reactively and debounced by the LaunchedEffect(layers, background, aspectRatio) hook.

    }
    fun initializeDefault(quote: ShiaQuote?) {
        if (quote != null) {
            val initialLayers = mutableListOf<CardLayer>()
            var totalHeight = 0f
            if (quote.arabic.isNotBlank()) totalHeight += 150f
            if (quote.english.isNotBlank()) totalHeight += 100f
            if (quote.urdu.isNotBlank()) totalHeight += 100f
            if (quote.source.isNotBlank()) totalHeight += 50f
            
            var currentY = -totalHeight / 2f + 50f
            if (quote.arabic.isNotBlank()) {
                initialLayers.add(TextLayer(text = quote.arabic, fontName = "Amiri", fontSize = 28f, color = Color(0xFF065F46), isBold = true, offset = Offset(0f, currentY)))
                currentY += 150f
            }
            if (quote.english.isNotBlank()) {
                initialLayers.add(TextLayer(text = quote.english, fontName = "Serif", fontSize = 18f, color = Color(0xFF0F172A), offset = Offset(0f, currentY)))
                currentY += 100f
            }
            if (quote.urdu.isNotBlank()) {
                initialLayers.add(TextLayer(text = quote.urdu, fontName = "Nastaliq", fontSize = 24f, color = Color(0xFF0F172A), offset = Offset(0f, currentY)))
                currentY += 100f
            }
            if (quote.source.isNotBlank()) {
                initialLayers.add(TextLayer(text = "- ${quote.source}", fontName = "Roboto", fontSize = 14f, color = Color(0xFFB55D00), offset = Offset(0f, currentY + 50f)))
            }
            layers = initialLayers
            if (initialLayers.isNotEmpty()) selectedLayerId = initialLayers.first().id
            history.clear()
            historyIndex = -1
            saveState(initialLayers, background)
        } else {
            val newLayer = TextLayer(text = "Tap to edit", fontSize = 24f)
            layers = listOf(newLayer)
            selectedLayerId = newLayer.id
            history.clear()
            historyIndex = -1
            saveState(layers, background)
        }
    }

    LaunchedEffect(initialQuote) {
        val draft = loadDraftFromPrefs(context)
        if (draft != null) {
            try {
                background = restoreBackground(
                    type = draft.bgType,
                    colorArgb = draft.bgColorArgb,
                    colorsArgb = draft.bgColorsArgb,
                    uriStr = draft.bgUriStr,
                    resId = draft.bgResId,
                    brightness = draft.bgBrightness,
                    contrast = draft.bgContrast,
                    grayscale = draft.bgGrayscale
                )
                layers = draft.layers.map { it.toCardLayer() }
                aspectRatio = draft.aspectRatio
                if (layers.isNotEmpty()) {
                    selectedLayerId = layers.first().id
                }
                history.clear()
                historyIndex = -1
                saveState(layers, background)
            } catch (e: Exception) {
                e.printStackTrace()
                if (layers.isEmpty()) {
                    initializeDefault(initialQuote)
                }
            }
            if (initialQuote != null) {
                showOverwriteDraftDialog = true
            }
        } else {
            if (layers.isEmpty()) {
                initializeDefault(initialQuote)
            }
        }
    }

    LaunchedEffect(layers, background, aspectRatio) {
        if (layers.isNotEmpty()) {
            kotlinx.coroutines.delay(500) // Debounce rapid keystrokes or drag operations
            try {
                saveDraftToPrefs(context, layers, background, aspectRatio)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    val picture = remember { Picture() }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            background = CardBackground.CustomImage(uri)
            saveState(layers, background)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = cardMakerText("title", currentLang),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = cardMakerText("subtitle", currentLang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (historyIndex > 0) {
                                historyIndex--
                                val state = history[historyIndex]
                                layers = state.first
                                background = state.second
                                aspectRatio = state.third
                            }
                        },
                        enabled = historyIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = {
                            if (historyIndex < history.size - 1) {
                                historyIndex++
                                val state = history[historyIndex]
                                layers = state.first
                                background = state.second
                                aspectRatio = state.third
                            }
                        },
                        enabled = historyIndex < history.size - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = cardMakerText("autosave", currentLang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    text = "${layers.size} ${cardMakerText("layers", currentLang)} · ${cardAspectRatioLabel(aspectRatio)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Preview Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                        .shadow(12.dp, RoundedCornerShape(20.dp), clip = false)
                        .clip(RoundedCornerShape(20.dp))
                        .drawWithCache {
                        val width = size.width.toInt()
                        val height = size.height.toInt()
                        onDrawWithContent {
                            if (captureTrigger || captureForSaveTrigger) {
                                val pictureCanvas = androidx.compose.ui.graphics.Canvas(picture.beginRecording(width, height))
                                draw(this, layoutDirection, pictureCanvas, size) {
                                    this@onDrawWithContent.drawContent()
                                }
                                picture.endRecording()
                            }
                            drawContent()
                        }
                    }
            ) {
                // Draw Background
                when (val bg = background) {
                    is CardBackground.Solid -> {
                        Box(modifier = Modifier.fillMaxSize().background(bg.color))
                    }
                    is CardBackground.Gradient -> {
                        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(bg.colors)))
                        // The template gradient also restores its lightweight frame from drafts.
                        cardTemplates.firstOrNull { it.background.colors == bg.colors }?.let { template ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .border(1.dp, template.accentColor.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            )
                        }
                    }
                    is CardBackground.CustomImage -> {
                        AsyncImage(
                            model = bg.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            colorFilter = getBackgroundImageFilter(bg.brightness, bg.contrast, bg.grayscale),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is CardBackground.PresetImage -> {
                        Image(
                            painter = painterResource(id = bg.resId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            colorFilter = getBackgroundImageFilter(bg.brightness, bg.contrast, bg.grayscale),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Draw Layers
                layers.forEach { layer ->
                    key(layer.id) {
                        CardLayerItem(
                            layer = layer,
                            isSelected = selectedLayerId == layer.id && !captureTrigger && !captureForSaveTrigger,
                            onLayerSelected = { 
                                if (selectedLayerId == layer.id && layer is TextLayer) {
                                    editingTextLayerId = layer.id
                                }
                                selectedLayerId = layer.id 
                            },
                            onLayerDoubleTapped = {
                                if (layer is TextLayer) {
                                    editingTextLayerId = layer.id
                                }
                            },
                            onLayerTransform = { pan, zoom, rotation ->
                                layers = layers.map { l ->
                                    if (l.id == layer.id && !l.isLocked) {
                                        when (l) {
                                            is TextLayer -> l.copy(
                                                offset = l.offset + pan,
                                                scale = (l.scale * zoom).coerceIn(0.5f, 5f),
                                                rotation = l.rotation + rotation
                                            )
                                            is ImageLayer -> l.copy(
                                                offset = l.offset + pan,
                                                scale = (l.scale * zoom).coerceIn(0.2f, 5f),
                                                rotation = l.rotation + rotation
                                            )
                                        }
                                    } else l
                                }
                            },
                            onTransformEnd = {
                                saveState(layers, background)
                            }
                        )
                    }
                }

                if (showWatermark) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.34f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.shia_pulse_logo_1780375790845),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(19.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ShiaPulse",
                            color = Color.White.copy(alpha = 0.94f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            saveDraftToPrefs(context, layers, background, aspectRatio)
                            android.widget.Toast.makeText(
                                context,
                                "${cardMakerText("draft", currentLang)} ✓",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Failed to save draft", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(cardMakerText("draft", currentLang), fontSize = 12.sp, maxLines = 1)
                }
                Button(
                    onClick = {
                        scope.launch {
                            captureForSaveTrigger = true
                            kotlinx.coroutines.delay(60)
                            val bitmap = createCardExportBitmap(picture)
                            ShareService.saveBitmapToGallery(
                                context = context,
                                bitmap = bitmap,
                                scope = scope,
                                onSuccess = {
                                    android.widget.Toast.makeText(
                                        context,
                                        cardMakerText("hd_saved", currentLang),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = {
                                    android.widget.Toast.makeText(context, "Failed to save: $it", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            captureForSaveTrigger = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(cardMakerText("export", currentLang), fontSize = 12.sp, maxLines = 1)
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            captureTrigger = true
                            kotlinx.coroutines.delay(60)
                            val bitmap = createCardExportBitmap(picture)
                            ShareService.shareBitmap(context, bitmap, "custom_card", "Share Card", scope)
                            captureTrigger = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(cardMakerText("share", currentLang), fontSize = 12.sp, maxLines = 1)
                }
            }

            // Controls Section
            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf(
                cardMakerText("text", currentLang) to Icons.Default.TextFields,
                cardMakerText("design", currentLang) to Icons.Default.Photo,
                cardMakerText("stickers", currentLang) to Icons.Default.EmojiEmotions,
                cardMakerText("layers", currentLang) to Icons.Default.Layers,
                cardMakerText("tools", currentLang) to Icons.Default.AutoAwesome
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 8.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = tab.first,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.second,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val activeLayer = remember(layers, selectedLayerId) { layers.find { it.id == selectedLayerId } }
                    if (activeLayer != null && (selectedTab == 0 || selectedTab == 2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = {
                                val idx = layers.indexOfFirst { it.id == activeLayer.id }
                                if (idx > 0) {
                                    val newLayers = layers.toMutableList()
                                    val item = newLayers.removeAt(idx)
                                    newLayers.add(0, item)
                                    layers = newLayers
                                    saveState(layers, background)
                                }
                            }) {
                                Text("Send to Back")
                            }
                            OutlinedButton(onClick = {
                                val idx = layers.indexOfFirst { it.id == activeLayer.id }
                                if (idx != -1 && idx < layers.size - 1) {
                                    val newLayers = layers.toMutableList()
                                    val item = newLayers.removeAt(idx)
                                    newLayers.add(item)
                                    layers = newLayers
                                    saveState(layers, background)
                                }
                            }) {
                                Text("Bring to Front")
                            }
                        }
                    }

                    when (selectedTab) {
                        0 -> { // Text Tab
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(onClick = {
                                    val newLayer = TextLayer(text = "New Text", offset = Offset(0f, 0f))
                                    layers = layers + newLayer
                                    selectedLayerId = newLayer.id
                                    saveState(layers, background)
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Text")
                                }
                            }

                            if (activeLayer is TextLayer) {
                                Text("Edit Selected Text", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                
                                OutlinedTextField(
                                    value = (activeLayer as TextLayer).text,
                                    onValueChange = { newText ->
                                        layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(text = newText) else it }
                                    },
                                    label = { Text("Text") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    maxLines = 4
                                )
                                Button(onClick = { saveState(layers, background) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Save Text Edit")
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = cardMakerText("quick_styles", currentLang),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    cardTextStylePresets.forEach { preset ->
                                        val selected = activeLayer as TextLayer
                                        val isActive = selected.fontName == preset.fontName &&
                                            selected.fontSize == preset.fontSize && selected.color == preset.color
                                        Surface(
                                            onClick = {
                                                val styledLayer = selected.copy(
                                                    fontName = preset.fontName,
                                                    fontSize = preset.fontSize,
                                                    color = preset.color,
                                                    isBold = preset.isBold,
                                                    hasShadow = preset.hasShadow,
                                                    shadowColor = if (preset.hasShadow) Color.Black.copy(alpha = 0.55f) else selected.shadowColor
                                                )
                                                layers = layers.map { if (it.id == selected.id) styledLayer else it }
                                                saveState(layers, background)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.size(9.dp).background(preset.color, CircleShape))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = preset.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Colors
                                LazyRow(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(predefinedColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(1.dp, Color.Gray, CircleShape)
                                                .clickable {
                                                    layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(color = color) else it }
                                                    saveState(layers, background)
                                                }
                                        ) {
                                            if (activeLayer.color == color) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = if (color == Color.White) Color.Black else Color.White, modifier = Modifier.align(Alignment.Center))
                                            }
                                        }
                                    }
                                }

                                // Size
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Size: ${activeLayer.fontSize.toInt()}")
                                    Slider(
                                        value = activeLayer.fontSize,
                                        onValueChange = { newSize ->
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(fontSize = newSize) else it }
                                        },
                                        onValueChangeFinished = { saveState(layers, background) },
                                        valueRange = 10f..100f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                    )
                                }

                                // Rotation
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Rotate: ${(((activeLayer.rotation % 360f) + 360f) % 360f).toInt()}°")
                                    Slider(
                                        value = ((activeLayer.rotation % 360f) + 360f) % 360f,
                                        onValueChange = { newRot ->
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(rotation = newRot) else it }
                                        },
                                        onValueChangeFinished = { saveState(layers, background) },
                                        valueRange = 0f..360f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                    )
                                }

                                // Font Family Dropdown
                                var fontMenuExpanded by remember { mutableStateOf(false) }
                                val fonts = listOf("Amiri", "Noto Naskh", "Nastaliq", "Noorehuda", "Serif", "Monospace", "SansSerif")
                                
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    OutlinedButton(
                                        onClick = { fontMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Font: ${activeLayer.fontName}", modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Font")
                                    }
                                    
                                    DropdownMenu(
                                        expanded = fontMenuExpanded,
                                        onDismissRequest = { fontMenuExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        fonts.forEach { font ->
                                            DropdownMenuItem(
                                                text = { Text(font, fontFamily = getFontByName(font)) },
                                                onClick = {
                                                    layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(fontName = font) else it }
                                                    saveState(layers, background)
                                                    fontMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Formatting row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconToggleButton(
                                        checked = activeLayer.isBold,
                                        onCheckedChange = { isBold -> 
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(isBold = isBold) else it }
                                            saveState(layers, background)
                                        }
                                    ) {
                                        Text("B", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    }
                                    IconToggleButton(
                                        checked = activeLayer.isItalic,
                                        onCheckedChange = { isItalic -> 
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(isItalic = isItalic) else it }
                                            saveState(layers, background)
                                        }
                                    ) {
                                        Text("I", fontStyle = FontStyle.Italic, fontSize = 20.sp)
                                    }
                                    IconToggleButton(
                                        checked = activeLayer.isUnderline,
                                        onCheckedChange = { isUnder -> 
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(isUnderline = isUnder) else it }
                                            saveState(layers, background)
                                        }
                                    ) {
                                        Text("U", textDecoration = TextDecoration.Underline, fontSize = 20.sp)
                                    }
                                    IconToggleButton(
                                        checked = activeLayer.hasShadow,
                                        onCheckedChange = { hasShadow -> 
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(hasShadow = hasShadow) else it }
                                            saveState(layers, background)
                                        }
                                    ) {
                                        Text("S", style = TextStyle(shadow = Shadow(blurRadius = 4f)), fontSize = 20.sp)
                                    }
                                    IconToggleButton(
                                        checked = activeLayer.hasOutline,
                                        onCheckedChange = { hasOut -> 
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(hasOutline = hasOut) else it }
                                            saveState(layers, background)
                                        }
                                    ) {
                                        Text("O", fontSize = 20.sp)
                                    }
                                    // Alignment
                                    IconButton(onClick = {
                                        val nextAlign = when(activeLayer.alignment) {
                                            TextAlign.Left -> TextAlign.Center
                                            TextAlign.Center -> TextAlign.Right
                                            TextAlign.Right -> TextAlign.Left
                                            else -> TextAlign.Center
                                        }
                                        layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(alignment = nextAlign) else it }
                                        saveState(layers, background)
                                    }) {
                                        Icon(
                                            imageVector = when(activeLayer.alignment) {
                                                TextAlign.Left -> Icons.Default.FormatAlignLeft
                                                TextAlign.Center -> Icons.Default.FormatAlignCenter
                                                TextAlign.Right -> Icons.Default.FormatAlignRight
                                                else -> Icons.Default.FormatAlignCenter
                                            },
                                            contentDescription = null
                                        )
                                    }
                                    // Flip Horizontal
                                    IconButton(onClick = {
                                        layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(flipX = !it.flipX) else it }
                                        saveState(layers, background)
                                    }) {
                                        Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal")
                                    }
                                    // Delete
                                    IconButton(onClick = {
                                        layers = layers.filter { it.id != activeLayer.id }
                                        selectedLayerId = null
                                        saveState(layers, background)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Layer", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val newLayer = activeLayer.copy(id = UUID.randomUUID().toString(), offset = activeLayer.offset + Offset(20f, 20f))
                                            layers = layers + newLayer
                                            selectedLayerId = newLayer.id
                                            saveState(layers, background)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) { Text("Copy", fontSize = 12.sp) }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(offset = it.offset.copy(x = 0f)) else it }
                                            saveState(layers, background)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) { Text("Ctr X", fontSize = 12.sp) }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(offset = it.offset.copy(y = 0f)) else it }
                                            saveState(layers, background)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) { Text("Ctr Y", fontSize = 12.sp) }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            layers = layers.map { if (it.id == activeLayer.id) (it as TextLayer).copy(offset = Offset.Zero) else it }
                                            saveState(layers, background)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) { Text("Center", fontSize = 12.sp) }
                                }

                                Text("Text Layout & Paths", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                
                                val textPresetTabs = listOf("Standard", "Arcs", "Circles", "Wavy & Paths")
                                var selectedPresetTab by remember { mutableStateOf(0) }
                                
                                // Sync tab when active layer text shape changes
                                LaunchedEffect(activeLayer.textShape) {
                                    selectedPresetTab = when (activeLayer.textShape) {
                                        "NORMAL" -> 0
                                        "CURVED_UP", "CURVED_DOWN" -> 1
                                        "CIRCLE_OUTER", "CIRCLE_INNER" -> 2
                                        "WAVY", "BELL", "IRREGULAR" -> 3
                                        else -> 0
                                    }
                                }
                                
                                TabRow(
                                    selectedTabIndex = selectedPresetTab,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) }
                                ) {
                                    textPresetTabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedPresetTab == index,
                                            onClick = { selectedPresetTab = index },
                                            text = { Text(title, fontSize = 11.sp, maxLines = 1) }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                when (selectedPresetTab) {
                                    0 -> { // Standard
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val isSelected = activeLayer.textShape == "NORMAL"
                                            Button(
                                                onClick = {
                                                    layers = layers.map { 
                                                        if (it.id == activeLayer.id) (it as TextLayer).copy(
                                                            textShape = "NORMAL", isCurved = false
                                                        ) else it 
                                                    }
                                                    saveState(layers, background)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.TextFields, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Reset (Normal Text)")
                                            }
                                        }
                                    }
                                    1 -> { // Arcs
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val isUpSelected = activeLayer.textShape == "CURVED_UP"
                                            Button(
                                                onClick = {
                                                    layers = layers.map { 
                                                        if (it.id == activeLayer.id) (it as TextLayer).copy(
                                                            textShape = "CURVED_UP", isCurved = true, curveAngle = 180f
                                                        ) else it 
                                                    }
                                                    saveState(layers, background)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isUpSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isUpSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Arc Up ⌢")
                                            }
                                            
                                            val isDownSelected = activeLayer.textShape == "CURVED_DOWN"
                                            Button(
                                                onClick = {
                                                    layers = layers.map { 
                                                        if (it.id == activeLayer.id) (it as TextLayer).copy(
                                                            textShape = "CURVED_DOWN", isCurved = true, curveAngle = 180f
                                                        ) else it 
                                                    }
                                                    saveState(layers, background)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isDownSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isDownSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Arc Down ⌣")
                                            }
                                        }
                                    }
                                    2 -> { // Circles
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val isOutSelected = activeLayer.textShape == "CIRCLE_OUTER"
                                            Button(
                                                onClick = {
                                                    layers = layers.map { 
                                                        if (it.id == activeLayer.id) (it as TextLayer).copy(
                                                            textShape = "CIRCLE_OUTER", isCurved = true
                                                        ) else it 
                                                    }
                                                    saveState(layers, background)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isOutSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isOutSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Circle Outer ◯")
                                            }
                                            
                                            val isInSelected = activeLayer.textShape == "CIRCLE_INNER"
                                            Button(
                                                onClick = {
                                                    layers = layers.map { 
                                                        if (it.id == activeLayer.id) (it as TextLayer).copy(
                                                            textShape = "CIRCLE_INNER", isCurved = true
                                                        ) else it 
                                                    }
                                                    saveState(layers, background)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isInSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isInSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Circle Inner ⊙")
                                            }
                                        }
                                    }
                                    3 -> { // Wavy & Paths
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val isWavySelected = activeLayer.textShape == "WAVY"
                                                Button(
                                                    onClick = {
                                                        layers = layers.map { 
                                                            if (it.id == activeLayer.id) (it as TextLayer).copy(
                                                                textShape = "WAVY", isCurved = true
                                                            ) else it 
                                                        }
                                                        saveState(layers, background)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isWavySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = if (isWavySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Wavy ~")
                                                }
                                                
                                                val isBellSelected = activeLayer.textShape == "BELL"
                                                Button(
                                                    onClick = {
                                                        layers = layers.map { 
                                                            if (it.id == activeLayer.id) (it as TextLayer).copy(
                                                                textShape = "BELL", isCurved = true
                                                            ) else it 
                                                        }
                                                        saveState(layers, background)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isBellSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = if (isBellSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Bell Curve 🔔")
                                                }
                                            }
                                            
                                            val isIrregSelected = activeLayer.textShape == "IRREGULAR"
                                            Button(
                                                onClick = {
                                                    layers = layers.map { 
                                                        if (it.id == activeLayer.id) (it as TextLayer).copy(
                                                            textShape = "IRREGULAR", isCurved = true
                                                        ) else it 
                                                    }
                                                    saveState(layers, background)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isIrregSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isIrregSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Irregular / Hand-Drawn Path ⟿")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Bg Tab
                            Text(
                                text = cardMakerText("templates", currentLang),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(cardTemplates, key = { it.id }) { template ->
                                    CardTemplatePreview(
                                        template = template,
                                        languageCode = currentLang,
                                        onSelect = { pendingTemplate = template }
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(bottom = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                            Text("Background Style", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            // Aspect Ratio Section
                            Spacer(Modifier.height(8.dp))
                            Text("Card Aspect Ratio", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val ratios = cardAspectRatios
                                ratios.forEach { (label, ratio) ->
                                    val isSelected = aspectRatio == ratio
                                    OutlinedButton(
                                        onClick = {
                                            aspectRatio = ratio
                                            saveState(layers, background)
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                            
                            // Preset Backgrounds Section
                            Spacer(Modifier.height(12.dp))
                            Text("Preset Backgrounds", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            LazyRow(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(predefinedBackgrounds) { bg ->
                                    val isSelected = background == bg
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp, 100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                background = bg
                                                saveState(layers, background)
                                            }
                                    ) {
                                        when (bg) {
                                            is CardBackground.Solid -> {
                                                Box(modifier = Modifier.fillMaxSize().background(bg.color))
                                            }
                                            is CardBackground.Gradient -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Brush.linearGradient(bg.colors))
                                                )
                                            }
                                            is CardBackground.PresetImage -> {
                                                androidx.compose.foundation.Image(
                                                    painter = painterResource(id = bg.resId),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            is CardBackground.CustomImage -> {
                                                AsyncImage(
                                                    model = bg.uri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Solid Colors Section
                            Spacer(Modifier.height(12.dp))
                            Text("Solid Colors", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            LazyRow(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(predefinedColors) { color ->
                                    val isSelected = (background as? CardBackground.Solid)?.color == color
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                background = CardBackground.Solid(color)
                                                saveState(layers, background)
                                            }
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (color == Color.White) Color.Black else Color.White,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }

                            // Choose Custom Image Button
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Choose Custom Image Background")
                            }

                            // Image Controls (Only show if image background selected)
                            if (background is CardBackground.CustomImage || background is CardBackground.PresetImage) {
                                Spacer(Modifier.height(16.dp))
                                Text("Background Adjustments", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                
                                val currentBrightness = when (val bg = background) {
                                    is CardBackground.CustomImage -> bg.brightness
                                    is CardBackground.PresetImage -> bg.brightness
                                    else -> 0f
                                }
                                val currentContrast = when (val bg = background) {
                                    is CardBackground.CustomImage -> bg.contrast
                                    is CardBackground.PresetImage -> bg.contrast
                                    else -> 1f
                                }
                                val currentGrayscale = when (val bg = background) {
                                    is CardBackground.CustomImage -> bg.grayscale
                                    is CardBackground.PresetImage -> bg.grayscale
                                    else -> 0f
                                }

                                // Brightness Slider
                                Text("Brightness: ${(currentBrightness * 100).roundToInt()}%", fontSize = 12.sp)
                                Slider(
                                    value = currentBrightness,
                                    onValueChange = { newVal ->
                                        background = when (val bg = background) {
                                            is CardBackground.CustomImage -> bg.copy(brightness = newVal)
                                            is CardBackground.PresetImage -> bg.copy(brightness = newVal)
                                            else -> bg
                                        }
                                    },
                                    onValueChangeFinished = { saveState(layers, background) },
                                    valueRange = -1f..1f
                                )

                                // Contrast Slider
                                Text("Contrast: ${(currentContrast * 100).roundToInt()}%", fontSize = 12.sp)
                                Slider(
                                    value = currentContrast,
                                    onValueChange = { newVal ->
                                        background = when (val bg = background) {
                                            is CardBackground.CustomImage -> bg.copy(contrast = newVal)
                                            is CardBackground.PresetImage -> bg.copy(contrast = newVal)
                                            else -> bg
                                        }
                                    },
                                    onValueChangeFinished = { saveState(layers, background) },
                                    valueRange = 0.2f..3f
                                )

                                // Grayscale Slider
                                Text("Grayscale: ${(currentGrayscale * 100).roundToInt()}%", fontSize = 12.sp)
                                Slider(
                                    value = currentGrayscale,
                                    onValueChange = { newVal ->
                                        background = when (val bg = background) {
                                            is CardBackground.CustomImage -> bg.copy(grayscale = newVal)
                                            is CardBackground.PresetImage -> bg.copy(grayscale = newVal)
                                            else -> bg
                                        }
                                    },
                                    onValueChangeFinished = { saveState(layers, background) },
                                    valueRange = 0f..1f
                                )
                            }
                        }
                        2 -> { // Stickers Tab
                            Text("Islamic Stickers", fontWeight = FontWeight.Bold)
                            LazyRow(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(predefinedStickers) { resId ->
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val newLayer = ImageLayer(resId = resId, offset = Offset(150f, 150f))
                                                layers = layers + newLayer
                                                selectedLayerId = newLayer.id
                                                saveState(layers, background)
                                                com.example.utils.AnalyticsManager.logStickerUsed(context.resources.getResourceEntryName(resId))
                                            }
                                            .padding(8.dp)
                                    )
                                }
                            }
                            Text("Tap a sticker to add it to the canvas. Use pinch to zoom/rotate.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                        3 -> { // Layers Tab
                            Text("Layers", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (layers.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = cardMakerText("no_layers", currentLang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            layers.forEachIndexed { index, layer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedLayerId == layer.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
                                        .clickable { 
                                    if (!layer.isLocked) selectedLayerId = layer.id 
                                }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        when (layer) {
                                            is TextLayer -> layer.text.take(15) + if (layer.text.length > 15) "…" else ""
                                            is ImageLayer -> "Sticker"
                                        },
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        layers = layers.map { if (it.id == layer.id) {
                                            when (it) {
                                                is TextLayer -> it.copy(isLocked = !it.isLocked)
                                                is ImageLayer -> it.copy(isLocked = !it.isLocked)
                                            }
                                        } else it }
                                        saveState(layers, background)
                                    }) {
                                        Icon(if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = "Lock")
                                    }
                                    IconButton(onClick = {
                                        if (index < layers.size - 1) {
                                            val mutLayers = layers.toMutableList()
                                            mutLayers.removeAt(index)
                                            mutLayers.add(index + 1, layer)
                                            layers = mutLayers
                                            saveState(layers, background)
                                        }
                                    }) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Bring Forward")
                                    }
                                    IconButton(onClick = {
                                        if (index > 0) {
                                            val mutLayers = layers.toMutableList()
                                            mutLayers.removeAt(index)
                                            mutLayers.add(index - 1, layer)
                                            layers = mutLayers
                                            saveState(layers, background)
                                        }
                                    }) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Send Backward")
                                    }
                                    IconButton(onClick = {
                                        layers = layers.filter { it.id != layer.id }
                                        if (selectedLayerId == layer.id) selectedLayerId = null
                                        saveState(layers, background)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        4 -> { // AI & Settings Tab
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cardMakerText("branding", currentLang),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = cardMakerText("branding_sub", currentLang),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Switch(
                                        checked = showWatermark,
                                        onCheckedChange = { showWatermark = it }
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text("Aspect Ratio (Crop)", fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cardAspectRatios.forEach { (label, ratio) ->
                                    FilterChip(
                                        selected = aspectRatio == ratio,
                                        onClick = {
                                            aspectRatio = ratio
                                            saveState(layers, background)
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            Text("AI Text Suggestions", fontWeight = FontWeight.Bold)
                            var aiPrompt by remember { mutableStateOf("") }
                            var isGeneratingText by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = aiPrompt,
                                onValueChange = { aiPrompt = it },
                                label = { Text("Topic (e.g. Sabr, Jumma)") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                            Text(
                                com.example.utils.LocalizationUtility.get("ai_disclaimer", currentLang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                enabled = !isGeneratingText,
                                onClick = {
                                    scope.launch {
                                        isGeneratingText = true
                                        try {
                                            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                                            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    com.example.utils.LocalizationUtility.get("ai_unavailable", currentLang),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                return@launch
                                            }
                                            val responseLanguage = when (currentLang) {
                                                "ur" -> "Urdu"
                                                "ar" -> "Arabic"
                                                else -> "English"
                                            }
                                            val prompt = "Provide one short verified Islamic quotation or supplication about $aiPrompt in $responseLanguage, with its exact source. Never invent or misattribute religious text. If you are not completely certain of the wording and source, respond exactly NO_VERIFIED_QUOTE."
                                            val req = com.example.api.GenerateContentRequest(
                                                contents = listOf(
                                                    com.example.api.Content(
                                                        parts = listOf(com.example.api.Part(text = prompt))
                                                    )
                                                )
                                            )
                                            val response = com.example.api.DailyContentClient.service.generateContent(apiKey, req)
                                            val suggestion = response.candidates?.firstOrNull()?.content?.parts
                                                ?.firstOrNull()?.text?.trim()
                                            if (suggestion.isNullOrBlank() || suggestion.contains("NO_VERIFIED_QUOTE")) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    com.example.utils.LocalizationUtility.get("ai_no_response", currentLang),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val newLayer = TextLayer(
                                                    text = suggestion.removeSurrounding("\""),
                                                    offset = Offset(0f, 0f)
                                                )
                                                layers = layers + newLayer
                                                selectedLayerId = newLayer.id
                                                saveState(layers, background)
                                            }
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(
                                                context,
                                                com.example.utils.LocalizationUtility.get("ai_network_error", currentLang),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } finally {
                                            isGeneratingText = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isGeneratingText) "Generating..." else "Generate Text")
                            }

                            Spacer(Modifier.height(16.dp))
                            Text("Color Background Generator", fontWeight = FontWeight.Bold)
                            Button(onClick = {
                                // Deterministic local palette generation: no misleading AI claim,
                                // random third-party images, network dependency, or content risk.
                                background = generatedColorBackground(aiPrompt)
                                saveState(layers, background)
                            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Color Background")
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (editingTextLayerId != null) {
        val layerToEdit = layers.find { it.id == editingTextLayerId } as? TextLayer
        if (layerToEdit != null) {
            var tempText by remember { mutableStateOf(layerToEdit.text) }
            AlertDialog(
                onDismissRequest = { editingTextLayerId = null },
                title = { Text("Edit Text") },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        layers = layers.map { if (it.id == editingTextLayerId) (it as TextLayer).copy(text = tempText) else it }
                        editingTextLayerId = null
                        saveState(layers, background)
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingTextLayerId = null }) {
                        Text("Cancel")
                    }
                }
            )
        } else {
            editingTextLayerId = null
        }
    }

    if (showEraserDialog && selectedLayerId != null) {
        val activeLayer = layers.find { it.id == selectedLayerId } as? ImageLayer
        if (activeLayer != null) {
            var brushSize by remember { mutableStateOf(20f) }
            val activeErasedPaths = remember { mutableStateListOf<ErasedPath>().apply { addAll(activeLayer.erasedPaths) } }
            
            val undoList = remember { mutableStateListOf<List<ErasedPath>>() }
            val redoList = remember { mutableStateListOf<List<ErasedPath>>() }
            
            // Zoom & Pan State
            var zoomScale by remember { mutableStateOf(1f) }
            var zoomOffset by remember { mutableStateOf(Offset.Zero) }
            var isZoomPanMode by remember { mutableStateOf(false) } // false = Erase/Brush, true = Pan & Zoom
            
            // Cropping State
            var cropLeft by remember { mutableStateOf(activeLayer.cropLeft) }
            var cropTop by remember { mutableStateOf(activeLayer.cropTop) }
            var cropRight by remember { mutableStateOf(activeLayer.cropRight) }
            var cropBottom by remember { mutableStateOf(activeLayer.cropBottom) }
            
            Dialog(
                onDismissRequest = { showEraserDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            androidx.compose.material3.TopAppBar(
                                title = { Text("Manual Eraser Brush", fontWeight = FontWeight.Bold) },
                                navigationIcon = {
                                    IconButton(onClick = { showEraserDialog = false }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            if (undoList.isNotEmpty()) {
                                                redoList.add(activeErasedPaths.toList())
                                                val prev = undoList.removeAt(undoList.size - 1)
                                                activeErasedPaths.clear()
                                                activeErasedPaths.addAll(prev)
                                            }
                                        },
                                        enabled = undoList.isNotEmpty()
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            if (redoList.isNotEmpty()) {
                                                undoList.add(activeErasedPaths.toList())
                                                val next = redoList.removeAt(redoList.size - 1)
                                                activeErasedPaths.clear()
                                                activeErasedPaths.addAll(next)
                                            }
                                        },
                                        enabled = redoList.isNotEmpty()
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                                    }
                                    
                                    TextButton(onClick = {
                                        undoList.add(activeErasedPaths.toList())
                                        redoList.clear()
                                        activeErasedPaths.clear()
                                        cropLeft = 0f
                                        cropTop = 0f
                                        cropRight = 1f
                                        cropBottom = 1f
                                        zoomScale = 1f
                                        zoomOffset = Offset.Zero
                                    }) {
                                        Text("Reset All", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                    ) { pad ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(pad)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            var componentWidth by remember { mutableStateOf(0f) }
                            var componentHeight by remember { mutableStateOf(0f) }

                            Text(
                                text = if (isZoomPanMode) "Use two fingers to pinch-to-zoom or drag to pan the sticker canvas." else "Drag your finger over the sticker to erase background manually.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val imageBitmap = remember(activeLayer.resId) {
                                    val drawable = androidx.core.content.res.ResourcesCompat.getDrawable(context.resources, activeLayer.resId, null)
                                    drawable?.let { drawableToBitmap(it).asImageBitmap() }
                                }
                                
                                CheckerboardBackground(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                ) {
                                    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
                                    
                                    if (imageBitmap != null) {
                                        androidx.compose.foundation.Canvas(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .onSizeChanged { size ->
                                                    componentWidth = size.width.toFloat()
                                                    componentHeight = size.height.toFloat()
                                                }
                                                .pointerInput(isZoomPanMode, zoomScale, zoomOffset, cropLeft, cropTop, cropRight, cropBottom, componentWidth, componentHeight, brushSize) {
                                                    if (componentWidth > 0f && componentHeight > 0f) {
                                                        if (isZoomPanMode) {
                                                            detectTransformGestures { _, pan, zoom, _ ->
                                                                zoomScale = (zoomScale * zoom).coerceIn(1f, 8f)
                                                                val maxOffsetX = (zoomScale - 1f) * componentWidth / 2f
                                                                val maxOffsetY = (zoomScale - 1f) * componentHeight / 2f
                                                                zoomOffset = Offset(
                                                                    (zoomOffset.x + pan.x).coerceIn(-maxOffsetX - 100f, maxOffsetX + 100f),
                                                                    (zoomOffset.y + pan.y).coerceIn(-maxOffsetY - 100f, maxOffsetY + 100f)
                                                                )
                                                            }
                                                        } else {
                                                            awaitPointerEventScope {
                                                                while (true) {
                                                                    val down = awaitFirstDown()
                                                                    val cropScale = 1f / (cropRight - cropLeft).coerceAtLeast(0.01f)
                                                                    
                                                                    val downXUnzoom = (down.position.x - zoomOffset.x) / (zoomScale * componentWidth)
                                                                    val downYUnzoom = (down.position.y - zoomOffset.y) / (zoomScale * componentHeight)
                                                                    val downX = downXUnzoom / cropScale + cropLeft
                                                                    val downY = downYUnzoom / cropScale + cropTop
                                                                    
                                                                    var points = listOf(Offset(downX, downY))
                                                                    currentPathPoints = points
                                                                    
                                                                    var dragEvent = awaitPointerEvent()
                                                                    while (dragEvent.changes.any { it.pressed }) {
                                                                        val change = dragEvent.changes.firstOrNull { it.pressed }
                                                                        if (change != null) {
                                                                            change.consume()
                                                                            val dragXUnzoom = (change.position.x - zoomOffset.x) / (zoomScale * componentWidth)
                                                                            val dragYUnzoom = (change.position.y - zoomOffset.y) / (zoomScale * componentHeight)
                                                                            val dragX = dragXUnzoom / cropScale + cropLeft
                                                                            val dragY = dragYUnzoom / cropScale + cropTop
                                                                            points = points + Offset(dragX, dragY)
                                                                            currentPathPoints = points
                                                                        }
                                                                        dragEvent = awaitPointerEvent()
                                                                    }
                                                                    
                                                                    if (points.isNotEmpty()) {
                                                                        undoList.add(activeErasedPaths.toList())
                                                                        redoList.clear()
                                                                        val saveBrushSize = brushSize / (zoomScale * cropScale)
                                                                        activeErasedPaths.add(ErasedPath(points, saveBrushSize))
                                                                    }
                                                                    currentPathPoints = emptyList()
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                        ) {
                                            if (componentWidth > 0 && componentHeight > 0) {
                                                drawIntoCanvas { canvas ->
                                                    val paint = android.graphics.Paint().apply { isAntiAlias = true }
                                                    val saveCount = canvas.nativeCanvas.saveLayer(0f, 0f, size.width, size.height, null)
                                                    
                                                    canvas.nativeCanvas.translate(zoomOffset.x, zoomOffset.y)
                                                    canvas.nativeCanvas.scale(zoomScale, zoomScale)
                                                    
                                                    val cropScale = 1f / (cropRight - cropLeft).coerceAtLeast(0.01f)
                                                    if (cropLeft > 0f || cropTop > 0f || cropRight < 1f || cropBottom < 1f) {
                                                        canvas.nativeCanvas.scale(cropScale, cropScale)
                                                        canvas.nativeCanvas.translate(-cropLeft * size.width, -cropTop * size.height)
                                                    }
                                                    
                                                    val srcRect = android.graphics.Rect(0, 0, imageBitmap.width, imageBitmap.height)
                                                    val dstRect = android.graphics.RectF(0f, 0f, size.width, size.height)
                                                    canvas.nativeCanvas.drawBitmap(
                                                        imageBitmap.asAndroidBitmap(),
                                                        srcRect,
                                                        dstRect,
                                                        paint
                                                    )
                                                    
                                                    val eraserPaint = android.graphics.Paint().apply {
                                                        isAntiAlias = true
                                                        style = android.graphics.Paint.Style.STROKE
                                                        strokeCap = android.graphics.Paint.Cap.ROUND
                                                        strokeJoin = android.graphics.Paint.Join.ROUND
                                                        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                                    }
                                                    
                                                    activeErasedPaths.forEach { erasedPath ->
                                                        if (erasedPath.points.size > 1) {
                                                            val path = android.graphics.Path()
                                                            val first = erasedPath.points.first()
                                                            path.moveTo(first.x * size.width, first.y * size.height)
                                                            for (i in 1 until erasedPath.points.size) {
                                                                val pt = erasedPath.points[i]
                                                                path.lineTo(pt.x * size.width, pt.y * size.height)
                                                            }
                                                            eraserPaint.strokeWidth = erasedPath.brushSize
                                                            canvas.nativeCanvas.drawPath(path, eraserPaint)
                                                        } else if (erasedPath.points.size == 1) {
                                                            val pt = erasedPath.points.first()
                                                            val dotPaint = android.graphics.Paint(eraserPaint).apply {
                                                                style = android.graphics.Paint.Style.FILL
                                                            }
                                                            canvas.nativeCanvas.drawCircle(pt.x * size.width, pt.y * size.height, erasedPath.brushSize / 2f, dotPaint)
                                                        }
                                                    }
                                                    
                                                    if (currentPathPoints.size > 1) {
                                                        val path = android.graphics.Path()
                                                        val first = currentPathPoints.first()
                                                        path.moveTo(first.x * size.width, first.y * size.height)
                                                        for (i in 1 until currentPathPoints.size) {
                                                            val pt = currentPathPoints[i]
                                                            path.lineTo(pt.x * size.width, pt.y * size.height)
                                                        }
                                                        eraserPaint.strokeWidth = brushSize / (zoomScale * cropScale)
                                                        canvas.nativeCanvas.drawPath(path, eraserPaint)
                                                    } else if (currentPathPoints.size == 1) {
                                                        val pt = currentPathPoints.first()
                                                        val dotPaint = android.graphics.Paint(eraserPaint).apply {
                                                            style = android.graphics.Paint.Style.FILL
                                                        }
                                                        canvas.nativeCanvas.drawCircle(pt.x * size.width, pt.y * size.height, (brushSize / (zoomScale * cropScale)) / 2f, dotPaint)
                                                    }
                                                    
                                                    canvas.nativeCanvas.restoreToCount(saveCount)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Mode Toggle Row (Brush Mode vs Pan/Zoom)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { isZoomPanMode = false },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (!isZoomPanMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = if (!isZoomPanMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Brush Mode", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        
                                        Button(
                                            onClick = { isZoomPanMode = true },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isZoomPanMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = if (isZoomPanMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pan & Zoom", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        
                                        OutlinedButton(
                                            onClick = {
                                                zoomScale = 1f
                                                zoomOffset = Offset.Zero
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Reset Zoom", fontSize = 10.sp)
                                        }
                                    }
                                    
                                    // Cropping Action Row (Auto-Trim / Reset Crop)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val originalD = androidx.core.content.res.ResourcesCompat.getDrawable(context.resources, activeLayer.resId, null)
                                                if (originalD != null) {
                                                    val originalB = drawableToBitmap(originalD)
                                                    val autoBounds = calculateAutoTrimBounds(
                                                        originalBitmap = originalB,
                                                        erasedPaths = activeErasedPaths.toList(),
                                                        componentWidth = componentWidth
                                                     )
                                                     cropLeft = autoBounds.left
                                                     cropTop = autoBounds.top
                                                     cropRight = autoBounds.right
                                                     cropBottom = autoBounds.bottom
                                                     android.widget.Toast.makeText(context, "Auto-trimmed transparent borders!", android.widget.Toast.LENGTH_SHORT).show()
                                                 }
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Auto-Trim", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        
                                        OutlinedButton(
                                            onClick = {
                                                cropLeft = 0f
                                                cropTop = 0f
                                                cropRight = 1f
                                                cropBottom = 1f
                                                android.widget.Toast.makeText(context, "Reset crop bounds!", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset Crop", fontSize = 11.sp)
                                        }
                                    }
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    
                                    // Brush Size Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Brush: ${brushSize.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(64.dp)
                                        )
                                        Slider(
                                            value = brushSize,
                                            onValueChange = { brushSize = it },
                                            valueRange = 5f..80f,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier.size(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size((brushSize / 2).coerceIn(2f, 22f).dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Save / Exit Action Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showEraserDialog = false },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Cancel")
                                        }
                                        
                                        Button(
                                            onClick = {
                                                val prevL = activeLayer.cropLeft
                                                val prevT = activeLayer.cropTop
                                                val prevR = activeLayer.cropRight
                                                val prevB = activeLayer.cropBottom
                                                
                                                val newL = cropLeft
                                                val newT = cropTop
                                                val newR = cropRight
                                                val newB = cropBottom
                                                
                                                val prevCenterX = (prevL + prevR) / 2f
                                                val prevCenterY = (prevT + prevB) / 2f
                                                val newCenterX = (newL + newR) / 2f
                                                val newCenterY = (newT + newB) / 2f
                                                
                                                val shiftX = newCenterX - prevCenterX
                                                val shiftY = newCenterY - prevCenterY
                                                
                                                val rawShiftX = shiftX * 100f * activeLayer.scale
                                                val rawShiftY = shiftY * 100f * activeLayer.scale
                                                
                                                val angleRad = Math.toRadians(activeLayer.rotation.toDouble())
                                                val cosA = Math.cos(angleRad)
                                                val sinA = Math.sin(angleRad)
                                                val rotatedShiftX = (rawShiftX * cosA - rawShiftY * sinA).toFloat()
                                                val rotatedShiftY = (rawShiftX * sinA + rawShiftY * cosA).toFloat()
                                                
                                                val finalOffset = activeLayer.offset + Offset(rotatedShiftX, rotatedShiftY)
                                                
                                                layers = layers.map { 
                                                    if (it.id == activeLayer.id) {
                                                        (it as ImageLayer).copy(
                                                            erasedPaths = activeErasedPaths.toList(),
                                                            cropLeft = newL,
                                                            cropTop = newT,
                                                            cropRight = newR,
                                                            cropBottom = newB,
                                                            offset = finalOffset
                                                        )
                                                    } else it 
                                                }
                                                showEraserDialog = false
                                                saveState(layers, background)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Apply & Save")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingTemplate = null },
            title = {
                Text(
                    text = cardMakerText("apply_template", currentLang),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "${cardMakerText(template.titleKey, currentLang)}\n\n${cardMakerText("template_confirm", currentLang)}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLayers = createCardTemplateLayers(template, currentLang)
                        layers = newLayers
                        background = template.background
                        aspectRatio = template.aspectRatio
                        selectedLayerId = newLayers.first().id
                        editingTextLayerId = null
                        pendingTemplate = null
                        saveState(newLayers, template.background)
                        android.widget.Toast.makeText(
                            context,
                            cardMakerText("template_applied", currentLang),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text(cardMakerText("apply", currentLang))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTemplate = null }) {
                    Text(cardMakerText("cancel", currentLang))
                }
            }
        )
    }

    if (showOverwriteDraftDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteDraftDialog = false },
            title = {
                Text(
                    text = "Load New Quote / نیا کلام لوڈ کریں",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "You have an existing draft in Card Maker. Would you like to discard it and load this new text?\n\n" +
                           "کارڈ میکر میں پہلے سے ایک ڈیزائن موجود ہے۔ کیا آپ اسے ختم کر کے نیا کلام لوڈ کرنا چاہتے ہیں؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverwriteDraftDialog = false
                        clearDraftFromPrefs(context)
                        initializeDefault(initialQuote)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard & Load / نیا لوڈ کریں")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOverwriteDraftDialog = false
                    }
                ) {
                    Text("Keep Draft / پرانا رکھیں")
                }
            }
        )
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = {
                Text(text = "Unsaved Changes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            },
            text = {
                Text(text = "You have unsaved changes to your card. What would you like to do?")
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        showUnsavedDialog = false
                        captureForSaveTrigger = true
                        kotlinx.coroutines.delay(50) // wait for recomposition and draw
                        val bitmap = createCardExportBitmap(picture)
                        ShareService.saveBitmapToGallery(
                            context = context,
                            bitmap = bitmap,
                            scope = scope,
                            onSuccess = {
                                android.widget.Toast.makeText(context, cardMakerText("hd_saved", currentLang), android.widget.Toast.LENGTH_SHORT).show()
                                clearDraftFromPrefs(context)
                                onBack()
                            },
                            onError = {
                                android.widget.Toast.makeText(context, "Failed to save: $it", android.widget.Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        )
                        captureForSaveTrigger = false
                    }
                }) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { 
                        showUnsavedDialog = false
                        clearDraftFromPrefs(context)
                        onBack()
                    }) {
                        Text("Discard Changes", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("Continue Editing")
                    }
                }
            }
        )
    }
}

fun calculateAutoTrimBounds(
    originalBitmap: Bitmap,
    erasedPaths: List<ErasedPath>,
    componentWidth: Float
): android.graphics.RectF {
    val width = originalBitmap.width
    val height = originalBitmap.height
    if (width <= 0 || height <= 0) return android.graphics.RectF(0f, 0f, 1f, 1f)
    
    val processed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(processed)
    val paint = android.graphics.Paint().apply { isAntiAlias = true }
    canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
    
    val eraserPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
    }
    
    erasedPaths.forEach { erasedPath ->
        if (erasedPath.points.size > 1) {
            val path = android.graphics.Path()
            val first = erasedPath.points.first()
            path.moveTo(first.x * width, first.y * height)
            for (i in 1 until erasedPath.points.size) {
                val pt = erasedPath.points[i]
                path.lineTo(pt.x * width, pt.y * height)
            }
            val scaleFactor = width / componentWidth.coerceAtLeast(1f)
            eraserPaint.strokeWidth = erasedPath.brushSize * scaleFactor
            canvas.drawPath(path, eraserPaint)
        } else if (erasedPath.points.size == 1) {
            val pt = erasedPath.points.first()
            val dotPaint = android.graphics.Paint(eraserPaint).apply {
                style = android.graphics.Paint.Style.FILL
            }
            val scaleFactor = width / componentWidth.coerceAtLeast(1f)
            canvas.drawCircle(pt.x * width, pt.y * height, (erasedPath.brushSize * scaleFactor) / 2f, dotPaint)
        }
    }
    
    var minX = width
    var maxX = 0
    var minY = height
    var maxY = 0
    
    val pixels = IntArray(width * height)
    processed.getPixels(pixels, 0, width, 0, 0, width, height)
    
    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val pixel = pixels[rowOffset + x]
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha > 15) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }
    
    processed.recycle()
    
    if (maxX < minX || maxY < minY) {
        return android.graphics.RectF(0f, 0f, 1f, 1f)
    }
    
    val padX = (width * 0.01f).roundToInt()
    val padY = (height * 0.01f).roundToInt()
    
    val left = ((minX - padX).coerceAtLeast(0).toFloat()) / width
    val top = ((minY - padY).coerceAtLeast(0).toFloat()) / height
    val right = ((maxX + padX).coerceIn(0, width).toFloat()) / width
    val bottom = ((maxY + padY).coerceIn(0, height).toFloat()) / height
    
    return android.graphics.RectF(left, top, right, bottom)
}

data class DraftJson(
    val bgType: String,
    val bgColorArgb: Int?,
    val bgColorsArgb: List<Int>?,
    val bgUriStr: String?,
    val bgResId: Int?,
    val bgBrightness: Float?,
    val bgContrast: Float?,
    val bgGrayscale: Float?,
    val aspectRatio: Float,
    val layers: List<DraftLayerJson>
)

data class DraftLayerJson(
    val type: String, // "text" or "image"
    val id: String,
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float,
    val rotation: Float,
    val isLocked: Boolean,
    val flipX: Boolean,
    val flipY: Boolean,
    
    // Text fields
    val text: String? = null,
    val colorArgb: Int? = null,
    val fontName: String? = null,
    val fontSize: Float? = null,
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val isUnderline: Boolean? = null,
    val hasShadow: Boolean? = null,
    val shadowColorArgb: Int? = null,
    val shadowBlur: Float? = null,
    val shadowDistance: Float? = null,
    val shadowOpacity: Float? = null,
    val hasOutline: Boolean? = null,
    val outlineColorArgb: Int? = null,
    val outlineWidth: Float? = null,
    val isCurved: Boolean? = null,
    val curveAngle: Float? = null,
    val textShape: String? = null,
    
    
    val alignmentStr: String? = null,
    
    // Image fields
    val resId: Int? = null,
    val cropLeft: Float? = null,
    val cropTop: Float? = null,
    val cropRight: Float? = null,
    val cropBottom: Float? = null,
    val erasedPaths: List<DraftErasedPathJson>? = null
)

data class DraftErasedPathJson(
    val points: List<DraftPointJson>,
    val brushSize: Float
)

data class DraftPointJson(
    val x: Float,
    val y: Float
)

fun CardBackground.toType(): String = when (this) {
    is CardBackground.Solid -> "solid"
    is CardBackground.Gradient -> "gradient"
    is CardBackground.CustomImage -> "custom_image"
    is CardBackground.PresetImage -> "preset_image"
}

fun CardLayer.toDraftLayerJson(): DraftLayerJson {
    return when (this) {
        is TextLayer -> {
            DraftLayerJson(
                type = "text",
                id = id,
                offsetX = offset.x,
                offsetY = offset.y,
                scale = scale,
                rotation = rotation,
                isLocked = isLocked,
                flipX = flipX,
                flipY = flipY,
                text = text,
                colorArgb = color.toArgb(),
                fontName = fontName,
                fontSize = fontSize,
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                hasShadow = hasShadow,
                shadowColorArgb = shadowColor.toArgb(),
                shadowBlur = shadowBlur,
                shadowDistance = shadowDistance,
                shadowOpacity = shadowOpacity,
                hasOutline = hasOutline,
                outlineColorArgb = outlineColor.toArgb(),
                outlineWidth = outlineWidth,
                isCurved = isCurved,
                curveAngle = curveAngle,
                textShape = textShape,
                alignmentStr = getTextAlignString(alignment)
            )
        }
        is ImageLayer -> {
            DraftLayerJson(
                type = "image",
                id = id,
                offsetX = offset.x,
                offsetY = offset.y,
                scale = scale,
                rotation = rotation,
                isLocked = isLocked,
                flipX = flipX,
                flipY = flipY,
                resId = resId,
                cropLeft = cropLeft,
                cropTop = cropTop,
                cropRight = cropRight,
                cropBottom = cropBottom,
                erasedPaths = erasedPaths.map { path ->
                    DraftErasedPathJson(
                        points = path.points.map { DraftPointJson(it.x, it.y) },
                        brushSize = path.brushSize
                    )
                }
            )
        }
    }
}

fun DraftLayerJson.toCardLayer(): CardLayer {
    return when (type) {
        "text" -> {
            TextLayer(
                id = id,
                text = text ?: "",
                color = Color(colorArgb ?: Color.Black.toArgb()),
                fontName = fontName ?: "Roboto",
                fontSize = fontSize ?: 20f,
                isBold = isBold ?: false,
                isItalic = isItalic ?: false,
                isUnderline = isUnderline ?: false,
                hasShadow = hasShadow ?: false,
                shadowColor = Color(shadowColorArgb ?: Color.Black.toArgb()),
                shadowBlur = shadowBlur ?: 4f,
                shadowDistance = shadowDistance ?: 4f,
                shadowOpacity = shadowOpacity ?: 0.5f,
                hasOutline = hasOutline ?: false,
                outlineColor = Color(outlineColorArgb ?: Color.White.toArgb()),
                outlineWidth = outlineWidth ?: 2f,
                isCurved = isCurved ?: false,
                curveAngle = curveAngle ?: 180f,
                textShape = textShape ?: (if (isCurved == true) "CURVED_UP" else "NORMAL"),
                alignment = parseTextAlign(alignmentStr),
                offset = Offset(offsetX, offsetY),
                scale = scale,
                rotation = rotation,
                isLocked = isLocked,
                flipX = flipX,
                flipY = flipY
            )
        }
        "image" -> {
            ImageLayer(
                id = id,
                resId = resId ?: 0,
                erasedPaths = erasedPaths?.map { path ->
                    ErasedPath(
                        points = path.points.map { Offset(it.x, it.y) },
                        brushSize = path.brushSize
                    )
                } ?: emptyList(),
                cropLeft = cropLeft ?: 0f,
                cropTop = cropTop ?: 0f,
                cropRight = cropRight ?: 1f,
                cropBottom = cropBottom ?: 1f,
                offset = Offset(offsetX, offsetY),
                scale = scale,
                rotation = rotation,
                isLocked = isLocked,
                flipX = flipX,
                flipY = flipY
            )
        }
        else -> throw IllegalArgumentException("Unknown layer type: $type")
    }
}

fun restoreBackground(
    type: String,
    colorArgb: Int?,
    colorsArgb: List<Int>?,
    uriStr: String?,
    resId: Int?,
    brightness: Float?,
    contrast: Float?,
    grayscale: Float?
): CardBackground {
    return when (type) {
        "solid" -> CardBackground.Solid(Color(colorArgb ?: Color.White.toArgb()))
        "gradient" -> CardBackground.Gradient(
            colorsArgb?.map { Color(it) } ?: listOf(Color.White, Color.LightGray)
        )
        "custom_image" -> CardBackground.CustomImage(
            uri = Uri.parse(uriStr ?: ""),
            brightness = brightness ?: 0f,
            contrast = contrast ?: 1f,
            grayscale = grayscale ?: 0f
        )
        "preset_image" -> CardBackground.PresetImage(
            resId = resId ?: 0,
            brightness = brightness ?: 0f,
            contrast = contrast ?: 1f,
            grayscale = grayscale ?: 0f
        )
        else -> CardBackground.Solid(Color.White)
    }
}

fun saveDraftToPrefs(context: Context, layers: List<CardLayer>, background: CardBackground, aspectRatio: Float) {
    try {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(DraftJson::class.java)
        
        val bgType = background.toType()
        val bgColorArgb = if (background is CardBackground.Solid) background.color.toArgb() else null
        val bgColorsArgb = if (background is CardBackground.Gradient) background.colors.map { it.toArgb() } else null
        val bgUriStr = if (background is CardBackground.CustomImage) background.uri.toString() else null
        val bgResId = if (background is CardBackground.PresetImage) background.resId else null
        val bgBrightness = when (background) {
            is CardBackground.CustomImage -> background.brightness
            is CardBackground.PresetImage -> background.brightness
            else -> null
        }
        val bgContrast = when (background) {
            is CardBackground.CustomImage -> background.contrast
            is CardBackground.PresetImage -> background.contrast
            else -> null
        }
        val bgGrayscale = when (background) {
            is CardBackground.CustomImage -> background.grayscale
            is CardBackground.PresetImage -> background.grayscale
            else -> null
        }
        
        val draft = DraftJson(
            bgType = bgType,
            bgColorArgb = bgColorArgb,
            bgColorsArgb = bgColorsArgb,
            bgUriStr = bgUriStr,
            bgResId = bgResId,
            bgBrightness = bgBrightness,
            bgContrast = bgContrast,
            bgGrayscale = bgGrayscale,
            aspectRatio = aspectRatio,
            layers = layers.map { it.toDraftLayerJson() }
        )
        
        val json = adapter.toJson(draft)
        val prefs = context.getSharedPreferences("card_maker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_draft", json).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadDraftFromPrefs(context: Context): DraftJson? {
    return try {
        val prefs = context.getSharedPreferences("card_maker_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_draft", null) ?: return null
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(DraftJson::class.java)
        adapter.fromJson(json)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun clearDraftFromPrefs(context: Context) {
    val prefs = context.getSharedPreferences("card_maker_prefs", Context.MODE_PRIVATE)
    prefs.edit().remove("saved_draft").apply()
}

fun getTextAlignString(alignment: TextAlign): String {
    return when (alignment) {
        TextAlign.Left -> "Left"
        TextAlign.Right -> "Right"
        TextAlign.Justify -> "Justify"
        else -> "Center"
    }
}

fun parseTextAlign(str: String?): TextAlign {
    return when (str) {
        "Left" -> TextAlign.Left
        "Right" -> TextAlign.Right
        "Justify" -> TextAlign.Justify
        else -> TextAlign.Center
    }
}
