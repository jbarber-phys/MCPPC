# Importing
Importing allows mcpp code to use functions and variables from another file.
Other MCPP files can be imported.
```mcpp
import thispack:path/otherfile;
```
The public variables and functions from that file can now be used in this file, as below.
```mcpp
otherfile.var = 3;
otherfile.func();
```
Note that the other function will not be executed unless the `run` keyword is added to the import (see [import run](#import-run)).
## Import As
By default, the name of the imported file will just be the last name in the path. If we want to choose a different name, we can use the mask operator (`->`) after the import to do so:
```mcpp
import thispack:path/otherfile -> ot;
ot.var = 3;
```
## Import Run
Importing a file will not run it unless the `run` keyword is added to the import:
```mcpp
import run thispack:path/otherfile; // run thispack:path/otherfile
```
## Import If
When using a precompiled library (such as the [standard library](/docs/stdlib.md)), all the contents of the imported library will be copied into the datapack. This often includes a lot of unnececary files (such as the many copies of each function from templates). To prevent unnececary copying, add the `if` keyword to the import:
```mcpp
import if mcppc:math -> m;
private double a = m.sqrt<3>(3);
//will not copy exp, ln, ...
```
This will prevent unnececary clutter in the resulting datapack folder.