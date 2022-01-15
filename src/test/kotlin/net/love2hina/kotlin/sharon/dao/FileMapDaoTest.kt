package net.love2hina.kotlin.sharon.dao

import net.love2hina.kotlin.sharon.DbManager
import net.love2hina.kotlin.sharon.entity.FileMap
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.seasar.doma.jdbc.UniqueConstraintException

open class FileMapDaoTest {

    protected val dbManager = DbManager()

    // JUnitのテスト実行時とは違うインスタンスになってしまうが…
    companion object Impl: FileMapDaoTest() {

        @Suppress("unused")
        @BeforeAll
        @JvmStatic
        fun initialize() {
            dbManager.execute { dao-> dao.create() }
        }

        @Suppress("unused")
        @AfterAll
        @JvmStatic
        fun release() {
            dbManager.execute { dao-> dao.drop() }
        }

    }

    @Test
    fun insert001() {
        val entity = FileMap()

        dbManager.execute { dao-> dao.insert(entity) }
    }

    @Test
    fun insert002() {
        val entity = FileMap()

        assertThrows<UniqueConstraintException> {
            dbManager.execute { dao ->
                dao.insert(entity)
                dao.insert(entity)
            }
        }
    }

}
