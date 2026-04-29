package toro.sources

import java.io.File
import java.util.zip.ZipFile

object CbzReader {
    fun getPage(cbzFile: File, pageIndex: Int): ByteArray? {
        try {
            val zip = ZipFile(cbzFile)

            val imageEntries = zip.entries().toList()
                .filter { entry ->
                    !entry.isDirectory &&
                            entry.name.matches(Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE))
                }
                .sortedBy { it.name }

            if (pageIndex in imageEntries.indices) {
                val targetEntry = imageEntries[pageIndex]
                val inputStream = zip.getInputStream(targetEntry)

                return inputStream.use { it.readBytes() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun getPageCount(cbzFile: File): Int {
        try {
            val zip = ZipFile(cbzFile)
            return zip.entries().toList().count {
                !it.isDirectory && it.name.matches(Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE))
            }
        } catch (e: Exception) {
            return 0
        }
    }
}