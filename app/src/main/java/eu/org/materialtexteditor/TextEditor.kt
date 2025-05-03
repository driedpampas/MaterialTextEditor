package eu.org.materialtexteditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import eu.org.materialtexteditor.ui.theme.displayFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditor(
    text: String,
    fileName: String,
    mimeType: String?,
    onBackClick: () -> Unit,
    onRename: (String) -> Unit,
    onSave: (String) -> Unit,
    onShare: (String) -> Unit,
    loading: MutableState<Boolean>
) {
    var textState by remember { mutableStateOf(text) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf(fileName) }
    var fontSize by remember { mutableStateOf(16.sp) }
    var showTextSizeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loading) {
        loading.value = false
    }

    Column {
        if (showRenameDialog) {
            RenameDialog(
                currentName = currentFileName,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    currentFileName = newName
                    onRename.invoke(newName)
                    showRenameDialog = false
                },
                mimeType = mimeType,
                lineCount = textState.lineSequence().count(),
                maxLineLength = textState.lineSequence().maxOfOrNull { it.length } ?: 0
            )
        }
        if (showTextSizeDialog) {
            TextSizeDialog(
                currentSize = fontSize,
                onSizeChange = { fontSize = it },
                onDismiss = { showTextSizeDialog = false }
            )
        }

        TopAppBar(
            title = {
                Text(
                    text = currentFileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { showRenameDialog = true }
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showTextSizeDialog = true }) {
                    Icon(Icons.Outlined.FormatSize, contentDescription = "Adjust text size")
                }
                IconButton(onClick = { onSave(textState) }) {
                    Icon(Icons.Outlined.Save, contentDescription = "Save")
                }
                IconButton(onClick = { onShare(textState) }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
        )

        TextField(
            value = textState,
            onValueChange = { textState = it },
            textStyle = TextStyle(fontSize = fontSize, fontFamily = displayFontFamily),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    mimeType: String?,
    lineCount: Int? = null,
    maxLineLength: Int? = null
) {
    var newName by remember { mutableStateOf(currentName) }
    Log.d("RenameDialog", "MimeType: $mimeType")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            Column {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("File Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("MIME type: ${mimeType ?: "Unknown"}")
                Spacer(modifier = Modifier.height(4.dp))
                lineCount?.let {
                    Text("Lines: $it")
                }
                maxLineLength?.let {
                    Text("Max line length: $it")
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onConfirm(newName) }) {
                    Text("Rename")
                }
            }
        }
    )
}

@Composable
fun TextSizeDialog(
    currentSize: TextUnit,
    onSizeChange: (TextUnit) -> Unit,
    onDismiss: () -> Unit
) {
    val sliderPosition = remember { mutableFloatStateOf(currentSize.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Text Size") },
        text = {
            Column {
                Slider(
                    value = sliderPosition.floatValue,
                    onValueChange = { newValue ->
                        sliderPosition.floatValue = newValue
                        onSizeChange(newValue.sp)
                    },
                    valueRange = 12f..30f
                )
                Text(buildAnnotatedString {
                    append("Text size: ")
                    withStyle(
                        style = SpanStyle(background = Color.LightGray)
                    ) {
                        append(sliderPosition.floatValue.toInt().toString())
                    }
                    append(" sp")
                })
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        sliderPosition.floatValue = 16f
                        onSizeChange(16.sp)
                    }
                ) {
                    Text("Reset")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFileViewer(
    content: String,
    fileName: String,
    mimeType: String?,
    onBackClick: () -> Unit,
    onRename: (String) -> Unit,
    onShare: (String) -> Unit,
    loading: MutableState<Boolean>
) {
    val lines = remember(content) { content.lineSequence().toList() }
    var showRenameDialog by remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf(fileName) }
    var fontSize by remember { mutableStateOf(16.sp) }
    var showTextSizeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loading) {
        loading.value = false
    }

    Column {
        if (showRenameDialog) {
            RenameDialog(
                currentName = currentFileName,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    currentFileName = newName
                    onRename.invoke(newName)
                    showRenameDialog = false
                },
                mimeType = mimeType,
                lineCount = lines.size,
                maxLineLength = lines.maxOfOrNull { it.length } ?: 0
            )
        }
        if (showTextSizeDialog) {
            TextSizeDialog(
                currentSize = fontSize,
                onSizeChange = { fontSize = it },
                onDismiss = { showTextSizeDialog = false }
            )
        }

        TopAppBar(
            title = {
                Text(
                    text = currentFileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { showRenameDialog = true }
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showTextSizeDialog = true }) {
                    Icon(Icons.Outlined.FormatSize, contentDescription = "Adjust text size")
                }
                IconButton(onClick = { onShare(content) }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(lines.size) { index ->
                Text(
                    lines[index],
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = fontSize,
                    fontFamily = displayFontFamily
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BufferedTextEditor(
    lines: List<String>,
    windowSize: Int = 500,
    fileName: String,
    mimeType: String?,
    onBackClick: () -> Unit,
    onRename: (String) -> Unit,
    onSave: (List<String>) -> Unit,
    onShare: (String) -> Unit,
    loading: MutableState<Boolean>
) {
    var windowStart by remember { mutableStateOf(0) }
    val windowEnd = (windowStart + windowSize).coerceAtMost(lines.size)
    var visibleLines by remember { mutableStateOf(lines.subList(windowStart, windowEnd)) }
    var textState by remember { mutableStateOf(visibleLines.joinToString("\n")) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf(fileName) }
    var fontSize by remember { mutableStateOf(16.sp) }
    var showTextSizeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(windowStart, lines, loading) {
        visibleLines = lines.subList(windowStart, windowEnd)
        textState = visibleLines.joinToString("\n")
        loading.value = false
    }

    Column {
        if (showRenameDialog) {
            RenameDialog(
                currentName = currentFileName,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    currentFileName = newName
                    onRename.invoke(newName)
                    showRenameDialog = false
                },
                mimeType = mimeType,
                lineCount = lines.size,
                maxLineLength = lines.maxOfOrNull { it.length } ?: 0
            )
        }
        if (showTextSizeDialog) {
            TextSizeDialog(
                currentSize = fontSize,
                onSizeChange = { fontSize = it },
                onDismiss = { showTextSizeDialog = false }
            )
        }

        TopAppBar(
            title = {
                Text(
                    text = currentFileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { showRenameDialog = true }
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showTextSizeDialog = true }) {
                    Icon(Icons.Outlined.FormatSize, contentDescription = "Adjust text size")
                }
                IconButton(onClick = {
                    val editedLines = textState.split("\n")
                    val newLines = lines.toMutableList()
                    for (i in editedLines.indices) {
                        if (windowStart + i < newLines.size) {
                            newLines[windowStart + i] = editedLines[i]
                        }
                    }
                    onSave(newLines)
                }) {
                    Icon(Icons.Outlined.Save, contentDescription = "Save")
                }
                IconButton(onClick = { onShare(textState) }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { windowStart = (windowStart - windowSize).coerceAtLeast(0) },
                enabled = windowStart > 0
            ) { Text("Previous") }
            Text("Lines ${windowStart + 1}–$windowEnd / ${lines.size}")
            Button(
                onClick = { windowStart = (windowStart + windowSize).coerceAtMost(lines.size - windowSize) },
                enabled = windowEnd < lines.size
            ) { Text("Next") }
        }
        TextField(
            value = textState,
            onValueChange = { textState = it },
            textStyle = TextStyle(fontSize = fontSize, fontFamily = displayFontFamily),
            modifier = Modifier.fillMaxSize()
        )
    }
}