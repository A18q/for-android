package chat.stoat.api.internals

import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.SpecialUsers.PLATFORM_MODERATION_USER
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType

object DirectMessages {
    fun unreadDMs(): List<Channel> {
        val unreads = StoatAPI.unreads
        return StoatAPI.channelCache.values.filter { channel ->
            val channelId = channel.id ?: return@filter false
            val lastMsgId = channel.lastMessageID ?: return@filter false
            val type = channel.channelType
            (type == ChannelType.DirectMessage || type == ChannelType.Group) &&
                    channel.active == true &&
                    unreads.hasUnread(channelId, lastMsgId, serverId = null)
        }
    }

    fun hasPlatformModerationDM(): Boolean {
        return unreadDMs().any {
            it.channelType == ChannelType.DirectMessage &&
                    it.recipients?.contains(PLATFORM_MODERATION_USER) == true
        }
    }

    fun getPlatformModerationDM(): Channel? {
        return unreadDMs().firstOrNull {
            it.channelType == ChannelType.DirectMessage &&
                    it.recipients?.contains(PLATFORM_MODERATION_USER) == true
        }
    }
}
