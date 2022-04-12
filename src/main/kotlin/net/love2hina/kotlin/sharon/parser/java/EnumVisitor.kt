package net.love2hina.kotlin.sharon.parser.java

import com.github.javaparser.ast.body.EnumConstantDeclaration
import com.github.javaparser.ast.body.EnumDeclaration
import com.github.javaparser.ast.expr.*
import net.love2hina.kotlin.sharon.data.*
import net.love2hina.kotlin.sharon.*

/**
 * Enum定義.
 *
 * `enum enum_name`
 */
internal fun Parser.Visitor.visitInEnum(n: EnumDeclaration, arg: Void?) {
    val enumName = n.name.asString()

    // Enum情報
    val enumInfo = EnumInfo()

    // 実体解析
    // コメントの解析
    n.comment.parseJavadoc(enumInfo)

    // 出力開始
    writer.writeStartElement("enum")
    writer.writeAttribute("modifier", getModifier(n.modifiers))
    writer.writeAttribute("name", enumName)
    writer.writeAttribute("fullname", packageStack.getFullName(enumName))

    packageStack.push(enumName)

    // インターフェース
    n.implementedTypes.forEach {
        writer.writeEmptyElement("implements")
        writer.writeAttribute("name", it.name.asString())
    }

    // Javadoc
    writer.writeStartElement("javadoc")
    writer.writeAttribute("since", enumInfo.since)
    writer.writeAttribute("deprecated", enumInfo.deprecated)
    writer.writeAttribute("serial", enumInfo.serial)
    writer.writeAttribute("version", enumInfo.version)
    enumInfo.author.forEach {
        writer.writeStartElement("author")
        writer.writeStrings(it)
        writer.writeEndElement()
    }
    writer.writeEndElement()
    // 説明の出力
    writer.writeStartElement("description")
    writer.writeStrings(enumInfo.description.toString())
    writer.writeEndElement()

    // アノテーション
    n.annotations.forEach { it.accept(this, arg) }
    // エントリー
    n.entries.forEach { it.accept(this, arg) }
    // メンバー
    n.members.forEach { it.accept(this, arg) }

    packageStack.pop()
    writer.writeEndElement()
}

/**
 * Enum値定義.
 */
internal fun Parser.Visitor.visitInEnumConstant(n: EnumConstantDeclaration, arg: Void?) {
    val valueName = n.name.asString()

    // エントリ情報
    val entryInfo = EnumEntryInfo()

    // 実体解析
    // コメントの解析
    n.comment.parseJavadoc(entryInfo)

    // TODO
    //        n.getClassBody().forEach(p -> p.accept(this, arg));

    // 出力開始
    writer.writeStartElement("entry")
    writer.writeAttribute("name", valueName)

    // Javadoc
    writer.writeStartElement("javadoc")
    writer.writeAttribute("since", entryInfo.since)
    writer.writeAttribute("deprecated", entryInfo.deprecated)
    writer.writeAttribute("serial", entryInfo.serial)
    writer.writeEndElement()
    // 説明の出力
    writer.writeStartElement("description")
    writer.writeStrings(entryInfo.description.toString())
    writer.writeEndElement()

    // アノテーション
    n.annotations.forEach { it.accept(this, arg) }
    // パラメーター
    n.arguments.forEach {
        writer.writeStartElement("argument")
        when (it) {
            is NullLiteralExpr -> {
                writer.writeAttribute("type", "null")
            }
            is BooleanLiteralExpr -> {
                writer.writeAttribute("type", "boolean")
                writer.writeStrings(it.value.toString())
            }
            is CharLiteralExpr -> {
                writer.writeAttribute("type", "char")
                writer.writeStrings(it.value)
            }
            is IntegerLiteralExpr -> {
                writer.writeAttribute("type", "integer")
                writer.writeStrings(it.value)
            }
            is LongLiteralExpr -> {
                writer.writeAttribute("type", "long")
                writer.writeStrings(it.value)
            }
            is DoubleLiteralExpr -> {
                writer.writeAttribute("type", "double")
                writer.writeStrings(it.value)
            }
            is StringLiteralExpr -> {
                writer.writeAttribute("type", "string")
                writer.writeStrings(it.value)
            }
            is TextBlockLiteralExpr -> {
                writer.writeAttribute("type", "textblock")
                writer.writeStrings(it.value)
            }
            else -> {
                writer.writeAttribute("type", "composite")
            }
        }
        writer.writeEndElement()
    }

    writer.writeEndElement()
}
