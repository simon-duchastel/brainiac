# Core Identity Module

Centralized service for managing the AI system's core identity, providing a foundation for future sophistication and extensibility.

## Why Dedicated Identity Service

- **Separation of Concerns**: Isolates identity management from other core processes
- **Extensibility**: Provides foundation for future enhancements like dynamic identity evolution
- **Testability**: Enables independent testing of identity logic without filesystem dependencies
- **Future-Proofing**: Ready for sophisticated features like context-aware identity adaptation

## Current Implementation

### CoreIdentityService Interface
```kotlin
interface CoreIdentityService {
    fun getCoreIdentity(): CoreIdentity
    fun getCoreIdentityContent(): String
}
```

### DefaultCoreIdentityService
- Reads core identity content from filesystem via FileSystemService
- Parses content into structured CoreIdentity object
- Provides both raw content and parsed object access
- Currently uses simple parsing logic but ready for enhancement

## Future Enhancements

The service architecture enables sophisticated future capabilities:

### Dynamic Identity Evolution
- Context-aware personality adaptation
- Learning from interaction patterns
- Personality refinement based on user feedback

### Multi-Persona Support
- Different identities for different contexts
- Role-based identity switching
- Specialized expertise personas

### Identity Versioning
- Track identity changes over time
- Rollback capabilities
- A/B testing of different personas

### Smart Parsing
- YAML/Markdown frontmatter parsing
- Template-based identity generation
- Validation and consistency checking

## Integration

The CoreLoopProcess uses CoreIdentityService instead of direct file access:

```kotlin
class CoreLoopProcess(
    // ...
    private val coreIdentityService: CoreIdentityService
) {
    private fun assembleWorkingMemory(...): String {
        val coreIdentity = coreIdentityService.getCoreIdentityContent()
        // ...
    }
}
```

This abstraction layer ensures clean separation and enables future enhancements without breaking existing functionality.