package net.love2hina.kotlin.sharon

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

        stack.stream().forEach {
            if (strPackageName.isNotEmpty())
                strPackageName.append('.')
            strPackageName.append(it)
        }

        return strPackageName.toString()
    }

    fun getFullName(name: String): String {
        val strPackageName = StringBuilder()

        stack.stream().forEach {
            if (strPackageName.isNotEmpty())
                strPackageName.append('.')
            strPackageName.append(it)
        }

        if (strPackageName.isNotEmpty())
            strPackageName.append('.')
        strPackageName.append(name)

        return strPackageName.toString()
    }

}
