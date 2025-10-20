package com.duchastel.simon.brainiac.core.process.context

import ai.koog.prompt.llm.LLModel

data class BrainiacContext(
    val highThoughtModel: LLModel,
    val mediumThoughtModel: LLModel,
    val lowThoughtModel: LLModel,
)