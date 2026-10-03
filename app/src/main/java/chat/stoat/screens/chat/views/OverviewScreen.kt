package chat.stoat.screens.chat.views

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.internals.FriendRequests
import chat.stoat.api.internals.ULID
import chat.stoat.api.routes.channel.createGroupDM
import chat.stoat.api.routes.user.acceptFriendRequest
import chat.stoat.api.routes.user.friendUser
import chat.stoat.api.routes.user.openDM
import chat.stoat.api.routes.user.unfriendUser
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.composables.screens.chat.drawer.StoatUserCapsule
import chat.stoat.core.model.data.STOAT_INVITES
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.User
import chat.stoat.screens.chat.ChatRouterDestination
import chat.stoat.screens.create.MAX_ADDABLE_PEOPLE_IN_GROUP
import chat.stoat.sheets.ChannelContextSheet
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Discord Authentic Color Tokens ───
private val DiscordDarkBg = Color(0xFF1E1F22)
private val DiscordCardBg = Color(0xFF2B2D31)
private val DiscordHeader = Color(0xFFF2F3F5)
private val DiscordTextNormal = Color(0xFFDBDEE1)
private val DiscordTextMuted = Color(0xFF949BA4)
private val DiscordBlurple = Color(0xFF5865F2)
private val DiscordUnreadBadge = Color(0xFFED4245)
private val DiscordDivider = Color(0xFF35373C)
private val DiscordGreen = Color(0xFF23A55A)

private val ActionButtonShape = RoundedCornerShape(12.dp)
private val FriendCardShape = RoundedCornerShape(20.dp)

private val relativeTimeDateFormat = ThreadLocal.withInitial {
    SimpleDateFormat("MM/dd/yy", Locale.getDefault())
}

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
            else -> relativeTimeDateFormat.get().format(Date(timestamp))
        }
    } catch (_: Exception) {
        ""
    }
}

private fun groupUsersAlphabetically(users: List<User>): Map<Char, List<User>> {
    return users
        .sortedWith { a, b ->
            val nameA = User.resolveDefaultName(a)
            val nameB = User.resolveDefaultName(b)
            String.CASE_INSENSITIVE_ORDER.compare(nameA, nameB)
        }
        .groupBy {
            val name = User.resolveDefaultName(it).trim()
            if (name.isNotEmpty() && name.first().isLetter()) name.first().uppercaseChar() else '#'
        }
}

enum class MessagesSubView {
    Home,
    Search,
    MessageRequests,
    AddFriends,
    NewMessage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    navController: NavController,
    useDrawer: Boolean = false,
    onDrawerClicked: () -> Unit,
    includePadding: Boolean = true,
    onDestinationChanged: (ChatRouterDestination) -> Unit = {}
) {
    var currentSubView by rememberSaveable { mutableStateOf(MessagesSubView.Home) }

    BackHandler(enabled = currentSubView != MessagesSubView.Home) {
        currentSubView = MessagesSubView.Home
    }

    val topInset = if (includePadding) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() else 0.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordDarkBg)
    ) {
        if (topInset > 0.dp) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topInset)
                    .background(DiscordDarkBg)
            )
        }

        AnimatedContent(
            targetState = currentSubView,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "MessagesNavigationAnimation",
            modifier = Modifier.weight(1f)
        ) { subView ->
            when (subView) {
                MessagesSubView.Home -> {
                    MessagesHomeScreen(
                        navController = navController,
                        useDrawer = useDrawer,
                        onDrawerClicked = onDrawerClicked,
                        onDestinationChanged = onDestinationChanged,
                        onOpenSearch = { currentSubView = MessagesSubView.Search },
                        onOpenRequests = { currentSubView = MessagesSubView.MessageRequests },
                        onOpenAddFriends = { currentSubView = MessagesSubView.AddFriends },
                        onOpenNewMessage = { currentSubView = MessagesSubView.NewMessage }
                    )
                }

                MessagesSubView.Search -> {
                    DMsSearchScreen(
                        onBack = { currentSubView = MessagesSubView.Home },
                        onOpenDM = { channelId ->
                            onDestinationChanged(ChatRouterDestination.Channel(channelId))
                            currentSubView = MessagesSubView.Home
                        }
                    )
                }

                MessagesSubView.MessageRequests -> {
                    MessageRequestsScreen(
                        onBack = { currentSubView = MessagesSubView.Home },
                        onOpenDM = { channelId ->
                            onDestinationChanged(ChatRouterDestination.Channel(channelId))
                            currentSubView = MessagesSubView.Home
                        }
                    )
                }

                MessagesSubView.AddFriends -> {
                    AddFriendsScreen(
                        onBack = { currentSubView = MessagesSubView.Home }
                    )
                }

                MessagesSubView.NewMessage -> {
                    NewMessageScreen(
                        onBack = { currentSubView = MessagesSubView.Home },
                        onOpenChannel = { channelId ->
                            onDestinationChanged(ChatRouterDestination.Channel(channelId))
                            currentSubView = MessagesSubView.Home
                        },
                        onOpenAddFriends = { currentSubView = MessagesSubView.AddFriends }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FLOW 1: MESSAGES HOME
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagesHomeScreen(
    navController: NavController,
    useDrawer: Boolean,
    onDrawerClicked: () -> Unit,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenAddFriends: () -> Unit,
    onOpenNewMessage: () -> Unit
) {
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

    val pendingRequestsCount = remember(StoatAPI.userCache.values) {
        FriendRequests.getIncoming().size
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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

                // Discord Action Row: Search, Requests (Mail), Add Friends pill, Accent "+"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Search Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(ActionButtonShape)
                            .background(DiscordCardBg)
                            .clickable { onOpenSearch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_24dp),
                            contentDescription = "Search in DMs",
                            tint = DiscordHeader,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 2. Message Requests (Mail) Button with Badge Dot
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(ActionButtonShape)
                            .background(DiscordCardBg)
                            .clickable { onOpenRequests() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mail_24dp),
                            contentDescription = "Message Requests",
                            tint = DiscordHeader,
                            modifier = Modifier.size(20.dp)
                        )
                        if (pendingRequestsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp, end = 2.dp)
                                    .clip(CircleShape)
                                    .background(DiscordUnreadBadge)
                            )
                        }
                    }

                    // 3. Add Friends pill button
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(ActionButtonShape)
                            .background(DiscordCardBg)
                            .clickable { onOpenAddFriends() }
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

                    // 4. Accent square "+" button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(ActionButtonShape)
                            .background(DiscordBlurple)
                            .clickable { onOpenNewMessage() },
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
            }
        },
        containerColor = DiscordDarkBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 76.dp)
            ) {
                // ─── ACTIVE NOW AVATARS ───
                if (onlinePartners.isNotEmpty()) {
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

                item {
                    Spacer(Modifier.height(4.dp))
                }

                if (dmChannels.isEmpty()) {
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
                                    "No direct messages yet",
                                    color = DiscordTextMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = dmChannels,
                        key = { it.id ?: it.hashCode() }
                    ) { channel ->
                        DirectMessageItemRow(
                            channel = channel,
                            onClick = {
                                channel.id?.let {
                                    onDestinationChanged(ChatRouterDestination.Channel(it))
                                    if (useDrawer) onDrawerClicked()
                                }
                            },
                            onLongClick = {
                                channel.id?.let { contextSheetChannelId = it }
                            }
                        )
                    }
                }
            }

            // Pinned User Card at the bottom
            StoatUserCapsule(
                onOpenProfile = {
                    scope.launch {
                        StoatAPI.selfId?.let { ActionChannel.send(Action.OpenUserSheet(it, null)) }
                    }
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenNotifications = {
                    navController.navigate("settings/notifications")
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }

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

// ═══════════════════════════════════════════════════════════════════════════
// FLOW 2: SEARCH IN DMS
// ═══════════════════════════════════════════════════════════════════════════

private enum class SearchTab(val label: String) {
    Recent("Recent"),
    People("People"),
    Media("Media"),
    Pins("Pins"),
    Links("Links"),
    Files("Files")
}

@Composable
private fun DMsSearchScreen(
    onBack: () -> Unit,
    onOpenDM: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(SearchTab.Recent) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var selectedSort by rememberSaveable { mutableStateOf("Relevance") }

    LaunchedEffect(query) {
        delay(300L)
        debouncedQuery = query
    }

    val allFriends = remember(StoatAPI.userCache.values) {
        FriendRequests.getFriends()
    }

    val dmChannels = remember(StoatAPI.channelCache.values) {
        StoatAPI.channelCache.values
            .filter { it.channelType == ChannelType.DirectMessage || it.channelType == ChannelType.Group }
            .filter { if (it.channelType == ChannelType.DirectMessage) it.active == true else true }
            .sortedByDescending { it.lastMessageID ?: it.id }
    }

    val filteredPeople = remember(allFriends, dmChannels, debouncedQuery) {
        val trimmed = debouncedQuery.trim()
        val users = mutableListOf<User>()
        // Friends
        users.addAll(allFriends)
        // DM partners
        dmChannels.forEach { ch ->
            if (ch.channelType == ChannelType.DirectMessage) {
                val partnerId = ChannelUtils.resolveDMPartner(ch)
                StoatAPI.userCache[partnerId]?.let { partner ->
                    if (!users.any { it.id == partner.id }) users.add(partner)
                }
            }
        }
        if (trimmed.isEmpty()) {
            users
        } else {
            users.filter { user ->
                user.displayName?.contains(trimmed, ignoreCase = true) == true ||
                user.username?.contains(trimmed, ignoreCase = true) == true
            }
        }
    }

    val groupedPeople = remember(filteredPeople) {
        groupUsersAlphabetically(filteredPeople)
    }

    // Cached attachments in memory for Media/Links/Files
    val cachedMedia = remember(debouncedQuery) {
        StoatAPI.messageCache.values
            .filter { msg ->
                val channel = StoatAPI.channelCache[msg.channel]
                channel != null && (channel.channelType == ChannelType.DirectMessage || channel.channelType == ChannelType.Group)
            }
            .flatMap { it.attachments ?: emptyList() }
            .filter { it.metadata?.type == "Image" || it.contentType?.startsWith("image/") == true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordDarkBg)
    ) {
        // Top Search Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_24dp),
                    contentDescription = "Back",
                    tint = DiscordHeader
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(DiscordCardBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search in DMs",
                                color = DiscordTextMuted,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = DiscordHeader,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(DiscordBlurple),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close_24dp),
                                contentDescription = "Clear",
                                tint = DiscordTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Filter Icon Button
            Box {
                IconButton(onClick = { filterMenuExpanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_adjust_24dp),
                        contentDescription = "Filter",
                        tint = DiscordHeader
                    )
                }

                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort: Relevance ${if (selectedSort == "Relevance") "✓" else ""}") },
                        onClick = {
                            selectedSort = "Relevance"
                            filterMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort: Latest ${if (selectedSort == "Latest") "✓" else ""}") },
                        onClick = {
                            selectedSort = "Latest"
                            filterMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort: Oldest ${if (selectedSort == "Oldest") "✓" else ""}") },
                        onClick = {
                            selectedSort = "Oldest"
                            filterMenuExpanded = false
                        }
                    )
                    HorizontalDivider()
                    // Unsupported server filters disabled per no-invention rule
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Advanced filters (date, author) require server indexing",
                                fontSize = 11.sp,
                                color = DiscordTextMuted
                            )
                        },
                        enabled = false,
                        onClick = {}
                    )
                }
            }
        }

        // Horizontal Scrollable Tabs with Accent Underline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
        ) {
            SearchTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab.label,
                        color = if (isSelected) DiscordHeader else DiscordTextMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(28.dp)
                            .background(if (isSelected) DiscordBlurple else Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp)

        // Tab Content
        when (selectedTab) {
            SearchTab.Recent -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = "SUGGESTED PEOPLE",
                            color = DiscordTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(filteredPeople.take(6), key = { it.id ?: it.username ?: it.hashCode() }) { user ->
                        FriendSearchRow(
                            user = user,
                            onClick = {
                                val partnerId = user.id ?: return@FriendSearchRow
                                val dm = dmChannels.firstOrNull { ch ->
                                    ChannelUtils.resolveDMPartner(ch) == partnerId
                                }
                                dm?.id?.let { onOpenDM(it) }
                            }
                        )
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "PHOTOS & MEDIA",
                                color = DiscordTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "View all",
                                color = DiscordBlurple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { selectedTab = SearchTab.Media }
                            )
                        }
                    }

                    if (cachedMedia.isEmpty()) {
                        item {
                            Text(
                                text = "No media cached in recent direct messages.",
                                color = DiscordTextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cachedMedia.take(3).forEach {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DiscordCardBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_photo_library_24dp),
                                            contentDescription = null,
                                            tint = DiscordTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SearchTab.People -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    groupedPeople.forEach { (initial, peopleList) ->
                        item(key = "header_$initial") {
                            Text(
                                text = initial.toString(),
                                color = DiscordTextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp)
                            )
                        }
                        items(peopleList, key = { it.id ?: it.username ?: it.hashCode() }) { user ->
                            FriendSearchRow(
                                user = user,
                                onClick = {
                                    val partnerId = user.id ?: return@FriendSearchRow
                                    val dm = dmChannels.firstOrNull { ch ->
                                        ChannelUtils.resolveDMPartner(ch) == partnerId
                                    }
                                    dm?.id?.let { onOpenDM(it) }
                                }
                            )
                        }
                    }
                }
            }

            SearchTab.Media -> {
                if (cachedMedia.isEmpty()) {
                    SearchEmptyState(
                        icon = R.drawable.ic_photo_library_24dp,
                        message = "No media found in direct messages.",
                        hint = "Global media search across all DMs is not indexed server-side."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(cachedMedia, key = { it.id ?: it.hashCode() }) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DiscordCardBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_photo_library_24dp),
                                    contentDescription = null,
                                    tint = DiscordTextMuted
                                )
                            }
                        }
                    }
                }
            }

            SearchTab.Pins -> {
                SearchEmptyState(
                    icon = R.drawable.ic_keep_24dp,
                    message = "No pinned messages match your search.",
                    hint = "Pins are indexed inside individual channels."
                )
            }

            SearchTab.Links -> {
                SearchEmptyState(
                    icon = R.drawable.ic_link_24dp,
                    message = "No links found in direct messages.",
                    hint = "Global link indexing requires server-side support."
                )
            }

            SearchTab.Files -> {
                SearchEmptyState(
                    icon = R.drawable.ic_attach_file_24dp,
                    message = "No files found in direct messages.",
                    hint = "Global file indexing requires server-side support."
                )
            }
        }
    }
}

@Composable
private fun SearchEmptyState(icon: Int, message: String, hint: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = DiscordTextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = DiscordHeader,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = hint,
                color = DiscordTextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FriendSearchRow(user: User, onClick: () -> Unit) {
    val name = User.resolveDefaultName(user)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        UserAvatar(
            username = name,
            presence = presenceFromStatus(user.status?.presence, user.online == true),
            userId = user.id ?: "",
            avatar = user.avatar,
            size = 40.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = DiscordHeader,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!user.username.isNullOrBlank() && user.username != name) {
                Text(
                    text = "@${user.username}",
                    color = DiscordTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FLOW 3: MESSAGE REQUESTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MessageRequestsScreen(
    onBack: () -> Unit,
    onOpenDM: (String) -> Unit
) {
    var selectedSegment by rememberSaveable { mutableStateOf("Requests") }
    val incomingRequests = remember(StoatAPI.userCache.values) {
        FriendRequests.getIncoming()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordDarkBg)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_24dp),
                    contentDescription = "Back",
                    tint = DiscordHeader
                )
            }
            Text(
                text = "Message Requests",
                color = DiscordHeader,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Segmented Control: Requests | Spam
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(DiscordCardBg, RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedSegment == "Requests") DiscordDarkBg else Color.Transparent)
                    .clickable { selectedSegment = "Requests" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Requests (${incomingRequests.size})",
                    color = if (selectedSegment == "Requests") DiscordHeader else DiscordTextMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedSegment == "Spam") DiscordDarkBg else Color.Transparent)
                    .clickable { selectedSegment = "Spam" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Spam",
                    color = if (selectedSegment == "Spam") DiscordHeader else DiscordTextMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (selectedSegment == "Requests") {
            if (incomingRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mail_24dp),
                            contentDescription = null,
                            tint = DiscordTextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "There are no pending message requests.",
                            color = DiscordTextMuted,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(incomingRequests, key = { it.id ?: "" }) { user ->
                        val name = User.resolveDefaultName(user)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            UserAvatar(
                                username = name,
                                presence = presenceFromStatus(user.status?.presence, user.online == true),
                                userId = user.id ?: "",
                                avatar = user.avatar,
                                size = 44.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    color = DiscordHeader,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "@${user.username ?: "user"}",
                                    color = DiscordTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            // Accept Button (Check)
                            IconButton(
                                onClick = {
                                    user.id?.let { uId ->
                                        scope.launch {
                                            runCatching { acceptFriendRequest(uId) }
                                                .onSuccess {
                                                    Toast.makeText(context, "Accepted $name", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF23A55A).copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24dp),
                                    contentDescription = "Accept",
                                    tint = DiscordGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            // Decline Button (X)
                            IconButton(
                                onClick = {
                                    user.id?.let { uId ->
                                        scope.launch {
                                            runCatching { unfriendUser(uId) }
                                                .onSuccess {
                                                    Toast.makeText(context, "Declined $name", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF23F43).copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close_24dp),
                                    contentDescription = "Decline",
                                    tint = Color(0xFFF23F43),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Spam Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "SPAM REQUESTS - 0",
                    color = DiscordTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mail_24dp),
                            contentDescription = null,
                            tint = DiscordTextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "There are no pending message requests.",
                            color = DiscordTextMuted,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Note: Spam categorization is not supported server-side.",
                            color = DiscordTextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FLOW 4: ADD FRIENDS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AddFriendsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var usernameQuery by rememberSaveable { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val selfUser = StoatAPI.userCache[StoatAPI.selfId]

    val scanQrLauncher = rememberLauncherForActivityResult(ScanQRCode()) { result ->
        if (result is QRResult.QRSuccess) {
            Toast.makeText(context, "QR scanned: ${result.content.rawValue}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordDarkBg)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_24dp),
                    contentDescription = "Back",
                    tint = DiscordHeader
                )
            }
            Text(
                text = "Add Friends",
                color = DiscordHeader,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Supported Share Actions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Share Link
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DiscordCardBg)
                    .clickable {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Add me on Stoat: @${selfUser?.username ?: ""}")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Profile"))
                    }
                    .padding(vertical = 14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ios_share_24dp),
                    contentDescription = null,
                    tint = DiscordHeader,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("Share", color = DiscordHeader, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Copy Username
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DiscordCardBg)
                    .clickable {
                        val handle = selfUser?.username ?: ""
                        clipboard.setText(AnnotatedString(handle))
                        Toast.makeText(context, "Username copied!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_content_copy_24dp),
                    contentDescription = null,
                    tint = DiscordHeader,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("Copy Handle", color = DiscordHeader, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Scan QR
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DiscordCardBg)
                    .clickable { scanQrLauncher.launch(null) }
                    .padding(vertical = 14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_qr_code_scanner_24dp),
                    contentDescription = null,
                    tint = DiscordHeader,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("Scan QR", color = DiscordHeader, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(24.dp))

        // "Add by Username" Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "ADD BY USERNAME",
                color = DiscordTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = usernameQuery,
                onValueChange = {
                    usernameQuery = it
                    feedbackMessage = null
                },
                placeholder = {
                    Text("Enter a Username or tag", color = DiscordTextMuted, fontSize = 14.sp)
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
                trailingIcon = {
                    if (usernameQuery.isNotEmpty()) {
                        IconButton(onClick = { usernameQuery = "" }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close_24dp),
                                contentDescription = "Clear",
                                tint = DiscordTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            if (feedbackMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = feedbackMessage!!,
                    color = if (isError) Color(0xFFF23F43) else DiscordGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (usernameQuery.isNotBlank() && !isSending) DiscordBlurple else DiscordBlurple.copy(alpha = 0.5f))
                    .clickable(enabled = usernameQuery.isNotBlank() && !isSending) {
                        isSending = true
                        scope.launch {
                            try {
                                friendUser(usernameQuery.trim())
                                isError = false
                                feedbackMessage = "Friend request sent to ${usernameQuery.trim()}!"
                                usernameQuery = ""
                            } catch (e: Exception) {
                                isError = true
                                feedbackMessage = e.message ?: "Failed to send friend request"
                            } finally {
                                isSending = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Send Friend Request",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FLOW 5: NEW MESSAGE ("+")
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun NewMessageScreen(
    onBack: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenAddFriends: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isGroupMode by rememberSaveable { mutableStateOf(false) }
    val selectedUsers = remember { mutableStateListOf<String>() }
    var groupName by rememberSaveable { mutableStateOf("") }
    var isCreatingGroup by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val allFriends = remember(StoatAPI.userCache.values) {
        FriendRequests.getFriends()
    }

    val filteredFriends = remember(allFriends, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            allFriends
        } else {
            allFriends.filter { friend ->
                friend.displayName?.contains(trimmed, ignoreCase = true) == true ||
                friend.username?.contains(trimmed, ignoreCase = true) == true
            }
        }
    }

    val groupedFriends = remember(filteredFriends) {
        groupUsersAlphabetically(filteredFriends)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordDarkBg)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_24dp),
                    contentDescription = "Back",
                    tint = DiscordHeader
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "New Message",
                    color = DiscordHeader,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedUsers.size}/$MAX_ADDABLE_PEOPLE_IN_GROUP selected",
                    color = DiscordTextMuted,
                    fontSize = 12.sp
                )
            }

            if (isGroupMode && selectedUsers.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DiscordBlurple)
                        .clickable(enabled = !isCreatingGroup) {
                            isCreatingGroup = true
                            scope.launch {
                                try {
                                    val name = groupName.ifBlank { "Group" }
                                    val group = createGroupDM(name, selectedUsers.toList())
                                    group.id?.let { onOpenChannel(it) }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isCreatingGroup = false
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isCreatingGroup) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Create", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Live "To:" Search Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .height(44.dp)
                .background(DiscordCardBg, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "To: ",
                    color = DiscordTextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search your friends",
                            color = DiscordTextMuted,
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = DiscordHeader, fontSize = 14.sp),
                        cursorBrush = SolidColor(DiscordBlurple),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_24dp),
                            contentDescription = "Clear",
                            tint = DiscordTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Group name field if in group mode
        if (isGroupMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(40.dp)
                    .background(DiscordCardBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    singleLine = true,
                    textStyle = TextStyle(color = DiscordHeader, fontSize = 13.sp),
                    cursorBrush = SolidColor(DiscordBlurple),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (groupName.isEmpty()) {
                            Text("Group Name (optional)", color = DiscordTextMuted, fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Virtualized List
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (searchQuery.isEmpty()) {
                // "New Group" Action Row
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGroupMode = !isGroupMode }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DiscordCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_group_add_24dp),
                                contentDescription = null,
                                tint = if (isGroupMode) DiscordBlurple else DiscordHeader,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isGroupMode) "Cancel Group Mode" else "New Group",
                            color = DiscordHeader,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
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

                // "Add a Friend" Action Row
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAddFriends() }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DiscordCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_person_add_24dp),
                                contentDescription = null,
                                tint = DiscordHeader,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Add a Friend",
                            color = DiscordHeader,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
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

                item {
                    HorizontalDivider(color = DiscordDivider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            // Alphabetically Sectioned Friends List
            groupedFriends.forEach { (initial, friends) ->
                item(key = "letter_$initial") {
                    Text(
                        text = initial.toString(),
                        color = DiscordTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                }

                items(friends, key = { it.id ?: "" }) { friend ->
                    val friendId = friend.id ?: return@items
                    val name = User.resolveDefaultName(friend)
                    val isSelected = selectedUsers.contains(friendId)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isGroupMode) {
                                    if (isSelected) selectedUsers.remove(friendId)
                                    else if (selectedUsers.size < MAX_ADDABLE_PEOPLE_IN_GROUP) {
                                        selectedUsers.add(friendId)
                                    }
                                } else {
                                    // Single tap opens/creates DM
                                    scope.launch {
                                        try {
                                            val dm = openDM(friendId)
                                            dm.id?.let { onOpenChannel(it) }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open DM: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        UserAvatar(
                            username = name,
                            presence = presenceFromStatus(friend.status?.presence, friend.online == true),
                            userId = friendId,
                            avatar = friend.avatar,
                            size = 42.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                color = DiscordHeader,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            if (!friend.username.isNullOrBlank() && friend.username != name) {
                                Text(
                                    text = "@${friend.username}",
                                    color = DiscordTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (isGroupMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (selectedUsers.size < MAX_ADDABLE_PEOPLE_IN_GROUP) selectedUsers.add(friendId)
                                    } else {
                                        selectedUsers.remove(friendId)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = DiscordBlurple,
                                    checkmarkColor = Color.White,
                                    uncheckedColor = DiscordTextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SHARED REUSABLE COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

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
            .clip(FriendCardShape)
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
                    .border(2.dp, DiscordCardBg, CircleShape)
                    .background(statusDotColor, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectMessageItemRow(
    channel: Channel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val partner = remember(channel.id, channel.recipients) {
        if (channel.channelType == ChannelType.DirectMessage) {
            val partnerId = ChannelUtils.resolveDMPartner(channel)
            StoatAPI.userCache[partnerId]
        } else null
    }

    val isBlocked = partner?.relationship == "Blocked"

    val name = remember(partner?.id, partner?.username, channel.name) {
        partner?.let { User.resolveDefaultName(it) } ?: channel.name ?: "Unknown"
    }

    val lastMessage = channel.lastMessageID?.let { StoatAPI.messageCache[it] }
    val previewText = remember(lastMessage?.id, lastMessage?.content, lastMessage?.attachments, isBlocked) {
        when {
            isBlocked -> "Blocked message"
            lastMessage != null -> {
                val prefix = if (lastMessage.author == StoatAPI.selfId) "You: " else ""
                val content = lastMessage.content?.takeIf { it.isNotBlank() }
                    ?: if (lastMessage.attachments?.isNotEmpty() == true) "📷 Attachment" else ""
                "$prefix$content"
            }
            else -> ""
        }
    }

    val isUnread = channel.id?.let { chId ->
        channel.lastMessageID?.let { msgId ->
            StoatAPI.unreads.hasUnread(chId, msgId, serverId = null)
        }
    } ?: false

    val relativeTimestamp = remember(channel.lastMessageID) {
        formatRelativeTime(channel.lastMessageID)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(ActionButtonShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .alpha(if (isBlocked) 0.5f else 1f)
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
