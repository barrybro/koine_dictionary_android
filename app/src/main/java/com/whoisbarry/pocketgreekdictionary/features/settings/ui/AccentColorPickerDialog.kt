package com.whoisbarry.pocketgreekdictionary.features.settings.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.util.Locale

private val SaturationValueHeight = 200.dp
private val HueSliderHeight = 28.dp

/**
 * An interactive colour picker: a saturation/brightness field over a hue slider, both of which
 * follow a dragged finger. The colour is only applied if the user confirms, so backing out with
 * Cancel leaves the app on whatever accent it already had.
 */
@Composable
fun AccentColorPickerDialog(
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // HSV rather than RGB: it is what makes a two-control picker feel continuous to drag.
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }

    val color = Color.hsv(hue, saturation, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Accent Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SaturationValueField(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onChange = { newSaturation, newValue ->
                        saturation = newSaturation
                        value = newValue
                    }
                )

                HueSlider(hue = hue, onHueChange = { hue = it })

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorSwatch(color = color, modifier = Modifier.size(40.dp))
                    Text(
                        text = color.toHexString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

/** A circular swatch of [color], outlined so that white and near-white stay visible. */
@Composable
fun ColorSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}

@Composable
private fun SaturationValueField(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(SaturationValueHeight)
            .clip(RoundedCornerShape(12.dp))
            .trackPress { position, size ->
                onChange(
                    (position.x / size.width).coerceIn(0f, 1f),
                    1f - (position.y / size.height).coerceIn(0f, 1f)
                )
            }
    ) {
        // White to the fully saturated hue across, then shaded to black down.
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        drawSelectionRing(
            center = Offset(saturation * size.width, (1f - value) * size.height),
            radius = 10.dp.toPx()
        )
    }
}

@Composable
private fun HueSlider(hue: Float, onHueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val hues = remember { List(7) { Color.hsv(it * 60f, 1f, 1f) } }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(HueSliderHeight)
            .clip(RoundedCornerShape(percent = 50))
            .trackPress { position, size ->
                onHueChange((position.x / size.width).coerceIn(0f, 1f) * 360f)
            }
    ) {
        drawRect(Brush.horizontalGradient(hues))

        drawSelectionRing(
            center = Offset(hue / 360f * size.width, size.height / 2f),
            radius = size.height / 2f - 3.dp.toPx()
        )
    }
}

/**
 * The thumb shared by both controls: a white ring with a dark hairline, which stays visible over
 * every colour underneath it.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSelectionRing(
    center: Offset,
    radius: Float
) {
    // Held far enough inside the control that the rounded clip never cuts the ring in half when
    // the selection is at an edge or a corner.
    val inset = radius + 2.dp.toPx()
    val clamped = Offset(
        center.x.coerceIn(inset, size.width - inset),
        center.y.coerceIn(inset, size.height - inset)
    )

    drawCircle(Color.Black.copy(alpha = 0.4f), radius, clamped, style = Stroke(3.dp.toPx()))
    drawCircle(Color.White, radius, clamped, style = Stroke(2.dp.toPx()))
}

/**
 * Reports the touch position for the whole press, so the control follows a dragged finger instead
 * of only reacting to a tap.
 *
 * [onPick] is captured once for the life of the gesture detector, so it must only write to
 * remembered state — which is all either caller does.
 */
private fun Modifier.trackPress(onPick: (position: Offset, size: IntSize) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            onPick(down.position, size)

            var pressed = true
            while (pressed) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: event.changes.first()
                onPick(change.position, size)
                pressed = change.pressed
                change.consume()
            }
        }
    }

/** The `#RRGGBB` form shown next to the swatch, so a colour can be noted down and recognised. */
fun Color.toHexString(): String = String.format(Locale.US, "#%06X", 0xFFFFFF and toArgb())
