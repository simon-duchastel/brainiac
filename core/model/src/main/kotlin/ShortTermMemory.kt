package com.braniac.core.model

import java.time.Instant

data class StructuredData(
    val goals: List<String>,
    val keyFacts: List<String>,
    val tasks: List<String>
)

data class Event(
    val timestamp: Instant,
    val user: String,
    val ai: String,
    val thoughts: String
)

data class ShortTermMemory(
    val summary: String,
    val structuredData: StructuredData,
    val eventLog: List<Event>
)