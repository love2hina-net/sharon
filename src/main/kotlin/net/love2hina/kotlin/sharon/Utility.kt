package net.love2hina.kotlin.sharon

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList

private const val NEWLINE = "\r\n"
private const val COMMA = ','

internal fun <Ts, Tv> StringBuilder.appendSeparator(s: Ts, v: Tv): StringBuilder {
    return (if (this.isEmpty()) { this } else { this.append(s) }).append(v)
}

internal fun <T> StringBuilder.appendNewLine(v: T): StringBuilder = this.appendSeparator(NEWLINE, v)

internal fun getModifier(modifiers: NodeList<Modifier>): String {
    return modifiers.stream()
        .map{ it.keyword.asString() }
        .reduce(StringBuilder(),
            { a: StringBuilder, i: String -> a.appendSeparator(COMMA, i) },
            { a: StringBuilder, b: StringBuilder -> a.appendSeparator(COMMA, b) }).toString()
}
