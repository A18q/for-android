package chat.stoat.sheets

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.server.createChannelInServer
import chat.stoat.api.routes.server.leaveOrDeleteServer
import chat.stoat.api.settings.ServerFolders
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.SheetButton
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.composables.screens.settings.ServerOverview
import chat.stoat.core.model.data.STOAT_WEB_APP
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.internals.Platform
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.availableServerSettingsOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Discord Modern Surface Tokens — match StoatUserCapsule
private val SheetBg = Color(0xFF1E1F22)
private val SectionBg = Color(0xFF2B2D31)
private val SectionItemBg = Color(0xFF232428)
private val TokenHeader = Color(0xFFF2F3F5)
private val TokenMuted = Color(0xFF949BA4)
private val TokenDanger = Color(0xFFF23F43)
private val TokenBlurple = Color(0xFF5865F2)

@Composable
fun ServerContextSheet(
    serverId: String,
    onReportServer: () -> Unit,
    onPickFolder: suspend () -> Unit = {},
    onHideSheet: suspend () -> Unit,
    onNavigateToRoles: (() -> Unit)? = null,
) {
    val server = StoatAPI.serverCache[serverId] ?: return
    val isOwner = server.owner == StoatAPI.selfId

    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val permissions by rememberServerPermissions(serverId)
    val serverSettingsOptions = permissions?.let {
        availableServerSettingsOptions(
            permissions = it,
            isOwner = server.owner == StoatAPI.selfId,
        )
    }.orEmpty()

    var showLeaveConfirmation by remember { mutableStateOf(false) }
    val currentFolder = ServerFolders.folderOf(serverId)
    val newFolderName = stringResource(R.string.server_folder_default_name)
    var leaveSilently by remember { mutableStateOf(false) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var newChannelName by remember { mutableStateOf("") }
    var newChannelType by remember { mutableStateOf("Text") }
    var settingsExpanded by remember { mutableStateOf(false) }
    val settingsChevron by animateFloatAsState(
        targetValue = if (settingsExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "chevron"
    )

    // — Leave confirmation dialog —
    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmation = false },
            containerColor = SectionBg,
            title = {
                Text(
                    text = "Leave '${server.name ?: "Server"}'?",
                    color = TokenHeader,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.server_context_sheet_actions_leave_confirm_eyebrow),
                        color = TokenMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = leaveSilently, onCheckedChange = { leaveSilently = it })
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.server_context_sheet_actions_leave_silently),
                            color = TokenMuted
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            onHideSheet()
                            leaveOrDeleteServer(serverId, leaveSilently)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokenDanger)
                ) {
                    Text(stringResource(R.string.server_context_sheet_actions_leave_confirm_yes), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmation = false }) {
                    Text(stringResource(R.string.server_context_sheet_actions_leave_confirm_no), color = TokenBlurple)
                }
            }
        )
    }

    // — Create Channel dialog —
    if (showCreateChannelDialog) {
        AlertDialog(
            onDismissRequest = { showCreateChannelDialog = false },
            containerColor = SectionBg,
            title = {
                Text("Create Channel", color = TokenHeader, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Channel type toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SectionItemBg),
                    ) {
                        listOf("Text" to R.drawable.ic_tag_24dp, "Voice" to R.drawable.ic_volume_up_24dp).forEach { (type, icon) ->
                            val selected = newChannelType == type
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) TokenBlurple else Color.Transparent)
                                    .clickable { newChannelType = type }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    tint = if (selected) Color.White else TokenMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = type,
                                    color = if (selected) Color.White else TokenMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newChannelName,
                        onValueChange = { newChannelName = it },
                        label = { Text("Channel Name", color = TokenMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TokenHeader,
                            unfocusedTextColor = TokenHeader,
                            focusedBorderColor = TokenBlurple,
                            unfocusedBorderColor = TokenMuted,
                            cursorColor = TokenBlurple
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChannelName.isNotBlank()) {
                            coroutineScope.launch {
                                createChannelInServer(serverId, newChannelName.trim(), newChannelType)
                                newChannelName = ""
                                showCreateChannelDialog = false
                                onHideSheet()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokenBlurple),
                    enabled = newChannelName.isNotBlank()
                ) {
                    Text("Create Channel", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateChannelDialog = false }) {
                    Text("Cancel", color = TokenMuted)
                }
            }
        )
    }

    // — Sheet body —
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .background(SheetBg)
    ) {
        // Server header card
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
        ) {
            ServerOverview(server)

            if (!server.description.isNullOrBlank()) {
                SelectionContainer {
                    ChatMarkdown(
                        content = server.description!!,
                        serverId = serverId,
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF3A3C42), thickness = 0.5.dp)
        Spacer(Modifier.height(4.dp))

        // ─── INVITE PEOPLE ───
        SheetButton(
            leadingContent = {
                Icon(painter = painterResource(R.drawable.ic_group_add_24dp), contentDescription = null)
            },
            headlineContent = { Text("Invite People") },
            onClick = {
                coroutineScope.launch {
                    // Fire InviteDialog via the channel — navigate to first text channel
                    val firstChannel = server.channels
                        ?.mapNotNull { StoatAPI.channelCache[it] }
                        ?.firstOrNull { it.channelType == chat.stoat.core.model.schemas.ChannelType.TextChannel }
                    if (firstChannel?.id != null) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "$STOAT_WEB_APP/server/$serverId/channel/${firstChannel.id}".toUri())
                        )
                    }
                    onHideSheet()
                }
            },
            special = true
        )

        // ─── NOTIFICATION SETTINGS ───
        SheetButton(
            leadingContent = {
                Icon(painter = painterResource(R.drawable.ic_notifications_24dp), contentDescription = null)
            },
            headlineContent = { Text("Notification Settings") },
            onClick = {
                coroutineScope.launch {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, "$STOAT_WEB_APP/server/$serverId/settings/notifications".toUri())
                    )
                    onHideSheet()
                }
            }
        )

        // ─── CREATE CHANNEL ───
        SheetButton(
            leadingContent = {
                Icon(painter = painterResource(R.drawable.ic_add_24dp), contentDescription = null)
            },
            headlineContent = { Text("Create Channel") },
            onClick = { showCreateChannelDialog = true }
        )

        HorizontalDivider(color = Color(0xFF3A3C42), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        HorizontalDivider(color = Color(0xFF3A3C42), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        // ─── MARK AS READ ───
        SheetButton(
            leadingContent = {
                Icon(painter = painterResource(R.drawable.ic_mark_chat_read_24dp), contentDescription = null)
            },
            headlineContent = { Text(stringResource(R.string.server_context_sheet_actions_mark_read)) },
            onClick = {
                coroutineScope.launch {
                    server.id?.let { StoatAPI.unreads.markServerAsRead(it, sync = true) }
                    onHideSheet()
                }
            }
        )

        // ─── COPY SERVER ID ───
        SheetButton(
            leadingContent = {
                Icon(painter = painterResource(R.drawable.ic_identifier_copy_24dp), contentDescription = null)
            },
            headlineContent = { Text(stringResource(R.string.server_context_sheet_actions_copy_id)) },
            onClick = {
                val id = server.id ?: return@SheetButton
                coroutineScope.launch {
                    clipboard.setClipEntry(
                        android.content.ClipData.newPlainText("Server ID", id).toClipEntry()
                    )
                    if (Platform.needsShowClipboardNotification()) {
                        Toast.makeText(context, context.getString(R.string.server_context_sheet_actions_copy_id_copied), Toast.LENGTH_SHORT).show()
                    }
                    onHideSheet()
                }
            }
        )

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(stringResource(R.string.server_context_sheet_actions_add_to_folder))
            },
            onClick = {
                if (ServerFolders.folders.isEmpty()) {
                    coroutineScope.launch {
                        onHideSheet()
                        ServerFolders.create(newFolderName, listOf(serverId))
                    }
                } else {
                    coroutineScope.launch { onPickFolder() }
                }
            }
        )

        if (currentFolder != null) {
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder_off_24dp),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(stringResource(R.string.server_context_sheet_actions_remove_from_folder))
                },
                onClick = {
                    coroutineScope.launch {
                        onHideSheet()
                        ServerFolders.removeServer(serverId)
                    }
                }
            )
        }

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_id_card_24dp),
                    contentDescription = null,
                )
            },
            headlineContent = {
                Text(stringResource(R.string.server_identity))
            },
            onClick = {
                coroutineScope.launch {
                    onHideSheet()
                }
                coroutineScope.launch {
                    delay(100.milliseconds)
                    ActionChannel.send(
                        Action.TopNavigate("settings/server/$serverId/identity")
                    )
                }
            },
        )

        if (serverSettingsOptions.isNotEmpty()) {
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings_24dp),
                        contentDescription = null,
                    )
                },
                headlineContent = {
                    Text(stringResource(R.string.server_settings))
                },
                onClick = {
                    coroutineScope.launch {
                        onHideSheet()
                    }
                    coroutineScope.launch {
                        delay(100.milliseconds)
                        ActionChannel.send(Action.TopNavigate("settings/server/$serverId"))
                    }
                },
            )
        }

        // ─── NON-OWNER: REPORT + LEAVE ───
        if (!isOwner) {
            HorizontalDivider(color = Color(0xFF3A3C42), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            SheetButton(
                leadingContent = {
                    Icon(painter = painterResource(R.drawable.ic_report_24dp), contentDescription = null)
                },
                headlineContent = { Text(stringResource(R.string.server_context_sheet_actions_report)) },
                dangerous = true,
                onClick = { onReportServer() }
            )

            SheetButton(
                leadingContent = {
                    Icon(painter = painterResource(R.drawable.ic_door_open_24dp), contentDescription = null)
                },
                headlineContent = { Text(stringResource(R.string.server_context_sheet_actions_leave)) },
                dangerous = true,
                onClick = { showLeaveConfirmation = true }
            )
        }

        // ─── OWNER: DELETE (danger zone) ───
        if (isOwner) {
            HorizontalDivider(color = Color(0xFF3A3C42), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            SheetButton(
                leadingContent = {
                    Icon(painter = painterResource(R.drawable.ic_delete_24dp), contentDescription = null)
                },
                headlineContent = { Text("Delete Server") },
                supportingContent = { Text("This action is irreversible", color = TokenMuted, fontSize = 12.sp) },
                dangerous = true,
                onClick = { showLeaveConfirmation = true }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ServerSettingSubItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = TokenMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(text = label, color = TokenHeader, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back_24dp),
            contentDescription = null,
            tint = TokenMuted,
            modifier = Modifier
                .size(16.dp)
                .rotate(-90f)
        )
    }
}
