package com.monebie.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monebie.app.domain.model.Currency
import com.monebie.app.ui.theme.MoneBieTheme

@Composable
fun CurrencySelector(
    currencies: List<Currency>, selected: Currency, onCurrencySelected: (Currency) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    MoneBieTheme {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSheet = true },
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MoneBieTheme.spacing.medium, vertical = MoneBieTheme.spacing.medium
                    ), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selected.flag, fontSize = 30.sp)

                Spacer(Modifier.width(MoneBieTheme.spacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected.code,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = selected.name, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Select currency",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showSheet) {
        CurrencyBottomSheet(
            currencies = currencies,
            selected = selected,
            onDismiss = { showSheet = false },
            onSelect = {
                onCurrencySelected(it)
                showSheet = false
            })
    }
}
