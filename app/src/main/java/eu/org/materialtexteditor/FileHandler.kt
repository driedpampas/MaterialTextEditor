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
import java.io.File

class FileHandler(private val context: Context) {

    companion object {
        private const val TAG = "FileHandler"
        private const val RECENT_FILES_PREF_NAME = "recent_files"
        private const val RECENT_FILES_KEY = "files"
        private const val MAX_RECENT_FILES = 5
    }

    private val filePath = mutableStateOf("")

    data class RecentFile(val name: String, val path: String)

    @SuppressLint("WrongConstant")
    fun handleFileUri(
        contentResolver: ContentResolver,
        uri: Uri,
        fileName: MutableState<String>,
        fileContent: MutableState<String>,
        showTextEditor: MutableState<Boolean>,
        fileUri: MutableState<Uri?>,
        intent: Intent
    ) {
        try {
            val flags = intent.flags
            val takeFlags = flags and (FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)
            val persistable = flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

            if (takeFlags != 0 && persistable != 0) {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            }

            // Retrieve the display name with a fallback to the file name from the URI's path.
            var displayName = File(uri.path ?: "Untitled.txt").name
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            }

            val content = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""

            fileUri.value = uri
            fileName.value = displayName
            fileContent.value = content
            showTextEditor.value = true

            saveRecentFile(RecentFile(displayName, uri.toString()))
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Unable to take persistable URI permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file", e)
        }
    }

    private fun saveRecentFile(recentFile: RecentFile) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(RECENT_FILES_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            val recentFiles = getRecentFiles().toMutableSet()

            // Remove the oldest file if we have too many
            if (recentFiles.size >= MAX_RECENT_FILES) {
                recentFiles.remove(recentFiles.first())
            }

            recentFiles.add(recentFile)
            putStringSet(RECENT_FILES_KEY, recentFiles.map { "${it.name}|${it.path}" }.toSet())
        }
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
        }
    }
}