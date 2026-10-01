package chat.stoat.composables.screens.chat.drawer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.colour.CSSColours
import chat.stoat.api.internals.parseCssFunctionColour
import chat.stoat.api.settings.ServerSidebarEntry
import chat.stoat.composables.generic.IconPlaceholder
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.bottomEndCircleCutout
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Server

private val ServerVoiceBadgeSize = 16.dp
private val ServerVoiceBadgeIconSize = 12.dp

fun parseFolderColour(colour: String): Color? {
    val trimmed = colour.trim()
    return CSSColours[trimmed.lowercase()]
        ?: parseCssFunctionColour(trimmed)
        ?: runCatching { Color(trimmed.toColorInt()) }.getOrNull()
}

private val Server.iconUrl: String?
    get() = icon?.id?.let { "$STOAT_FILES/icons/$it" }

@Composable
private fun RailIndicatorBox(
    selected: Boolean,
    unread: Boolean,
    modifier: Modifier = Modifier,
    ring: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val indicatorHeight by animateDpAsState(
        targetValue = when {
            selected -> 36.dp
            unread -> 8.dp
            else -> 0.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Left indicator height"
    )
    val indicatorColour by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            unread -> Color.White.copy(alpha = 0.75f)
            else -> Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Left indicator colour"
    )

    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .padding(6.dp)
                .then(
                    if (ring) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .padding(2.dp)
                .size(48.dp),
            contentAlignment = Alignment.BottomEnd,
            content = content
        )

        Box(
            Modifier
                .height(indicatorHeight)
                .width(8.dp)
                .offset(x = (-4).dp)
                .clip(CircleShape)
                .background(indicatorColour)
                .align(Alignment.CenterStart)
        )
    }
}

data class FolderGroupStyle(val colour: Color, val lastMemberKey: String)

@Stable
class FolderGroupLayout {
    internal data class Row(val folderId: String, val top: Float, val bottom: Float)

    internal var listCoordinates: LayoutCoordinates? = null
    internal val rows = mutableStateMapOf<String, Row>()

    internal fun report(key: String, folderId: String, coordinates: LayoutCoordinates) {
        if (!coordinates.isAttached) return
        val top = coordinates.positionInWindow().y
        rows[key] = Row(folderId, top, top + coordinates.size.height)
    }
}

fun Modifier.folderGroupMember(layout: FolderGroupLayout, key: String, folderId: String) =
    composed {
        DisposableEffect(layout, key, folderId) {
            onDispose { layout.rows.remove(key) }
        }
        onGloballyPositioned { layout.report(key, folderId, it) }
    }

fun Modifier.folderGroupBackgrounds(
    layout: FolderGroupLayout,
    styles: Map<String, FolderGroupStyle>,
) = onGloballyPositioned { layout.listCoordinates = it }
    .clipToBounds()
    .drawBehind {
        val listTop = layout.listCoordinates?.takeIf { it.isAttached }?.positionInWindow()?.y
            ?: return@drawBehind
        val inset = 6.dp.toPx()
        val radius = size.width / 2 - inset
        val gap = RailItemGap.toPx()

        styles.forEach { (folderId, style) ->
            val header = layout.rows[folderId]
            var count = 0
            var minTop = Float.POSITIVE_INFINITY
            var maxBottom = Float.NEGATIVE_INFINITY
            for (row in layout.rows.values) {
                if (row.folderId == folderId) {
                    count++
                    if (row.top < minTop) minTop = row.top
                    if (row.bottom > maxBottom) maxBottom = row.bottom
                }
            }
            if (count == if (header != null) 1 else 0) return@forEach

            val last = layout.rows[style.lastMemberKey]
            val top = header?.let { it.top + inset } ?: (minTop - radius * 2)
            val bottom = last?.let { it.bottom - gap - inset }
                ?: (maxBottom + radius * 2)

            drawRoundRect(
                color = style.colour,
                topLeft = Offset(inset, top - listTop),
                size = Size(size.width - inset * 2, bottom - top),
                cornerRadius = CornerRadius(radius)
            )
        }
    }

@Composable
fun ServerIconImage(
    server: Server,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
) {
    val icon = server.iconUrl
    val shape = RoundedCornerShape(cornerRadius)
    if (icon != null) {
        RemoteImage(
            url = icon,
            allowAnimation = false,
            modifier = Modifier
                .clip(shape)
                .then(modifier),
            description = server.name ?: stringResource(R.string.unknown)
        )
    } else {
        IconPlaceholder(
            name = server.name ?: stringResource(R.string.unknown),
            modifier = Modifier
                .clip(shape)
                .then(modifier)
        )
    }
}

@Composable
fun ServerRailIcon(
    server: Server,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    foldTarget: Boolean = false,
) {
    val hasUnread = server.id?.let { StoatAPI.unreads.serverHasUnread(it) } ?: false
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 16.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "server_icon_corner"
    )
    val voiceParticipants = server.channels.orEmpty().flatMap { channelId ->
        StoatAPI.voiceStateCache[channelId]?.participants.orEmpty()
    }
    val hasScreenShare = voiceParticipants.any { it.screensharing }
    val voiceBadgeIcon = when {
        hasScreenShare -> R.drawable.ic_screen_share_24dp
        voiceParticipants.isNotEmpty() -> R.drawable.ic_volume_up_24dp
        else -> null
    }
    val longClickLabel = stringResource(R.string.server_context_sheet_open)

    RailIndicatorBox(
        selected = selected,
        unread = hasUnread,
        ring = foldTarget,
        modifier = modifier
    ) {
        ServerIconImage(
            server = server,
            cornerRadius = cornerRadius,
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (voiceBadgeIcon != null) {
                        Modifier.bottomEndCircleCutout(ServerVoiceBadgeSize)
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick)
                .semantics {
                    onLongClick(label = longClickLabel) {
                        onLongClick()
                        true
                    }
                }
        )

        if (voiceBadgeIcon != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ServerVoiceBadgeSize)
            ) {
                Icon(
                    painter = painterResource(voiceBadgeIcon),
                    contentDescription = stringResource(
                        if (hasScreenShare) {
                            R.string.voice_screen_sharing
                        } else {
                            R.string.voice_notification_ongoing_call
                        }
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ServerVoiceBadgeIconSize)
                )
            }
        }
    }
}

@Composable
fun FolderRailHeader(
    entry: ServerSidebarEntry.Folder,
    currentServer: String?,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    foldTarget: Boolean = false,
) {
    val collapsed = entry.folder.collapsed == true
    val name = entry.folder.name.ifEmpty { stringResource(R.string.server_folder_unnamed) }
    val longClickLabel = stringResource(R.string.server_folder_sheet_open)
    val holdsSelected = entry.servers.any { it.id == currentServer }
    val hasUnread = entry.servers.any { server ->
        server.id?.let { StoatAPI.unreads.serverHasUnread(it) } == true
    }

    RailIndicatorBox(
        selected = collapsed && holdsSelected,
        unread = collapsed && hasUnread,
        ring = foldTarget,
        modifier = modifier
    ) {
        FolderIcon(
            entry = entry,
            modifier = Modifier
                .clickable(onClickLabel = name, onClick = onToggle)
                .semantics {
                    contentDescription = name
                    onLongClick(label = longClickLabel) {
                        onLongClick()
                        true
                    }
                }
        )
    }
}

@Composable
fun FolderIcon(entry: ServerSidebarEntry.Folder, modifier: Modifier = Modifier) {
    val collapsed = entry.folder.collapsed == true
    val colour = entry.folder.colour?.let(::parseFolderColour)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (collapsed) {
                    colour?.copy(alpha = 0.35f) ?: Color(0xFF2B2D31)
                } else {
                    Color.Transparent
                }
            )
            .then(modifier)
    ) {
        if (collapsed) {
            FolderPreview(entry.servers)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_folder_open_24dp),
                contentDescription = null,
                tint = colour ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FolderPreview(servers: List<Server>) {
    val iconSize: Dp = if (servers.size > 2) 18.dp else 20.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        servers.take(4).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { server ->
                    val modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(4.dp))
                    val icon = server.iconUrl
                    if (icon != null) {
                        RemoteImage(
                            url = icon,
                            allowAnimation = false,
                            modifier = modifier,
                            description = null
                        )
                    } else {
                        IconPlaceholder(
                            name = server.name ?: "",
                            fontSize = 8.sp,
                            modifier = modifier
                        )
                    }
                }
            }
        }
    }
}
