package me.rerere.awara.ui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.awara.ui.theme.bestTextColor
import me.rerere.awara.ui.theme.info
import me.rerere.awara.ui.theme.success
import me.rerere.awara.ui.theme.warning

@Composable
fun Tag(
    modifier: Modifier = Modifier,
    type: TagType = TagType.Default,
    content: @Composable () -> Unit
) {
    val surfaceColor = when(type) {
        TagType.Default -> MaterialTheme.colorScheme.primaryContainer
        TagType.Success -> MaterialTheme.colorScheme.success
        TagType.Info -> MaterialTheme.colorScheme.info
        TagType.Warning -> MaterialTheme.colorScheme.warning
        TagType.Error -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = surfaceColor.copy(alpha = 0.9f),
        contentColor = surfaceColor.bestTextColor
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelSmall) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(4.dp)
            ) {
                content()
            }
        }
    }
}

enum class TagType {
    Default, Success, Info, Warning, Error
}