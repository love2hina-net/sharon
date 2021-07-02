package net.love2hina.kotlin.sharon

import com.github.javaparser.utils.CodeGenerationUtils
import org.junit.jupiter.api.Test

class ParserTest {

    @Test
    fun testParse() {
        val pathProjectRoot = CodeGenerationUtils.mavenModuleRoot(ParserTest::class.java).resolve("../..")
        val pathSrcFile = pathProjectRoot.resolve("src/test/java/net/love2hina/kotlin/sharon/ParseTestTarget.java")
        val pathXmlFile = pathProjectRoot.resolve("test.xml")

        val parser = Parser(pathSrcFile.toFile())
        parser.parse(pathXmlFile.toFile())
    }

}
