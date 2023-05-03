package me.rerere.awara.ui.component.iwara

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.data.entity.User
import me.rerere.awara.ui.component.common.Tag
import me.rerere.awara.ui.component.common.TagType

@Composable
fun UserStatus(user: User, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tag(
            type = TagType.Info
        ) {
            Text(text = user.role)
        }

        if (user.premium) {
            Tag(
                type = TagType.Info
            ) {
                Text(text = stringResource(R.string.premium))
            }
        }
    }
}