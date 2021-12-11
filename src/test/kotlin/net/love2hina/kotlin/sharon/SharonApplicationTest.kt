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
            pathProjectRoot.resolve("out").toString(),
            pathProjectRoot.resolve("src/test/java").toString())

        // インスタンス作成
        val application = SharonApplication(args)

        // コール
        application.initialize()
    }

    @Test
    fun testParseArgs001() {
        val args = arrayOf(
            "--outdir",
            pathProjectRoot.resolve("out").toString(),
            pathProjectRoot.resolve("src/test/java1").toString(),
            "--",
            pathProjectRoot.resolve("src/test/java2").toString())

        // インスタンス作成
        val application = SharonApplication(args)

        // コール
        val method = ReflectionUtils.findMethod(SharonApplication::class.java, "parseArgs")
        ReflectionUtils.invokeMethod(method.get(), application)

        // チェック
        assertEquals(pathProjectRoot.resolve("out"), application.pathOutputDir)
        assertEquals(2, application.listFilePath.size)
        val pathSrcFile = arrayOf(
            pathProjectRoot.resolve("src/test/java1").toString(),
            pathProjectRoot.resolve("src/test/java2").toString())
        assertEquals(pathSrcFile[0], application.listFilePath[0])
        assertEquals(pathSrcFile[1], application.listFilePath[1])
    }

    @Test
    fun testParseArgs002() {
        val args = arrayOf(
            "--outdir",
            "--")

        // インスタンス作成
        val application = SharonApplication(args)

        try {
            // コール
            val method = ReflectionUtils.findMethod(SharonApplication::class.java, "parseArgs")
            ReflectionUtils.invokeMethod(method.get(), application)

            fail("例外が発生しない")
        }
        catch (e: IllegalArgumentException) {
            // 例外チェック
            assertEquals("フラグの値が指定されていません。 --outdir", e.message)
        }
        catch (e: Throwable) {
            fail(e)
        }
    }

    @Test
    fun testParseArgs003() {
        val args = arrayOf(
            "--dummy",
            "TEST")

        // インスタンス作成
        val application = SharonApplication(args)

        try {
            // コール
            val method = ReflectionUtils.findMethod(SharonApplication::class.java, "parseArgs")
            ReflectionUtils.invokeMethod(method.get(), application)

            fail("例外が発生しない")
        }
        catch (e: IllegalArgumentException) {
            // 例外チェック
            assertEquals("不明なフラグ値です。 --dummy", e.message)
        }
        catch (e: Throwable) {
            fail(e)
        }
    }

    @Test
    fun testCheckArgs001() {
        val args = Array(0) { "" }
        val path = pathProjectRoot.resolve("out")

        // インスタンス作成
        val application = SharonApplication(args)

        // プロパティ設定
        setProperty(application, "pathOutputDir", path)

        run {
            // コール
            val method = ReflectionUtils.findMethod(SharonApplication::class.java, "checkArgs")
            ReflectionUtils.invokeMethod(method.get(), application)
        }
    }

    @Test
    fun testCheckArgs002() {
        val args = arrayOf(
            pathProjectRoot.resolve("out").toString())

        // インスタンス作成
        val application = SharonApplication(args)

        try {
            // コール
            val method = ReflectionUtils.findMethod(SharonApplication::class.java, "checkArgs")
            ReflectionUtils.invokeMethod(method.get(), application)

            fail("例外が発生しない")
        }
        catch (e: IllegalArgumentException) {
            // 例外チェック
            assertEquals("出力ディレクトリの指定がありません。 --outdir", e.message)
        }
        catch (e: Throwable) {
            fail(e)
        }
    }

    @Test
    fun testListUpFiles001() {
        val args = Array(0) { "" }
        val pathSrcFile = pathProjectRoot.resolve("src/test/java/net/love2hina/kotlin/sharon/ParseTestTarget.java").toString()

        // インスタンス作成
        val application = SharonApplication(args)

        // プロパティ設定
        application.listFilePath.add(pathSrcFile)
        application.listFilePath.add(pathProjectRoot.resolve("src/test/java").toString())

        run {
            val method = ReflectionUtils.findMethod(SharonApplication::class.java, "listUpFiles")
            ReflectionUtils.invokeMethod(method.get(), application)
        }

        assertEquals(2, application.files.size)
        assertEquals(pathSrcFile, application.files[0].toString())
        assertEquals(pathSrcFile, application.files[1].toString())
    }

    @Test
    fun testListUpFiles002() {
        val args = Array(0) { "" }

        // インスタンス作成
        val application = SharonApplication(args)

        // プロパティ設定
        application.listFilePath.add("TEST")

        try {
            val method = ReflectionUtils.findMethod(SharonApplication::class.java, "listUpFiles")
            ReflectionUtils.invokeMethod(method.get(), application)

            fail("例外が発生しない")
        }
        catch (e: IllegalArgumentException) {
            // 例外チェック
            assertEquals("指定がファイルでもディレクトリでもありません。 TEST", e.message)
        }
        catch (e: Throwable) {
            fail(e)
        }
    }

    @Test
    fun testProcessFiles001() {
        // TODO:
    }

}
