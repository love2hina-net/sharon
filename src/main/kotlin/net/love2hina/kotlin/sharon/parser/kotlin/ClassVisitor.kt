package net.love2hina.kotlin.sharon.parser.kotlin

import net.love2hina.kotlin.sharon.data.*
import net.love2hina.kotlin.sharon.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

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

    klass.superTypeListEntries.forEach {
        if (it is KtSuperTypeCallEntry) {
            // 継承クラス
            writer.writeEmptyElement("extends")
            writer.writeAttribute("name", it.getChildOfType<KtConstructorCalleeExpression>()?.text)
        }
        else {
            // インターフェース
            writer.writeEmptyElement("implements")
            writer.writeAttribute("name", it.text)
        }
    }

    // KDoc
    writer.writeStartElement("kdoc")
    writer.writeAttribute("since", classInfo.since)
    writer.writeAttribute("deprecated", classInfo.deprecated)
    writer.writeAttribute("serial", classInfo.serial)
    writer.writeAttribute("version", classInfo.version)
    classInfo.author.forEach {
        writer.writeStartElement("author")
        writer.writeStrings(it)
        writer.writeEndElement()
    }
    writer.writeEndElement()
    // 説明の出力
    writer.writeStartElement("description")
    writer.writeStrings(classInfo.description.toString())
    writer.writeEndElement()
    // タイプパラメーターの出力
    classInfo.typeParameters.forEach {
        writer.writeStartElement("typeParameter")
        writer.writeAttribute("type", it.value.type)
        writer.writeStrings(it.value.description)
        writer.writeEndElement()
    }

    // メンバー
    klass.acceptChildren(this)

    packageStack.pop()
    writer.writeEndElement()
}
