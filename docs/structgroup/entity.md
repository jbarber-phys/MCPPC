<!-- vscode-markdown-toc -->
* 1. [Usage](#Usage)
* 2. [Selector equivalence](#Selectorequivalence)
* 3. [Structs](#Structs)
	* 3.1. [Entity: one entity](#Entity:oneentity)
	* 3.2. [Entities: multiple entities](#Entities:multipleentities)
	* 3.3. [Player: one player](#Player:oneplayer)
	* 3.4. [Players: multiple players](#Players:multipleplayers)
* 4. [Methods](#Methods)
	* 4.1. [summon(entityId,pos position)](#summonentityIdposposition)
	* 4.2. [kill()](#kill)
	* 4.3. [bool exists()](#boolexists)
	* 4.4. [int count()](#intcount)
	* 4.5. [clear()](#clear)
* 5. [Static Methods](#StaticMethods)
	* 5.1. [bool exist(selector s):](#boolexistselectors:)
	* 5.2. [int count(selector s):](#intcountselectors:)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc --># Entity Structs
Entity Structs are a variable type that represent re-assignable entities. They are an abstraction of entity tags.
##  1. <a name='Usage'></a>Usage
Entity variables act kind of like re-assignable selectors. They can be assigned to a selector and will remember what entities it recieved (by tagging them).
```mcpp
Entities nearby = @e[distance=..10];
//now we can do stuff to nearby
```
If a function takes an Entity struct as an argument then it allows it to accept selectors as arguments:
```mcpp
private void lift(Entity e) {
    tp(e,~ ~1 ~);
}
lift(@s);//will lift executor by 1 block
```
##  2. <a name='Selectorequivalence'></a>Selector equivalence
Variables with an entity struct type can appear anywhere that a selector can.
##  3. <a name='Structs'></a>Structs
There are 4 entity structs. They differ only in whether they can be multiple entities, and whether they can only be 
players:
###  3.1. <a name='Entity:oneentity'></a>Entity: one entity
###  3.2. <a name='Entities:multipleentities'></a>Entities: multiple entities
###  3.3. <a name='Player:oneplayer'></a>Player: one player
###  3.4. <a name='Players:multipleplayers'></a>Players: multiple players
##  4. <a name='Methods'></a>Methods
all entity structs have the same methods.
###  4.1. <a name='summonentityIdposposition'></a>summon(entityId,pos position)
summons a new entity into this variable. If the type only allows one, remove any previous entities from this variable.
###  4.2. <a name='kill'></a>kill()
Kills all entities tagged by this variable.
###  4.3. <a name='boolexists'></a>bool exists()
Returns true if at least one entity is tagged by this variable.
###  4.4. <a name='intcount'></a>int count()
Returns the number of entities tagged by this variable.
###  4.5. <a name='clear'></a>clear()
Frees all entities from this variable.
##  5. <a name='StaticMethods'></a>Static Methods
The static methods are identical for all entity structs
###  5.1. <a name='boolexistselectors:'></a>bool exist(selector s):
returns true if there are any entities that meet `s`
###  5.2. <a name='intcountselectors:'></a>int count(selector s):
returns the number of entities that meet `s`