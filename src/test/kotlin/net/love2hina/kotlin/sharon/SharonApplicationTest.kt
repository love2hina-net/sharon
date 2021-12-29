package net.love2hina.kotlin.sharon

import net.love2hina.kotlin.sharon.TestUtils.pathProjectRoot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.commons.util.ReflectionUtils
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SharonApplicationTest {

    @Test
    fun testInitialize() {
        val args = arrayOf(
            "--outdir",
            pathProjectRoot.resolve("test/out").toString(),
            pathProjectRoot.resolve("src/test/java").toString())

        SharonApplication(args).use { application ->
            // コール
            application.initialize()
        }
    }

    @Test
    fun testParseArgs001() {
        val args = arrayOf(
            "--outdir",
            pathProjectRoot.resolve("test/out").toString(),
            pathProjectRoot.resolve("src/test/java1").toString(),
            "--",
            pathProjectRoot.resolve("src/test/java2").toString())

        SharonApplication(args).use { application ->
            // コール
            val method = ReflectionUtils.findMethod(SharonApplication::class.java, "parseArgs")
            ReflectionUtils.invokeMethod(method.get(), application)

            // チェック
            assertEquals(pathProjectRoot.resolve("test/out"), application.pathOutputDir)
            assertEquals(2, application.listFilePath.size)
            val pathSrcFile = arrayOf(
                pathProjectRoot.resolve("src/test/java1").toString(),
                pathProjectRoot.resolve("src/test/java2").toString()
            )
            assertEquals(pathSrcFile[0], application.listFilePath[0])
            assertEquals(pathSrcFile[1], application.listFilePath[1])
        }
    }

    @Test
    fun testParseArgs002() {
        val args = arrayOf(
            "--outdir",
            "--")

        SharonApplication(args).use { application ->
            val e = assertThrows<IllegalArgumentException> {
                // コール
                val method = ReflectionUtils.findMethod(SharonApplication::class.java, "parseArgs")
                ReflectionUtils.invokeMethod(method.get(), application)
            }
            assertEquals("フラグの値が指定されていません。 --outdir", e.message)
        }
    }

    @Test
    fun testParseArgs003() {
        val args = arrayOf(
            "--dummy",
            "TEST")

        SharonApplication(args).use { application ->
            val e = assertThrows<IllegalArgumentException> {
                // コール
                val method = ReflectionUtils.findMethod(SharonApplication::class.java, "parseArgs")
                ReflectionUtils.invokeMethod(method.get(), application)
            }
            assertEquals("不明なフラグ値です。 --dummy", e.message)
        }
    }

    @Test
    fun testCheckArgs001() {
        val args = Array(0) { "" }
        val path = pathProjectRoot.resolve("test/out")

        SharonApplication(args).use { application ->
            // プロパティ設定
            application.setProperty("pathOutputDir", path)

            run {
                // コール
                val method = ReflectionUtils.findMethod(SharonApplication::class.java, "checkArgs")
                ReflectionUtils.invokeMethod(method.get(), application)
            }
        }
    }

    @Test
    fun testCheckArgs002() {
        val args = arrayOf(
            pathProjectRoot.resolve("out").toString())

        SharonApplication(args).use { application ->
            val e = assertThrows<IllegalArgumentException> {
                // コール
                val method = ReflectionUtils.findMethod(SharonApplication::class.java, "checkArgs")
                ReflectionUtils.invokeMethod(method.get(), application)
            }
            assertEquals("出力ディレクトリの指定がありません。 --outdir", e.message)
        }
    }

    @Test
    fun testListUpFiles001() {
        val args = Array(0) { "" }
        val path = pathProjectRoot.resolve("test/out")
        val pathSrcFile = pathProjectRoot.resolve("src/test/java/net/love2hina/kotlin/sharon/ParseTestTarget.java").toString()

        SharonApplication(args).use { application ->
            // プロパティ設定
            application.setProperty("pathOutputDir", path)
            application.listFilePath.add(pathSrcFile)
            application.listFilePath.add(pathProjectRoot.resolve("src/test/java").toString())

            run {
                val method = ReflectionUtils.findMethod(SharonApplication::class.java, "listUpFiles")
                ReflectionUtils.invokeMethod(method.get(), application)
            }

            val files = application.getFiles()
            assertEquals(2, files.size)
            assertEquals(pathSrcFile, files[0].srcFile)
            assertEquals(pathSrcFile, files[1].srcFile)
        }
    }

    @Test
    fun testListUpFiles002() {
        val args = Array(0) { "" }

        SharonApplication(args).use { application ->
            // プロパティ設定
            application.listFilePath.add("_TEST_")

            val e = assertThrows<IllegalArgumentException> {
                val method = ReflectionUtils.findMethod(SharonApplication::class.java, "listUpFiles")
                ReflectionUtils.invokeMethod(method.get(), application)
            }
            assertEquals("指定がファイルでもディレクトリでもありません。 _TEST_", e.message)
        }
    }

    @Test
    fun testProcessFiles001() {
        // TODO:
    }

}
