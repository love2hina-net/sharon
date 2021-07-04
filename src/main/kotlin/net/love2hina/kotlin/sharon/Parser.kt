package net.love2hina.kotlin.sharon

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.MarkerAnnotationExpr
import com.github.javaparser.ast.expr.NormalAnnotationExpr
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr
import com.github.javaparser.ast.modules.ModuleDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.type.ReferenceType
import com.github.javaparser.ast.type.TypeParameter
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.util.function.Consumer

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
            writer.writeStartElement("file")
            writer.writeAttribute("language", "java")
            writer.writeAttribute("src", this@Parser.file.canonicalPath)

            super.visit(n, arg)
            writer.writeEndElement()
        }

        override fun visit(n: JavadocComment?, arg: Void?) {
            writer.writeStartElement("comment")
            writer.writeStrings(n!!.content)

            super.visit(n, arg)
            writer.writeEndElement()
        }

        override fun visit(n: BlockComment?, arg: Void?) {
            writer.writeStartElement("comment")
            writer.writeStrings(n!!.content)

            super.visit(n, arg)
            writer.writeEndElement()
        }

        override fun visit(n: LineComment?, arg: Void?) {
            writer.writeStartElement("comment")
            writer.writeStrings(n!!.content)

            super.visit(n, arg)
            writer.writeEndElement()
        }

        /**
         * インポート.
         *
         * `import package_name;`
         */
        override fun visit(n: ImportDeclaration?, arg: Void?) {
            writer.writeEmptyElement("import")
            writer.writeAttribute("package", n!!.name.asString())
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

            // 継承クラスの出力
            n.extendedTypes.forEach{
                writer.writeEmptyElement("extends")
                writer.writeAttribute("name", it.name.asString())
            }

            // インターフェースの出力
            n.implementedTypes.forEach {
                writer.writeEmptyElement("implements")
                writer.writeAttribute("name", it.name.asString())
            }

            packageStack.push(name)
            super.visit(n, arg)
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

                // アノテーションを処理する
                n.annotations.forEach{ a -> a.accept(this, arg) }
                // コメントを処理する
                n.comment.ifPresent{ c -> c.accept(this, arg) }

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

            writer.writeStartElement("method")
            writer.writeAttribute("modifier", getModifier(n!!.modifiers))
            writer.writeAttribute("return", n.type.asString())
            writer.writeAttribute("name", n.name.asString())

            // パラメータ
            // 明示的なthisパラメータ
            n.receiverParameter.ifPresent { it.accept(this, arg) }
            // パラメータ
            n.parameters.forEach { it.accept(this, arg) }

            // throws
            n.thrownExceptions.forEach { it.accept(this, arg) }
            // アノテーション
            n.annotations.forEach { it.accept(this, arg) }
            // コメント
            n.comment.ifPresent { it.accept(this, arg) }

            // ステートメント
            n.body.ifPresent { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * 明示的なthisパラメータ.
         */
        override fun visit(n: ReceiverParameter?, arg: Void?) {

            writer.writeStartElement("parameter")
            writer.writeAttribute("type", n!!.type.asString())
            writer.writeAttribute("name", n.name.asString())

            // アノテーション
            n.annotations.forEach { it.accept(this, arg) }
            // コメント
            n.comment.ifPresent { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * パラメータ.
         */
        override fun visit(n: Parameter?, arg: Void?) {

            writer.writeStartElement("parameter")
            writer.writeAttribute("modifier", getModifier(n!!.modifiers))
            writer.writeAttribute("type", n.type.asString())
            writer.writeAttribute("name", n.name.asString())

            // アノテーション
            n.varArgsAnnotations.forEach { it.accept(this, arg) }
            n.annotations.forEach { it.accept(this, arg) }
            // コメント
            n.comment.ifPresent { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * ブロックステートメント.
         */
        override fun visit(n: BlockStmt?, arg: Void?) {

            writer.writeStartElement("block")

            n!!.statements.forEach { it.accept(this, arg) }
            n.comment.ifPresent { it.accept(this, arg) }

            writer.writeEndElement()
        }

    }

}
