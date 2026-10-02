package chat.stoat.composables.generic

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.activities.StoatTweenColour
import chat.stoat.activities.StoatTweenDp
import chat.stoat.internals.server.PermissionOverrideValue

private val SegmentWidth = 40.dp
private val SegmentHeight = 32.dp
private val PillCornerRadius = 8.dp

private const val PositionDeny = 0
private const val PositionNeutral = 1
private const val PositionAllow = 2

private fun positionForValue(value: PermissionOverrideValue): Int {
    return when (value) {
        PermissionOverrideValue.Deny -> PositionDeny
        PermissionOverrideValue.Neutral -> PositionNeutral
        PermissionOverrideValue.Allow -> PositionAllow
    }
}

private fun valueForPosition(position: Int): PermissionOverrideValue {
    return when (position) {
        PositionDeny -> PermissionOverrideValue.Deny
        PositionNeutral -> PermissionOverrideValue.Neutral
        PositionAllow -> PermissionOverrideValue.Allow
        else -> PermissionOverrideValue.Neutral
    }
}

private data class OverrideSegmentDefinition(
    val position: Int,
    val value: PermissionOverrideValue,
    val labelRes: Int,
    val iconRes: Int,
)

private val VisualOverrideSegments = listOf(
    OverrideSegmentDefinition(
        position = PositionDeny,
        value = PermissionOverrideValue.Deny,
        labelRes = R.string.permission_deny,
        iconRes = R.drawable.ic_close_24dp,
    ),
    OverrideSegmentDefinition(
        position = PositionNeutral,
        value = PermissionOverrideValue.Neutral,
        labelRes = R.string.permission_neutral,
        iconRes = R.drawable.ic_remove_24dp,
    ),
    OverrideSegmentDefinition(
        position = PositionAllow,
        value = PermissionOverrideValue.Allow,
        labelRes = R.string.permission_allow,
        iconRes = R.drawable.ic_check_24dp,
    ),
)

private val DiscordDenyRed = Color(0xFFED4245)
private val DiscordNeutralGray = Color(0xFF4E5058)
private val DiscordAllowGreen = Color(0xFF23A55A)
private val DiscordPillBackground = Color(0xFF1E1F22)
private val DiscordUnselectedDim = Color(0xFF80848E)

@Composable
fun PermissionOverridePicker(
    value: PermissionOverrideValue,
    enabled: Boolean,
    onValueChange: (PermissionOverrideValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(value, label = "permission override")
    val highlightOffset by transition.animateDp(
        transitionSpec = { StoatTweenDp },
        label = "permission override position",
    ) { state -> (positionForValue(state) * SegmentWidth.value).dp }

    val highlightColor by transition.animateColor(
        transitionSpec = { StoatTweenColour },
        label = "permission override color",
    ) { state ->
        when (state) {
            PermissionOverrideValue.Deny -> DiscordDenyRed
            PermissionOverrideValue.Neutral -> DiscordNeutralGray
            PermissionOverrideValue.Allow -> DiscordAllowGreen
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PillCornerRadius))
            .background(DiscordPillBackground)
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        Box(
            Modifier
                .offset(x = highlightOffset)
                .size(width = SegmentWidth, height = SegmentHeight)
                .background(highlightColor)
        )

        Row(Modifier.selectableGroup()) {
            VisualOverrideSegments.forEach { segment ->
                val selected = value == segment.value
                val contentColor by transition.animateColor(
                    transitionSpec = { StoatTweenColour },
                    label = "${segment.value.name.lowercase()} permission override icon",
                ) { state ->
                    if (state == segment.value) Color.White else DiscordUnselectedDim
                }

                PermissionOverrideOption(
                    segment = segment,
                    selected = selected,
                    enabled = enabled,
                    contentColor = contentColor,
                    onClick = { onValueChange(segment.value) },
                )
            }
        }
    }
}

@Composable
private fun PermissionOverrideOption(
    segment: OverrideSegmentDefinition,
    selected: Boolean,
    enabled: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val label = stringResource(segment.labelRes)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = SegmentWidth, height = SegmentHeight)
            .selectable(
                selected = selected,
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
            },
    ) {
        Icon(
            painter = painterResource(segment.iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
    }
}
