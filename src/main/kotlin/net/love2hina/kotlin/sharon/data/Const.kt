package net.love2hina.kotlin.sharon.data

internal val REGEXP_BLOCK_COMMENT = Regex("^\\s*[/*]*\\s*(?<content>\\S|\\S.*\\S)?\\s*$")
internal val REGEXP_LINE_COMMENT = Regex("^/\\s*(?<content>\\S|\\S.*\\S)\\s*$")
internal val REGEXP_ASSIGNMENT = Regex("^(?<var>.*\\S)\\s*\\=\\s*(?<value>\\S.*)$")
internal val REGEXP_ANNOTATION = Regex("@(?<name>\\w+)")
internal val REGEXP_NEWLINE = Regex("\r\n|\r|\n")
