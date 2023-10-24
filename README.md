# MCPPC
A compiler for compiling MCPP code into mcfunction files for Minecraft datapacks (for Java Edition).
## MCPP
MCPP (or MC++) is a heavy weight statically typed language for writing Minecraft datapacks.
Some of its key features are:
 - variables, which are abstraction of:  data (storage, entity, block), scores, and entity tags; with variables
 - free-form equations
 - fixed point arithmatic, basic math functions, and vector math
 - functions with arguments and return values
 - several builtin functions that are built into the compiler (for optimization)
 - a type system, with the following basic types: int, long, short, byte, float<precision>, double<precision>, bool, void; in addition to structs
 - constants expressions; these have their own type system with the types being: num, flag, text, type, selector, nbt, coords, rot; these evaluate at compile time
 - structs: similar to classes but are built into the compiler (cannot be defined by mcpp code); examples being: String, Vector, Entity, Uuid
 - collections: struct types for various collections: Stack, Queue, List, Set; as well as: Map
 - low level control
 - interoperability with
 - a standard library
 - interoperability within and between mcpp projects, including pre-compiled ones
 - interoperability with normal mcfunction code
 - project-level compilation
It is possible that MCPP will have more key features in the future (true classes, version targeting, true floats, etc).
## Documentation
Documentation is still TODO.

## Installation
Currently, this repository is an eclipse Java Project. No bin-install is present here.
