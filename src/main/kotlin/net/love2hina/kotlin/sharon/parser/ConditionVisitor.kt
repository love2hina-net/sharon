package net.love2hina.kotlin.sharon.parser

import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.SwitchStmt
import java.util.*

/**
 * Ifステートメント.
 */
internal fun Parser.Visitor.visitInIf(n: IfStmt, arg: Void?) {
    // 条件分岐の出力
    writer.writeStartElement("condition")
    writer.writeAttribute("type", "if")

    // 第1条件
    writer.writeStartElement("case")

    // コメント
    n.comment.ifPresent { it.accept(this, arg) }
    // 条件
    writer.writeStartElement("expr")
    writer.writeStrings(n.condition.toString())
    writer.writeEndElement()
    // 本文
    writer.writeStartElement("code")
    n.thenStmt.accept(this, arg)
    writer.writeEndElement()

    writer.writeEndElement()

    // 継続条件
    visitInElse(n.elseStmt, arg)

    writer.writeEndElement()
}

/**
 * Elseステートメント.
 */
private fun Parser.Visitor.visitInElse(n: Optional<Statement>, arg: Void?) {

    n.ifPresent { e ->
        if (e is IfStmt) {
            // 継続条件(else if)
            writer.writeStartElement("case")

            // コメント
            e.comment.ifPresent { it.accept(this, arg) }
            // 条件
            writer.writeStartElement("expr")
            writer.writeStrings(e.condition.toString())
            writer.writeEndElement()
            // 本文
            writer.writeStartElement("code")
            e.thenStmt.accept(this, arg)
            writer.writeEndElement()

            writer.writeEndElement()

            // 継続条件(再帰呼び出し)
            visitInElse(e.elseStmt, arg)
        }
        else {
            // 条件なし(else)
            writer.writeStartElement("case")

            // コメント
            e.comment.ifPresent { it.accept(this, arg) }
            // 本文
            writer.writeStartElement("code")
            e.accept(this, arg)
            writer.writeEndElement()

            writer.writeEndElement()
        }
    }
}

/**
 * switchステートメント.
 */
internal fun Parser.Visitor.visitInSwitch(n: SwitchStmt, arg: Void?) {
    // コメント
    n.comment.ifPresent { it.accept(this, arg) }

    // 条件分岐の出力
    writer.writeStartElement("condition")
    writer.writeAttribute("type", "switch")
    writer.writeAttribute("selector", n.selector.toString())

    var caseContinue = false

    // 条件エントリ
    for (i in n.entries) {
        if (!caseContinue) { writer.writeStartElement("case") }

        // コメント
        i.comment.ifPresent { it.accept(this, arg) }
        // 条件
        i.labels.forEach {
            writer.writeStartElement("expr")
            writer.writeStrings(it.toString())
            writer.writeEndElement()
        }
        // 本文
        writer.writeStartElement("code")
        i.statements.forEach { it.accept(this, arg) }
        writer.writeEndElement()

        caseContinue = i.statements.isEmpty()
        if (!caseContinue) { writer.writeEndElement() }
    }

    writer.writeEndElement()
}
