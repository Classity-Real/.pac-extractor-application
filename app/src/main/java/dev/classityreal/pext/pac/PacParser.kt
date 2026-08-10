package dev.classityreal.pext.pac

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.Charset

/**
 * Minimal random-access byte source. Implemented once for plain JVM files
 * (RandomAccessFile) and once for Android SAF Uris (ParcelFileDescriptor),
 * so PacParser itself has zero Android dependencies and can be unit tested
 * on the desktop JVM against a real .pac file.
 */
interface PacRandomAccess {
    val length: Long
    fun readAt(offset: Long, size: Int): ByteArray
}

/** Wraps any java.nio FileChannel (works for RandomAccessFile.channel or
 *  FileInputStream(pfd.fileDescriptor).channel on Android). */
class ChannelRandomAccess(private val channel: FileChannel) : PacRandomAccess {
    override val length: Long get() = channel.size()

    override fun readAt(offset: Long, size: Int): ByteArray {
        val buf = ByteBuffer.allocate(size)
        var readTotal = 0
        while (readTotal < size) {
            val n = channel.read(buf, offset + readTotal)
            if (n < 0) break
            readTotal += n
        }
        if (readTotal != size) {
            throw PacParseException("Unexpected EOF reading $size bytes at offset $offset (got $readTotal)")
        }
        return buf.array()
    }
}

object PacConstants {
    const val HEADER_SIZE = 2124
    const val ENTRY_SIZE = 2580

    // Field offsets within PAC_HEADER (bytes)
    const val OFF_VERSION = 0
    const val LEN_VERSION = 44
    const val OFF_FILE_SIZE = OFF_VERSION + LEN_VERSION            // 44
    const val OFF_PRODUCT_NAME = OFF_FILE_SIZE + 4                 // 48
    const val LEN_PRODUCT_NAME = 512
    const val OFF_FIRMWARE_NAME = OFF_PRODUCT_NAME + LEN_PRODUCT_NAME // 560
    const val LEN_FIRMWARE_NAME = 512
    const val OFF_FILE_COUNT = OFF_FIRMWARE_NAME + LEN_FIRMWARE_NAME  // 1072
    const val OFF_FILE_OFFSET = OFF_FILE_COUNT + 4                 // 1076
    const val OFF_MODE = OFF_FILE_OFFSET + 4                       // 1080
    const val OFF_FLASH_TYPE = OFF_MODE + 4                        // 1084
    const val OFF_NAND_STRATEGY = OFF_FLASH_TYPE + 4                // 1088
    const val OFF_IS_ENCRYPTED = OFF_NAND_STRATEGY + 4              // 1092
    const val OFF_ENTRY_SIZE = 1096
    const val OFF_CRC16 = HEADER_SIZE - 8
    const val OFF_CRC32 = HEADER_SIZE - 4

    // Field offsets within FILE_T (bytes)
    const val E_OFF_FILE_ID = 0
    const val E_LEN_FILE_ID = 256
    const val E_OFF_FILE_NAME = E_OFF_FILE_ID + E_LEN_FILE_ID       // 256
    const val E_LEN_FILE_NAME = 512
    const val E_OFF_FILE_PATH = E_OFF_FILE_NAME + E_LEN_FILE_NAME   // 768
    const val E_LEN_FILE_PATH = 1024
    const val E_OFF_FILE_SIZE = E_OFF_FILE_PATH + E_LEN_FILE_PATH   // 1792
    const val E_OFF_FILE_FLAG = E_OFF_FILE_SIZE + 8                 // 1800
    const val E_OFF_CHECK_FLAG = E_OFF_FILE_FLAG + 4                // 1804
    const val E_OFF_DATA_OFFSET = E_OFF_CHECK_FLAG + 4              // 1808
    const val E_OFF_OMIT_FLAG = E_OFF_DATA_OFFSET + 8               // 1816
    const val E_OFF_ADDR_NUM = E_OFF_OMIT_FLAG + 4                  // 1820
    const val E_OFF_PARTITION_SIZE = E_OFF_ADDR_NUM + 4             // 1824
}

class PacParser(private val io: PacRandomAccess) {

    private val utf16le: Charset = Charset.forName("UTF-16LE")

    fun parse(): PacFile {
        if (io.length < PacConstants.HEADER_SIZE) {
            throw PacParseException("File too small to be a valid .pac (${io.length} bytes)")
        }
        val header = parseHeader()
        val entries = (0 until header.fileCount).map { i ->
            parseEntry(index = i, baseOffset = header.fileEntryOffset, stride = header.fileEntrySize.toLong())
        }
        return PacFile(header, entries)
    }

    private fun parseHeader(): PacHeader {
        val raw = io.readAt(0, PacConstants.HEADER_SIZE)
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)

        val version = readUtf16(raw, PacConstants.OFF_VERSION, PacConstants.LEN_VERSION)
        val fileSize = buf.getInt(PacConstants.OFF_FILE_SIZE).toUIntSafe()
        val productName = readUtf16(raw, PacConstants.OFF_PRODUCT_NAME, PacConstants.LEN_PRODUCT_NAME)
        val firmwareName = readUtf16(raw, PacConstants.OFF_FIRMWARE_NAME, PacConstants.LEN_FIRMWARE_NAME)
        val fileCount = buf.getInt(PacConstants.OFF_FILE_COUNT)
        val fileEntryOffset = buf.getInt(PacConstants.OFF_FILE_OFFSET).toUIntSafe()
        val entrySize = buf.getInt(PacConstants.OFF_ENTRY_SIZE).let {
            if (it in 1..8192) it else PacConstants.ENTRY_SIZE // fall back to known stride
        }
        val crc16 = buf.getShort(PacConstants.OFF_CRC16).toInt() and 0xFFFF
        val crc32 = buf.getInt(PacConstants.OFF_CRC32).toUIntSafe()

        if (fileCount < 0 || fileCount > 4096) {
            throw PacParseException("Implausible file count in header: $fileCount — this may not be a .pac, or uses an unsupported header revision")
        }

        return PacHeader(
            version = version,
            fileSize = fileSize,
            productName = productName,
            firmwareName = firmwareName,
            fileCount = fileCount,
            fileEntryOffset = fileEntryOffset,
            fileEntrySize = entrySize,
            crc16 = crc16,
            crc32 = crc32
        )
    }

    private fun parseEntry(index: Int, baseOffset: Long, stride: Long): PacEntry {
        val entryOffset = baseOffset + index * stride
        val raw = io.readAt(entryOffset, PacConstants.ENTRY_SIZE)
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)

        return PacEntry(
            index = index,
            fileId = readUtf16(raw, PacConstants.E_OFF_FILE_ID, PacConstants.E_LEN_FILE_ID),
            fileName = readUtf16(raw, PacConstants.E_OFF_FILE_NAME, PacConstants.E_LEN_FILE_NAME),
            filePath = readUtf16(raw, PacConstants.E_OFF_FILE_PATH, PacConstants.E_LEN_FILE_PATH),
            fileSize = buf.getLong(PacConstants.E_OFF_FILE_SIZE),
            fileFlag = buf.getInt(PacConstants.E_OFF_FILE_FLAG),
            checkFlag = buf.getInt(PacConstants.E_OFF_CHECK_FLAG),
            dataOffset = buf.getLong(PacConstants.E_OFF_DATA_OFFSET),
            omitFlag = buf.getInt(PacConstants.E_OFF_OMIT_FLAG),
            addrNum = buf.getInt(PacConstants.E_OFF_ADDR_NUM),
            partitionSize = runCatching { buf.getInt(PacConstants.E_OFF_PARTITION_SIZE).toUIntSafe() }.getOrDefault(0L)
        )
    }

    private fun readUtf16(raw: ByteArray, offset: Int, byteLen: Int): String {
        val slice = raw.copyOfRange(offset, offset + byteLen)
        val str = String(slice, utf16le)
        val nul = str.indexOf('\u0000')
        return (if (nul >= 0) str.substring(0, nul) else str).trim()
    }

    /** Copies one entry's raw partition data to [sink] in chunks, reporting progress via [onProgress]. */
    fun extractEntry(entry: PacEntry, sink: java.io.OutputStream, chunkSize: Int = 1 shl 20, onProgress: (bytesDone: Long, bytesTotal: Long) -> Unit = { _, _ -> }) {
        var remaining = entry.fileSize
        var offset = entry.dataOffset
        var done = 0L
        while (remaining > 0) {
            val toRead = minOf(remaining, chunkSize.toLong()).toInt()
            val chunk = io.readAt(offset, toRead)
            sink.write(chunk)
            offset += toRead
            remaining -= toRead
            done += toRead
            onProgress(done, entry.fileSize)
        }
    }

    private fun Int.toUIntSafe(): Long = this.toLong() and 0xFFFFFFFFL
}
