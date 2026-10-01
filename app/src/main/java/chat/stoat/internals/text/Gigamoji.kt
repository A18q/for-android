package chat.stoat.internals.text

private const val VARIATION_SELECTOR_16 = 0xFE0F
private const val COMBINING_ENCLOSING_KEYCAP = 0x20E3

private fun isKeycapBase(codepoint: Int): Boolean =
    codepoint == '#'.code || codepoint == '*'.code || codepoint in '0'.code..'9'.code

private fun isCustomEmoteAt(content: String, index: Int): Boolean {
    if (index + 27 >= content.length) return false
    if (content[index] != ':' || content[index + 27] != ':') return false
    for (i in (index + 1)..(index + 26)) {
        val c = content[i]
        if (!((c in '0'..'9') || (c in 'A'..'H') || (c in 'J'..'K') || (c in 'M'..'N') || (c in 'P'..'T') || (c in 'V'..'Z'))) {
            return false
        }
    }
    return true
}

sealed class GigamojiState {
    object None : GigamojiState()
    object Single : GigamojiState()
    object Multiple : GigamojiState()

    val isGigamoji: Boolean get() = this != None
}

object Gigamoji {
    fun useGigamojiForMessage(rawContent: String): GigamojiState {
        val len = rawContent.length
        if (len == 0) return GigamojiState.None

        var index = 0
        var unicodeCount = 0
        var customEmoteCount = 0
        var lastWasZwj = false
        var hasVisibleEmoji = false

        while (index < len) {
            if (rawContent[index] == ':' && isCustomEmoteAt(rawContent, index)) {
                customEmoteCount++
                lastWasZwj = false
                hasVisibleEmoji = true
                index += 28
                continue
            }

            val codepoint = Character.codePointAt(rawContent, index)
            val charCount = Character.charCount(codepoint)

            when {
                codepoint == 0x200C -> Unit // ZERO_WIDTH_NON_JOINER
                Character.isWhitespace(codepoint) -> lastWasZwj = false
                codepoint == 0x200D -> lastWasZwj = true
                isKeycapBase(codepoint) -> {
                    var nextIndex = index + charCount
                    if (nextIndex < len && Character.codePointAt(rawContent, nextIndex) == VARIATION_SELECTOR_16) {
                        nextIndex += Character.charCount(VARIATION_SELECTOR_16)
                    }
                    if (nextIndex >= len || Character.codePointAt(rawContent, nextIndex) != COMBINING_ENCLOSING_KEYCAP) {
                        return GigamojiState.None
                    }
                    if (!lastWasZwj) {
                        unicodeCount++
                        hasVisibleEmoji = true
                    }
                    lastWasZwj = false
                    index = nextIndex + Character.charCount(COMBINING_ENCLOSING_KEYCAP)
                    continue
                }
                codepoint == 0xFE0F || codepoint == 0xFE0E || codepoint == 0x20E3 -> Unit
                codepoint in PUA_MIN..PUA_MAX -> Unit
                codepoint in 0x1F3FB..0x1F3FF -> lastWasZwj = false
                codepoint < 0x80 -> return GigamojiState.None
                MessageProcessor.emoji.codepointIsEmoji(codepoint) -> {
                    if (!lastWasZwj) {
                        unicodeCount++
                        hasVisibleEmoji = true
                    }
                    lastWasZwj = false
                }
                else -> return GigamojiState.None
            }

            index += charCount
        }

        if (!hasVisibleEmoji) return GigamojiState.None

        val total = unicodeCount + customEmoteCount
        return when {
            total == 0 -> GigamojiState.None
            total == 1 -> GigamojiState.Single
            else -> GigamojiState.Multiple
        }
    }
}
