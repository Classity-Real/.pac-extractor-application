package dev.classityreal.pext.native

import android.content.Context
import java.io.File

/**
 * Wraps `pacextractor`. Real usage string:
 *   Usage: pacextractor [-d] [-c] firmware.pac [outdir]
 *
 * No `list`/selective-extract support — this always dumps every partition
 * in one pass. Useful as a fallback engine if a given .pac trips up
 * `unpac`, or when the user just wants everything and doesn't care about
 * picking partitions.
 */
class PacExtractorRunner(context: Context) {
    private val tools = NativeTools(context)
    private val binary by lazy { tools.binaryFor(ExtractionEngine.PAC_EXTRACTOR) }

    /** Extracts every partition from [pacPath] into [outDir]. [checkOnly] runs -c (validate, no write). */
    fun extractAll(pacPath: String, outDir: File, checkOnly: Boolean = false, onLine: (String) -> Unit = {}): Int {
        outDir.mkdirs()
        val args = buildList {
            if (checkOnly) add("-c")
            add(pacPath)
            add(outDir.absolutePath)
        }
        val (exitCode, lines) = tools.run(binary, args, onLine)
        if (exitCode != 0) {
            throw NativeToolException("pacextractor exited with code $exitCode", lines.joinToString("\n"))
        }
        return exitCode
    }
}
