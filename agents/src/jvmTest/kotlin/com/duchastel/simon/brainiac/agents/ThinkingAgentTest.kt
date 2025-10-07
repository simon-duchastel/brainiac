package com.duchastel.simon.brainiac.agents

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ThinkingAgentTest : StringSpec({

    "should create ThinkingAgent successfully" {
        val agent = ThinkingAgent("test-api-key")
        agent.shouldBeInstanceOf<ThinkingAgent>()
    }

    "should implement Agent interface" {
        val agent: com.duchastel.simon.brainiac.core.agent.Agent = ThinkingAgent("test-api-key")
        agent.shouldBeInstanceOf<com.duchastel.simon.brainiac.core.agent.Agent>()
    }

    // Note: Integration tests with actual API calls should be done separately
    // with real API keys in a test environment
})
