package net.love2hina.kotlin.sharon.dao

import net.love2hina.kotlin.sharon.entity.*
import org.seasar.doma.*
import org.seasar.doma.jdbc.Config
import org.seasar.doma.kotlin.jdbc.criteria.KEntityql

import java.util.Arrays
import java.util.stream.Stream

@Dao
interface FileMapDao {

    private val query
        get() = KEntityql(Config.get(this))

    @Sql("""
CREATE TABLE FILEMAP (
 ID CHAR(36) NOT NULL PRIMARY KEY,
 SRC_FILE VARCHAR(2048) NOT NULL,
 XML_FILE VARCHAR(2048) NOT NULL,
 PACKAGE VARCHAR(2048),
 FILENAME VARCHAR(2048) NOT NULL
);

CREATE INDEX IDX_FILEMAP001 ON FILEMAP (
 PACKAGE ASC,
 FILENAME ASC
);
""")
    @Script
    fun create()

    @Sql("""
DROP INDEX IF EXISTS IDX_FILEMAP001;
DROP TABLE IF EXISTS FILEMAP;
""")
    @Script
    fun drop()

    @Sql("SELECT * FROM FILEMAP WHERE ID = /*id*/''")
    @Select
    fun select(id: String): FileMap

    @Sql("SELECT * FROM FILEMAP ORDER BY PACKAGE ASC, FILENAME ASC")
    @Select
    fun selectAll(): Stream<FileMap>

    @BatchInsert
    fun insert(entities: List<FileMap>): IntArray

    fun insert(vararg entities: FileMap): Int =
        Arrays.stream(insert(entities.asList())).sum()

    @BatchUpdate
    fun update(entities: List<FileMap>): IntArray

    fun update(vararg entities: FileMap): Int =
        Arrays.stream(update(entities.asList())).sum()

}
