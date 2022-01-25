package net.love2hina.kotlin.sharon.data

import kotlin.collections.LinkedHashMap

internal data class MethodInfo(

    /** 説明 */
    override val description: StringBuilder = StringBuilder(),

    /** パラメーター型 */
    val typeParameters: HashMap<String, TypeParameterInfo> = LinkedHashMap(),

    /** パラメーター */
    val parameters: HashMap<String, ParameterInfo> = LinkedHashMap(),

    /** 戻り値 */
    var result: ReturnInfo? = null,

    /** 例外 */
    val throws: HashMap<String, ThrowsInfo> = LinkedHashMap(),

    /*
    * see
@since
@deprecated
@serialData
{@link}
{@linkplain}
{@inheritDoc}
{@docRoot}
    * */

): JavadocInfo {

    override fun parseTag(name: String?, value: String): Boolean {
        return when (name) {
            "param" -> {
                val r = Regex("^(?:(?<name>\\w+)|<(?<type>\\w+)>)(?:\\s+(?<desc>.*))?$")
                val m = r.find(value)

                if (m != null) {
                    val groups = (m.groups as MatchNamedGroupCollection)

                    val paramName = groups["name"]?.value
                    val paramType = groups["type"]?.value
                    val paramDesc = groups["desc"]?.value ?: ""

                    if (paramName != null) {
                        val paramInfo = parameters.getOrPut(paramName) { ParameterInfo("", "", paramName) }

                        paramInfo.description = paramDesc
                    }
                    else if (paramType != null) {
                        val typeParameterInfo = typeParameters.getOrPut(paramType) { TypeParameterInfo(paramType) }

                        typeParameterInfo.description = paramDesc
                    }
                }

                true
            }
            "return" -> {
                val resultInfo: ReturnInfo = result ?: ReturnInfo()

                resultInfo.description = value
                result = resultInfo

                true
            }
            "throws", "exception" -> {
                val r = Regex("^(?<name>\\w+)(?:\\s+(?<desc>.*))?$")
                val m = r.find(value)

                if (m != null) {
                    val groups = (m.groups as MatchNamedGroupCollection)

                    val expType = groups["name"]!!.value
                    val expDesc = groups["desc"]?.value ?: ""

                    val throwsInfo = throws.getOrPut(expType) { ThrowsInfo(expType) }

                    throwsInfo.description = expDesc
                }

                true
            }
            else -> false
        }
    }

}
