package dev.nohus.rift.compose.text

import androidx.compose.ui.text.font.FontWeight

sealed interface FormattedText {
    data class Plain(
        val text: String,
    ) : FormattedText

    data class Formatted(
        val text: FormattedText,
        val spans: List<Span>,
    ) : FormattedText

    data class Compound(
        val texts: List<FormattedText>,
    ) : FormattedText
}

sealed class Span(open val target: SpanTarget) {
    data class Color(override val target: SpanTarget, val color: FormattedTextColor) : Span(target)
    data class Weight(override val target: SpanTarget, val fontWeight: FontWeight) : Span(target)
}

sealed interface SpanTarget {
    data class Range(val startIndex: Int, val endIndex: Int) : SpanTarget
    data class Text(val text: String) : SpanTarget
    data class Formatted(val text: FormattedText) : SpanTarget
    object Full : SpanTarget
}

enum class FormattedTextColor {
    Highlighted,
    Primary,
    Secondary,
    Disabled,
}
