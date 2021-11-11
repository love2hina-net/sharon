package net.love2hina.kotlin.sharon

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream
import kotlin.streams.asStream

internal class SharonApplication(val args: Array<String>) {

    lateinit var pathOutputDir: Path
        private set

    val listFilePath = LinkedList<String>()

    lateinit var files: List<File>
        private set

    fun initialize() {
        parseArgs()
        checkArgs()
        listUpFiles()
    }

    private fun parseArgs() {
        val reg = Regex("^--(?<flag>\\w+)?$")
        var flagCurrent: String? = null
        var flagAllFiles = false

        for (i in args) {
            var matchFlag: MatchResult?

            if (!flagAllFiles) {
                matchFlag = reg.find(i)

                if ((matchFlag != null) && (flagCurrent != null)) {
                    // 異常なフラグ連続指定
                    throw IllegalArgumentException("フラグの値が指定されていません。 --$flagCurrent")
                }
                else if (matchFlag != null) {
                    // フラグ
                    val groups = (matchFlag.groups as MatchNamedGroupCollection)
                    when (val flag = groups["flag"]?.value) {
                        null -> {
                            flagAllFiles = true
                            flagCurrent = null
                        }
                        else -> {
                            flagCurrent = flag
                        }
                    }
                }
                else if (flagCurrent != null) {
                    // フラグの値
                    when (flagCurrent) {
                        "outdir" -> { pathOutputDir = Path.of(i) }
                        else -> {
                            throw IllegalArgumentException("不明なフラグ値です。 --$flagCurrent")
                        }
                    }

                    flagCurrent = null
                }
                else {
                    // ファイル
                    listFilePath.add(i)
                }
            }
            else {
                // 常にファイルとして扱う
                listFilePath.add(i)
            }
        }
    }

    private fun checkArgs() {
        if (!::pathOutputDir.isInitialized) {
            throw IllegalArgumentException("出力ディレクトリの指定がありません。 --outdir")
        }
    }

    private fun listUpFiles() {
        files = listFilePath.asSequence().asStream().flatMap(this::mapToFile).toList()
    }

    private fun mapToFile(path: String): Stream<File> {
        val file = File(path)

        return if (file.isFile) {
            // 単一のファイル
            Stream.of(file)
        }
        else if (file.isDirectory) {
            // ディレクトリ
            Files.walk(file.toPath())
                .filter { it.toString().endsWith(".java", true) }
                .map { it.toFile() }
        }
        else {
            throw IllegalArgumentException("指定がファイルでもディレクトリでもありません。 $path")
        }
    }

}
