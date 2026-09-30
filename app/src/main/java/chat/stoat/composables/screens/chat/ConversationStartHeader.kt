package chat.stoat.composables.screens.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.User

// ─── Discord Authentic Surface Tokens ───
private val DiscordHeader = Color(0xFFF2F3F5)
private val DiscordTextNormal = Color(0xFFDBDEE1)
private val DiscordTextMuted = Color(0xFF949BA4)
private val DiscordCardBg = Color(0xFF2B2D31)
private val DiscordHashCircle = Color(0xFF35373C)
private val DiscordBlurple = Color(0xFF5865F2)

@Composable
fun ConversationStartHeader(
    channel: Channel?,
    onWave: () -> Unit = {}
) {
    if (channel == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        when (channel.channelType) {
            ChannelType.DirectMessage -> {
                // ─── Discord Direct Message Start Header ───
                val partnerId = ChannelUtils.resolveDMPartner(channel)
                val partner = StoatAPI.userCache[partnerId]
                val displayName = partner?.let { User.resolveDefaultName(it) } ?: partner?.username ?: "User"
                val username = partner?.username?.let { "@$it" } ?: ""

                // 80dp User Avatar
                UserAvatar(
                    username = displayName,
                    presence = presenceFromStatus(partner?.status?.presence, partner?.online == true),
                    userId = partner?.id ?: channel.id ?: "",
                    avatar = partner?.avatar,
                    size = 80.dp,
                    presenceSize = 22.dp
                )

                Spacer(Modifier.height(14.dp))

                // Display Name
                Text(
                    text = displayName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DiscordHeader,
                    lineHeight = 32.sp
                )

                // Username handle
                if (username.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = username,
                        fontSize = 15.sp,
                        color = DiscordTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Discord's signature message text
                Text(
                    text = "This is the beginning of your direct message history with $displayName.",
                    fontSize = 14.sp,
                    color = DiscordTextNormal,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(16.dp))

                // Wave Button
                Button(
                    onClick = onWave,
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordCardBg),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Wave to $displayName 👋",
                        color = DiscordHeader,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            ChannelType.Group -> {
                // ─── Discord Group DM Start Header ───
                val groupName = channel.name ?: "Group"

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DiscordHashCircle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_group_24dp),
                        contentDescription = null,
                        tint = DiscordHeader,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Welcome to $groupName!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DiscordHeader,
                    lineHeight = 32.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "This is the start of the $groupName group.",
                    fontSize = 14.sp,
                    color = DiscordTextMuted
                )
            }

            else -> {
                // ─── Discord Server Text Channel Start Header ───
                val channelName = channel.name ?: "channel"

                // Big '#' circle badge (68dp)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(DiscordHashCircle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tag_24dp),
                        contentDescription = null,
                        tint = DiscordHeader,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Title: "Welcome to #channel-name!"
                Text(
                    text = "Welcome to #$channelName!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DiscordHeader,
                    lineHeight = 32.sp
                )

                Spacer(Modifier.height(6.dp))

                // Subtitle: "This is the start of the #channel-name channel."
                Text(
                    text = "This is the start of the #$channelName channel.",
                    fontSize = 14.sp,
                    color = DiscordTextMuted
                )

                // Optional Topic / Description card
                if (!channel.description.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DiscordCardBg)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "TOPIC",
                                color = DiscordTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            SelectionContainer {
                                ChatMarkdown(
                                    content = channel.description!!,
                                    serverId = channel.server
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
