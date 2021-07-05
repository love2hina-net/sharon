package net.love2hina.kotlin.sharon

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.modules.ModuleDeclaration
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*

internal class Parser(val file: File) {

    fun parse(xml: File) {
        val unit = StaticJavaParser.parse(file)

        // XML
        val xmlWriter = SmartXMLStreamWriter(xml)

        xmlWriter.use {
            xmlWriter.writeStartDocument(UTF_8.name(), "1.0")

            unit.accept(Visitor(xmlWriter), null)

            xmlWriter.writeEndDocument()
            xmlWriter.flush()
            xmlWriter.close()
        }
        // https://qiita.com/opengl-8080/items/50ddee7d635c7baee0ab
    }

    private inner class Visitor(
        val writer: SmartXMLStreamWriter
    ): VoidVisitorAdapter<Void>() {

        private val packageStack = PackageStack()

        /**
         * コンパイル単位.
         *
         * つまりファイル
         */
        override fun visit(n: CompilationUnit?, arg: Void?) {
            n!!

            writer.writeStartElement("file")
            writer.writeAttribute("language", "java")
            writer.writeAttribute("src", this@Parser.file.canonicalPath)

            // モジュール
            n.module.ifPresent { it.accept(this, arg) }
            // パッケージ宣言
            n.packageDeclaration.ifPresent { it.accept(this, arg) }
            // インポート
            n.imports.forEach { it.accept(this, arg) }
            // 型宣言
            n.types.forEach { it.accept(this, arg) }

            writer.writeEndElement()
        }

        override fun visit(n: JavadocComment?, arg: Void?) {
            n!!

            writer.writeStartElement("comment")
            writer.writeStrings(n.content)
            writer.writeEndElement()
        }

        override fun visit(n: BlockComment?, arg: Void?) {
            n!!

            writer.writeStartElement("comment")
            writer.writeStrings(n.content)
            writer.writeEndElement()
        }

        override fun visit(n: LineComment?, arg: Void?) {
            n!!

            writer.writeStartElement("comment")
            writer.writeStrings(n.content)
            writer.writeEndElement()
        }

        /**
         * インポート.
         *
         * `import package_name;`
         */
        override fun visit(n: ImportDeclaration?, arg: Void?) {
            n!!

            writer.writeEmptyElement("import")
            writer.writeAttribute("package", n.name.asString())
        }

        /**
         * モジュール定義.
         *
         * `modules module_name`
         */
        override fun visit(n: ModuleDeclaration?, arg: Void?) {
            // 特に処理しない
        }

        /**
         * パッケージ定義.
         *
         * `package package_name;`
         */
        override fun visit(n: PackageDeclaration?, arg: Void?) {
            val name = n!!.name.asString()

            packageStack.push(name)

            writer.writeEmptyElement("package")
            writer.writeAttribute("package", name)
        }

        /**
         * Enum定義.
         *
         * `enum enum_name`
         */
        override fun visit(n: EnumDeclaration?, arg: Void?) {
            val name = n!!.name.asString()

            writer.writeStartElement("enum")
            writer.writeAttribute("modifier", getModifier(n.modifiers))
            writer.writeAttribute("name", name)
            writer.writeAttribute("fullname", packageStack.getFullName(name))

            // TODO

            super.visit(n, arg)
            writer.writeEndElement()
        }

        /**
         * Enum値定義.
         */
        override fun visit(n: EnumConstantDeclaration?, arg: Void?) {
            // TODO
            super.visit(n, arg)
        }

        /**
         * アノテーション定義.
         *
         * `@interface anon_name`
         */
        override fun visit(n: AnnotationDeclaration?, arg: Void?) {
            val name = n!!.name.asString()

            writer.writeStartElement("annotation")
            writer.writeAttribute("modifier", getModifier(n.modifiers))
            writer.writeAttribute("name", name)
            writer.writeAttribute("fullname", packageStack.getFullName(name))

            // TODO

            super.visit(n, arg)
            writer.writeEndElement()
        }

        /**
         * アノテーションメンバ定義.
         */
        override fun visit(n: AnnotationMemberDeclaration?, arg: Void?) {
            // TODO
            super.visit(n, arg)
        }

        /**
         * アノテーション指定(パラメータなし).
         *
         * `@annotation`
         */
        override fun visit(n: MarkerAnnotationExpr?, arg: Void?) {
            // TODO
            super.visit(n, arg)
        }

        /**
         * アノテーション指定(デフォルトパラメータ).
         *
         * `@annotation(value)`
         */
        override fun visit(n: SingleMemberAnnotationExpr?, arg: Void?) {
            // TODO
            super.visit(n, arg)
        }

        /**
         * アノテーション指定(名前指定パラメータ).
         *
         * `@annotation(name = value)`
         */
        override fun visit(n: NormalAnnotationExpr?, arg: Void?) {
            // TODO
            super.visit(n, arg)
        }

        /**
         * クラス定義.
         *
         * `class class_name`
         */
        override fun visit(n: ClassOrInterfaceDeclaration?, arg: Void?) {
            val name = n!!.name.asString()

            // クラスの出力
            writer.writeStartElement("class")
            writer.writeAttribute("modifier", getModifier(n.modifiers))
            writer.writeAttribute("name", name)
            writer.writeAttribute("fullname", packageStack.getFullName(name))

            packageStack.push(name)

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // アノテーション
            n.annotations.forEach { it.accept(this, arg) }
            // 型パラメータ
            n.typeParameters.forEach { it.accept(this, arg) }
            // 継承クラス
            n.extendedTypes.forEach{
                writer.writeEmptyElement("extends")
                writer.writeAttribute("name", it.name.asString())
            }
            // インターフェース
            n.implementedTypes.forEach {
                writer.writeEmptyElement("implements")
                writer.writeAttribute("name", it.name.asString())
            }
            // メンバー
            n.members.forEach { it.accept(this, arg) }

            packageStack.pop()
            writer.writeEndElement()
        }

        /**
         * レコード定義.
         *
         * `record record_name`
         * (Java 14からの機能)
         */
        override fun visit(n: RecordDeclaration?, arg: Void?) {
            val name = n!!.name.asString()

            writer.writeStartElement("record")
            writer.writeAttribute("modifier", getModifier(n.modifiers))
            writer.writeAttribute("name", name)
            writer.writeAttribute("fullname", packageStack.getFullName(name))

            // TODO

            packageStack.push(name)
            super.visit(n, arg)
            packageStack.pop()
            writer.writeEndElement()
        }

        /**
         * Static イニシャライザ定義.
         *
         * `static { ... }`
         */
        override fun visit(n: InitializerDeclaration?, arg: Void?) {
            // TODO
            super.visit(n, arg)
        }

        /**
         * 短縮コンストラクタ定義.
         *
         * `class_name {}`
         * (Java 14からの機能)
         */
        override fun visit(n: CompactConstructorDeclaration?, arg: Void?) {
            // TODO
            super.visit(n, arg)
        }

        /**
         * メンバ変数定義.
         */
        override fun visit(n: FieldDeclaration?, arg: Void?) {
            val modifier = getModifier(n!!.modifiers)

            n.variables.forEach{
                writer.writeStartElement("field")
                writer.writeAttribute("modifier", modifier)
                writer.writeAttribute("type", it.type.asString())
                writer.writeAttribute("name", it.name.asString())
                it.initializer.ifPresent{ value -> writer.writeAttribute("value", value.toString()) }

                // コメントを処理する
                n.comment.ifPresent{ c -> c.accept(this, arg) }
                // アノテーションを処理する
                n.annotations.forEach{ a -> a.accept(this, arg) }

                writer.writeEndElement()
            }
        }

        /**
         * コンストラクタ定義.
         */
        override fun visit(n: ConstructorDeclaration?, arg: Void?) {
            // TODO
            super.visit(n, arg)
        }

        /**
         * 関数定義.
         */
        override fun visit(n: MethodDeclaration?, arg: Void?) {
            n!!

            writer.writeStartElement("method")
            writer.writeAttribute("modifier", getModifier(n.modifiers))
            writer.writeAttribute("return", n.type.asString())
            writer.writeAttribute("name", n.name.asString())

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // アノテーション
            n.annotations.forEach { it.accept(this, arg) }

            // パラメータ
            // 明示的なthisパラメータ
            n.receiverParameter.ifPresent { it.accept(this, arg) }
            // パラメータ
            n.parameters.forEach { it.accept(this, arg) }

            // throws
            n.thrownExceptions.forEach { it.accept(this, arg) }

            // ステートメント
            n.body.ifPresent { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * 明示的なthisパラメータ.
         */
        override fun visit(n: ReceiverParameter?, arg: Void?) {
            n!!

            writer.writeStartElement("parameter")
            writer.writeAttribute("type", n.type.asString())
            writer.writeAttribute("name", n.name.asString())

            // アノテーション
            n.annotations.forEach { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * パラメータ.
         */
        override fun visit(n: Parameter?, arg: Void?) {
            n!!

            writer.writeStartElement("parameter")
            writer.writeAttribute("modifier", getModifier(n.modifiers))
            writer.writeAttribute("type", n.type.asString())
            writer.writeAttribute("name", n.name.asString())

            // アノテーション
            n.varArgsAnnotations.forEach { it.accept(this, arg) }
            n.annotations.forEach { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * ブロックステートメント.
         */
        override fun visit(n: BlockStmt?, arg: Void?) {
            n!!

            writer.writeStartElement("block")

            n.statements.forEach { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * Ifステートメント.
         */
        override fun visit(n: IfStmt?, arg: Void?) {
            n!!

            // 条件分岐の出力
            writer.writeStartElement("condition")
            writer.writeAttribute("type", "if")

            // 第1条件
            writer.writeStartElement("case")
            writer.writeStartElement("expr")
            writer.writeStrings(n.condition.toString())
            writer.writeEndElement()

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // 本文
            n.thenStmt.accept(this, arg)

            writer.writeEndElement()

            // 継続条件
            visitInElse(n.elseStmt, arg)

            writer.writeEndElement()
        }

        /**
         * Elseステートメント.
         */
        private fun visitInElse(n: Optional<Statement>, arg: Void?) {

            n.ifPresent { e ->
                if (e is IfStmt) {
                    // 継続条件(else if)
                    writer.writeStartElement("case")
                    writer.writeStartElement("expr")
                    writer.writeStrings(e.condition.toString())
                    writer.writeEndElement()

                    // コメント
                    e.comment.ifPresent { it.accept(this, arg) }
                    // 本文
                    e.thenStmt.accept(this, arg)

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
                    e.accept(this, arg)

                    writer.writeEndElement()
                }
            }
        }

        /**
         * switchステートメント.
         */
        override fun visit(n: SwitchStmt?, arg: Void?) {
            n!!

            // 条件分岐の出力
            writer.writeStartElement("condition")
            writer.writeAttribute("type", "switch")
            writer.writeAttribute("selector", n.selector.toString())

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // 条件エントリ
            n.entries.forEach { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * caseステートメント.
         */
        override fun visit(n: SwitchEntry?, arg: Void?) {
            n!!

            // 継続条件(else if)
            writer.writeStartElement("case")
            // 条件
            n.labels.forEach {
                writer.writeStartElement("expr")
                writer.writeStrings(it.toString())
                writer.writeEndElement()
            }

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // 本文
            n.statements.forEach { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * for eachステートメント.
         */
        override fun visit(n: ForEachStmt?, arg: Void?) {

            writer.writeStartElement("for-each")

            // イテレータ
            // TODO: なんか違う気がする
            writer.writeStartElement("iterator")
            writer.writeAttribute("expression", n!!.iterable.toString())

            // 変数
            writer.writeStartElement("variable")
            writer.writeAttribute("expression", n.variable.toString())

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }

            // 処理本体
            n.body.accept(this, arg)

            writer.writeEndElement()
            super.visit(n, arg)
        }

    }

}
