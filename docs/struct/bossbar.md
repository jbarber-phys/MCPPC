# Bossbar
An instance of Bossbar represents an absraction of a bossbar. It has an optional type argument for it's precision (default 0).

If defined with a mask, the syntax uses the `---` operator like below:
```mcpp
public Bossbar<3> healthbar -> bossbarId --- "Boss Name"; // the name can be changed later
```
<!-- vscode-markdown-toc -->
* 1. [Fields](#Fields)
	* 1.1. [max](#max)
* 2. [Methods](#Methods)
	* 2.1. [color(c)](#colorc)
	* 2.2. [setPlayers(selector s)](#setPlayersselectors)
	* 2.3. [show()](#show)
	* 2.4. [hide()](#hide)
	* 2.5. [style(st)](#stylest)
	* 2.6. [setName(name):](#setNamename:)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->
##  1. <a name='Fields'></a>Fields
###  1.1. <a name='max'></a>max
The bossbar's maximum value.
##  2. <a name='Methods'></a>Methods
###  2.1. <a name='colorc'></a>color(c)
Set the bossbar color to `c`, where `c` is just the name of a bossbar color.
###  2.2. <a name='setPlayersselectors'></a>setPlayers(selector s)
Sets which players can see the bossbar to be `s`.
###  2.3. <a name='show'></a>show()
Shows the bossbar.
###  2.4. <a name='hide'></a>hide()
 Hides the bossbar.
###  2.5. <a name='stylest'></a>style(st)
Sets the bossbar style to `st`, where `st` is the name of a bossbar style.
###  2.6. <a name='setNamename:'></a>setName(name):
Sets the name of the bossbar to `name`, which can be a text or formatted json text.