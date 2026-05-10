package me.rerere.awara.ui.component.iwara

import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.ui.component.common.SelectButton
import me.rerere.awara.ui.component.common.SelectOption
import me.rerere.awara.ui.component.ext.DynamicStaggeredGridCells
import me.rerere.compose_setting.preference.rememberStringPreference

const val SETTING_MEDIA_LIST_MODE = "setting.media_list_mode"
const val MEDIA_LIST_MODE_DETAIL = "detail"
const val MEDIA_LIST_MODE_THUMBNAIL = "thumbnail"

@Composable
fun rememberMediaListModePreference() = rememberStringPreference(
    key = SETTING_MEDIA_LIST_MODE,
    default = MEDIA_LIST_MODE_DETAIL,
)

fun mediaListGridCells(listMode: String): StaggeredGridCells = when (listMode) {
    MEDIA_LIST_MODE_THUMBNAIL -> DynamicStaggeredGridCells(150.dp, 2, 4)
    else -> StaggeredGridCells.Fixed(1)
}

@Composable
fun MediaListModeButton(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectButton(
        modifier = modifier,
        value = value,
        options = listOf(
            SelectOption(
                value = MEDIA_LIST_MODE_DETAIL,
                label = {
                    Text(stringResource(R.string.media_list_mode_detail))
                },
            ),
            SelectOption(
                value = MEDIA_LIST_MODE_THUMBNAIL,
                label = {
                    Text(stringResource(R.string.media_list_mode_thumbnail))
                },
            ),
        ),
        onValueChange = onValueChange,
    )
}