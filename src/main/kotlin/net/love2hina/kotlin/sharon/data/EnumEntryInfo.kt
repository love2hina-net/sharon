package net.love2hina.kotlin.sharon.data

import net.love2hina.kotlin.sharon.setProperty

internal data class EnumEntryInfo(

    /** 説明 */
    override val description: StringBuilder = StringBuilder(),

    /** since */
    var since: String? = null,

    /** deprecated */
    var deprecated: String? = null,

    /** serial */
    var serial: String? = null,

    /*
     * @see
     * @serialField
     */

): JavadocInfo {

    override fun parseTag(name: String?, value: String): Boolean
        = when (name) {
            "since", "deprecated", "serial" -> {
                this.setProperty(name, value)
                true
            }
            else -> false
        }

}
