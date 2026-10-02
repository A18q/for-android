package chat.stoat.sheets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.channel.leaveDeleteOrCloseChannel
import chat.stoat.api.routes.server.patchServer
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.SheetButton
import chat.stoat.internals.Platform
import chat.stoat.internals.server.UncategorisedChannelSectionId
import chat.stoat.internals.server.serverChannelSections
import chat.stoat.internals.server.toServerCategories
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TokenBlurple = Color(0xFF5865F2)
private val TokenDanger = Color(0xFFED4245)
private val TokenMuted = Color(0xFF949BA4)
private val TokenHeader = Color(0xFFF2F3F5)
private val SectionBg = Color(0xFF2B2D31)

@Composable
fun ChannelContextSheet(
    channelId: String,
    onHideSheet: suspend () -> Unit
) {
    val channel = StoatAPI.channelCache[channelId]
    if (channel == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val server = channel.server?.let { StoatAPI.serverCache[it] }
    val selfId = StoatAPI.selfId
    val isOwner = server != null && selfId != null && server.owner == selfId
    val member = if (server != null && selfId != null) StoatAPI.members.getMember(server.id.orEmpty(), selfId) else null
    val channelPerms = if (server != null && member != null) Roles.permissionFor(channel, user = null, member = member, server = server) else 0L
    val canManageChannel = isOwner || channelPerms.hasPermission(PermissionBit.ManageChannel) || channelPerms.hasPermission(PermissionBit.ManageServer)
    val canManagePermissions = isOwner || channelPerms.hasPermission(PermissionBit.ManagePermissions) || channelPerms.hasPermission(PermissionBit.ManageServer)

    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showMoveDialog && server != null) {
        val sections = remember(server) { serverChannelSections(server) }
        var selectedSectionId by remember {
            mutableStateOf(
                sections.firstOrNull { it.channelIds.contains(channelId) }?.id ?: UncategorisedChannelSectionId
            )
        }

        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            containerColor = SectionBg,
            title = {
                Text("Move Channel to Category", color = TokenHeader, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sections.forEach { section ->
                        val isSelected = section.id == selectedSectionId
                        val title = if (section.id == UncategorisedChannelSectionId) "Uncategorized" else section.title.orEmpty()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSectionId = section.id }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedSectionId = section.id },
                                colors = RadioButtonDefaults.colors(selectedColor = TokenBlurple)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = title,
                                color = if (isSelected) TokenHeader else TokenMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val updated = sections.map { section ->
                                if (section.id == selectedSectionId) {
                                    if (channelId !in section.channelIds) section.copy(channelIds = section.channelIds + channelId) else section
                                } else {
                                    section.copy(channelIds = section.channelIds - channelId)
                                }
                            }
                            patchServer(server.id!!, categories = updated.toServerCategories())
                            showMoveDialog = false
                            onHideSheet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokenBlurple)
                ) {
                    Text("Move", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) {
                    Text("Cancel", color = TokenMuted)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SectionBg,
            title = {
                Text("Delete Channel?", color = TokenHeader, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete #${channel.name ?: "this channel"}? This cannot be undone.",
                    color = TokenMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            leaveDeleteOrCloseChannel(channelId)
                            showDeleteConfirm = false
                            onHideSheet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TokenDanger)
                ) {
                    Text("Delete Channel", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TokenMuted)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SheetButton(
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.channel_context_sheet_actions_copy_id),
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_identifier_copy_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                if (channel.id == null) return@SheetButton

                clipboardManager.setText(AnnotatedString(channel.id!!))

                if (Platform.needsShowClipboardNotification()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.channel_context_sheet_actions_copy_id_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                coroutineScope.launch {
                    onHideSheet()
                }
            }
        )

        SheetButton(
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.channel_context_sheet_actions_mark_read),
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mark_chat_read_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                coroutineScope.launch {
                    channel.lastMessageID?.let {
                        StoatAPI.unreads.markAsRead(channelId, it, sync = true)
                    }
                    onHideSheet()
                }
            }
        )

        if (server != null && canManageChannel) {
            HorizontalDivider(
                color = Color(0xFF3A3C42),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            SheetButton(
                headlineContent = {
                    Text("Edit Channel")
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_24dp),
                        contentDescription = null
                    )
                },
                onClick = {
                    coroutineScope.launch {
                        onHideSheet()
                        delay(100)
                        ActionChannel.send(Action.TopNavigate("settings/channel/$channelId"))
                    }
                }
            )

            if (canManagePermissions) {
                SheetButton(
                    headlineContent = {
                        Text("Permissions")
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock_24dp),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        coroutineScope.launch {
                            onHideSheet()
                            delay(100)
                            ActionChannel.send(Action.TopNavigate("settings/channel/$channelId/permissions"))
                        }
                    }
                )
            }

            SheetButton(
                headlineContent = {
                    Text("Move to Category")
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder_24dp),
                        contentDescription = null
                    )
                },
                onClick = {
                    showMoveDialog = true
                }
            )

            SheetButton(
                headlineContent = {
                    Text("Delete Channel", color = TokenDanger)
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_24dp),
                        contentDescription = null,
                        tint = TokenDanger
                    )
                },
                onClick = {
                    showDeleteConfirm = true
                }
            )
        }
    }
}
