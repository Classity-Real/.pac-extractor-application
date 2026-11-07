package dev.classityreal.pext.native

import android.content.Context
import java.io.File

/**
 * Wraps `unpac`. Real usage string (from the binary's own --help output):
 *   Usage: unpac [-d dir] {list|extract|check} firmware.pac [names]
 */
class UnpacRunner(context: Context) {
    private val tools = NativeTools(context)
    private val binary by lazy { tools.binaryFor(ExtractionEngine.UNPAC) }

    private val nameRe = Regex("""name\s*=\s*\"([^\"]*)\"""")
    private val idRe = Regex("""id\s*=\s*\"([^\"]*)\"""")
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
            val n = pendingName?.trim().orEmpty()
            if (n.isNotEmpty()) {
                entries += NativeEntry(n, pendingId, pendingType, pendingSize)
            }
            pendingName = null; pendingId = ""; pendingType = 0; pendingSize = 0L
        }

        for (line in lines) {
            val name = = nameRe.find(line)?.groupValues?.get(1)
            if (name != null) {
                flush()
                pendingName = name
                sawAnyField = true
            }
            idRe.find(line)?.let { pendingId = it.groupValues[1]; sawAnyField = true }
            typeRe.find(line)?.let { pendingType = it.groupValues[1].toIntOrNull() ?: 0; sawAnyField = true }
            sizeRe.find(line)?.let { pendingSize = it.groupValues[1].toLongOrNull(16) ?: 0L; sawAnyField = true }
        }
        flush()

        val unique = entries.distinctBy { it.name }

        if (!sawAnyField || unique.isEmpty()) {
            throw NativeToolException(
                "Couldn't find any partition entries in unpac's output — its list format may differ from what this parser expects.",
                lines.joinToString("\n")
            )
        }
        return unique
    }

    /**
     * Extracts [names] from [pacPath] into [outDir].
     * Empty list = extract all. Blank names are filtered out.
     *
     * unpac uses chdir(-d dir); the directory must exist and be empty.
     * (Unlike pacextractor, which creates the dir itself and fails if it exists.)
     */
    fun extract(pacPath: String, outDir: File, names: List<String> = emptyList(), onLine: (String) -> Unit = {}): Int {
        outDir.deleteRecursively()
        // Must exist before unpac -d or the process fails with "chdir failed"
        if (!outDir.mkdirs() && !outDir.isDirectory) {
            throw NativeToolException("Could not create output directory: ${outDir.absolutePath}")
        }

        val cleanNames = names.map { it.trim() }.filter { it.isNotEmpty() }

        val args = mutableListOf("-d", outDir.absolutePath, "extract", pacPath)
        if (cleanNames.isNotEmpty()) {
            args += cleanNames
        }

        val (exitCode, lines) = tools.run(binary, args, onLine)
        if (exitCode != 0) {
            throw NativeToolException(
                "unpac extract exited with code $exitCode" +
                    if (cleanNames.size == 1) " (selected: ${cleanNames.first()})" else "",
                lines.joinToString("\n")
            )
        }
        return exitCode
    }
}
