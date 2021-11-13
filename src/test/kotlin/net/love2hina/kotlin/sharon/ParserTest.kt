package net.love2hina.kotlin.sharon

import net.love2hina.kotlin.sharon.TestUtils.pathProjectRoot
import org.junit.jupiter.api.Test

class ParserTest {

    @Test
    fun testParse() {
        val pathSrcFile = pathProjectRoot.resolve("src/test/java/net/love2hina/kotlin/sharon/ParseTestTarget.java")
        val pathXmlFile = pathProjectRoot.resolve("test.xml")

        val parser = Parser(pathSrcFile.toFile())
        parser.parse(pathXmlFile.toFile())
    }

}
