package chat.stoat.api.internals

import chat.stoat.api.StoatAPI
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User
import kotlinx.datetime.Clock

import java.util.concurrent.ConcurrentHashMap

object Roles {
    private val channelPermissionCache = ConcurrentHashMap<String, Long>()
    private val serverPermissionCache = ConcurrentHashMap<String, Long>()

    fun invalidateCache() {
        channelPermissionCache.clear()
        serverPermissionCache.clear()
    }

    // lowest rank = highest role
    fun resolveHighestRole(
        serverId: String,
        userId: String,
        withColour: Boolean = false,
        hoisted: Boolean = false
    ): Role? {
        val server = StoatAPI.serverCache[serverId] ?: return null
        val member = StoatAPI.members.getMember(serverId, userId) ?: return null
        val memberRoles = member.roles ?: return null
        val serverRoles = server.roles ?: return null

        var bestRole: Role? = null
        var bestRank = Double.MAX_VALUE

        for (i in memberRoles.indices) {
            val role = serverRoles[memberRoles[i]] ?: continue
            if (hoisted && role.hoist != true) continue
            if (withColour && role.colour == null) continue
            val rank = role.rank ?: 0.0
            if (rank < bestRank) {
                bestRank = rank
                bestRole = role
            }
        }

        return bestRole
    }

    fun inOrder(serverId: String, predicate: (Role) -> Boolean): List<Role> {
        val server = StoatAPI.serverCache[serverId] ?: return emptyList()

        return server.roles?.values?.filter(predicate)?.sortedBy { it.rank } ?: emptyList()
    }

    fun permissionFor(server: Server, member: Member): Long {
        val userId = member.id?.user ?: return 0L
        val serverId = server.id.orEmpty()
        val cacheKey = "$serverId:$userId"
        serverPermissionCache[cacheKey]?.let { return it }

        val computed = computeServerPermission(server, member)
        serverPermissionCache[cacheKey] = computed
        return computed
    }

    private fun computeServerPermission(server: Server, member: Member): Long {
        val userId = member.id?.user ?: return 0L
        val user = StoatAPI.userCache[userId]

        // 1. Owner or privileged user: allow all
        if (user?.privileged == true) return PermissionBit.GrantAllSafe.value
        if (server.owner == userId) return PermissionBit.GrantAllSafe.value

        // 2. Server default permissions
        var calculated = server.defaultPermissions ?: BitDefaults.Server

        if (calculated.hasPermission(PermissionBit.GrantAllSafe) || calculated.hasPermission(PermissionBit.GrantAll)) {
            return PermissionBit.GrantAllSafe.value
        }

        // 3. Member's roles in rank order (lowest to highest priority)
        val sortedRoles = member.roles
            ?.mapNotNull { server.roles?.get(it) }
            ?.sortedByDescending { it.rank ?: Double.MAX_VALUE }

        sortedRoles?.forEach { role ->
            val permissions = role.permissions ?: return@forEach
            calculated = (calculated or permissions.a) and permissions.d.inv()
            if (calculated.hasPermission(PermissionBit.GrantAllSafe) || calculated.hasPermission(PermissionBit.GrantAll)) {
                return PermissionBit.GrantAllSafe.value
            }
        }

        if (member.timeoutTimestamp()?.let { it > Clock.System.now() } == true) {
            calculated = calculated and BitDefaults.AllowedInTimeout
        }

        return calculated
    }

    fun permissionFor(
        channel: Channel,
        user: User? = null,
        member: Member? = null,
        server: Server? = null
    ): Long {
        val channelId = channel.id.orEmpty()
        val userId = user?.id ?: member?.id?.user.orEmpty()
        val cacheKey = "$channelId:$userId"
        if (channelId.isNotEmpty() && userId.isNotEmpty()) {
            channelPermissionCache[cacheKey]?.let { return it }
        }

        val computed = computeChannelPermission(channel, user, member, server)
        if (channelId.isNotEmpty() && userId.isNotEmpty()) {
            channelPermissionCache[cacheKey] = computed
        }
        return computed
    }

    private fun computeChannelPermission(
        channel: Channel,
        user: User? = null,
        member: Member? = null,
        serverOpt: Server? = null
    ): Long {
        return when (channel.channelType) {
            ChannelType.SavedMessages -> BitDefaults.SavedMessages

            ChannelType.DirectMessage -> BitDefaults.DirectMessages
            ChannelType.Group -> if (channel.owner == user?.id) PermissionBit.GrantAllSafe.value else BitDefaults.DirectMessages

            ChannelType.TextChannel, ChannelType.VoiceChannel -> {
                val server = serverOpt ?: StoatAPI.serverCache[channel.server]
                    ?: return 0L
                val userId = user?.id ?: member?.id?.user ?: return 0L

                // 1. Owner: allow all
                if (server.owner == userId) return PermissionBit.GrantAllSafe.value

                val chMember = member ?: StoatAPI.members.getMember(
                    server.id ?: return 0L,
                    userId
                ) ?: return 0L

                // 2. Server default permissions & 3. Member's roles in rank order
                var calculated = computeServerPermission(server, chMember)
                if (calculated.hasPermission(PermissionBit.GrantAllSafe) || calculated.hasPermission(PermissionBit.GrantAll)) {
                    return PermissionBit.GrantAllSafe.value
                }

                val memberRolesSorted = chMember.roles
                    ?.mapNotNull { id -> server.roles?.get(id)?.let { id to it } }
                    ?.sortedByDescending { it.second.rank ?: Double.MAX_VALUE }
                    .orEmpty()

                // 4. Category overrides: default, then roles, then member
                val category = server.categories?.firstOrNull { it.channels?.contains(channel.id) == true }
                if (category != null) {
                    category.defaultPermissions?.let {
                        calculated = (calculated or it.a) and it.d.inv()
                    }
                    memberRolesSorted.forEach { (roleId, _) ->
                        category.rolePermissions?.get(roleId)?.let {
                            calculated = (calculated or it.a) and it.d.inv()
                        }
                    }
                    category.userPermissions?.get(userId)?.let {
                        calculated = (calculated or it.a) and it.d.inv()
                    }
                }

                // 5. Channel overrides: default, then roles, then member
                channel.defaultPermissions?.let {
                    calculated = (calculated or it.a) and it.d.inv()
                }
                memberRolesSorted.forEach { (roleId, _) ->
                    channel.rolePermissions?.get(roleId)?.let {
                        calculated = (calculated or it.a) and it.d.inv()
                    }
                }
                channel.userPermissions?.get(userId)?.let {
                    calculated = (calculated or it.a) and it.d.inv()
                }

                if (chMember.canPublish == false) {
                    calculated = calculated and PermissionBit.Speak.value.inv()
                    calculated = calculated and PermissionBit.Video.value.inv()
                }
                if (chMember.canReceive == false) {
                    calculated = calculated and PermissionBit.Listen.value.inv()
                }

                if (chMember.timeoutTimestamp()?.let { it > Clock.System.now() } == true) {
                    calculated = calculated and BitDefaults.AllowedInTimeout
                }

                // 6. If the member can't view the channel, it is hidden and everything else is moot
                if (!calculated.hasPermission(PermissionBit.ViewChannel)) {
                    return 0L
                }

                return calculated
            }

            null -> 0L
        }
    }
}
