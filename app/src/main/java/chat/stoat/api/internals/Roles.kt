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
        val user = StoatAPI.userCache[member.id?.user] ?: return 0L

        if (user.privileged == true) return PermissionBit.GrantAllSafe.value
        if (server.owner == member.id?.user) return PermissionBit.GrantAllSafe.value

        var calculated = server.defaultPermissions ?: BitDefaults.Server

        if (calculated.hasPermission(PermissionBit.GrantAllSafe) || calculated.hasPermission(PermissionBit.GrantAll)) {
            return PermissionBit.GrantAllSafe.value
        }

        val sortedRoles = member.roles?.mapNotNull { server.roles?.get(it) }?.sortedByDescending { it.rank ?: 0.0 }
        sortedRoles?.forEach { role ->
            val permissions = role.permissions ?: return@forEach

            calculated = calculated or permissions.a and permissions.d.inv()
            if (calculated.hasPermission(PermissionBit.GrantAllSafe) || calculated.hasPermission(PermissionBit.GrantAll)) {
                return PermissionBit.GrantAllSafe.value
            }
        }

        if (member.timeoutTimestamp()?.let { it > Clock.System.now() } == true) {
            calculated = calculated and BitDefaults.AllowedInTimeout
        }

        return calculated
    }

    // TODO may not be exactly accurate
    // See https://github.com/revoltchat/revolt.js/blob/2ba023c879b2a53f9a3cc7042e6721c28dd970ba/src/permissions/calculator.ts#L80-L158
    fun permissionFor(channel: Channel, user: User? = null, member: Member? = null): Long {
        return when (channel.channelType) {
            ChannelType.SavedMessages -> BitDefaults.SavedMessages

            ChannelType.DirectMessage -> BitDefaults.DirectMessages
            ChannelType.Group -> if (channel.owner == user?.id) PermissionBit.GrantAllSafe.value else BitDefaults.DirectMessages

            ChannelType.TextChannel, ChannelType.VoiceChannel -> {
                val server = StoatAPI.serverCache[channel.server]
                    ?: return 0L

                if (server.owner == user?.id) return PermissionBit.GrantAllSafe.value

                val chMember = member ?: StoatAPI.members.getMember(
                    server.id ?: return 0L,
                    user?.id ?: return 0L
                ) ?: return 0L

                var calculated = permissionFor(server, chMember)

                if (calculated.hasPermission(PermissionBit.GrantAllSafe) || calculated.hasPermission(PermissionBit.GrantAll)) {
                    return PermissionBit.GrantAllSafe.value
                }

                if (channel.defaultPermissions != null) {
                    calculated =
                        calculated or channel.defaultPermissions!!.a and channel.defaultPermissions!!.d.inv()
                }

                if (chMember.roles?.isNotEmpty() == true) {
                    val sortedRoles = chMember.roles!!.mapNotNull { id -> server.roles?.get(id)?.let { id to it } }.sortedByDescending { it.second.rank ?: 0.0 }
                    sortedRoles.forEach { (roleId, role) ->
                        val override = channel.rolePermissions?.get(roleId) ?: return@forEach
                        calculated = calculated or override.a and override.d.inv()
                        if (calculated.hasPermission(PermissionBit.GrantAllSafe) || calculated.hasPermission(PermissionBit.GrantAll)) {
                            return PermissionBit.GrantAllSafe.value
                        }
                    }
                }

                if (chMember.timeoutTimestamp()?.let { it > Clock.System.now() } == true) {
                    calculated = calculated and BitDefaults.AllowedInTimeout
                }

                return calculated
            }

            null -> 0L
        }
    }
}
