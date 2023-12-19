# Variable Types

## Usage
Every [Variable](#variables), [Function](#functions) argument, and function return value has a variable type ([Constants](#constants) have their own seperate type system).
## Basic Types
The following basic types are supported:
 - bool
 - byte
 - short
 - int
 - long
 - float`<`precision`>`
 - double`<`precision`>`

The basic types all correspond to an nbt-type (with bool being equivalent to a byte).

This leaves three nbt tag types unaccounted for: string, list, and compound. These are only used by [struct types](#struct-types).

Even though the 64 bit types long and double are supported, all mathematical operations must happen on the scoreboard (which is restricted to 32 bits) and so operations will not work if the values are too large.
## Precision
MCPP by default will 
Both of the float types will be operated on as fixed points. By default, MCPP will do all operations with a precision of 3.

The default precision can be overridden per variable by putting the precision in angle brackets after the type name:
```mcpp
private double a = 0.123; //default precision
private double<4> b = 0.1234; // higher precision
```

Most operations on floats will end up rounding it to its precision, but this will not be done unless nececcary for math operations. In particular, a direct assignment will allow a value below the precision to be transmitted losslessly.
## Struct Types
In addition to the basic types, there are also [struct](/docs/structs.md) types. The distinction is similar to the distinction between designated types and class types in java.

A Struct is kind of like a class except that they cannot be created in mcpp, but instead are made to exist by the compiler (this is similar to structs in Kerbal Operating System).

Most struct types mask one of the types of nbt data not supported by basic types (Nbt strings, lists, compounds). Note that there are a few structs that either do not represent nbt data or do so in an unusual way.

See [Structs](#struct-types) for more info and a list of structs.

True classes (definable in mcpp) do not yet exist but might be added in the future.