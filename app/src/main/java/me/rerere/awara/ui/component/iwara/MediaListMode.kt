package me.rerere.awara.ui.component.iwara

import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import me.rerere.awara.ui.component.ext.DynamicStaggeredGridCells
import me.rerere.compose_setting.preference.rememberBooleanPreference
import me.rerere.compose_setting.preference.rememberStringPreference

const val SETTING_MEDIA_LIST_MODE = "setting.media_list_mode"
const val SETTING_BLOCK_MEDIA_THUMBNAILS = "setting.block_media_thumbnails"
const val MEDIA_LIST_MODE_DETAIL = "detail"
const val MEDIA_LIST_MODE_THUMBNAIL = "thumbnail"

@Composable
fun rememberMediaListModePreference() = rememberStringPreference(
    key = SETTING_MEDIA_LIST_MODE,
    default = MEDIA_LIST_MODE_DETAIL,
)

@Composable
fun rememberBlockMediaThumbnailsPreference() = rememberBooleanPreference(
    key = SETTING_BLOCK_MEDIA_THUMBNAILS,
    default = false,
)

fun mediaListGridCells(listMode: String): StaggeredGridCells = when (listMode) {
    MEDIA_LIST_MODE_THUMBNAIL -> DynamicStaggeredGridCells(150.dp, 2, 4)
    else -> StaggeredGridCells.Fixed(1)
}