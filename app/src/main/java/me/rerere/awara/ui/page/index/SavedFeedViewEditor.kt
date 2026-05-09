package me.rerere.awara.ui.page.index

// TODO(user): Decide whether smart subscriptions should later gain extra local rules like unread-only or partially watched-only, instead of staying a metadata flag.
// TODO(agent): If saved-view metadata keeps growing, move this editor out of ad-hoc dialogs into a dedicated saved-view management screen.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.awara.R

data class SavedFeedViewDraft(
    val name: String = "",
    val tagsText: String = "",
    val description: String = "",
    val smartSubscription: Boolean = false,
    val pinned: Boolean = false,
)

fun SavedFeedViewDraft.normalizedTags(): List<String> {
    return tagsText
        .split(',', '，', '\n')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
}

@Composable
fun SavedFeedViewEditor(
    draft: SavedFeedViewDraft,
    onDraftChange: (SavedFeedViewDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = {
                Text(stringResource(R.string.saved_view_editor_name_title))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = draft.tagsText,
            onValueChange = { onDraftChange(draft.copy(tagsText = it)) },
            label = {
                Text(stringResource(R.string.saved_view_editor_tags_title))
            },
            supportingText = {
                Text(stringResource(R.string.saved_view_editor_tags_text))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = draft.description,
            onValueChange = { onDraftChange(draft.copy(description = it)) },
            label = {
                Text(stringResource(R.string.saved_view_editor_description_title))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        SavedFeedViewSwitchRow(
            title = stringResource(R.string.saved_view_editor_smart_title),
            summary = stringResource(R.string.saved_view_editor_smart_text),
            checked = draft.smartSubscription,
            onCheckedChange = { onDraftChange(draft.copy(smartSubscription = it)) },
        )

        SavedFeedViewSwitchRow(
            title = stringResource(R.string.saved_view_editor_pinned_title),
            summary = stringResource(R.string.saved_view_editor_pinned_text),
            checked = draft.pinned,
            onCheckedChange = { onDraftChange(draft.copy(pinned = it)) },
        )
    }
}

@Composable
private fun SavedFeedViewSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}