package net.love2hina.kotlin.sharon

import com.github.javaparser.Position
import com.github.javaparser.Range
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.modules.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.VoidVisitor
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File
import java.lang.Integer.compare
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
    ): VoidVisitor<Void> {

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

            val regex = Regex("^\\s*(?:[/*])\\s*(?<content>\\S|\\S.*\\S)?\\s*$")

            if (n.content.startsWith('/')) {
                val lines = n.content.split(Regex("\r\n|\r|\n"))
                    .stream().map {
                        val m = regex.find(it)
                        (m?.groups as MatchNamedGroupCollection?)?.get("content")?.value ?: ""
                    }.reduce(
                        StringBuilder(),
                        { b, s -> (if (b.isEmpty()) b else b.append("\r\n")).append(s) },
                        { b1, b2 -> (if (b1.isEmpty()) b1 else b1.append("\r\n")).append(b2) }
                    )

                writer.writeStartElement("comment")
                writer.writeStrings(lines.toString())
                writer.writeEndElement()
            }
        }

        override fun visit(n: LineComment?, arg: Void?) {
            n!!

            val regex = Regex("^/\\s*(?<content>\\S|\\S.*\\S)?\\s*$")
            val match = regex.find(n.content)

            if (match != null) {
                writer.writeStartElement("comment")
                writer.writeStrings((match.groups as MatchNamedGroupCollection)["content"]!!.value)
                writer.writeEndElement()
            }
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

            // n.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * モジュール定義.
         *
         * `modules module_name`
         */
        override fun visit(n: ModuleDeclaration?, arg: Void?) {
            n!!

            // 特に処理しない
//            n.annotations.forEach { it.accept(this, arg) }
//            n.directives.forEach { it.accept(this, arg) }
//            n.name.accept(this, arg)
//            n.comment.ifPresent { it.accept(this, arg) }
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

            // n.annotations.forEach { it.accept(this, arg) }
            // n.comment.ifPresent { it.accept(this, arg) }
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
            // n.entries.forEach { it.accept(this, arg) }
            // n.getImplementedTypes().forEach(p -> p.accept(this, arg));
            // n.getMembers().forEach(p -> p.accept(this, arg));
            // n.getAnnotations().forEach(p -> p.accept(this, arg));
            // n.getComment().ifPresent(l -> l.accept(this, arg));

            writer.writeEndElement()
        }

        /**
         * Enum値定義.
         */
        override fun visit(n: EnumConstantDeclaration?, arg: Void?) {
            // TODO
            // n.getArguments().forEach(p -> p.accept(this, arg));
            //        n.getClassBody().forEach(p -> p.accept(this, arg));
            //        n.getName().accept(this, arg);
            //        n.getAnnotations().forEach(p -> p.accept(this, arg));
            //        n.getComment().ifPresent(l -> l.accept(this, arg));
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
            // n.getMembers().forEach(p -> p.accept(this, arg));
            //        n.getAnnotations().forEach(p -> p.accept(this, arg));
            //        n.getComment().ifPresent(l -> l.accept(this, arg));

            writer.writeEndElement()
        }

        /**
         * アノテーションメンバ定義.
         */
        override fun visit(n: AnnotationMemberDeclaration?, arg: Void?) {
            // TODO
            // n.getDefaultValue().ifPresent(l -> l.accept(this, arg));
            //        n.getModifiers().forEach(p -> p.accept(this, arg));
            //        n.getName().accept(this, arg);
            //        n.getType().accept(this, arg);
            //        n.getAnnotations().forEach(p -> p.accept(this, arg));
            //        n.getComment().ifPresent(l -> l.accept(this, arg));
        }

        /**
         * アノテーション指定(パラメータなし).
         *
         * `@annotation`
         */
        override fun visit(n: MarkerAnnotationExpr?, arg: Void?) {
            // TODO
            // n.getName().accept(this, arg);
            //        n.getComment().ifPresent(l -> l.accept(this, arg));
        }

        /**
         * アノテーション指定(デフォルトパラメータ).
         *
         * `@annotation(value)`
         */
        override fun visit(n: SingleMemberAnnotationExpr?, arg: Void?) {
            // TODO
            // n.getMemberValue().accept(this, arg);
            //        n.getName().accept(this, arg);
            //        n.getComment().ifPresent(l -> l.accept(this, arg));
        }

        /**
         * アノテーション指定(名前指定パラメータ).
         *
         * `@annotation(name = value)`
         */
        override fun visit(n: NormalAnnotationExpr?, arg: Void?) {
            // TODO
            // n.getPairs().forEach(p -> p.accept(this, arg));
            //        n.getName().accept(this, arg);
            //        n.getComment().ifPresent(l -> l.accept(this, arg));
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
            n.extendedTypes.forEach {
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

            packageStack.push(name)

            // TODO
            // n.getImplementedTypes().forEach(p -> p.accept(this, arg));
            //        n.getParameters().forEach(p -> p.accept(this, arg));
            //        n.getReceiverParameter().ifPresent(l -> l.accept(this, arg));
            //        n.getTypeParameters().forEach(p -> p.accept(this, arg));
            //        n.getMembers().forEach(p -> p.accept(this, arg));
            //        n.getAnnotations().forEach(p -> p.accept(this, arg));
            //        n.getComment().ifPresent(l -> l.accept(this, arg));

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
            // n.getBody().accept(this, arg);
            //        n.getAnnotations().forEach(p -> p.accept(this, arg));
            //        n.getComment().ifPresent(l -> l.accept(this, arg));
        }

        /**
         * 短縮コンストラクタ定義.
         *
         * `class_name {}`
         * (Java 14からの機能)
         */
        override fun visit(n: CompactConstructorDeclaration?, arg: Void?) {
            // TODO
            // n.getBody().accept(this, arg);
            //        n.getModifiers().forEach(p -> p.accept(this, arg));
            //        n.getName().accept(this, arg);
            //        n.getThrownExceptions().forEach(p -> p.accept(this, arg));
            //        n.getTypeParameters().forEach(p -> p.accept(this, arg));
            //        n.getAnnotations().forEach(p -> p.accept(this, arg));
            //        n.getComment().ifPresent(l -> l.accept(this, arg));
        }

        /**
         * メンバ変数定義.
         */
        override fun visit(n: FieldDeclaration?, arg: Void?) {
            val modifier = getModifier(n!!.modifiers)

            n.variables.forEach {
                writer.writeStartElement("field")
                writer.writeAttribute("modifier", modifier)
                writer.writeAttribute("type", it.type.asString())
                writer.writeAttribute("name", it.name.asString())
                it.initializer.ifPresent{ v -> writer.writeAttribute("value", v.toString()) }

                // コメントを処理する
                n.comment.ifPresent{ c -> c.accept(this, arg) }
                // アノテーションを処理する
                n.annotations.forEach { a -> a.accept(this, arg) }

                writer.writeEndElement()
            }
        }

        /**
         * コンストラクタ定義.
         */
        override fun visit(n: ConstructorDeclaration?, arg: Void?) {
            // TODO
            //  n.getBody().accept(this, arg);
            //        n.getModifiers().forEach(p -> p.accept(this, arg));
            //        n.getName().accept(this, arg);
            //        n.getParameters().forEach(p -> p.accept(this, arg));
            //        n.getReceiverParameter().ifPresent(l -> l.accept(this, arg));
            //        n.getThrownExceptions().forEach(p -> p.accept(this, arg));
            //        n.getTypeParameters().forEach(p -> p.accept(this, arg));
            //        n.getAnnotations().forEach(p -> p.accept(this, arg));
            //        n.getComment().ifPresent(l -> l.accept(this, arg));
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

            // TODO
            // n.typeParameters.forEach { it.accept(this, arg) }

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

            val positionUnknown = Position(Int.MAX_VALUE, 0)
            val rangeUnknown = Range(positionUnknown, positionUnknown)

            // ソースコードの登場順でソートして出力
            n.childNodes.stream()
                .sorted { x, y -> compare(
                    x.range.orElse(rangeUnknown).begin.line,
                    y.range.orElse(rangeUnknown).begin.line)
                }.forEach { it.accept(this, arg) }

            writer.writeEndElement()
        }

        override fun visit(n: ExpressionStmt?, arg: Void?) {
            n!!

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // 式
            n.expression.accept(this, arg)
        }

        /**
         * 変数宣言.
         */
        override fun visit(n: VariableDeclarationExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * オブジェクト作成.
         */
        override fun visit(n: ObjectCreationExpr?, arg: Void?) {
            n!!

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // TODO 無名クラス定義
            n.anonymousClassBody.ifPresent { it.accept(this, arg) }
        }

        /**
         * メソッド呼び出し.
         */
        override fun visit(n: MethodCallExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * nullリテラル値.
         */
        override fun visit(n: NullLiteralExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * Booleanリテラル値.
         */
        override fun visit(n: BooleanLiteralExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * Charリテラル値.
         */
        override fun visit(n: CharLiteralExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * Integerリテラル値.
         */
        override fun visit(n: IntegerLiteralExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * Longリテラル値.
         */
        override fun visit(n: LongLiteralExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * Doubleリテラル値.
         */
        override fun visit(n: DoubleLiteralExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * Stringリテラル値.
         */
        override fun visit(n: StringLiteralExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
        }

        /**
         * TextBlockリテラル値.
         */
        override fun visit(n: TextBlockLiteralExpr?, arg: Void?) {
            // コメント
            n!!.comment.ifPresent { it.accept(this, arg) }
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

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // 条件
            writer.writeStartElement("expr")
            writer.writeStrings(n.condition.toString())
            writer.writeEndElement()
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

                    // コメント
                    e.comment.ifPresent { it.accept(this, arg) }
                    // 条件
                    writer.writeStartElement("expr")
                    writer.writeStrings(e.condition.toString())
                    writer.writeEndElement()
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

            var caseContinue = false

            // 条件エントリ
            for (i in n.entries) {
                if (!caseContinue)
                    writer.writeStartElement("case")

                // コメント
                i.comment.ifPresent { it.accept(this, arg) }
                // 条件
                i.labels.forEach {
                    writer.writeStartElement("expr")
                    writer.writeStrings(it.toString())
                    writer.writeEndElement()
                }
                // 本文
                i.statements.forEach { it.accept(this, arg) }

                caseContinue = i.statements.isEmpty()
                if (!caseContinue)
                    writer.writeEndElement()
            }

            writer.writeEndElement()
        }

        /**
         * caseステートメント.
         */
        override fun visit(n: SwitchEntry?, arg: Void?) {
            // 使われない
            throw IllegalAccessException()
        }

        /**
         * forステートメント.
         */
        override fun visit(n: ForStmt?, arg: Void?) {
            n!!

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
            n.body.accept(this, arg)

            writer.writeEndElement()
        }

        /**
         * for eachステートメント.
         */
        override fun visit(n: ForEachStmt?, arg: Void?) {
            n!!

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
            n.body.accept(this, arg)

            writer.writeEndElement()
        }

        /**
         * whileステートメント.
         */
        override fun visit(n: WhileStmt?, arg: Void?) {
            n!!

            writer.writeStartElement("loop")
            writer.writeAttribute("type", "while")

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // 条件
            writer.writeEmptyElement("condition")
            writer.writeAttribute("expr", n.condition.toString())
            // 本文
            n.body.accept(this, arg)

            writer.writeEndElement()
        }

        /**
         * do~whileステートメント.
         */
        override fun visit(n: DoStmt?, arg: Void?) {
            n!!

            writer.writeStartElement("loop")
            writer.writeAttribute("type", "do")

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // 条件
            writer.writeEmptyElement("condition")
            writer.writeAttribute("expr", n.condition.toString())
            // 本文
            n.body.accept(this, arg)

            writer.writeEndElement()
        }

        /**
         * breakステートメント.
         */
        override fun visit(n: BreakStmt?, arg: Void?) {
            n!!

            writer.writeStartElement("break")
            // ラベル
            n.label.ifPresent {
                writer.writeAttribute("label", it.asString())
            }

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * continueステートメント.
         */
        override fun visit(n: ContinueStmt?, arg: Void?) {
            n!!

            writer.writeStartElement("continue")
            // ラベル
            n.label.ifPresent {
                writer.writeAttribute("label", it.asString())
            }

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }

            writer.writeEndElement()
        }

        /**
         * throwステートメント.
         */
        override fun visit(n: ThrowStmt?, arg: Void?) {
            n!!

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // 式
            n.expression.accept(this, arg)
        }

        /**
         * returnステートメント.
         */
        override fun visit(n: ReturnStmt?, arg: Void?) {
            n!!

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // 式
            n.expression.ifPresent { it.accept(this, arg) }
        }

        override fun visit(n: NodeList<*>?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ArrayAccessExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ArrayCreationExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ArrayCreationLevel?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ArrayInitializerExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ArrayType?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: AssertStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: AssignExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: BinaryExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: CastExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: CatchClause?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ClassExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ClassOrInterfaceType?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ConditionalExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: EmptyStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: EnclosedExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ExplicitConstructorInvocationStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: FieldAccessExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: InstanceOfExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: IntersectionType?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: LabeledStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: LambdaExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: LocalClassDeclarationStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: LocalRecordDeclarationStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: MemberValuePair?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: MethodReferenceExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: NameExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: Name?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: PrimitiveType?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: SimpleName?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: SuperExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: SynchronizedStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ThisExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: TryStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: TypeExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: TypeParameter?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: UnaryExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: UnionType?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: UnknownType?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: VariableDeclarator?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: VoidType?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: WildcardType?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ModuleRequiresDirective?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ModuleExportsDirective?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ModuleProvidesDirective?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ModuleUsesDirective?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: ModuleOpensDirective?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: UnparsableStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: VarType?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: Modifier?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(switchExpr: SwitchExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(yieldStmt: YieldStmt?, arg: Void?) {
            TODO("Not yet implemented")
        }

        override fun visit(n: PatternExpr?, arg: Void?) {
            TODO("Not yet implemented")
        }

    }

}
