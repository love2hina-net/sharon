package net.love2hina.kotlin.sharon.parser.kotlin

import net.love2hina.kotlin.sharon.appendNewLine
import net.love2hina.kotlin.sharon.data.*
import org.jetbrains.kotlin.kdoc.psi.api.KDoc

internal fun KDoc?.parseKdoc(info: JavadocInfo) {
    this?.let {
        val content = getCommentContents(it)
        var index = 0
        var name: String? = null

        // KDocをパースする
        for (match in REGEXP_ANNOTATION.findAll(content)) {
            index = info.pushJavadocComment(content, name, index, match.range)
            name = (match.groups as MatchNamedGroupCollection)["name"]!!.value
        }
        info.pushJavadocComment(content, name, index, IntRange(content.length, content.length))
    }
}

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
