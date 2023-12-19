# Builtin Functions
<!-- vscode-markdown-toc -->
* 1. [Usage](#Usage)
* 2. [List of Builtin Functions](#ListofBuiltinFunctions)
	* 2.1. [printf() and related](#printfandrelated)
	* 2.2. [titlef(), subtitlef(), actionbarf()](#titlefsubtitlefactionbarf)
	* 2.3. [format() and formatlit()](#formatandformatlit)
	* 2.4. [printnextline()](#printnextline)
	* 2.5. [stopLongMult() and doLongMult()](#stopLongMultanddoLongMult)
	* 2.6. [debug()](#debug)
	* 2.7. [tp()](#tp)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

##  1. <a name='Usage'></a>Usage
Builtin functions are similar to functions but they interop directly with the compiler.
Builtin functions can take unusual exressions as arguments that normal functions would not know how to accept. Most of them are still function like but many also have variable number of arguments. See each functions description for details on what its args are.
##  2. <a name='ListofBuiltinFunctions'></a>List of Builtin Functions

###  2.1. <a name='printfandrelated'></a>printf() and related
the printf function prints formatted text to chat (using `/tellraw`). 

the related functions errorf() and warnf() are identical to printf() excpet for default color (dark_red and gold).

printf() has the following groups of arguments (seperated by commas) in this order:
 - (optional) the reciever entity (defaults to `@s`)
 - the format string
 - the arguments in the format string
 - other json formatting arguments (such as color)

The first arguments are just like any other function, except that format substatements can appear in the string format args (see [formatf()](#formatf-and-formatlitf)).

The json format arguments at the end have a different syntax (similar to kwargs in python): `argname = value`:
```mcpp
printf("format arg = %s", 123,bold=true,color="blue");
```
All of the text json format args that are not subjsons are supported, but one additional name as been added: `run`, and takes the name of an mcpp function (with no parens or args) and will run it on click:
```mcpp
private void onclick(){
    printf("runned onclick.",color="green",font="minecraft:alt");
    //galactic
}
printf("click me!",color="light_purple",run=onclick);
```


###  2.2. <a name='titlefsubtitlefactionbarf'></a>titlef(), subtitlef(), actionbarf()
These are similar to printf() but use the `/title` command instead. They accomadiate 3 additional format arguments for the timing of the command:
 - fadIn: deault 5
 - stay: default 30
 - fadeOut: default 20

###  2.3. <a name='formatandformatlit'></a>format() and formatlit()
These builtinfunctions generate formatted json text elements.

format() can appear as a format arg in other printing functions (printf(), titlef(), etc.) and has the same arguments (except the reciever). It will insert the json text it generates as that format argument.
```mcpp
printf("there is a sub format: %s",format("formatted %s",1234,bold=true),color="light_purple");
```
formatlit() is similar to format() but instead it generates a string literal of the json text that can be inserted into nbt data (for things like lore or book pages).
###  2.4. <a name='printnextline'></a>printnextline()
prints the next line of code in this mcpp file with vscode formatting using `/tellraw`. It has only one optional argument (the reciever, which defaults to `@s`).

This is very cool to see. Highly recommend trying it out.
###  2.5. <a name='stopLongMultanddoLongMult'></a>stopLongMult() and doLongMult())
Determine whether to prohibit [long multiplication](/docs/equations.md#short-and-long-multiplication) in this and nested scopes.

stopLongMult() prevents long multiplication (or re-allows if a `false` is put in as an arg).
stopLongMult() allows long multiplication (or prohibits if a `false` is put in as an arg).

Both override any previous such statements in this scope.

###  2.6. <a name='debug'></a>debug()
Sets the rest of this scope to debug mode (or disables it if a `false` is put in as an arg). This will generate more compile and runtime output.

###  2.7. <a name='tp'></a>tp()
the tp() (or teleport()) function teleports entities. It will use the `/tp` command.

Calls have the arguments: tp(me,destination,rotation,anchor).
 - me: a selector for what to teleoport
 - destination: a position or target entity to teleport to
 - rotation: a rotation, position to look at, or selector to look at
  - anchor: feet or eyes
All but the first argument `me` are optional but if there is only 1 arg then `me` will actually be used as the destination entity.
<!--particles are in a singleton-->

