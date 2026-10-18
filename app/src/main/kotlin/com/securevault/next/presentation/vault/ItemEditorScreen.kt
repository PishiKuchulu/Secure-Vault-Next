package com.securevault.next.presentation.vault

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.next.R
import com.securevault.next.core.common.result.Result
import com.securevault.next.core.common.result.getOrNull
import com.securevault.next.core.crypto.TotpEngine
import com.securevault.next.core.domain.crypto.ItemEncryptionEngine
import com.securevault.next.core.domain.error.DomainError
import com.securevault.next.core.domain.model.VaultItemMetadata
import com.securevault.next.core.domain.model.item.PasswordItem
import com.securevault.next.core.domain.model.item.SecureNote
import com.securevault.next.core.domain.model.item.TOTPConfig
import com.securevault.next.core.domain.model.item.VaultItem
import com.securevault.next.core.domain.model.search.SearchFilter
import com.securevault.next.core.domain.model.valueobject.CreatedAt
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.model.valueobject.ItemId
import com.securevault.next.core.domain.model.valueobject.Notes
import com.securevault.next.core.domain.model.valueobject.UpdatedAt
import com.securevault.next.core.domain.model.valueobject.UrlValue
import com.securevault.next.core.domain.model.valueobject.VaultId
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.usecase.CreateItemUseCase
import com.securevault.next.core.domain.usecase.DeleteItemUseCase
import com.securevault.next.core.domain.usecase.GeneratePasswordUseCase
import com.securevault.next.core.domain.usecase.PasswordGenerationOptions
import com.securevault.next.core.domain.usecase.SearchItemsUseCase
import com.securevault.next.core.domain.usecase.UpdateItemUseCase
import com.securevault.next.core.ui.components.ErrorState
import com.securevault.next.core.ui.components.InlineError
import com.securevault.next.core.ui.components.SectionHeader
import com.securevault.next.core.ui.components.VaultDestructiveButton
import com.securevault.next.core.ui.components.VaultPasswordTextField
import com.securevault.next.core.ui.components.VaultSecondaryButton
import com.securevault.next.core.ui.components.VaultTextButton
import com.securevault.next.core.ui.components.VaultTextField
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import com.securevault.next.core.ui.theme.SecureVaultTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

// Screen copy is centralized in strings.xml (item_editor_*). Validation messages
// and save/delete/load failure messages stay consts: they are set in the
// ViewModel, outside composition, where stringResource() cannot be called.
private const val STR_TITLE_REQUIRED = "Enter a title for this item."
private const val STR_PASSWORD_REQUIRED = "Enter a password."
private const val STR_CONTENT_REQUIRED = "Write something first."
private const val STR_URL_INVALID = "Enter a valid website address."
private const val STR_TOTP_SECRET_INVALID = "This doesn't look like a valid setup key."
private const val STR_SAVE_FAILED = "Couldn't save. Check the highlighted fields and try again."
private const val STR_DELETE_FAILED = "Couldn't delete this item. Try again."
private const val STR_LOAD_FAILED = "Couldn't load the editor"

enum class EditableType {
    PASSWORD,
    SECURE_NOTE,
    UNSUPPORTED,
}

data class ItemEditorUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isCreate: Boolean = true,
    val editableType: EditableType = EditableType.PASSWORD,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val noteContent: String = "",
    val totpSecret: String = "",
    val totpIssuer: String = "",
    val totpAccount: String = "",
    val titleError: String? = null,
    val passwordError: String? = null,
    val contentError: String? = null,
    val urlError: String? = null,
    val totpSecretError: String? = null,
    val formError: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isDirty: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showDiscardConfirm: Boolean = false,
    val readOnlyTitle: String = "",
    val readOnlyTypeLabel: String = "",
    val readOnlyUpdated: String = "",
)

/** The editable form fields, compared to detect unsaved changes. */
private data class FormSnapshot(
    val type: EditableType,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val noteContent: String,
    val totpSecret: String,
    val totpIssuer: String,
    val totpAccount: String,
)

@HiltViewModel
class ItemEditorViewModel
    @Inject
    constructor(
        private val vaultRepository: VaultRepository,
        private val searchItems: SearchItemsUseCase,
        private val createItem: CreateItemUseCase,
        private val updateItem: UpdateItemUseCase,
        private val deleteItem: DeleteItemUseCase,
        private val encryption: ItemEncryptionEngine,
        private val generatePassword: GeneratePasswordUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ItemEditorUiState())
        val uiState: StateFlow<ItemEditorUiState> = _uiState.asStateFlow()

        private var vaultId: VaultId? = null
        private var editingItem: VaultItem? = null
        private var editingItemId: String? = null
        private var initialSnapshot: FormSnapshot? = null

        fun load(itemId: String?) {
            viewModelScope.launch {
                _uiState.value = ItemEditorUiState(isLoading = true)
                val resolvedVaultId = vaultRepository.firstVaultId()
                if (resolvedVaultId == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = STR_LOAD_FAILED) }
                    return@launch
                }
                vaultId = resolvedVaultId
                if (itemId == null) {
                    initialSnapshot = snapshot()
                    _uiState.update { it.copy(isLoading = false, isCreate = true) }
                    return@launch
                }
                val items = searchItems(resolvedVaultId, SearchFilter()).getOrNull()?.items
                val item = items?.firstOrNull { it.metadata.id.value == itemId }
                if (item == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = STR_LOAD_FAILED) }
                    return@launch
                }
                editingItem = item
                editingItemId = itemId
                try {
                    val prefilled = prefill(item)
                    initialSnapshot = snapshot(prefilled)
                    _uiState.value = prefilled.copy(isLoading = false, isCreate = false)
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, loadError = STR_LOAD_FAILED) }
                }
            }
        }

        fun onRetry(itemId: String?) = load(itemId)

        // --- Field changes (each recomputes the dirty flag) ---

        private fun updateForm(transform: (ItemEditorUiState) -> ItemEditorUiState) {
            _uiState.update { current ->
                val next =
                    transform(current).copy(
                        // Clear the field's own error as soon as the user edits it.
                        formError = null,
                    )
                next.copy(isDirty = snapshot(next) != initialSnapshot)
            }
        }

        private fun snapshot(state: ItemEditorUiState = _uiState.value) =
            FormSnapshot(
                type = state.editableType,
                title = state.title,
                username = state.username,
                password = state.password,
                url = state.url,
                notes = state.notes,
                noteContent = state.noteContent,
                totpSecret = state.totpSecret,
                totpIssuer = state.totpIssuer,
                totpAccount = state.totpAccount,
            )

        fun onTitleChange(value: String) = updateForm { it.copy(title = value, titleError = null) }

        fun onUsernameChange(value: String) = updateForm { it.copy(username = value) }

        fun onPasswordChange(value: String) = updateForm { it.copy(password = value, passwordError = null) }

        fun onUrlChange(value: String) = updateForm { it.copy(url = value, urlError = null) }

        fun onNotesChange(value: String) = updateForm { it.copy(notes = value) }

        fun onNoteContentChange(value: String) = updateForm { it.copy(noteContent = value, contentError = null) }

        fun onTotpSecretChange(value: String) = updateForm { it.copy(totpSecret = value, totpSecretError = null) }

        fun onTotpIssuerChange(value: String) = updateForm { it.copy(totpIssuer = value) }

        fun onTotpAccountChange(value: String) = updateForm { it.copy(totpAccount = value) }

        fun onTypeChange(type: EditableType) {
            if (type == EditableType.UNSUPPORTED) return
            updateForm { it.copy(editableType = type) }
        }

        fun onGeneratePassword() {
            val generated = generatePassword(PasswordGenerationOptions())
            updateForm { it.copy(password = generated.value, passwordError = null) }
        }

        // --- Save / delete / discard ---

        fun onSave(onDone: () -> Unit) {
            if (!validate()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, formError = null) }
                val result =
                    try {
                        persist()
                    } catch (e: Exception) {
                        Result.failure<DomainError>(
                            DomainError.UnexpectedError(e.message ?: "save failed", e),
                        )
                    }
                when (result) {
                    is Result.Success -> onDone()
                    is Result.Failure ->
                        _uiState.update {
                            it.copy(isSaving = false, formError = STR_SAVE_FAILED)
                        }
                }
            }
        }

        fun onDeleteRequested() {
            _uiState.update { it.copy(showDeleteConfirm = true) }
        }

        fun onDeleteDismissed() {
            _uiState.update { it.copy(showDeleteConfirm = false) }
        }

        fun onDeleteConfirmed(onDone: () -> Unit) {
            val id = editingItemId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isDeleting = true, showDeleteConfirm = false, formError = null) }
                when (deleteItem(ItemId(id), permanent = false)) {
                    is Result.Success -> onDone()
                    is Result.Failure ->
                        _uiState.update {
                            it.copy(isDeleting = false, formError = STR_DELETE_FAILED)
                        }
                }
            }
        }

        fun onDiscardRequested() {
            _uiState.update { it.copy(showDiscardConfirm = true) }
        }

        fun onDiscardDismissed() {
            _uiState.update { it.copy(showDiscardConfirm = false) }
        }

        // --- Internals ---

        private suspend fun prefill(item: VaultItem): ItemEditorUiState {
            val base = ItemEditorUiState()
            return when (item) {
                is PasswordItem ->
                    base.copy(
                        editableType = EditableType.PASSWORD,
                        title = item.metadata.title,
                        username = item.username.orEmpty(),
                        password = reveal(item.password),
                        url = item.url?.value.orEmpty(),
                        notes =
                            item.metadata.notes
                                ?.value
                                .orEmpty(),
                        totpSecret = item.totp?.let { reveal(it.secret) }.orEmpty(),
                        totpIssuer = item.totp?.issuer.orEmpty(),
                        totpAccount = item.totp?.accountName.orEmpty(),
                    )
                is SecureNote ->
                    base.copy(
                        editableType = EditableType.SECURE_NOTE,
                        title = item.metadata.title,
                        noteContent = reveal(item.content),
                        notes =
                            item.metadata.notes
                                ?.value
                                .orEmpty(),
                    )
                else ->
                    base.copy(
                        editableType = EditableType.UNSUPPORTED,
                        readOnlyTitle = item.metadata.title,
                        readOnlyTypeLabel = item.type.uiLabel,
                        readOnlyUpdated = formatInstant(item.metadata.updatedAt.value),
                    )
            }
        }

        private suspend fun reveal(field: EncryptedField): String {
            val chars =
                when (val result = encryption.decryptField(field)) {
                    is Result.Success -> result.value
                    is Result.Failure -> throw IllegalStateException("decrypt failed")
                }
            return try {
                String(chars)
            } finally {
                chars.fill('\u0000')
            }
        }

        private suspend fun encrypt(text: String): EncryptedField {
            val chars = text.toCharArray()
            return try {
                when (val result = encryption.encryptField(chars)) {
                    is Result.Success -> result.value
                    is Result.Failure -> throw IllegalStateException("encrypt failed")
                }
            } finally {
                chars.fill('\u0000')
            }
        }

        private fun validate(): Boolean {
            val state = _uiState.value
            val titleError = if (state.title.isBlank()) STR_TITLE_REQUIRED else null
            val passwordError =
                if (state.editableType == EditableType.PASSWORD &&
                    state.password.isBlank()
                ) {
                    STR_PASSWORD_REQUIRED
                } else {
                    null
                }
            val contentError =
                if (state.editableType == EditableType.SECURE_NOTE && state.noteContent.isBlank()) {
                    STR_CONTENT_REQUIRED
                } else {
                    null
                }
            val urlError =
                if (state.url.isNotBlank() && !isPlausibleUrl(state.url.trim())) STR_URL_INVALID else null
            val totpSecretError =
                if (state.totpSecret.isNotBlank() && !isPlausibleTotpSecret(state.totpSecret.trim())) {
                    STR_TOTP_SECRET_INVALID
                } else {
                    null
                }
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    passwordError = passwordError,
                    contentError = contentError,
                    urlError = urlError,
                    totpSecretError = totpSecretError,
                )
            }
            return listOf(titleError, passwordError, contentError, urlError, totpSecretError).all { it == null }
        }

        private fun isPlausibleUrl(url: String): Boolean = runCatching { UrlValue(url) }.isSuccess && url.contains('.')

        private fun isPlausibleTotpSecret(secret: String): Boolean =
            runCatching { TotpEngine.generateTotp(secret) }.isSuccess

        private suspend fun persist(): Result<VaultItem, DomainError> {
            val state = _uiState.value
            val resolvedVaultId = vaultId ?: throw IllegalStateException("no vault")
            val now = Instant.now()
            return when (state.editableType) {
                EditableType.PASSWORD -> {
                    val item =
                        PasswordItem(
                            metadata = metadataFor(state, resolvedVaultId, now),
                            username = state.username.trim().takeIf { it.isNotBlank() },
                            password = encrypt(state.password),
                            url =
                                state.url
                                    .trim()
                                    .takeIf { it.isNotBlank() }
                                    ?.let { UrlValue(it) },
                            totp =
                                state.totpSecret.trim().takeIf { it.isNotBlank() }?.let { secret ->
                                    TOTPConfig(
                                        secret = encrypt(secret),
                                        issuer = state.totpIssuer.trim().takeIf { it.isNotBlank() },
                                        accountName = state.totpAccount.trim().takeIf { it.isNotBlank() },
                                    )
                                },
                        )
                    if (state.isCreate) createItem(item) else updateItem(item)
                }
                EditableType.SECURE_NOTE -> {
                    val item =
                        SecureNote(
                            metadata = metadataFor(state, resolvedVaultId, now),
                            content = encrypt(state.noteContent),
                        )
                    if (state.isCreate) createItem(item) else updateItem(item)
                }
                EditableType.UNSUPPORTED -> throw IllegalStateException("unsupported type")
            }
        }

        private fun metadataFor(
            state: ItemEditorUiState,
            vaultId: VaultId,
            now: Instant,
        ): VaultItemMetadata {
            val existing = editingItem?.metadata
            return if (existing != null && !state.isCreate) {
                existing.copy(
                    title = state.title.trim(),
                    notes = state.notes.takeIf { it.isNotBlank() }?.let { Notes(it) },
                    updatedAt = UpdatedAt(now),
                    revision = existing.revision.next(),
                )
            } else {
                VaultItemMetadata(
                    id = ItemId(UUID.randomUUID().toString()),
                    vaultId = vaultId,
                    title = state.title.trim(),
                    notes = state.notes.takeIf { it.isNotBlank() }?.let { Notes(it) },
                    createdAt = CreatedAt(now),
                    updatedAt = UpdatedAt(now),
                )
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorScreen(
    itemId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ItemEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateLifecycleAware()

    LaunchedEffect(itemId) {
        viewModel.load(itemId)
    }

    // Unsaved work in this form consumes the first back press with a question.
    BackHandler(enabled = state.isDirty && !state.isSaving && !state.isLoading) {
        viewModel.onDiscardRequested()
    }

    ItemEditorContent(
        state = state,
        onBack = {
            if (state.isDirty && !state.isSaving) viewModel.onDiscardRequested() else onBack()
        },
        onDone = onDone,
        onTitleChange = viewModel::onTitleChange,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onUrlChange = viewModel::onUrlChange,
        onNotesChange = viewModel::onNotesChange,
        onNoteContentChange = viewModel::onNoteContentChange,
        onTotpSecretChange = viewModel::onTotpSecretChange,
        onTotpIssuerChange = viewModel::onTotpIssuerChange,
        onTotpAccountChange = viewModel::onTotpAccountChange,
        onTypeChange = viewModel::onTypeChange,
        onGeneratePassword = viewModel::onGeneratePassword,
        onSave = { viewModel.onSave(onDone) },
        onRetry = { viewModel.onRetry(itemId) },
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onDeleteConfirmed = { viewModel.onDeleteConfirmed(onDone) },
        onDiscardDismissed = viewModel::onDiscardDismissed,
        onDiscardConfirmed = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditorContent(
    state: ItemEditorUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onTitleChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onNoteContentChange: (String) -> Unit,
    onTotpSecretChange: (String) -> Unit,
    onTotpIssuerChange: (String) -> Unit,
    onTotpAccountChange: (String) -> Unit,
    onTypeChange: (EditableType) -> Unit,
    onGeneratePassword: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDiscardDismissed: () -> Unit,
    onDiscardConfirmed: () -> Unit,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing
    val editable = state.editableType != EditableType.UNSUPPORTED

    Scaffold(
        containerColor = colors.baseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isCreate) {
                            stringResource(R.string.item_editor_title_new)
                        } else {
                            stringResource(R.string.item_editor_title_edit)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.item_editor_cancel_desc),
                        )
                    }
                },
                actions = {
                    if (editable && !state.isLoading && state.loadError == null) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier
                                        .padding(end = spacing.medium)
                                        .size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.accent,
                            )
                        } else {
                            TextButton(
                                onClick = onSave,
                            ) {
                                Text(
                                    text = stringResource(R.string.item_editor_save),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
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
    ) { innerPadding ->
        when {
            state.isLoading ->
                EditorSkeleton(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            state.loadError != null ->
                ErrorState(
                    title = STR_LOAD_FAILED,
                    description = stringResource(R.string.item_editor_load_desc),
                    onRetry = onRetry,
                    retryLabel = stringResource(R.string.item_editor_retry),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            !editable ->
                UnsupportedTypeBody(
                    state = state,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            else ->
                EditorForm(
                    state = state,
                    onTitleChange = onTitleChange,
                    onUsernameChange = onUsernameChange,
                    onPasswordChange = onPasswordChange,
                    onUrlChange = onUrlChange,
                    onNotesChange = onNotesChange,
                    onNoteContentChange = onNoteContentChange,
                    onTotpSecretChange = onTotpSecretChange,
                    onTotpIssuerChange = onTotpIssuerChange,
                    onTotpAccountChange = onTotpAccountChange,
                    onTypeChange = onTypeChange,
                    onGeneratePassword = onGeneratePassword,
                    onDeleteRequested = onDeleteRequested,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
        }
    }

    if (state.showDeleteConfirm) {
        EditorAlertDialog(
            title = stringResource(R.string.item_editor_delete_title),
            description = stringResource(R.string.item_editor_delete_desc),
            confirmLabel = stringResource(R.string.item_editor_delete_confirm),
            dismissLabel = stringResource(R.string.item_editor_keep),
            destructive = true,
            isWorking = state.isDeleting,
            onConfirm = onDeleteConfirmed,
            onDismiss = onDeleteDismissed,
        )
    }
    if (state.showDiscardConfirm) {
        EditorAlertDialog(
            title = stringResource(R.string.item_editor_discard_title),
            description = stringResource(R.string.item_editor_discard_desc),
            confirmLabel = stringResource(R.string.item_editor_discard_confirm),
            dismissLabel = stringResource(R.string.item_editor_keep_editing),
            destructive = true,
            isWorking = false,
            onConfirm = onDiscardConfirmed,
            onDismiss = onDiscardDismissed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorForm(
    state: ItemEditorUiState,
    onTitleChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onNoteContentChange: (String) -> Unit,
    onTotpSecretChange: (String) -> Unit,
    onTotpIssuerChange: (String) -> Unit,
    onTotpAccountChange: (String) -> Unit,
    onTypeChange: (EditableType) -> Unit,
    onGeneratePassword: () -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SecureVaultSpacingScope.spacing
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        if (state.isCreate) {
            SectionHeader(title = stringResource(R.string.item_editor_type_label))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val options =
                    listOf(
                        EditableType.PASSWORD to stringResource(R.string.item_editor_type_login),
                        EditableType.SECURE_NOTE to stringResource(R.string.item_editor_type_note),
                    )
                options.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = state.editableType == type,
                        onClick = { onTypeChange(type) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size,
                            ),
                        colors =
                            SegmentedButtonDefaults.colors(
                                activeContainerColor = SecureVaultColorsScope.colors.accentSoft,
                                activeContentColor = SecureVaultColorsScope.colors.accent,
                            ),
                        label = { Text(label) },
                    )
                }
            }
        }

        VaultTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = stringResource(R.string.item_editor_field_title),
            error = state.titleError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.small),
        )

        if (state.editableType == EditableType.PASSWORD) {
            VaultTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.item_editor_field_username),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            VaultPasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.item_editor_field_password),
                error = state.passwordError,
                modifier = Modifier.fillMaxWidth(),
            )
            VaultSecondaryButton(
                text = stringResource(R.string.item_editor_generate),
                onClick = onGeneratePassword,
                icon = Icons.Filled.Refresh,
                modifier = Modifier.align(Alignment.Start),
            )
            VaultTextField(
                value = state.url,
                onValueChange = onUrlChange,
                label = stringResource(R.string.item_editor_field_website),
                supporting = stringResource(R.string.item_editor_field_url_hint),
                error = state.urlError,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(title = stringResource(R.string.item_editor_totp_section))
            VaultTextField(
                value = state.totpSecret,
                onValueChange = onTotpSecretChange,
                label = stringResource(R.string.item_editor_totp_secret),
                supporting = stringResource(R.string.item_editor_totp_secret_help),
                error = state.totpSecretError,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                VaultTextField(
                    value = state.totpIssuer,
                    onValueChange = onTotpIssuerChange,
                    label = stringResource(R.string.item_editor_totp_issuer),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                VaultTextField(
                    value = state.totpAccount,
                    onValueChange = onTotpAccountChange,
                    label = stringResource(R.string.item_editor_totp_account),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            VaultTextField(
                value = state.noteContent,
                onValueChange = onNoteContentChange,
                label = stringResource(R.string.item_editor_field_content),
                error = state.contentError,
                singleLine = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        VaultTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = stringResource(R.string.item_editor_field_notes),
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.formError != null) {
            InlineError(
                message = state.formError,
                modifier = Modifier.padding(top = spacing.small),
            )
        }

        if (!state.isCreate) {
            VaultDestructiveButton(
                text = stringResource(R.string.item_editor_delete),
                onClick = onDeleteRequested,
                loading = state.isDeleting,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = spacing.large,
                            bottom = spacing.extraLarge,
                        ),
            )
        } else {
            // Bottom breathing room so the last field clears the keyboard area.
            Spacer(
                modifier = Modifier.padding(bottom = spacing.extraLarge),
            )
        }
    }
}

@Composable
private fun UnsupportedTypeBody(
    state: ItemEditorUiState,
    modifier: Modifier = Modifier,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Column(
        modifier = modifier.padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        DetailFieldRow(
            label = stringResource(R.string.item_editor_unsupported_title),
            value = state.readOnlyTitle,
        )
        DetailFieldRow(
            label = stringResource(R.string.item_editor_type_label),
            value = state.readOnlyTypeLabel,
        )
        DetailFieldRow(
            label = stringResource(R.string.item_editor_unsupported_updated),
            value = state.readOnlyUpdated,
        )
        QuietInfoNote(text = stringResource(R.string.item_editor_unsupported_note))
    }
}

/** Confirm dialog shared by delete and discard: one destructive action, one way out. */
@Composable
private fun EditorAlertDialog(
    title: String,
    description: String,
    confirmLabel: String,
    dismissLabel: String,
    destructive: Boolean,
    isWorking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SecureVaultColorsScope.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.label,
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryLabel,
            )
        },
        confirmButton = {
            if (destructive) {
                VaultDestructiveButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    loading = isWorking,
                )
            } else {
                VaultTextButton(text = confirmLabel, onClick = onConfirm)
            }
        },
        dismissButton = {
            VaultTextButton(text = dismissLabel, onClick = onDismiss)
        },
        containerColor = colors.surface,
        titleContentColor = colors.label,
        textContentColor = colors.secondaryLabel,
    )
}

/** Loading skeleton shaped like the form: full-width 12dp field blocks. */
@Composable
private fun EditorSkeleton(modifier: Modifier = Modifier) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Column(
        modifier = modifier.padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(colors.fill),
            )
        }
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun ItemEditorCreatePreview() {
    SecureVaultTheme {
        ItemEditorContent(
            state = ItemEditorUiState(isLoading = false, isCreate = true),
            onBack = {},
            onDone = {},
            onTitleChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onUrlChange = {},
            onNotesChange = {},
            onNoteContentChange = {},
            onTotpSecretChange = {},
            onTotpIssuerChange = {},
            onTotpAccountChange = {},
            onTypeChange = {},
            onGeneratePassword = {},
            onSave = {},
            onRetry = {},
            onDeleteRequested = {},
            onDeleteDismissed = {},
            onDeleteConfirmed = {},
            onDiscardDismissed = {},
            onDiscardConfirmed = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemEditorEditPreview() {
    SecureVaultTheme {
        ItemEditorContent(
            state =
                ItemEditorUiState(
                    isLoading = false,
                    isCreate = false,
                    editableType = EditableType.PASSWORD,
                    title = "GitHub",
                    username = "octocat",
                    url = "https://github.com",
                    notes = "Personal account.",
                ),
            onBack = {},
            onDone = {},
            onTitleChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onUrlChange = {},
            onNotesChange = {},
            onNoteContentChange = {},
            onTotpSecretChange = {},
            onTotpIssuerChange = {},
            onTotpAccountChange = {},
            onTypeChange = {},
            onGeneratePassword = {},
            onSave = {},
            onRetry = {},
            onDeleteRequested = {},
            onDeleteDismissed = {},
            onDeleteConfirmed = {},
            onDiscardDismissed = {},
            onDiscardConfirmed = {},
        )
    }
}
