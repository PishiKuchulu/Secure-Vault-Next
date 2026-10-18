package com.securevault.next.presentation.vault

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.next.R
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.enums.SortField
import com.securevault.next.core.domain.model.enums.SortOrder
import com.securevault.next.core.domain.model.item.PasswordItem
import com.securevault.next.core.domain.model.item.SecureNote
import com.securevault.next.core.domain.model.item.VaultItem
import com.securevault.next.core.domain.model.search.SearchFilter
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.usecase.LockVaultUseCase
import com.securevault.next.core.domain.usecase.SearchItemsUseCase
import com.securevault.next.core.ui.components.EmptyState
import com.securevault.next.core.ui.components.ErrorState
import com.securevault.next.core.ui.components.SectionHeader
import com.securevault.next.core.ui.components.VaultCard
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultEaseOut
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import com.securevault.next.core.ui.theme.SecureVaultTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

// Screen copy is centralized in strings.xml (vault_home_*). The lock-failure
// message stays a const: it is set in the ViewModel, outside composition,
// where stringResource() cannot be called.
private const val STR_LOCK_FAILED = "Couldn't lock the vault. Try again."

private const val SEARCH_DEBOUNCE_MS = 300L

data class VaultHomeUiState(
    val vaultName: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<VaultItem> = emptyList(),
    val error: String? = null,
    val lockError: String? = null,
    val isLocking: Boolean = false,
    val noVault: Boolean = false,
)

@HiltViewModel
class VaultHomeViewModel
    @Inject
    constructor(
        private val vaultRepository: VaultRepository,
        private val searchItems: SearchItemsUseCase,
        private val lockVault: LockVaultUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(VaultHomeUiState())
        val uiState: StateFlow<VaultHomeUiState> = _uiState.asStateFlow()

        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        private var vaultId: VaultId? = null

        init {
            viewModelScope.launch {
                val metadata = vaultRepository.observeAllVaultMetadata().first().firstOrNull()
                if (metadata == null) {
                    _uiState.update { it.copy(isLoading = false, noVault = true) }
                    return@launch
                }
                vaultId = metadata.id
                _uiState.update { it.copy(vaultName = metadata.name.value) }
                // The collector below emits the current query ("") right away,
                // debounced — no separate initial search needed.
                observeQuery(metadata.id)
            }
        }

        @OptIn(FlowPreview::class)
        private suspend fun observeQuery(vaultId: VaultId) {
            _query
                .debounce(SEARCH_DEBOUNCE_MS)
                .collect { runSearch(vaultId, it) }
        }

        fun onQueryChange(query: String) {
            _query.value = query
        }

        fun onRetry() {
            val id = vaultId ?: return
            viewModelScope.launch { runSearch(id, _query.value) }
        }

        fun onLock(onLocked: () -> Unit) {
            val id = vaultId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isLocking = true, lockError = null) }
                when (val result = lockVault(id)) {
                    is Result.Success -> onLocked()
                    is Result.Failure ->
                        _uiState.update {
                            it.copy(isLocking = false, lockError = STR_LOCK_FAILED)
                        }
                }
            }
        }

        fun onLockErrorShown() {
            _uiState.update { it.copy(lockError = null) }
        }

        private suspend fun runSearch(
            vaultId: VaultId,
            query: String,
        ) {
            val hadItems = _uiState.value.items.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hadItems,
                    isRefreshing = hadItems,
                    error = null,
                )
            }
            val filter =
                if (query.isBlank()) {
                    SearchFilter(
                        sortField = SortField.UPDATED_AT,
                        sortOrder = SortOrder.DESCENDING,
                    )
                } else {
                    SearchFilter(
                        query = query,
                        sortField = SortField.TITLE,
                        sortOrder = SortOrder.ASCENDING,
                    )
                }
            when (val result = searchItems(vaultId, filter)) {
                is Result.Success ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = result.value.items,
                            error = null,
                        )
                    }
                is Result.Failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = friendlyErrorMessage(result.error),
                        )
                    }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHomeScreen(
    onOpenItem: (itemId: String) -> Unit,
    onAddItem: () -> Unit,
    onOpenGenerator: () -> Unit,
    onOpenSettings: () -> Unit,
    onLock: () -> Unit,
    viewModel: VaultHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateLifecycleAware()
    val query by viewModel.query.collectAsStateLifecycleAware()

    VaultHomeContent(
        state = state,
        query = query,
        onQueryChange = viewModel::onQueryChange,
        onOpenItem = onOpenItem,
        onAddItem = onAddItem,
        onOpenGenerator = onOpenGenerator,
        onOpenSettings = onOpenSettings,
        onLock = { viewModel.onLock(onLock) },
        onRetry = viewModel::onRetry,
        onLockErrorShown = viewModel::onLockErrorShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultHomeContent(
    state: VaultHomeUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    onAddItem: () -> Unit,
    onOpenGenerator: () -> Unit,
    onOpenSettings: () -> Unit,
    onLock: () -> Unit,
    onRetry: () -> Unit,
    onLockErrorShown: () -> Unit,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.lockError) {
        state.lockError?.let {
            snackbarHostState.showSnackbar(it)
            onLockErrorShown()
        }
    }

    Scaffold(
        containerColor = colors.baseBackground,
        topBar = {
            TopAppBar(
                title = { Text(state.vaultName ?: stringResource(R.string.vault_home_title_fallback)) },
                actions = {
                    IconButton(onClick = onOpenGenerator) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = stringResource(R.string.vault_home_generator_desc),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.vault_home_settings_desc),
                        )
                    }
                    IconButton(onClick = onLock, enabled = !state.isLocking) {
                        if (state.isLocking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.accent,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = stringResource(R.string.vault_home_lock_desc),
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.baseBackground,
                        titleContentColor = colors.label,
                        navigationIconContentColor = colors.label,
                        actionIconContentColor = colors.accent,
                    ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItem,
                containerColor = colors.accent,
                contentColor = colors.onAccent,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.vault_home_add_desc),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            VaultSearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier =
                    Modifier.padding(
                        horizontal = spacing.medium,
                        vertical = spacing.small,
                    ),
            )
            when {
                state.noVault ->
                    ErrorState(
                        title = stringResource(R.string.vault_home_error_title),
                        description = stringResource(R.string.vault_home_no_vault_description),
                        onRetry = onRetry,
                        retryLabel = stringResource(R.string.vault_home_retry),
                        modifier = Modifier.fillMaxSize(),
                    )
                state.isLoading ->
                    VaultListSkeleton(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = spacing.medium),
                    )
                state.error != null ->
                    ErrorState(
                        title = stringResource(R.string.vault_home_error_title),
                        description = state.error,
                        onRetry = onRetry,
                        retryLabel = stringResource(R.string.vault_home_retry),
                        modifier = Modifier.fillMaxSize(),
                    )
                state.items.isEmpty() && query.isBlank() ->
                    EmptyState(
                        icon = Icons.Filled.VpnKey,
                        title = stringResource(R.string.vault_home_empty_title),
                        description = stringResource(R.string.vault_home_empty_description),
                        actionLabel = stringResource(R.string.vault_home_empty_action),
                        onAction = onAddItem,
                        modifier = Modifier.fillMaxSize(),
                    )
                state.items.isEmpty() ->
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.vault_home_no_results_title),
                        description = stringResource(R.string.vault_home_no_results_description),
                        actionLabel = stringResource(R.string.vault_home_no_results_action),
                        onAction = { onQueryChange("") },
                        modifier = Modifier.fillMaxSize(),
                    )
                else -> {
                    SectionHeader(
                        title =
                            if (query.isBlank()) {
                                stringResource(R.string.vault_home_section_recent)
                            } else {
                                stringResource(R.string.vault_home_section_results)
                            },
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                start = spacing.medium,
                                end = spacing.medium,
                                bottom = spacing.extraLarge,
                            ),
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        items(
                            items = state.items,
                            key = { it.metadata.id.value },
                            contentType = { it.type },
                        ) { item ->
                            VaultCard(
                                title = item.metadata.title,
                                subtitle = itemListSubtitle(item),
                                onClick = { onOpenItem(item.metadata.id.value) },
                                onClickLabel = stringResource(R.string.vault_home_open_item, item.metadata.title),
                                leading = { ItemTypeIcon(item) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun itemListSubtitle(item: VaultItem): String {
    val username = (item as? PasswordItem)?.username?.takeIf { it.isNotBlank() }
    return if (username != null) "$username · ${item.type.uiLabel}" else item.type.uiLabel
}

@Composable
private fun ItemTypeIcon(item: VaultItem) {
    val colors = SecureVaultColorsScope.colors
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(colors.fill, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = item.type.uiIcon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = colors.secondaryLabel,
        )
    }
}

/**
 * iOS-style search field: quiet fill, no border, 12dp per the shape
 * lock, with a clear button while text is present.
 */
@Composable
private fun VaultSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SecureVaultColorsScope.colors
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.vault_home_search_placeholder)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.vault_home_clear_search_desc),
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = MaterialTheme.shapes.medium,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.label,
                unfocusedTextColor = colors.label,
                focusedContainerColor = colors.fill,
                unfocusedContainerColor = colors.fill,
                cursorColor = colors.accent,
                focusedBorderColor = colors.fill,
                unfocusedBorderColor = colors.fill,
                focusedLeadingIconColor = colors.secondaryLabel,
                unfocusedLeadingIconColor = colors.secondaryLabel,
                focusedTrailingIconColor = colors.secondaryLabel,
                unfocusedTrailingIconColor = colors.secondaryLabel,
                focusedPlaceholderColor = colors.secondaryLabel,
                unfocusedPlaceholderColor = colors.secondaryLabel,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
    )
}

/** Loading skeleton shaped like the final rows: 16dp card, icon well, two text lines. */
@Composable
private fun VaultListSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing
    val transition = rememberInfiniteTransition(label = "vaultListSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = SecureVaultEaseOut),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "skeletonAlpha",
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        items(6) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .graphicsLayer { this.alpha = alpha },
                shape = MaterialTheme.shapes.large,
                color = colors.surface,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = spacing.medium,
                                vertical = spacing.small + spacing.extraSmall,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.fill),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .height(14.dp)
                                    .fillMaxWidth(0.55f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.fill),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .height(12.dp)
                                    .fillMaxWidth(0.35f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.fill),
                        )
                    }
                }
            }
        }
    }
}

// --- Previews (not shipped UI; compositional smoke checks) ---

private fun previewItems(): List<VaultItem> {
    val now = Instant.now()

    fun metadata(title: String) =
        VaultItemMetadata(
            id = ItemId("preview-$title"),
            vaultId = VaultId("preview-vault"),
            title = title,
            createdAt = CreatedAt(now),
            updatedAt = UpdatedAt(now),
        )
    return listOf(
        PasswordItem(
            metadata = metadata("GitHub"),
            username = "octocat",
            password = EncryptedField("cipher", "nonce"),
        ),
        SecureNote(
            metadata = metadata("Wi-Fi details"),
            content = EncryptedField("cipher", "nonce"),
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun VaultHomeContentPreview() {
    SecureVaultTheme {
        VaultHomeContent(
            state =
                VaultHomeUiState(
                    vaultName = "Personal",
                    isLoading = false,
                    items = previewItems(),
                ),
            query = "",
            onQueryChange = {},
            onOpenItem = {},
            onAddItem = {},
            onOpenGenerator = {},
            onOpenSettings = {},
            onLock = {},
            onRetry = {},
            onLockErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VaultHomeEmptyPreview() {
    SecureVaultTheme {
        VaultHomeContent(
            state = VaultHomeUiState(vaultName = "Personal", isLoading = false),
            query = "",
            onQueryChange = {},
            onOpenItem = {},
            onAddItem = {},
            onOpenGenerator = {},
            onOpenSettings = {},
            onLock = {},
            onRetry = {},
            onLockErrorShown = {},
        )
    }
}
