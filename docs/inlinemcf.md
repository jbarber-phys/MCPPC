# Inline Mcfunction Commands

## Usage
Mcfunction commands can be inlined after a `$/` symbol. A standalone command must end with a semicolon:
```mcpp
$/say hi;
```
A command inside an equation can end with a semicolon or newline:
```mcpp
a = $/time query daytime
    + 1;
```
Inline commands should be avoided if possible. Some commands are made obselete by mcpp features; for example, all of the data commands are unneccecary because variables can do everything those commands do.

Some other commands, such as `/tp` have a builtin function for them.
```mcpp
tp(@s , ~ ~1 ~ , ~ 0);
```
Commands that put json text somehwere (`/tellraw`, `/title`) also have builtin functions ([printf, titlef](/docs/builtinfunctions.md#21-printf-and-related)).

In the future, it could be that all commands have a builtin function, making inline commands obselete.
## Formatting Mcfunctions With Constants
Constant expressions (or types equivalent to consts) can be inserted into mcfunctions by putting an equation inside a `$(...)` statement:
```mcpp
private const num H = 3;
$/tp @s ~ ~$(H) ~;
```
The expression inside the parens must be evaluatable to a const value.

The entity structs are const equivalent, so they can be inlined as well:
```mcpp
private Entity e;
//make a pig fly
$/tp $(e & @e[type=pig]) ~ ~6 ~;
```
This system is not the same as macros (which look the same but the line must start with a `$`). If the inline command starts with a `$` then it will just use macros instead.
## Macros
MCPP was mostly developed before macros existed. By coincidence, the syntax for macros and that for constant formatting is the same (except for the leading `$`).

MCPP functions do not use `/return` or macro arguments unless they are [`extern`](/docs/functions.md#5-extern-functions).
