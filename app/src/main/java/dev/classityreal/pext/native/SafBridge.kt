package dev.classityreal.pext.native

import android.content.Context
import android.net.Uri
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

        val files = localDir.listFiles()?.filter { it.isFile } ?: emptyList()
        for (file in files) {
            val outFile = outputRoot.createFile("application/octet-stream", file.name)
                ?: throw NativeToolException("Could not create output file for ${file.name}")
            context.contentResolver.openOutputStream(outFile.uri)!!.use { out ->
                file.inputStream().use { input -> input.copyTo(out, bufferSize = 1 shl 20) }
            }
        }
    }
}
