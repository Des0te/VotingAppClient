package com.example.votingclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.votingclient.data.local.SessionStore
import com.example.votingclient.data.local.SettingsStore
import com.example.votingclient.data.model.CreatePollRequest
import com.example.votingclient.data.model.PollResponse
import com.example.votingclient.data.model.ResultsResponse
import com.example.votingclient.data.repository.VotingRepository
import com.example.votingclient.ui.VotingUiState
import com.example.votingclient.ui.VotingViewModel
import com.example.votingclient.ui.VotingViewModelFactory
import com.example.votingclient.ui.theme.VotingClientTheme
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity(), KoinComponent {
    private val repository: VotingRepository by inject()
    private val settingsStore: SettingsStore by inject()
    private val sessionStore: SessionStore by inject()
    private val votingViewModel: VotingViewModel by viewModels {
        VotingViewModelFactory(repository, settingsStore, sessionStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by votingViewModel.state
            VotingClientTheme(darkTheme = state.darkTheme) {
                VotingApp(state = state, viewModel = votingViewModel)
            }
        }
    }
}

@Composable
private fun VotingApp(state: VotingUiState, viewModel: VotingViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.user?.id) {
        if (state.user == null) {
            navController.navigate("login") {
                popUpTo(0)
            }
        } else {
            navController.navigate("home") {
                popUpTo(0)
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable("login") {
                LoginScreen(
                    state = state,
                    onLogin = viewModel::login,
                    onRegister = { navController.navigate("register") },
                )
            }
            composable("register") {
                RegisterScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onRegister = viewModel::register,
                )
            }
            composable("home") {
                HomeScreen(
                    state = state,
                    viewModel = viewModel,
                    navController = navController,
                )
            }
            composable("detail") {
                DetailScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onVote = viewModel::vote,
                    onRefresh = {
                        state.selectedPoll?.let { viewModel.openPoll(it) }
                    },
                )
            }
            composable("create") {
                CreatePollScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onCreate = { request ->
                        viewModel.createPoll(request) {
                            navController.popBackStack()
                            viewModel.loadActive()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    state: VotingUiState,
    onLogin: (String, String) -> Unit,
    onRegister: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    CenteredColumn {
        Text("Голосования", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { onLogin(email, password) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Войти")
        }
        TextButton(onClick = onRegister) {
            Text("Зарегистрироваться")
        }
        AuthStatus(state)
    }
}

@Composable
private fun RegisterScreen(
    state: VotingUiState,
    onBack: () -> Unit,
    onRegister: (String, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }

    CenteredColumn {
        Text("Регистрация", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Имя пользователя") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(repeat, { repeat = it }, label = { Text("Подтверждение пароля") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { onRegister(name, email, password, repeat) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Зарегистрироваться")
        }
        TextButton(onClick = onBack) {
            Text("Уже есть аккаунт")
        }
        AuthStatus(state)
    }
}

@Composable
private fun AuthStatus(state: VotingUiState) {
    if (state.isLoading) {
        Spacer(Modifier.height(12.dp))
        CircularProgressIndicator()
    }
    state.error?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = MaterialTheme.colorScheme.error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: VotingUiState,
    viewModel: VotingViewModel,
    navController: NavHostController,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Голосования") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Тёмная")
                        Switch(checked = state.darkTheme, onCheckedChange = { viewModel.toggleTheme() })
                    }
                    IconButton(onClick = { navController.navigate("create") }) {
                        Icon(Icons.Default.Add, contentDescription = "Создать")
                    }
                    TextButton(onClick = { viewModel.logout() }) {
                        Text("Выйти")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("create") }) {
                Icon(Icons.Default.Add, contentDescription = "Создать")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            SearchPanel(state, viewModel)
            Spacer(Modifier.height(12.dp))
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.error?.let {
                ErrorBlock(text = it, onRefresh = { viewModel.search() })
            }
            if (!state.isLoading && state.error == null && state.polls.isEmpty()) {
                Text(
                    text = if (state.searchText.isBlank()) "Активных голосований нет" else "Нет результатов поиска",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.polls) { poll ->
                    PollItem(
                        poll = poll,
                        currentUserId = state.user?.id,
                        onClick = {
                            viewModel.openPoll(poll)
                            navController.navigate("detail")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchPanel(state: VotingUiState, viewModel: VotingViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = state.searchText,
            onValueChange = viewModel::onSearchChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            placeholder = { Text("Введите запрос...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
            trailingIcon = {
                if (state.searchText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            viewModel.clearSearch()
                            keyboard?.hide()
                            focusManager.clearFocus()
                        },
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.search()
                    keyboard?.hide()
                },
            ),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { viewModel.search() }) {
            Icon(Icons.Default.Search, contentDescription = "Искать")
        }
    }

    if (focused && state.searchText.isBlank() && state.history.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("История поиска", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.clearHistory() }) {
                Icon(Icons.Default.Delete, contentDescription = "Очистить историю")
            }
        }
        state.history.forEach { item ->
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.onSearchChange(item)
                        viewModel.search(item)
                        keyboard?.hide()
                        focusManager.clearFocus()
                    }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun PollItem(poll: PollResponse, currentUserId: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(poll.question, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (poll.authorId == currentUserId) {
                    Text("Моё", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(if (poll.choiceType == "SINGLE") "Один вариант" else "Несколько вариантов")
            Text("С ${poll.startsAt} до ${poll.endsAt}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    state: VotingUiState,
    onBack: () -> Unit,
    onVote: (List<String>) -> Unit,
    onRefresh: () -> Unit,
) {
    val poll = state.selectedPoll
    var selected by remember(poll?.id) { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Голосование") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
            )
        },
    ) { padding ->
        if (poll == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(poll.question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (poll.anonymous) "Анонимное голосование" else "Открытая статистика")
            }
            items(poll.options) { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (poll.choiceType == "SINGLE") {
                                setOf(option.id)
                            } else if (selected.contains(option.id)) {
                                selected - option.id
                            } else {
                                selected + option.id
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (poll.choiceType == "SINGLE") {
                        RadioButton(selected = selected.contains(option.id), onClick = { selected = setOf(option.id) })
                    } else {
                        Checkbox(
                            checked = selected.contains(option.id),
                            onCheckedChange = {
                                selected = if (it) selected + option.id else selected - option.id
                            },
                        )
                    }
                    Text(option.text, modifier = Modifier.padding(start = 8.dp))
                }
            }
            item {
                Button(
                    onClick = { onVote(selected.toList()) },
                    enabled = selected.isNotEmpty() && !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Голосовать")
                }
                state.message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
            state.results?.let { results ->
                item {
                    ResultsBlock(results)
                }
            }
        }
    }
}

@Composable
private fun ResultsBlock(results: ResultsResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Результаты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Проголосовало: ${results.totalVoters}")
        results.options.forEach { option ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(option.text, modifier = Modifier.weight(1f))
                Text("${option.votes} (${option.percent.toInt()}%)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePollScreen(
    state: VotingUiState,
    onBack: () -> Unit,
    onCreate: (CreatePollRequest) -> Unit,
) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf("Да\nНет") }
    var multiple by remember { mutableStateOf(false) }
    var anonymous by remember { mutableStateOf(false) }
    var maxChoices by remember { mutableStateOf("2") }
    var startsAt by remember { mutableStateOf(defaultDate(5 * 60 * 1000L)) }
    var endsAt by remember { mutableStateOf(defaultDate(24 * 60 * 60 * 1000L)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создать") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(question, { question = it }, label = { Text("Вопрос") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(
                    value = options,
                    onValueChange = { options = it },
                    label = { Text("Варианты ответов, каждый с новой строки") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = multiple, onCheckedChange = { multiple = it })
                    Text("Можно выбрать несколько вариантов")
                }
                if (multiple) {
                    OutlinedTextField(
                        value = maxChoices,
                        onValueChange = { maxChoices = it.filter(Char::isDigit) },
                        label = { Text("Максимум вариантов") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = anonymous, onCheckedChange = { anonymous = it })
                    Text("Анонимное голосование")
                }
            }
            item {
                OutlinedTextField(startsAt, { startsAt = it }, label = { Text("Дата начала") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(endsAt, { endsAt = it }, label = { Text("Дата окончания") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Button(
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onCreate(
                            CreatePollRequest(
                                question = question,
                                options = options.lines().map { it.trim() }.filter { it.isNotBlank() },
                                startsAt = startsAt,
                                endsAt = endsAt,
                                choiceType = if (multiple) "MULTIPLE" else "SINGLE",
                                anonymous = anonymous,
                                maxChoices = maxChoices.toIntOrNull() ?: 1,
                            )
                        )
                    },
                ) {
                    Text("Создать голосование")
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ErrorBlock(text: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Ошибка выполнения поиска", color = MaterialTheme.colorScheme.error)
        Text(text)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Обновить")
        }
    }
}

@Composable
private fun CenteredColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

private fun defaultDate(offsetMs: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(System.currentTimeMillis() + offsetMs))
}
