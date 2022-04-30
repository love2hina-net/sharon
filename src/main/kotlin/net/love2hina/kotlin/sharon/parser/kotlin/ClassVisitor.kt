package net.love2hina.kotlin.sharon.parser.kotlin

import net.love2hina.kotlin.sharon.data.*
import net.love2hina.kotlin.sharon.*
import org.jetbrains.kotlin.psi.KtClass

/**
 * クラス定義.
 *
 * `class class_name`
 */
internal fun Parser.Visitor.visitInClass(klass: KtClass) {
    val className = klass.name ?: ""

    // クラス情報
    val classInfo = ClassInfo()

    // 実体解析
    // タイプパラメーター
    klass.typeParameters.forEach {
        it.name?.let { name -> classInfo.typeParameters[name] = TypeParameterInfo(name) }
    }

    // コメントの解析
    klass.docComment.parseKdoc(classInfo)

    // 出力開始
    writer.writeStartElement("class")
    writer.writeAttribute("modifier", getModifier(klass.modifierList))
    writer.writeAttribute("name", className)
    writer.writeAttribute("fullname", klass.fqName?.asString() ?: "")

    packageStack.push(className)

    // TODO

    packageStack.pop()
    writer.writeEndElement()
}
