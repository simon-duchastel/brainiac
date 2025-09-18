package com.braniac.core.model

import java.time.Instant

data class LTMFrontmatter(
    val uuid: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val tags: List<String>,
    var reinforcementCount: Int
)

data class LTMFile(
    val frontmatter: LTMFrontmatter,
    val content: String
)