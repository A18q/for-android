package chat.stoat.screens.chat.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.internals.ULID
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.User
import chat.stoat.screens.chat.ChatRouterDestination
import chat.stoat.sheets.ChannelContextSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Discord Mobile Authentic Color Tokens ───
private val DiscordDarkBg = Color(0xFF1E1F22)
private val DiscordCardBg = Color(0xFF2B2D31)
private val DiscordInsetBg = Color(0xFF232428)
private val DiscordHeader = Color(0xFFF2F3F5)
private val DiscordTextNormal = Color(0xFFDBDEE1)
private val DiscordTextMuted = Color(0xFF949BA4)
private val DiscordBlurple = Color(0xFF5865F2)
private val DiscordUnreadBadge = Color(0xFFED4245)
private val DiscordDivider = Color(0xFF35373C)

private fun formatRelativeTime(ulid: String?): String {
    if (ulid == null) return ""
    return try {
        val timestamp = ULID.asTimestamp(ulid)
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        if (diff < 0) return ""
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d"
            else -> {
                val format = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
                format.format(Date(timestamp))
            }
        }
    } catch (_: Exception) {
        ""
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OverviewScreen(
    navController: NavController,
    useDrawer: Boolean = false,
    onDrawerClicked: () -> Unit,
    includePadding: Boolean = true,
    onDestinationChanged: (ChatRouterDestination) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var contextSheetChannelId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val dmChannels = remember(StoatAPI.channelCache.values) {
        StoatAPI.channelCache.values
            .filter { it.channelType == ChannelType.DirectMessage || it.channelType == ChannelType.Group }
            .filter { if (it.channelType == ChannelType.DirectMessage) it.active == true else true }
            .sortedByDescending { it.lastMessageID ?: it.id }
    }

    val onlinePartners = remember(dmChannels, StoatAPI.userCache.values) {
        dmChannels.mapNotNull { ch ->
            if (ch.channelType == ChannelType.DirectMessage) {
                val partnerId = ChannelUtils.resolveDMPartner(ch)
                StoatAPI.userCache[partnerId]
            } else null
        }.filter { it.online == true }
    }

    // Real-time filtered DMs based on search query
    val filteredDMs = remember(dmChannels, searchQuery) {
        if (searchQuery.isBlank()) {
            dmChannels
        } else {
            dmChannels.filter { channel ->
                val partner = if (channel.channelType == ChannelType.DirectMessage) {
                    val partnerId = ChannelUtils.resolveDMPartner(channel)
                    StoatAPI.userCache[partnerId]
                } else null

                val name = partner?.let { User.resolveDefaultName(it) } ?: channel.name ?: ""
                val preview = channel.lastMessageID?.let { StoatAPI.messageCache[it]?.content } ?: ""

                name.contains(searchQuery, ignoreCase = true) ||
                        preview.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DiscordDarkBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Top Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (useDrawer) {
                        IconButton(
                            onClick = onDrawerClicked,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_menu_24dp),
                                contentDescription = "Menu",
                                tint = DiscordHeader
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        "Messages",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DiscordHeader
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Discord Action Row: Search Toggle, Message Requests (Mail), Add Friends, New DM (+)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSearchExpanded) DiscordBlurple else DiscordCardBg)
                            .clickable { isSearchExpanded = !isSearchExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_24dp),
                            contentDescription = "Search",
                            tint = if (isSearchExpanded) Color.White else DiscordHeader,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Message Requests / Inbox Mail Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DiscordCardBg)
                            .clickable { onDestinationChanged(ChatRouterDestination.Friends) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mail_24dp),
                            contentDescription = "Message Requests",
                            tint = DiscordHeader,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Add Friends pill button
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DiscordCardBg)
                            .clickable { onDestinationChanged(ChatRouterDestination.Friends) }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_person_add_24dp),
                            contentDescription = null,
                            tint = DiscordHeader,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Add Friends",
                            color = DiscordHeader,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // New DM Blurple "+" pill button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DiscordBlurple)
                            .clickable { onDestinationChanged(ChatRouterDestination.Friends) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
                            contentDescription = "New DM",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Interactive Expandable Search Bar
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search direct messages...",
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
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close_24dp),
                                        contentDescription = "Clear",
                                        tint = DiscordTextMuted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
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
                }
            }
        },
        containerColor = DiscordDarkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── ACTIVE NOW SECTION (Discord Mobile Squircle Story Cards 1:1) ───
            if (onlinePartners.isNotEmpty() && searchQuery.isBlank()) {
                item {
                    Spacer(Modifier.height(4.dp))
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(
                            items = onlinePartners,
                            key = { it.id ?: it.username ?: it.hashCode() }
                        ) { partner ->
                            ActiveNowFriendCard(
                                partner = partner,
                                onClick = {
                                    val dm = dmChannels.firstOrNull { ch ->
                                        ChannelUtils.resolveDMPartner(ch) == partner.id
                                    }
                                    dm?.id?.let {
                                        onDestinationChanged(ChatRouterDestination.Channel(it))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ─── DIRECT MESSAGES SECTION ───
            item {
                Spacer(Modifier.height(4.dp))
            }

            if (filteredDMs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chat_24dp),
                                contentDescription = null,
                                tint = DiscordTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "No conversations match your search" else "No direct messages yet",
                                color = DiscordTextMuted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredDMs,
                    key = { it.id ?: it.hashCode() }
                ) { channel ->
                    DirectMessageItemRow(
                        channel = channel,
                        onClick = {
                            channel.id?.let {
                                onDestinationChanged(ChatRouterDestination.Channel(it))
                                onDrawerClicked()
                            }
                        },
                        onLongClick = {
                            channel.id?.let { contextSheetChannelId = it }
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ─── Long-Press DM Context Sheet ───
    if (contextSheetChannelId != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { contextSheetChannelId = null },
            sheetState = sheetState,
            containerColor = DiscordDarkBg
        ) {
            ChannelContextSheet(
                channelId = contextSheetChannelId!!,
                onHideSheet = {
                    sheetState.hide()
                    contextSheetChannelId = null
                }
            )
        }
    }
}

// ─── Discord Active Now Story Card (1:1 with Screenshot) ───
@Composable
private fun ActiveNowFriendCard(
    partner: User,
    onClick: () -> Unit
) {
    val displayName = User.resolveDefaultName(partner)
    val statusDotColor = when (partner.status?.presence) {
        "Idle" -> Color(0xFFF0B232)
        "Focus", "Busy" -> Color(0xFFF23F43)
        "Invisible" -> Color(0xFF80848E)
        else -> if (partner.online == true) Color(0xFF23A55A) else Color(0xFF80848E)
    }

    Box(
        modifier = Modifier
            .size(86.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DiscordCardBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            UserAvatar(
                username = displayName,
                presence = presenceFromStatus(partner.status?.presence, partner.online == true),
                userId = partner.id ?: "",
                avatar = partner.avatar,
                size = 52.dp,
                presenceSize = 0.dp
            )

            // Status badge dot with protective ring cutout
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(DiscordCardBg),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
            }
        }
    }
}

// ─── Discord DM List Item Row with Timestamp & Unread Badge ───
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectMessageItemRow(
    channel: Channel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val partner = if (channel.channelType == ChannelType.DirectMessage) {
        val partnerId = ChannelUtils.resolveDMPartner(channel)
        StoatAPI.userCache[partnerId]
    } else null

    val name = partner?.let { User.resolveDefaultName(it) } ?: channel.name ?: "Unknown"

    val lastMessage = channel.lastMessageID?.let { StoatAPI.messageCache[it] }
    val previewText = when {
        lastMessage != null -> {
            val prefix = if (lastMessage.author == StoatAPI.selfId) "You: " else ""
            val content = lastMessage.content?.takeIf { it.isNotBlank() }
                ?: if (lastMessage.attachments?.isNotEmpty() == true) "📷 Attachment" else ""
            "$prefix$content"
        }
        else -> ""
    }

    val isUnread = remember(channel.id, channel.lastMessageID, StoatAPI.unreads) {
        channel.id?.let { chId ->
            channel.lastMessageID?.let { msgId ->
                StoatAPI.unreads.hasUnread(chId, msgId, serverId = null)
            }
        } ?: false
    }

    val relativeTimestamp = remember(channel.lastMessageID) {
        formatRelativeTime(channel.lastMessageID)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar
        UserAvatar(
            username = name,
            presence = presenceFromStatus(partner?.status?.presence, partner?.online == true),
            userId = partner?.id ?: channel.id ?: "",
            avatar = partner?.avatar ?: channel.icon,
            size = 48.dp,
            presenceSize = 14.dp
        )

        Spacer(Modifier.width(14.dp))

        // Center Column: Name and Message Preview
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                color = DiscordHeader,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (previewText.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = previewText,
                    fontSize = 13.sp,
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isUnread) DiscordTextNormal else DiscordTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Right Column: Relative Timestamp & Unread Dot Badge
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            if (relativeTimestamp.isNotEmpty()) {
                Text(
                    text = relativeTimestamp,
                    fontSize = 12.sp,
                    color = if (isUnread) DiscordHeader else DiscordTextMuted,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(Modifier.height(4.dp))

            if (isUnread) {
                // Discord Unread Dot Indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(DiscordUnreadBadge)
                )
            } else {
                Spacer(Modifier.size(10.dp))
            }
        }
    }
}
