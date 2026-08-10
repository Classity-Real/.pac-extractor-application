package dev.classityreal.pext.native

import android.content.Context
import java.io.File
import java.io.IOException

/** One partition/image entry as reported by `unpac list`. */
data class NativeEntry(
    val name: String,
    val id: String,
    val type: Int,
    val size: Long
)

class NativeToolException(message: String, val stdout: String = "") : IOException(message)

enum class ExtractionEngine(val libName: String) {
    /** Supports listing and extracting a chosen subset of partitions by name. */
    UNPAC("libunpac.so"),
    /** Simpler CLI, always extracts everything in one pass — no partition selection. */
    PAC_EXTRACTOR("libpacextractor.so")
}

/**
 * Locates the extracted native binaries and runs them as subprocesses.
 *
 * Both libunpac.so and libpacextractor.so are real ELF executables (they
 * have a PT_INTERP pointing at /system/bin/linker64) despite the .so
 * extension — that naming is intentional so Gradle/the installer places
 * them under jniLibs and extracts them into nativeLibraryDir with the exec
 * bit set, which is the standard sanctioned way to ship a helper binary.
 * They are NOT JNI libraries and must never be passed to
 * System.loadLibrary()/dlopen() — only run via ProcessBuilder, as here.
 */
class NativeTools(private val context: Context) {

    fun binaryFor(engine: ExtractionEngine): File {
        val path = File(context.applicationInfo.nativeLibraryDir, engine.libName)
        if (!path.exists()) {
            throw NativeToolException("${engine.libName} was not found in nativeLibraryDir — check it's under app/src/main/jniLibs/arm64-v8a and that useLegacyPackaging is set for jniLibs, and that the device is arm64-v8a.")
        }
        return path
    }

    /** Runs a binary with [args], returning (exitCode, combined stdout+stderr lines). */
    fun run(binary: File, args: List<String>, onLine: (String) -> Unit = {}): Pair<Int, List<String>> {
        val process = ProcessBuilder(listOf(binary.absolutePath) + args)
            .redirectErrorStream(true)
            .directory(context.cacheDir)
            .start()

        val lines = mutableListOf<String>()
        process.inputStream.bufferedReader().forEachLine { line ->
            lines += line
            onLine(line)
        }
        val exitCode = process.waitFor()
        return exitCode to lines
    }
}
