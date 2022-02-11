package net.love2hina.kotlin.sharon

import net.love2hina.kotlin.sharon.entity.FileMap
import net.love2hina.kotlin.sharon.parser.Parser
import org.seasar.doma.jdbc.UniqueConstraintException

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.*
import kotlinx.serialization.json.*

class SharonApplication(val args: Array<String>): FileMapper, AutoCloseable {

    private val dbManager = DbManager()

    lateinit var pathOutputDir: Path
        private set

    val listFilePath = LinkedList<String>()

    init {
        // setup
        dbManager.execute { it.create() }
    }

    override fun close() {
        dbManager.execute { it.drop() }
        dbManager.close()
    }

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

        if (!pathOutputDir.isAbsolute) {
            // 絶対パス化
            pathOutputDir = pathOutputDir.toAbsolutePath()
        }
    }

    private fun listUpFiles() {
        listFilePath.forEach {
            val file = File(it)

            if (file.isFile) {
                // 単一のファイル
                assign(file)
            }
            else if (file.isDirectory) {
                // ディレクトリ
                Files.walk(file.toPath())
                    .filter { path -> path.toString().endsWith(".java", true) }
                    .forEach { path -> assign(path.toFile()) }
            }
            else {
                throw IllegalArgumentException("指定がファイルでもディレクトリでもありません。 $it")
            }
        }
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { "%02x".format(it) }

    private fun Int.format(format: String): String =  String.format(format, this)

    private fun assign(fileSrc: File): FileMap {
        val entity = FileMap()
        entity.srcFile = fileSrc.absolutePath
        entity.fileName = fileSrc.name // TODO 拡張子削除

        val md = MessageDigest.getInstance("SHA-256")
        md.update(fileSrc.absolutePath.toByteArray())
        val fileHash = md.digest().toHexString()
        var index = 0

        do {
            entity.id = "${fileHash}_${index.format("%03d")}"
            entity.xmlFile = pathOutputDir.resolve("${entity.id}.xml").toString()

            try {
                dbManager.execute { it.insert(entity) }

                return entity
            }
            catch (e: UniqueConstraintException) {
                // リトライ(キー重複)
                ++index
            }
        } while (true)
    }

    fun processFiles() {
        dbManager.execute { dao ->
            dao.selectAll().use { stream ->
                stream.parallel().forEach { Parser.parse(this, it) }
            }
        }
    }

    fun export() {
        val json = Json { encodeDefaults = true }
        val fileList = pathOutputDir.resolve("filelist.json").toFile()

        BufferedWriter(OutputStreamWriter(FileOutputStream(fileList, false), StandardCharsets.UTF_8)).use { writer ->
            dbManager.execute { dao ->
                dao.selectAll().use { stream ->
                    stream.forEach {
                        writer.appendLine(json.encodeToString(it))
                    }
                }
            }
        }
    }

    internal fun getFiles(): Array<FileMap> {
        lateinit var result: Array<FileMap>

        dbManager.execute { dao ->
            dao.selectAll().use { stream ->
                result = stream.toArray { arrayOfNulls<FileMap>(it) }
            }
        }

        return result
    }

    override fun applyPackage(entity: FileMap, packagePath: String?) {
        entity.packagePath = packagePath
        dbManager.execute { it.update(entity) }
    }

}
