package eu.org.materialtexteditor

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import eu.org.materialtexteditor.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val fileContent = mutableStateOf("")
    private val showTextEditor = mutableStateOf(false)
    private val fileName = mutableStateOf("")
    private lateinit var fileHandler : FileHandler
    private val fileUri = mutableStateOf<Uri?>(null)
    private val showMissingFileDialog = mutableStateOf(false)
    private val missingFileUri = mutableStateOf<Uri?>(null)
    private val errorMessage = mutableStateOf("File Not Found")
    private var mimeTypeString = mutableStateOf<String?>(null)
    private lateinit var loading : MutableState<Boolean>
    private val isLargeFile = mutableStateOf(false)

    private val openDocument: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            loading.value = true
            val intent = Intent().apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            fileUri.value = it
            Log.d("MainActivity", "intent flags: ${intent.flags}")
            Log.d("MainActivity", "File path: $it")
            CoroutineScope(Dispatchers.Main).launch {
                fileHandler.handleFileUri(
                    contentResolver,
                    it,
                    fileName,
                    fileContent,
                    showTextEditor,
                    loading,
                    isLargeFile,
                    fileUri,
                    intent,
                    mimeTypeString
                )
                loading.value = false
                mimeTypeString.value = contentResolver.getType(it)
            }
        }
    }

    private fun saveText(content: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fileUri.value?.let { uri ->
                    val outputStream = when (uri.scheme) {
                        "file" -> File(uri.path!!).outputStream()
                        "content" -> contentResolver.openOutputStream(uri, "wt")
                        else -> null
                    }
                    outputStream?.use { it.write(content.toByteArray()) }
                    Log.d("MainActivity", "File saved successfully: $uri")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error saving file", e)
            }
        }
    }

    private fun shareText(content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_TITLE, fileName.value)
        }
        startActivity(Intent.createChooser(intent, "Share text"))
    }

    @ExperimentalMaterial3ExpressiveApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileHandler = FileHandler(this)

        handleIntent(intent)

        setContent {
            MainContent()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    @SuppressLint("WrongConstant")
    private fun handleIntent(intent: Intent?) {
        // Handle URLs first
        intent?.let {
            val action = it.action
            val uri = it.data

            when (uri?.host) {
                "fixupx.com" -> {
                    handleRedirect(uri.toString().replace("fixupx.com", "x.com"))
                    return
                }
                "vxtwitter.com" -> {
                    handleRedirect(uri.toString().replace("vxtwitter.com", "twitter.com"))
                    return
                }
                "fxtwitter.com" -> {
                    handleRedirect(uri.toString().replace("fxtwitter.com", "twitter.com"))
                    return
                }
                "fixvx.com" -> {
                    handleRedirect(uri.toString().replace("fixvx.com", "x.com"))
                    return
                }
                "autistic.kids" -> {
                    handleRedirect(uri.toString().replace("autistic.kids", "twitter.com"))
                    return
                }
                "girlcockx.com" -> {
                    handleRedirect(uri.toString().replace("girlcockx.com", "x.com"))
                    return
                }
                "vt.vxtiktok.com" -> {
                    handleRedirect(uri.toString().replace("vt.vxtiktok.com", "vm.tiktok.com"))
                    return
                }
            }

            if (uri != null) {
                when (action) {
                    Intent.ACTION_OPEN_DOCUMENT, Intent.ACTION_VIEW -> {

                        if (!Environment.isExternalStorageManager()) {
                            val intent =
                                Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.setData(uri)
                            startActivity(intent)
                            Toast.makeText(this, "Please grant 'All Files Access' permission to continue.", Toast.LENGTH_LONG).show()
                        }

                        if (it.type?.startsWith("text/") == true ||
                            it.type == "application/text" ||
                            it.type == "application/x-unknown" ||
                            it.type?.startsWith("application/") == true
                        ) {
                            CoroutineScope(Dispatchers.Main).launch {
                                loading = mutableStateOf(true)
                                showTextEditor.value = true
                                fileHandler.handleFileUri(
                                    contentResolver,
                                    uri,
                                    fileName,
                                    fileContent,
                                    showTextEditor,
                                    loading,
                                    isLargeFile,
                                    fileUri,
                                    it,
                                    mimeTypeString
                                )
                                mimeTypeString.value = contentResolver.getType(uri)
                            }
                        }
                    }
                    else -> Log.w("MainActivity", "Unhandled intent action: $action")
                }
            }
        }
    }

    private fun handleRedirect(xUrl: String) {
        try {
            val xIntent = Intent(Intent.ACTION_VIEW, xUrl.toUri())
            xIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(xIntent)
            finish()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error redirecting to X app", e)
        }
    }

    private val createDocument: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            fileUri.value = it
            showTextEditor.value = true
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun MainContent() {
        val configuration = LocalConfiguration.current
        val isSystemInDarkTheme = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val isDarkTheme by remember { mutableStateOf(isSystemInDarkTheme) }
        val dialogText = when (errorMessage.value) {
            "File Not Found" -> "If the file was moved or renamed please re-pick it. Otherwise, you may remove it from the list."
            "Permission Denied" -> "Permission to access this file was denied. Please re-pick the file or remove it from the list."
            "Error reading file" -> "An error occurred while reading the file. Please try re-picking it."
            else -> "Unknown error. Please re-pick or remove the file."
        }
        loading = remember { mutableStateOf(false) }
        val lines = fileContent.value.lineSequence().toList()
        val totalChars = fileContent.value.length
        val maxLineLength = lines.maxOfOrNull { it.length } ?: 0
        val bufferedWindowSize = if (maxLineLength > 10_000) 15 else 500
        val showLongLineDialog = remember { mutableStateOf(false) }
        val showLongLineDialogStage2 = remember { mutableStateOf(false) }
        val showLargeFileDialog = remember { mutableStateOf(false) }
        val showLargeFileDialogStage2 = remember { mutableStateOf(false) }
        var lastOpenedFile by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(fileContent.value, isLargeFile.value) {
            if (isLargeFile.value && fileContent.value.isNotEmpty() && lastOpenedFile != fileName.value) {
                showLargeFileDialog.value = true
                lastOpenedFile = fileName.value
            }
        }

        enableEdgeToEdge()
        AppTheme {
            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    if (loading.value) {
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {},
                            dismissButton = {},
                            title = { Text("Loading...") },
                            text = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(80.dp)
                                    )
                                    CircularWavyProgressIndicator(modifier = Modifier.size(100.dp))
                                }
                            }
                        )
                    }
                    if (showMissingFileDialog.value) {
                        AlertDialog(
                            onDismissRequest = {
                                showMissingFileDialog.value = false
                            },
                            title = { Text(errorMessage.value) },
                            text = { Text(dialogText) },
                            confirmButton = {
                                Row {
                                    TextButton(onClick = {
                                        showMissingFileDialog.value = false
                                        openDocument.launch(arrayOf("text/*", "application/*"))
                                    }) {
                                        Text("Re-pick")
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        showMissingFileDialog.value = false
                                        missingFileUri.value?.let { fileHandler.removeRecentFile(it.toString()) }
                                    }) {
                                        Text("Remove")
                                    }
                                }
                            }
                        )
                    }
                    if (showLongLineDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showLongLineDialog.value = false },
                            title = { Text("Warning: Very Long Lines") },
                            text = {
                                Text("The file selected has lines of length: $maxLineLength. If you open it for editing, performance may be severely impacted.")
                            },
                            confirmButton = {
                                Row {
                                    TextButton(onClick = {
                                        showLongLineDialog.value = false
                                        showLongLineDialogStage2.value = true
                                        showTextEditor.value = true
                                    }) { Text("Edit Anyway") }
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        showLongLineDialog.value = false
                                        isLargeFile.value = true
                                        //showTextEditor.value = true
                                    }) { Text("View Only") }
                                }
                            }
                        )
                    }
                    if (showTextEditor.value) {
                        if (isLargeFile.value) {
                            if (showLargeFileDialog.value) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showLargeFileDialog.value = false
                                        showLargeFileDialogStage2.value = false
                                    },
                                    title = { Text("Large File Preview") },
                                    text = { Text("This file is too large to edit. It will be opened in read-only preview mode.") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showLargeFileDialog.value = false
                                            showLargeFileDialogStage2.value = true
                                            showTextEditor.value = true
                                        }) {
                                            Text("OK")
                                        }
                                    }
                                )
                            } else if (showLargeFileDialogStage2.value) {
                                LargeFileViewer(
                                    content = fileContent.value,
                                    fileName = fileName.value,
                                    mimeType = mimeTypeString.value,
                                    onBackClick = { showTextEditor.value = false },
                                    onRename = { newName -> fileName.value = newName; fileHandler.updateFileName(newName) },
                                    onShare = { content -> shareText(content) },
                                    loading = loading
                                )
                            }
                        } else if (maxLineLength > 10_000 && !showLongLineDialogStage2.value) {
                            showLongLineDialog.value = true
                        } else if (
                            lines.size > 1000 ||
                            totalChars > 100_000
                            ) {
                            BufferedTextEditor(
                                lines = fileContent.value.lineSequence().toList(),
                                windowSize = bufferedWindowSize,
                                fileName = fileName.value,
                                mimeType = mimeTypeString.value,
                                onBackClick = { showTextEditor.value = false },
                                onRename = { newName -> fileName.value = newName; fileHandler.updateFileName(newName) },
                                onSave = { newLines -> saveText(newLines.joinToString("\n")) },
                                onShare = { content -> shareText(content) },
                                loading = loading
                            )
                        } else {
                            Log.d("MainActivity", "MainContent: File name: $fileName")
                            TextEditor(
                                text = fileContent.value,
                                fileName = fileName.value,
                                mimeType = mimeTypeString.value,
                                onBackClick = {
                                    showTextEditor.value = false
                                    fileHandler.recentFilesState.value = fileHandler.getRecentFiles()
                                },
                                onRename = { newName ->
                                    fileName.value = newName
                                    fileHandler.updateFileName(newName)
                                },
                                onSave = { content -> saveText(content) },
                                onShare = { content -> shareText(content) },
                                loading = loading
                            )
                        }
                    } else {
                        MainMenu(
                            onNewFileClick = { createDocument.launch("NewFile.txt") },
                            onOpenFileClick = {
                                loading.value = true
                                openDocument.launch(arrayOf("text/*", "application/*"))
                            },
                            recentFilesState = fileHandler.recentFilesState.value,
                            clearRecentFiles = {
                                clearRecentFiles()
                                fileHandler.recentFilesState.value = emptySet()
                            },
                            onRecentFileClick = { recentFile ->
                                loading.value = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    val uri = recentFile.path.toUri()
                                    fileUri.value = uri
                                    fileHandler.handleFileUri(
                                        contentResolver,
                                        uri,
                                        fileName,
                                        fileContent,
                                        showTextEditor,
                                        loading,
                                        isLargeFile,
                                        fileUri,
                                        Intent(),
                                        mimeTypeString,
                                        onFileMissing = { missingUri, dialogTitle ->
                                            missingFileUri.value = missingUri
                                            errorMessage.value = dialogTitle
                                            showMissingFileDialog.value = true
                                        },
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun clearRecentFiles() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("recent_files", MODE_PRIVATE)
        sharedPreferences.edit {
            clear()
        }
    }
}