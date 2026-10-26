package com.monebie.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.monebie.app.ui.theme.MoneBieTheme

@Composable
fun MoneBieTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    prefix: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    singleLine: Boolean = true
) {

    var isFocused by remember {
        mutableStateOf(false)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        textStyle = textStyle.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        ),
        cursorBrush = SolidColor(
            MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                isFocused = it.isFocused
            },
        decorationBox = { innerTextField ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(MoneBieTheme.spacing.medium)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(MoneBieTheme.spacing.medium)
                    )
                    .padding(
                        horizontal = MoneBieTheme.spacing.large,
                        vertical = MoneBieTheme.spacing.medium
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(MoneBieTheme.spacing.medium))
                }

                prefix?.let {
                    it()
                    Spacer(Modifier.width(MoneBieTheme.spacing.small))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    innerTextField()
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun MoneBieTextFieldPreview() {
    MoneBieTheme {
        Box(modifier = Modifier.padding(MoneBieTheme.spacing.medium)) {
            MoneBieTextField(
                value = "",
                onValueChange = {},
                placeholder = "e.g. Jane Doe"
            )
        }
    }
}
