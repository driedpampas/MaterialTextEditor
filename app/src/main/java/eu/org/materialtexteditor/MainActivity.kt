package eu.org.materialtexteditor

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.org.materialtexteditor.ui.theme.AppTheme
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private val fileContent = mutableStateOf("")
    private val showTextEditor = mutableStateOf(false)
    private val fileName = mutableStateOf("")
    private val filePath = mutableStateOf("")

    private val getContent: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            handleFileUri(it)
        }
    }

    private val requestManageAllFilesPermission: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        // Handle the result if needed
    }

    private fun requestManageAllFilesPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        requestManageAllFilesPermission.launch(intent)
    }

    private fun handleFileUri(uri: Uri) {
        Log.d("MainActivity", "Selected file URI: $uri")
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor?.moveToFirst()
            val displayName = nameIndex?.let { index -> cursor.getString(index) }
            cursor?.close()

            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            reader.close()

            filePath.value = uri.toString()
            Log.d("MainActivity", "File path: ${filePath.value}")
            fileContent.value = content
            showTextEditor.value = true
            fileName.value = displayName ?: "Untitled"
            Log.d("MainActivity", "File name: ${fileName.value}")
            saveRecentFile(fileName.value, uri.toString())
        } catch (e: Exception) {
            Log.e("MainActivity", "Error reading file", e)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Environment.isExternalStorageManager()) {
            requestManageAllFilesPermission()
        }
        setContent {
            MainContent()
        }
    }

    data class RecentFile(val name: String, val path: String)
    
    private fun saveRecentFile(fileName: String, filePath: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("recent_files", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val recentFiles = sharedPreferences.getStringSet("files", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        if (recentFiles.size >= 5) {
            val iterator = recentFiles.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }

        recentFiles.add("$fileName|$filePath")
        editor.putStringSet("files", recentFiles)
        editor.apply()
    }

    private fun getRecentFiles(): Set<RecentFile> {
        val sharedPreferences: SharedPreferences = getSharedPreferences("recent_files", MODE_PRIVATE)
        val recentFiles = sharedPreferences.getStringSet("files", mutableSetOf()) ?: mutableSetOf()
        return recentFiles.mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 2) {
                RecentFile(parts[0], parts[1])
            } else {
                null
            }
        }.toSet()
    }

    @Composable
    fun MainContent() {
        val configuration = LocalConfiguration.current
        val recentFilesState = remember { mutableStateOf(getRecentFiles()) }
        val isSystemInDarkTheme = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val isDarkTheme by remember { mutableStateOf(isSystemInDarkTheme) }

        AppTheme {
            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (showTextEditor.value) {
                        Log.d("MainActivity", "MainContent: File name: $fileName")
                        TextEditor(
                            modifier = Modifier.padding(innerPadding),
                            text = fileContent.value,
                            fileName = fileName.value,
                            onBackClick = { showTextEditor.value = false }
                        )
                    } else {
                        MainMenu(
                            onNewFileClick = { showTextEditor.value = true },
                            onOpenFileClick = { getContent.launch("text/plain") },
                            recentFiles = recentFilesState.value,
                            clearRecentFiles = {
                                clearRecentFiles()
                                recentFilesState.value = emptySet()
                            }
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TextEditor(modifier: Modifier = Modifier, text: String, fileName: String, onBackClick: () -> Unit) {
        var textState by remember { mutableStateOf(text) }
        Column(modifier = modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { /* Handle file renaming */ }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { textState = "" }) {
                        Icon(Icons.Outlined.Save, contentDescription = "Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
            TextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            )
        }
    }

    @Composable
    fun MainMenu(onNewFileClick: () -> Unit, onOpenFileClick: () -> Unit, recentFiles: Set<RecentFile>, clearRecentFiles: () -> Unit) {
        var recentFilesState by remember { mutableStateOf(recentFiles) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Recently Opened Files", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(32.dp))
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable {
                            clearRecentFiles()
                            recentFilesState = emptySet()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    if (recentFilesState.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "No recent files",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "No recent files", modifier = Modifier.padding(8.dp))
                        }
                    } else {
                        Column {
                            recentFilesState.forEachIndexed { index, recentFile ->
                                Column {
                                    Text(
                                        text = recentFile.name,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .clickable {
                                                handleFileUri(Uri.parse(recentFile.path))
                                            }
                                    )
                                    if (index < recentFilesState.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onNewFileClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(text = "Create New File")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onOpenFileClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(text = "Open File")
                    }
                }
            }
        }
    }

    private fun clearRecentFiles() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("recent_files", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}