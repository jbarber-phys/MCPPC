# Functions
<!-- vscode-markdown-toc -->
* 1. [Declaring Functions](#DeclaringFunctions)
* 2. [Function Access](#FunctionAccess)
* 3. [Args and Local Variables](#ArgsandLocalVariables)
* 4. [Recursive Functions](#RecursiveFunctions)
* 5. [Extern Functions](#ExternFunctions)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->



##  1. <a name='DeclaringFunctions'></a>Declaring Functions
Functions can be created in mcpp like so; they are declared in a way similar to java/c++/c#:
```mcpp
private int afunction(int arg1, bool arg2){
  // your code here
  return = arg1 + 3;
}
```
Return statements do not break out of the function, instead acting similar to variable assignment (like Fortran). True returns (that also end execution) do not yet exist.

arguments normally pass by value. This can be changed by adding the `ref` keyword before the argument:
```mcpp
private void increment(ref int a){
  a = a + 1;
}
```
Note that the variable reference is currently not actually given to the function, but instead it will be back-copied after the function call.

Functions can also have template arguments (which are const valued). See [Templates](/docs/consts.md#templates) for details.

There are a few other things a function declaration could have (if you look at the standard library), but some of those are for internal use only (referenced by structs).

A location for the function can be specified with a resourcelocation and a subname. If this is done then the definition of the function's code becomes optional. This can be used to import / export functions to another mcpp datapack that you do not have the include files for:
```mcpp
private void otherfunction() -> otherpack:path.subfuncname;
```
The actual file it writes to will be some combination of the resourcelocation and the subname.
##  2. <a name='FunctionAccess'></a>Function Access
Functions can only be declared in file-global scope. They otherwise behave as you would expect: public functions can be called from other files (if [imported](/docs/importing.md)) and private ones can't.
##  3. <a name='ArgsandLocalVariables'></a>Args and Local Variables
The arguments of a function can only be accessed inside it. The arguments will be stored in storage, all inside a subtag with the function name.

Any variables declared inside a function must be labeled private and can be accessed anywhere inside the function (even outside the defining block) but never outside.
##  4. <a name='RecursiveFunctions'></a>Recursive Functions
Any function that calls itself recursively must declare that it will do so in its declaration. This is done by adding the keyword `recursive` after the access (or after `extern`).
```mcpp
private recursive int factorial(int n){
    //locals will work just fine
    if(n <=1) {return = 1;}
    else {return n*factorial(n-1);}
}
```
Normally copies of locals will be made for each call to make sure they don't collide, but this can be prevented by using the keyword `volatile` when declaring the local variable:
```mcpp
private recursive void recurr(bool inside){
    private int local = 0;
    private volatile int localVolatile = 0;
    if(inside) {
        local = 1;
        localVolatile = 1;
    }
    if(!inside) {
        recurr(true);
        printf("local = %s",local);// local = 0
        printf("localVolatile = %s",local);// localVolatile = 1
    }
    if(inside) {
    }
}
recurr(false);
```
All layers of the call stack will share the same value of a volatile variable.
##  5. <a name='ExternFunctions'></a>Extern Functions
MCPP has its own system for passing arguments and return values in and out of functions (it uses storage). Sometime around Java 1.20.2, macros and the `/return` command were added. This means that if an mcpp function is supposed to be called by a raw mcfunction datapack (or vice versa), then that other datapack might need the function to take macro arguments and return using `/return`. In this case, the function must be labeled with the keyword `extern` to tell mcppc to use minecraft's new return/arg system instead of mcpp's. It also requires that a resourcelocation be given to tell the function where to write itself or call itself (if exporting, subfunctions may be created with locations start with the given resourcelocation).
```mcpp
public int importedExtern(int arg) -> otherpack:a_mcfunction;
public int exportedExtern(int arg) -> thispack:a_mcfunction {
    //do something
    return = arg + 2;
};
```
On older versions, return values and or args may not be supported but argless `void` externs will always work.