# Obj
<!-- vscode-markdown-toc -->
* 1. [Usage](#Usage)
* 2. [Operations](#Operations)
	* 2.1. [Obj == Obj](#ObjObj)
	* 2.2. [Obj != Obj](#ObjObj-1)
	* 2.3. [Obj == Any](#ObjAny)
	* 2.4. [Obj != Any](#ObjAny-1)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->
##  1. <a name='Usage'></a>Usage
This struct is a polymorphic type (similar to Object in java). It can hold any type of value that is equivalent to an nbt data type. Thus it can be assigned any type of value:
```mcpp
private Obj a;
a= "Hello World" ;// this is OK
a = 3; // this is also OK
```
It can also be assigned a null value:
```mcpp
a = null;
```
We can also use the value in equations. In some situations, a cast might be required.
```mcpp
private int i = a;
private bool b = (int)a == 3;
```
 If this doesn't work, try adding a cast.

##  2. <a name='Operations'></a>Operations
The Obj supports any binary operation with another nbt variable type as long as that operation is allowed if the Obj was the same type as the other operand. The following additional operations are supported:
###  2.1. <a name='ObjObj'></a>Obj == Obj
Returns true if the values are the same. If one is null, then return false unless the other value is also null.
###  2.2. <a name='ObjObj-1'></a>Obj != Obj
Oposite of `Obj == Obj`.
###  2.3. <a name='ObjAny'></a>Obj == Any
Performs the operation `Any == Any` (if allowed) with the Obj value. If the `Obj` is null, then return false instead.
###  2.4. <a name='ObjAny-1'></a>Obj != Any
Oposite of `Obj == Any`.