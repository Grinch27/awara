package me.rerere.awara.ui.component.iwara

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.data.entity.User
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.Button
import me.rerere.awara.ui.component.common.ButtonType

@Composable
fun AuthorCard(
    user: User?,
    onClickSub: (() -> Unit)?
) {
    val router = LocalRouterProvider.current
    if (user != null) {
        Card(
            onClick = {
                if (user.hasNavigableProfile) {
                    router.navigate("user/${Uri.encode(user.username)}")
                }
            }
        ) {
            Row(
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Avatar(
                            user = user,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f),
                            onClick = {
                                if (user.hasNavigableProfile) {
                                    router.navigate("user/${Uri.encode(user.username)}")
                                }
                            }
                        )

                        Column(
                            modifier = Modifier
                        ) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            UserStatus(user = user, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                onClickSub?.let {
                    Button(
                        onClick = onClickSub,
                        type = if (user.following) ButtonType.Outlined else ButtonType.Default
                    ) {
                        Text(
                            if (user.following) stringResource(R.string.unfollow) else stringResource(
                                R.string.follow
                            )
                        )
                    }
                }
            }
        }
    }
}