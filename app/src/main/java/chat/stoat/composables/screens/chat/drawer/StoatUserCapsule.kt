package chat.stoat.composables.screens.chat.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.core.model.schemas.User

// Stoat Modern Discord Theme Surface Tokens
val StoatCapsuleBg = Color(0xFF1E1F22)
val StoatCapsuleBorder = Color(0xFF2B2D31)
val StoatBellBg = Color(0xFF2B2D31)
val StoatTextHeader = Color(0xFFF2F3F5)
val StoatTextMuted = Color(0xFF949BA4)

val StatusOnlineColor = Color(0xFF23A55A)
val StatusIdleColor = Color(0xFFF0B232)
val StatusDndColor = Color(0xFFF23F43)
val StatusOfflineColor = Color(0xFF80848E)

@Composable
fun StoatUserCapsule(
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selfUser = StoatAPI.userCache[StoatAPI.selfId]
    val displayName = selfUser?.let { User.resolveDefaultName(it) } ?: "You"
    val customStatus = selfUser?.status?.text?.takeIf { it.isNotBlank() }
    val presenceText = customStatus ?: when (selfUser?.status?.presence) {
        "Idle" -> "Idle"
        "Focus", "Busy" -> "Do Not Disturb"
        "Invisible" -> "Invisible"
        else -> if (selfUser?.online == true) "Online" else "Offline"
    }

    val statusDotColor = when (selfUser?.status?.presence) {
        "Idle" -> StatusIdleColor
        "Focus", "Busy" -> StatusDndColor
        "Invisible" -> StatusOfflineColor
        else -> if (selfUser?.online == true) StatusOnlineColor else StatusOfflineColor
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(StoatCapsuleBg)
            .border(1.dp, StoatCapsuleBorder, RoundedCornerShape(28.dp))
            .clickable { onOpenProfile() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 38dp Avatar Container with Status Badge
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                UserAvatar(
                    username = displayName,
                    presence = presenceFromStatus(selfUser?.status?.presence, selfUser?.online ?: false),
                    userId = StoatAPI.selfId ?: "",
                    avatar = selfUser?.avatar,
                    size = 38.dp,
                    presenceSize = 0.dp
                )

                // 13dp Status dot with 2dp border matching capsule
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(StoatCapsuleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // User identity text column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        color = StoatTextHeader,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "∨",
                        color = StoatTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(1.dp))

                Text(
                    text = presenceText,
                    color = StoatTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(6.dp))

            // 32dp Quick Notification Bell pill
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(StoatBellBg)
                    .clickable { onOpenNotifications() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notifications_24dp),
                    contentDescription = "Notifications",
                    tint = StoatTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            // 32dp Quick Settings gear button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(StoatBellBg)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_24dp),
                    contentDescription = "Settings",
                    tint = StoatTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
