# Getting Started
<!-- vscode-markdown-toc -->
* 1. [Installation](#Installation)
* 2. [Hello World](#HelloWorld)
* 3. [Compiling Examples](#CompilingExamples)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

##  1. <a name='Installation'></a>Installation
Currently, this repository is an eclipse Java Project. No bin-install is present here.

To install for use, create / open the project in eclipse (or other IDE). Compile and run it once with the arg "--std" to compile the compiler and its standard library. The compiler should now be "installed".

There is also a vscode extension for mcpp syntax highlighting at TODO. Download the newest package (EXTENSION.vsix) file and install it with the command
```sh
code --install-extension EXTENSION.vsix
```

##  2. <a name='HelloWorld'></a>Hello World
Decide on a source pack directory (this may or may not be the same as your datapack directory). Open that folder in vscode and add a build task; copy and paste the task in mcppc-build-task.json, adding the needed directories in angle braces.

make a new file:

 ${src}/data/< your namespace >/src/hello_world.mcpp

 Put in the line:
<!--https://stackoverflow.com/questions/75903579/how-to-add-custom-language-syntax-highlighter-to-markdown-code-block-in-vscode-->
<!--highlighting shows up in editor but not in document-->
```mcpp
printf("Hello World!");
```
Then compile the project to the datapack, you will then be able to run the function < your namespace >:hello_world in minecraft (if the output was inside a world folder). It will print "Hello World!" to chat.

Note that mcppc might refuse to compile if there is not a pack.mcmeta file present in the output folder (to prevent accidental deleation of files).

##  3. <a name='CompilingExamples'></a>Compiling Examples
there is a folder,

 ${MCPPC}/examples

with example mcpp code in it. Set this to be the source directory to compile the examples. When in minecraft, reload the datapack to get all important functions in chat.