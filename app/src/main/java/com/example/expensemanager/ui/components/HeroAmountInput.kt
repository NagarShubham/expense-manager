package com.example.expensemanager.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.expensemanager.R
import com.example.expensemanager.ui.theme.ExpenseManagerTheme
import com.example.expensemanager.ui.theme.TabularFigures
import com.example.expensemanager.ui.theme.appColors

/**
 * A rupee amount entered at display size on the brand gradient — the "this number is
 * the point of the screen" field. Used by Add/Edit Expense and by Budget Settings,
 * which ask for the same thing and so should look and behave identically.
 *
 * [BasicTextField] rather than a text field component because the hero styling leaves
 * nothing of a Material field visible: the currency symbol is a sibling, the label is
 * an [OverlineText], and the placeholder is drawn behind the field so the caret still
 * sits at the start when the value is empty.
 */
@Composable
internal fun HeroAmountInput(
    label: String,
    amountText: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = MaterialTheme.appColors
    HeroGradientCard(modifier = modifier) {
        OverlineText(
            text = label,
            color = appColors.onHeroMuted
        )
        VSpace(AppSpacing.small)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.currency_symbol),
                style = MaterialTheme.typography.headlineMedium,
                color = appColors.onHeroMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            HSpace(AppSpacing.small)
            Box(modifier = Modifier.weight(1f)) {
                if (amountText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.expense_amount_placeholder),
                        style = MaterialTheme.typography.displaySmall.merge(TabularFigures),
                        color = appColors.onHero.copy(alpha = 0.4f)
                    )
                }
                BasicTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    textStyle = MaterialTheme.typography.displaySmall
                        .merge(TabularFigures)
                        .copy(color = appColors.onHero),
                    singleLine = true,
                    cursorBrush = SolidColor(appColors.onHero),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Narrows a keystroke to what an amount can contain: digits and at most one decimal
 * point. Returns null for an edit that would produce a second point ("1.2.3"), which
 * the caller treats as "reject this keystroke and keep what was there".
 */
internal fun sanitizeAmountInput(raw: String): String? {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    return filtered.takeIf { it.count { c -> c == '.' } <= 1 }
}

@Preview(showBackground = true, name = "Hero amount input")
@Composable
private fun HeroAmountInputPreview() {
    ExpenseManagerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(AppSpacing.screen)) {
                HeroAmountInput(
                    label = "Amount",
                    amountText = "4200",
                    onAmountChange = {}
                )
            }
        }
    }
}
