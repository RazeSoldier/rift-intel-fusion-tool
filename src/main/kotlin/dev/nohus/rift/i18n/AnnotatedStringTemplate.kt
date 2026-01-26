package dev.nohus.rift.i18n

import androidx.compose.ui.text.AnnotatedString
import java.util.LinkedList
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * A class for handling [AnnotatedString] templates, which allows for the creation and expansion of
 * text with placeholders. The template is composed of groups, each potentially containing
 * placeholders to be replaced by actual values. It supports conditional processing of groups
 * through predicates and provides strategies for how the text blocks are processed.
 *
 * Use [Builder] to build this template then use [expand] to expand all groups
 *
 * @param groups A list of [Group] objects that make up the template.
 * @param annotationBuilder An [AnnotatedString.Builder] used to construct the final annotated string.
 *
 * @see Group
 * @see AnnotatedString
 */
class AnnotatedStringTemplate(
    private val groups: List<Group>,
    private val annotationBuilder: AnnotatedString.Builder,
) {
    fun expand() {
        groups.forEach {
            if (it.isOption && it.predicate?.invoke() != true) {
                return@forEach
            }
            if (it.whatShouldPassBlock == BlockParameterType.JUST_PLACEHOLDER_TEXT) {
                justPlaceholderTextGroupStrategy(it)
            } else {
                wholeTextGroupStrategy(it)
            }
        }
    }

    /**
     * Represents the type of text block parameter to be used in a group processing strategy.
     *
     * This enum is utilized to determine whether the entire text including placeholders or just
     * the placeholder texts should be passed through a specific processing block. The two options
     * available are:
     * - [WHOLE_GROUP_TEXT]: Indicates that the whole text, including any placeholders, should be
     *   processed as a single unit.
     * - [JUST_PLACEHOLDER_TEXT]: Indicates that only the text within placeholders should be
     *   individually processed, with the surrounding text being handled separately.
     */
    enum class BlockParameterType {
        WHOLE_GROUP_TEXT,
        JUST_PLACEHOLDER_TEXT,
    }

    private fun wholeTextGroupStrategy(group: Group) {
        group.placeholders.forEach { (string, function) ->
            group.text = group.text.replace("$$string", function.invoke() ?: "")
        }
        if (group.block == null) {
            annotationBuilder.append(group.text)
        } else {
            group.block.invoke(group.text)
        }
    }

    private fun justPlaceholderTextGroupStrategy(group: Group) {
        var originalText = group.text
        var substring = originalText.substringBefore("$")
        while (originalText != substring) {
            annotationBuilder.append(substring)
            val placeholder = getPlaceholder("$" + originalText.substringAfter("$")) ?: continue
            val placeholderName = placeholder.substringAfter("$")
            val replaceProvider = group.placeholders[placeholderName] ?: { "" }
            if (group.block == null) {
                annotationBuilder.append(group.text)
            } else {
                group.block.invoke(replaceProvider.invoke() ?: "")
            }
            originalText = originalText.substringAfter("$substring$$placeholderName")
            substring = originalText.substringBefore("$")
        }
        annotationBuilder.append(originalText)
    }

    companion object {
        fun getPlaceholder(text: String): String? {
            return "\\$[a-zA-Z_\\x80-\\xff][a-zA-Z0-9_\\x80-\\xff]*".toRegex().find(text)?.value
        }

        fun parseGroup(text: String): List<GroupBuilder> {
            val groupBuilders = LinkedList<GroupBuilder>()
            "(\\[[^]]*])|([^\\[\\]]+)".toRegex().findAll(text).forEach {
                val isOptionalGroup = it.value.contains("\\[.*]".toRegex())
                val builder =
                    GroupBuilder(isOptionalGroup, it.value.removePrefix("[").removeSuffix("]"))
                "\\$([a-zA-Z0-9]+)".toRegex().findAll(it.value).forEach {
                    builder.placeholders[it.groupValues[1]] = { null }
                }
                groupBuilders.add(builder)
            }
            return groupBuilders
        }
    }

    class Builder(private val annotatedStringBuilder: AnnotatedString.Builder) {
        private val groups = arrayListOf<Group>()

        fun addGroup(group: Group) {
            groups.add(group)
        }

        fun build(): AnnotatedStringTemplate {
            return AnnotatedStringTemplate(groups, annotatedStringBuilder)
        }
    }

    data class Group(
        val isOption: Boolean,
        var text: String,
        val placeholders: Map<String, () -> String?> = mapOf(),
        val predicate: (() -> Boolean)? = null,
        val whatShouldPassBlock: BlockParameterType = BlockParameterType.JUST_PLACEHOLDER_TEXT,
        val block: ((String) -> Unit)? = null
    )

    class GroupBuilder(val isOption: Boolean, private val text: String) {
        var placeholders: MutableMap<String, () -> String?> = mutableMapOf()
        var predicate: (() -> Boolean)? = null
        var whatShouldPassBlock: BlockParameterType = BlockParameterType.JUST_PLACEHOLDER_TEXT
        var block: ((String) -> Unit)? = null
        fun build(): Group {
            return Group(isOption, text, placeholders, predicate, whatShouldPassBlock, block)
        }
    }
}

fun List<AnnotatedStringTemplate.GroupBuilder>.optionGroups(): List<AnnotatedStringTemplate.GroupBuilder> {
    return filter { it.isOption }
}