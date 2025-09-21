# Core FS Module

File system operations module implementing transparent, hierarchical memory storage for the Braniac AI Memory System.

## Why File-System Centric Design

- **Transparency**: Human-readable plain-text files ensure data longevity and interoperability
- **Hierarchical Structure**: Directory hierarchy itself represents knowledge organization
- **Durability**: Avoids proprietary databases in favor of standard file formats
- **Debugging**: Memory structures are directly inspectable and modifiable

## Key Design Decisions

- **Atomic Operations**: File locking ensures consistent memory operations during concurrent access
- **YAML Frontmatter**: Structured metadata in memory files for indexing and relationships
- **Directory Indexing**: `_index.md` files provide searchable summaries and cross-references
- **Access Logging**: All file operations are logged for usage pattern analysis

This module provides the foundation for the specification's file-system centric memory architecture, handling all low-level storage operations while maintaining the transparency principle.