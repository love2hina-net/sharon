package net.love2hina.kotlin.sharon

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtModifierList
import java.util.stream.Stream

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

private const val NEWLINE = "\r\n"
private const val COMMA = ','

internal fun <Ts, Tv> StringBuilder.appendSeparator(s: Ts, v: Tv): StringBuilder {
    return (if (this.isEmpty()) { this } else { this.append(s) }).append(v)
}

internal fun <T> StringBuilder.appendNewLine(v: T): StringBuilder = this.appendSeparator(NEWLINE, v)

internal fun setProperty(target: Any, cls: KClass<*>, name: String, value: Any) {
    val property = cls.declaredMemberProperties
        .filterIsInstance<KMutableProperty<*>>()
        .first { it.name == name }

    property.setter.isAccessible = true
    property.setter.call(target, value)
}

internal inline fun <reified T: Any> T.setProperty(name: String, value: Any) {
    setProperty(this, T::class, name, value)
}

internal fun getProperty(target: Any, cls: KClass<*>, name: String): Any? {
    val property = cls.declaredMemberProperties
        .filterIsInstance<KMutableProperty<*>>()
        .first { it.name == name }

    property.getter.isAccessible = true
    return property.getter.call(target)
}

internal inline fun <reified T: Any> T.getProperty(name: String): Any? {
    return getProperty(this, T::class, name)
}

internal fun getModifier(modifiers: NodeList<Modifier>): String =
    modifiers.stream()
        .map{ it.keyword.asString() }
        .reduce(StringBuilder(),
            { a: StringBuilder, i: String -> a.appendSeparator(COMMA, i) },
            { a: StringBuilder, b: StringBuilder -> a.appendSeparator(COMMA, b) }).toString()

internal fun getModifier(modifiers: KtModifierList?): String  =
    (modifiers?.childStream() ?: Stream.of())
        .map { it.text }
        .reduce(StringBuilder(),
            { a: StringBuilder, i: String -> a.appendSeparator(COMMA, i) },
            { a: StringBuilder, b: StringBuilder -> a.appendSeparator(COMMA, b) }).toString()

internal fun PsiElement.childStream(): Stream<PsiElement> {
    // TODO: 先に列挙してしまっている
    val list = mutableListOf<PsiElement>()

    var node = this.firstChild
    while (node != null) {
        list.add(node)
        node = node.nextSibling
    }

    return list.stream()
}
