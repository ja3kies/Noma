package dev.juuri.noma.ui

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.juuri.noma.ui.theme.NomaSpacing

/** Shared labels and touch targets for the four stable editor tool slots. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorToolButton(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconModifier: Modifier = Modifier
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.defaultMinSize(
                minWidth = NomaSpacing.touchTarget, minHeight = NomaSpacing.touchTarget
            )
        ) {
            Icon(painterResource(icon), contentDescription = description, modifier = iconModifier)
        }
    }
}
