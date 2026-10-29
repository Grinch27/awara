package me.rerere.awara.ui.stores

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.collectAsState
import me.rerere.awara.data.session.SecureSession
import me.rerere.awara.data.session.SessionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import me.rerere.awara.data.dto.ProfileDto
import me.rerere.awara.data.entity.Tag
import me.rerere.awara.data.entity.User
import me.rerere.awara.data.repo.UserRepo
import me.rerere.awara.data.source.onError
import me.rerere.awara.data.source.onException
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching
import me.rerere.compose_setting.preference.mmkvPreference
import me.rerere.compose_setting.preference.rememberStringPreference
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent

private const val TAG = "UserStore"

val LocalUserStore = staticCompositionLocalOf {
    ReduxStore(
        initialState = UserStoreState(),
        reducer = object : Reducer<UserStoreState, UserStoreAction>, KoinComponent {
            // private val userRepo by inject<UserRepo>()
            override fun reduce(
                prevState: UserStoreState,
                action: UserStoreAction
            ): UserStoreState {
                return when (action) {
                    is UserStoreAction.SetUser -> prevState.copy(user = action.user)
                    is UserStoreAction.SetTagBlacklist -> prevState.copy(tagBlacklist = action.tagBlacklist)
                    is UserStoreAction.Logout -> {
                        SecureSession.logout()
                        prevState.copy(
                            user = null,
                            tagBlacklist = emptyList(),
                            profile = null,
                            loading = false,
                            refreshing = false
                        )
                    }

                    is UserStoreAction.SetProfile -> {
                        prevState.copy(profile = action.profile)
                    }

                    is UserStoreAction.SetLoading -> {
                        prevState.copy(loading = action.loading)
                    }

                    is UserStoreAction.SetRefreshing -> {
                        prevState.copy(refreshing = action.refreshing)
                    }
                }
            }
        }
    )
}

data class UserStoreState(
    val refreshing: Boolean = true,
    val loading: Boolean = false,
    val user: User? = null,
    val profile: ProfileDto? = null,
    val tagBlacklist: List<Tag> = emptyList()
)

sealed class UserStoreAction {
    data class SetUser(val user: User) : UserStoreAction()
    data class SetTagBlacklist(val tagBlacklist: List<Tag>) : UserStoreAction()
    data class SetProfile(val profile: ProfileDto) : UserStoreAction()
    data class SetLoading(val loading: Boolean) : UserStoreAction()
    data class SetRefreshing(val refreshing: Boolean) : UserStoreAction()
    object Logout : UserStoreAction()
}

@Composable
fun UserStoreProvider(
    userRepo: UserRepo = koinInject(),
    sessionRepository: SessionRepository = koinInject(),
    content: @Composable () -> Unit
) {
    val store = LocalUserStore.current
    val session by SecureSession.state.collectAsState()
    LaunchedEffect(session.epoch, session.refreshToken) {
        store.dispatch(UserStoreAction.SetRefreshing(session.refreshToken.isNotBlank()))
        try {
            if (session.refreshToken.isNotBlank()) sessionRepository.ensureAccessToken()
        } finally {
            store.dispatch(UserStoreAction.SetRefreshing(false))
        }
    }
    LaunchedEffect(session.epoch, session.accessToken) {
        if (session.accessToken.isBlank()) return@LaunchedEffect
        val epoch = session.epoch
        store.dispatch(UserStoreAction.SetLoading(true))
        try {
            runAPICatching { userRepo.getSelfProfile() }.onSuccess {
                if (SecureSession.snapshot.epoch == epoch &&
                    SecureSession.snapshot.accessToken == session.accessToken
                ) {
                    store.dispatch(UserStoreAction.SetUser(it.user))
                    store.dispatch(UserStoreAction.SetTagBlacklist(it.tagBlacklist))
                    store.dispatch(UserStoreAction.SetProfile(it.profile))
                }
            }
        } finally {
            if (SecureSession.snapshot.epoch == epoch) store.dispatch(UserStoreAction.SetLoading(false))
        }
    }
    content()
}
