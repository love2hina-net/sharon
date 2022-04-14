package net.love2hina.kotlin.sharon.parser.kotlin

import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings

internal fun createUnitPhase(
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<SharonBackendContext, *>> = emptySet(),
    op: SharonBackendContext.() -> Unit
) = namedOpUnitPhase(name, description, prerequisite, op)

internal val analyzePhase = createUnitPhase(
    op = {
        val sourceFiles = configuration.getNotNull(SharonConfigurationKey.SOURCE_FILES)

        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(
            messageCollector, environment.configuration.languageVersionSettings)

        analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
            val project = environment.project
            val moduleOutputs = environment.configuration.get(JVMConfigurationKeys.MODULES)?.mapNotNullTo(hashSetOf()) { module ->
                environment.projectEnvironment.environment.localFileSystem.findFileByPath(module.getOutputDirectory())
            }.orEmpty()
            val sourcesOnly = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, sourceFiles)
            val scope = if (moduleOutputs.isEmpty()) sourcesOnly else sourcesOnly.uniteWith(
                KotlinToJVMBytecodeCompiler.DirectoriesScope(
                    project,
                    moduleOutputs
                )
            )
            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project,
                sourceFiles,
                NoScopeRecordCliBindingTrace(),
                environment.configuration,
                environment::createPackagePartProvider,
                sourceModuleSearchScope = scope,
                klibList = emptyList()
            )
        }

        if (analyzerWithCompilerReport.hasErrors()) {
            throw RuntimeException("")
        }
    },
    name = "analyze",
    description = "Builds AST(PSI)"
)

val toplevelPhase: NamedCompilerPhase<SharonBackendContext, Unit> = namedUnitPhase(
    name = "Compiler",
    description = "The whole compilation process",
    prerequisite = emptySet(),
    nlevels = 1,
    lower = analyzePhase)
