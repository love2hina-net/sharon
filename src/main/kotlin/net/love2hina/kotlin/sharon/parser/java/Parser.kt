package net.love2hina.kotlin.sharon.parser.java

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
import net.love2hina.kotlin.sharon.*
import net.love2hina.kotlin.sharon.data.*
import net.love2hina.kotlin.sharon.entity.FileMap
import net.love2hina.kotlin.sharon.parser.ParserInterface

import java.io.File
import java.lang.Integer.compare
import java.nio.charset.StandardCharsets.UTF_8
import java.util.stream.Stream

internal class Parser(
    val fileSrc: File,
    val mapper: FileMapper?,
    val entity: FileMap?
) {

    companion object Default: ParserInterface {
        override fun parse(mapper: FileMapper, entities: Stream<FileMap>) {
            entities.parallel().forEach {
                Parser(File(it.srcFile), mapper, it).parse(File(it.xmlFile))
            }
        }

        fun parse(file: File, xml: File) {
            Parser(file, null, null).parse(xml)
        }
    }

    fun parse(fileXml: File) {
        val unit = StaticJavaParser.parse(fileSrc)

        // XML
        val xmlWriter = SmartXMLStreamWriter(fileXml)

        xmlWriter.use {
            xmlWriter.writeStartDocument(UTF_8.name(), "1.0")

            unit.accept(Visitor(xmlWriter), null)

            xmlWriter.writeEndDocument()
            xmlWriter.flush()
            xmlWriter.close()
        }
    }

    internal inner class Visitor(
        internal val writer: SmartXMLStreamWriter
    ): VoidVisitor<Void> {

        internal val packageStack = PackageStack()

        /**
         * コンパイル単位.
         *
         * つまりファイル
         */
        override fun visit(n: CompilationUnit?, arg: Void?) {
            n!!

            writer.writeStartElement("file")
            writer.writeAttribute("language", "java")
            writer.writeAttribute("src", this@Parser.fileSrc.canonicalPath)

            // モジュール
            n.module.ifPresent { it.accept(this, arg) }
            // パッケージ宣言
            n.packageDeclaration.ifPresent {
                // パッケージを設定
                this@Parser.entity?.let { entity -> mapper?.applyPackage(entity, it.name.asString()) }

                it.accept(this, arg)
            }
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

            if (n.content.startsWith('/')) {
                n.content.split(REGEXP_NEWLINE)
                    .stream().map { REGEXP_BLOCK_COMMENT.find(it) }
                    .forEach { this.visitLineComment(it) }
            }
        }

        override fun visit(n: LineComment?, arg: Void?) {
            n!!

            this.visitLineComment(REGEXP_LINE_COMMENT.find(n.content))
        }

        private fun visitLineComment(matchContent: MatchResult?) {

            if (matchContent != null) {
                val content = (matchContent.groups as MatchNamedGroupCollection)["content"]?.value ?: ""
                val matchAssignment = REGEXP_ASSIGNMENT.find(content)

                if (matchAssignment != null) {
                    // 代入式
                    val groupsAssign = (matchAssignment.groups as MatchNamedGroupCollection)
                    writer.writeEmptyElement("assignment")
                    writer.writeAttribute("var", groupsAssign["var"]!!.value)
                    writer.writeAttribute("value", groupsAssign["value"]!!.value)
                }
                else if (content.isNotBlank()) {
                    // 処理記述
                    writer.writeStartElement("description")
                    writer.writeStrings(content)
                    writer.writeEndElement()
                }
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

        override fun visit(n: EnumDeclaration?, arg: Void?) = visitInEnum(n!!, arg)
        override fun visit(n: EnumConstantDeclaration?, arg: Void?) = visitInEnumConstant(n!!, arg)

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

        override fun visit(n: ClassOrInterfaceDeclaration?, arg: Void?) = visitInClass(n!!, arg)

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

        override fun visit(n: ConstructorDeclaration?, arg: Void?) = visitInConstructor(n!!, arg)
        override fun visit(n: MethodDeclaration?, arg: Void?) = visitInFunction(n!!, arg)
        override fun visit(n: ReceiverParameter?, arg: Void?) = error("使われない")
        override fun visit(n: Parameter?, arg: Void?) = error("使われない")

        /**
         * ブロックステートメント.
         */
        override fun visit(n: BlockStmt?, arg: Void?) {
            n!!

            val positionUnknown = Position(Int.MAX_VALUE, 0)
            val rangeUnknown = Range(positionUnknown, positionUnknown)

            // ソースコードの登場順でソートして出力
            n.childNodes.stream()
                .sorted { x, y -> compare(
                    x.range.orElse(rangeUnknown).begin.line,
                    y.range.orElse(rangeUnknown).begin.line)
                }.forEach { it.accept(this, arg) }
        }

        override fun visit(n: ExpressionStmt?, arg: Void?) {
            n!!

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // TODO: 式
            // n.expression.accept(this, arg)
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

        override fun visit(n: NullLiteralExpr?, arg: Void?) = error("使われない")
        override fun visit(n: BooleanLiteralExpr?, arg: Void?) = error("使われない")
        override fun visit(n: CharLiteralExpr?, arg: Void?) = error("使われない")
        override fun visit(n: IntegerLiteralExpr?, arg: Void?) = error("使われない")
        override fun visit(n: LongLiteralExpr?, arg: Void?) = error("使われない")
        override fun visit(n: DoubleLiteralExpr?, arg: Void?) = error("使われない")
        override fun visit(n: StringLiteralExpr?, arg: Void?) = error("使われない")
        override fun visit(n: TextBlockLiteralExpr?, arg: Void?) = error("使われない")

        override fun visit(n: IfStmt?, arg: Void?) = visitInIf(n!!, arg)
        override fun visit(n: SwitchStmt?, arg: Void?) = visitInSwitch(n!!, arg)
        override fun visit(n: SwitchEntry?, arg: Void?) = error("使われない")

        override fun visit(n: ForStmt?, arg: Void?) = visitInFor(n!!, arg)
        override fun visit(n: ForEachStmt?, arg: Void?) = visitInForEach(n!!, arg)
        override fun visit(n: WhileStmt?, arg: Void?) = visitInWhile(n!!, arg)
        override fun visit(n: DoStmt?, arg: Void?) = visitInDo(n!!, arg)

        /**
         * breakステートメント.
         */
        override fun visit(n: BreakStmt?, arg: Void?) {
            n!!

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }

            writer.writeStartElement("break")
            // ラベル
            n.label.ifPresent {
                writer.writeAttribute("label", it.asString())
            }
            writer.writeEndElement()
        }

        /**
         * continueステートメント.
         */
        override fun visit(n: ContinueStmt?, arg: Void?) {
            n!!

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }

            writer.writeStartElement("continue")
            // ラベル
            n.label.ifPresent {
                writer.writeAttribute("label", it.asString())
            }
            writer.writeEndElement()
        }

        /**
         * throwステートメント.
         */
        override fun visit(n: ThrowStmt?, arg: Void?) {
            n!!

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // TODO: 式
            // n.expression.accept(this, arg)
        }

        /**
         * returnステートメント.
         */
        override fun visit(n: ReturnStmt?, arg: Void?) {
            n!!

            // コメント
            n.comment.ifPresent { it.accept(this, arg) }
            // TODO: 式
            // n.expression.ifPresent { it.accept(this, arg) }
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
