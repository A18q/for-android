package chat.stoat.screens.settings.server

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.routes.server.createRole
import chat.stoat.api.routes.server.deleteRole
import chat.stoat.api.routes.server.editRole
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import kotlinx.coroutines.launch

// ─── Discord Mobile Authentic Color Tokens ───
private val DiscordDarkBg = Color(0xFF1E1F22)
private val DiscordCardBg = Color(0xFF2B2D31)
private val DiscordInsetBg = Color(0xFF232428)
private val DiscordHeader = Color(0xFFF2F3F5)
private val DiscordTextNormal = Color(0xFFDBDEE1)
private val DiscordTextMuted = Color(0xFF949BA4)
private val DiscordBlurple = Color(0xFF5865F2)
private val DiscordDanger = Color(0xFFED4245)
private val DiscordDivider = Color(0xFF35373C)

// Discord Classic Role Preset Palette
private val DiscordRolePresets = listOf(
    "#99AAB5", // Default Grey
    "#1ABC9C", // Aqua
    "#57F287", // Green
    "#3498DB", // Blue
    "#9B59B6", // Purple
    "#EB459E", // Fuchsia
    "#F1C40F", // Gold
    "#E67E22", // Orange
    "#ED4245", // Red
    "#34495E"  // Navy
)

private fun safeParseColor(hex: String?, fallback: Color = Color(0xFF99AAB5)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerRolesScreen(
    navController: NavController,
    serverId: String
) {
    val server = StoatAPI.serverCache[serverId]
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newRoleName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    val rolesMap = server?.roles ?: emptyMap()
    val sortedRoles = remember(rolesMap, searchQuery) {
        rolesMap.entries
            .filter { (id, role) ->
                searchQuery.isBlank() || (role.name ?: id).contains(searchQuery, ignoreCase = true)
            }
            .sortedByDescending { it.value.rank ?: 0.0 }
    }

    Scaffold(
        containerColor = DiscordDarkBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DiscordDarkBg,
                    titleContentColor = DiscordHeader,
                    navigationIconContentColor = DiscordHeader,
                    actionIconContentColor = DiscordBlurple
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Roles",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    // Discord "+" button to create role
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
                            contentDescription = "Create Role",
                            tint = DiscordBlurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar (Discord rounded pill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search roles",
                            color = DiscordTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_24dp),
                            contentDescription = null,
                            tint = DiscordTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DiscordCardBg,
                        unfocusedContainerColor = DiscordCardBg,
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = DiscordHeader,
                        unfocusedTextColor = DiscordHeader,
                        cursorColor = DiscordBlurple
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }

            // Category Subheader: "ROLES — {count}"
            Text(
                text = "ROLES — ${rolesMap.size}",
                color = DiscordTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
            )

            // Roles List Card Container
            if (sortedRoles.isEmpty() && rolesMap.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shield_lock_24dp),
                            contentDescription = null,
                            tint = DiscordTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No roles created yet",
                            color = DiscordTextMuted,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Create Role", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DiscordCardBg)
                ) {
                    items(sortedRoles, key = { it.key }) { (roleId, role) ->
                        RoleListItem(
                            roleId = roleId,
                            role = role,
                            onClick = { selectedRoleId = roleId }
                        )
                        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp)
                    }

                    // Default @everyone role item at the end
                    item {
                        EveryoneRoleListItem(
                            serverDefaultPermissions = server?.defaultPermissions ?: 0L
                        )
                    }
                }
            }
        }
    }

    // ─── Create Role Dialog ───
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DiscordCardBg,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = "Create Role",
                    color = DiscordHeader,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ROLE NAME",
                        color = DiscordTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = newRoleName,
                        onValueChange = { newRoleName = it },
                        placeholder = { Text("new role", color = DiscordTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DiscordInsetBg,
                            unfocusedContainerColor = DiscordInsetBg,
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = DiscordHeader,
                            unfocusedTextColor = DiscordHeader,
                            cursorColor = DiscordBlurple
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRoleName.isNotBlank() && !isCreating) {
                            isCreating = true
                            scope.launch {
                                try {
                                    val created = createRole(serverId, newRoleName.trim())
                                    newRoleName = ""
                                    showCreateDialog = false
                                    selectedRoleId = created.id
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isCreating = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                    shape = RoundedCornerShape(8.dp),
                    enabled = newRoleName.isNotBlank() && !isCreating
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Create", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = DiscordTextMuted)
                }
            }
        )
    }

    // ─── Role Detail Editor Sheet ───
    if (selectedRoleId != null) {
        val roleToEdit = rolesMap[selectedRoleId]
        if (roleToEdit != null) {
            RoleEditorBottomSheet(
                serverId = serverId,
                roleId = selectedRoleId!!,
                role = roleToEdit,
                onDismiss = { selectedRoleId = null }
            )
        }
    }
}

@Composable
private fun RoleListItem(
    roleId: String,
    role: Role,
    onClick: () -> Unit
) {
    val roleColor = safeParseColor(role.colour)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color dot circle
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(roleColor)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = role.name ?: "Unnamed Role",
                color = DiscordHeader,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (role.hoist == true) {
                Text(
                    text = "Hoisted",
                    color = DiscordTextMuted,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back_24dp),
            contentDescription = null,
            tint = DiscordTextMuted,
            modifier = Modifier
                .size(16.dp)
                .padding(end = 4.dp)
        )
    }
}

@Composable
private fun EveryoneRoleListItem(
    serverDefaultPermissions: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFF99AAB5))
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "@everyone",
                color = DiscordHeader,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Default permissions for everyone",
                color = DiscordTextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RoleEditorBottomSheet(
    serverId: String,
    roleId: String,
    role: Role,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var roleName by remember { mutableStateOf(role.name ?: "") }
    var roleColorHex by remember { mutableStateOf(role.colour ?: "") }
    var isHoisted by remember { mutableStateOf(role.hoist ?: false) }
    var allowedPermissions by remember { mutableLongStateOf(role.permissions?.a ?: 0L) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DiscordDarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Role: ${role.name ?: "Role"}",
                    color = DiscordHeader,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Save Button (Blurple pill)
                Button(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            scope.launch {
                                try {
                                    editRole(
                                        serverId = serverId,
                                        roleId = roleId,
                                        name = roleName.takeIf { it.isNotBlank() },
                                        colour = roleColorHex.takeIf { it.isNotBlank() },
                                        hoist = isHoisted,
                                        permissions = PermissionDescription(a = allowedPermissions, d = 0L)
                                    )
                                    Toast.makeText(context, "Role updated", Toast.LENGTH_SHORT).show()
                                    sheetState.hide()
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Tabs: Display vs Permissions
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DiscordDarkBg,
                contentColor = DiscordBlurple
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Display",
                            color = if (selectedTab == 0) DiscordHeader else DiscordTextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Permissions",
                            color = if (selectedTab == 1) DiscordHeader else DiscordTextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }

            // Content per tab
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (selectedTab == 0) {
                    // ─── DISPLAY TAB ───
                    Text(
                        text = "ROLE NAME",
                        color = DiscordTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = roleName,
                        onValueChange = { roleName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DiscordInsetBg,
                            unfocusedContainerColor = DiscordInsetBg,
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = DiscordHeader,
                            unfocusedTextColor = DiscordHeader,
                            cursorColor = DiscordBlurple
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "ROLE COLOR",
                        color = DiscordTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))

                    // Discord 10-Color Swatches
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DiscordRolePresets.forEach { hex ->
                            val color = safeParseColor(hex)
                            val isSelected = roleColorHex.equals(hex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { roleColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check_24dp),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Custom hex code field
                    OutlinedTextField(
                        value = roleColorHex,
                        onValueChange = { roleColorHex = it },
                        label = { Text("Custom Hex Color (#RRGGBB)", color = DiscordTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DiscordInsetBg,
                            unfocusedContainerColor = DiscordInsetBg,
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = DiscordHeader,
                            unfocusedTextColor = DiscordHeader,
                            cursorColor = DiscordBlurple
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "ROLE SETTINGS",
                        color = DiscordTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))

                    // Hoist Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DiscordCardBg)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Display role members separately",
                                color = DiscordHeader,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Members with this role will appear in their own section in the member list.",
                                color = DiscordTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isHoisted,
                            onCheckedChange = { isHoisted = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DiscordBlurple,
                                uncheckedThumbColor = DiscordTextMuted,
                                uncheckedTrackColor = DiscordInsetBg
                            )
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Danger Zone: Delete Role
                    Text(
                        text = "DANGER ZONE",
                        color = DiscordDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordDanger),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Role", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // ─── PERMISSIONS TAB ───
                    Text(
                        text = "SERVER PERMISSIONS",
                        color = DiscordTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DiscordCardBg)
                    ) {
                        PermissionToggleItem(
                            title = "Administrator",
                            description = "Members with this permission have every permission and can bypass channel specific permissions.",
                            isGranted = (allowedPermissions and PermissionBit.ManageServer.value) != 0L,
                            onToggle = { granted ->
                                allowedPermissions = if (granted) {
                                    allowedPermissions or PermissionBit.ManageServer.value
                                } else {
                                    allowedPermissions and PermissionBit.ManageServer.value.inv()
                                }
                            }
                        )
                        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp)

                        PermissionToggleItem(
                            title = "Manage Channels",
                            description = "Allows members to create, edit, or delete channels.",
                            isGranted = (allowedPermissions and PermissionBit.ManageChannel.value) != 0L,
                            onToggle = { granted ->
                                allowedPermissions = if (granted) {
                                    allowedPermissions or PermissionBit.ManageChannel.value
                                } else {
                                    allowedPermissions and PermissionBit.ManageChannel.value.inv()
                                }
                            }
                        )
                        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp)

                        PermissionToggleItem(
                            title = "Manage Roles",
                            description = "Allows members to create new roles and edit or delete roles lower than this one.",
                            isGranted = (allowedPermissions and PermissionBit.ManageRole.value) != 0L,
                            onToggle = { granted ->
                                allowedPermissions = if (granted) {
                                    allowedPermissions or PermissionBit.ManageRole.value
                                } else {
                                    allowedPermissions and PermissionBit.ManageRole.value.inv()
                                }
                            }
                        )
                        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp)

                        PermissionToggleItem(
                            title = "Kick Members",
                            description = "Allows members to remove other members from this server.",
                            isGranted = (allowedPermissions and PermissionBit.KickMembers.value) != 0L,
                            onToggle = { granted ->
                                allowedPermissions = if (granted) {
                                    allowedPermissions or PermissionBit.KickMembers.value
                                } else {
                                    allowedPermissions and PermissionBit.KickMembers.value.inv()
                                }
                            }
                        )
                        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp)

                        PermissionToggleItem(
                            title = "Ban Members",
                            description = "Allows members to permanently ban other members from this server.",
                            isGranted = (allowedPermissions and PermissionBit.BanMembers.value) != 0L,
                            onToggle = { granted ->
                                allowedPermissions = if (granted) {
                                    allowedPermissions or PermissionBit.BanMembers.value
                                } else {
                                    allowedPermissions and PermissionBit.BanMembers.value.inv()
                                }
                            }
                        )
                        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp)

                        PermissionToggleItem(
                            title = "Send Messages",
                            description = "Allows members to send messages in text channels.",
                            isGranted = (allowedPermissions and PermissionBit.SendMessage.value) != 0L,
                            onToggle = { granted ->
                                allowedPermissions = if (granted) {
                                    allowedPermissions or PermissionBit.SendMessage.value
                                } else {
                                    allowedPermissions and PermissionBit.SendMessage.value.inv()
                                }
                            }
                        )
                        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp)

                        PermissionToggleItem(
                            title = "Manage Messages",
                            description = "Allows members to delete messages by other members or pin messages.",
                            isGranted = (allowedPermissions and PermissionBit.ManageMessages.value) != 0L,
                            onToggle = { granted ->
                                allowedPermissions = if (granted) {
                                    allowedPermissions or PermissionBit.ManageMessages.value
                                } else {
                                    allowedPermissions and PermissionBit.ManageMessages.value.inv()
                                }
                            }
                        )
                        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp)

                        PermissionToggleItem(
                            title = "Mention @everyone",
                            description = "Allows members to use @everyone or @here in channels.",
                            isGranted = (allowedPermissions and PermissionBit.MentionEveryone.value) != 0L,
                            onToggle = { granted ->
                                allowedPermissions = if (granted) {
                                    allowedPermissions or PermissionBit.MentionEveryone.value
                                } else {
                                    allowedPermissions and PermissionBit.MentionEveryone.value.inv()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Alert
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DiscordCardBg,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = "Delete Role",
                    color = DiscordHeader,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${role.name ?: "Role"}'? This action cannot be undone.",
                    color = DiscordTextNormal
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                deleteRole(serverId, roleId)
                                Toast.makeText(context, "Role deleted", Toast.LENGTH_SHORT).show()
                                showDeleteConfirm = false
                                sheetState.hide()
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordDanger)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = DiscordTextMuted)
                }
            }
        )
    }
}

@Composable
private fun PermissionToggleItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isGranted) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DiscordHeader,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = DiscordTextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = isGranted,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DiscordBlurple,
                uncheckedThumbColor = DiscordTextMuted,
                uncheckedTrackColor = DiscordInsetBg
            )
        )
    }
}
