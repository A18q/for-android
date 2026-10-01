package chat.stoat.markdown

import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

internal fun CharSequence.isUlidAt(start: Int, length: Int): Boolean {
    if (length != 26 || start < 0 || start + 26 > this.length) return false
    for (i in start until start + 26) {
        val c = this[i]
        if (!((c in '0'..'9') || (c in 'A'..'H') || (c in 'J'..'K') || (c in 'M'..'N') || (c in 'P'..'T') || (c in 'V'..'Z'))) {
            return false
        }
    }
    return true
}

class CustomEmoteSequentialParser(private val content: String) : SequentialParser {

    override fun parse(
        tokens: TokensCache,
        rangesToGlue: List<IntRange>,
    ): SequentialParser.ParsingResult {
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

        while (iterator.type != null) {
            if (iterator.type == MarkdownTokenTypes.COLON) {
                val openEnd = iterator.end

                var lookahead = iterator.advance()
                var hops = 0
                while (lookahead.type != null &&
                    lookahead.type != MarkdownTokenTypes.COLON &&
                    lookahead.type != MarkdownTokenTypes.EOL &&
                    hops < 30
                ) {
                    lookahead = lookahead.advance()
                    hops++
                }

                if (lookahead.type == MarkdownTokenTypes.COLON) {
                    if (content.isUlidAt(openEnd, lookahead.start - openEnd)) {
                        result.withNode(
                            SequentialParser.Node(
                                iterator.index..lookahead.index + 1,
                                CUSTOM_EMOTE_ELEMENT_TYPE
                            )
                        )
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
}
