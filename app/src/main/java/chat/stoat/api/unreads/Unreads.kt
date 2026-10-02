package chat.stoat.api.unreads

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ULID
import chat.stoat.api.routes.channel.ackChannel
import chat.stoat.api.routes.server.ackServer
import chat.stoat.api.routes.sync.syncUnreads
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.ChannelUnread
import chat.stoat.api.settings.NotificationSettingsProvider

class Unreads {
    private val hasLoaded = mutableStateOf(false)
    private val channels = mutableStateMapOf<String, ChannelUnread>()

    suspend fun sync() {
        channels.clear()
        try {
            val unreadList = syncUnreads()
            for (i in unreadList.indices) {
                val item = unreadList[i]
                channels[item.id.channel] = ChannelUnread(
                    id = item.id.channel,
                    last_id = item.last_id,
                    mentions = item.mentions
                )
            }
        } catch (e: Exception) {
            Log.e("Unreads", "Failed to sync unreads", e)
        }
        hasLoaded.value = true
    }

    fun getForChannel(channelId: String, serverId: String?): ChannelUnread? {
        if (!hasLoaded.value) return null
        if (NotificationSettingsProvider.isChannelMuted(channelId, serverId)) return null
        return channels[channelId]
    }

    fun hasUnread(channelId: String, lastMessageId: String, serverId: String?): Boolean {
        if (!hasLoaded.value) return false
        if (NotificationSettingsProvider.isChannelMuted(channelId, serverId)) return false
        return (channels[channelId]?.last_id?.compareTo(lastMessageId) ?: 0) < 0
    }

    fun serverHasUnread(serverId: String): Boolean {
        if (!hasLoaded.value) return false

        return StoatAPI.serverCache[serverId]?.channels?.any {
            val channel = StoatAPI.channelCache[it] ?: return@any false // Channel not found
            if (channel.channelType == ChannelType.VoiceChannel) return@any false // Channel is voice
            if (NotificationSettingsProvider.isChannelMuted(
                    it,
                    serverId
                )
            ) return@any false // Channel is muted
            hasUnread(it, channel.lastMessageID ?: "", serverId) // Channel has unread
        } == true // Null guard
    }

    suspend fun markAsRead(channelId: String, messageId: String, sync: Boolean = true) {
        if (!hasLoaded.value) return
        val current = channels[channelId]
        if (current != null) {
            channels[channelId] = current.copy(last_id = messageId)
        }
        if (sync) {
            ackChannel(channelId, messageId)
        }
    }

    fun processExternalAck(channelId: String, messageId: String) {
        val current = channels[channelId]
        if (current != null) {
            channels[channelId] = current.copy(last_id = messageId)
        }
    }

    suspend fun markServerAsRead(serverId: String, sync: Boolean = true) {
        if (!hasLoaded.value) return

        val server = StoatAPI.serverCache[serverId] ?: return
        server.channels?.forEach { channel ->
            channels[channel] = channels[channel]?.copy(last_id = ULID.makeNext()) ?: ChannelUnread(
                channel,
                ULID.makeNext()
            )
        }

        if (sync) {
            ackServer(serverId)
        }
    }

    fun getAllUnreads(): List<ChannelUnread> {
        if (!hasLoaded.value) return emptyList()
        return channels.values.toList()
    }

    fun removeChannels(channelIds: Collection<String>) {
        channelIds.forEach(channels::remove)
    }

    /**
     * Returns true if there are any unreads in any of the channels that are not muted.
     * **SLOW:** Run in a background coroutine.
     */
    fun hasAnyUnreads(): Boolean {
        if (!hasLoaded.value) return false

        var found = false
        val deadChannels = mutableListOf<String>()
        for ((channelId, _) in channels) {
            val channel = StoatAPI.channelCache[channelId]
            if (channel == null) {
                deadChannels.add(channelId)
                continue
            }
            if (found) continue
            if (NotificationSettingsProvider.isChannelMuted(channelId, channel.server)) continue
            if (hasUnread(channelId, channel.lastMessageID ?: "", channel.server)) {
                found = true
            }
        }
        deadChannels.forEach { channels.remove(it) }
        return found
    }

    /**
     * Returns the count of channels with unreads that are not muted.
     * **SLOW:** Run in a background coroutine.
     */
    fun countChannelsWithUnreads(): Int? {
        if (!hasLoaded.value) return null

        var count = 0
        val deadChannels = mutableListOf<String>()
        for ((channelId, _) in channels) {
            val channel = StoatAPI.channelCache[channelId]
            if (channel == null) {
                deadChannels.add(channelId)
                continue
            }
            if (NotificationSettingsProvider.isChannelMuted(channelId, channel.server)) continue
            if (hasUnread(channelId, channel.lastMessageID ?: "", channel.server)) {
                count++
            }
        }
        deadChannels.forEach { channels.remove(it) }
        return count
    }

    fun clear() {
        channels.clear()
        hasLoaded.value = false
    }
}
