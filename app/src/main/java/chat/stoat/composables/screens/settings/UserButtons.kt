package chat.stoat.composables.screens.settings

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.user.acceptFriendRequest
import chat.stoat.api.routes.user.blockUser
import chat.stoat.api.routes.user.friendUser
import chat.stoat.api.routes.user.openDM
import chat.stoat.api.routes.user.unblockUser
import chat.stoat.api.routes.user.unfriendUser
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.core.model.schemas.User
import chat.stoat.dialogs.MemberModerationAction
import chat.stoat.dialogs.MemberModerationDialog
import chat.stoat.dialogs.memberModerationPermissions
import chat.stoat.internals.Platform
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.serverIdentityCapabilities
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

@Composable
fun UserButtons(
    user: User,
    serverId: String? = null,
    dismissSheet: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboard = LocalClipboardManager.current

    var botEasterEgg by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var moderationAction by remember { mutableStateOf<MemberModerationAction?>(null) }

    val serverPermissions by rememberServerPermissions(serverId.orEmpty())
    val moderationPermissions = serverId?.let {
        memberModerationPermissions(it, user.id)
    }
    val identityCapabilities = user.id?.let { userId ->
        serverId?.let {
            serverIdentityCapabilities(
                targetUserId = userId,
                selfUserId = StoatAPI.selfId,
                permissions = serverPermissions,
            )
        }
    }
    val targetMember = user.id?.let { userId ->
        serverId?.let { StoatAPI.members.getMember(it, userId) }
    }
    val canEditServerIdentity = user.id != StoatAPI.selfId && targetMember != null &&
            (identityCapabilities?.canChangeNickname == true ||
                    identityCapabilities?.canRemoveAvatar == true && targetMember.avatar != null)

    if (serverId != null && moderationAction != null) {
        MemberModerationDialog(
            action = moderationAction!!,
            serverId = serverId,
            user = user,
            dismissUserSheet = dismissSheet,
            onDismiss = { moderationAction = null },
        )
    }

    if (user.id == null) return Row {
        Button(
            onClick = {
                scope.launch {
                    try {
                        friendUser("${user.username}#${user.discriminator}")
                    } catch (e: Exception) {
                        // Button did nothing, but not an error
                        if (e.message == "NoEffect") return@launch

                        // Log all other errors
                        logcat(LogPriority.ERROR) { e.asLog() }
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.user_info_sheet_add_friend))
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (user.relationship) {
            "None" -> {
                if (user.bot == null) {
                    // Giant Discord Add Friend Button
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    friendUser("${user.username}#${user.discriminator}")
                                } catch (e: Exception) {
                                    if (e.message == "NoEffect") return@launch
                                    logcat(LogPriority.ERROR) { e.asLog() }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5865F2),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_person_add_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.user_info_sheet_add_friend),
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }

                    // Small square Message button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(Color(0xFF2B2D31))
                            .clickable {
                                scope.launch {
                                    try {
                                        val dm = openDM(user.id!!)
                                        if (dm.id != null) {
                                            if (StoatAPI.channelCache[dm.id] == null)
                                                StoatAPI.channelCache[dm.id!!] = dm
                                            ActionChannel.send(Action.SwitchChannel(dm.id!!))
                                            dismissSheet()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.user_info_sheet_failed_to_open_dm),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        logcat(LogPriority.ERROR) { e.asLog() }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chat_24dp),
                            contentDescription = stringResource(R.string.user_info_sheet_send_message),
                            tint = Color(0xFFDBDEE1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Bot — wide Message button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(Color(0xFF2B2D31))
                            .clickable {
                                scope.launch {
                                    try {
                                        val dm = openDM(user.id!!)
                                        if (dm.id != null) {
                                            if (StoatAPI.channelCache[dm.id] == null)
                                                StoatAPI.channelCache[dm.id!!] = dm
                                            ActionChannel.send(Action.SwitchChannel(dm.id!!))
                                            dismissSheet()
                                        }
                                    } catch (e: Exception) {
                                        logcat(LogPriority.ERROR) { e.asLog() }
                                    }
                                }
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chat_24dp),
                            contentDescription = stringResource(R.string.user_info_sheet_send_message),
                            tint = Color(0xFFF2F3F5),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Message",
                            color = Color(0xFFF2F3F5),
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }
                }
            }

            "User" -> {
                Button(
                    onClick = {
                        scope.launch {
                            ActionChannel.send(Action.TopNavigate("settings/profile"))
                            // We must now close the bottom sheet,
                            // else we will crash if we try to open this sheet again
                            dismissSheet()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_edit_profile))
                }
            }

            "Friend" -> {
                // Wide Message Button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(Color(0xFF2B2D31))
                        .clickable {
                            scope.launch {
                                try {
                                    val dm = openDM(user.id!!)
                                    if (dm.id != null) {
                                        if (StoatAPI.channelCache[dm.id] == null)
                                            StoatAPI.channelCache[dm.id!!] = dm
                                        ActionChannel.send(Action.SwitchChannel(dm.id!!))
                                        dismissSheet()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.user_info_sheet_failed_to_open_dm),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    logcat(LogPriority.ERROR) { e.asLog() }
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chat_24dp),
                        contentDescription = stringResource(R.string.user_info_sheet_send_message),
                        tint = Color(0xFFF2F3F5),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Message",
                        color = Color(0xFFF2F3F5),
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }

                // Wide Call Button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(Color(0xFF2B2D31))
                        .clickable {
                            scope.launch {
                                try {
                                    val dm = openDM(user.id!!)
                                    if (dm.id != null) {
                                        if (StoatAPI.channelCache[dm.id] == null)
                                            StoatAPI.channelCache[dm.id!!] = dm
                                        dismissSheet()
                                        ActionChannel.send(Action.SwitchChannel(dm.id!!))
                                        ActionChannel.send(Action.OpenVoiceChannelOverlay(dm.id!!))
                                    } else {
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.user_info_sheet_failed_to_open_dm),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    logcat(LogPriority.ERROR) { e.asLog() }
                                    Toast.makeText(context, "Failed to start call", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_call_24dp__fill),
                        contentDescription = "Call",
                        tint = Color(0xFFF2F3F5),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Call",
                        color = Color(0xFFF2F3F5),
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }

            "Outgoing" -> {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                unfriendUser(user.id!!)
                            } catch (e: Exception) {
                                if (e.message == "NoEffect") return@launch
                                logcat(LogPriority.ERROR) { e.asLog() }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_cancel_request))
                }
            }

            "Incoming" -> {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                acceptFriendRequest(user.id!!)
                            } catch (e: Exception) {
                                if (e.message == "NoEffect") return@launch
                                logcat(LogPriority.ERROR) { e.asLog() }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_accept_request))
                }
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                unfriendUser(user.id!!)
                            } catch (e: Exception) {
                                if (e.message == "NoEffect") return@launch
                                logcat(LogPriority.ERROR) { e.asLog() }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_decline_request))
                }
            }

            "Blocked" -> {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                unblockUser(user.id!!)
                            } catch (e: Exception) {
                                if (e.message == "NoEffect") return@launch
                                logcat(LogPriority.ERROR) { e.asLog() }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_unblock))
                }
            }

            "BlockedOther" -> Box(Modifier.weight(1f))
        }

        if (user.relationship != "User" && user.relationship != "Friend") {
            Row { // Prevent the dropdown menu from counting towards arrangement spacing
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    when (user.relationship) {
                        "Friend" -> {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.user_info_sheet_remove_friend))
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_person_off_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    scope.launch {
                                        try {
                                            unfriendUser(user.id!!)
                                        } catch (e: Exception) {
                                            if (e.message == "NoEffect") return@launch
                                            logcat(LogPriority.ERROR) { e.asLog() }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    when (user.relationship) {
                        "Blocked" -> {}

                        else -> DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.user_info_sheet_block))
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_block_24dp),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                scope.launch {
                                    try {
                                        blockUser(user.id!!)
                                    } catch (e: Exception) {
                                        if (e.message == "NoEffect") return@launch
                                        logcat(LogPriority.ERROR) { e.asLog() }
                                    }
                                }
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.user_info_sheet_copy_id))
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_identifier_copy_24dp),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            scope.launch {
                                clipboard.setText(AnnotatedString(user.id!!))
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.user_info_sheet_report),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_flag_24dp),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            scope.launch {
                                ActionChannel.send(Action.ReportUser(user.id!!))

                                if (Platform.needsShowClipboardNotification()) {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )

                    if (canEditServerIdentity || moderationPermissions?.any == true) {
                        HorizontalDivider()

                        if (canEditServerIdentity) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.server_identity_edit_member))
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_id_card_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    scope.launch {
                                        dismissSheet()
                                        ActionChannel.send(
                                            Action.TopNavigate(
                                                "settings/server/${serverId!!}/identity/${user.id!!}"
                                            )
                                        )
                                    }
                                },
                            )
                        }

                        if (moderationPermissions?.canTimeout == true) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.member_moderation_timeout),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_timer_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    moderationAction = MemberModerationAction.Timeout
                                },
                            )
                        }

                        if (moderationPermissions?.canKick == true) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.member_moderation_kick),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sports_and_outdoors_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    moderationAction = MemberModerationAction.Kick
                                },
                            )
                        }

                        if (moderationPermissions?.canBan == true) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.member_moderation_ban),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_gavel_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    moderationAction = MemberModerationAction.Ban
                                },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(Color(0xFF2B2D31))
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_24dp),
                        contentDescription = stringResource(R.string.menu),
                        tint = Color(0xFFDBDEE1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
