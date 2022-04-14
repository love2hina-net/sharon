package net.love2hina.kotlin.sharon.parser.kotlin

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import net.love2hina.kotlin.sharon.FileMapper
import net.love2hina.kotlin.sharon.entity.FileMap
import net.love2hina.kotlin.sharon.parser.ParserInterface
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.PreprocessedFileCreator
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

internal object Parser: ParserInterface {

    override fun parse(mapper: FileMapper, entities: Stream<FileMap>) {
        parse(entities.map { it.srcFile })
    }

    fun parse(file: File) {
        parse(Stream.of(file.absolutePath))
    }

    private fun parse(files: Stream<String>) {
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

        configuration.put(CLIConfigurationKeys.PHASE_CONFIG, createPhaseConfig(toplevelPhase, arguments, messageCollector))

        // モジュール名は固定
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "main")

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES)

        // ファイル
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val virtualFileCreator = PreprocessedFileCreator(environment.project)
        val psiManager = PsiManager.getInstance(environment.project)
        val ktFiles: List<KtFile> = files.map { file ->
                val virtualFile = localFileSystem.findFileByPath(file)?.let(virtualFileCreator::create)
                virtualFile?.let { psiManager.findFile(it) }
            }.filter { it is KtFile }
            .map { it as KtFile }
            .collect(Collectors.toList())
        configuration.put(SharonConfigurationKey.SOURCE_FILES, ktFiles)

        val context = SharonBackendContext(environment, configuration)

        toplevelPhase.invokeToplevel(context.phaseConfig, context, Unit)
    }

}
