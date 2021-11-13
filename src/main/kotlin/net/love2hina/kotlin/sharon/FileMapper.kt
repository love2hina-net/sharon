package net.love2hina.kotlin.sharon

import java.io.File
import java.util.*
import kotlin.collections.HashMap

internal class FileMapper {

    private val map = HashMap<String, File>()

    fun assign(file: File): String {
        var id: String

        synchronized(map) {
            do {
                val uuid = UUID.randomUUID()
                id = "$uuid.xml"
            } while (map.containsKey(id))

            map[id] = file
        }

        return id
    }

}
