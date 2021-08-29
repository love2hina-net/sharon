package net.love2hina.kotlin.sharon.data

import kotlin.collections.LinkedHashMap

internal class MethodInfo {

    /** 説明 */
    var description = StringBuilder()

    /** パラメーター型 */
    var typeParameters = LinkedHashMap<String, TypeParameterInfo>()

    /** パラメーター */
    val parameters = LinkedHashMap<String, ParameterInfo>()

    /** 戻り値 */
    var result: ReturnInfo? = null

    /** 例外 */
    val throws = LinkedHashMap<String, ThrowsInfo>()

}
