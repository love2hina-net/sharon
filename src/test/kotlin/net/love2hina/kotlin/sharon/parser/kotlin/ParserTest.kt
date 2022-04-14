package net.love2hina.kotlin.sharon.parser.kotlin

import net.love2hina.kotlin.sharon.TestUtils
import org.junit.jupiter.api.Test

class ParserTest {

    @Test
    fun testParse() {
        val pathSrcFile = TestUtils.pathProjectRoot.resolve("src/test/kotlin/net/love2hina/kotlin/sharon/kotlin/ParseTestClassTarget.kt")
        Parser.parse(pathSrcFile.toFile())
    }

}
