package net.love2hina.kotlin.sharon

import net.love2hina.kotlin.sharon.entity.FileMap

import java.io.File

internal interface FileMapper {
    fun applyPackage(entity: FileMap, packagePath: String?)
}
