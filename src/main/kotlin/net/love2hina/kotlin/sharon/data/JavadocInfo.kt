package net.love2hina.kotlin.sharon.data

import com.github.javaparser.ast.comments.Comment
import net.love2hina.kotlin.sharon.appendNewLine
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import java.util.Optional

internal interface JavadocInfo {

    val description: StringBuilder

    fun parseTag(name: String?, value: String): Boolean

}

internal fun Optional<Comment>.parseJavadoc(info: JavadocInfo) {
    this.ifPresent {
        if (it.isJavadocComment) {
            val content = getCommentContents(it)
            var index = 0
            var name: String? = null

            // Javadocをパースする
            for (match in REGEXP_ANNOTATION.findAll(content)) {
                index = pushJavadocComment(content, name, index, match.range, info)
                name = (match.groups as MatchNamedGroupCollection)["name"]!!.value
            }
            pushJavadocComment(content, name, index, IntRange(content.length, content.length), info)
        }
        else if (it.isBlockComment) {
            val content = getCommentContents(it)
            info.description.appendNewLine(content)
        }
        else {
            info.description.appendNewLine(it.content)
        }
    }
}

internal fun KDoc?.parseKdoc(info: JavadocInfo) {
    this?.let {
        val content = getCommentContents(it)
        var index = 0
        var name: String? = null

        // KDocをパースする
        for (match in REGEXP_ANNOTATION.findAll(content)) {
            index = pushJavadocComment(content, name, index, match.range, info)
            name = (match.groups as MatchNamedGroupCollection)["name"]!!.value
        }
        pushJavadocComment(content, name, index, IntRange(content.length, content.length), info)
    }
}

private fun getCommentContents(comment: Comment): String =
    comment.content.split(REGEXP_NEWLINE)
        .stream().map { l ->
            val m = REGEXP_BLOCK_COMMENT.find(l)
            if (m != null)
                (m.groups as MatchNamedGroupCollection)["content"]?.value ?: ""
            else l
        }
        .reduce(StringBuilder(),
            { b, l -> b.appendNewLine(l) },
            { b1, b2 -> b1.appendNewLine(b2) })
        .toString()

private fun getCommentContents(doc: KDoc): String =
    doc.text.split(REGEXP_NEWLINE)
        .stream().map { l ->
            val m = REGEXP_BLOCK_COMMENT.find(l)
            if (m != null)
                (m.groups as MatchNamedGroupCollection)["content"]?.value ?: ""
            else l
        }
        .reduce(StringBuilder(),
            { b, l -> b.appendNewLine(l) },
            { b1, b2 -> b1.appendNewLine(b2) })
        .toString()

private fun pushJavadocComment(content: String, name: String?, start: Int, range: IntRange, info: JavadocInfo ): Int {

    if (start < range.start) {
        val value = content.substring(start, range.start).trim()

        when {
            info.parseTag(name, value) -> {}
            name == null -> {
                info.description.appendNewLine(value)
            }
            else -> {
                info.description.appendNewLine("@$name $value")
            }
        }
    }

    return range.endInclusive + 1
}
