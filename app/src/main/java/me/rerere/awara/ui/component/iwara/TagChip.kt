package me.rerere.awara.ui.component.iwara

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.awara.data.entity.Tag

@Composable
fun TagChip(
    tag: Tag,
    onClick: ((Tag) -> Unit)? = null,
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small,
        modifier = if (onClick == null) {
            Modifier
        } else {
            Modifier.clickable { onClick(tag) }
        },
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelMedium) {
            Text(
                text = tag.id,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun TagRow(
    modifier: Modifier = Modifier,
    tags: List<Tag>,
    onTagClick: ((Tag) -> Unit)? = null,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(tags) {
            TagChip(tag = it, onClick = onTagClick)
        }
    }
}