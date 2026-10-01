package chat.stoat.markdown

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.lexer.MarkdownLexer
import org.intellij.markdown.parser.sequentialparsers.EmphasisLikeParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParserManager

class StoatMarkdownFlavour(private val content: String) : GFMFlavourDescriptor() {
    override fun createInlinesLexer(): MarkdownLexer = MarkdownLexer(SpoilerLexer())

    override val sequentialParserManager = object : SequentialParserManager() {
        override fun getParserSequence(): List<SequentialParser> {
            val result = ArrayList<SequentialParser>(4 + upstreamBeforeEmphasis.size + upstreamFromEmphasis.size)
            result.add(MentionSequentialParser(content))
            result.add(TimestampSequentialParser(content))
            result.add(CustomEmoteSequentialParser(content))
            result.addAll(upstreamBeforeEmphasis)
            result.add(spoilerSequentialParser)
            result.addAll(upstreamFromEmphasis)
            return result
        }
    }

    companion object {
        private val upstreamParsers: List<SequentialParser> by lazy {
            GFMFlavourDescriptor().sequentialParserManager.getParserSequence()
        }

        private val emphasisIndex: Int by lazy {
            val indices = upstreamParsers.indices.filter { upstreamParsers[it] is EmphasisLikeParser }
            check(indices.size == 1) {
                "Expected exactly one upstream emphasis parser, found ${indices.size}"
            }
            indices.single()
        }

        private val upstreamBeforeEmphasis: List<SequentialParser> by lazy {
            upstreamParsers.subList(0, emphasisIndex)
        }

        private val upstreamFromEmphasis: List<SequentialParser> by lazy {
            upstreamParsers.subList(emphasisIndex, upstreamParsers.size)
        }

        private val spoilerSequentialParser = SpoilerSequentialParser()
    }
}
