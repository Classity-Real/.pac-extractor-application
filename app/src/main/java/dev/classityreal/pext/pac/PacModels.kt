package dev.classityreal.pext.pac

/**
 * Parsed representation of a Unisoc/Spreadtrum .pac firmware header.
 *
 * Layout is based on the well-known public PAC_HEADER / FILE_T structs used
 * by existing open-source unpackers (all little-endian, fixed-size, UTF-16LE
 * strings). Field names loosely follow the original C struct naming so this
 * is easy to cross-reference against reference tools.
 *
 * PAC_HEADER is a fixed 2124-byte block at offset 0 of the file.
 */
data class PacHeader(
    val version: String,          // szVersion, UTF-16LE, 44 bytes
    val fileSize: Long,           // dwFileSize
    val productName: String,      // szPrdName, UTF-16LE, 512 bytes
    val firmwareName: String,     // szPrdVersion, UTF-16LE, 512 bytes
    val fileCount: Int,           // dwFileCount - number of FILE_T entries
    val fileEntryOffset: Long,    // dwFileOffset - byte offset of the first FILE_T
    val fileEntrySize: Int,       // dwFileEntrySize - stride between entries (2580)
    val crc16: Int,               // wCRC1 - covers header excluding CRC fields
    val crc32: Long               // wCRC2 - covers whole file (field name kept
                                   // generic; some header revisions use CRC16 here too)
)

/**
 * A single embedded partition/image entry (FILE_T), 2580 bytes on disk.
 * One of these exists per partition packed into the .pac (boot, super,
 * vendor_boot, modem images, etc).
 */
data class PacEntry(
    val index: Int,
    val fileId: String,       // szFileID, UTF-16LE, 256 bytes - internal id, e.g. "FILE_ID"
    val fileName: String,     // szFileName, UTF-16LE, 512 bytes - e.g. "boot.img"
    val filePath: String,     // szFilePath, UTF-16LE, 1024 bytes - original build path, informational
    val fileSize: Long,       // dwFileSize - 64-bit size split hi/lo in the real struct
    val fileFlag: Int,        // dwFileFlag - flash flag (e.g. skip if 0-length "flag" placeholder)
    val checkFlag: Int,       // dwCheckFlag
    val dataOffset: Long,     // dwDataOffset - absolute byte offset of this partition's raw data
    val omitFlag: Int,        // dwOmitFlag
    val addrNum: Int,
    val partitionSize: Long   // dwPartitionSize - target partition size on device (informational)
) {
    /** Heuristic: some tool revisions emit zero-length placeholder ("*.flag") entries. */
    val isPlaceholder: Boolean get() = fileSize == 0L || fileName.endsWith(".flag", ignoreCase = true)
}

data class PacFile(
    val header: PacHeader,
    val entries: List<PacEntry>
) {
    val realEntries: List<PacEntry> get() = entries.filterNot { it.isPlaceholder }
}

class PacParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
