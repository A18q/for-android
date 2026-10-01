package chat.stoat.internals.text

import chat.stoat.api.StoatAPI
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.internals.EmojiImpl

object MessageProcessor {
    private val MentionRegex = Regex("@((?:\\p{L}|[\\d_.-])+)#([0-9]{4})", RegexOption.IGNORE_CASE)
    private val ChannelRegex = Regex("(?:\\s|^)#(.+?)(?:\\s|\$)", RegexOption.IGNORE_CASE)
    private val EmoteRegex = Regex(":([a-zA-Z0-9_+-]+):", RegexOption.IGNORE_CASE)

    val emoji = EmojiImpl()

    /**
     * Processes an outgoing message for sending.
     * 1. Replaces @mentions#0000 with <@userId>
     * 2. Replaces #channel with <#channelId> if the current server has a channel with that name
     * 2. Replaces :emoji-shortcode: with the emoji's unicode character, if it exists
     */
    fun processOutgoing(content: String, serverId: String?): String {
        var returnable = if (content.contains('@')) {
            MentionRegex.replace(content) { match ->
                val (username, discriminator) = match.destructured
                val user =
                    StoatAPI.userCache.values.find { it.username == username && it.discriminator == discriminator }
                val userId = user?.id
                if (userId != null) "<@$userId>" else match.value
            }
        } else {
            content
        }

        if (returnable.contains('#')) {
            val channels = ChannelRegex.findAll(returnable)
            for (match in channels) {
                val channelName = match.groups[1]?.value ?: continue
                val fetchedChannel =
                    StoatAPI.channelCache.values.find {
                        it.name == channelName && it.server == serverId && it.channelType == ChannelType.TextChannel
                    } ?: continue

                fetchedChannel.name?.let {
                    returnable = returnable.replace("#$it", "<#${fetchedChannel.id}>")
                }
            }
        }

        if (returnable.contains(':')) {
            returnable = EmoteRegex.replace(returnable) { match ->
                val emojiName = match.groupValues[1]
                val byShortcode = emoji.unicodeByShortcode(emojiName)
                byShortcode ?: match.value
            }
        }

        return returnable
    }

    private val roleRegex = Regex("<%([0-9A-HJKMNP-TV-Z]{26})>")
    fun findMentionedRoleIDs(content: String?): List<String> {
        if (content.isNullOrEmpty() || !content.contains("<%")) return emptyList()
        val matches = roleRegex.findAll(content)
        val result = ArrayList<String>(2)
        for (match in matches) {
            val roleId = match.groupValues[1]
            if (roleId.isNotEmpty() && !result.contains(roleId)) {
                result.add(roleId)
            }
        }
        return result
    }
}