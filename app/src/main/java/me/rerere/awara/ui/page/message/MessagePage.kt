package me.rerere.awara.ui.page.message

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.ext.excludeBottom
import org.koin.androidx.compose.koinViewModel

@Composable
fun MessagePage(vm: MessageVM = koinViewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    BackButton()
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it.excludeBottom())
                .fillMaxSize()
        ) {
            // TODO: Add conversation list
        }
    }
}