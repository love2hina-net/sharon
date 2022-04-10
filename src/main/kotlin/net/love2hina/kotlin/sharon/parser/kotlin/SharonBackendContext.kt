package net.love2hina.kotlin.sharon.parser.kotlin

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.name.FqName

class SharonBackendContext(
    val environment: KotlinCoreEnvironment,
    override val configuration: CompilerConfiguration
): CommonBackendContext {

    lateinit var moduleDescriptor: ModuleDescriptor
    val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)!!

    override val builtIns: SharonBuiltIns by lazy {
        moduleDescriptor.builtIns as SharonBuiltIns
    }

    override val irBuiltIns: IrBuiltIns
        get() = TODO("not implemented")

    override val sharedVariablesManager: SharedVariablesManager
        get() = TODO("not implemented")

    override val typeSystem: IrTypeSystemContext
        get() = TODO("Not yet implemented")

    override val irFactory: IrFactory
        get() = TODO("Not yet implemented")

    override val mapping: Mapping
        get() = TODO("Not yet implemented")

    override val scriptMode: Boolean
        get() = TODO("Not yet implemented")

    override var inVerbosePhase: Boolean = false

    override val internalPackageFqn: FqName
        get() = TODO("not implemented")

    override val ir: Ir<CommonBackendContext>
        get() = TODO("not implemented")

    override fun log(message: () -> String) {
        if (inVerbosePhase) {
            println(message())
        }
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        this.messageCollector.report(
            if (isError) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING,
            message, null
        )
    }

    val messageCollector: MessageCollector
        get() = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

}
