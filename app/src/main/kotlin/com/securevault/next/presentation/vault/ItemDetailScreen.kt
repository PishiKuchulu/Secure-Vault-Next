package com.securevault.next.presentation.vault

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.securevault.next.core.domain.model.enums.ItemType
import com.securevault.next.core.domain.model.item.ApiKey
import com.securevault.next.core.domain.model.item.BankAccount
import com.securevault.next.core.domain.model.item.CreditCard
import com.securevault.next.core.domain.model.item.IdentityDocument
import com.securevault.next.core.domain.model.item.PasswordItem
import com.securevault.next.core.domain.model.item.RecoveryCode
import com.securevault.next.core.domain.model.item.SSHKey
import com.securevault.next.core.domain.model.item.SecureNote
import com.securevault.next.core.domain.model.item.SoftwareLicense
import com.securevault.next.core.domain.model.item.TOTPConfig
import com.securevault.next.core.domain.model.item.TOTPSeed
import com.securevault.next.core.domain.model.item.VaultItem
import com.securevault.next.core.domain.model.search.SearchFilter
import com.securevault.next.core.domain.model.valueobject.EncryptedField
import com.securevault.next.core.domain.repository.VaultRepository
import com.securevault.next.core.domain.usecase.SearchItemsUseCase
import com.securevault.next.core.platform.clipboard.SecureClipboardManager
import com.securevault.next.core.ui.components.ErrorState
import com.securevault.next.core.ui.components.SectionHeader
import com.securevault.next.core.ui.theme.SecureVaultColorsScope
import com.securevault.next.core.ui.theme.SecureVaultSpacingScope
import com.securevault.next.core.ui.theme.SecureVaultTheme
import com.securevault.next.core.ui.theme.footnote
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Screen copy is centralized in strings.xml (item_detail_*). The field labels,
// copy descriptions and the load-error description stay consts: they are used in
// the ViewModel's suspend decrypt() path, outside composition.
private const val STR_FIELD_USERNAME = "Username"
private const val STR_FIELD_PASSWORD = "Password"
private const val STR_FIELD_URL = "Website"
private const val STR_ERROR_DESC = "It may have been deleted or the vault may be locked."
private const val STR_COPY_USERNAME_DESC = "Copy username"
private const val STR_COPY_PASSWORD_DESC = "Copy password"
private const val STR_COPY_URL_DESC = "Copy website"

/** One decrypted detail row: label, plaintext value, whether it starts masked. */
data class DetailFieldUi(
    val label: String,
    val value: String,
    val sensitive: Boolean,
    val copyDescription: String,
)

/** Live TOTP material, decrypted once at load and re-rendered every second. */
data class TotpUi(
    val secret: String,
    val issuer: String?,
    val accountName: String?,
    val digits: Int,
    val periodSeconds: Int,
)

sealed interface DecryptedItemUi {
    val id: String
    val title: String
    val type: ItemType
    val updatedAt: Instant

    data class Password(
        override val id: String,
        override val title: String,
        override val updatedAt: Instant,
        val fields: List<DetailFieldUi>,
        val notes: String?,
        val totp: TotpUi?,
    ) : DecryptedItemUi {
        override val type: ItemType = ItemType.PASSWORD
    }

    data class Note(
        override val id: String,
        override val title: String,
        override val updatedAt: Instant,
        val content: String,
    ) : DecryptedItemUi {
        override val type: ItemType = ItemType.SECURE_NOTE
    }

    data class Other(
        override val id: String,
        override val title: String,
        override val type: ItemType,
        override val updatedAt: Instant,
        val fields: List<DetailFieldUi>,
        val totp: TotpUi? = null,
    ) : DecryptedItemUi
}

sealed interface ItemDetailUiState {
    data object Loading : ItemDetailUiState
    data class Loaded(
        val item: DecryptedItemUi,
        val copiedLabel: String? = null,
    ) : ItemDetailUiState
    data class Error(val message: String) : ItemDetailUiState
}

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val searchItems: SearchItemsUseCase,
    private val encryption: ItemEncryptionEngine,
    private val clipboard: SecureClipboardManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemDetailUiState>(ItemDetailUiState.Loading)
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    fun load(itemId: String) {
        viewModelScope.launch {
            _uiState.value = ItemDetailUiState.Loading
            val vaultId = vaultRepository.firstVaultId()
            if (vaultId == null) {
                _uiState.value = ItemDetailUiState.Error(STR_ERROR_DESC)
                return@launch
            }
            val items = searchItems(vaultId, SearchFilter()).getOrNull()?.items
            if (items == null) {
                _uiState.value = ItemDetailUiState.Error(STR_ERROR_DESC)
                return@launch
            }
            val item = items.firstOrNull { it.metadata.id.value == itemId }
            if (item == null) {
                _uiState.value = ItemDetailUiState.Error(STR_ERROR_DESC)
                return@launch
            }
            _uiState.value = try {
                ItemDetailUiState.Loaded(decrypt(item))
            } catch (e: Exception) {
                ItemDetailUiState.Error(STR_ERROR_DESC)
            }
        }
    }

    fun onRetry(itemId: String) = load(itemId)

    fun onCopy(label: String, value: String) {
        clipboard.copyToClipboard(label, value)
        val current = _uiState.value
        if (current is ItemDetailUiState.Loaded) {
            _uiState.update { current.copy(copiedLabel = label) }
        }
    }

    private suspend fun reveal(field: EncryptedField): String {
        val chars = when (val result = encryption.decryptField(field)) {
            is Result.Success -> result.value
            is Result.Failure -> throw IllegalStateException("decrypt failed: ${result.error.message}")
        }
        return try {
            String(chars)
        } finally {
            chars.fill('\u0000')
        }
    }

    private suspend fun decrypt(item: VaultItem): DecryptedItemUi {
        val metadata = item.metadata
        return when (item) {
            is PasswordItem -> DecryptedItemUi.Password(
                id = metadata.id.value,
                title = metadata.title,
                updatedAt = metadata.updatedAt.value,
                fields = buildList {
                    item.username?.takeIf { it.isNotBlank() }?.let {
                        add(
                            DetailFieldUi(
                                STR_FIELD_USERNAME,
                                it,
                                sensitive = false,
                                copyDescription = STR_COPY_USERNAME_DESC,
                            ),
                        )
                    }
                    add(
                        DetailFieldUi(
                            label = STR_FIELD_PASSWORD,
                            value = reveal(item.password),
                            sensitive = true,
                            copyDescription = STR_COPY_PASSWORD_DESC,
                        ),
                    )
                    item.url?.let {
                        add(
                            DetailFieldUi(
                                STR_FIELD_URL,
                                it.value,
                                sensitive = false,
                                copyDescription = STR_COPY_URL_DESC,
                            ),
                        )
                    }
                },
                notes = metadata.notes?.value,
                totp = item.totp?.let { config ->
                    TotpUi(
                        secret = reveal(config.secret),
                        issuer = config.issuer,
                        accountName = config.accountName,
                        digits = config.digits,
                        periodSeconds = config.periodSeconds,
                    )
                },
            )
            is SecureNote -> DecryptedItemUi.Note(
                id = metadata.id.value,
                title = metadata.title,
                updatedAt = metadata.updatedAt.value,
                content = reveal(item.content),
            )
            is CreditCard -> DecryptedItemUi.Other(
                id = metadata.id.value,
                title = metadata.title,
                type = item.type,
                updatedAt = metadata.updatedAt.value,
                fields = buildList {
                    add(
                        DetailFieldUi(
                            "Cardholder",
                            item.cardholderName,
                            sensitive = false,
                            copyDescription = "Copy cardholder name",
                        ),
                    )
                    add(
                        DetailFieldUi(
                            "Card number",
                            reveal(item.cardNumber),
                            sensitive = true,
                            copyDescription = "Copy card number",
                        ),
                    )
                    add(
                        DetailFieldUi(
                            "Expires",
                            "%02d/%d".format(item.expiryMonth, item.expiryYear),
                            sensitive = false,
                            copyDescription = "Copy expiry date",
                        ),
                    )
                    add(
                        DetailFieldUi(
                            "Security code",
                            reveal(item.cvv),
                            sensitive = true,
                            copyDescription = "Copy security code",
                        ),
                    )
                    item.pin?.let {
                        add(DetailFieldUi("PIN", reveal(it), sensitive = true, copyDescription = "Copy PIN"))
                    }
                },
            )
            is BankAccount -> DecryptedItemUi.Other(
                id = metadata.id.value,
                title = metadata.title,
                type = item.type,
                updatedAt = metadata.updatedAt.value,
                fields = buildList {
                    add(DetailFieldUi("Bank", item.bankName, sensitive = false, copyDescription = "Copy bank name"))
                    add(
                        DetailFieldUi(
                            "Account holder",
                            item.accountHolderName,
                            sensitive = false,
                            copyDescription = "Copy account holder",
                        ),
                    )
                    add(
                        DetailFieldUi(
                            "Account number",
                            reveal(item.accountNumber),
                            sensitive = true,
                            copyDescription = "Copy account number",
                        ),
                    )
                    item.routingNumber?.let {
                        add(
                            DetailFieldUi(
                                "Routing number",
                                reveal(it),
                                sensitive = true,
                                copyDescription = "Copy routing number",
                            ),
                        )
                    }
                    item.iban?.let {
                        add(DetailFieldUi("IBAN", reveal(it), sensitive = true, copyDescription = "Copy IBAN"))
                    }
                    item.swiftCode?.let {
                        add(DetailFieldUi("SWIFT code", it, sensitive = false, copyDescription = "Copy SWIFT code"))
                    }
                },
            )
            is IdentityDocument -> DecryptedItemUi.Other(
                id = metadata.id.value,
                title = metadata.title,
                type = item.type,
                updatedAt = metadata.updatedAt.value,
                fields = buildList {
                    add(
                        DetailFieldUi(
                            "Document type",
                            item.documentType,
                            sensitive = false,
                            copyDescription = "Copy document type",
                        ),
                    )
                    add(
                        DetailFieldUi(
                            "Full name",
                            item.fullName,
                            sensitive = false,
                            copyDescription = "Copy full name",
                        ),
                    )
                    add(
                        DetailFieldUi(
                            "Document number",
                            reveal(item.documentNumber),
                            sensitive = true,
                            copyDescription = "Copy document number",
                        ),
                    )
                    item.issuingCountry?.let {
                        add(
                            DetailFieldUi(
                                "Issuing country",
                                it,
                                sensitive = false,
                                copyDescription = "Copy issuing country",
                            ),
                        )
                    }
                },
            )
            is SoftwareLicense -> DecryptedItemUi.Other(
                id = metadata.id.value,
                title = metadata.title,
                type = item.type,
                updatedAt = metadata.updatedAt.value,
                fields = buildList {
                    add(
                        DetailFieldUi(
                            "Product",
                            item.productName,
                            sensitive = false,
                            copyDescription = "Copy product name",
                        ),
                    )
                    add(
                        DetailFieldUi(
                            "License key",
                            reveal(item.licenseKey),
                            sensitive = true,
                            copyDescription = "Copy license key",
                        ),
                    )
                    item.licensedTo?.let {
                        add(DetailFieldUi("Licensed to", it, sensitive = false, copyDescription = "Copy licensee"))
                    }
                    item.vendor?.let {
                        add(DetailFieldUi("Vendor", it, sensitive = false, copyDescription = "Copy vendor"))
                    }
                },
            )
            is ApiKey -> DecryptedItemUi.Other(
                id = metadata.id.value,
                title = metadata.title,
                type = item.type,
                updatedAt = metadata.updatedAt.value,
                fields = buildList {
                    add(DetailFieldUi("Service", item.service, sensitive = false, copyDescription = "Copy service"))
                    add(DetailFieldUi("Key", reveal(item.keyValue), sensitive = true, copyDescription = "Copy API key"))
                    item.keySecret?.let {
                        add(DetailFieldUi("Secret", reveal(it), sensitive = true, copyDescription = "Copy API secret"))
                    }
                    item.environment?.let {
                        add(DetailFieldUi("Environment", it, sensitive = false, copyDescription = "Copy environment"))
                    }
                },
            )
            is SSHKey -> DecryptedItemUi.Other(
                id = metadata.id.value,
                title = metadata.title,
                type = item.type,
                updatedAt = metadata.updatedAt.value,
                fields = buildList {
                    add(DetailFieldUi("Key type", item.keyType, sensitive = false, copyDescription = "Copy key type"))
                    add(
                        DetailFieldUi(
                            "Private key",
                            reveal(item.privateKey),
                            sensitive = true,
                            copyDescription = "Copy private key",
                        ),
                    )
                    item.publicKey?.let {
                        add(DetailFieldUi("Public key", it, sensitive = false, copyDescription = "Copy public key"))
                    }
                    item.passphrase?.let {
                        add(
                            DetailFieldUi(
                                "Passphrase",
                                reveal(it),
                                sensitive = true,
                                copyDescription = "Copy passphrase",
                            ),
                        )
                    }
                    item.hostAddress?.let {
                        add(DetailFieldUi("Host", it, sensitive = false, copyDescription = "Copy host"))
                    }
                },
            )
            is RecoveryCode -> DecryptedItemUi.Other(
                id = metadata.id.value,
                title = metadata.title,
                type = item.type,
                updatedAt = metadata.updatedAt.value,
                fields = buildList {
                    add(DetailFieldUi("Service", item.service, sensitive = false, copyDescription = "Copy service"))
                    item.codes.forEachIndexed { index, code ->
                        if (index !in item.usedCodeIndices) {
                            add(
                                DetailFieldUi(
                                    "Code ${index + 1}",
                                    reveal(code),
                                    sensitive = true,
                                    copyDescription = "Copy recovery code ${index + 1}",
                                ),
                            )
                        }
                    }
                },
            )
            is TOTPSeed -> DecryptedItemUi.Other(
                id = metadata.id.value,
                title = metadata.title,
                type = item.type,
                updatedAt = metadata.updatedAt.value,
                fields = buildList {
                    item.config.issuer?.let {
                        add(DetailFieldUi("Issuer", it, sensitive = false, copyDescription = "Copy issuer"))
                    }
                    item.config.accountName?.let {
                        add(DetailFieldUi("Account", it, sensitive = false, copyDescription = "Copy account name"))
                    }
                },
                totp = TotpUi(
                    secret = reveal(item.config.secret),
                    issuer = item.config.issuer,
                    accountName = item.config.accountName,
                    digits = item.config.digits,
                    periodSeconds = item.config.periodSeconds,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: String,
    onEdit: (itemId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateLifecycleAware()

    LaunchedEffect(itemId) {
        viewModel.load(itemId)
    }

    ItemDetailContent(
        state = state,
        onEdit = { onEdit(itemId) },
        onBack = onBack,
        onRetry = { viewModel.onRetry(itemId) },
        onCopy = viewModel::onCopy,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailContent(
    state: ItemDetailUiState,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing
    val title = (state as? ItemDetailUiState.Loaded)?.item?.title ?: ""

    Scaffold(
        containerColor = colors.baseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.item_detail_back_desc),
                        )
                    }
                },
                actions = {
                    if (state is ItemDetailUiState.Loaded) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.item_detail_edit_desc),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.baseBackground,
                    titleContentColor = colors.label,
                    navigationIconContentColor = colors.label,
                    actionIconContentColor = colors.accent,
                ),
            )
        },
    ) { innerPadding ->
        when (state) {
            is ItemDetailUiState.Loading -> DetailSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            is ItemDetailUiState.Error -> ErrorState(
                title = stringResource(R.string.item_detail_error_title),
                description = state.message,
                onRetry = onRetry,
                retryLabel = stringResource(R.string.item_detail_retry),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            is ItemDetailUiState.Loaded -> LoadedDetail(
                item = state.item,
                copiedLabel = state.copiedLabel,
                onCopy = onCopy,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun LoadedDetail(
    item: DecryptedItemUi,
    copiedLabel: String?,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SecureVaultSpacingScope.spacing
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        when (item) {
            is DecryptedItemUi.Password -> {
                DetailCard(fields = item.fields, copiedLabel = copiedLabel, onCopy = onCopy)
                item.totp?.let { totp ->
                    SectionHeader(title = stringResource(R.string.item_detail_section_totp))
                    TotpCard(totp = totp, onCopy = onCopy)
                }
                val notesLabel = stringResource(R.string.item_detail_field_notes)
                item.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    SectionHeader(title = notesLabel)
                    NotesCard(
                        notes = notes,
                        copied = copiedLabel == notesLabel,
                        onCopy = { onCopy(notesLabel, notes) },
                    )
                }
            }
            is DecryptedItemUi.Note -> {
                val noteLabel = stringResource(R.string.item_detail_field_notes)
                NotesCard(
                    notes = item.content,
                    copied = copiedLabel == noteLabel,
                    onCopy = { onCopy(noteLabel, item.content) },
                )
            }
            is DecryptedItemUi.Other -> {
                SectionHeader(title = item.type.uiLabel)
                if (item.fields.isNotEmpty()) {
                    DetailCard(fields = item.fields, copiedLabel = copiedLabel, onCopy = onCopy)
                }
                item.totp?.let { totp ->
                    SectionHeader(title = stringResource(R.string.item_detail_section_totp))
                    TotpCard(totp = totp, onCopy = onCopy)
                }
                QuietInfoNote(
                    text = stringResource(R.string.item_detail_editing_soon_note),
                    modifier = Modifier.padding(horizontal = spacing.medium),
                )
            }
        }
        Text(
            text = stringResource(R.string.item_detail_updated, formatInstant(item.updatedAt)),
            style = MaterialTheme.typography.footnote,
            color = SecureVaultColorsScope.colors.secondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = SecureVaultSpacingScope.spacing.medium,
                    bottom = SecureVaultSpacingScope.spacing.extraLarge,
                ),
        )
    }
}

/** Inset-grouped card of detail rows with hairlines between them. */
@Composable
private fun DetailCard(
    fields: List<DetailFieldUi>,
    copiedLabel: String?,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium),
        shape = MaterialTheme.shapes.large,
        color = colors.surface,
    ) {
        Column {
            fields.forEachIndexed { index, field ->
                DetailFieldRow(
                    label = field.label,
                    value = field.value,
                    sensitive = field.sensitive,
                    copyContentDescription = field.copyDescription,
                    onCopy = { onCopy(field.label, field.value) },
                    showCopiedHint = copiedLabel == field.label,
                    copiedHintText = stringResource(R.string.item_detail_copied_hint),
                )
                if (index < fields.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = spacing.medium),
                        thickness = 1.dp,
                        color = colors.separator,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    copied: Boolean,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium),
        shape = MaterialTheme.shapes.large,
        color = colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = spacing.medium,
                vertical = spacing.small + spacing.extraSmall,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            SelectionContainer {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.label,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (copied) {
                    Text(
                        text = stringResource(R.string.item_detail_copied_hint),
                        style = MaterialTheme.typography.footnote,
                        color = colors.success,
                        modifier = Modifier.padding(end = spacing.small),
                    )
                }
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.item_detail_copy_notes_desc),
                        tint = colors.accent,
                    )
                }
            }
        }
    }
}

/**
 * Live TOTP block: the 6-digit code in tabular numerals, a countdown
 * ring for the current 30-second window, and a copy button. Ticks on
 * second boundaries so the ring never jumps.
 */
@Composable
private fun TotpCard(
    totp: TotpUi,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    var now by remember(totp) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(totp) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - (now % 1000L))
        }
    }

    val code = remember(now, totp) {
        runCatching {
            TotpEngine.generateTotp(
                secretBase32 = totp.secret,
                timeMillis = now,
                digits = totp.digits,
                periodSeconds = totp.periodSeconds,
                algorithm = TOTPConfig.DEFAULT_ALGORITHM,
            )
        }.getOrNull()
    }
    val remaining = TotpEngine.getRemainingSeconds(now, totp.periodSeconds)
    val fraction = remaining.toFloat() / totp.periodSeconds.toFloat()
    val trackColor = colors.fill
    val progressColor = colors.accent

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium),
        shape = MaterialTheme.shapes.large,
        color = colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (totp.issuer != null || totp.accountName != null) {
                Text(
                    text = listOfNotNull(totp.issuer, totp.accountName).joinToString(" · "),
                    style = MaterialTheme.typography.footnote,
                    color = colors.secondaryLabel,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                if (code != null) {
                    Text(
                        text = formatTotpCode(code),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        color = colors.label,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.item_detail_totp_invalid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.danger,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Countdown ring: track + progress, round caps.
                Canvas(modifier = Modifier.size(44.dp)) {
                    val stroke = 4.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                val totpLabel = stringResource(R.string.item_detail_section_totp)
                if (code != null) {
                    IconButton(onClick = { onCopy(totpLabel, code) }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.item_detail_totp_copy_desc),
                            tint = colors.accent,
                        )
                    }
                }
            }
            Text(
                text = "$remaining",
                style = MaterialTheme.typography.footnote.copy(fontFeatureSettings = "tnum"),
                color = colors.secondaryLabel,
            )
        }
    }
}

/** Loading skeleton shaped like the final detail: card blocks with text lines. */
@Composable
private fun DetailSkeleton(modifier: Modifier = Modifier) {
    val colors = SecureVaultColorsScope.colors
    val spacing = SecureVaultSpacingScope.spacing

    Column(
        modifier = modifier.padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        repeat(2) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.surface,
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .fillMaxWidth(0.6f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.fill),
                        )
                    }
                }
            }
        }
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun ItemDetailPasswordPreview() {
    SecureVaultTheme {
        ItemDetailContent(
            state = ItemDetailUiState.Loaded(
                item = DecryptedItemUi.Password(
                    id = "1",
                    title = "GitHub",
                    updatedAt = Instant.now(),
                    fields = listOf(
                        DetailFieldUi(
                            STR_FIELD_USERNAME,
                            "octocat",
                            sensitive = false,
                            copyDescription = STR_COPY_USERNAME_DESC,
                        ),
                        DetailFieldUi(
                            STR_FIELD_PASSWORD,
                            "correct-horse-battery-staple",
                            sensitive = true,
                            copyDescription = STR_COPY_PASSWORD_DESC,
                        ),
                        DetailFieldUi(
                            STR_FIELD_URL,
                            "https://github.com",
                            sensitive = false,
                            copyDescription = STR_COPY_URL_DESC,
                        ),
                    ),
                    notes = "Personal account.",
                    totp = TotpUi(
                        secret = "JBSWY3DPEHPK3PXP",
                        issuer = "GitHub",
                        accountName = "octocat",
                        digits = 6,
                        periodSeconds = 30,
                    ),
                ),
            ),
            onEdit = {},
            onBack = {},
            onRetry = {},
            onCopy = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailNotePreview() {
    SecureVaultTheme {
        ItemDetailContent(
            state = ItemDetailUiState.Loaded(
                item = DecryptedItemUi.Note(
                    id = "2",
                    title = "Wi-Fi details",
                    updatedAt = Instant.now(),
                    content = "Network: Home-5G\nPassword: hunter2",
                ),
            ),
            onEdit = {},
            onBack = {},
            onRetry = {},
            onCopy = { _, _ -> },
        )
    }
}
