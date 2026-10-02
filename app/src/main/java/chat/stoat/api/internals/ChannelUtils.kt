package chat.stoat.api.internals

import chat.stoat.api.StoatAPI
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User

sealed class CategorisedChannelList {
    data class Channel(val channel: chat.stoat.core.model.schemas.Channel) :
        CategorisedChannelList()

    data class Category(val category: chat.stoat.core.model.schemas.Category) :
        CategorisedChannelList()
}

object ChannelUtils {
    /**
     * Resolves the name of a channel, preferring the name of the channel itself, then the name of the first recipient.
     * @param channel The channel to resolve the name of.
     * @return The name of the channel, or the name of the first recipient if the channel is a DM.
     * @see User.resolveDefaultName
     */
    fun resolveName(channel: Channel): String? {
        return channel.name
            ?: StoatAPI.userCache[channel.recipients?.firstOrNull { u -> u != StoatAPI.selfId }]?.let {
                User.resolveDefaultName(
                    it
                )
            }
    }

    fun resolveDMPartner(channel: Channel): String? {
        return channel.recipients?.firstOrNull { u -> u != StoatAPI.selfId }
    }

    private data class ServerCategoryCacheKey(val serverId: String, val categories: List<chat.stoat.core.model.schemas.Category>?, val channelsHash: Int)
    private val serverCategoryCache = java.util.concurrent.ConcurrentHashMap<String, Pair<ServerCategoryCacheKey, List<CategorisedChannelList>>>()

    fun categoriseServerFlat(server: Server): List<CategorisedChannelList> {
        val serverId = server.id ?: return emptyList()
        var channelsHash = 0
        server.channels?.forEach { c -> channelsHash = 31 * channelsHash + (StoatAPI.channelCache[c]?.hashCode() ?: 0) }
        val key = ServerCategoryCacheKey(serverId, server.categories, channelsHash)
        
        serverCategoryCache[serverId]?.let { (cachedKey, cachedList) ->
            if (cachedKey == key) return cachedList
        }

        val output = mutableListOf<CategorisedChannelList>()
        val categories = server.categories

        val categorizedChannelIds = HashSet<String>()
        if (categories != null) {
            for (cat in categories) {
                cat.channels?.let { categorizedChannelIds.addAll(it) }
            }
        }

        server.channels?.forEach { c ->
            if (c !in categorizedChannelIds) {
                StoatAPI.channelCache[c]?.let {
                    output.add(CategorisedChannelList.Channel(it))
                }
            }
        }

        if (categories != null) {
            for (cat in categories) {
                output.add(CategorisedChannelList.Category(cat))
                cat.channels?.forEach { c ->
                    StoatAPI.channelCache[c]?.let {
                        output.add(CategorisedChannelList.Channel(it))
                    }
                }
            }
        }

        serverCategoryCache[serverId] = Pair(key, output)
        return output
    }
}
