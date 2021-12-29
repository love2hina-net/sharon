package net.love2hina.kotlin.sharon

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var returnCode: Int = 0

    try {
        SharonApplication(args).use { application ->
            application.initialize()

            application.processFiles()
            application.export()
        }
    }
    catch (e: Throwable) {
        returnCode = -1
    }

    exitProcess(returnCode)
}
