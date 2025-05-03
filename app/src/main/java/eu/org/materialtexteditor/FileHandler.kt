package eu.org.materialtexteditor

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileHandler(private val context: Context) {

    companion object {
        private const val TAG = "FileHandler"
        private const val RECENT_FILES_PREF_NAME = "recent_files"
        private const val RECENT_FILES_KEY = "files"
        private const val MAX_RECENT_FILES = 5
        private const val LARGE_FILE_THRESHOLD = 1 * 1024 * 1024
    }

    val recentFilesState = mutableStateOf(getRecentFiles())
    private val filePath = mutableStateOf("")

    data class RecentFile(val name: String, val path: String)

    @SuppressLint("WrongConstant")
    suspend fun handleFileUri(
        contentResolver: ContentResolver,
        uri: Uri,
        fileName: MutableState<String>,
        fileContent: MutableState<String>,
        showTextEditor: MutableState<Boolean>,
        loading: MutableState<Boolean>,
        isLargeFile: MutableState<Boolean>,
        fileUri: MutableState<Uri?>,
        intent: Intent,
        mimeTypeString: MutableState<String?>,
        onFileMissing: ((Uri, String) -> Unit)? = null,
    ) {
        withContext(Dispatchers.IO) {
            loading.value = true
            try {
                Log.d(TAG, "Opening URI: $uri || MimeType: ${contentResolver.getType(uri)}")
                mimeTypeString.value = contentResolver.getType(uri)
                val flags = intent.flags
                Log.d(TAG, "Intent flags: $flags")
                val takeFlags =
                    flags and (FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)
                Log.d(TAG, "Take flags: $takeFlags")
                val persistable = flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                Log.d(TAG, "Persistable: $persistable")

                if (takeFlags != 0 && persistable != 0) {
                    try {
                        contentResolver.takePersistableUriPermission(uri, takeFlags)
                        Log.d(TAG, "Persistable URI permission taken: $uri")
                    } catch (e: SecurityException) {
                        Log.e(
                            TAG,
                            "SecurityException in contentResolver: Unable to take persistable URI permission",
                            e
                        )
                    }
                } else {
                    Log.d(
                        TAG,
                        "Not taking persistable URI permission (not needed or not available)"
                    )
                }

                var displayName = File(uri.path ?: "Untitled.txt").name
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }

                var fileSize: Long = -1
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }

                isLargeFile.value = fileSize > LARGE_FILE_THRESHOLD

                val content = if (isLargeFile.value) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        val previewLines = StringBuilder()
                        var count = 0
                        var line: String?
                        while (reader.readLine().also { line = it } != null && count < 1000) {
                            previewLines.appendLine(line)
                            count++
                        }
                        previewLines.toString()
                    } ?: ""
                } else {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                }

                Log.d(TAG, "File content: $content")

                fileUri.value = uri
                fileName.value = displayName
                fileContent.value = content
                showTextEditor.value = true

                saveRecentFile(RecentFile(displayName, uri.toString()))
            } catch (e: java.io.FileNotFoundException) {
                loading.value = false
                Log.e(TAG, "File not found: $uri", e)
                onFileMissing?.invoke(uri, "File Not Found")
            } catch (e: SecurityException) {
                loading.value = false
                Log.e(TAG, "SecurityException: Unable to take persistable URI permission", e)
                onFileMissing?.invoke(uri, "Permission Denied")
            } catch (e: Exception) {
                loading.value = false
                Log.e(TAG, "Error reading file", e)
                onFileMissing?.invoke(uri, "Error reading file")
            }
        }
    }

    internal fun removeRecentFile(uriStr: String) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(RECENT_FILES_PREF_NAME, Context.MODE_PRIVATE)
        val recentFilesStrings = sharedPreferences.getStringSet(RECENT_FILES_KEY, emptySet()) ?: emptySet()
        val filtered = recentFilesStrings.filterNot { it.endsWith("|$uriStr") }.toSet()
        sharedPreferences.edit {
            putStringSet(RECENT_FILES_KEY, filtered)
        }
        Log.d(TAG, "Removed missing file from recent files: $uriStr")
        recentFilesState.value = getRecentFiles()
    }

    private fun saveRecentFile(recentFile: RecentFile) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(RECENT_FILES_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            val recentFiles = getRecentFiles().toMutableSet()

            // Remove the oldest file if too many.
            if (recentFiles.size >= MAX_RECENT_FILES) {
                recentFiles.remove(recentFiles.first())
            }

            recentFiles.add(recentFile)
            putStringSet(RECENT_FILES_KEY, recentFiles.map { "${it.name}|${it.path}" }.toSet())
        }
        recentFilesState.value = getRecentFiles()
    }

    fun getRecentFiles(): Set<RecentFile> {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(RECENT_FILES_PREF_NAME, Context.MODE_PRIVATE)
        val recentFilesStrings = sharedPreferences.getStringSet(RECENT_FILES_KEY, emptySet()) ?: emptySet()

        return recentFilesStrings.mapNotNull { fileString ->
            val parts = fileString.split("|")
            if (parts.size == 2) {
                RecentFile(parts[0], parts[1])
            } else {
                Log.w(TAG, "Invalid recent file format: $fileString")
                null
            }
        }.toSet()
    }

    fun updateFileName(newName: String) {
        val currentFiles = getRecentFiles().toMutableSet()
        val currentFilePath = filePath.value

        currentFiles.find { it.path == currentFilePath }?.let { oldFile ->
            currentFiles.remove(oldFile)
            currentFiles.add(RecentFile(newName, currentFilePath))

            context.getSharedPreferences(RECENT_FILES_PREF_NAME, Context.MODE_PRIVATE).edit {
                putStringSet(RECENT_FILES_KEY, currentFiles.map { "${it.name}|${it.path}" }.toSet())
            }
            recentFilesState.value = getRecentFiles()
        }
    }
}