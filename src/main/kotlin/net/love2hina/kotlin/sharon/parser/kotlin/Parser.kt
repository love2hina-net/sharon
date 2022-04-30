package net.love2hina.kotlin.sharon.parser.kotlin

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import net.love2hina.kotlin.sharon.FileMapper
import net.love2hina.kotlin.sharon.SmartXMLStreamWriter
import net.love2hina.kotlin.sharon.entity.FileMap
import net.love2hina.kotlin.sharon.parser.ParserInterface
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.PreprocessedFileCreator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtVisitorVoid
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.function.Predicate
import java.util.stream.Stream

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

        /**
         * コンパイル単位.
         */
        override fun visitKtFile(file: KtFile) {

            writer.writeStartElement("file")
            writer.writeAttribute("language", "kotlin")
            writer.writeAttribute("src", file.virtualFile.path)

            // モジュール

            // パッケージ宣言
            file.packageDirective?.let {

            }
            // インポート
            file.importDirectives.forEach { it.accept(this) }
            // 型宣言
            file.acceptChildren(this)

            writer.writeEndElement()
        }

    }

}
