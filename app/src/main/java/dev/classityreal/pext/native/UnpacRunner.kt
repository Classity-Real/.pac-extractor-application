package dev.classityreal.pext.native

import android.content.Context
import java.io.File

/**
 * Wraps `unpac`. Real usage string (from the binary's own --help output):
 *   Usage: unpac [-d dir] {list|extract|check} firmware.pac [names]
 *
 * `list` prints one summary block per partition; each printed field looks
 * like `name = "boot.img"`, `id = "FDL"`, `type = 0`, `size = 0x1234`. The
 * exact line grouping isn't confirmed against a real device .pac — the
 * parser below scans every stdout line for those four `key = value` tokens
 * independent of how they're grouped, so it degrades gracefully (missing
 * fields just come back blank/zero) rather than throwing if the real
 * layout differs slightly. Verify field values against a known-good .pac
 * before shipping, and adjust the regexes below if something looks off.
 */
class UnpacRunner(context: Context) {
    private val tools = NativeTools(context)
    private val binary by lazy { tools.binaryFor(ExtractionEngine.UNPAC) }

    private val nameRe = Regex("""name\s*=\s*"([^"]*)"""")
    private val idRe = Regex("""id\s*=\s*"([^"]*)"""")
    private val typeRe = Regex("""type\s*=\s*(\d+)""")
    private val sizeRe = Regex("""size\s*=\s*0x([0-9a-fA-F]+)""")

    fun list(pacPath: String): List<NativeEntry> {
        val (exitCode, lines) = tools.run(binary, listOf("list", pacPath))
        if (exitCode != 0) {
            throw NativeToolException("unpac exited with code $exitCode", lines.joinToString("\n"))
        }

        val entries = mutableListOf<NativeEntry>()
        var pendingName: String? = null
        var pendingId = ""
        var pendingType = 0
        var pendingSize = 0L
        var sawAnyField = false

        fun flush() {
            if (pendingName != null) {
                entries += NativeEntry(pendingName!!, pendingId, pendingType, pendingSize)
            }
            pendingName = null; pendingId = ""; pendingType = 0; pendingSize = 0L
        }

        for (line in lines) {
            val name = nameRe.find(line)?.groupValues?.get(1)
            if (name != null) {
                // A new "name = ..." starts a new entry block.
                flush()
                pendingName = name
                sawAnyField = true
            }
            idRe.find(line)?.let { pendingId = it.groupValues[1]; sawAnyField = true }
            typeRe.find(line)?.let { pendingType = it.groupValues[1].toIntOrNull() ?: 0; sawAnyField = true }
            sizeRe.find(line)?.let { pendingSize = it.groupValues[1].toLongOrNull(16) ?: 0L; sawAnyField = true }
        }
        flush()

        if (!sawAnyField) {
            throw NativeToolException(
                "Couldn't find any partition entries in unpac's output — its list format may differ from what this parser expects.",
                lines.joinToString("\n")
            )
        }
        return entries
    }

    /** Extracts [names] (empty = all) from [pacPath] into [outDir]. */
    fun extract(pacPath: String, outDir: File, names: List<String> = emptyList(), onLine: (String) -> Unit = {}): Int {
        // See PacExtractorRunner.extractAll for why we don't outDir.mkdirs() here —
        // same precaution in case unpac's own -d handling behaves the same way.
        outDir.deleteRecursively()
        outDir.parentFile?.mkdirs()
        val args = mutableListOf("-d", outDir.absolutePath, "extract", pacPath)
        args += names
        val (exitCode, lines) = tools.run(binary, args, onLine)
        if (exitCode != 0) {
            throw NativeToolException("unpac extract exited with code $exitCode", lines.joinToString("\n"))
        }
        return exitCode
    }
}
