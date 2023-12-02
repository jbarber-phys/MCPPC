# MCPP Basics
<!-- vscode-markdown-toc -->
* 1. [Project Structure and Compilation (Basics)](#ProjectStructureandCompilationBasics)
* 2. [Syntax](#Syntax)
* 3. [Variables (Basics)](#VariablesBasics)
* 4. [Functions (Basics)](#FunctionsBasics)
* 5. [Variable Types (Basics)](#VariableTypesBasics)
* 6. [Constants (Basics)](#ConstantsBasics)
* 7. [Equations (Basics)](#EquationsBasics)
* 8. [Flow Control (Basics)](#FlowControlBasics)
* 9. [Importing (Basics)](#ImportingBasics)
* 10. [Threads (Basics)](#ThreadsBasics)
* 11. [Inline Mcfunction Commands (Basics)](#InlineMcfunctionCommandsBasics)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->
##  1. <a name='ProjectStructureandCompilationBasics'></a>Project Structure and Compilation (Basics)
MCPPC compiles at the project (datapack) level. An mcpp project looks similar to a datapack:
```
├── pack.mcmeta
├── data
│   ├── <namespace>
│   │   ├── src
│   │   │   ├── (all your .mcpp files)
│   │   ├── include (optional)
│   │   │   ├── (mcpp will put generated .mch files here)
│   │   ├── functions (optional)
│   │   │   ├── (mcpp will put compiled .mcfunction files here)
│   ├── ... more namespaces
├── ...
```
The mcpp project may or may not be the same as the datapack output and include output directories.

A vscode build task for running the compiler is found in 
[/mcppc-build-task.json](../mcppc-build-task.json). In order to work, the compiler must be runned with the cwd in its directory.

The compiler accepts lots of arguments but the most important ones are below.
- -src $path : the source path; defaults to cwd
- -o $path : set the output directory (the datapack); defaults to cwd
  - note: the compiler will issue a warning if the output lacks an pack.mcmeta file, and then get user input as to whether to continue
- -g : add comments to mcfunctions with line number info

For more information, see [Compilation](./compilation.md).
##  2. <a name='Syntax'></a>Syntax
Mcpp uses syntax similar java / c++ / c#.

Code blocks are delimited by curly braces and every line of code (a statement) ends in a semicolon.
Comments use double forward slashes, etc.
##  3. <a name='VariablesBasics'></a>Variables (Basics)
MCPP supports statically typed variables. Variables are typically defined like below.
```mcpp
private double<3> a;
public int b = 2;
```
The 3 in the brackets specifies the fixed point precision (if absent, it defaults to 3).
A public variable can be accessed in other mcpp files. The access convention is different than normal: there is not block-specific variable access.
Variables can be assigned like so:
```mcpp
a = 3.5 * b;
```
Variables are usually an abstraction of storage but can be set to "live" somewhere else using masks. The following variable is the same as the executors velocity nbt data:
```mcpp
private Vec3d<3> velocity -> @s.Motion;
```
See [Variables](/docs/variables.md) for more info.
##  4. <a name='FunctionsBasics'></a>Functions (Basics)
Functions can be defined in mcpp like so:
```mcpp
private int afunction(int arg1, bool arg2){
  // your code here
  return = arg1 + 3;
}
```
Return statements do not break out of the function, and instead are treated similar to variable assignment (like Fortran). True returns (that also end execution) do not yet exist.

arguments normally pass by value. This can be changed with the `ref` keyword:
```mcpp
private void increment(ref int a){
  a = a + 1;
}
```
Normally, mcppc decides where to write the function's files to, but this can be overridden to make it make the function at a specific resourcelocation.

For more info, see [Functions](/docs/functions.md).

Note that in addition to normal functions, there are also [BuiltinFunctions](/docs/builtinfunctions.md). Like Structs, Builtin Functions are defined by the compiler and can take unusual expressions as arguments (but they will usually be function-like). They can also require that their arguments be [Constant](#constants) valued or be references to [Variables](#variables).
##  5. <a name='VariableTypesBasics'></a>Variable Types (Basics)
Every [Variable](#variables), [Function](#functions) argument, and function return value has a variable type ([Constants](#constants) have their own seperate type system).
The following basic types are supported:
 - bool
 - byte
 - short
 - int
 - long
 - float`<`precision`>`
 - double`<`precision`>`

The basic types all correspond to an nbt-type (with bool being equivalent to a byte).

In addition there are several [Struct](/docs/structs.md) types. Structs are similar to classes but they are defined within the mcppc compiler, and so cannot be created in mcpp code (similar to Kerbal Operating System). True classes (definable in mcpp) do not yet exist but might be added in the future.

See [Variable Types](/docs/variables.md) for more information. See [Structs](/docs/structs.md) for a list of struct types.
##  6. <a name='ConstantsBasics'></a>Constants (Basics)
Constants are parameters with values known at compile time. They can be used in equations, function templates, type arguments, and formatted inline mcfunctions.

Constants are declared differently:
```mcpp
public const num PI = 3.1415;
```
Consts have their own type system; all of the supported types are below.
 - num : a const number
 - flag: a const bool
 - text: a const string
 - selector: a target selector
 - nbt: a nbt tag or path
 - coords: world cordinates, which may be in caret or tilde notation
 - rot: similar to coords but for rotations

For more info, see [Constants](/docs/consts.md).
##  7. <a name='EquationsBasics'></a>Equations (Basics)
MCPP supports equations (expressions containing operators and values) in assignments, arguments, and indexes. MCPP does not currently allow equations at the start of statements or in.
The syntax is the same as c++ or java with the most important differences being:
 - there is no ternary operator
 - there are no short circuiting operators
 - the xor operator is `|!&` ("or and not and")
 - the caret `^` is used for exponentiation, but only const integer exponents are supported.
 For More information, see [Equations](/docs/equations.md).
##  8. <a name='FlowControlBasics'></a>Flow Control (Basics)
MCPPC supports flow statements similar to java, but with a few syntax differences. An if statement is shown below:
```mcpp
if(condition1) {
  // do something
} 
else if (condition2) {
  // do something
} else {
  // do something
}
```
Just like return, break statements are handled like assignmens and do not alter flow until the end of the loop block:
```mcpp
while(condition){
  if(otherCondition){
    break=true;
  }
  //do more stuff (will be done even in if otherCondition was true)
}
```
A for statement in mcpp looks like below:
```mcpp
for(int counter,min,max,step){
  // do something
}
```
there is also an execute flow statement:
```mcpp
execute as (@r) facing (@p) {
  //do stuff
}
```
For more info, see [Flow Control](/docs/flow.md).
##  9. <a name='ImportingBasics'></a>Importing (Basics)
Other MCPP files can be imported.
```mcpp
import thispack:path/otherfile
```
The public variables and functions from that file can now be used in this file, as below.
```mcpp
otherfile.var = 3;
otherfile.func();
```
Note that the other function will not be executed unless the `run` keyword is added to the import.

For more info, See [Importing](/docs/importing.md).
##  10. <a name='ThreadsBasics'></a>Threads (Basics)
Threads are used to run mcfunction code over multiple ticks, with complicated flow control that can involve delays. It is especially usefull for custom bossfights.

An below example defines and runs a thread:
```mcpp
thread public countdown {
  printf("starting");
  public int count = 5;
} next wait(30) while (count > 0) wait (10) {
  //enter after 30 ticks but loop every 10
  printf("%s",a);
  a = a-1;
} next {
  //enter after 30 ticks but loop every 10
  printf("finished.");
} next stop;

start countdown();
```
For more information, see [Threads](/docs/threads.md)
##  11. <a name='InlineMcfunctionCommandsBasics'></a>Inline Mcfunction Commands (Basics)
Mcpp was built to minimize the need for inline mcfunction code, but it is supported.

Inline mcpp lines can appear as statements (ending with a semicolon), or inside equations (ending with a newline). Either way they must start with a `$/`.
```mcpp
$/say this is an inline mcfunction command;
```
Some mcfunctions are made obselete by Builtin Functions, For example, the `/tp` command:
```mcpp
tp(@s , ~ ~1 ~ , ~ 0);
```
Inline mcfunction commands can also be formatted with Constants.
For more information, See [Inline Mcfunction Commands](/docs/inlinemcf.md)
<!-- give each of these its own more comprehensive page
give some of those pages more subpages for things like specific functions / structs-->

