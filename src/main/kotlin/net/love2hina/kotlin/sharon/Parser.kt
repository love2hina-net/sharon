package net.love2hina.kotlin.sharon

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.modules.ModuleDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import javax.xml.stream.XMLStreamWriter

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
        val writer: XMLStreamWriter
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
            writer.writeCharacters(n!!.content)

            super.visit(n, arg)
            writer.writeEndElement()
        }

        override fun visit(n: BlockComment?, arg: Void?) {
            writer.writeStartElement("comment")
            writer.writeCharacters(n!!.content)

            super.visit(n, arg)
            writer.writeEndElement()
        }

        override fun visit(n: LineComment?, arg: Void?) {
            writer.writeStartElement("comment")
            writer.writeCharacters(n!!.content)

            super.visit(n, arg)
            writer.writeEndElement()
        }

        /**
         * インポート.
         *
         * `import package_name;`
         */
        override fun visit(n: ImportDeclaration?, arg: Void?) {
            writer.writeStartElement("import")
            writer.writeAttribute("package", n!!.name.asString())

            super.visit(n, arg)
            writer.writeEndElement()
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

            writer.writeStartElement("package")
            writer.writeAttribute("package", name)

            super.visit(n, arg)
            writer.writeEndElement()
        }

        /**
         * Enum定義.
         *
         * `enum enum_name`
         */
        override fun visit(n: EnumDeclaration?, arg: Void?) {
            val name = n!!.name.asString()

            writer.writeStartElement("enum")
            writer.writeAttribute("name", name)
            writer.writeAttribute("fullname", packageStack.getFullName(name))

            super.visit(n, arg)
            writer.writeEndElement()
        }

        /**
         * アノテーション定義.
         *
         * `@interface anon_name`
         */
        override fun visit(n: AnnotationDeclaration?, arg: Void?) {
            val name = n!!.name.asString()

            writer.writeStartElement("annotation")
            writer.writeAttribute("name", name)
            writer.writeAttribute("fullname", packageStack.getFullName(name))

            super.visit(n, arg)
            writer.writeEndElement()
        }

        /**
         * クラス定義.
         *
         * `class class_name`
         */
        override fun visit(n: ClassOrInterfaceDeclaration?, arg: Void?) {
            val name = n!!.name.asString()

            writer.writeStartElement("class")
            writer.writeAttribute("name", name)
            writer.writeAttribute("fullname", packageStack.getFullName(name))

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
            writer.writeAttribute("name", name)
            writer.writeAttribute("fullname", packageStack.getFullName(name))

            packageStack.push(name)
            super.visit(n, arg)
            packageStack.pop()
            writer.writeEndElement()
        }

        override fun visit(n: FieldDeclaration?, arg: Void?) {
            super.visit(n, arg)
        }

    }

}
