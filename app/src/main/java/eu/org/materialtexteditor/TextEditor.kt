package eu.org.materialtexteditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditor(
    text: String,
    fileName: String,
    onBackClick: () -> Unit,
    onRename: ((String) -> Unit)? = null,
    onSave: (String) -> Unit,
    onShare: (String) -> Unit
) {
    var textState by remember { mutableStateOf(text) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf(fileName) }

    Column {
        if (showRenameDialog) {
            RenameDialog(
                currentName = currentFileName,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    currentFileName = newName
                    onRename?.invoke(newName)
                    showRenameDialog = false
                }
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
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                label = { Text("File Name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newName) }) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}