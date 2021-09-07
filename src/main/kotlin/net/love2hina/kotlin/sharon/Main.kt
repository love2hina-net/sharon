package net.love2hina.kotlin.sharon

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var returnCode: Int = 0

    try {
        val application = SharonApplication(args)

        application.initialize()
    }
    catch (e: Throwable) {
        returnCode = -1
    }

    exitProcess(returnCode)
}
