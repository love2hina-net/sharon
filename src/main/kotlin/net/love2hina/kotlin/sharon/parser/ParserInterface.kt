package net.love2hina.kotlin.sharon.parser

import net.love2hina.kotlin.sharon.FileMapper
import net.love2hina.kotlin.sharon.entity.FileMap
import java.util.stream.Stream

internal interface ParserInterface {

    fun parse(mapper: FileMapper, entities: Stream<FileMap>)

}
