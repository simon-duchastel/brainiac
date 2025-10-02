package com.brainiac.core.fileaccess

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.datetime.Instant

@Serializable
data class LTMFrontmatter(
    val uuid: String,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    val tags: List<String>,
    var reinforcementCount: Int
)

@Serializable
data class LTMFile(
    val frontmatter: LTMFrontmatter,
    val content: String
)