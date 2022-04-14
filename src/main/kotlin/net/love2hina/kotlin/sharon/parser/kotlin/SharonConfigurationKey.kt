package net.love2hina.kotlin.sharon.parser.kotlin

import org.jetbrains.kotlin.config.CompilerConfigurationKey;
import org.jetbrains.kotlin.psi.KtFile;

internal object SharonConfigurationKey {

    val SOURCE_FILES: CompilerConfigurationKey<List<KtFile>> =
            CompilerConfigurationKey.create("analyse source files")

}
