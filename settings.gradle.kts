rootProject.name = "brainiac"

include(
    "core",
    "core:identity",
    "core:identity:api",
    "core:identity:impl",
    "core:fileaccess",
    "core:fileaccess:api",
    "core:fileaccess:impl",
    "core:process",
    "core:process:core-loop",
    "core:process:core-loop:api",
    "core:process:core-loop:impl",
    "core:process:search",
    "core:process:search:api",
    "core:process:search:impl",
    "agents",
    "agents:api",
    "agents:impl",
)