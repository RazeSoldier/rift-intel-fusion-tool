package dev.nohus.rift.sso

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.sso.authentication.SsoAuthenticator
import dev.nohus.rift.sso.authentication.SsoClient
import dev.nohus.rift.sso.scopes.ScopeGroup
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.utils.toggle
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import java.net.BindException

private val logger = KotlinLogging.logger {}

@Factory
class SsoViewModel(
    @InjectedParam private val inputModel: SsoAuthority,
    private val ssoAuthenticator: SsoAuthenticator,
    private val localCharactersRepository: LocalCharactersRepository,
    private val ssoClient: SsoClient
) : ViewModel() {

    data class UiState(
        val status: SsoStatus = SsoStatus.Waiting,
        val scopeGroups: List<ScopeGroup>,
        val selectedScopeGroups: List<ScopeGroup>,
        val ssoCode: String = "", // 添加SSO认证码状态
    )

    sealed interface SsoStatus {
        data object Scopes : SsoStatus
        data object Waiting : SsoStatus
        data object Complete : SsoStatus
        data class Failed(val message: String?) : SsoStatus
    }

    private val _state = MutableStateFlow(
        UiState(
            scopeGroups = getScopeGroups(),
            selectedScopeGroups = getScopeGroups(),
        ),
    )
    val state = _state.asStateFlow()

    fun onWindowOpened() {
        _state.update { it.copy(status = SsoStatus.Waiting) }
        raiseAuthentication()
    }

    private fun raiseAuthentication() {
        viewModelScope.launch {
            ssoClient.raiseAuthenticationPage(inputModel, _state.value.selectedScopeGroups.flatMap { it.scopes }.map { it.id })
        }
    }

    private fun authenticate() {
        val scopes = _state.value.selectedScopeGroups.flatMap { it.scopes }.map { it.id }
        viewModelScope.launch {
            try {
                ssoAuthenticator.authenticate(inputModel, scopes, _state.value.ssoCode)
                _state.update { it.copy(status = SsoStatus.Complete) }
                localCharactersRepository.load()
            } catch (e: BindException) {
                _state.update { it.copy(status = SsoStatus.Failed("Could not listen for a response from EVE SSO. Likely some other application on your computer is interfering.")) }
            } catch (e: Exception) {
                logger.error(e) { "SSO flow failed" }
                _state.update { it.copy(status = SsoStatus.Failed(null)) }
            }
        }
    }

    fun onCheckedChange(group: ScopeGroup) {
        _state.update { it.copy(selectedScopeGroups = it.selectedScopeGroups.toggle(group)) }
    }

    fun onScopesButtonClick() {
        ssoAuthenticator.cancel()
        _state.update { it.copy(status = SsoStatus.Scopes) }
    }

    fun onContinueClick() {
        _state.update { it.copy(status = SsoStatus.Waiting) }
        raiseAuthentication()
    }

    fun onCloseRequest() {
        ssoAuthenticator.cancel()
        _state.update { it.copy(status = SsoStatus.Waiting) }
    }

    fun onSsoCodeChange(code: String) {
        // https://ali-esi.evepc.163.com/ui/oauth2-redirect.html?code=xxxxx&state=xxxxx
        _state.update { it.copy(ssoCode = code.substringAfter("code=").substringBefore("&state=")) }
    }

    fun onSsoCodeSubmit() {
        if (_state.value.ssoCode.isNotBlank()) {
            authenticate()
        }
    }

    private fun getScopeGroups(): List<ScopeGroup> {
        return ScopeGroups.all
    }
}