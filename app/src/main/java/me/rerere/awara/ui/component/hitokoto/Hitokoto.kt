package me.rerere.awara.ui.component.hitokoto

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import me.rerere.awara.data.entity.Hitokoto
import me.rerere.awara.data.source.HitokotoAPI
import org.koin.compose.koinInject

@Composable
fun Hitokoto(
    modifier: Modifier = Modifier,
    type: String = "a",
) {
    val api = koinInject<HitokotoAPI>()
    val scope = rememberCoroutineScope()
    var hitokoto by remember {
        mutableStateOf<Hitokoto?>(null)
    }
    LaunchedEffect(Unit) {
        hitokoto = kotlin.runCatching { api.getHitokoto(type = type) }.getOrNull()
    }
    Text(
        text = "${hitokoto?.hitokoto ?: ""} -- ${hitokoto?.from ?: ""}",
        modifier = modifier.clickable {
            scope.launch {
                hitokoto = kotlin.runCatching { api.getHitokoto(type = type) }.getOrNull()
            }
        }
    )
}