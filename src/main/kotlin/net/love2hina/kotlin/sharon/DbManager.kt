package net.love2hina.kotlin.sharon

import net.love2hina.kotlin.sharon.dao.FileMapDao
import net.love2hina.kotlin.sharon.dao.FileMapDaoImpl
import org.seasar.doma.jdbc.Config
import org.seasar.doma.jdbc.JdbcLogger
import org.seasar.doma.jdbc.Naming
import org.seasar.doma.jdbc.dialect.Dialect
import org.seasar.doma.jdbc.dialect.H2Dialect
import org.seasar.doma.jdbc.tx.LocalTransaction
import org.seasar.doma.jdbc.tx.LocalTransactionDataSource
import org.seasar.doma.jdbc.tx.LocalTransactionManager
import org.seasar.doma.jdbc.tx.TransactionManager
import org.seasar.doma.slf4j.Slf4jJdbcLogger
import java.io.File

import javax.sql.DataSource

class DbManager: AutoCloseable {

    private data class DbConfig(
        private val dialect: Dialect,
        private val dataSource: DataSource,
        private val jdbcLogger: JdbcLogger,
        val localTransaction: LocalTransaction
    ): Config {
        private val transactionManager: TransactionManager = LocalTransactionManager(localTransaction)

        override fun getDialect(): Dialect = dialect
        override fun getDataSource(): DataSource = dataSource
        override fun getJdbcLogger(): JdbcLogger = jdbcLogger
        override fun getTransactionManager(): TransactionManager = transactionManager
        override fun getNaming(): Naming = Naming.SNAKE_LOWER_CASE

        val fileMapDao: FileMapDao = FileMapDaoImpl(this)
    }

    private val dbFile = File("db").absoluteFile

    private var config: DbConfig? = null

    init {
        val dataSource = LocalTransactionDataSource(
            "jdbc:h2:${dbFile.path};DB_CLOSE_DELAY=1", "sa", null)
        val logger = Slf4jJdbcLogger()

        config = DbConfig(
            H2Dialect(),
            dataSource,
            logger,
            dataSource.getLocalTransaction(logger))
    }

    override fun close() {
        config = null

        val regexDbFile = Regex("db\\..+\\.db", RegexOption.IGNORE_CASE)
        val dbFiles = dbFile.parentFile.listFiles { _: File, name: String -> regexDbFile.matches(name) }
        for (file in dbFiles) {
            file.delete()
        }
    }

    fun <R> execute(block: (FileMapDao) -> R): R {
        val _config = config!!
        val supplier: () -> R = { block.invoke(_config.fileMapDao) }
        return _config.transactionManager.required(supplier)
    }

}
