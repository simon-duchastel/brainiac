rootProject.name = "brainiac"

include("core-identity-api")
include("core-identity-impl")
include("core-fileaccess-api")
include("core-fileaccess-impl")
include("core-process-coreloop-api")
include("core-process-coreloop-impl")
include("core-process-search-api")
include("core-process-search-impl")
include("agents-api")
include("agents-impl")

// Map flattened names to actual directories
project(":core-identity-api").projectDir = file("core/identity/api")
project(":core-identity-impl").projectDir = file("core/identity/impl")
project(":core-fileaccess-api").projectDir = file("core/fileaccess/api")
project(":core-fileaccess-impl").projectDir = file("core/fileaccess/impl")
project(":core-process-coreloop-api").projectDir = file("core/process/core-loop/api")
project(":core-process-coreloop-impl").projectDir = file("core/process/core-loop/impl")
project(":core-process-search-api").projectDir = file("core/process/search/api")
project(":core-process-search-impl").projectDir = file("core/process/search/impl")
project(":agents-api").projectDir = file("agents/api")
project(":agents-impl").projectDir = file("agents/impl")