package net.love2hina.kotlin.sharon.data

internal val REGEXP_ASSIGNMENT = Regex("^(?<var>.*\\S)\\s*\\=\\s*(?<value>\\S.*)$")
internal val REGEXP_ANNOTATION = Regex("@(?<name>\\w+)\\s+")
internal val REGEXP_NEWLINE = Regex("\r\n|\r|\n")
