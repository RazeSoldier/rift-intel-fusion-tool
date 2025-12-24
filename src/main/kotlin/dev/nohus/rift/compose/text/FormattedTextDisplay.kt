package dev.nohus.rift.compose.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation.Clickable
import androidx.compose.ui.text.LinkAnnotation.Url
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.di.koin
import dev.nohus.rift.game.GameUiController
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull

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

                is Span.Italics -> {
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), indices.start, indices.end)
                }

                is Span.Underline -> {
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), indices.start, indices.end)
                }

                is Span.CustomColor -> {
                    addStyle(SpanStyle(color = span.color), indices.start, indices.end)
                }

                is Span.Size -> {
                    with(LocalDensity.current) {
                        addStyle(SpanStyle(fontSize = span.size.toSp()), indices.start, indices.end)
                    }
                }

                is Span.Url -> {
                    val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
                    val annotation = Url(
                        url = span.url,
                        styles = TextLinkStyles(
                            style = linkStyle,
                            hoveredStyle = linkStyle.copy(textDecoration = TextDecoration.Underline),
                        ),
                        linkInteractionListener = {
                            span.url.toURIOrNull()?.openBrowser()
                        },
                    )
                    addLink(annotation, indices.start, indices.end)
                }

                is Span.InGameLink -> {
                    val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
                    val gameUiController: GameUiController = remember { koin.get() }
                    val text = text.text.substring(indices.start, indices.end)
                    val annotation = Clickable(
                        tag = span.url,
                        styles = TextLinkStyles(
                            style = linkStyle,
                            hoveredStyle = linkStyle.copy(textDecoration = TextDecoration.Underline),
                        ),
                        linkInteractionListener = {
                            gameUiController.pushUrl(span.url, text)
                        },
                    )
                    addLink(annotation, indices.start, indices.end)
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
