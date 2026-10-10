package com.whoisbarry.pocketgreekdictionary.features.dictionary.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.whoisbarry.pocketgreekdictionary.data.models.DictionaryEntry
import com.whoisbarry.pocketgreekdictionary.singletons.TextToSpeechService
import com.whoisbarry.pocketgreekdictionary.ui.theme.LocalGreekFontFamily
import com.whoisbarry.pocketgreekdictionary.util.renderEntryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Past this the entry reads as a long thin strip, so on a wide screen it is centred instead. */
private val MaxContentWidth = 640.dp

/**
 * An entry's detail view. [onBack] is null where there is nothing to go back to — in the
 * two-pane layout, where the list stays beside the entry — and hides the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryEntryDetailScreen(
    entry: DictionaryEntry,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var isSharingImage by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(entry.word, fontFamily = LocalGreekFontFamily.current) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DictionaryEntryDetailRow(
                entry = entry,
                modifier = Modifier.widthIn(max = MaxContentWidth)
            )

            // Speak above Share, as on iOS. Both span the card's width and line up with its edges
            // (the card insets itself by 16dp), so they read as one column rather than two pills
            // of different sizes.
            val actionButtonModifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)

            if (TextToSpeechService.canSpeak()) {
                Button(
                    onClick = {
                        TextToSpeechService.speakText(entry.word)
                    },
                    modifier = actionButtonModifier.padding(top = 16.dp)
                ) {
                    ButtonIcon(Icons.AutoMirrored.Filled.VolumeUp)
                    Text("Speak Entry")
                }
            }

            EntryShareMenu(
                isSharingImage = isSharingImage,
                onShareText = { shareText(context, entry.shareText()) },
                onShareImage = {
                    isSharingImage = true
                    coroutineScope.launch {
                        val uri = withContext(Dispatchers.IO) {
                            saveBitmapAndGetUri(context, renderEntryImage(context, entry), entry.id)
                        }
                        isSharingImage = false
                        if (uri != null) {
                            shareImage(context, uri, entry.shareText())
                        } else {
                            Toast.makeText(
                                context,
                                "Couldn't create the image to share",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = actionButtonModifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * One Share button that asks what to share — the entry as text, or as the rendered card image —
 * instead of a button for each, matching the iOS app. Either choice goes on to the system share
 * sheet. Share Image is disabled while a previous image is still being rendered.
 */
@Composable
private fun EntryShareMenu(
    isSharingImage: Boolean,
    onShareText: () -> Unit,
    onShareImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // The Box anchors the menu to the button, so it drops down from it rather than the screen edge.
    Box(modifier = modifier) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            ButtonIcon(Icons.Filled.Share)
            Text("Share")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Share Text") },
                leadingIcon = { Icon(Icons.Outlined.FormatQuote, contentDescription = null) },
                onClick = {
                    expanded = false
                    onShareText()
                }
            )
            DropdownMenuItem(
                text = { Text("Share Image") },
                leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                enabled = !isSharingImage,
                onClick = {
                    expanded = false
                    onShareImage()
                }
            )
        }
    }
}

/** A button's leading icon, sized and spaced as Material 3 specifies. */
@Composable
private fun ButtonIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize)
    )
    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Entry"))
}

private fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap, entryId: Int): Uri? {
    return try {
        val imagesFolder = File(context.cacheDir, "images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "entry_$entryId.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        null
    } finally {
        bitmap.recycle()
    }
}

private fun shareImage(context: Context, uri: Uri, caption: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        // Some targets read the URI from the clip data rather than the extra.
        clipData = ClipData.newUri(context.contentResolver, caption, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Entry"))
}

@Composable
fun DictionaryEntryDetailRow(
    entry: DictionaryEntry,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = entry.word,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = LocalGreekFontFamily.current,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = entry.gloss,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = entry.sourceName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            )
        }
    }
}
