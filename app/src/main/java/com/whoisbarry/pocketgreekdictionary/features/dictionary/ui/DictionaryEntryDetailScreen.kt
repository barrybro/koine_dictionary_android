package com.whoisbarry.pocketgreekdictionary.features.dictionary.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.whoisbarry.pocketgreekdictionary.data.models.DictionaryEntry
import com.whoisbarry.pocketgreekdictionary.singletons.TextToSpeechService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryEntryDetailScreen(entry: DictionaryEntry, onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var rowBounds by remember { mutableStateOf<Rect?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry.word) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        rowBounds = it.boundsInRoot()
                    }
            ) {
                DictionaryEntryDetailRow(entry)
            }

            // TODO: Troubleshoot share as image, doesn't work
//            Button(
//                onClick = {
//                    coroutineScope.launch {
//                        val bitmap = captureView(view, rowBounds)
//                        if (bitmap != null) {
//                            val uri = saveBitmapAndGetUri(context, bitmap)
//                            if (uri != null) {
//                                shareImage(context, uri)
//                            }
//                        }
//                    }
//                },
//                modifier = Modifier
//                    .align(Alignment.CenterHorizontally)
//                    .padding(top = 16.dp)
//            ) {
//                Text("Share as image")
//            }

            Button(
                onClick = {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, entry.shareText())
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            ) {
                Text("Share as text")
            }

            if (TextToSpeechService.canSpeak()) {
                Button(
                    onClick = {
                        TextToSpeechService.speakText(entry.word)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                ) {
                    Text("Speak Entry")
                }
            }
        }
    }
}

private suspend fun captureView(view: View, bounds: Rect?): Bitmap? {
    return withContext(Dispatchers.Main) {
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            if (bounds != null) {
                val left = bounds.left.toInt().coerceAtLeast(0)
                val top = bounds.top.toInt().coerceAtLeast(0)
                val width = bounds.width.toInt().coerceAtMost(bitmap.width - left)
                val height = bounds.height.toInt().coerceAtMost(bitmap.height - top)
                if (width > 0 && height > 0) {
                    Bitmap.createBitmap(bitmap, left, top, width, height)
                } else {
                    bitmap
                }
            } else {
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }
}

private suspend fun saveBitmapAndGetUri(context: android.content.Context, bitmap: Bitmap): Uri? {
    return withContext(Dispatchers.IO) {
        try {
            val imagesFolder = File(context.cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "shared_entry.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }
}

private fun shareImage(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        type = "image/png"
    }
    context.startActivity(Intent.createChooser(intent, "Share Entry"))
}

@Composable
fun DictionaryEntryDetailRow(entry: DictionaryEntry, onClick: () -> Unit = {}) {
    ElevatedCard(
        modifier = Modifier
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
