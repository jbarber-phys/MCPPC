# Compound
A Compound represents an compound nbt value. Currently it has no fields, as which keys are present and their types is not known.

<!-- vscode-markdown-toc -->
* 1. [Methods](#Methods)
	* 1.1. [merge(Compound other)](#mergeCompoundother)
	* 1.2. [size()](#size)
* 2. [Operations](#Operations)
	* 2.1. [Compound == Compound](#CompoundCompound)
	* 2.2. [Compound != Compound](#CompoundCompound-1)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

##  1. <a name='Methods'></a>Methods

###  1.1. <a name='mergeCompoundother'></a>merge(Compound other)
Merges the tag `other` into this tag. Returns void.
###  1.2. <a name='size'></a>size()
Returns the number of keys in this nbt tag.

##  2. <a name='Operations'></a>Operations

###  2.1. <a name='CompoundCompound'></a>Compound == Compound
Returns true if the tags are the same.
###  2.2. <a name='CompoundCompound-1'></a>Compound != Compound
Returns true if the tags are different.
