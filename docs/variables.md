# Variables
<!-- vscode-markdown-toc -->
* 1. [Declaration and Usage](#DeclarationandUsage)
* 2. [Access](#Access)
* 3. [Masks](#Masks)
* 4. [Other Variable Properties](#OtherVariableProperties)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

##  1. <a name='DeclarationandUsage'></a>Declaration and Usage
Variables must be declared with an access, type and name:
```mcpp
private double<3> a;
public int b = 2;
```
An assignment may be optionally added (after masks and estimation).
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
##  2. <a name='Access'></a>Access
Variables (and functions) have two access types: public and private, one of which must begin any Declaration. The accessability differs from the usual code-block dependant rule:
 - Anywhere not inside any function or thread:
    - public variables can be accessed anywhere
    - private variables can be accessed anywhere in this mcpp file (even if they are inside a flow block)
 - Anywhere in a function scope:
    - private variables can be accessed anywhere in this function
    - (public variables cannot be defined)
 - Anywhere in a thread:
    - private variables can be accessed in the top-level block where they are declared
    - public variables can be accessed anywhere in the thread
An example is shown below (with a focus on the conterintuitive part):
```mcpp
if (true) {
    private int a = 0;
}
a = 3; // this is allowed
private int func(int arg) {
    if(true){
        private int b = 0;
        public int b2 = 0; //this is not allowed
    }
    b=1; //this is allowed
}
b = 3; // this is not allowed
thread public athread {
    if(true){
        private int c;
        public int d;
    }
    c=3;//this is allowed
} next {
    c=4; //this is not allowed
    d=4; //this is allowed
}next stop;
```
While MCPP allows this fast and loose access behavior, avoid using it in mcpp code because it might be changed in the future.
##  3. <a name='Masks'></a>Masks
In order to make data manipulation easier, variables can be made to "live" in a certain place in the world data. This makes it easy to set things like entity data, block data, and scores. Basically, variables are an abstraction of data and mcpp lets the user choose what data it abstracts using something called "masks".

By default, variables live in storage of the mcpp file they are in (with some exceptions). For example, if we have a file `file.mcpp` in the source of a data pack `apack`
```mcpp
private int a; // this variable will be stored in: storage apack:file a
a=3; // this will compile to: /data modify storage apack:file a set value 3
```
But we can instead tell the variable where it should live using the mask operator '`->`' during declaration (not later). If the location is nbt data then the holder and nbt path are seperated by a dot to set the mask:
```mcpp
private int b -> anamespace:apath.subtag; // storage anamespace:apath subtag
private short burntime -> ~ ~ ~.BurnTime; // block ~ ~ ~ BurnTime
private float health -> @s.Health; // entity @s Health
```
A variable can also be set to live as a score using a double colon:
```mcpp
private int ascore -> holder::objective;
private int myscore -> @s::objective;
```
Not all types are compadible with scores, but all of the basic ones are (and a few others). Float types will be stored with a multiplier based on its precison. 

A variable can also mask a bossbar but only the Bossbar struct will accept in.

There are a few variable types that have unusual data behavior (Entity types and Random).

Other variables can also be used as masks. This essentially turns the variable into a reference to the other one.
```mcpp
private int a = 0;
private int b -> a;
b = 2;
printf("%s",a);//will print 2
```
Some related types will tolerate sharing a location (like collections).

There is one more way that adding a mask changes a variable. Even if the location was unaffected, adding any mask (except bossbar) stops the variable from setting itself to a default value on load. Normal variables do this to make sure their nbt path exists.

After a mask, an estimate or assignment can also appear.
##  4. <a name='OtherVariableProperties'></a>Other Variable Properties
The keyword `extern` can be added after the access but it has no effect on variables.

The keyword `volatile` can be added beffore the type to mark the variable as volatile. This does nothing outside one of the contexts below.
 - in a recursive function: this variable is not copied when called recursively (see [Recursive Functions](/docs/functions.md/#recursive-functions))
 - in a thread: this variable is not given a seperate copy for each executor (see [Thread Variables](/docs/threads.md/#thread-variables))
