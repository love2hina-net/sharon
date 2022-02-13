package net.love2hina.kotlin.sharon.data

import net.love2hina.kotlin.sharon.setProperty

internal data class EnumInfo(

    /** 説明 */
    override val description: StringBuilder = StringBuilder(),

    /** since */
    var since: String? = null,

    /** deprecated */
    var deprecated: String? = null,

    /** serial */
    var serial: String? = null,

    /** author */
    val author: MutableList<String> = mutableListOf(),

    /** version */
    var version: String? = null,

    /*
     * @see
     */

): JavadocInfo {

    override fun parseTag(name: String?, value: String): Boolean
        = when (name) {
            "author" -> {
                author.addAll(value.split(", "))
                true
            }
            "since", "deprecated", "serial", "version" -> {
                this.setProperty(name, value)
                true
            }
            else -> false
        }

}
