package net.love2hina.kotlin.sharon.parser.kotlin

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import net.love2hina.kotlin.sharon.FileMapper
import net.love2hina.kotlin.sharon.SmartXMLStreamWriter
import net.love2hina.kotlin.sharon.data.PackageStack
import net.love2hina.kotlin.sharon.data.REGEXP_ASSIGNMENT
import net.love2hina.kotlin.sharon.entity.FileMap
import net.love2hina.kotlin.sharon.parser.ParserInterface
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.PreprocessedFileCreator
import org.jetbrains.kotlin.psi.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.function.Predicate
import java.util.stream.Stream

internal val REGEXP_BLOCK_COMMENT = Regex("^\\s*[/*]*\\s*(?<content>\\S|\\S.*\\S)?\\s*$")
internal val REGEXP_LINE_COMMENT = Regex("^///\\s*(?<content>\\S|\\S.*\\S)\\s*$")

internal class Parser(
    val fileSrc: KtFile,
    val mapper: FileMapper?,
    val entity: FileMap?
) {

    internal data class ParseFile(
        val fileSrc: File,
        val fileXml: File,
        val entity: FileMap?
    )

    internal data class ParseKtFile(
        val fileSrc: KtFile,
        val fileXml: File,
        val entity: FileMap?
    )

    companion object Default: ParserInterface {
        override fun parse(mapper: FileMapper, entities: Stream<FileMap>) {
            parse(entities.map { ParseFile(File(it.srcFile), File(it.xmlFile), it) }, mapper)
        }

        fun parse(file: File, xml: File) {
            parse(Stream.of(ParseFile(file, xml, null)), null)
        }

        private fun parse(files: Stream<ParseFile>, mapper: FileMapper?) {
            val arguments = SharonCompilerArguments()
            var messageCollector: MessageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, arguments.verbose)
            val rootDisposable: Disposable = Disposer.newDisposable()
            val configuration = CompilerConfiguration()

            // メッセージコレクター
            val fixedMessageCollector = if (arguments.suppressWarnings && !arguments.allWarningsAsErrors) {
                FilteringMessageCollector(messageCollector, Predicate.isEqual(CompilerMessageSeverity.WARNING))
            } else {
                messageCollector
            }
            configuration.put(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY, fixedMessageCollector)
            GroupingMessageCollector(fixedMessageCollector, arguments.allWarningsAsErrors).also {
                messageCollector = it
                configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            }

            // プロジェクトの作成
            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES)

            // ファイル
            val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
            val virtualFileCreator = PreprocessedFileCreator(environment.project)
            val psiManager = PsiManager.getInstance(environment.project)

            val ktFiles: Stream<ParseKtFile> = files.map { file ->
                val virtualFile = localFileSystem.findFileByPath(file.fileSrc.absolutePath)?.let(virtualFileCreator::create)
                ParseKtFile(psiManager.findFile(virtualFile!!) as KtFile, file.fileXml, file.entity)
            }

            ktFiles.forEach { Parser(it.fileSrc, mapper, it.entity).parse(it.fileXml) }
        }

    }

    fun parse(fileXml: File) {
        // XML
        val xmlWriter = SmartXMLStreamWriter(fileXml)

        xmlWriter.use {
            it.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0")

            fileSrc.accept(Visitor(it))

            it.writeEndDocument()
            it.flush()
        }
    }

    internal inner class Visitor(
        internal val writer: SmartXMLStreamWriter
    ): KtVisitorVoid() {

        internal val packageStack = PackageStack()

        /**
         * コンパイル単位.
         */
        override fun visitKtFile(file: KtFile) {

            writer.writeStartElement("file")
            writer.writeAttribute("language", "kotlin")
            writer.writeAttribute("src", file.virtualFile.path)

            // パッケージ宣言
            file.packageDirective?.let {
                // パッケージを設定
                this@Parser.entity?.let { entity -> mapper?.applyPackage(entity, it.fqName.asString()) }
            }
            // インポート／型宣言
            file.acceptChildren(this)

            writer.writeEndElement()
        }

        /**
         * パッケージ定義.
         *
         * `package package_name`
         */
        override fun visitPackageDirective(directive: KtPackageDirective) {
            val name = directive.fqName.asString()

            packageStack.push(name)

            writer.writeEmptyElement("package")
            writer.writeAttribute("package", name)
        }

        override fun visitImportList(importList: KtImportList) {
            importList.acceptChildren(this)
        }

        /**
         * インポート.
         *
         * `import package_name`
         */
        override fun visitImportDirective(importDirective: KtImportDirective) {
            importDirective.importedFqName?.let { name ->
                writer.writeEmptyElement("import")
                writer.writeAttribute("package", name.asString())
            }
        }

        override fun visitClass(klass: KtClass) = visitInClass(klass)
        override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) = visitInPrimaryCtor(constructor)

        override fun visitBlockExpression(expression: KtBlockExpression) {
            expression.acceptChildren(this)
        }

        override fun visitComment(comment: PsiComment) = visitLineComment(REGEXP_LINE_COMMENT.find(comment.text))

        private fun visitLineComment(matchContent: MatchResult?) {

            if (matchContent != null) {
                val content = (matchContent.groups as MatchNamedGroupCollection)["content"]?.value ?: ""
                val matchAssignment = REGEXP_ASSIGNMENT.find(content)

                if (matchAssignment != null) {
                    // 代入式
                    val groupAssign = (matchAssignment.groups as MatchNamedGroupCollection)
                    writer.writeEmptyElement("assignment")
                    writer.writeAttribute("var", groupAssign["var"]!!.value)
                    writer.writeAttribute("value", groupAssign["value"]!!.value)
                }
                else if (content.isNotBlank()) {
                    // 処理記述
                    writer.writeStartElement("description")
                    writer.writeStrings(content)
                    writer.writeEndElement()
                }
            }
        }

    }

}
