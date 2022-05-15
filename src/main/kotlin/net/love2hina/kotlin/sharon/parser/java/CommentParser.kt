package net.love2hina.kotlin.sharon.parser.java

import com.github.javaparser.ast.comments.Comment
import net.love2hina.kotlin.sharon.appendNewLine
import net.love2hina.kotlin.sharon.data.*
import java.util.*

internal fun Optional<Comment>.parseJavadoc(info: JavadocInfo) {
    this.ifPresent {
        if (it.isJavadocComment) {
            val content = getCommentContents(it)
            var index = 0
            var name: String? = null

            // Javadocをパースする
            for (match in REGEXP_ANNOTATION.findAll(content)) {
                index = info.pushJavadocComment(content, name, index, match.range)
                name = (match.groups as MatchNamedGroupCollection)["name"]!!.value
            }
            info.pushJavadocComment(content, name, index, IntRange(content.length, content.length))
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
