package de.ptrlx.oneshot.feature_diary.domain.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.internal.readJson
import okio.use

class DiaryFileManager(baseLocation: Uri, context: Context) {
    val baseLocation = baseLocation
    val context = context
    private val baseFolder: DocumentFile? = DocumentFile.fromTreeUri(context, baseLocation)
    private var diaryEntryFileRepository = HashMap<String, Uri?>()
    private val baseLocationDocumentPath = baseLocation.encodedPath.toString().substring(6)


    /**
     * Search for entry's image file in baseFolder.
     *
     * @param filename filename of an image in baseFolder
     * @return The uri corresponding to baseFolder
     */
    fun resolveUri(filename: String): Uri? {
        return if (!diaryEntryFileRepository.containsKey(filename)) {
            val uri = Uri.withAppendedPath(
                baseLocation,
                "document/${baseLocationDocumentPath}%2F$filename"  // unfortunately, this was the only solution I found to get fast uri building
            )
            diaryEntryFileRepository[filename] = uri
            uri
        } else {
            diaryEntryFileRepository[filename]
        }

    }

//* this may be more stable, but is too slow
////    fun resolve(filename: String): Flow<Uri> {
////        return flow {
////            if (!diaryEntryFileRepository.containsKey(filename)) {
////                val uri = baseFolder?.findFile(filename)?.uri
////                diaryEntryFileRepository[filename] = uri
////                uri?.let { emit(it) }
////            } else {
////                diaryEntryFileRepository[filename]?.let { emit(it) }
////            }
////        }
////    }

    /**
     * Create a new dummy file before launching camara activity.
     *
     * @param filename
     * @return the URI of the dummy file or null if file could not be created
     */
    fun createNewImageDummy(filename: String): Uri? {
        val file: DocumentFile? = baseFolder?.createFile("image/jpg", filename)
        return file?.uri
    }

    /**
     * Delete an image from baseFolder.
     *
     * @param filename Filename of image in baseFolder
     */
    fun deleteImage(filename: String) {
        baseFolder?.findFile(filename)?.delete()
    }

    /**
     * Write a JSON export to base location
     *
     * @param filename
     * @param export JSON String of export
     * @return success of write
     */
    fun writeJSONExport(filename: String, entries: List<DiaryEntry>): Boolean {
        val file: DocumentFile? = baseFolder?.createFile("application/json", filename)
        file?.uri?.let { uri ->
            context.contentResolver.openOutputStream(uri)?.writer()?.let { writer ->
                writer.write(
                    Json.encodeToString(entries)
                )
                writer.flush()
                writer.close()
                return true
            }
        }
        return false
    }

    /**
     * Read a JSON export from given uri
     *
     * @param uri uri of JSON file
     * @return list of diary entries
     */
    fun readJSONExport(uri: Uri): List<DiaryEntry> {
        var entries = emptyList<DiaryEntry>()
        context.contentResolver.openInputStream(uri)?.let { stream ->
            entries = Json.decodeFromStream(stream)
        }
        return entries
    }
}
