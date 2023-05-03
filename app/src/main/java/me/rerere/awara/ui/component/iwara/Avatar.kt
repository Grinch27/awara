package me.rerere.awara.ui.component.iwara

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import me.rerere.awara.data.entity.User
import me.rerere.awara.data.entity.toAvatarUrl
import me.rerere.awara.ui.theme.success
import me.rerere.awara.ui.theme.warning
import java.time.Instant

@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    user: User?,
    showOnlineStatus: Boolean = true,
    onClick: () -> Unit = {}
) {
    val online by remember {
        derivedStateOf {
            Instant.now().epochSecond - (user?.seenAt?.epochSecond ?: 0) < 60 * 5
        }
    }
    val onlineColor = if (online) {
        MaterialTheme.colorScheme.success
    } else {
        MaterialTheme.colorScheme.warning
    }
    me.rerere.awara.ui.component.common.Avatar(
        model = (user?.avatar).toAvatarUrl(),
        modifier = modifier.drawWithContent {
            drawContent()
            if (showOnlineStatus) {
                val offset = center + Offset(
                    x = size.width / 2f - 4.dp.toPx(),
                    y = size.height / 2f - 4.dp.toPx()
                )
                drawCircle(
                    color = onlineColor,
                    radius = 4.dp.toPx(),
                    center = offset,
                    alpha = 1f,
                )
            }
        },
        onClick = onClick
    )
}