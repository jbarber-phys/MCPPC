# Equations
<!-- vscode-markdown-toc -->
* 1. [Structure](#Structure)
* 2. [Evaluation Order](#EvaluationOrder)
* 3. [Estimation](#Estimation)
* 4. [Short and Long Multiplication](#ShortandLongMultiplication)
* 5. [Integer Overflow](#IntegerOverflow)
* 6. [Const and Reference Nature](#ConstandReferenceNature)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

##  1. <a name='Structure'></a>Structure
Equations can appear as function arguments or after an assignment operator.

They are structured similarly to java but with the following differences:
 - assignments cannot appear in equations
 - there are no short circuiting operators
 - the `^` operator is exponentiation, but it only supports const int exponents up to 32 (to prevent overflow)
 - there is no ternary operator
 - there are no increment / multiply operators (`++`,`+=`, `*=`
 - logical operands are never bitwise
 - the xor operator is `^!&` (OR and NOT AND)
Most math is numeric, but some other operations (like equality testing of strings) can also be done.

All operations are done on the scoreboard so only 32 bits of figures will be kept. All arithmatic is also fixed point.

Several math function are available in the standard library in [mcppc:math](/docs/stdlib.md#1-math)
##  2. <a name='EvaluationOrder'></a>Evaluation Order
Terms in an equation are always evaluated / runned in the order they appear in code.
```mcpp
a = f(1) + g(2); // f will run before g
```
##  3. <a name='Estimation'></a>Estimation
variables can be given estimates using the `~~` operator.
```mcpp
float angle ~~ 300;
```
This gives the compiler information it can use to optimize operations. The compiler might also use it to issue warnings about math overflow problems. In particular, it will be used to determine whether [long multiplication](#short-and-long-multiplication) will be done.
##  4. <a name='ShortandLongMultiplication'></a>Short and Long Multiplication
MCPPC will choose one of two ways to perfform multiplication: short or long multiplication. The decision will be made based on the estimates of both variables, their precision, and whether the user put a `stopLongMult()` statement in the scope.

Short multiplication just does multiplication on the scoreboard at fixed point precision.

Long multiplication is used if the compiler thinks (or cant rule out) integer overflow that happens during multiplucation. Multiplying two numbers with a large precision will temporarily produce lots of unnedded figures and that can result in unnedded overflow, so long multiplication stops this. It performs several scoreboard operations to do this.

Long multiplication can be prevented by adding a `stopLongMult()` statement before the operations:
```mcpp
stopLongMult();
a = b * c; // no long multiplication
```
##  5. <a name='IntegerOverflow'></a>Integer Overflow
Even though some nbt types (double, long) are 64 bit, all scores are limited to 32 bits and so equations that operate on long / double values may still overflow at a 32 bit value.
##  6. <a name='ConstandReferenceNature'></a>Const and Reference Nature
Equations that have no operations but just a variable can be interpereted as a reference. Some functions will require that an argument be a reference.

There are also some [builtin functions](/docs/builtinfunctions.md) that require an argument to be evaluatable as a const. This means that it can contain only const values and operations that can be performed at compile time.