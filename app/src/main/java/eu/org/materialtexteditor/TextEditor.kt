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
import eu.org.materialtexteditor.ui.theme.displayFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditor(
    text: String,
    fileName: String,
    mimeType: String?,
    onBackClick: () -> Unit,
    onRename: ((String) -> Unit)? = null,
    onSave: (String) -> Unit,
    onShare: (String) -> Unit
) {
    var textState by remember { mutableStateOf(text) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf(fileName) }
    var fontSize by remember { mutableStateOf(16.sp) }
    var showTextSizeDialog by remember { mutableStateOf(false) }

    Column {
        if (showRenameDialog) {
            RenameDialog(
                currentName = currentFileName,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    currentFileName = newName
                    onRename?.invoke(newName)
                    showRenameDialog = false
                },
                mimeType = mimeType,
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
    mimeType: String?
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