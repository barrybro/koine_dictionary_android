package com.whoisbarry.pocketgreekdictionary.features.dictionary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whoisbarry.pocketgreekdictionary.data.models.DictionaryEntry
import kotlinx.coroutines.launch

/** Width of the alphabet index track, wide enough to be a comfortable drag target. */
private val IndexWidth = 30.dp

/** The index never grows past these, however much vertical room it is given. */
private val MaxIndexFontSize = 11.sp
private val MaxIndexSlotHeight = 24.dp

@Composable
fun DictionaryScreen(viewModel: DictionaryViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedEntry by viewModel.selectedEntry.collectAsState()

    if (selectedEntry != null) {
        BackHandler {
            viewModel.selectEntry(null)
        }
        DictionaryEntryDetailScreen(
            entry = selectedEntry!!,
            onBack = { viewModel.selectEntry(null) }
        )
        return
    }

    // Group the filtered entries by their keyLetter to maintain the alphabetical structure
    val groupedEntries = uiState.groupBy { it.keyLetter }
    val dictionaryKeys = groupedEntries.keys.sorted()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val keyToIndex = remember(dictionaryKeys, groupedEntries) {
        var cumulativeIndex = 0
        dictionaryKeys.associateWith { key ->
            val currentIndex = cumulativeIndex
            cumulativeIndex += 1 + (groupedEntries[key]?.size ?: 0)
            currentIndex
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search in Greek or English") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        Row(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                for (key in dictionaryKeys) {
                    item(key = key) {
                        Text(
                            text = "${key.uppercase()} ${key.lowercase()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    val entries = groupedEntries[key] ?: emptyList()
                    items(entries, key = { it.id }) { entry ->
                        DictionaryEntryRow(
                            entry = entry,
                            onClick = { viewModel.selectEntry(entry) }
                        )
                    }
                }
            }

            AlphabetIndex(
                keys = dictionaryKeys,
                onKeySelected = { key ->
                    keyToIndex[key]?.let { index ->
                        // Jumps rather than animates: a scrub follows the finger, so an animation
                        // per letter would queue up and lag behind it.
                        coroutineScope.launch { listState.scrollToItem(index) }
                    }
                }
            )
        }
    }
}

/**
 * The iOS-style section index down the right edge of the list.
 *
 * The whole alphabet is always visible: the letters are spread evenly over the height available
 * and the type shrinks to whatever fits, rather than overflowing into a scroll. A short index — a
 * search filtered down to a few sections — stays at its natural spacing, centred, instead of
 * scattering itself over the full height. Dragging a finger down it scrubs through the sections;
 * a tap jumps to one.
 */
@Composable
private fun AlphabetIndex(
    keys: List<String>,
    onKeySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (keys.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    // Letter the finger is currently over, or -1 when the index isn't being touched.
    var activeIndex by remember { mutableIntStateOf(-1) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(IndexWidth)
            .padding(vertical = 4.dp)
    ) {
        val slotHeight = (maxHeight / keys.size).coerceAtMost(MaxIndexSlotHeight)
        // toSp() keeps the fit honest at large system font scales, where sp and dp diverge.
        val fontSize = with(LocalDensity.current) {
            val fitted = slotHeight.toSp() * 0.72f
            if (fitted < MaxIndexFontSize) fitted else MaxIndexFontSize
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(slotHeight * keys.size)
                .background(
                    color = if (activeIndex >= 0) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(percent = 50)
                )
                .pointerInput(keys) {
                    awaitEachGesture {
                        // Track the pointer for the whole press, so a drag scrubs continuously
                        // instead of only the letter that was first touched responding.
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var pressed = true
                        var position = down.position.y

                        while (pressed) {
                            val index = (position / size.height * keys.size).toInt()
                                .coerceIn(keys.indices)
                            if (index != activeIndex) {
                                activeIndex = index
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onKeySelected(keys[index])
                            }

                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.first()
                            position = change.position.y
                            pressed = change.pressed
                            change.consume()
                        }

                        activeIndex = -1
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keys.forEach { key ->
                Text(
                    text = key.uppercase(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeight)
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    fontSize = fontSize,
                    // Without an explicit line height the letters inherit the body style's, which
                    // is far taller than the type and pushes the tail of the alphabet off screen.
                    lineHeight = fontSize * 1.15f,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DictionaryEntryRow(entry: DictionaryEntry, onClick: () -> Unit = {}) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = entry.word,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = entry.gloss,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
