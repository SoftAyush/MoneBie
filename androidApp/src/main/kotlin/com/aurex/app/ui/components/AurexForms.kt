package com.aurex.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aurex.app.ui.theme.AurexTheme

/*
 * The form kit.
 *
 * Every entry screen in the application asks the same shapes of question - a
 * label over a field, a group of related fields, one choice out of two or
 * three, an amount in the user's currency - and each of them had grown its own
 * copy of the answer. They live here once so a form reads the same wherever it
 * is opened, and so the field a user learns on one screen behaves identically
 * on the next.
 */

/** Uppercase label above a field. */
@Composable
fun FormLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

/**
 * Validation message under the field it belongs to.
 *
 * It grows in and shrinks out rather than appearing instantly: an error that
 * pops into existence shifts everything below it in one frame, and the user
 * loses their place in the form.
 */
@Composable
fun FieldError(
    message: String?,
    modifier: Modifier = Modifier,
    centered: Boolean = false
) {
    // Held so the text is still there to read on the way out.
    var lastMessage by remember { mutableStateOf(message.orEmpty()) }
    if (message != null) lastMessage = message

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Text(
            text = lastMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .padding(top = AurexTheme.spacing.small)
                .then(if (centered) Modifier.fillMaxWidth() else Modifier)
        )
    }
}

/**
 * A group of related questions, on one raised card under its own heading.
 *
 * A long form as one flat column of fields gives the user no way to tell where
 * one decision ends and the next begins; the card is what turns "eleven fields"
 * into "three things to fill in".
 */
@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PremiumSectionHeader(title = title)

        Spacer(Modifier.height(AurexTheme.spacing.small))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AurexTheme.spacing.large),
                content = content
            )
        }
    }
}

/**
 * One question: its label, the field itself, and whatever the field needs to
 * say about what it did or wants.
 *
 * [hint] explains a consequence before it happens - what a blank field will
 * fall back to, what a date implies - and sits under the field rather than
 * above it, where it would push the field away from its own label.
 */
@Composable
fun FormField(
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormLabel(text = label)
            if (trailing != null) {
                Spacer(Modifier.weight(1f))
                trailing()
            }
        }

        Spacer(Modifier.height(AurexTheme.spacing.small))

        content()

        if (hint != null) {
            Spacer(Modifier.height(AurexTheme.spacing.extraSmall))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FieldError(error)
    }
}

/** An amount in the user's currency, with the symbol standing outside the number. */
@Composable
fun MoneyInputField(
    value: String,
    currencySymbol: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0.00",
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall
) {
    AurexTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = textStyle,
        prefix = {
            Text(
                text = currencySymbol,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/** One option in a [SegmentedSelector]. */
data class Segment<T>(
    val value: T,
    val label: String,
    /** The colour this option means - income green, money owed red, and so on. */
    val accent: Color
)

/**
 * A small, mutually exclusive choice, shown all at once.
 *
 * Used where the options are few and the choice changes what the rest of the
 * form means - the type of a transaction, the direction of a loan. Every option
 * stays visible with its own colour, because these are exactly the choices a
 * user must not make by accident.
 */
@Composable
fun <T> SegmentedSelector(
    segments: List<Segment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val trackShape = RoundedCornerShape(18.dp)
    val segmentShape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(scheme.surface.copy(alpha = 0.9f))
            .border(1.dp, scheme.outline.copy(alpha = 0.20f), trackShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEach { segment ->
            val isSelected = segment.value == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(segmentShape)
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(segment.accent.copy(alpha = 0.16f))
                                .border(1.dp, segment.accent.copy(alpha = 0.45f), segmentShape)
                        } else {
                            Modifier
                        }
                    )
                    .pressable { onSelect(segment.value) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) segment.accent else scheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * A standing note on a form: something the user should read before filling it
 * in, rather than a complaint about what they have already typed.
 */
@Composable
fun FormNotice(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    val tone = accent ?: MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.copy(alpha = 0.08f))
            .border(1.dp, tone.copy(alpha = 0.22f), shape)
            .padding(AurexTheme.spacing.medium),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AurexTheme.spacing.small)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (accent != null) tone else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

