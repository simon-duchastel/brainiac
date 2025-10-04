@file:OptIn(ExperimentalTime::class)

package com.brainiac.core.fileaccess

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@Serializable
data class StructuredData(
    val goals: List<String>,
    val keyFacts: List<String>,
    val tasks: List<String>
)

@Serializable
data class Event(
    @Contextual val timestamp: Instant,
    val user: String,
    val ai: String,
    val thoughts: String
)

@Serializable
data class ShortTermMemory(
    val summary: String,
    val structuredData: StructuredData,
    val eventLog: List<Event>
)
