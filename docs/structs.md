# Structs

## Struct Variable Types
Variables can take on a struct type (as opposed to a basic type) asociated with a struct. Structs are similar to classes but they are defined within the mcppc compiler, and so cannot be created in mcpp code (similar to Kerbal Operating System). True classes (definable in mcpp) do not yet exist but might be added in the future.

The struct type defines the following behaviors:
 - what if any type arguments are required
 - what a variable of this type can be set to (and how to set it)
 - whether the type has a constructor
 - any operations that can be done on this variable
 - any fields it has (accessable with the `.` operator)
 - any member builtin functions it has (accessable with the `.` operator)
 - any static builtin functions it has (accessable with the `.` operator)
 - whether the type can be indexed (see [Indexing](#indexing))

Structs have no concept of inheritence, but some belong to a group with very similar behavior. We put these in their own subsection.

Some structs have a constructor. This is called in an equation just like a function with the type as the function name.
```mcpp
Vec3d<3> vec = Vec3d<3>(0,1,0);
```
## Indexing
Some struct variables can be indexed with `[]` brackets.
```mcpp
List<int> a = List(1,2,3);
a[1] = 5;
printf("2nd item = %s", a[1]); // will print 5
```
## Unusual Data Behavior
<!--mention Entity and Bossbar-->
Most struct types are simply an abstraction of nbt data.
However, there are a few structs that are not, or have unusual behavoior.
The following structs cannot mask an nbt location:
 - [Entity Structs](/docs/structgroup/entity.md) are all an abstraction of an entity tag
 - [Bossbar](/docs/struct/bossbar.md) is an abstraction of a bossbar
 - [Random](/docs/struct/random.md) is an abstraction of a sequenceId
In addition, most of the structs have a fixed nbt tag type. The exception to this is [Obj](/docs/struct/obj.md), which is polymorphic.
## List of Basic Structs
 - [Bossbar](/docs/struct/bossbar.md)
 - [Compound](/docs/struct/compound.md)
 - [Random](/docs/struct/random.md)
 - [Obj](/docs/struct/obj.md)
 - [String](/docs/struct/string.md)
 - [UUID](/docs/struct/uuid.md)
# List of Groups of Structs
 - [Collection Structs](/docs/structgroup/collection.md)
 - [Entity Structs](/docs/structgroup/entity.md)
 - [Map Structs](/docs/structgroup/map.md)
 - [Vector Structs](/docs/structgroup/vector.md)
# List of Singleton Structs
 - [particles](/docs/singleton/particles.md)
