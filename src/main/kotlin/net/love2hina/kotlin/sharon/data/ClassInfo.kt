package net.love2hina.kotlin.sharon.data

import net.love2hina.kotlin.sharon.setProperty

internal data class ClassInfo(

    /** 説明 */
    override val description: StringBuilder = StringBuilder(),

    /** パラメーター型 */
    val typeParameters: HashMap<String, TypeParameterInfo> = LinkedHashMap(),

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
     * {@link}
     * {@linkplain}
     * {@docRoot}
     */

): JavadocInfo {

    override fun parseTag(name: String?, value: String): Boolean {
        return when (name) {
            "param" -> {
                val r = Regex("^<(?<type>\\w+)>(?:\\s+(?<desc>.*))?$")
                val m = r.find(value)

                if (m != null) {
                    val groups = (m.groups as MatchNamedGroupCollection)

                    val paramType = groups["type"]?.value
                    val paramDesc = groups["desc"]?.value ?: ""

                    if (paramType != null) {
                        val typeParameterInfo = typeParameters.getOrPut(paramType) { TypeParameterInfo(paramType) }

                        typeParameterInfo.description = paramDesc
                    }
                }

                true
            }
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

}
