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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.edit
import androidx.core.net.toUri
import eu.org.materialtexteditor.ui.theme.AppTheme
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

    private val openDocument: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val intent = Intent().apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            fileUri.value = it
            Log.d("MainActivity", "intent flags: ${intent.flags}")
            Log.d("MainActivity", "File path: $it")
            fileHandler.handleFileUri(
                contentResolver, it, fileName, fileContent, showTextEditor, fileUri, intent, mimeTypeString
            )
            mimeTypeString.value = contentResolver.getType(it)
        }
    }

    private fun saveText(content: String) {
        try {
            fileUri.value?.let { uri ->
                Log.d("MainActivity", "Saving file to URI: $uri")
                val outputStream = when (uri.scheme) {
                    "file" -> {
                        // Saving to local file
                        File(uri.path!!).outputStream()
                    }
                    "content" -> {
                        // Normal SAF file
                        contentResolver.openOutputStream(uri, "wt")
                    }
                    else -> {
                        Log.w("MainActivity", "Unknown URI scheme: ${uri.scheme}")
                        null
                    }
                }

                outputStream?.use { it.write(content.toByteArray()) }

                Log.d("MainActivity", "File saved successfully: $uri")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving file", e)
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

                        /*
                        val takeFlags = it.flags and
                                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                        if (takeFlags != 0) {
                            try {
                                contentResolver.takePersistableUriPermission(uri, takeFlags)
                                Log.d("MainActivity",
                                    "Persistable URI permission granted for $uri")
                            } catch (e: SecurityException) {
                                Log.w("MainActivity",
                                    "Failed to take persistable permission for $uri", e)
                            }
                        }
                        */

                        if (it.type?.startsWith("text/") == true ||
                            it.type == "application/text" ||
                            it.type == "application/x-unknown" ||
                            it.type?.startsWith("application/") == true
                        ) {
                            showTextEditor.value = true
                            fileHandler.handleFileUri(
                                contentResolver,
                                uri,
                                fileName,
                                fileContent,
                                showTextEditor,
                                fileUri,
                                it,
                                mimeTypeString
                            )
                            mimeTypeString.value = contentResolver.getType(uri)
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
    @Composable
    fun MainContent() {
        val configuration = LocalConfiguration.current
        //val recentFilesState = remember { mutableStateOf(fileHandler.getRecentFiles()) }
        val isSystemInDarkTheme = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val isDarkTheme by remember { mutableStateOf(isSystemInDarkTheme) }

        enableEdgeToEdge()
        AppTheme {
            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    if (showMissingFileDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showMissingFileDialog.value = false },
                            title = { Text(errorMessage.value) },
                            text = { Text("This file is no longer available. If you don't re-pick it, it will be removed from the recent files list.") },
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
                    if (showTextEditor.value) {
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
                            onShare = { content -> shareText(content) }
                        )
                    } else {
                        MainMenu(
                            onNewFileClick = { createDocument.launch("NewFile.txt") },
                            onOpenFileClick = { openDocument.launch(arrayOf("text/*", "application/*")) },
                            recentFilesState = fileHandler.recentFilesState.value,
                            clearRecentFiles = {
                                clearRecentFiles()
                                fileHandler.recentFilesState.value = emptySet()
                            },
                            onRecentFileClick = { recentFile ->
                                val uri = recentFile.path.toUri()
                                fileUri.value = uri
                                fileHandler.handleFileUri(
                                    contentResolver,
                                    uri,
                                    fileName,
                                    fileContent,
                                    showTextEditor,
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