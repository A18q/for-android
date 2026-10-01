package chat.stoat.markdown

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.gfm.lexer._GFMLexer
import org.intellij.markdown.lexer.GeneratedLexer

/** Splits `||` out of text so the sequential parser can pair spoiler delimiters. */
class SpoilerLexer(
    private val delegate: GeneratedLexer = _GFMLexer(),
) : GeneratedLexer {
    private data class Token(
        val type: IElementType,
        val start: Int,
        val end: Int,
    )

    private var content: CharSequence = ""
    private var contentEnd: Int = 0
    private var consumedUntil: Int = 0
    private val pending = ArrayDeque<Token>()

    override var tokenStart: Int = 0
        private set

    override var tokenEnd: Int = 0
        private set

    override val state: Int
        get() = delegate.state

    override fun reset(buffer: CharSequence, start: Int, end: Int, initialState: Int) {
        content = buffer
        contentEnd = end
        consumedUntil = start
        pending.clear()
        tokenStart = start
        tokenEnd = start
        delegate.reset(buffer, start, end, initialState)
    }

    override fun advance(): IElementType? {
        if (!pending.isEmpty()) {
            val token = pending.removeFirst()
            tokenStart = token.start
            tokenEnd = token.end
            return token.type
        }

        while (true) {
            val type = delegate.advance() ?: return null
            val start = maxOf(delegate.tokenStart, consumedUntil)
            val end = delegate.tokenEnd
            if (start >= end) continue

            if (type != MarkdownTokenTypes.TEXT) {
                tokenStart = start
                tokenEnd = end
                consumedUntil = maxOf(consumedUntil, end)
                return type
            }

            var cursor = start
            val limit = minOf(end - 1, contentEnd - 1)
            var hasDelimiter = false
            while (cursor < limit) {
                if (content[cursor] == '|' && content[cursor + 1] == '|' && !isEscaped(cursor)) {
                    hasDelimiter = true
                    break
                }
                cursor++
            }

            if (!hasDelimiter) {
                tokenStart = start
                tokenEnd = end
                consumedUntil = maxOf(consumedUntil, end)
                return type
            }

            split(start, end)
            if (!pending.isEmpty()) {
                val token = pending.removeFirst()
                tokenStart = token.start
                tokenEnd = token.end
                return token.type
            }
        }
    }

    private fun split(start: Int, end: Int) {

        var cursor = start
        var segmentStart = start
        while (cursor < end) {
            if (
                cursor + 1 < contentEnd &&
                content[cursor] == '|' &&
                content[cursor + 1] == '|' &&
                !isEscaped(cursor)
            ) {
                if (segmentStart < cursor) {
                    pending += Token(MarkdownTokenTypes.TEXT, segmentStart, cursor)
                }
                pending += Token(SPOILER_DELIMITER_TOKEN_TYPE, cursor, cursor + 2)
                cursor += 2
                segmentStart = cursor
            } else {
                cursor++
            }
        }

        if (segmentStart < end) {
            pending += Token(MarkdownTokenTypes.TEXT, segmentStart, end)
        }
        consumedUntil = maxOf(consumedUntil, cursor, end)
    }

    private fun isEscaped(offset: Int): Boolean {
        var backslashes = 0
        var cursor = offset - 1
        while (cursor >= 0 && content[cursor] == '\\') {
            backslashes++
            cursor--
        }
        return backslashes % 2 != 0
    }
}
