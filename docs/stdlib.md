# Standard Library
The standard library is located in the namespace `mcppc` and is always included in an mcpp prject. Its files and their functions are listed below.
<!-- vscode-markdown-toc -->
* 1. [Math](#Math)
* 2. [Armory](#Armory)
* 3. [All others](#Allothers)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->


##  1. <a name='Math'></a>Math
This library contains several basic math functions:
 - sqrt(x)
 - exp(x)
 - ln(x)
 - sin(x)
 - cos(x)
 - atan2(x,y)
 - hypot(x,y)
 - hypot3(x,y,z)

All of these take doubles as args and return doubles, both with a precision set by the template argument.
```mcpp
import if mcppc:math;
private double<4> a = math.sqrt<4>(3.0000);
```
This library also contains a basic implementation of a linear congruital [random0](https://en.wikipedia.org/wiki/Linear_congruential_generator#Parameters_in_common_use) generator:
 - int random0(int max) : gets the next int up to the max (exclusive)
 - `<num n= 1 .. 6 >` double`<n>` random0d(double`<n>` max) : gets the next double up to the max (exclusive)
 - void seed0(int seed) : sets the seed of the generator
 - void reset0() : randomizes the generator

<!--might be internal only-->
##  2. <a name='Armory'></a>Armory
Functions for damaging entities before the `/damage` command was added:
 - `<num n = 1 .. 6>` Entity shootForward(Entity target,double`<n>` damage,double`<n>` speed) : shoots an arow towards the target entity (from `@s`)
 - `<num n = 1 .. 6>` Entity attack(Entity target,double`<n>` damage) : deals damage to the target entity as `@s` (knocking them away)

##  3. <a name='Allothers'></a>All others
All of the other files (including `mcppc:vecmath`) are internal use only. They either supply structs with functions to call or are just there to test the standard library.


