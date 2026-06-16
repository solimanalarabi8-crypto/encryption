package com.example

object StegoText {
    // Zero-width characters for binary encoding
    private const val CHAR_0 = '\u200C' // ZWNJ
    private const val CHAR_1 = '\u200D' // ZWJ
    private const val MARKER = '\uFEFF' // ZWNBS (BOM)

    fun embed(visible: String, hiddenBase64: String): String {
        val binary = hiddenBase64.toByteArray(Charsets.UTF_8).joinToString("") { byte ->
            byte.toUByte().toString(2).padStart(8, '0')
        }
        val invisible = binary.map { if (it == '0') CHAR_0 else CHAR_1 }.joinToString("")
        return visible + MARKER + invisible
    }

    fun extractVisible(combined: String): String {
        val startIndex = combined.indexOf(MARKER)
        if (startIndex == -1) return combined
        return combined.substring(0, startIndex)
    }

    fun extractHiddenBase64(combined: String): String? {
        val startIndex = combined.indexOf(MARKER)
        if (startIndex == -1) return null
        val invisible = combined.substring(startIndex + 1).filter { it == CHAR_0 || it == CHAR_1 }
        
        var binary = ""
        for (char in invisible) {
            when (char) {
                CHAR_0 -> binary += "0"
                CHAR_1 -> binary += "1"
            }
        }
        if (binary.length % 8 != 0 || binary.isEmpty()) return null
        
        val bytes = binary.chunked(8).map { it.toInt(2).toByte() }.toByteArray()
        return String(bytes, Charsets.UTF_8)
    }
}
