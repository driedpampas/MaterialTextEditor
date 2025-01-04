package eu.org.materialtexteditor

import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.org.materialtexteditor.ui.theme.AppTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource

class MainActivity : ComponentActivity() {
    private val fileContent = mutableStateOf("")
    private val showTextEditor = mutableStateOf(false)
    private val fileName = mutableStateOf("")

    private val getContent: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Log.d("MainActivity", "Selected file URI: $it")
            try {
                val cursor = contentResolver.query(it, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor?.moveToFirst()
                val fileName = nameIndex?.let { index -> cursor.getString(index) }
                cursor?.close()

                val inputStream = contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                // Update the state with the file content and file name
                fileContent.value = content
                showTextEditor.value = true
                this.fileName.value = fileName ?: "Untitled"
                Log.d("MainActivity", "File name: $fileName")
                saveRecentFile(fileName ?: "Untitled")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error reading file", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainContent()
        }
    }

    private fun saveRecentFile(filePath: String) {
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

        recentFiles.add(filePath)
        editor.putStringSet("files", recentFiles)
        editor.apply()
    }

    private fun getRecentFiles(): Set<String> {
        val sharedPreferences: SharedPreferences = getSharedPreferences("recent_files", MODE_PRIVATE)
        return sharedPreferences.getStringSet("files", mutableSetOf()) ?: mutableSetOf()
    }

    @Composable
    fun MainContent() {
        val configuration = LocalConfiguration.current
        val isSystemInDarkTheme = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val isDarkTheme by remember { mutableStateOf(isSystemInDarkTheme) }
        val recentFilesState = remember { mutableStateOf(getRecentFiles()) }

        AppTheme {
            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Log.d("MainActivity", "MainContent: File name: $fileName")
                    if (showTextEditor.value) {
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
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { textState = "" }) {
                        Icon(Icons.Default.Info, contentDescription = "Clear")
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
                modifier = Modifier.fillMaxSize().padding(top = 8.dp)
            )
        }
    }


    @Composable
    fun MainMenu(onNewFileClick: () -> Unit, onOpenFileClick: () -> Unit, recentFiles: Set<String>, clearRecentFiles: () -> Unit) {
        var recentFilesState by remember { mutableStateOf(recentFiles) }
        val backgroundPainter = painterResource(id = R.drawable.ic_launcher_foreground)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = repeatingPatternBackground(modifier = Modifier.fillMaxSize()))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Recently Opened Files", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(32.dp))
                    Text(
                        text = "Clear",
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
                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (recentFilesState.isEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No recent files",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(text = "No recent files", modifier = Modifier.padding(8.dp))
                    } else {
                        Column {
                            recentFilesState.forEachIndexed { index, filePath ->
                                Text(
                                    text = filePath,
                                    modifier = Modifier
                                        .clickable { /* Handle file reopening */ }
                                        .padding(8.dp)
                                )
                                if (index < recentFilesState.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onNewFileClick) {
                        Text(text = "Create New File")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onOpenFileClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(text = "Open File")
                    }
                }
            }
        }
    }

    @Composable
    fun repeatingPatternBackground(modifier: Modifier = Modifier): Brush {
        Canvas(modifier = modifier) {
            val imageBitmap = ImageVector.vectorResource(id = R.drawable.ic_launcher_background)
            val imageWidth = imageBitmap.intrinsicSize.width
            val imageHeight = imageBitmap.intrinsicSize.height

            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint()
                val shader = android.graphics.BitmapShader(
                    imageBitmap,
                    android.graphics.Shader.TileMode.REPEAT,
                    android.graphics.Shader.TileMode.REPEAT
                )
                paint.shader = shader

                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
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