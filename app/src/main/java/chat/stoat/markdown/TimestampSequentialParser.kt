package chat.stoat.markdown

import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

class TimestampSequentialParser(private val content: String) : SequentialParser {
    override fun parse(tokens: TokensCache, rangesToGlue: List<IntRange>): SequentialParser.ParsingResult {
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

        while (iterator.type != null) {
            if (iterator.type == MarkdownTokenTypes.LT) {
                val ltEnd = iterator.end

                var lookahead = iterator.advance()
                var hops = 0
                while (lookahead.type != null &&
                    lookahead.type != MarkdownTokenTypes.GT &&
                    lookahead.type != MarkdownTokenTypes.EOL &&
                    hops < 15
                ) {
                    lookahead = lookahead.advance()
                    hops++
                }

                if (lookahead.type == MarkdownTokenTypes.GT) {
                    if (isTimestamp(content, ltEnd, lookahead.start)) {
                        result.withNode(SequentialParser.Node(iterator.index..lookahead.index + 1, TIMESTAMP_ELEMENT_TYPE))
                        iterator = lookahead.advance()
                        continue
                    }
                }
            }
            delegateIndices.put(iterator.index)
            iterator = iterator.advance()
        }

        return result.withFurtherProcessing(delegateIndices.get())
    }

    private fun isTimestamp(content: CharSequence, start: Int, end: Int): Boolean {
        val len = end - start
        if (len < 3 || content[start] != 't' || content[start + 1] != ':') return false
        val digitsStart = start + 2
        var colonIdx = -1
        for (i in digitsStart until end) {
            if (content[i] == ':') {
                colonIdx = i
                break
            }
        }
        return if (colonIdx == -1) {
            if (digitsStart >= end) false
            else {
                for (i in digitsStart until end) {
                    if (!content[i].isDigit()) return false
                }
                true
            }
        } else {
            if (digitsStart >= colonIdx) return false
            for (i in digitsStart until colonIdx) {
                if (!content[i].isDigit()) return false
            }
            (end - colonIdx - 1) == 1
        }
    }
}
