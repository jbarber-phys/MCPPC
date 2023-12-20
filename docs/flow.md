# Flow Control
<!-- vscode-markdown-toc -->
* 1. [If /  Else](#IfElse)
* 2. [While Loop](#WhileLoop)
* 3. [For Loop](#ForLoop)
* 4. [Breaking](#Breaking)
* 5. [Switch / Case](#SwitchCase)
* 6. [Execute](#Execute)
* 7. [Target](#Target)
* 8. [Tag](#Tag)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

##  1. <a name='IfElse'></a>If /  Else
These work exactly like in java, but usage of `elif` is supported in addition to `if else`.
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
##  2. <a name='WhileLoop'></a>While Loop
while works similar to as in java but a do-while statement is not supported.
```mcpp
while(condition){
  if(otherCondition){
    break=true;
  }
  //do more stuff (will be done even in if otherCondition was true)
}
```
##  3. <a name='ForLoop'></a>For Loop
A for statement in mcpp looks different than in java.
```mcpp
for(int counter,min,max,step){
  // do something
}
```
An external counter variable may also be used
```mcpp
int ocounter;
for(ocounter,min,max,step){
  // do something
}
```
step is optional (defaults to 1). Range is inclusive.
For loops can also be done over collections types as in java:
```mcpp
private List<int> alist;
for(int a: alist){
    //do something
}
```
By default, `a` is treated as a reference. If this is not desired, then the `final` keyword should be added to prevent back-copying.
```mcpp
private List<int> alist;
for(int a: final alist){
    //do something
}
```
Maps can also be iterated:
```mcpp
private Map<int,int> map;
for (int key,int val : map) {
    //do something
}
```
##  4. <a name='Breaking'></a>Breaking
<!--mention depth-->
some flow statements (while, for, case), support breaking. This is treated as a variable assignment similar to returns.
```mcpp
while (true){
    if(a == b){
        break = true; //will break after this loop
    }
    //do stuff
}
```
If there are multiple nested loops, the loop is chosen by adding parens with an int inside, specifying the depth (defaults to zero, minimal depth):
```mcpp
while(true){ //Loop A
    while (true){ //Loop B
        if(a == b){
            break = true;
            break(0) = true;
            //both of these break from loop B
        }else if (a > b){
            break(1) = true;//breaks from loop A
        }
    }
}
```
##  5. <a name='SwitchCase'></a>Switch / Case
Switch/case statements have the following syntax:
```mcpp
switch(value)
case(v1) {
    //do something
    break=true;//break
} 
case(v2,v3) { //either case
    //do somethign
    //do not break, continue to default
}
default {
    //do something
    break=true;
}
```
##  6. <a name='Execute'></a>Execute
there is a statement for execute. The syntax consists of several sub-executes with the argument in parens.
```mcpp
execute as (@r) facing (@p) {
  //do stuff
}
```
In addition to the usual executes, there is also the `asat` subexecute, which will execute as and at the target:
```mcpp
execute asat(@r){} // becomes /execute as @r at @s
```
The entity qualifier is never needed, it is infered from args.
```mcpp
execute facing(@r){} 
// becomes /execute facing entity @r
```
##  7. <a name='Target'></a>Target
Conditional compilation based on target pack version. A version range is given as ints seperated by a `..` operator.
```mcpp
target 20.. {
    //do something that requires new mcfunction stuff
} else target 15..19 {
    //an older alternative
} else target default {
    //for even older versions
}
```
##  8. <a name='Tag'></a>Tag
Tag statements add either a block of code or the whole file to a datapack tag:
```mcpp
tag mypack:mytag; //adds this mcfunction file to the tag
tag mypack:myblocktag {
    //this code block is a tagged mcfunction
}
```
There is a simpilfied syntax for the two special tags: `minecraft:tick` and `minecraft:load`:
```mcpp
load {
    //this will run on load
}
tick {
    //this will run every tick
}
```