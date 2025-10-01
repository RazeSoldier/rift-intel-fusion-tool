package dev.nohus.rift.compose.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import dev.nohus.rift.compose.theme.RiftTheme

@Composable
fun FormattedText.toAnnotatedString(): AnnotatedString {
    return when (this) {
        is FormattedText.Plain -> AnnotatedString(text)
        is FormattedText.Formatted -> toAnnotatedString()
        is FormattedText.Compound -> buildAnnotatedString {
            texts.forEach { append(it.toAnnotatedString()) }
        }
    }
}

@Composable
private fun FormattedText.Formatted.toAnnotatedString(): AnnotatedString {
    val text = text.toAnnotatedString()
    return buildAnnotatedString {
        append(text)
        spans.forEach { span ->
            val indices = span.target.getIndices(text)
            when (span) {
                is Span.Color -> {
                    val color = when (span.color) {
                        FormattedTextColor.Highlighted -> RiftTheme.colors.textHighlighted
                        FormattedTextColor.Primary -> RiftTheme.colors.textPrimary
                        FormattedTextColor.Secondary -> RiftTheme.colors.textSecondary
                        FormattedTextColor.Disabled -> RiftTheme.colors.textDisabled
                    }
                    addStyle(SpanStyle(color = color), indices.start, indices.end)
                }

                is Span.Weight -> {
                    addStyle(SpanStyle(fontWeight = span.fontWeight), indices.start, indices.end)
                }
            }
        }
    }
}

private data class SpanIndices(
    val start: Int,
    val end: Int,
)

@Composable
private fun SpanTarget.getIndices(text: AnnotatedString): SpanIndices {
    return when (this) {
        is SpanTarget.Range -> SpanIndices(startIndex, endIndex)
        is SpanTarget.Text -> {
            val start = text.indexOf(this.text)
            SpanIndices(start, start + this.text.length)
        }
        is SpanTarget.Formatted -> {
            val string = this.text.toAnnotatedString().text
            val start = text.indexOf(string)
            SpanIndices(start, start + string.length)
        }
        is SpanTarget.Full -> SpanIndices(0, text.length)
    }
}
