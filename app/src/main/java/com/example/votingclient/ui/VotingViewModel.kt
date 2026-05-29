package com.example.votingclient.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.votingclient.data.local.SessionStore
import com.example.votingclient.data.local.SettingsStore
import com.example.votingclient.data.model.CreatePollRequest
import com.example.votingclient.data.model.PollResponse
import com.example.votingclient.data.model.ResultsResponse
import com.example.votingclient.data.model.UserResponse
import com.example.votingclient.data.repository.VotingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class PollFilter {
    ALL,
    ACTIVE,
    SOON,
    FINISHED,
}

data class VotingUiState(
    val user: UserResponse? = null,
    val darkTheme: Boolean = false,
    val history: List<String> = emptyList(),
    val activePolls: List<PollResponse> = emptyList(),
    val searchResults: List<PollResponse> = emptyList(),
    val selectedPoll: PollResponse? = null,
    val results: ResultsResponse? = null,
    val searchText: String = "",
    val filter: PollFilter = PollFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) {
    val polls: List<PollResponse>
        get() {
            val source = if (searchText.isBlank()) activePolls else searchResults
            return when (filter) {
                PollFilter.ALL -> source
                PollFilter.ACTIVE -> source.filter { it.hasStatus("ACTIVE") }
                PollFilter.SOON -> source.filter { it.hasStatus("SOON") }
                PollFilter.FINISHED -> source.filter { it.hasStatus("FINISHED") }
            }
        }
}

class VotingViewModel(
    private val repository: VotingRepository,
    private val settingsStore: SettingsStore,
    private val sessionStore: SessionStore,
) : ViewModel() {
    var state = androidx.compose.runtime.mutableStateOf(VotingUiState())
        private set
    private var historyJob: Job? = null

    init {
        viewModelScope.launch {
            settingsStore.darkTheme.collectLatest { value ->
                state.value = state.value.copy(darkTheme = value)
            }
        }
        viewModelScope.launch {
            settingsStore.clearOldHistory()
        }
        viewModelScope.launch {
            sessionStore.user.collectLatest { user ->
                historyJob?.cancel()
                state.value = state.value.copy(
                    user = user,
                    history = emptyList(),
                    searchText = "",
                    searchResults = emptyList(),
                    selectedPoll = null,
                    results = null,
                )
                if (user != null) {
                    val historyUserId = user.id
                    historyJob = viewModelScope.launch {
                        settingsStore.historyFor(historyUserId).collectLatest { value ->
                            if (state.value.user?.id == historyUserId) {
                                state.value = state.value.copy(history = value)
                            }
                        }
                    }
                    loadActive()
                } else {
                    state.value = state.value.copy(activePolls = emptyList())
                }
            }
        }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        runLoading {
            repository.login(email, password)
            loadActive()
        }
    }

    fun register(name: String, email: String, password: String, repeat: String) = viewModelScope.launch {
        if (password != repeat) {
            state.value = state.value.copy(error = "Пароли не совпадают")
            return@launch
        }
        runLoading {
            repository.register(name, email, password)
            loadActive()
        }
    }

    fun logout() = viewModelScope.launch {
        sessionStore.clear()
        state.value = VotingUiState(darkTheme = state.value.darkTheme)
    }

    fun toggleTheme() = viewModelScope.launch {
        settingsStore.setDarkTheme(!state.value.darkTheme)
    }

    fun onSearchChange(text: String) {
        state.value = state.value.copy(searchText = text, error = null)
    }

    fun clearSearch() {
        state.value = state.value.copy(searchText = "", searchResults = emptyList(), error = null)
        loadActive()
    }

    fun loadActive() = viewModelScope.launch {
        runLoading {
            reloadCurrentList(forceActive = true)
        }
    }

    fun search(text: String = state.value.searchText) = viewModelScope.launch {
        val query = text.trim()
        if (query.isBlank()) {
            loadActive()
            return@launch
        }
        state.value = state.value.copy(searchText = query)
        runLoading {
            reloadCurrentList()
        }
    }

    fun setFilter(filter: PollFilter) {
        state.value = state.value.copy(filter = filter)
    }

    fun openPoll(poll: PollResponse) = viewModelScope.launch {
        settingsStore.addHistory(state.value.user?.id, poll.question)
        runLoading {
            val fullPoll = repository.poll(poll.id)
            val result = if (fullPoll.canLoadResults()) {
                runCatching { repository.results(poll.id) }.getOrNull()
            } else {
                null
            }
            state.value = state.value.copy(selectedPoll = fullPoll, results = result, message = null)
        }
    }

    fun vote(optionIds: List<String>) = viewModelScope.launch {
        val poll = state.value.selectedPoll ?: return@launch
        runLoading {
            val response = repository.vote(poll.id, optionIds)
            val freshPoll = runCatching { repository.poll(poll.id) }.getOrNull() ?: poll
            val result = if (freshPoll.canLoadResults()) {
                runCatching { repository.results(poll.id) }.getOrNull() ?: response.results
            } else {
                null
            }
            state.value = state.value.copy(
                selectedPoll = freshPoll,
                message = response.message,
                results = result,
            )
        }
    }

    fun createPoll(request: CreatePollRequest, onDone: () -> Unit) = viewModelScope.launch {
        runLoading {
            val poll = repository.createPoll(request)
            state.value = state.value.copy(message = "Голосование создано")
            openPoll(poll)
            onDone()
        }
    }

    fun deletePoll(poll: PollResponse) = viewModelScope.launch {
        runLoading {
            repository.deletePoll(poll.id)
            val selectedPoll = state.value.selectedPoll
            state.value = state.value.copy(
                selectedPoll = if (selectedPoll?.id == poll.id) null else selectedPoll,
                results = if (selectedPoll?.id == poll.id) null else state.value.results,
                message = "Голосование удалено",
            )
            reloadCurrentList()
        }
    }

    fun clearHistory() = viewModelScope.launch {
        settingsStore.clearHistory(state.value.user?.id)
    }

    fun clearMessage() {
        state.value = state.value.copy(message = null, error = null)
    }

    private suspend fun runLoading(block: suspend () -> Unit) {
        state.value = state.value.copy(isLoading = true, error = null)
        try {
            block()
        } catch (e: Exception) {
            state.value = state.value.copy(error = e.message ?: "Ошибка")
        } finally {
            state.value = state.value.copy(isLoading = false)
        }
    }

    private suspend fun reloadCurrentList(forceActive: Boolean = false) {
        val query = state.value.searchText.trim()
        if (forceActive || query.isBlank()) {
            state.value = state.value.copy(
                activePolls = repository.activePolls(),
                searchResults = emptyList(),
            )
        } else {
            state.value = state.value.copy(searchResults = repository.search(query))
        }
    }
}

private fun PollResponse.hasStatus(value: String): Boolean =
    status.equals(value, ignoreCase = true)

private fun PollResponse.canLoadResults(): Boolean {
    val finished = hasStatus("FINISHED")
    val soon = hasStatus("SOON")
    return !soon && (!anonymous || finished)
}

class VotingViewModelFactory(
    private val repository: VotingRepository,
    private val settingsStore: SettingsStore,
    private val sessionStore: SessionStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VotingViewModel(repository, settingsStore, sessionStore) as T
    }
}
