package chat.stoat.composables.screens.chat.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.CategorisedChannelList
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.internals.DirectMessages
import chat.stoat.api.internals.FriendRequests
import chat.stoat.api.routes.user.addUserIfUnknown
import chat.stoat.api.settings.GeoStateProvider
import chat.stoat.api.settings.NotificationSettingsProvider
import chat.stoat.api.settings.ServerFolders
import chat.stoat.api.settings.ServerSidebarEntry
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.api.settings.resolveServerSidebar
import chat.stoat.composables.generic.GroupIcon
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.composables.screens.chat.ChannelIcon
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Category
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.ServerFlags
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.schemas.has
import chat.stoat.core.model.util.UserVoiceState
import chat.stoat.screens.chat.ChatRouterDestination
import chat.stoat.screens.chat.LocalIsConnected
import chat.stoat.sheets.ChannelContextSheet
import chat.stoat.sheets.UserInfoSheet
import androidx.compose.foundation.shape.RoundedCornerShape
import chat.stoat.sheets.ColourPickerSheet
import chat.stoat.sheets.ServerFolderSheet
import chat.stoat.sheets.colourPickerString
import chat.stoat.sheets.colourPickerValue
import chat.stoat.sheets.CategoryContextSheet
import chat.stoat.ui.theme.FragmentMono
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.ULID
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.server.createChannelInServer
import chat.stoat.api.routes.server.patchServer
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.ServerChannelListEntry
import chat.stoat.internals.server.ServerChannelSection
import chat.stoat.internals.server.UncategorisedChannelSectionId
import chat.stoat.internals.server.flattenChannelSections
import chat.stoat.internals.server.moveServerChannelEntry
import chat.stoat.internals.server.serverChannelSections
import chat.stoat.internals.server.toServerCategories
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelSideDrawer(
    currentServer: String?,
    currentDestination: ChatRouterDestination,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    onLongPressAvatar: () -> Unit,
    drawerState: DrawerState?,
    navigateToServer: (String) -> Unit,
    onShowServerContextSheet: (String) -> Unit,
    showSettingsIcon: Boolean,
    onOpenSettings: () -> Unit,
    topNav: NavController,
    onShowAddServerSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val server = StoatAPI.serverCache[currentServer]
    val categorisedChannels = remember(server, StoatAPI.channelCache.values) {
        server?.let {
            ChannelUtils.categoriseServerFlat(it)
        }
    }
    val channelListState = rememberLazyListState()

    LaunchedEffect(currentDestination) {
        if (currentDestination is ChatRouterDestination.Channel && currentServer != null) {
            val channelIndex = categorisedChannels?.indexOfFirst {
                when (it) {
                    is CategorisedChannelList.Channel -> it.channel.id == currentDestination.channelId
                    else -> false
                }
            } ?: 0
            val firstVisibleIndex = kotlin.math.max(0, channelIndex - 2)

            // Add an offset to the scroll position so it is obvious to the user that they are not at the top.
            channelListState.animateScrollToItem(
                firstVisibleIndex,
                if (firstVisibleIndex == 0) 0 else 85
            )
        }
    }

    val isAtFirst by remember { derivedStateOf { channelListState.firstVisibleItemIndex == 0 } }
    val serverBannerHeight by animateDpAsState(
        targetValue = if (server?.banner == null) {
            76.dp // Magic number deducted by trial and error
        } else if (isAtFirst) {
            192.dp
        } else {
            128.dp
        },
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = 0
        ), label = "Server banner height"
    )

    val serverInfoOffset by animateDpAsState(
        if (LocalIsConnected.current)
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        else
            0.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Dp.VisibilityThreshold
        )
    )

    val sidebarEntries = remember(
        StoatAPI.serverCache.values,
        SyncedSettings.ordering,
        SyncedSettings.serverFolders.folders
    ) {
        resolveServerSidebar(
            servers = StoatAPI.serverCache.filterValues { it.id != null },
            ordering = SyncedSettings.ordering,
            folders = SyncedSettings.serverFolders.folders,
        )
    }

    val railRows = remember(sidebarEntries) { sidebarEntries.toRailRows() }
    val railListState = rememberLazyListState()
    val railDragState = remember(railListState) { RailDragState(railListState, stickyHeaderKey = "self") }
    SideEffect { railDragState.rows = railRows }
    val haptics = LocalHapticFeedback.current
    val folderGroupLayout = remember { FolderGroupLayout() }
    val railOverscroll = rememberOverscrollEffect()
    val defaultFolderGroupColour = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val folderGroupStyles = remember(sidebarEntries, defaultFolderGroupColour) {
        sidebarEntries
            .filterIsInstance<ServerSidebarEntry.Folder>()
            .associate { entry ->
                entry.id to FolderGroupStyle(
                    colour = entry.folder.colour?.let(::parseFolderColour)?.copy(alpha = 0.18f)
                        ?: defaultFolderGroupColour,
                    lastMemberKey = entry.servers.last().id!!
                )
            }
    }
    val unreadDMs by remember { derivedStateOf { DirectMessages.unreadDMs() } }
    val newFolderName = stringResource(R.string.server_folder_default_name)

    val scope = rememberCoroutineScope()
    var serverFolderSheetTarget by remember { mutableStateOf<String?>(null) }
    var serverFolderColourTarget by remember { mutableStateOf<String?>(null) }

    serverFolderSheetTarget?.let { folderId ->
        val serverFolderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = serverFolderSheetState,
            onDismissRequest = {
                serverFolderSheetTarget = null
            }
        ) {
            ServerFolderSheet(
                folderId = folderId,
                onHideSheet = {
                    serverFolderSheetState.hide()
                    serverFolderSheetTarget = null
                },
                onChangeColour = {
                    serverFolderSheetState.hide()
                    serverFolderSheetTarget = null
                    serverFolderColourTarget = folderId
                }
            )
        }
    }

    serverFolderColourTarget?.let { folderId ->
        val colourSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val folder = ServerFolders.folders.firstOrNull { it.id == folderId }
        val hideColourSheet: () -> Unit = {
            scope.launch {
                colourSheetState.hide()
                serverFolderColourTarget = null
            }
        }

        ModalBottomSheet(
            sheetState = colourSheetState,
            onDismissRequest = {
                serverFolderColourTarget = null
            }
        ) {
            ColourPickerSheet(
                initialValue = colourPickerValue(
                    folder?.colour,
                    MaterialTheme.colorScheme.primary.toArgb()
                ),
                onColourSelected = { colour ->
                    folder?.let { ServerFolders.edit(it.id, it.name, colourPickerString(colour)) }
                    hideColourSheet()
                },
                onUseDefaultColour = {
                    folder?.let { ServerFolders.edit(it.id, it.name, null) }
                    hideColourSheet()
                },
                onDismiss = hideColourSheet
            )
        }
    }

    var channelContextSheetTarget by remember { mutableStateOf<String?>(null) }
    var categoryContextSheetTarget by remember { mutableStateOf<String?>(null) }
    var createChannelTargetCategory by remember { mutableStateOf<String?>(null) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showEmptySpaceContextMenu by remember { mutableStateOf(false) }
    var newChannelName by remember { mutableStateOf("") }
    var newChannelType by remember { mutableStateOf("Text") }
    var newCategoryName by remember { mutableStateOf("") }
    var collapsedCategoryIds by remember(currentServer) { mutableStateOf(setOf<String>()) }
    val serverPermissions by rememberServerPermissions(currentServer.orEmpty())
    val canManageServerChannels = currentServer != null && (StoatAPI.serverCache[currentServer]?.owner == StoatAPI.selfId || (serverPermissions != null && (serverPermissions!!.hasPermission(PermissionBit.ManageChannel) || serverPermissions!!.hasPermission(PermissionBit.ManageServer))))
    var showSelfProfileSheet by remember { mutableStateOf(false) }

    if (showSelfProfileSheet && StoatAPI.selfId != null) {
        val selfSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = selfSheetState,
            dragHandle = null,
            containerColor = Color.Transparent,
            onDismissRequest = { showSelfProfileSheet = false },
        ) {
            UserInfoSheet(
                userId = StoatAPI.selfId!!,
                serverId = currentServer,
                dismissSheet = {
                    selfSheetState.hide()
                    showSelfProfileSheet = false
                }
            )
        }
    }

    if (channelContextSheetTarget != null) {
        val channelContextSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = channelContextSheetState,
            onDismissRequest = {
                channelContextSheetTarget = null
            }
        ) {
            ChannelContextSheet(
                channelId = channelContextSheetTarget!!,
                onHideSheet = {
                    channelContextSheetState.hide()
                    channelContextSheetTarget = null
                }
            )
        }
    }

    if (categoryContextSheetTarget != null && currentServer != null) {
        val catSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            sheetState = catSheetState,
            onDismissRequest = { categoryContextSheetTarget = null }
        ) {
            CategoryContextSheet(
                serverId = currentServer,
                categoryId = categoryContextSheetTarget!!,
                onHideSheet = {
                    catSheetState.hide()
                    categoryContextSheetTarget = null
                },
                isCollapsed = categoryContextSheetTarget in collapsedCategoryIds,
                onToggleCollapse = {
                    val id = categoryContextSheetTarget ?: return@CategoryContextSheet
                    collapsedCategoryIds = if (id in collapsedCategoryIds) {
                        collapsedCategoryIds - id
                    } else {
                        collapsedCategoryIds + id
                    }
                },
                onOpenCreateChannel = {
                    createChannelTargetCategory = categoryContextSheetTarget
                    showCreateChannelDialog = true
                }
            )
        }
    }

    if (showCreateCategoryDialog && currentServer != null) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = {
                showCreateCategoryDialog = false
                newCategoryName = ""
            },
            containerColor = Color(0xFF2B2D31),
            title = {
                Text(
                    text = "Create Category",
                    color = Color(0xFFF2F3F5),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name", color = Color(0xFF949BA4)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newCategoryName.isNotBlank()) {
                                scope.launch {
                                    val srv = StoatAPI.serverCache[currentServer]
                                    if (srv != null) {
                                        val sections = serverChannelSections(srv) + ServerChannelSection(
                                            id = ULID.makeNext(),
                                            title = newCategoryName.trim(),
                                            channelIds = emptyList()
                                        )
                                        patchServer(currentServer, categories = sections.toServerCategories())
                                    }
                                    newCategoryName = ""
                                    showCreateCategoryDialog = false
                                }
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFF2F3F5),
                        unfocusedTextColor = Color(0xFFF2F3F5),
                        focusedBorderColor = Color(0xFF5865F2),
                        unfocusedBorderColor = Color(0xFF949BA4),
                        cursorColor = Color(0xFF5865F2)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            scope.launch {
                                val srv = StoatAPI.serverCache[currentServer]
                                if (srv != null) {
                                    val sections = serverChannelSections(srv) + ServerChannelSection(
                                        id = ULID.makeNext(),
                                        title = newCategoryName.trim(),
                                        channelIds = emptyList()
                                    )
                                    patchServer(currentServer, categories = sections.toServerCategories())
                                }
                                newCategoryName = ""
                                showCreateCategoryDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Create Category", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateCategoryDialog = false
                    newCategoryName = ""
                }) {
                    Text("Cancel", color = Color(0xFF949BA4))
                }
            }
        )
    }

    if (showCreateChannelDialog && currentServer != null) {
        val channelFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            channelFocusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = {
                showCreateChannelDialog = false
                newChannelName = ""
                createChannelTargetCategory = null
            },
            containerColor = Color(0xFF2B2D31),
            title = {
                Text(
                    text = "Create Channel",
                    color = Color(0xFFF2F3F5),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1F22)),
                    ) {
                        listOf("Text" to R.drawable.ic_tag_24dp, "Voice" to R.drawable.ic_volume_up_24dp).forEach { (type, icon) ->
                            val selected = newChannelType == type
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Color(0xFF5865F2) else Color.Transparent)
                                    .clickable { newChannelType = type }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    tint = if (selected) Color.White else Color(0xFF949BA4),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = type,
                                    color = if (selected) Color.White else Color(0xFF949BA4),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newChannelName,
                        onValueChange = { newChannelName = it },
                        label = { Text("Channel Name", color = Color(0xFF949BA4)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newChannelName.isNotBlank()) {
                                    scope.launch {
                                        createChannelInServer(
                                            serverId = currentServer,
                                            name = newChannelName.trim(),
                                            type = newChannelType,
                                            categoryId = createChannelTargetCategory
                                        )
                                        newChannelName = ""
                                        createChannelTargetCategory = null
                                        showCreateChannelDialog = false
                                    }
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(channelFocusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFF2F3F5),
                            unfocusedTextColor = Color(0xFFF2F3F5),
                            focusedBorderColor = Color(0xFF5865F2),
                            unfocusedBorderColor = Color(0xFF949BA4),
                            cursorColor = Color(0xFF5865F2)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChannelName.isNotBlank()) {
                            scope.launch {
                                createChannelInServer(
                                    serverId = currentServer,
                                    name = newChannelName.trim(),
                                    type = newChannelType,
                                    categoryId = createChannelTargetCategory
                                )
                                newChannelName = ""
                                createChannelTargetCategory = null
                                showCreateChannelDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                    enabled = newChannelName.isNotBlank()
                ) {
                    Text("Create Channel", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateChannelDialog = false
                    newChannelName = ""
                    createChannelTargetCategory = null
                }) {
                    Text("Cancel", color = Color(0xFF949BA4))
                }
            }
        )
    }

    if (showEmptySpaceContextMenu && currentServer != null) {
        val emptySheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            sheetState = emptySheetState,
            onDismissRequest = { showEmptySpaceContextMenu = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                if (canManageServerChannels) {
                    chat.stoat.composables.generic.SheetButton(
                        headlineContent = { Text("Create Channel") },
                        leadingContent = {
                            Icon(painter = painterResource(R.drawable.ic_add_24dp), contentDescription = null)
                        },
                        onClick = {
                            scope.launch {
                                emptySheetState.hide()
                                showEmptySpaceContextMenu = false
                                createChannelTargetCategory = null
                                showCreateChannelDialog = true
                            }
                        }
                    )
                    chat.stoat.composables.generic.SheetButton(
                        headlineContent = { Text("Create Category") },
                        leadingContent = {
                            Icon(painter = painterResource(R.drawable.ic_folder_24dp), contentDescription = null)
                        },
                        onClick = {
                            scope.launch {
                                emptySheetState.hide()
                                showEmptySpaceContextMenu = false
                                showCreateCategoryDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    Row(modifier.fillMaxSize()) {
        Box(
            Modifier
                .width(64.dp)
                .fillMaxHeight()
                .background(Color(0xFF1E1F22))
                .zIndex(1f)
                .overscroll(railOverscroll)
                .folderGroupBackgrounds(folderGroupLayout, folderGroupStyles)
        ) {
            LazyColumn(
                state = railListState,
                overscrollEffect = railOverscroll?.withoutVisualEffect(),
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        railDragState.railWidthPx = it.width.toFloat()
                        railDragState.railHeightPx = it.height.toFloat()
                    }
                    .railDragGestures(
                        state = railDragState,
                        haptics = haptics,
                        onLongPress = { row ->
                            when (row) {
                                is RailRow.ServerRow -> onShowServerContextSheet(row.key)
                                is RailRow.FolderRow -> serverFolderSheetTarget = row.key
                            }
                        },
                        onDrop = { key, intent ->
                            when (intent) {
                                is RailIntent.Fold -> ServerFolders.fold(
                                    entries = sidebarEntries,
                                    target = intent.target,
                                    incoming = key,
                                    newFolderName = newFolderName
                                )

                                is RailIntent.Move -> ServerFolders.move(
                                    entries = sidebarEntries,
                                    moved = key,
                                    before = intent.before,
                                    parent = intent.parent
                                )
                            }
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            ) {
                stickyHeader(key = "self") {
                    Column(
                        Modifier
                            .background(Color(0xFF1E1F22))
                            .padding(bottom = RailItemGap)
                    ) {
                        AnimatedVisibility(LocalIsConnected.current) {
                            Spacer(
                                Modifier
                                    .height(
                                        WindowInsets.statusBars.asPaddingValues()
                                            .calculateTopPadding()
                                    )
                            )
                        }
                        UserAvatar(
                            username = StoatAPI.userCache[StoatAPI.selfId]?.let {
                                User.resolveDefaultName(
                                    it
                                )
                            }
                                ?: "",
                            presence = presenceFromStatus(
                                StoatAPI.userCache[StoatAPI.selfId]?.status?.presence,
                                StoatAPI.userCache[StoatAPI.selfId]?.online ?: false
                            ),
                            userId = StoatAPI.selfId ?: "",
                            avatar = StoatAPI.userCache[StoatAPI.selfId]?.avatar,
                            size = 48.dp,
                            presenceSize = 16.dp,
                            onClick = {
                                onDestinationChanged(ChatRouterDestination.defaultForDMList)
                            },
                            onLongClick = onLongPressAvatar,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(48.dp)
                        )
                    }
                }

                items(
                    items = unreadDMs,
                    key = { it.id ?: it.hashCode() }
                ) { dm ->
                    when (dm.channelType) {
                        ChannelType.Group -> GroupIcon(
                            name = dm.name ?: "?",
                            size = 48.dp,
                            onClick = {
                                dm.id?.let { id ->
                                    onDestinationChanged(ChatRouterDestination.Channel(id))
                                }
                            },
                            icon = dm.icon,
                            modifier = Modifier
                                .padding(bottom = RailItemGap)
                                .padding(8.dp)
                                .size(48.dp)
                        )

                        else -> {
                            val partner =
                                if (dm.channelType == ChannelType.DirectMessage) {
                                    StoatAPI.userCache[
                                        ChannelUtils.resolveDMPartner(
                                            dm
                                        )
                                    ]
                                } else {
                                    null
                                }

                            UserAvatar(
                                username = partner?.let { p ->
                                    User.resolveDefaultName(
                                        p
                                    )
                                } ?: dm.name ?: "?",
                                presence = presenceFromStatus(
                                    partner?.status?.presence,
                                    partner?.online ?: false
                                ),
                                userId = partner?.id ?: dm.id ?: "",
                                avatar = partner?.avatar ?: dm.icon,
                                size = 48.dp,
                                presenceSize = 16.dp,
                                onClick = {
                                    dm.id?.let { id ->
                                        onDestinationChanged(ChatRouterDestination.Channel(id))
                                    }
                                },
                                modifier = Modifier
                                    .padding(bottom = RailItemGap)
                                    .padding(8.dp)
                                    .size(48.dp)
                            )
                        }
                    }
                }

                item(key = "divider") {
                    HorizontalDivider(
                        Modifier
                            .padding(bottom = RailItemGap)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }

                items(railRows, key = { it.key }) { row ->
                    val foldTarget = (railDragState.intent as? RailIntent.Fold)?.target == row.key
                    val rowModifier = Modifier
                        .animateItem()
                        .alpha(if (railDragState.held == row.key) 0.35f else 1f)

                    when (row) {
                        is RailRow.ServerRow -> ServerRailIcon(
                            server = row.server,
                            selected = row.key == currentServer,
                            foldTarget = foldTarget,
                            onClick = {
                                navigateToServer(row.key)
                                scope.launch {
                                    drawerState?.close()
                                }
                            },
                            onLongClick = { onShowServerContextSheet(row.key) },
                            modifier = rowModifier
                                .then(
                                    row.parent?.let {
                                        Modifier.folderGroupMember(folderGroupLayout, row.key, it)
                                    } ?: Modifier
                                )
                                .padding(bottom = RailItemGap)
                        )

                        is RailRow.FolderRow -> FolderRailHeader(
                            entry = row.entry,
                            currentServer = currentServer,
                            foldTarget = foldTarget,
                            onToggle = { ServerFolders.toggle(row.key) },
                            onLongClick = { serverFolderSheetTarget = row.key },
                            modifier = rowModifier
                                .folderGroupMember(folderGroupLayout, row.key, row.key)
                                .padding(bottom = RailItemGap)
                        )
                    }
                }

                item(key = "add_server") {
                    Box(
                        Modifier
                            .padding(bottom = RailItemGap)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .clickable {
                                onShowAddServerSheet()
                            }
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
                            contentDescription = stringResource(R.string.server_plus_alt)
                        )
                    }
                }

                item(key = "discover") {
                    Box(
                        Modifier
                            .padding(bottom = RailItemGap)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .clickable {
                                topNav.navigate("discover")
                            }
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_explore_24dp),
                            contentDescription = stringResource(R.string.discover_alt)
                        )
                    }
                }

                if (showSettingsIcon) {
                    item(key = "settings") {
                        Box(
                            Modifier
                                .padding(bottom = RailItemGap)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onOpenSettings()
                                    scope.launch {
                                        drawerState?.close()
                                    }
                                }
                                .size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_24dp),
                                contentDescription = stringResource(R.string.settings)
                            )
                        }
                    }
                }
            }

            RailDragAutoScroll(railDragState, railListState)
            RailDragOverlay(railDragState, railListState) { row ->
                when (row) {
                    is RailRow.ServerRow -> ServerIconImage(row.server, Modifier.size(48.dp))
                    is RailRow.FolderRow -> FolderIcon(row.entry)
                }
            }
        }
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                Modifier
                    .clip(
                        MaterialTheme.shapes.medium.copy(
                            topStart = CornerSize(0.dp),
                            topEnd = CornerSize(0.dp)
                        )
                    )
                    .height(
                        serverBannerHeight + WindowInsets.statusBars.asPaddingValues()
                            .calculateTopPadding()
                    )
                //.offset(y = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            ) {
                if (server?.banner != null) {
                    RemoteImage(
                        url = "$STOAT_FILES/banners/${server.banner!!.id}/original",
                        description = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                    )

                    with(MaterialTheme.colorScheme) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    drawRect(
                                        Brush.linearGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.6f),
                                                Color.Transparent
                                            ),
                                            Offset.Zero,
                                            Offset.Infinite.copy(x = 0f)
                                        ),
                                    )
                                })
                    }
                }

                Row(
                    Modifier
                        .padding(16.dp)
                        .offset(y = serverInfoOffset),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                                if (server?.banner != null) Color.White
                                else LocalContentColor.current
                    ) {
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (server?.flags has ServerFlags.Official) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_workspace_premium_24dp__fill
                                    ),
                                    contentDescription = stringResource(
                                        R.string.server_flag_official
                                    ),
                                    tint = LocalContentColor.current,
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }
                            if (server?.flags has ServerFlags.Verified) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_verified_24dp__fill
                                    ),
                                    contentDescription = stringResource(
                                        R.string.server_flag_verified
                                    ),
                                    tint = LocalContentColor.current,
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }

                            Text(
                                text = when (currentServer) {
                                    null -> stringResource(R.string.direct_messages)
                                    else -> server?.name ?: stringResource(R.string.unknown)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (currentServer != null) {
                            IconButton(onClick = {
                                server?.id?.let { srvId -> onShowServerContextSheet(srvId) }
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert_24dp),
                                    contentDescription = stringResource(R.string.menu),
                                    tint = LocalContentColor.current
                                )
                            }
                        } else {
                            Spacer(Modifier.height(64.dp))
                        }
                    }
                }
            }

            if (currentServer == null) {
                DirectMessagesChannelListRenderer(
                    currentDestination,
                    onDestinationChanged,
                    drawerState,
                    channelListState,
                    onOpenChannelContextSheet = { channelContextSheetTarget = it }
                )
            } else {
                ServerChannelListRenderer(
                    categorisedChannels = categorisedChannels,
                    currentDestination = currentDestination,
                    onDestinationChanged = onDestinationChanged,
                    drawerState = drawerState,
                    channelListState = channelListState,
                    onOpenChannelContextSheet = { channelContextSheetTarget = it },
                    onOpenCategoryContextSheet = { categoryContextSheetTarget = it },
                    onOpenCreateChannelForCategory = {
                        createChannelTargetCategory = it
                        showCreateChannelDialog = true
                    },
                    onOpenEmptySpaceContextMenu = {
                        showEmptySpaceContextMenu = true
                    },
                    collapsedCategoryIds = collapsedCategoryIds,
                    onToggleCategory = { catId ->
                        collapsedCategoryIds = if (catId in collapsedCategoryIds) {
                            collapsedCategoryIds - catId
                        } else {
                            collapsedCategoryIds + catId
                        }
                    },
                    serverId = currentServer
                )
            }

            // Modern Discord Floating User Capsule
            StoatUserCapsule(
                onOpenProfile = { showSelfProfileSheet = true },
                onOpenSettings = onOpenSettings,
                onOpenNotifications = { onDestinationChanged(ChatRouterDestination.Overview) },
                modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 6.dp)
            )
        }
    }
}

@Composable
fun ColumnScope.DirectMessagesChannelListRenderer(
    currentDestination: ChatRouterDestination,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    drawerState: DrawerState?,
    channelListState: LazyListState,
    onOpenChannelContextSheet: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dmAbleChannels = remember(StoatAPI.channelCache.values) {
        StoatAPI.channelCache.values
            .filter { it.channelType == ChannelType.DirectMessage || it.channelType == ChannelType.Group }
            .filter { if (it.channelType == ChannelType.DirectMessage) it.active == true else true }
            .sortedByDescending { it.lastMessageID ?: it.id }
    }

    LazyColumn(
        state = channelListState,
        modifier = Modifier
            .fillMaxSize()
            .weight(1f)
    ) {
        item(key = "overview") {
            val isOverviewCurrent = currentDestination is ChatRouterDestination.Overview
            val overviewBg = if (isOverviewCurrent) Color(0xFF35373C) else Color.Transparent
            val overviewColor = if (isOverviewCurrent) Color(0xFFF2F3F5) else Color(0xFF949BA4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(overviewBg)
                    .clickable {
                        onDestinationChanged(ChatRouterDestination.Overview)
                        scope.launch { drawerState?.close() }
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chat_24dp),
                    contentDescription = null,
                    tint = overviewColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Direct Messages",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = overviewColor
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        item(key = "friends") {
            ChannelItem(
                channel = Channel(
                    id = "friends",
                    name = stringResource(R.string.friends),
                    channelType = ChannelType.TextChannel
                ),
                iconType = ChannelItemIconType.Painter(painterResource(R.drawable.ic_group_24dp)),
                isCurrent = currentDestination is ChatRouterDestination.Friends,
                onDestinationChanged = {
                    onDestinationChanged(ChatRouterDestination.Friends)
                    scope.launch {
                        drawerState?.close()
                    }
                },
                hasUnread = FriendRequests.getIncoming().isNotEmpty(),
                onOpenChannelContextSheet = {},
            )
            Spacer(Modifier.height(4.dp))
        }

        item(key = "saved_messages") {
            val notesChannel =
                StoatAPI.channelCache.values.firstOrNull { it.channelType == ChannelType.SavedMessages }

            if (notesChannel != null) {
                ChannelItem(
                    channel = Channel(
                        id = notesChannel.id,
                        name = stringResource(R.string.channel_notes),
                        channelType = ChannelType.SavedMessages
                    ),
                    isCurrent = currentDestination is ChatRouterDestination.Channel &&
                            currentDestination.channelId == notesChannel.id,
                    onDestinationChanged = {
                        onDestinationChanged(it)
                        scope.launch {
                            drawerState?.close()
                        }
                    },
                    hasUnread = false,
                    onOpenChannelContextSheet = {},
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        item("divider") {
            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
        }

        items(
            items = dmAbleChannels,
            key = { it.id ?: it.hashCode() }
        ) { channel ->

            val partner =
                if (channel.channelType == ChannelType.DirectMessage) {
                    StoatAPI.userCache[
                        ChannelUtils.resolveDMPartner(
                            channel
                        )
                    ]
                } else {
                    null
                }

            DMOrGroupItem(
                channel = channel,
                partner = partner,
                isCurrent = when (currentDestination) {
                    is ChatRouterDestination.Channel -> {
                        currentDestination.channelId == channel.id
                    }

                    else -> false
                },
                hasUnread = channel.lastMessageID?.let { lastMessageID ->
                    StoatAPI.unreads.hasUnread(
                        channel.id!!,
                        lastMessageID,
                        serverId = null
                    )
                } ?: false,
                isMuted = NotificationSettingsProvider.isChannelMuted(channel.id!!, null),
                onDestinationChanged = { dest ->
                    onDestinationChanged(dest)
                    scope.launch {
                        drawerState?.close()
                    }
                },
                onOpenChannelContextSheet = onOpenChannelContextSheet
            )
        }

        item(key = "last") {
            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.ServerChannelListRenderer(
    categorisedChannels: List<CategorisedChannelList>?,
    currentDestination: ChatRouterDestination,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    drawerState: DrawerState?,
    channelListState: LazyListState,
    onOpenChannelContextSheet: (String) -> Unit,
    onOpenCategoryContextSheet: (String) -> Unit,
    onOpenCreateChannelForCategory: (String) -> Unit,
    onOpenEmptySpaceContextMenu: () -> Unit,
    collapsedCategoryIds: Set<String>,
    onToggleCategory: (String) -> Unit,
    serverId: String
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val server = StoatAPI.serverCache[serverId]
    val permissions by rememberServerPermissions(serverId)
    val isOwner = server?.owner == StoatAPI.selfId
    val canManage = isOwner || (permissions != null && (permissions!!.hasPermission(PermissionBit.ManageChannel) || permissions!!.hasPermission(PermissionBit.ManageServer)))

    val sourceSections = remember(server) { server?.let { serverChannelSections(it) }.orEmpty() }
    var displayedSections by remember(serverId) { mutableStateOf(sourceSections) }
    LaunchedEffect(sourceSections) {
        displayedSections = sourceSections
    }

    var hasMovedDuringDrag by remember { mutableStateOf(false) }
    var dragStartSections by remember(serverId) { mutableStateOf<List<ServerChannelSection>?>(null) }
    var hoverJob by remember { mutableStateOf<Job?>(null) }
    var hoveredCategoryId by remember { mutableStateOf<String?>(null) }

    val entries = remember(displayedSections, collapsedCategoryIds) {
        displayedSections.flatMap { section ->
            val isUncategorized = section.id == UncategorisedChannelSectionId
            val sectionHeader = if (isUncategorized) emptyList() else listOf(ServerChannelListEntry.Section(section.id))
            val isCollapsed = !isUncategorized && section.id in collapsedCategoryIds
            if (isCollapsed) {
                sectionHeader
            } else {
                sectionHeader + section.channelIds.map { channelId ->
                    ServerChannelListEntry.Channel(section.id, channelId)
                }
            }
        }
    }

    val reorderableState = rememberReorderableLazyListState(channelListState) { from, to ->
        if (!canManage) return@rememberReorderableLazyListState
        val fullEntries = displayedSections.flattenChannelSections()
        val fullFromIndex = fullEntries.indexOfFirst { it.key == from.key }
        val fullToIndex = fullEntries.indexOfFirst { it.key == to.key }
        if (fullFromIndex < 0 || fullToIndex < 0 || fullFromIndex == fullToIndex) return@rememberReorderableLazyListState

        val moved = moveServerChannelEntry(displayedSections, fullFromIndex, fullToIndex)
        if (moved != displayedSections) {
            displayedSections = moved
            hasMovedDuringDrag = true
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)

            val toEntry = fullEntries[fullToIndex]
            val targetCatId = when (toEntry) {
                is ServerChannelListEntry.Section -> toEntry.sectionId
                is ServerChannelListEntry.Channel -> toEntry.sectionId
            }
            if (targetCatId in collapsedCategoryIds && targetCatId != UncategorisedChannelSectionId) {
                if (hoveredCategoryId != targetCatId) {
                    hoveredCategoryId = targetCatId
                    hoverJob?.cancel()
                    hoverJob = scope.launch {
                        delay(600)
                        onToggleCategory(targetCatId)
                        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    }
                }
            } else {
                hoveredCategoryId = null
                hoverJob?.cancel()
            }
        }
    }

    fun onStartDrag() {
        dragStartSections = displayedSections
        hasMovedDuringDrag = false
        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    fun onStopDrag(onHoldReleaseWithoutMove: () -> Unit) {
        hoverJob?.cancel()
        hoverJob = null
        hoveredCategoryId = null
        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
        if (!hasMovedDuringDrag) {
            onHoldReleaseWithoutMove()
        } else {
            val previous = dragStartSections ?: return
            if (displayedSections != previous) {
                scope.launch {
                    try {
                        patchServer(serverId, categories = displayedSections.toServerCategories())
                    } catch (e: Exception) {
                        displayedSections = previous
                        Toast.makeText(context, "Failed to reorder: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dragStartSections = null
        hasMovedDuringDrag = false
    }

    LazyColumn(
        state = channelListState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 8.dp),
        modifier = Modifier
            .fillMaxSize()
            .weight(1f)
    ) {
        if (entries.isEmpty()) {
            item {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_channels_heading),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_channels_body),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(
            items = entries,
            key = { it.key }
        ) { entry ->
            ReorderableItem(
                state = reorderableState,
                key = entry.key,
                enabled = true
            ) { isDragging ->
                when (entry) {
                    is ServerChannelListEntry.Section -> {
                        val section = displayedSections.firstOrNull { it.id == entry.sectionId }
                        if (section != null) {
                            val isCollapsed = section.id in collapsedCategoryIds
                            val dragModifier = Modifier.longPressDraggableHandle(
                                enabled = true,
                                onDragStarted = { onStartDrag() },
                                onDragStopped = {
                                    onStopDrag {
                                        onOpenCategoryContextSheet(section.id)
                                    }
                                }
                            )
                            CategoryItem(
                                category = Category(id = section.id, title = section.title, channels = section.channelIds),
                                isExpanded = !isCollapsed,
                                onToggle = { onToggleCategory(section.id) },
                                canManage = canManage,
                                onCreateChannel = { onOpenCreateChannelForCategory(section.id) },
                                modifier = dragModifier.then(
                                    if (isDragging) {
                                        Modifier
                                            .shadow(8.dp, RoundedCornerShape(4.dp))
                                            .scale(1.02f)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    } else {
                                        Modifier
                                    }
                                )
                            )
                        }
                    }

                    is ServerChannelListEntry.Channel -> {
                        val channel = StoatAPI.channelCache[entry.channelId]
                        if (channel != null) {
                            val dragModifier = Modifier.longPressDraggableHandle(
                                enabled = true,
                                onDragStarted = { onStartDrag() },
                                onDragStopped = {
                                    onStopDrag {
                                        onOpenChannelContextSheet(entry.channelId)
                                    }
                                }
                            )
                            ChannelItem(
                                channel = channel,
                                isCurrent = when (currentDestination) {
                                    is ChatRouterDestination.Channel -> {
                                        currentDestination.channelId == entry.channelId
                                    }
                                    else -> false
                                },
                                onDestinationChanged = {
                                    onDestinationChanged(it)
                                    scope.launch {
                                        drawerState?.close()
                                    }
                                },
                                hasUnread = channel.lastMessageID?.let { lastMessageID ->
                                    StoatAPI.unreads.hasUnread(
                                        entry.channelId,
                                        lastMessageID,
                                        serverId
                                    )
                                } ?: false,
                                isMuted = NotificationSettingsProvider.isChannelMuted(
                                    entry.channelId,
                                    serverId
                                ),
                                showVoiceParticipants = true,
                                onOpenChannelContextSheet = onOpenChannelContextSheet,
                                modifier = dragModifier.then(
                                    if (isDragging) {
                                        Modifier
                                            .shadow(8.dp, CircleShape)
                                            .scale(1.02f)
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                                    } else {
                                        Modifier
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }

        item(key = "empty_space_footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenEmptySpaceContextMenu
                    )
            )
        }

        item(key = "last") {
            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
                )
            )
        }
    }
}

sealed class ChannelItemIconType {
    data class Channel(val type: ChannelType) : ChannelItemIconType()
    data class Painter(val painter: androidx.compose.ui.graphics.painter.Painter) :
        ChannelItemIconType()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelItem(
    channel: Channel,
    isCurrent: Boolean,
    iconType: ChannelItemIconType = ChannelItemIconType.Channel(
        channel.channelType ?: ChannelType.TextChannel
    ),
    hasUnread: Boolean = false,
    isMuted: Boolean = false,
    appendServerName: Boolean = false,
    showVoiceParticipants: Boolean = false,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    onOpenChannelContextSheet: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(
        LocalContentColor provides if (isCurrent) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            if (hasUnread) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
    ) {
        val clickModifier = if (modifier != Modifier) {
            Modifier
                .then(modifier)
                .clickable {
                    channel.id?.let { chId ->
                        onDestinationChanged(ChatRouterDestination.Channel(chId))
                    }
                }
        } else {
            Modifier.combinedClickable(
                onLongClickLabel = stringResource(R.string.channel_context_sheet_open),
                onLongClick = {
                    channel.id?.let { chId ->
                        onOpenChannelContextSheet(chId)
                    }
                },
                onClick = {
                    channel.id?.let { chId ->
                        onDestinationChanged(ChatRouterDestination.Channel(chId))
                    }
                }
            )
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .clip(
                        CircleShape
                    )
                    .then(clickModifier)
                    .then(
                        if (isCurrent) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (isMuted) {
                            Modifier.alpha(0.5f)
                        } else {
                            Modifier
                        }
                    )
                    .padding(16.dp)
                    .fillMaxWidth()) {
                when (iconType) {
                    is ChannelItemIconType.Channel -> {
                        when {
                            GeoStateProvider.geoState?.isAgeRestrictedGeo == true &&
                                    channel.nsfw == true -> {
                                Icon(
                                    painter = painterResource(R.drawable.ic_grid_3x3_off_24dp),
                                    contentDescription = stringResource(R.string.geogate_channel_icon_alt),
                                )
                            }

                            channel.channelType == ChannelType.TextChannel && channel.voice != null -> {
                                ChannelIcon(channel = channel)
                            }

                            else -> ChannelIcon(iconType.type)
                        }
                    }

                    is ChannelItemIconType.Painter -> {
                        Icon(painter = iconType.painter, contentDescription = null)
                    }
                }
                Text(
                    text = (ChannelUtils.resolveName(channel) ?: stringResource(R.string.unknown))
                            + if (appendServerName && channel.server != null) {
                        " (${StoatAPI.serverCache[channel.server]?.name ?: stringResource(R.string.unknown)})"
                    } else {
                        ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (hasUnread && !isCurrent) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .requiredSize(8.dp)
                    )
                }
                channel.voice?.maxUsers?.let { maxUsers ->
                    val participantCount = channel.id
                        ?.let { StoatAPI.voiceStateCache[it]?.participants?.size }
                        ?: 0
                    Text(
                        text = "$participantCount/$maxUsers",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FragmentMono
                        ),
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            if (showVoiceParticipants &&
                channel.channelType == ChannelType.TextChannel &&
                channel.voice != null
            ) {
                VoiceChannelParticipantPreview(
                    channel = channel,
                    modifier = if (isMuted) Modifier.alpha(0.5f) else Modifier
                )
            }
        }
    }
}

private const val MAX_VISIBLE_VOICE_PARTICIPANTS = 5

@Composable
private fun VoiceChannelParticipantPreview(
    channel: Channel,
    modifier: Modifier = Modifier,
) {
    val channelId = channel.id ?: return
    val participants = StoatAPI.voiceStateCache[channelId]?.participants.orEmpty()
    val participantIds = participants.map { it.id }.distinct()

    LaunchedEffect(participantIds) {
        supervisorScope {
            participantIds
                .filter { StoatAPI.userCache[it] == null }
                .forEach { userId ->
                    launch {
                        try {
                            addUserIfUnknown(userId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR) {
                                "Failed to fetch voice participant $userId\n" +
                                        e.asLog()
                            }
                        }
                    }
                }
        }
    }

    AnimatedVisibility(
        visible = participants.isNotEmpty(),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, top = 4.dp, end = 24.dp, bottom = 8.dp)
        ) {
            participants.take(MAX_VISIBLE_VOICE_PARTICIPANTS).forEach { participant ->
                VoiceChannelParticipantRow(
                    state = participant,
                    channel = channel
                )
            }

            val hiddenParticipantCount =
                (participants.size - MAX_VISIBLE_VOICE_PARTICIPANTS).coerceAtLeast(0)
            if (hiddenParticipantCount > 0) {
                Text(
                    text = stringResource(
                        R.string.channel_voice_participants_more,
                        hiddenParticipantCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 28.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceChannelParticipantRow(
    state: UserVoiceState,
    channel: Channel,
) {
    val user = StoatAPI.userCache[state.id]
    val displayName = channel.server
        ?.let { StoatAPI.members.getMember(it, state.id)?.nickname }
        ?: user?.let(User::resolveDefaultName)
        ?: stringResource(R.string.unknown)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        UserAvatar(
            username = displayName,
            userId = state.id,
            avatar = user?.avatar,
            size = 20.dp
        )
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (state.screensharing) {
            Icon(
                painter = painterResource(R.drawable.ic_screen_share_24dp),
                contentDescription = stringResource(R.string.voice_screen_sharing),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    isExpanded: Boolean = true,
    onToggle: () -> Unit = {},
    canManage: Boolean = false,
    onCreateChannel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "category_chevron_rotation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_keyboard_arrow_right_24dp),
            contentDescription = null,
            tint = Color(0xFF949BA4),
            modifier = Modifier
                .size(12.dp)
                .rotate(rotation)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = (category.title ?: stringResource(R.string.unknown)).uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = Color(0xFF949BA4),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (canManage) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 18.dp),
                        onClick = onCreateChannel
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_24dp),
                    contentDescription = stringResource(R.string.server_settings_channels_create_channel),
                    tint = Color(0xFF949BA4),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DMOrGroupItem(
    channel: Channel,
    partner: User?,
    isCurrent: Boolean,
    hasUnread: Boolean,
    isMuted: Boolean = false,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    onOpenChannelContextSheet: (String) -> Unit
) {
    val backgroundColor = if (isCurrent) Color(0xFF35373C) else Color.Transparent
    val nameColor = if (isCurrent) Color(0xFFF2F3F5) else Color(0xFF949BA4)
    val previewText = channel.lastMessageID?.let { chat.stoat.api.StoatAPI.messageCache[it]?.content } ?: ""

    Row(
        Modifier
            .padding(horizontal = 8.dp)
            .height(40.dp)
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .combinedClickable(
                onLongClickLabel = stringResource(R.string.channel_context_sheet_open),
                onLongClick = {
                    channel.id?.let { chId ->
                        onOpenChannelContextSheet(chId)
                    }
                },
                onClick = {
                    channel.id?.let { chId ->
                        onDestinationChanged(ChatRouterDestination.Channel(chId))
                    }
                }
            )
            .then(
                if (isMuted) Modifier.alpha(0.5f) else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val name = when (channel.channelType) {
            ChannelType.Group -> channel.name ?: stringResource(R.string.unknown)
            else -> partner?.let { User.resolveDefaultName(it) } ?: channel.name ?: stringResource(R.string.unknown)
        }

        when (channel.channelType) {
            ChannelType.Group -> GroupIcon(
                name = name,
                size = 36.dp,
                icon = channel.icon
            )
            else -> UserAvatar(
                username = name,
                presence = presenceFromStatus(
                    partner?.status?.presence,
                    partner?.online ?: false
                ),
                userId = partner?.id ?: channel.id ?: "",
                avatar = partner?.avatar ?: channel.icon,
                size = 36.dp,
                presenceSize = 12.dp
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (previewText.isNotBlank()) {
                Text(
                    text = previewText,
                    fontSize = 12.sp,
                    color = Color(0xFF949BA4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (hasUnread && !isCurrent) {
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFFED4245))
                    .requiredSize(8.dp)
            )
        }
    }
}
