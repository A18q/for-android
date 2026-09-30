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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

private fun safeParseColor(hex: String?, fallback: Color = Color(0xFF99AAB5)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        fallback
    }
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
    var profile by remember { mutableStateOf<Profile?>(null) }
    var profileNotFound by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        try {
            user?.id?.let { fetchUserProfile(it) }?.let { profile = it }
        } catch (e: Exception) {
            if (e.message == "NotFound") {
                profileNotFound = true
            }
            e.printStackTrace()
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

    val isSelf = userId == StoatAPI.selfId

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
                .height(175.dp)
        ) {
            val background = profile?.background
            if (background != null) {
                val bgUrl = "$STOAT_FILES/backgrounds/${background.id}/${background.filename}"
                RemoteImage(
                    url = bgUrl,
                    description = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Discord default aesthetic gradient banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
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

            // Discord Overlapping 96dp Circular Avatar with 6dp Cut-Out Border
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
            ) {
                UserAvatar(
                    username = User.resolveDefaultName(user),
                    userId = user.id ?: "",
                    avatar = user.avatar,
                    size = 96.dp,
                    presenceSize = 26.dp,
                    presence = presenceFromStatus(user.status?.presence, user.online ?: false),
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(6.dp, DiscordDarkCanvas, CircleShape)
                )
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
        }

        // ─── 2. Identity Block (Display Name, Handle, Pronouns, Custom Status) ───
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            val displayName = member?.nickname ?: User.resolveDefaultName(user)
            Text(
                text = displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DiscordHeader
            )

            val handle = user.username?.let {
                val disc = user.discriminator?.takeIf { d -> d.isNotEmpty() }?.let { d -> "#$d" } ?: ""
                "@$it$disc"
            } ?: ""
            val pronouns = user.pronouns?.trim()?.takeIf { it.isNotEmpty() }
            val subtitle = if (pronouns != null && handle.isNotEmpty()) "$handle • $pronouns" else handle.ifEmpty { pronouns ?: "" }
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
                UserButtons(user, dismissSheet)
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

            // Card 2: MEMBER SINCE (Server Join & Account Creation)
            val accountAt = user.id?.let {
                DateUtils.getRelativeTimeSpanString(
                    ULID.asTimestamp(user.id!!),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            }
            val joinedAt = member?.joinedAt?.let {
                DateUtils.getRelativeTimeSpanString(
                    Instant.parse(member.joinedAt!!).toEpochMilliseconds(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
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
            val roles = member?.roles?.mapNotNull { roleId -> server?.roles?.get(roleId) }
                ?.sortedByDescending { it.rank ?: 0.0 }
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
    val roleColor = safeParseColor(role.colour)

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