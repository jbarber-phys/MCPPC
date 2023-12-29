# MCPPC
A compiler for compiling MCPP code into mcfunction files for Minecraft datapacks \(for Java Edition\).
## MCPP
MCPP \(or MC++\) is a heavy weight statically typed language for writing Minecraft datapacks. The Mcfunction language is very bad so it takes a lot of heavy equipment to fix it.

MCPP is designed to minimise the use of inline mcfunction code (whereas lots of similar tools heavily rely on it), especially for data manipulation and math operations.

Some of its key features are:
 - variables, which can manipulate:  data , scores, and a few other things
 - free-form equations
 - fixed point arithmatic, basic math functions, and vector math
 - functions with arguments and return values
 - several builtin functions that are built into the compiler (for optimization)
 - a type system, with several basic and struct types
 - constants expressions (which have their own type system)
 - several struct data types: String, Vector, Entity, Uuid
 - collection struct types: Stack, Queue, List, Set; as well as Map
 - threads that can execute over multiple ticks
 - a standard library
 - interoperability within and between mcpp projects, including pre-compiled ones
 - interoperability with normal mcfunction code
 - project-level compilation

It is possible that MCPP will have more key features in the future \(true classes, true floats, re-assignable references, etc\).

## Installation
See [Installation](docs/getting_started.md#1-installation).

In addition, a vscode extension for mcpp can be found [HERE](https://github.com/jbarber-phys/mcpp_vscode_ext).
## Hello World!
See [Hello World](docs/getting_started.md#2-hello-world).
## Documentation
Documentation is stored locally in this repository (see [Documentation](docs/index.md)).

## Known Issues
MCPPC will not always exit cleanly if there are compile errors in the mcpp code it is compiling (though it usually will).

