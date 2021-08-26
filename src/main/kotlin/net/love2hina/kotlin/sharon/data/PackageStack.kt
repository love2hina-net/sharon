package net.love2hina.kotlin.sharon.data

import net.love2hina.kotlin.sharon.appendSeparator
import java.util.LinkedList

internal class PackageStack {

    private val stack = LinkedList<String>()

    fun push(name: String) {
        stack.addLast(name)
    }

    fun pop() {
        stack.removeLast()
    }

    fun getPackageName(): String {
        val strPackageName = StringBuilder()

        stack.stream().forEach { strPackageName.appendSeparator('.', it) }
        return strPackageName.toString()
    }

    fun getFullName(name: String): String {
        val strPackageName = StringBuilder()

        stack.stream().forEach { strPackageName.appendSeparator('.', it) }
        strPackageName.appendSeparator('.', name)
        return strPackageName.toString()
    }

}
