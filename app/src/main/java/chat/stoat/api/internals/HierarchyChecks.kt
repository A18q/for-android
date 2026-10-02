package chat.stoat.api.internals

import chat.stoat.api.StoatAPI
import chat.stoat.core.model.schemas.Category
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server

object HierarchyChecks {

    fun resolveMemberTopRoleRank(server: Server, userId: String, memberFallback: Member? = null): Double {
        if (server.owner == userId) return Double.NEGATIVE_INFINITY
        val member = memberFallback ?: StoatAPI.members.getMember(server.id.orEmpty(), userId) ?: return Double.MAX_VALUE
        return member.roles.orEmpty()
            .mapNotNull { server.roles?.get(it)?.rank }
            .minOrNull() ?: Double.MAX_VALUE
    }

    fun checkCanManageRole(server: Server, actorUserId: String, targetRole: Role, actorMember: Member? = null) {
        if (server.owner == actorUserId) return
        val actorRank = resolveMemberTopRoleRank(server, actorUserId, actorMember)
        val targetRank = targetRole.rank ?: Double.MAX_VALUE
        if (targetRank <= actorRank) {
            throw SecurityException(
                "Hierarchy violation: Cannot edit or delete a role ranked equal to or higher than your top role"
            )
        }
    }

    fun checkCanManageRole(server: Server, actorUserId: String, roleId: String, actorMember: Member? = null) {
        if (server.owner == actorUserId) return
        val role = server.roles?.get(roleId) ?: throw SecurityException("Role not found: $roleId")
        checkCanManageRole(server, actorUserId, role, actorMember)
    }

    fun checkCanAssignRole(server: Server, actorUserId: String, roleId: String) {
        if (server.owner == actorUserId) return
        val actorRank = resolveMemberTopRoleRank(server, actorUserId)
        val role = server.roles?.get(roleId)
        val targetRank = role?.rank ?: Double.MAX_VALUE
        if (targetRank <= actorRank) {
            throw SecurityException(
                "Hierarchy violation: Cannot assign a role ranked equal to or higher than your top role"
            )
        }
    }

    fun checkNoPrivilegeEscalation(actorPermissions: Long, requestedPermissions: PermissionDescription) {
        checkNoPrivilegeEscalation(actorPermissions, requestedPermissions.a)
    }

    fun checkNoPrivilegeEscalation(actorPermissions: Long, grantedPermissions: Long) {
        if (actorPermissions.hasPermission(PermissionBit.GrantAllSafe) ||
            actorPermissions.hasPermission(PermissionBit.GrantAll)
        ) {
            return
        }
        val unauthorized = grantedPermissions and actorPermissions.inv()
        if (unauthorized != 0L) {
            throw SecurityException(
                "Privilege escalation: Cannot grant permission bits not held by actor: $unauthorized"
            )
        }
    }

    fun checkNoPrivilegeEscalation(
        server: Server,
        caller: Member,
        requestedAllow: Long,
        requestedDeny: Long = 0L
    ) {
        val actorPermissions = Roles.permissionFor(server, caller)
        checkNoPrivilegeEscalation(actorPermissions, requestedAllow)
    }

    fun checkCanModerateMember(
        server: Server,
        actorUserId: String,
        targetUserId: String,
        actorMember: Member? = null,
        targetMember: Member? = null
    ) {
        if (server.owner == actorUserId) return
        if (server.owner == targetUserId) {
            throw SecurityException("Hierarchy violation: Cannot moderate the server owner")
        }
        val actorRank = resolveMemberTopRoleRank(server, actorUserId, actorMember)
        val targetRank = resolveMemberTopRoleRank(server, targetUserId, targetMember)
        if (targetRank <= actorRank) {
            throw SecurityException(
                "Hierarchy violation: Cannot edit, timeout, or kick a member whose top rank is equal to or above yours"
            )
        }
    }

    fun checkCanManageChannel(
        server: Server,
        actorUserId: String,
        categoryId: String? = null,
        callerMember: Member? = null
    ) {
        if (server.owner == actorUserId) return
        val member = callerMember ?: StoatAPI.members.getMember(server.id.orEmpty(), actorUserId)
            ?: throw SecurityException("Actor is not a member of the server")
        var perms = Roles.permissionFor(server, member)
        if (categoryId != null) {
            val category = server.categories?.firstOrNull { it.id == categoryId }
            category?.defaultPermissions?.let { perms = (perms or it.a) and it.d.inv() }
            category?.rolePermissions?.let { roleOverrides ->
                member.roles.orEmpty().forEach { rId ->
                    roleOverrides[rId]?.let { perms = (perms or it.a) and it.d.inv() }
                }
            }
            category?.userPermissions?.get(actorUserId)?.let { perms = (perms or it.a) and it.d.inv() }
        }
        if (!perms.hasPermission(PermissionBit.ManageChannel)) {
            throw SecurityException("Missing Permission: ManageChannel required")
        }
    }

    fun checkCanMoveChannel(
        server: Server,
        actorUserId: String,
        sourceCategoryId: String?,
        targetCategoryId: String?,
        callerMember: Member? = null
    ) {
        if (server.owner == actorUserId) return
        checkCanManageChannel(server, actorUserId, sourceCategoryId, callerMember)
        if (sourceCategoryId != targetCategoryId) {
            checkCanManageChannel(server, actorUserId, targetCategoryId, callerMember)
        }
    }

    fun checkCanManageRoles(server: Server, actorUserId: String, callerMember: Member? = null) {
        if (server.owner == actorUserId) return
        val member = callerMember ?: StoatAPI.members.getMember(server.id.orEmpty(), actorUserId)
            ?: throw SecurityException("Actor is not a member of the server")
        val perms = Roles.permissionFor(server, member)
        if (!perms.hasPermission(PermissionBit.ManageRole)) {
            throw SecurityException("Missing Permission: ManageRole required")
        }
    }

    fun checkCanManageChannelOnChannel(
        server: Server,
        channel: Channel,
        actorUserId: String,
        callerMember: Member? = null
    ) {
        if (server.owner == actorUserId) return
        val member = callerMember ?: StoatAPI.members.getMember(server.id.orEmpty(), actorUserId)
            ?: throw SecurityException("Actor is not a member of the server")
        val perms = Roles.permissionFor(channel, user = null, member = member, server = server)
        if (!perms.hasPermission(PermissionBit.ManageChannel)) {
            throw SecurityException("Missing Permission: ManageChannel required on channel")
        }
    }

    fun checkCanManagePermissions(
        server: Server,
        channel: Channel,
        actorUserId: String,
        callerMember: Member? = null
    ) {
        if (server.owner == actorUserId) return
        val member = callerMember ?: StoatAPI.members.getMember(server.id.orEmpty(), actorUserId)
            ?: throw SecurityException("Actor is not a member of the server")
        val perms = Roles.permissionFor(channel, user = null, member = member, server = server)
        if (!perms.hasPermission(PermissionBit.ManagePermissions)) {
            throw SecurityException("Missing Permission: ManagePermissions required on channel")
        }
    }

    fun validateCategoriesPatch(
        server: Server,
        actorUserId: String,
        requestedCategories: List<Category>,
        callerMember: Member? = null
    ) {
        if (server.owner == actorUserId) return
        val member = callerMember ?: StoatAPI.members.getMember(server.id.orEmpty(), actorUserId)
            ?: throw SecurityException("Actor is not a member of the server")

        val serverChannels = server.channels.orEmpty().toSet()
        val allRequestedChannels = mutableSetOf<String>()
        for (cat in requestedCategories) {
            for (chId in cat.channels.orEmpty()) {
                if (!serverChannels.contains(chId)) {
                    throw SecurityException("Cross-server ID detected: IDOR prevented for channel $chId")
                }
                allRequestedChannels.add(chId)
            }
        }

        val oldCatIds = server.categories.orEmpty().mapNotNull { it.id }
        val newCatIds = requestedCategories.mapNotNull { it.id }
        if (oldCatIds != newCatIds) {
            checkCanManageChannel(server, actorUserId, null, member)
        }

        val oldChannelToCategory = mutableMapOf<String, String?>()
        server.categories.orEmpty().forEach { cat ->
            cat.channels.orEmpty().forEach { chId ->
                oldChannelToCategory[chId] = cat.id
            }
        }
        val newChannelToCategory = mutableMapOf<String, String?>()
        requestedCategories.forEach { cat ->
            cat.channels.orEmpty().forEach { chId ->
                newChannelToCategory[chId] = cat.id
            }
        }

        for (chId in allRequestedChannels) {
            val oldCat = oldChannelToCategory[chId]
            val newCat = newChannelToCategory[chId]
            if (oldCat != newCat) {
                checkCanMoveChannel(server, actorUserId, oldCat, newCat, member)
            }
        }
    }
}
