package com.whoisbarry.pocketgreekdictionary.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.whoisbarry.pocketgreekdictionary.R
import com.whoisbarry.pocketgreekdictionary.data.models.DictionaryEntry

private const val IMAGE_WIDTH = 1080
private const val MIN_IMAGE_HEIGHT = 1080

private const val PAGE_MARGIN = 56f
private const val CARD_PADDING = 64f
private const val CARD_RADIUS = 44f
private const val FOOTER_AREA = 96f

private const val WORD_TEXT_SIZE = 128f
private const val GLOSS_TEXT_SIZE = 60f
private const val SOURCE_TEXT_SIZE = 38f
private const val FOOTER_TEXT_SIZE = 34f

private const val GAP_AFTER_WORD = 36f
private const val GAP_BEFORE_RULE = 44f
private const val GAP_AFTER_RULE = 28f

private const val COLOR_BACKGROUND_TOP = 0xFFEAF2FF.toInt()
private const val COLOR_BACKGROUND_BOTTOM = 0xFFFFFFFF.toInt()
private const val COLOR_CARD = 0xFFFFFFFF.toInt()
private const val COLOR_PRIMARY = 0xFF007FFF.toInt()
private const val COLOR_ON_SURFACE = 0xFF1B1B1F.toInt()
private const val COLOR_ON_SURFACE_VARIANT = 0xFF49454F.toInt()
private const val COLOR_RULE = 0xFFE2E5EC.toInt()
private const val COLOR_SHADOW = 0x33000000

/** Scale factors tried in order until the whole entry fits the standard image height. */
private val TEXT_SCALES = listOf(1f, 0.92f, 0.84f, 0.76f, 0.68f, 0.6f, 0.52f, 0.45f)

/**
 * Renders a dictionary entry as a standalone, social-media friendly image.
 *
 * The image is drawn off-screen rather than captured from the view hierarchy, so the result does
 * not depend on what is currently visible. Text is scaled down to fit the standard square canvas
 * and, if an entry is long enough that even the smallest scale does not fit, the canvas grows
 * instead of clipping.
 */
fun renderEntryImage(context: Context, entry: DictionaryEntry): Bitmap {
    val contentWidth = (IMAGE_WIDTH - 2 * PAGE_MARGIN - 2 * CARD_PADDING).toInt()
    val word = entry.word.ifBlank { entry.fullWord }
    val gloss = entry.gloss.trim()
    val source = entry.sourceName.trim()

    var block = layoutContent(word, gloss, source, contentWidth, TEXT_SCALES.last())
    var scale = TEXT_SCALES.last()
    for (candidate in TEXT_SCALES) {
        val candidateBlock = layoutContent(word, gloss, source, contentWidth, candidate)
        if (candidateBlock.height + 2 * CARD_PADDING + 2 * PAGE_MARGIN + FOOTER_AREA <= MIN_IMAGE_HEIGHT) {
            block = candidateBlock
            scale = candidate
            break
        }
    }

    val cardHeight = block.height + 2 * CARD_PADDING
    val imageHeight = maxOf(
        MIN_IMAGE_HEIGHT.toFloat(),
        cardHeight + 2 * PAGE_MARGIN + FOOTER_AREA
    ).toInt()

    val bitmap = createBitmap(IMAGE_WIDTH, imageHeight)
    val canvas = Canvas(bitmap)

    drawBackground(canvas, imageHeight)

    val cardTop = (imageHeight - FOOTER_AREA - cardHeight) / 2f
    val cardRect = RectF(
        PAGE_MARGIN,
        cardTop,
        IMAGE_WIDTH - PAGE_MARGIN,
        cardTop + cardHeight
    )
    drawCard(canvas, cardRect)

    val contentLeft = cardRect.left + CARD_PADDING
    var y = cardRect.top + CARD_PADDING

    y = canvas.drawLayoutAt(block.word, contentLeft, y)
    if (block.gloss != null) {
        y += GAP_AFTER_WORD * scale
        y = canvas.drawLayoutAt(block.gloss, contentLeft, y)
    }
    if (block.source != null) {
        y += GAP_BEFORE_RULE * scale
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_RULE
            strokeWidth = 2f
        }
        canvas.drawLine(contentLeft, y, contentLeft + contentWidth, y, rulePaint)
        y += GAP_AFTER_RULE * scale
        canvas.drawLayoutAt(block.source, contentLeft, y)
    }

    drawFooter(canvas, context.getString(R.string.app_name), imageHeight)

    return bitmap
}

private class ContentBlock(
    val word: StaticLayout,
    val gloss: StaticLayout?,
    val source: StaticLayout?,
    val height: Float
)

private fun layoutContent(
    word: String,
    gloss: String,
    source: String,
    width: Int,
    scale: Float
): ContentBlock {
    val wordLayout = buildLayout(word, wordPaint(scale), width)
    val glossLayout = gloss.takeIf { it.isNotEmpty() }
        ?.let { buildLayout(it, glossPaint(scale), width) }
    val sourceLayout = source.takeIf { it.isNotEmpty() }
        ?.let { buildLayout(it, sourcePaint(scale), width, Layout.Alignment.ALIGN_OPPOSITE) }

    var height = wordLayout.height.toFloat()
    if (glossLayout != null) {
        height += GAP_AFTER_WORD * scale + glossLayout.height
    }
    if (sourceLayout != null) {
        height += GAP_BEFORE_RULE * scale + GAP_AFTER_RULE * scale + sourceLayout.height
    }

    return ContentBlock(wordLayout, glossLayout, sourceLayout, height)
}

private fun buildLayout(
    text: String,
    paint: TextPaint,
    width: Int,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(alignment)
        .setLineSpacing(0f, 1.15f)
        .setIncludePad(false)
        .build()

private fun wordPaint(scale: Float) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = COLOR_PRIMARY
    textSize = WORD_TEXT_SIZE * scale
    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
}

private fun glossPaint(scale: Float) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = COLOR_ON_SURFACE
    textSize = GLOSS_TEXT_SIZE * scale
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
}

private fun sourcePaint(scale: Float) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = COLOR_ON_SURFACE_VARIANT
    textSize = SOURCE_TEXT_SIZE * scale
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
}

private fun drawBackground(canvas: Canvas, imageHeight: Int) {
    val paint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, imageHeight.toFloat(),
            COLOR_BACKGROUND_TOP, COLOR_BACKGROUND_BOTTOM,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), imageHeight.toFloat(), paint)
}

private fun drawCard(canvas: Canvas, rect: RectF) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_CARD
        setShadowLayer(28f, 0f, 10f, COLOR_SHADOW)
    }
    canvas.drawRoundRect(rect, CARD_RADIUS, CARD_RADIUS, paint)
}

private fun drawFooter(canvas: Canvas, appName: String, imageHeight: Int) {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_ON_SURFACE_VARIANT
        textSize = FOOTER_TEXT_SIZE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val baseline = imageHeight - PAGE_MARGIN
    canvas.drawText(appName, IMAGE_WIDTH / 2f, baseline, paint)
}

/** Draws [layout] with its top-left corner at ([left], [top]) and returns the resulting bottom. */
private fun Canvas.drawLayoutAt(layout: StaticLayout, left: Float, top: Float): Float {
    save()
    translate(left, top)
    layout.draw(this)
    restore()
    return top + layout.height
}

private fun createBitmap(width: Int, height: Int): Bitmap =
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.WHITE)
    }
