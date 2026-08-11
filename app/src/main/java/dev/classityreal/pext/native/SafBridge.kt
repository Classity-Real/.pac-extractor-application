package dev.classityreal.pext.native

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * The native CLIs read/write real filesystem paths, not content:// Uris.
 * These helpers stage the SAF-picked input into app cache, and push the
 * resulting output files out into the user's chosen SAF output tree once
 * extraction finishes.
 */
object SafBridge {

    fun copyUriToCache(context: Context, uri: Uri, workDir: File): File {
        workDir.mkdirs()
        val dest = File(workDir, "input.pac")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output, bufferSize = 1 shl 20)
            }
        }
        return dest
    }

    /** Copies every regular file directly under [localDir] into [outputTreeUri]. */
    fun copyDirToTree(context: Context, localDir: File, outputTreeUri: Uri) {
        val outputRoot = DocumentFile.fromTreeUri(context, outputTreeUri)
            ?: throw NativeToolException("Could not open the chosen output folder")
        copyDirToDocumentFile(context, localDir, outputRoot)
    }

    /** Copies every regular file directly under [localDir] into an already-resolved [targetDir]. */
    fun copyDirToDocumentFile(context: Context, localDir: File, targetDir: DocumentFile) {
        val files = localDir.listFiles()?.filter { it.isFile } ?: emptyList()
        for (file in files) {
            val outFile = targetDir.createFile("application/octet-stream", file.name)
                ?: throw NativeToolException("Could not create output file for ${file.name}")
            context.contentResolver.openOutputStream(outFile.uri)!!.use { out ->
                file.inputStream().use { input -> input.copyTo(out, bufferSize = 1 shl 20) }
            }
        }
    }

    /**
     * Finds (or creates) a "PExt" folder directly inside the tree the user just granted
     * access to via ACTION_OPEN_DOCUMENT_TREE. This is the folder every batch-extraction
     * run gets its numbered subfolders created in.
     */
    fun ensurePExtFolder(context: Context, parentTreeUri: Uri): DocumentFile {
        val parent = DocumentFile.fromTreeUri(context, parentTreeUri)
            ?: throw NativeToolException("Could not open the chosen folder")
        val existing = parent.findFile("PExt")
        return if (existing != null && existing.isDirectory) {
            existing
        } else {
            parent.createDirectory("PExt")
                ?: throw NativeToolException("Could not create the PExt folder in the chosen location")
        }
    }

    /** Creates "Folder <n>" (1-based) inside [pextRoot], failing loudly if it can't. */
    fun createNumberedSubfolder(pextRoot: DocumentFile, index1Based: Int): DocumentFile {
        val name = "Folder $index1Based"
        return pextRoot.createDirectory(name)
            ?: throw NativeToolException("Could not create \"$name\" inside PExt")
    }

    fun queryDisplayName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
        }
        return null
    }

    fun querySize(context: Context, uri: Uri): Long? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) return cursor.getLong(idx)
        }
        return null
    }

    /** Free space on internal storage, in bytes — used to warn before a batch extraction. */
    fun internalStorageFreeBytes(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }
}
