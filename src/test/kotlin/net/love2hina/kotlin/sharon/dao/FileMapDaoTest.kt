package net.love2hina.kotlin.sharon.dao

import net.love2hina.kotlin.sharon.DbConfig
import net.love2hina.kotlin.sharon.entity.FileMap
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.seasar.doma.jdbc.UniqueConstraintException

open class FileMapDaoTest {

    protected val config = DbConfig.create()

    protected val dao = FileMapDaoImpl(config)

    // JUnitのテスト実行時とは違うインスタンスになってしまうが…
    companion object Impl: FileMapDaoTest() {

        @Suppress("unused")
        @BeforeAll
        @JvmStatic
        fun initialize() {
            config.transactionManager.required { dao.create() }
        }

        @Suppress("unused")
        @AfterAll
        @JvmStatic
        fun release() {
            config.transactionManager.required { dao.drop() }
        }

    }

    @Test
    fun insert001() {
        val entity = FileMap()

        config.transactionManager.required { dao.insert(entity) }
    }

    @Test
    fun insert002() {
        val entity = FileMap()

        assertThrows<UniqueConstraintException> {
            config.transactionManager.required {
                dao.insert(entity)
                dao.insert(entity)
            }
        }
    }

}
