rootProject.name = "brainiac"

include(
    "agents:gemini",
    "core:identity:api",
    "core:identity:impl",
    "core:fileaccess:api",
    "core:fileaccess:impl",
    "core:agents:api",
    "core:process:core-loop:api",
    "core:process:core-loop:impl",
    "core:process:search:api",
    "core:process:search:impl",
)
