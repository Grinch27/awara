package me.rerere.awara.ui.component.iwara

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.awara.data.entity.User
import me.rerere.awara.ui.LocalRouterProvider

@Composable
fun UserCard(
    modifier: Modifier = Modifier,
    user: User
) {
    val router = LocalRouterProvider.current
    Card(
        modifier = modifier,
        onClick = {
            if (user.hasNavigableProfile) {
                router.navigate("user/${user.username}")
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Avatar(user = user, modifier = Modifier.size(48.dp))

            Column {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.displayHandle,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            UserStatus(user = user)
        }
    }
}