package chat.stoat.sheets

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.channel.createInvite
import chat.stoat.api.routes.channel.sendMessage
import chat.stoat.api.routes.invites.fetchServerInvites
import chat.stoat.api.routes.user.openDM
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.screens.chat.drawer.ServerIconImage
import chat.stoat.core.model.data.STOAT_INVITES
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User
import chat.stoat.screens.settings.server.availableInviteChannels
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class InviteSubView {
    Invite,
    LinkSettings,
    ExpirePicker,
    MaxUsesPicker,
    QrCode
}

private val expireOptionsList = listOf(
    "Never",
    "30 days",
    "7 days",
    "1 day",
    "12 hours",
    "6 hours",
    "1 hour",
    "30 mins"
)

private val maxUsesOptionsList = listOf(
    "No limit",
    "1 use",
    "5 uses",
    "10 uses",
    "25 uses",
    "50 uses",
    "100 uses"
)

private suspend fun generateQrBitmap(content: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            512,
            512,
            mapOf(EncodeHintType.MARGIN to "1")
        )
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        val black = Color.Black.toArgb()
        val white = Color.White.toArgb()
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) black else white
            }
        }
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        bmp
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ServerInviteContent(
    serverId: String,
    initialChannelId: String? = null,
    onBack: (() -> Unit)? = null,
    onDismiss: suspend () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val server = StoatAPI.serverCache[serverId]
    val channels = remember(server) { availableInviteChannels(server, StoatAPI.channelCache) }
    val targetChannel = remember(channels, initialChannelId) {
        if (initialChannelId != null) StoatAPI.channelCache[initialChannelId] ?: channels.firstOrNull()
        else channels.firstOrNull() ?: server?.channels?.mapNotNull { StoatAPI.channelCache[it] }?.firstOrNull()
    }

    var inviteCode by remember { mutableStateOf<String?>(null) }
    var isLoadingInvite by remember { mutableStateOf(true) }

    var currentSubView by remember { mutableStateOf(InviteSubView.Invite) }
    var searchQuery by remember { mutableStateOf("") }
    // TODO: Later implementation when backend supports invite expiry, max_uses, and temporary membership
    var expireOption by remember { mutableStateOf("30 days") }
    var maxUsesOption by remember { mutableStateOf("No limit") }
    var isTemporaryMembership by remember { mutableStateOf(false) }

    var showCopiedToast by remember { mutableStateOf(false) }
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    val invitedFriends = remember { mutableStateMapOf<String, Boolean>() }
    val sendingFriends = remember { mutableStateMapOf<String, Boolean>() }

    // Fetch existing invite or create new one
    LaunchedEffect(serverId, targetChannel?.id) {
        isLoadingInvite = true
        try {
            val existing = runCatching { fetchServerInvites(serverId) }.getOrNull()
            val match = existing?.firstOrNull {
                if (targetChannel != null) it.channel == targetChannel.id else true
            } ?: existing?.firstOrNull()

            if (match != null) {
                inviteCode = match.id
            } else {
                val chId = targetChannel?.id
                if (chId != null) {
                    val newInvite = createInvite(chId)
                    inviteCode = newInvite.id
                }
            }
        } catch (_: Exception) {
            val chId = targetChannel?.id
            if (chId != null) {
                runCatching {
                    val newInvite = createInvite(chId)
                    inviteCode = newInvite.id
                }
            }
        } finally {
            isLoadingInvite = false
        }
    }

    val inviteUrl = remember(inviteCode) {
        if (inviteCode != null) "$STOAT_INVITES/$inviteCode" else STOAT_INVITES
    }

    val handleCopyLink: () -> Unit = {
        clipboard.setText(AnnotatedString(inviteUrl))
        showCopiedToast = true
        showCopiedSnackbar = true
        scope.launch {
            delay(2500)
            showCopiedToast = false
        }
        scope.launch {
            delay(4000)
            showCopiedSnackbar = false
        }
    }

    BackHandler(enabled = currentSubView != InviteSubView.Invite) {
        when (currentSubView) {
            InviteSubView.ExpirePicker, InviteSubView.MaxUsesPicker -> currentSubView = InviteSubView.LinkSettings
            InviteSubView.LinkSettings, InviteSubView.QrCode -> currentSubView = InviteSubView.Invite
            InviteSubView.Invite -> {}
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = currentSubView,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "InviteSubViewAnimation"
        ) { subView ->
        when (subView) {
            InviteSubView.Invite -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2B2D31))
                        .padding(bottom = 16.dp)
                ) {
                    // Drag handle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .background(Color(0xFF4E5058), RoundedCornerShape(2.dp))
                        )
                    }

                    // Header row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close_24dp),
                                    contentDescription = "Back",
                                    tint = Color(0xFFF2F3F5)
                                )
                            }
                        }
                        Text(
                            text = "Invite a friend",
                            color = Color(0xFFF2F3F5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Quick Actions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Share Invite
                        QuickActionItem(
                            icon = painterResource(R.drawable.ic_ios_share_24dp),
                            label = "Share Invite",
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, inviteUrl)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Invite"))
                            }
                        )

                        // 2. Copy Link
                        QuickActionItem(
                            icon = painterResource(R.drawable.ic_link_24dp),
                            label = "Copy Link",
                            onClick = {
                                handleCopyLink()
                            }
                        )

                        // 3. QR Code
                        QuickActionItem(
                            icon = painterResource(R.drawable.ic_qr_code_scanner_24dp),
                            label = "QR Code",
                            onClick = {
                                currentSubView = InviteSubView.QrCode
                            }
                        )

                        // 4. Messages
                        QuickActionItem(
                            icon = painterResource(R.drawable.ic_chat_24dp),
                            label = "Messages",
                            onClick = {
                                val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
                                    putExtra("sms_body", inviteUrl)
                                }
                                runCatching { context.startActivity(smsIntent) }.onFailure {
                                    Toast.makeText(context, "Could not open messaging app", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        // 5. Email
                        QuickActionItem(
                            icon = painterResource(R.drawable.ic_mail_24dp),
                            label = "Email",
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "Join ${server?.name ?: "server"}")
                                    putExtra(Intent.EXTRA_TEXT, inviteUrl)
                                }
                                runCatching { context.startActivity(emailIntent) }.onFailure {
                                    Toast.makeText(context, "Could not open email app", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        // 6. Twitter
                        QuickActionItem(
                            icon = painterResource(R.drawable.ic_twitter_x_24dp),
                            label = "Twitter",
                            containerColor = Color(0xFF1D9BF0),
                            iconTint = Color.White,
                            onClick = {
                                val tweetText = Uri.encode("Join ${server?.name ?: "Server"} on Stoat: $inviteUrl")
                                val twitterIntent = Intent(Intent.ACTION_VIEW, "https://twitter.com/intent/tweet?text=$tweetText".toUri())
                                runCatching { context.startActivity(twitterIntent) }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Search bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(44.dp)
                            .background(Color(0xFF1E1F22), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF3F4147), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search_24dp),
                                contentDescription = null,
                                tint = Color(0xFF949BA4),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Invite friends to ${server?.name ?: "server"}",
                                        color = Color(0xFF80848E),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color(0xFFF2F3F5),
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF5865F2)),
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
                                        tint = Color(0xFF949BA4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Expiration Subtext & Edit invite link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Your invite link will never expire.",
                            color = Color(0xFF949BA4),
                            fontSize = 13.sp
                        )
                        // TODO: Later implementation when backend supports invite expiry, max_uses, and temporary membership
                        /*
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Edit invite link.",
                            color = Color(0xFF5865F2),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                currentSubView = InviteSubView.LinkSettings
                            }
                        )
                        */
                    }

                    Spacer(Modifier.height(4.dp))

                    // Friends list
                    val friends = remember {
                        StoatAPI.userCache.values
                            .asSequence()
                            .filter { it.relationship == "Friend" && !it.id.isNullOrBlank() }
                            .toList()
                    }
                    val filteredFriends = remember(friends, searchQuery) {
                        val trimmed = searchQuery.trim()
                        val baseList = if (trimmed.isEmpty()) {
                            friends
                        } else {
                            friends.filter { friend ->
                                friend.displayName?.contains(trimmed, ignoreCase = true) == true ||
                                friend.username?.contains(trimmed, ignoreCase = true) == true
                            }
                        }
                        baseList.sortedWith(
                            compareByDescending<User> { it.online == true }
                                .thenComparator { a, b ->
                                    val nameA = a.displayName ?: a.username ?: ""
                                    val nameB = b.displayName ?: b.username ?: ""
                                    String.CASE_INSENSITIVE_ORDER.compare(nameA, nameB)
                                }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (filteredFriends.isEmpty()) {
                            item {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp)
                                ) {
                                    Text(
                                        text = if (friends.isEmpty()) "No friends found" else "No matching friends",
                                        color = Color(0xFF949BA4),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredFriends, key = { it.id ?: "" }) { friend ->
                                val friendId = friend.id ?: return@items
                                val friendName = friend.displayName ?: friend.username ?: "User"
                                FriendInviteRow(
                                    friend = friend,
                                    isInvited = invitedFriends[friendId] == true,
                                    isSending = sendingFriends[friendId] == true,
                                    onInvite = {
                                        val code = inviteCode
                                        if (code == null && isLoadingInvite) {
                                            Toast.makeText(context, "Generating invite link...", Toast.LENGTH_SHORT).show()
                                            return@FriendInviteRow
                                        }
                                        sendingFriends[friendId] = true
                                        scope.launch {
                                            try {
                                                val targetChId = targetChannel?.id
                                                val activeCode = inviteCode ?: (if (targetChId != null) createInvite(targetChId).id else "")
                                                val link = "$STOAT_INVITES/$activeCode"
                                                val dm = openDM(friendId)
                                                val dmId = dm.id ?: ""
                                                if (dmId.isNotBlank()) {
                                                    sendMessage(dmId, link)
                                                    invitedFriends[friendId] = true
                                                    Toast.makeText(
                                                        context,
                                                        "Invite sent to $friendName!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to send invite: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } finally {
                                                sendingFriends.remove(friendId)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // TODO: Later implementation when backend supports invite expiry, max_uses, and temporary membership
            InviteSubView.LinkSettings -> {
                LinkSettingsView(
                    server = server,
                    channel = targetChannel,
                    expireOption = expireOption,
                    maxUsesOption = maxUsesOption,
                    isTemporaryMembership = isTemporaryMembership,
                    onTemporaryMembershipChange = { isTemporaryMembership = it },
                    onOpenExpirePicker = { currentSubView = InviteSubView.ExpirePicker },
                    onOpenMaxUsesPicker = { currentSubView = InviteSubView.MaxUsesPicker },
                    onClose = { currentSubView = InviteSubView.Invite }
                )
            }

            // TODO: Later implementation when backend supports invite expiry, max_uses, and temporary membership
            InviteSubView.ExpirePicker -> {
                OptionPickerView(
                    title = "Expire After",
                    options = expireOptionsList,
                    selectedOption = expireOption,
                    onSelect = { expireOption = it },
                    onClose = { currentSubView = InviteSubView.LinkSettings }
                )
            }

            // TODO: Later implementation when backend supports invite expiry, max_uses, and temporary membership
            InviteSubView.MaxUsesPicker -> {
                OptionPickerView(
                    title = "Max Uses",
                    options = maxUsesOptionsList,
                    selectedOption = maxUsesOption,
                    onSelect = { maxUsesOption = it },
                    onClose = { currentSubView = InviteSubView.LinkSettings }
                )
            }

            InviteSubView.QrCode -> {
                QrCodeView(
                    inviteUrl = inviteUrl,
                    onClose = { currentSubView = InviteSubView.Invite },
                    onCopy = { handleCopyLink() }
                )
            }
        }
    }

    // Top Pill Toast: "Link Copied!"
    AnimatedVisibility(
        visible = showCopiedToast,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp)
            .zIndex(10f)
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xFF23A55A), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_24dp),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Link Copied!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }

    // Bottom Snackbar: URL, [Edit (TODO)], Share, Dismiss
    AnimatedVisibility(
        visible = showCopiedSnackbar,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .zIndex(10f)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E1F22),
            border = BorderStroke(1.dp, Color(0xFF3F4147)),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = inviteUrl,
                    color = Color(0xFFF2F3F5),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                // TODO: Later implementation when backend supports invite expiry, max_uses, and temporary membership
                /*
                Text(
                    text = "Edit",
                    color = Color(0xFF5865F2),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable {
                            currentSubView = InviteSubView.LinkSettings
                            showCopiedSnackbar = false
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.width(4.dp))
                */
                Text(
                    text = "Share",
                    color = Color(0xFF5865F2),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, inviteUrl)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Invite"))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = { showCopiedSnackbar = false },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24dp),
                        contentDescription = "Dismiss",
                        tint = Color(0xFF949BA4),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
}

@Composable
private fun QuickActionItem(
    icon: Painter,
    label: String,
    containerColor: Color = Color(0xFF383A40),
    iconTint: Color = Color(0xFFDBDEE1),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .background(containerColor, RoundedCornerShape(14.dp))
        ) {
            Icon(
                painter = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF949BA4),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun FriendInviteRow(
    friend: User,
    isInvited: Boolean,
    isSending: Boolean,
    onInvite: () -> Unit
) {
    val friendName = friend.displayName ?: friend.username ?: "User"
    val friendId = friend.id ?: ""
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        UserAvatar(
            username = friendName,
            userId = friendId,
            avatar = friend.avatar,
            presence = chat.stoat.composables.generic.presenceFromStatus(friend.status?.presence, friend.online ?: false),
            size = 40.dp,
            shape = CircleShape
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friendName,
                color = Color(0xFFF2F3F5),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!friend.displayName.isNullOrBlank() && !friend.username.isNullOrBlank() && friend.displayName != friend.username) {
                Text(
                    text = "@${friend.username}",
                    color = Color(0xFF949BA4),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            isInvited -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(Color(0xFF23A55A), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_24dp),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Sent",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            isSending -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(Color(0xFF383A40), RoundedCornerShape(18.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
            }
            else -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF383A40))
                        .clickable(onClick = onInvite)
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Invite",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// TODO: Later implementation when backend supports invite expiry, max_uses, and temporary membership
@Composable
private fun LinkSettingsView(
    server: Server?,
    channel: Channel?,
    expireOption: String,
    maxUsesOption: String,
    isTemporaryMembership: Boolean,
    onTemporaryMembershipChange: (Boolean) -> Unit,
    onOpenExpirePicker: () -> Unit,
    onOpenMaxUsesPicker: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2D31))
            .padding(bottom = 24.dp)
    ) {
        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24dp),
                    contentDescription = "Close",
                    tint = Color(0xFFF2F3F5)
                )
            }
            Text(
                text = "Link Settings",
                color = Color(0xFFF2F3F5),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        // INVITE CHANNEL section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "INVITE CHANNEL",
                color = Color(0xFF949BA4),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    if (server != null) {
                        ServerIconImage(
                            server = server,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF383A40), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_tag_24dp),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "#${channel?.name ?: "general"}",
                            color = Color(0xFFF2F3F5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = server?.name ?: "Server",
                            color = Color(0xFF949BA4),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ADVANCED SETTINGS section
            Text(
                text = "ADVANCED SETTINGS",
                color = Color(0xFF949BA4),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Expire After row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenExpirePicker)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Expire After",
                            color = Color(0xFFF2F3F5),
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = expireOption,
                            color = Color(0xFF949BA4),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                            contentDescription = null,
                            tint = Color(0xFF949BA4),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFF35373C),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Max Uses row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenMaxUsesPicker)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Max Uses",
                            color = Color(0xFFF2F3F5),
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = maxUsesOption,
                            color = Color(0xFF949BA4),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                            contentDescription = null,
                            tint = Color(0xFF949BA4),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Temporary Membership card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Temporary Membership",
                        color = Color(0xFFF2F3F5),
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isTemporaryMembership,
                        onCheckedChange = onTemporaryMembershipChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF23A55A)
                        )
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Members are automatically kicked when they disconnect unless a role is assigned.",
                color = Color(0xFF949BA4),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// TODO: Later implementation when backend supports invite expiry, max_uses, and temporary membership
@Composable
private fun OptionPickerView(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2D31))
            .padding(bottom = 24.dp)
    ) {
        // Drag handle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color(0xFF4E5058), RoundedCornerShape(2.dp))
            )
        }

        Text(
            text = title,
            color = Color(0xFFF2F3F5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        options.forEachIndexed { index, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelect(option)
                        onClose()
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = option,
                    color = Color(0xFFF2F3F5),
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                RadioButton(
                    selected = option == selectedOption,
                    onClick = {
                        onSelect(option)
                        onClose()
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF5865F2),
                        unselectedColor = Color(0xFF949BA4)
                    )
                )
            }
            if (index < options.lastIndex) {
                HorizontalDivider(
                    color = Color(0xFF35373C),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun QrCodeView(
    inviteUrl: String,
    onClose: () -> Unit,
    onCopy: (() -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var qrBitmap by remember(inviteUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(inviteUrl) {
        qrBitmap = generateQrBitmap(inviteUrl)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2D31))
            .padding(bottom = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24dp),
                    contentDescription = "Close",
                    tint = Color(0xFFF2F3F5)
                )
            }
            Text(
                text = "QR Code",
                color = Color(0xFFF2F3F5),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = Color(0xFF5865F2))
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = inviteUrl,
            color = Color(0xFF949BA4),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(16.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF5865F2))
                .clickable {
                    if (onCopy != null) {
                        onCopy()
                    } else {
                        clipboard.setText(AnnotatedString(inviteUrl))
                        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Copy Link",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerInviteSheet(
    serverId: String,
    initialChannelId: String? = null,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF2B2D31),
        dragHandle = null
    ) {
        ServerInviteContent(
            serverId = serverId,
            initialChannelId = initialChannelId,
            onBack = {
                scope.launch {
                    sheetState.hide()
                    onDismissRequest()
                }
            },
            onDismiss = {
                sheetState.hide()
                onDismissRequest()
            }
        )
    }
}
