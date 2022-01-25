package net.love2hina.kotlin.sharon.data

internal interface JavadocInfo {

    val description: StringBuilder

    fun parseTag(name: String?, value: String): Boolean

}
