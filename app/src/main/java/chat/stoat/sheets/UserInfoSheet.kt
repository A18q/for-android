package chat.stoat.sheets

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import chat.stoat.api.routes.user.blockUser
import chat.stoat.api.routes.user.unblockUser
import chat.stoat.api.routes.user.unfriendUser
import chat.stoat.composables.screens.chat.drawer.ServerIconImage
import chat.stoat.dialogs.MemberModerationAction
import chat.stoat.dialogs.MemberModerationDialog
import chat.stoat.dialogs.memberModerationPermissions
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ULID
import chat.stoat.api.routes.user.fetchUserProfile
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.NonIdealState
import chat.stoat.composables.generic.PresenceBadge
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.composables.screens.settings.UserButtons
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.AutumnResource
import chat.stoat.core.model.schemas.Profile
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.schemas.UserBadges
import chat.stoat.core.model.schemas.has
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

// ─── Discord Desktop Profile Popout Tokens (1:1 Match) ───
private val DiscordDarkCanvas = Color(0xFF111214) // Discord Popout Outer Canvas
private val DiscordCardSurface = Color(0xFF232428) // Discord Inner Card Body
private val DiscordInsetSurface = Color(0xFF1E1F22) // Discord Inset Pill / Chip Surface
private val DiscordHeader = Color(0xFFF2F3F5)
private val DiscordTextNormal = Color(0xFFDBDEE1)
private val DiscordTextMuted = Color(0xFF949BA4)
private val DiscordBlurple = Color(0xFF5865F2)
private val DiscordDivider = Color(0xFF2B2D31)

private val FallbackRoleColor = Color(0xFF99AAB5)

private fun safeParseColor(hex: String?, fallback: Color = FallbackRoleColor): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        fallback
    }
}

sealed class NoteSaveState {
    object Idle : NoteSaveState()
    object Saving : NoteSaveState()
    object Saved : NoteSaveState()
    data class Error(val message: String) : NoteSaveState()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserInfoSheet(
    userId: String,
    serverId: String? = null,
    dismissSheet: suspend () -> Unit
) {
    val user = StoatAPI.userCache[userId]
    val member = serverId?.let { StoatAPI.members.getMember(it, userId) }
    val server = StoatAPI.serverCache[serverId]
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var profile by remember(user) { mutableStateOf(user?.profile) }
    var profileNotFound by remember { mutableStateOf(false) }

    val isSelf = userId == StoatAPI.selfId
    var noteText by remember(userId) { mutableStateOf("") }
    var noteSaveState by remember { mutableStateOf<NoteSaveState>(NoteSaveState.Idle) }
    var lastGoodNoteText by remember(userId) { mutableStateOf("") }
    var moderationAction by remember { mutableStateOf<MemberModerationAction?>(null) }

    LaunchedEffect(userId) {
        if (!isSelf) {
            try {
                val fetchedNote = chat.stoat.api.routes.user.fetchUserNote(userId)
                val text = fetchedNote ?: ""
                noteText = text
                lastGoodNoteText = text
            } catch (e: Exception) {
                noteSaveState = NoteSaveState.Error("Failed to fetch")
            }
        }
        try {
            user?.id?.let { uid ->
                val fetched = fetchUserProfile(uid)
                profile = fetched
                StoatAPI.userCache[uid]?.let { u ->
                    StoatAPI.userCache[uid] = u.copy(profile = fetched)
                }
            }
        } catch (e: Exception) {
            if (e.message == "NotFound") {
                profileNotFound = true
            }
            e.printStackTrace()
        }
    }

    LaunchedEffect(noteText) {
        if (!isSelf && noteText != lastGoodNoteText) {
            noteSaveState = NoteSaveState.Saving
            kotlinx.coroutines.delay(800L)
            try {
                chat.stoat.api.routes.user.putUserNote(userId, noteText)
                lastGoodNoteText = noteText
                noteSaveState = NoteSaveState.Saved
            } catch (e: Exception) {
                noteText = lastGoodNoteText
                noteSaveState = NoteSaveState.Error("Failed to save")
            }
        }
    }

    if (user == null) {
        NonIdealState(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_error_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(it)
                )
            },
            title = {
                Text(text = stringResource(R.string.user_info_sheet_user_not_found))
            },
            description = {
                Text(text = stringResource(R.string.user_info_sheet_user_not_found_description))
            }
        )
        Spacer(Modifier.height(20.dp))
        return
    }

    if (serverId != null && moderationAction != null) {
        MemberModerationDialog(
            action = moderationAction!!,
            serverId = serverId,
            user = user,
            dismissUserSheet = dismissSheet,
            onDismiss = { moderationAction = null },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiscordDarkCanvas)
            .verticalScroll(rememberScrollState())
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
    ) {
        // ─── 1. Header Banner & Overlapping Avatar Staging ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            val background = profile?.background
            val bgId = background?.id
            if (bgId != null && bgId.isNotBlank()) {
                val bgUrl = if (!background.filename.isNullOrBlank()) {
                    "$STOAT_FILES/backgrounds/$bgId/${background.filename}"
                } else {
                    "$STOAT_FILES/backgrounds/$bgId"
                }
                RemoteImage(
                    url = bgUrl,
                    description = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Discord default aesthetic gradient banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    DiscordBlurple.copy(alpha = 0.85f),
                                    DiscordCardSurface
                                )
                            )
                        )
                )
            }

            // Discord Overlapping Circular Avatar Staged Over Banner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .size(126.dp)
                    .background(DiscordDarkCanvas, CircleShape)
                    .padding(4.dp)
            ) {
                UserAvatar(
                    username = User.resolveDefaultName(user),
                    userId = user.id ?: "",
                    avatar = user.avatar,
                    size = 118.dp,
                    presenceSize = 0.dp,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )

                val userPresence = presenceFromStatus(user.status?.presence, user.online ?: false)
                if (userPresence != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .background(DiscordDarkCanvas, CircleShape)
                            .padding(3.dp)
                    ) {
                        PresenceBadge(userPresence, size = 24.dp)
                    }
                }
            }

            // Discord Badge Capsule Pinned on the Right
            val badges = user.badges ?: 0L
            if (badges > 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 4.dp)
                ) {
                    DiscordBadgeCapsule(badges = badges)
                }
            }

            // ─── Discord Sheet Drag Handle ───
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .width(36.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color.White.copy(alpha = 0.8f))
            )

            // ─── Discord Top-Right Action Buttons (Banner) ───
            if (!isSelf && user.id != null) {
                var bannerMenuOpen by remember { mutableStateOf(false) }
                var friendMenuOpen by remember { mutableStateOf(false) }
                val clipboard = LocalClipboardManager.current
                val moderationPermissions = serverId?.let { memberModerationPermissions(it, user.id) }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (user.relationship == "Friend") {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { friendMenuOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_person_24dp),
                                    contentDescription = "Friend Actions",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = friendMenuOpen,
                                onDismissRequest = { friendMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.user_info_sheet_remove_friend)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_person_off_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        friendMenuOpen = false
                                        scope.launch {
                                            try {
                                                unfriendUser(user.id!!)
                                            } catch (e: Exception) {
                                                if (e.message != "NoEffect") logcat(LogPriority.ERROR) { e.asLog() }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { bannerMenuOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert_24dp),
                                contentDescription = stringResource(R.string.menu),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = bannerMenuOpen,
                            onDismissRequest = { bannerMenuOpen = false }
                        ) {
                            if (user.relationship == "Blocked") {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.user_info_sheet_unblock)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_block_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        bannerMenuOpen = false
                                        scope.launch {
                                            try { unblockUser(user.id!!) }
                                            catch (e: Exception) { if (e.message != "NoEffect") logcat(LogPriority.ERROR) { e.asLog() } }
                                        }
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.user_info_sheet_block)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_block_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        bannerMenuOpen = false
                                        scope.launch {
                                            try { blockUser(user.id!!) }
                                            catch (e: Exception) { if (e.message != "NoEffect") logcat(LogPriority.ERROR) { e.asLog() } }
                                        }
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.user_info_sheet_copy_id)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_content_copy_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    bannerMenuOpen = false
                                    clipboard.setText(AnnotatedString(user.id!!))
                                    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                                }
                            )

                            if (serverId != null) {
                                if (moderationPermissions?.canKick == true) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.member_moderation_kick),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_logout_24dp),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            bannerMenuOpen = false
                                            moderationAction = MemberModerationAction.Kick
                                        }
                                    )
                                }
                                if (moderationPermissions?.canBan == true) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.member_moderation_ban),
                                                color = MaterialTheme.colorScheme.error
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
                                            bannerMenuOpen = false
                                            moderationAction = MemberModerationAction.Ban
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── 2. Identity Block (Display Name, Handle, Pronouns, Custom Status) ───
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            val displayName = remember(user, member?.nickname) {
                member?.nickname ?: User.resolveDefaultName(user)
            }
            Text(
                text = displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DiscordHeader
            )

            val subtitle = remember(user.username, user.discriminator, user.pronouns) {
                val handle = user.username?.let {
                    val disc = user.discriminator?.takeIf { d -> d.isNotEmpty() }?.let { d -> "#$d" } ?: ""
                    "@$it$disc"
                } ?: ""
                val pronouns = user.pronouns?.trim()?.takeIf { it.isNotEmpty() }
                if (pronouns != null && handle.isNotEmpty()) "$handle • $pronouns" else handle.ifEmpty { pronouns ?: "" }
            }

            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = DiscordTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }

            // Custom Status Pill / Bubble
            val statusText = user.status?.text
            if (!statusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DiscordCardSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        color = DiscordHeader,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val mutualServers = remember(userId) {
                StoatAPI.serverCache.values.filter { srv ->
                    srv.id != null && StoatAPI.members.hasMember(srv.id!!, userId)
                }
            }
            if (mutualServers.isNotEmpty() && !isSelf) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-6).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        mutualServers.take(4).forEach { srv ->
                            ServerIconImage(
                                server = srv,
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(1.5.dp, DiscordDarkCanvas, CircleShape)
                                    .clip(CircleShape),
                                cornerRadius = 10.dp
                            )
                        }
                    }
                    Text(
                        text = "${mutualServers.size} Mutual Server${if (mutualServers.size > 1) "s" else ""}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DiscordTextNormal
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (isSelf) {
                Button(
                    onClick = {
                        scope.launch {
                            dismissSheet()
                            ActionChannel.send(Action.TopNavigate("settings/profile"))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Profile",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            } else {
                UserButtons(
                    user = user,
                    serverId = serverId,
                    dismissSheet = dismissSheet,
                )
            }
        }

        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

        // ─── 3. Stacked Discord Elevated Cards ───
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: ABOUT ME (Bio)
            if (!profile?.content.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DiscordCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DiscordDivider.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ABOUT ME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DiscordTextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SelectionContainer {
                            ChatMarkdown(content = profile?.content!!, serverId = serverId)
                        }
                    }
                }
            }

            // Card 1.5: NOTE
            if (!isSelf) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DiscordCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DiscordDivider.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "NOTE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DiscordTextMuted,
                                letterSpacing = 0.8.sp
                            )
                            androidx.compose.animation.AnimatedVisibility(visible = noteSaveState != NoteSaveState.Idle) {
                                Text(
                                    text = when (val state = noteSaveState) {
                                        NoteSaveState.Saving -> "Saving..."
                                        NoteSaveState.Saved -> "Saved"
                                        is NoteSaveState.Error -> state.message
                                        else -> ""
                                    },
                                    fontSize = 11.sp,
                                    color = if (noteSaveState is NoteSaveState.Error) Color.Red else DiscordTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = noteText,
                            onValueChange = { if (it.length <= 500) noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Note", color = DiscordTextMuted, fontSize = 13.sp) },
                            minLines = 2,
                            maxLines = 5,
                            textStyle = androidx.compose.ui.text.TextStyle(color = DiscordTextNormal, fontSize = 13.sp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DiscordBlurple,
                                unfocusedBorderColor = DiscordDivider,
                                cursorColor = DiscordTextNormal
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Only visible to you",
                            fontSize = 10.sp,
                            color = DiscordTextMuted
                        )
                    }
                }
            }

            // Card 2: MEMBER SINCE (Server Join & Account Creation)
            val accountAt = remember(user.id) {
                user.id?.let {
                    DateUtils.getRelativeTimeSpanString(
                        ULID.asTimestamp(it),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                }
            }
            val joinedAt = remember(member?.joinedAt) {
                member?.joinedAt?.let {
                    DateUtils.getRelativeTimeSpanString(
                        Instant.parse(it).toEpochMilliseconds(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DiscordCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DiscordDivider.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "MEMBER SINCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DiscordTextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (joinedAt != null && server?.name != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_explore_24dp),
                                contentDescription = null,
                                tint = DiscordTextNormal,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = server.name!!,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DiscordHeader
                                )
                                Text(
                                    text = joinedAt,
                                    fontSize = 12.sp,
                                    color = DiscordTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (accountAt != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chat_24dp),
                                contentDescription = null,
                                tint = DiscordTextNormal,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Stoat",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DiscordHeader
                                )
                                Text(
                                    text = accountAt,
                                    fontSize = 12.sp,
                                    color = DiscordTextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Card 3: ROLES — {count} (Hierarchical Rank Sorted Discord Role Pills)
            val roles = remember(member?.roles, server?.roles) {
                member?.roles?.mapNotNull { roleId -> server?.roles?.get(roleId) }
                    ?.sortedByDescending { it.rank ?: 0.0 }
            }
            if (!roles.isNullOrEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DiscordCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DiscordDivider.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ROLES — ${roles.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DiscordTextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            roles.forEach { role ->
                                DiscordRolePill(role = role)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Discord Authentic Badge Capsule Row ───
@Composable
private fun DiscordBadgeCapsule(badges: Long) {
    val context = LocalContext.current
    val activeBadges = remember(badges) {
        UserBadges.entries.filter { badges has it }
    }
    if (activeBadges.isEmpty()) return

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DiscordInsetSurface)
            .border(1.dp, DiscordDivider, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        activeBadges.forEach { badge ->
            val iconRes = when (badge) {
                UserBadges.Developer -> R.drawable.user_badge_developer
                UserBadges.Translator -> R.drawable.user_badge_translator
                UserBadges.Supporter -> R.drawable.user_badge_supporter
                UserBadges.ResponsibleDisclosure -> R.drawable.user_badge_disclosure
                UserBadges.Founder -> R.drawable.user_badge_founder
                UserBadges.PlatformModeration -> R.drawable.user_badge_moderation
                UserBadges.ActiveSupporter -> R.drawable.ic_emoji_people_24dp
                UserBadges.Paw -> R.drawable.user_badge_paw
                UserBadges.EarlyAdopter -> R.drawable.user_badge_early_adopter
                else -> null
            }
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = badge.name,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            Toast.makeText(context, badge.name, Toast.LENGTH_SHORT).show()
                        }
                )
            }
        }
    }
}

// ─── Discord Authentic Role Pill (Color Dot + Text on Inset Background) ───
@Composable
private fun DiscordRolePill(role: Role) {
    val roleColor = remember(role.colour) { safeParseColor(role.colour) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DiscordInsetSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(roleColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = role.name ?: "role",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DiscordTextNormal
        )
    }
}
