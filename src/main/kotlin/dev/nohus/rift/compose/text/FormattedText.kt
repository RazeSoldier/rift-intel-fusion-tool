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
    data class CustomColor(override val target: SpanTarget, val color: androidx.compose.ui.graphics.Color) : Span(target)
    data class Weight(override val target: SpanTarget, val fontWeight: FontWeight) : Span(target)
    data class Italics(override val target: SpanTarget) : Span(target)
    data class Underline(override val target: SpanTarget) : Span(target)
    data class Size(override val target: SpanTarget, val size: Int) : Span(target)
    data class Url(override val target: SpanTarget, val url: String) : Span(target)
    data class InGameLink(override val target: SpanTarget, val url: String) : Span(target)
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
