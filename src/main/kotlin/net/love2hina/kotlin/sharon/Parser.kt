package net.love2hina.kotlin.sharon

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.modules.ModuleDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import javax.xml.stream.XMLStreamWriter

class Parser(val file: File) {

    fun parse(xml: File) {
        val unit = StaticJavaParser.parse(file)

        // XML
        val xmlWriter = SmartXMLStreamWriter(xml)

        xmlWriter.use {
            xmlWriter.writeStartDocument(UTF_8.name(), "1.0")

            unit.accept(Visitor(), xmlWriter)

            xmlWriter.writeEndDocument()
            xmlWriter.flush()
            xmlWriter.close()
        }
        // https://qiita.com/opengl-8080/items/50ddee7d635c7baee0ab
    }

    private inner class Visitor: VoidVisitorAdapter<XMLStreamWriter>() {

        /**
         * コンパイル単位.
         *
         * つまりファイル
         */
        override fun visit(n: CompilationUnit?, arg: XMLStreamWriter?) {
            val writer = arg!!
            writer.writeStartElement("file")
            writer.writeAttribute("language", "java")
            writer.writeAttribute("src", this@Parser.file.canonicalPath)

            super.visit(n, arg)
            writer.writeEndElement()
        }

        /**
         * インポート.
         *
         * `import package_name;`
         */
        override fun visit(n: ImportDeclaration?, arg: XMLStreamWriter?) {
            val writer = arg!!
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
        override fun visit(n: ModuleDeclaration?, arg: XMLStreamWriter?) {
            // 特に処理しない
        }

        /**
         * パッケージ定義.
         *
         * `package package_name;`
         */
        override fun visit(n: PackageDeclaration?, arg: XMLStreamWriter?) {
            val writer = arg!!
            writer.writeStartElement("package")
            writer.writeAttribute("package", n!!.name.asString())

            super.visit(n, arg)
            writer.writeEndElement()
        }

        override fun visit(n: ClassOrInterfaceDeclaration?, arg: XMLStreamWriter?) {
            n?.comment?.ifPresent {
                println("Class Definition ->")
                println(it)
            }
            super.visit(n, arg)
        }

        override fun visit(n: FieldDeclaration?, arg: XMLStreamWriter?) {
            n?.comment?.ifPresent {
                println("Field Definition ->")
                println(it)
            }
            super.visit(n, arg)
        }

    }

}
