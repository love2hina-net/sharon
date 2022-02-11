package net.love2hina.kotlin.sharon.parser

import com.github.javaparser.ast.body.MethodDeclaration
import net.love2hina.kotlin.sharon.data.*
import net.love2hina.kotlin.sharon.*

/**
 * 関数定義.
 */
internal fun Parser.Visitor.visitInFunction(n: MethodDeclaration, arg: Void?) {
    // メソッド情報
    val methodInfo = MethodInfo()

    // 実体解析
    // タイプパラメーター
    n.typeParameters.forEach {
        val name = it.name.asString()
        methodInfo.typeParameters[name] = TypeParameterInfo(name)
    }
    // 明示的なthisパラメーター
    n.receiverParameter.ifPresent {
        val name = it.name.asString()
        methodInfo.parameters[name] = ParameterInfo("", it.type.asString(), name)
    }
    // パラメーター
    n.parameters.forEach {
        val name = it.name.asString()
        methodInfo.parameters[name] = ParameterInfo(
            getModifier(it.modifiers),
            it.type.asString(),
            name
        )
    }
    // 戻り値
    if (!n.type.isVoidType) {
        methodInfo.result = ReturnInfo(n.type.asString())
    }
    // 例外
    n.thrownExceptions.forEach {
        val type = it.asString()
        methodInfo.throws[type] = ThrowsInfo(type)
    }

    // コメントの解析
    n.comment.ifPresent {
        if (it.isJavadocComment) {
            // Javadocコメントから、*を取り除く
            val content = getCommentContents(it)
            var index = 0
            var name: String? = null

            // Javadocをパースする
            for (match in REGEXP_ANNOTATION.findAll(content)) {
                index = pushJavadocComment(content, name, index, match.range, methodInfo)
                name = (match.groups as MatchNamedGroupCollection)["name"]!!.value
            }
            pushJavadocComment(content, name, index, IntRange(content.length, content.length), methodInfo)
        }
        else if (it.isBlockComment) {
            val content = getCommentContents(it)
            methodInfo.description.appendNewLine(content)
        }
        else {
            methodInfo.description.appendNewLine(it.content)
        }
    }

    // 出力開始
    writer.writeStartElement("method")
    writer.writeAttribute("modifier", getModifier(n.modifiers))
    writer.writeAttribute("name", n.name.asString())

    // 定義全体
    writer.writeStartElement("definition")
    writer.writeStrings(n.getDeclarationAsString(true, true, true))
    writer.writeEndElement()

    // 説明の出力
    writer.writeStartElement("description")
    writer.writeStrings(methodInfo.description.toString())
    writer.writeEndElement()
    // タイプパラメーターの出力
    methodInfo.typeParameters.forEach {
        writer.writeStartElement("typeParameter")
        writer.writeAttribute("type", it.value.type)
        writer.writeStrings(it.value.description)
        writer.writeEndElement()
    }
    // パラメーターの出力
    methodInfo.parameters.forEach {
        writer.writeStartElement("parameter")
        writer.writeAttribute("modifier", it.value.modifier)
        writer.writeAttribute("type", it.value.type)
        writer.writeAttribute("name", it.value.name)
        writer.writeStrings(it.value.description)
        writer.writeEndElement()
    }
    // 戻り値の出力
    methodInfo.result?.let {
        writer.writeStartElement("return")
        writer.writeAttribute("type", it.type)
        writer.writeStrings(it.description)
        writer.writeEndElement()
    }
    // 例外の出力
    methodInfo.throws.forEach {
        writer.writeStartElement("throws")
        writer.writeAttribute("type", it.value.type)
        writer.writeStrings(it.value.description)
        writer.writeEndElement()
    }

    // アノテーション
    n.annotations.forEach { it.accept(this, arg) }

    // ステートメント
    n.body.ifPresent {
        writer.writeStartElement("code")
        it.accept(this, arg)
        writer.writeEndElement()
    }

    writer.writeEndElement()
}
