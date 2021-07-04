package net.love2hina.kotlin.sharon

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList

internal fun getModifier(modifiers: NodeList<Modifier>): String {
    return modifiers.stream()
        .map{ it.keyword.asString() }
        .reduce(StringBuilder(),
            { a: StringBuilder, i: String ->
                if (a.isNotEmpty()) a.append(',')
                a.append(i)
            },
            { a: StringBuilder, b: StringBuilder ->
                if (a.isNotEmpty()) a.append(',')
                a.append(b)
            }).toString()
}
