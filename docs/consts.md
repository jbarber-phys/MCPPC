# Constants
<!-- vscode-markdown-toc -->
* 1. [Declaration](#Declaration)
* 2. [Const Types](#ConstTypes)
* 3. [Templates](#Templates)
* 4. [Formatted Mcfunction Commands](#FormattedMcfunctionCommands)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc --><!-- vscode-markdown-toc -->
##  1. <a name='Declaration'></a>Declaration
Consts are declared similar to variables but with the `const` keyword and the const type:
```mcpp
private const num PI = 3.1415;
```
##  2. <a name='ConstTypes'></a>Const Types
Consts have their own type system; all of the supported types are below.
 - num : a const number
 - flag: a const bool
 - text: a const string
 - selector: a target selector
 - nbt: a nbt tag or path
 - coords: world cordinates, which may be in caret or tilde notation
 - rot: similar to coords but for rotations
##  3. <a name='Templates'></a>Templates
Functions can contain templates with const arguments. They act similar to arguments but are const valued. For each argument, a default value or range must be specified.
```mcpp
public <num p=0..6> double<p> someMathFunction(double<p> x){
    //return something
}
private double<4> a = someMathFunc<4>(0.1234);
```
The function cannot be called with template arguments outside the range. If aditional values are needed, then an `export` statement can be used.
```mcpp
public <num p=0..6> double<p> someMathFunction(double<p> x)
    export <8> export <-3>
{
    //return something
}
private double<8> a = someMathFunc<8>(0.12345678);
```
Template parameters can appear as consts or in type arguments.
##  4. <a name='FormattedMcfunctionCommands'></a>Formatted Mcfunction Commands
Consts, or variables that are reducable to consts can appear in [formatted inline mcfunction commands](/docs/inlinemcf.md#formatting-mcfunctions-with-constants). 