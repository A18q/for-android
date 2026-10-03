package chat.stoat.sheets

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ULID
import chat.stoat.api.routes.user.acceptFriendRequest
import chat.stoat.api.routes.user.blockUser
import chat.stoat.api.routes.user.fetchUserNote
import chat.stoat.api.routes.user.fetchUserProfile
import chat.stoat.api.routes.user.friendUser
import chat.stoat.api.routes.user.openDM
import chat.stoat.api.routes.user.putUserNote
import chat.stoat.api.routes.user.unblockUser
import chat.stoat.api.routes.user.unfriendUser
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.NonIdealState
import chat.stoat.composables.generic.PresenceBadge
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.composables.screens.chat.drawer.ServerIconImage
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Profile
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.schemas.UserBadges
import chat.stoat.core.model.schemas.has
import chat.stoat.dialogs.MemberModerationAction
import chat.stoat.dialogs.MemberModerationDialog
import chat.stoat.dialogs.memberModerationPermissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Discord Mobile Profile Constants ───
private val AVATAR_SIZE = 118.dp
private val RING_WIDTH = 4.dp
private const val BANNER_HEIGHT_RATIO = 0.38f

// ─── Discord Surface & Theme Tokens ───
private val DiscordDarkCanvas = Color(0xFF111214) // Darkest canvas background
private val DiscordCardSurface = Color(0xFF232428) // Elevated section card surface
private val DiscordInsetSurface = Color(0xFF1E1F22) // Inset pills and chip containers
private val DiscordHeader = Color(0xFFF2F3F5)
private val DiscordTextNormal = Color(0xFFDBDEE1)
private val DiscordTextMuted = Color(0xFF949BA4)
private val DiscordBlurple = Color(0xFF5865F2)
private val DiscordDivider = Color(0xFF2B2D31)
private val DiscordGreen = Color(0xFF23A55A)

private val CardShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(12.dp)

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

private fun isRtl(text: String): Boolean {
    for (char in text) {
        val dir = Character.getDirectionality(char)
        if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
            return true
        }
        if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
            return false
        }
    }
    return false
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
    dismissSheet: suspend () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onOpenStatusSheet: (() -> Unit)? = null
) {
    val user = StoatAPI.userCache[userId]
    val member = serverId?.let { StoatAPI.members.getMember(it, userId) }
    val server = StoatAPI.serverCache[serverId]
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var profile by remember(user) { mutableStateOf(user?.profile) }

    val isSelf = userId == StoatAPI.selfId
    var noteText by remember(userId) { mutableStateOf("") }
    var noteSaveState by remember { mutableStateOf<NoteSaveState>(NoteSaveState.Idle) }
    var lastGoodNoteText by remember(userId) { mutableStateOf("") }
    var moderationAction by remember { mutableStateOf<MemberModerationAction?>(null) }
    var showNoteEditorSheet by remember { mutableStateOf(false) }

    val mutualServers = remember(userId) {
        StoatAPI.serverCache.values.filter { srv ->
            srv.id != null && StoatAPI.members.hasMember(srv.id!!, userId)
        }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val showTabs = mutualServers.isNotEmpty() && !isSelf

    // Fetch user note and full profile
    LaunchedEffect(userId) {
        if (!isSelf) {
            try {
                val fetchedNote = fetchUserNote(userId)
                val text = fetchedNote ?: ""
                noteText = text
                lastGoodNoteText = text
            } catch (_: Exception) {
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
            e.printStackTrace()
        }
    }

    // Debounced autosave for note
    LaunchedEffect(noteText) {
        if (!isSelf && noteText != lastGoodNoteText) {
            noteSaveState = NoteSaveState.Saving
            delay(800L)
            try {
                putUserNote(userId, noteText)
                lastGoodNoteText = noteText
                noteSaveState = NoteSaveState.Saved
            } catch (_: Exception) {
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

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val safeTopInset = maxOf(topInset, 28.dp)
    val bannerHeight = maxOf(175.dp, screenWidth * BANNER_HEIGHT_RATIO + safeTopInset)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordDarkCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
        ) {
            // ─── 1. Header Banner & Overlapping Avatar ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight + AVATAR_SIZE / 2)
            ) {
                // Full-bleed Banner
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
                            .height(bannerHeight),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight)
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

                // Status bar protection gradient over banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(safeTopInset + 16.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Overlapping Avatar with Ring
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                        .size(AVATAR_SIZE + RING_WIDTH * 2)
                        .background(DiscordDarkCanvas, CircleShape)
                        .padding(RING_WIDTH)
                ) {
                    UserAvatar(
                        username = User.resolveDefaultName(user),
                        userId = user.id ?: "",
                        avatar = user.avatar,
                        size = AVATAR_SIZE,
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
                                .size(28.dp)
                                .background(DiscordDarkCanvas, CircleShape)
                                .padding(3.dp)
                        ) {
                            PresenceBadge(userPresence, size = 22.dp)
                        }
                    }
                }

                // Status Thought Bubble (next to avatar on bottom right of banner)
                val statusText = user.status?.text
                val hasStatus = !statusText.isNullOrBlank()

                if (hasStatus) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = AVATAR_SIZE + 24.dp, bottom = 12.dp)
                            .clip(PillShape)
                            .background(DiscordCardSurface)
                            .border(1.dp, DiscordDivider, PillShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = statusText!!,
                            color = DiscordHeader,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (isSelf && onOpenStatusSheet != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = AVATAR_SIZE + 24.dp, bottom = 12.dp)
                            .clip(PillShape)
                            .background(DiscordCardSurface)
                            .border(1.dp, DiscordDivider, PillShape)
                            .clickable { onOpenStatusSheet() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_mood_24dp),
                                contentDescription = null,
                                tint = DiscordTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Add status",
                                color = DiscordTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ─── 2. Identity Block ───
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
                    color = DiscordHeader,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

                // Badges Row
                val badges = user.badges ?: 0L
                if (badges > 0L) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DiscordBadgeCapsule(badges = badges)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Action Button(s)
                if (isSelf) {
                    Button(
                        onClick = {
                            scope.launch {
                                dismissSheet()
                                if (onOpenSettings != null) {
                                    onOpenSettings()
                                } else {
                                    ActionChannel.send(Action.TopNavigate("settings/profile"))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Profile",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Message Button
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val dm = openDM(userId)
                                        dismissSheet()
                                        dm.id?.let { ActionChannel.send(Action.SwitchChannel(it)) }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open DM: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chat_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Message",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        // Friend Action Button
                        val relationship = user.relationship ?: "None"
                        var friendMenuOpen by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        when (relationship) {
                                            "Friend" -> friendMenuOpen = true
                                            "Incoming" -> runCatching { acceptFriendRequest(userId) }
                                            "Outgoing" -> friendMenuOpen = true
                                            "Blocked" -> runCatching { unblockUser(userId) }
                                            else -> runCatching { friendUser("${user.username}#${user.discriminator}") }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (relationship == "Friend") DiscordGreen else DiscordCardSurface,
                                    contentColor = DiscordHeader
                                )
                            ) {
                                val (icon, label) = when (relationship) {
                                    "Friend" -> Pair(R.drawable.ic_check_24dp, "Friends")
                                    "Incoming" -> Pair(R.drawable.ic_check_24dp, "Accept")
                                    "Outgoing" -> Pair(R.drawable.ic_person_24dp, "Pending")
                                    "Blocked" -> Pair(R.drawable.ic_block_24dp, "Unblock")
                                    else -> Pair(R.drawable.ic_person_add_24dp, "Add Friend")
                                }
                                Icon(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            DropdownMenu(
                                expanded = friendMenuOpen,
                                onDismissRequest = { friendMenuOpen = false }
                            ) {
                                if (relationship == "Friend") {
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
                                                runCatching { unfriendUser(userId) }
                                            }
                                        }
                                    )
                                } else if (relationship == "Outgoing") {
                                    DropdownMenuItem(
                                        text = { Text("Cancel Friend Request") },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_close_24dp),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            friendMenuOpen = false
                                            scope.launch {
                                                runCatching { unfriendUser(userId) }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Overflow More Button
                        var moreMenuOpen by remember { mutableStateOf(false) }
                        val clipboard = LocalClipboardManager.current
                        val moderationPermissions = serverId?.let { memberModerationPermissions(it, userId) }

                        Box {
                            IconButton(
                                onClick = { moreMenuOpen = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(PillShape)
                                    .background(DiscordCardSurface)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert_24dp),
                                    contentDescription = "More",
                                    tint = DiscordHeader
                                )
                            }

                            DropdownMenu(
                                expanded = moreMenuOpen,
                                onDismissRequest = { moreMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.user_info_sheet_copy_id)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_content_copy_24dp),
                                            contentDescription = null,
                                            tint = DiscordHeader
                                        )
                                    },
                                    onClick = {
                                        moreMenuOpen = false
                                        clipboard.setText(AnnotatedString(userId))
                                        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                                    }
                                )

                                if (user.relationship == "Blocked") {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.user_info_sheet_unblock)) },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_block_24dp),
                                                contentDescription = null,
                                                tint = DiscordHeader
                                            )
                                        },
                                        onClick = {
                                            moreMenuOpen = false
                                            scope.launch { runCatching { unblockUser(userId) } }
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
                                            moreMenuOpen = false
                                            scope.launch { runCatching { blockUser(userId) } }
                                        }
                                    )
                                }

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
                                                moreMenuOpen = false
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
                                                moreMenuOpen = false
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

            // ─── 3. Tabs (Shown only if >= 2 sections exist) ───
            if (showTabs) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    val tabs = listOf("User Info", "Mutual Servers (${mutualServers.size})")
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTabIndex == index
                        Column(
                            modifier = Modifier
                                .clickable { selectedTabIndex = index }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tabTitle,
                                color = if (isSelected) DiscordHeader else DiscordTextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(32.dp)
                                    .background(if (isSelected) DiscordBlurple else Color.Transparent)
                            )
                        }
                    }
                }
                HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 8.dp))
            }

            // ─── 4. Main Section Cards ───
            if (selectedTabIndex == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bio ("ABOUT ME") with auto RTL (Arabic) support
                    if (!profile?.content.isNullOrBlank()) {
                        Surface(
                            shape = CardShape,
                            color = DiscordCardSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ABOUT ME",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DiscordTextMuted,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SelectionContainer {
                                    CompositionLocalProvider(
                                        LocalLayoutDirection provides
                                                if (isRtl(profile?.content ?: "")) LayoutDirection.Rtl
                                                else LayoutDirection.Ltr
                                    ) {
                                        ChatMarkdown(content = profile?.content!!, serverId = serverId)
                                    }
                                }
                            }
                        }
                    }

                    // Member Since Card
                    val accountAt = remember(user.id) {
                        user.id?.let {
                            val timestamp = ULID.asTimestamp(it)
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
                        }
                    }
                    val joinedAt = remember(member?.joinedAt) {
                        member?.joinedAt?.let {
                            val epoch = Instant.parse(it).toEpochMilliseconds()
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(epoch))
                        }
                    }

                    Surface(
                        shape = CardShape,
                        color = DiscordCardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "MEMBER SINCE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DiscordTextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (joinedAt != null && server?.name != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_explore_24dp),
                                        contentDescription = null,
                                        tint = DiscordTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = server.name!!,
                                            fontSize = 14.sp,
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
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            if (accountAt != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_chat_24dp),
                                        contentDescription = null,
                                        tint = DiscordTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Stoat",
                                            fontSize = 14.sp,
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

                    // Roles Card (if in server context)
                    val roles = remember(member?.roles, server?.roles) {
                        member?.roles?.mapNotNull { roleId -> server?.roles?.get(roleId) }
                            ?.sortedByDescending { it.rank ?: 0.0 }
                    }
                    if (!roles.isNullOrEmpty()) {
                        Surface(
                            shape = CardShape,
                            color = DiscordCardSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ROLES — ${roles.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DiscordTextMuted,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
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

                    // Note Card Row (only for other users)
                    if (!isSelf) {
                        Surface(
                            shape = CardShape,
                            color = DiscordCardSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { showNoteEditorSheet = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Note (only visible to you)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DiscordTextMuted,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (noteText.isNotBlank()) noteText else "Tap to add a note",
                                        fontSize = 14.sp,
                                        color = if (noteText.isNotBlank()) DiscordHeader else DiscordTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                                    contentDescription = null,
                                    tint = DiscordTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Mutual Servers Tab Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mutualServers.forEach { srv ->
                        Surface(
                            shape = CardShape,
                            color = DiscordCardSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable {
                                    scope.launch {
                                        dismissSheet()
                                        srv.id?.let { ActionChannel.send(Action.SwitchServer(it)) }
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                ServerIconImage(
                                    server = srv,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    cornerRadius = 18.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = srv.name ?: "Server",
                                    color = DiscordHeader,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                                    contentDescription = null,
                                    tint = DiscordTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─── Pinned Overlays Over Banner ───
        if (isSelf) {
            // Own profile: Close (X) button pinned top-left over the banner
            Box(
                modifier = Modifier
                    .padding(top = safeTopInset + 6.dp, start = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        scope.launch { dismissSheet() }
                    }
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24dp),
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            // Other user: Single drag handle pinned top-center over the banner
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = safeTopInset + 4.dp)
                    .width(36.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color.White.copy(alpha = 0.75f))
                    .zIndex(10f)
            )
        }
    }

    // ─── Note Editor Bottom Sheet ───
    if (showNoteEditorSheet) {
        val noteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = noteSheetState,
            onDismissRequest = { showNoteEditorSheet = false },
            containerColor = DiscordCardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Edit Note",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DiscordHeader
                    )
                    AnimatedVisibility(visible = noteSaveState != NoteSaveState.Idle) {
                        Text(
                            text = when (val s = noteSaveState) {
                                NoteSaveState.Saving -> "Saving..."
                                NoteSaveState.Saved -> "Saved"
                                is NoteSaveState.Error -> s.message
                                else -> ""
                            },
                            fontSize = 12.sp,
                            color = if (noteSaveState is NoteSaveState.Error) Color.Red else DiscordTextMuted
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 500) noteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 220.dp),
                    placeholder = { Text("Add a note about this user...", color = DiscordTextMuted, fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiscordBlurple,
                        unfocusedBorderColor = DiscordDivider,
                        focusedTextColor = DiscordHeader,
                        unfocusedTextColor = DiscordHeader,
                        cursorColor = DiscordBlurple
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Only you can see this note.",
                    fontSize = 12.sp,
                    color = DiscordTextMuted
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            noteSheetState.hide()
                            showNoteEditorSheet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
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
