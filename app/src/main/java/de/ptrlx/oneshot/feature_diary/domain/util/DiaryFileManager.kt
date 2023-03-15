package de.ptrlx.oneshot.feature_diary.domain.util

import android.content.Context
import android.net.Uri
import android.os.FileUtils
import androidx.documentfile.provider.DocumentFile
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream


class DiaryFileManager(val baseLocation: Uri, val context: Context) {
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
     * @param filename filename of new dummy
     * @return the URI of the dummy file or null if file could not be created
     */
    fun createNewImageDummy(filename: String): Uri? {
        val file: DocumentFile? = baseFolder?.createFile("image/jpg", filename)
        return file?.uri
    }

    /**
     * Copy an image to baseFolder.
     *
     * @param srcUri Source Uri of image
     * @param filename filename of new image
     */
    fun copyImage(srcUri: Uri, filename: String): Uri? {
        val file: DocumentFile? = baseFolder?.createFile("image/jpg", filename)
        return file?.let { df ->
            val inStream = context.contentResolver.openInputStream(srcUri)
            inStream?.let {
                val uri = context.contentResolver.openOutputStream(df.uri)?.let { outStream ->
                    FileUtils.copy(
                        inStream,
                        outStream
                    )

                    outStream.flush()
                    outStream.close()

                    df.uri
                } ?: run {
                    deleteImage(filename)
                    null
                }

                inStream.close()
                uri
            }
        }
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
     * @param entries list of diary entries to export
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
