package dev.nohus.rift.postkillmail

import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.settings.persistence.KillmailPosting
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.get
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import org.koin.core.annotation.Single

@Single
class PostKillmailWatcher(
    private val settings: Settings,
    private val clipboard: Clipboard,
    private val windowManager: WindowManager,
    private val postKillmailUseCase: PostKillmailUseCase,
) {

    companion object {
        private val killmailUrlRegex = """https://esi\.evetech\.net/killmails/(?<killId>\d+)/(?<hash>[a-z0-9]+)""".toRegex()
    }

    suspend fun start() = coroutineScope {
        clipboard.state.filterNotNull().collect { text ->
            val result = killmailUrlRegex.find(text) ?: return@collect
            when (settings.killmailPosting) {
                KillmailPosting.DoNothing -> {}
                KillmailPosting.Dialog -> {
                    windowManager.onWindowOpen(
                        WindowManager.RiftWindow.PostKillmail,
                        inputModel = PostKillmailInputModel(result["killId"], result["hash"]),
                    )
                }
                KillmailPosting.Automatic -> {
                    postKillmailUseCase(result["killId"], result["hash"], KillmailPostingDelay.fromValue(settings.killmailPostingDelay))
                }
            }
        }
    }
}
