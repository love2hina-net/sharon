package net.love2hina.kotlin.sharon.parser.kotlin

import net.love2hina.kotlin.sharon.data.ConstructorInfo
import net.love2hina.kotlin.sharon.data.TypeParameterInfo
import net.love2hina.kotlin.sharon.getModifier
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

internal fun Parser.Visitor.visitInPrimaryCtor(ctor: KtPrimaryConstructor) {
    // コンストラクタ情報
    val ctorInfo = ConstructorInfo()

    // 実体解析
    // タイプパラメーター
    ctor.typeParameters.forEach {
        it.name?.let { name -> ctorInfo.typeParameters[name] = TypeParameterInfo(name) }
    }
    // パラメーター
    // ctor.

    // コメントの解析
    ctor.docComment.parseKdoc(ctorInfo)

    // 出力開始
    writer.writeStartElement("method")
    writer.writeAttribute("type", "constructor")
    writer.writeAttribute("modifier", getModifier(ctor.modifierList))
    writer.writeAttribute("name", null)

    // 定義全体
    writer.writeStartElement("definitions")
    writer.writeStrings(ctor.text)
    writer.writeEndElement()

    // 説明の出力
    writer.writeStartElement("description")
    writer.writeStrings(ctorInfo.description.toString())
    writer.writeEndElement()
    // タイプパラメーターの出力
    ctorInfo.typeParameters.forEach {
        writer.writeStartElement("typeParameter")
        writer.writeAttribute("type", it.value.type)
        writer.writeStrings(it.value.description)
        writer.writeEndElement()
    }
    // パラメーターの出力
    ctorInfo.parameters.forEach {
        writer.writeStartElement("parameter")
        writer.writeAttribute("modifier", it.value.modifier)
        writer.writeAttribute("type", it.value.type)
        writer.writeAttribute("name", it.value.name)
        writer.writeStrings(it.value.description)
        writer.writeEndElement()
    }

    // アノテーション
    // TODO:

    // ステートメント
    writer.writeStartElement("code")
    val body = ctor.parent.getChildOfType<KtClassBody>()
    for (init in body?.getChildrenOfType<KtClassInitializer>() ?: arrayOf()) {
        val block = init.getChildOfType<KtBlockExpression>()
        block?.acceptChildren(this)
    }
    writer.writeEndElement()

    writer.writeEndElement()
}
