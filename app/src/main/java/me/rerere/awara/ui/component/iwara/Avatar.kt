package me.rerere.awara.ui.component.iwara

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    val onlineType = remember(user) {
        when(Instant.now().epochSecond - (user?.seenAt?.epochSecond ?: 0)) {
            in 0..60 * 10 -> OnlineType.ONLINE
            in 0..60 * 60 * 12 -> OnlineType.RECENTLY
            else -> OnlineType.OFFLINE
        }
    }
    val onlineColor = when(onlineType) {
        OnlineType.ONLINE -> MaterialTheme.colorScheme.success
        OnlineType.RECENTLY -> MaterialTheme.colorScheme.warning
        OnlineType.OFFLINE -> MaterialTheme.colorScheme.error
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

enum class OnlineType {
    ONLINE, // 在线 (10分钟内)
    RECENTLY, // 最近在线 (12小时内)
    OFFLINE // 离线
}