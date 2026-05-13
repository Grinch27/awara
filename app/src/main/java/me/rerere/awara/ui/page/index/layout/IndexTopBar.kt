package me.rerere.awara.ui.page.index.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.ui.component.iwara.MEDIA_LIST_MODE_DETAIL
import me.rerere.awara.ui.component.iwara.MEDIA_LIST_MODE_THUMBNAIL
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.page.index.IndexNavigation

private val topSwitcherNavigationNames = setOf("video", "image", "forum")

@Composable
internal fun IndexTopBarTitle(
    currentNavigation: IndexNavigation?,
    contentNavigations: List<IndexNavigation>,
    onNavigationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val switcherNavigations = remember(contentNavigations) {
        contentNavigations.filter { it.name in topSwitcherNavigationNames }
    }
    var expanded by remember { mutableStateOf(false) }

    if (switcherNavigations.isEmpty()) {
        Text(
            text = stringResource(currentNavigation?.titleRes ?: R.string.app_name),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.widthIn(max = 220.dp),
        ) {
            Text(
                text = stringResource(currentNavigation?.titleRes ?: R.string.app_name),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Outlined.KeyboardArrowDown, null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            switcherNavigations.forEach { navigation ->
                DropdownMenuItem(
                    text = {
                        Text(stringResource(navigation.titleRes))
                    },
                    leadingIcon = navigation.icon,
                    trailingIcon = {
                        if (navigation.name == currentNavigation?.name) {
                            Icon(Icons.Outlined.Check, null)
                        }
                    },
                    onClick = {
                        onNavigationSelected(navigation.name)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun IndexTopBarActions(
    onSearch: () -> Unit,
) {
    IconButton(onClick = onSearch) {
        Icon(Icons.Outlined.Search, stringResource(R.string.search))
    }

    IndexViewMenu()
}

@Composable
private fun IndexViewMenu() {
    var expanded by remember { mutableStateOf(false) }
    var listMode by rememberMediaListModePreference()

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.ViewAgenda, stringResource(R.string.index_view_menu))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            IndexViewMenuHeader(text = stringResource(R.string.index_view_menu_view))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.media_list_mode_detail)) },
                leadingIcon = { Icon(Icons.Outlined.ViewAgenda, null) },
                trailingIcon = {
                    if (listMode == MEDIA_LIST_MODE_DETAIL) {
                        Icon(Icons.Outlined.Check, null)
                    }
                },
                onClick = {
                    listMode = MEDIA_LIST_MODE_DETAIL
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.media_list_mode_thumbnail)) },
                leadingIcon = { Icon(Icons.Outlined.Image, null) },
                trailingIcon = {
                    if (listMode == MEDIA_LIST_MODE_THUMBNAIL) {
                        Icon(Icons.Outlined.Check, null)
                    }
                },
                onClick = {
                    listMode = MEDIA_LIST_MODE_THUMBNAIL
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun IndexViewMenuHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 16.dp,
            top = 10.dp,
            end = 16.dp,
            bottom = 4.dp,
        ),
    )
}