package net.love2hina.kotlin.sharon.entity

import kotlinx.serialization.Serializable
import org.seasar.doma.*
import java.util.UUID

@Serializable
@Entity(metamodel = Metamodel())
@Table(name = "FILEMAP")
data class FileMap(

    /** ID(UUID) */
    @Id
    @Column(name = "ID")
    var id: String,

    /** ソースファイルパス */
    @Column(name = "SRC_FILE")
    var srcFile: String,

    /** XMLファイルパス */
    @Column(name = "XML_FILE")
    var xmlFile: String,

    /** パッケージパス */
    @Column(name = "PACKAGE")
    var packagePath: String?,

    /** ソースファイルの名前(拡張子含まず) */
    @Column(name = "FILENAME")
    var fileName: String,

) {
    constructor(): this(UUID.randomUUID().toString(), "", "", null, "")
}
