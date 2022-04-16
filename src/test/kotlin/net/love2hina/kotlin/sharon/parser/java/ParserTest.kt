package net.love2hina.kotlin.sharon.parser.java

import net.love2hina.kotlin.sharon.TestUtils.pathProjectRoot
import org.junit.jupiter.api.Test

class ParserTest {

    @Test
    fun testParse() {
        val pathSrcFile = pathProjectRoot.resolve("src/test/java/net/love2hina/kotlin/sharon/ParseTestTarget.java")
        val pathXmlFile = pathProjectRoot.resolve("test_java.xml")

        Parser.parse(pathSrcFile.toFile(), pathXmlFile.toFile())
    }

}
