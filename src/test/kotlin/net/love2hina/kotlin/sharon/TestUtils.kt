package net.love2hina.kotlin.sharon

import com.github.javaparser.utils.CodeGenerationUtils

internal object TestUtils {

    val pathProjectRoot = CodeGenerationUtils.mavenModuleRoot(TestUtils::class.java).resolve("../..").normalize()

}
