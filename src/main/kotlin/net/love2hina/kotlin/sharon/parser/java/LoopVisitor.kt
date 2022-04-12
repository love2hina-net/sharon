package net.love2hina.kotlin.sharon.parser.java

import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.WhileStmt

/**
 * forステートメント.
 */
internal fun Parser.Visitor.visitInFor(n: ForStmt, arg: Void?) {
    writer.writeStartElement("loop")
    writer.writeAttribute("type", "for")

    // コメント
    n.comment.ifPresent { it.accept(this, arg) }
    // 初期化子
    n.initialization.forEach {
        writer.writeEmptyElement("initializer")
        writer.writeAttribute("expr", it.toString())
    }
    // 条件
    n.compare.ifPresent {
        writer.writeEmptyElement("compare")
        writer.writeAttribute("expr", it.toString())
    }
    // 更新
    n.update.forEach {
        writer.writeEmptyElement("update")
        writer.writeAttribute("expr", it.toString())
    }
    // 本文
    writer.writeStartElement("code")
    n.body.accept(this, arg)
    writer.writeEndElement()

    writer.writeEndElement()
}

/**
 * for eachステートメント.
 */
internal fun Parser.Visitor.visitInForEach(n: ForEachStmt, arg: Void?) {
    writer.writeStartElement("loop")
    writer.writeAttribute("type", "for-each")

    // コメント
    n.comment.ifPresent { it.accept(this, arg) }
    // イテレータ
    writer.writeEmptyElement("iterator")
    writer.writeAttribute("expression", n.iterable.toString())
    // 変数
    writer.writeEmptyElement("variable")
    writer.writeAttribute("expression", n.variable.toString())
    // 本文
    writer.writeStartElement("code")
    n.body.accept(this, arg)
    writer.writeEndElement()

    writer.writeEndElement()
}

/**
 * whileステートメント.
 */
internal fun Parser.Visitor.visitInWhile(n: WhileStmt, arg: Void?) {
    writer.writeStartElement("loop")
    writer.writeAttribute("type", "while")

    // コメント
    n.comment.ifPresent { it.accept(this, arg) }
    // 条件
    writer.writeEmptyElement("condition")
    writer.writeAttribute("expr", n.condition.toString())
    // 本文
    writer.writeStartElement("code")
    n.body.accept(this, arg)
    writer.writeEndElement()

    writer.writeEndElement()
}

/**
 * do~whileステートメント.
 */
internal fun Parser.Visitor.visitInDo(n: DoStmt, arg: Void?) {
    writer.writeStartElement("loop")
    writer.writeAttribute("type", "do")

    // コメント
    n.comment.ifPresent { it.accept(this, arg) }
    // 条件
    writer.writeEmptyElement("condition")
    writer.writeAttribute("expr", n.condition.toString())
    // 本文
    writer.writeStartElement("code")
    n.body.accept(this, arg)
    writer.writeEndElement()

    writer.writeEndElement()
}
