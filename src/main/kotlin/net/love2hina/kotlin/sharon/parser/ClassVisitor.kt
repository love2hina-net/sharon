package net.love2hina.kotlin.sharon.parser

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import net.love2hina.kotlin.sharon.data.*
import net.love2hina.kotlin.sharon.*

/**
 * クラス定義.
 *
 * `class class_name`
 */
internal fun Parser.Visitor.visitInClass(n: ClassOrInterfaceDeclaration, arg: Void?) {
    val className = n.name.asString()

    // クラス情報
    val classInfo = ClassInfo()

    // 実体解析
    // タイプパラメーター
    n.typeParameters.forEach {
        val name = it.name.asString()
        classInfo.typeParameters[name] = TypeParameterInfo(name)
    }

    // コメントの解析
    n.comment.ifPresent {
        if (it.isJavadocComment) {
            val content = getCommentContents(it)
            var index = 0
            var name: String? = null

            // Javadocをパースする
            for (match in REGEXP_ANNOTATION.findAll(content)) {
                index = pushJavadocComment(content, name, index, match.range, classInfo)
                name = (match.groups as MatchNamedGroupCollection)["name"]!!.value
            }
            pushJavadocComment(content, name, index, IntRange(content.length, content.length), classInfo)
        }
        else if (it.isBlockComment) {
            val content = getCommentContents(it)
            classInfo.description.appendNewLine(content)
        }
        else {
            classInfo.description.appendNewLine(it.content)
        }
    }

    // 出力開始
    writer.writeStartElement("class")
    writer.writeAttribute("modifier", getModifier(n.modifiers))
    writer.writeAttribute("name", className)
    writer.writeAttribute("fullname", packageStack.getFullName(className))

    packageStack.push(className)

    // 継承クラス
    n.extendedTypes.forEach {
        writer.writeEmptyElement("extends")
        writer.writeAttribute("name", it.name.asString())
    }
    // インターフェース
    n.implementedTypes.forEach {
        writer.writeEmptyElement("implements")
        writer.writeAttribute("name", it.name.asString())
    }

    // Javadoc
    writer.writeStartElement("javadoc")
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

    // アノテーション
    n.annotations.forEach { it.accept(this, arg) }

    // メンバー
    n.members.forEach { it.accept(this, arg) }

    packageStack.pop()
    writer.writeEndElement()
}
