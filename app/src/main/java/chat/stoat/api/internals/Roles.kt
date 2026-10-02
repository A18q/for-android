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

object Roles {
    fun invalidateCache() {
        // Cache removed to prevent stale 0L lockouts
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
        val user = member.id?.user?.let { StoatAPI.userCache[it] }

        if (user?.privileged == true) return PermissionBit.GrantAllSafe.value
        if (server.owner == member.id?.user) return PermissionBit.GrantAllSafe.value

        var calculated = server.defaultPermissions ?: BitDefaults.Server

        if (calculated.hasPermission(PermissionBit.GrantAllSafe) || calculated.hasPermission(PermissionBit.GrantAll)) {
            return PermissionBit.GrantAllSafe.value
        }

        val sortedRoles = member.roles?.mapNotNull { server.roles?.get(it) }?.sortedByDescending { it.rank ?: Double.MAX_VALUE }
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
        return when (channel.channelType) {
            ChannelType.SavedMessages -> BitDefaults.SavedMessages

            ChannelType.DirectMessage -> BitDefaults.DirectMessages
            ChannelType.Group -> if (channel.owner == user?.id) PermissionBit.GrantAllSafe.value else BitDefaults.DirectMessages

            ChannelType.TextChannel, ChannelType.VoiceChannel -> {
                val targetServer = server ?: StoatAPI.serverCache[channel.server]
                    ?: return 0L
                val srvId = targetServer.id ?: return 0L

                val userId = user?.id ?: member?.id?.user
                if (targetServer.owner == userId) return PermissionBit.GrantAllSafe.value

                val chMember = member ?: (if (userId != null) StoatAPI.members.getMember(srvId, userId) else null)

                var calculated = if (chMember != null) {
                    permissionFor(targetServer, chMember)
                } else {
                    targetServer.defaultPermissions ?: BitDefaults.Server
                }

                if (calculated.hasPermission(PermissionBit.GrantAllSafe) || calculated.hasPermission(PermissionBit.GrantAll)) {
                    return PermissionBit.GrantAllSafe.value
                }

                val memberRolesSorted = chMember?.roles
                    ?.mapNotNull { id -> targetServer.roles?.get(id)?.let { id to it } }
                    ?.sortedByDescending { it.second.rank ?: Double.MAX_VALUE }
                    .orEmpty()

                // Category overrides: default, then roles, then user
                val category = targetServer.categories?.firstOrNull { it.channels?.contains(channel.id) == true }
                if (category != null) {
                    category.defaultPermissions?.let {
                        calculated = (calculated or it.a) and it.d.inv()
                    }
                    memberRolesSorted.forEach { (roleId, _) ->
                        category.rolePermissions?.get(roleId)?.let {
                            calculated = (calculated or it.a) and it.d.inv()
                        }
                    }
                    if (userId != null) {
                        category.userPermissions?.get(userId)?.let {
                            calculated = (calculated or it.a) and it.d.inv()
                        }
                    }
                }

                // Channel overrides: default, then roles, then user
                channel.defaultPermissions?.let {
                    calculated = (calculated or it.a) and it.d.inv()
                }
                memberRolesSorted.forEach { (roleId, _) ->
                    channel.rolePermissions?.get(roleId)?.let {
                        calculated = (calculated or it.a) and it.d.inv()
                    }
                }
                if (userId != null) {
                    channel.userPermissions?.get(userId)?.let {
                        calculated = (calculated or it.a) and it.d.inv()
                    }
                }

                if (chMember?.canPublish == false) {
                    calculated = calculated and PermissionBit.Speak.value.inv()
                    calculated = calculated and PermissionBit.Video.value.inv()
                }
                if (chMember?.canReceive == false) {
                    calculated = calculated and PermissionBit.Listen.value.inv()
                }

                if (chMember?.timeoutTimestamp()?.let { it > Clock.System.now() } == true) {
                    calculated = calculated and BitDefaults.AllowedInTimeout
                }

                if (!calculated.hasPermission(PermissionBit.ViewChannel)) {
                    return 0L
                }

                return calculated
            }

            null -> 0L
        }
    }
}
