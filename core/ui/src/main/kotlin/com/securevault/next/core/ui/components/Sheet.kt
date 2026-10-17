package com.securevault.next.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.securevault.next.core.ui.theme.SecureVaultColorsScope

/*
 * Shape lock: sheets are 28dp top corners only. A sheet is a short
 * interruption (picker, filters, item options) with one way out;
 * a sheet that grows a second step was a modal all along.
 */

/**
 * App-styled bottom sheet over Material3's [ModalBottomSheet].
 *
 * Resolves the surface token, the 28dp top-corner rule and a standard
 * drag handle; screens only supply content. Drag-to-dismiss stays
 * enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SecureVaultColorsScope.colors

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
        contentColor = colors.label,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = dragHandle,
        content = content,
    )
}

/**
 * Sheet title row helper: keeps sheet titles consistent (titleLarge,
 * label color) without each screen inventing its own header.
 */
@Composable
fun ColumnScope.VaultSheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = SecureVaultColorsScope.colors.label,
    )
}
