package net.love2hina.kotlin.sharon.data

import net.love2hina.kotlin.sharon.appendNewLine

internal interface JavadocInfo {

    val description: StringBuilder

    fun parseTag(name: String?, value: String): Boolean

}

internal fun JavadocInfo.pushJavadocComment(content: String, name: String?, start: Int, range: IntRange): Int {

    if (start < range.start) {
        val value = content.substring(start, range.start).trim()

        when {
            this.parseTag(name, value) -> {}
            name == null -> {
                this.description.appendNewLine(value)
            }
            else -> {
                this.description.appendNewLine("@$name $value")
            }
        }
    }

    return range.endInclusive + 1
}
