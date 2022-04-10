package net.love2hina.kotlin.sharon.parser.kotlin

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.util.function.Predicate

fun test() {
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

    compileToEmpty(environment, configuration)
}
