package chat.stoat

import chat.stoat.api.internals.HierarchyChecks
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.core.model.schemas.Category
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.ServerUserChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HierarchyAndPermissionsTest {

    @Before
    fun setUp() {
        Roles.invalidateCache()
    }

    private fun createServer(
        ownerId: String = "owner_123",
        defaultPerms: Long = PermissionBit.ViewChannel.value or PermissionBit.SendMessage.value,
        roles: Map<String, Role> = emptyMap(),
        categories: List<Category> = emptyList(),
        channels: List<String> = listOf("chan_1")
    ): Server {
        Roles.invalidateCache()
        return Server(
            id = "srv_1",
            owner = ownerId,
            name = "Test Server",
            channels = channels,
            defaultPermissions = defaultPerms,
            roles = roles,
            categories = categories
        )
    }

    private fun createMember(
        userId: String,
        roles: List<String> = emptyList()
    ): Member {
        return Member(
            id = ServerUserChoice("srv_1", userId),
            roles = roles
        )
    }

    private fun createChannel(
        channelId: String = "chan_1",
        rolePerms: Map<String, PermissionDescription>? = null,
        defaultPerms: PermissionDescription? = null,
        userPerms: Map<String, PermissionDescription>? = null
    ): Channel {
        Roles.invalidateCache()
        return Channel(
            id = channelId,
            server = "srv_1",
            name = "general",
            channelType = ChannelType.TextChannel,
            rolePermissions = rolePerms,
            defaultPermissions = defaultPerms,
            userPermissions = userPerms
        )
    }

    @Test
    fun step1_ownerHasAllPermissions() {
        val server = createServer(ownerId = "user_owner", defaultPerms = 0L)
        val member = createMember(userId = "user_owner")
        val channel = createChannel()

        val perms = Roles.permissionFor(channel, user = null, member = member, server = server)
        assertEquals(PermissionBit.GrantAllSafe.value, perms)
        assertTrue(perms and PermissionBit.ManageServer.value != 0L)
        assertTrue(perms and PermissionBit.ManageRole.value != 0L)
        assertTrue(perms and PermissionBit.ViewChannel.value != 0L)
    }

    @Test
    fun step2_defaultServerPermissionsApplyForRegularMember() {
        val server = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value or PermissionBit.SendMessage.value
        )
        val member = createMember(userId = "user_regular")
        val channel = createChannel()

        val perms = Roles.permissionFor(channel, user = null, member = member, server = server)
        assertEquals(PermissionBit.ViewChannel.value or PermissionBit.SendMessage.value, perms)
        assertEquals(0L, perms and PermissionBit.ManageServer.value)
    }

    @Test
    fun step3_memberRolesInRankOrderAreApplied() {
        val roleLow = Role(name = "Low", rank = 10.0, permissions = PermissionDescription(a = PermissionBit.SendEmbeds.value, d = 0L))
        val roleHigh = Role(name = "High", rank = 1.0, permissions = PermissionDescription(a = PermissionBit.UploadFiles.value, d = PermissionBit.SendEmbeds.value))

        val server = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value or PermissionBit.SendMessage.value,
            roles = mapOf("r_low" to roleLow, "r_high" to roleHigh)
        )
        val member = createMember(userId = "user_1", roles = listOf("r_low", "r_high"))
        val channel = createChannel()

        val perms = Roles.permissionFor(channel, user = null, member = member, server = server)
        // High rank (1.0) applies after low rank (10.0), denying SendEmbeds and allowing UploadFiles
        assertTrue(perms and PermissionBit.UploadFiles.value != 0L)
        assertEquals(0L, perms and PermissionBit.SendEmbeds.value)
        assertTrue(perms and PermissionBit.SendMessage.value != 0L)
    }

    @Test
    fun step4_categoryOverridesApplyInOrder() {
        val category = Category(
            id = "cat_1",
            title = "Text Channels",
            channels = listOf("chan_1"),
            defaultPermissions = PermissionDescription(a = 0L, d = PermissionBit.SendMessage.value),
            rolePermissions = mapOf("r_1" to PermissionDescription(a = PermissionBit.SendMessage.value, d = 0L)),
            userPermissions = mapOf("user_1" to PermissionDescription(a = 0L, d = PermissionBit.SendMessage.value))
        )
        val role1 = Role(name = "Mod", rank = 1.0, permissions = PermissionDescription(a = 0L, d = 0L))
        val server = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value or PermissionBit.SendMessage.value,
            roles = mapOf("r_1" to role1),
            categories = listOf(category)
        )
        val member = createMember(userId = "user_1", roles = listOf("r_1"))
        val channel = createChannel(channelId = "chan_1")

        // user override in category denies SendMessage
        val perms = Roles.permissionFor(channel, user = null, member = member, server = server)
        assertEquals(0L, perms and PermissionBit.SendMessage.value)
        assertTrue(perms and PermissionBit.ViewChannel.value != 0L)
    }

    @Test
    fun step5_channelOverridesApplyInOrder() {
        val server = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value or PermissionBit.SendMessage.value
        )
        val member = createMember(userId = "user_1")
        val channel = createChannel(
            channelId = "chan_1",
            userPerms = mapOf("user_1" to PermissionDescription(a = PermissionBit.ManageMessages.value, d = PermissionBit.SendMessage.value))
        )

        val perms = Roles.permissionFor(channel, user = null, member = member, server = server)
        assertTrue(perms and PermissionBit.ManageMessages.value != 0L)
        assertEquals(0L, perms and PermissionBit.SendMessage.value)
    }

    @Test
    fun step6_viewChannelDeniedReturnsZero() {
        val server = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value or PermissionBit.SendMessage.value
        )
        val member = createMember(userId = "user_1")
        val channel = createChannel(
            channelId = "chan_1",
            userPerms = mapOf("user_1" to PermissionDescription(a = PermissionBit.ManageMessages.value, d = PermissionBit.ViewChannel.value))
        )

        val perms = Roles.permissionFor(channel, user = null, member = member, server = server)
        assertEquals(0L, perms)
    }

    @Test
    fun hierarchy_cannotManageHigherOrEqualRole() {
        val roleMod = Role(name = "Mod", rank = 5.0, permissions = PermissionDescription(0L, 0L))
        val roleAdmin = Role(name = "Admin", rank = 1.0, permissions = PermissionDescription(0L, 0L))
        val server = createServer(
            ownerId = "owner_1",
            roles = mapOf("mod" to roleMod, "admin" to roleAdmin)
        )
        val memberMod = createMember(userId = "mod_user", roles = listOf("mod"))

        // Mod (rank 5.0) cannot edit Admin (rank 1.0) or Mod (rank 5.0)
        var thrown = false
        try {
            HierarchyChecks.checkCanManageRole(server, "mod_user", roleAdmin, memberMod)
        } catch (e: Exception) {
            thrown = true
        }
        assertTrue(thrown)

        thrown = false
        try {
            HierarchyChecks.checkCanManageRole(server, "mod_user", roleMod, memberMod)
        } catch (e: Exception) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun hierarchy_cannotEscalatePrivileges() {
        val server = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value
        )
        val member = createMember(userId = "user_1")

        // Member only has ViewChannel, trying to grant ManageServer must fail
        var thrown = false
        try {
            HierarchyChecks.checkNoPrivilegeEscalation(
                server = server,
                caller = member,
                requestedAllow = PermissionBit.ManageServer.value,
                requestedDeny = 0L
            )
        } catch (e: Exception) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun hierarchy_canMoveChannelRequiresManageChannelOnBothCategories() {
        val catSource = Category(
            id = "cat_src",
            title = "Source",
            channels = listOf("chan_1"),
            userPermissions = mapOf("user_1" to PermissionDescription(a = PermissionBit.ManageChannel.value, d = 0L))
        )
        val catTargetNoPerm = Category(
            id = "cat_tgt",
            title = "Target",
            channels = emptyList(),
            userPermissions = mapOf("user_1" to PermissionDescription(a = 0L, d = PermissionBit.ManageChannel.value))
        )
        val server = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value,
            categories = listOf(catSource, catTargetNoPerm)
        )
        val member = createMember(userId = "user_1")

        var thrown = false
        try {
            HierarchyChecks.checkCanMoveChannel(
                server = server,
                actorUserId = "user_1",
                sourceCategoryId = "cat_src",
                targetCategoryId = "cat_tgt",
                callerMember = member
            )
        } catch (e: Exception) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun matrix_createChannel_requiresManageChannel() {
        val server = createServer(ownerId = "owner_1", defaultPerms = PermissionBit.ViewChannel.value)
        val memberNoPerm = createMember(userId = "user_no_perm")
        val memberWithPerm = createMember(userId = "user_with_perm")
        val catWithOverride = Category(
            id = "cat_1",
            title = "Text Channels",
            channels = emptyList(),
            userPermissions = mapOf("user_with_perm" to PermissionDescription(a = PermissionBit.ManageChannel.value, d = 0L))
        )
        val serverWithCat = server.copy(categories = listOf(catWithOverride))

        var thrown = false
        try {
            HierarchyChecks.checkCanManageChannel(serverWithCat, "user_no_perm", "cat_1", memberNoPerm)
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue(thrown)

        // With permission on category
        HierarchyChecks.checkCanManageChannel(serverWithCat, "user_with_perm", "cat_1", memberWithPerm)
    }

    @Test
    fun matrix_createCategory_requiresManageChannelAtServerLevel() {
        val serverNoPerm = createServer(ownerId = "owner_1", defaultPerms = PermissionBit.ViewChannel.value)
        val member = createMember(userId = "user_1")
        var thrown = false
        try {
            HierarchyChecks.checkCanManageChannel(serverNoPerm, "user_1", null, member)
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue(thrown)

        val serverWithPerm = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value or PermissionBit.ManageChannel.value
        )
        HierarchyChecks.checkCanManageChannel(serverWithPerm, "user_1", null, member)
    }

    @Test
    fun matrix_renameOrDeleteChannel_requiresManageChannelOnChannel() {
        val server = createServer(ownerId = "owner_1", defaultPerms = PermissionBit.ViewChannel.value)
        val member = createMember(userId = "user_1")
        val channelDenied = createChannel(
            channelId = "c_denied",
            userPerms = mapOf("user_1" to PermissionDescription(a = 0L, d = PermissionBit.ManageChannel.value))
        )
        val channelAllowed = createChannel(
            channelId = "c_allowed",
            userPerms = mapOf("user_1" to PermissionDescription(a = PermissionBit.ManageChannel.value, d = 0L))
        )

        var thrown = false
        try {
            HierarchyChecks.checkCanManageChannelOnChannel(server, channelDenied, "user_1", member)
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue(thrown)

        HierarchyChecks.checkCanManageChannelOnChannel(server, channelAllowed, "user_1", member)
    }

    @Test
    fun matrix_renameOrDeleteCategory_requiresManageChannelAtServerLevel() {
        val server = createServer(ownerId = "owner_1", defaultPerms = PermissionBit.ViewChannel.value)
        val member = createMember(userId = "user_1")

        var thrown = false
        try {
            HierarchyChecks.checkCanManageChannel(server, "user_1", null, member)
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue(thrown)

        val serverAllowed = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value or PermissionBit.ManageChannel.value
        )
        HierarchyChecks.checkCanManageChannel(serverAllowed, "user_1", null, member)
    }

    @Test
    fun matrix_batchMove_crossServerChannelId_rejectedIDOR() {
        val server = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ManageChannel.value,
            channels = listOf("chan_1", "chan_2")
        )
        val member = createMember(userId = "user_1")
        val requestedCategories = listOf(
            Category(id = "cat_1", title = "Cat 1", channels = listOf("chan_1", "foreign_channel_from_other_server"))
        )

        var thrown = false
        try {
            HierarchyChecks.validateCategoriesPatch(server, "user_1", requestedCategories, member)
        } catch (e: SecurityException) {
            thrown = true
            assertTrue(e.message?.contains("IDOR") == true)
        }
        assertTrue(thrown)
    }

    @Test
    fun matrix_batchMove_oneUnauthorizedItemRejectsEntireBatch() {
        val cat1 = Category(
            id = "cat_1",
            title = "Cat 1",
            channels = listOf("chan_1"),
            userPermissions = mapOf("user_1" to PermissionDescription(a = PermissionBit.ManageChannel.value, d = 0L))
        )
        val cat2 = Category(
            id = "cat_2",
            title = "Cat 2",
            channels = listOf("chan_2"),
            userPermissions = mapOf("user_1" to PermissionDescription(a = 0L, d = PermissionBit.ManageChannel.value))
        )
        val cat3 = Category(
            id = "cat_3",
            title = "Cat 3",
            channels = emptyList(),
            userPermissions = mapOf("user_1" to PermissionDescription(a = PermissionBit.ManageChannel.value, d = 0L))
        )
        val server = createServer(
            ownerId = "owner_1",
            defaultPerms = PermissionBit.ViewChannel.value,
            channels = listOf("chan_1", "chan_2"),
            categories = listOf(cat1, cat2, cat3)
        )
        val member = createMember(userId = "user_1")

        // Moving chan_1 (allowed) to cat3 AND chan_2 (denied) to cat3 in the same batch
        val requestedCategories = listOf(
            cat1.copy(channels = emptyList()),
            cat2.copy(channels = emptyList()),
            cat3.copy(channels = listOf("chan_1", "chan_2"))
        )

        var thrown = false
        try {
            HierarchyChecks.validateCategoriesPatch(server, "user_1", requestedCategories, member)
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun matrix_editPermissionOverrides_requiresManagePermissions() {
        val server = createServer(ownerId = "owner_1", defaultPerms = PermissionBit.ViewChannel.value)
        val member = createMember(userId = "user_1")
        val channelNoPerm = createChannel(channelId = "c_1")
        val channelWithPerm = createChannel(
            channelId = "c_2",
            userPerms = mapOf("user_1" to PermissionDescription(a = PermissionBit.ManagePermissions.value, d = 0L))
        )

        var thrown = false
        try {
            HierarchyChecks.checkCanManagePermissions(server, channelNoPerm, "user_1", member)
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue(thrown)

        HierarchyChecks.checkCanManagePermissions(server, channelWithPerm, "user_1", member)
    }

    @Test
    fun matrix_manageRoles_requiresManageRoleAndRankHierarchy() {
        val server = createServer(ownerId = "owner_1", defaultPerms = PermissionBit.ViewChannel.value)
        val memberNoPerm = createMember(userId = "user_no_perm")
        var thrown = false
        try {
            HierarchyChecks.checkCanManageRoles(server, "user_no_perm", memberNoPerm)
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue(thrown)

        val roleAdmin = Role(name = "Admin", rank = 1.0, permissions = PermissionDescription(0L, 0L))
        val roleMod = Role(name = "Mod", rank = 5.0, permissions = PermissionDescription(0L, 0L))
        val serverWithRoles = server.copy(
            defaultPermissions = PermissionBit.ViewChannel.value or PermissionBit.ManageRole.value,
            roles = mapOf("admin" to roleAdmin, "mod" to roleMod)
        )
        val memberMod = createMember(userId = "mod_user", roles = listOf("mod"))

        // Mod has ManageRole bit, but cannot assign or manage equal or higher role
        thrown = false
        try {
            HierarchyChecks.checkCanManageRole(serverWithRoles, "mod_user", roleAdmin, memberMod)
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue(thrown)

        // Assigning role higher or equal also fails
        thrown = false
        try {
            HierarchyChecks.checkCanAssignRole(serverWithRoles, "mod_user", "admin")
        } catch (e: SecurityException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
