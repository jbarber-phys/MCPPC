# Map Structs
Map structs implement map data structures. There is currently only one: Map, but more could exist in the future so Map gets an entire group.
<!-- vscode-markdown-toc -->
* 1. [Type arguments: `<type K,type V>`](#Typearguments:typeKtypeV)
* 2. [Map](#Map)
	* 2.1. [Constructor Map<K,V>(K key,V value, ...)](#ConstructorMapKVKkeyVvalue...)
	* 2.2. [Indexing](#Indexing)
	* 2.3. [Methods](#Methods)
		* 2.3.1. [int size()](#intsize)
		* 2.3.2. [clear()](#clear)
		* 2.3.3. [put(K key,V value)](#putKkeyVvalue)
		* 2.3.4. [putIfAbsent(K key,V value)](#putIfAbsentKkeyVvalue)
		* 2.3.5. [putIfPresent(K key,V value)](#putIfPresentKkeyVvalue)
		* 2.3.6. [remove(K key)](#removeKkey)
		* 2.3.7. [V get(K key)](#VgetKkey)
		* 2.3.8. [bool has(K key)](#boolhasKkey)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->
##  1. <a name='Typearguments:typeKtypeV'></a>Type arguments: `<type K,type V>`
Two type arguments are required for the key and value types respectively.
##  2. <a name='Map'></a>Map
The current implementation of Map is a "simple list map". It is stored in nbt as a list of key-value pairs. Operations move queried entries to the top. This is just like the current [Set](/docs/structgroup/collection.md#6-set) implementation. This means that it has linear lookup time.

###  2.1. <a name='ConstructorMapKVKkeyVvalue...'></a>Constructor Map<K,V>(K key,V value, ...)
The constructor for a map contains key and then value argument pairs. There can be as many as desired.

###  2.2. <a name='Indexing'></a>Indexing
A key value can be used as an index to a map. This can be used to put and get entries from the map.
###  2.3. <a name='Methods'></a>Methods
####  2.3.1. <a name='intsize'></a>int size()
Returns the number of entries in the map.
####  2.3.2. <a name='clear'></a>clear()
Clears all entries from the map.
####  2.3.3. <a name='putKkeyVvalue'></a>put(K key,V value)
Puts an entry into the map, overwriting a previous entry if there was one.
####  2.3.4. <a name='putIfAbsentKkeyVvalue'></a>putIfAbsent(K key,V value)
Puts an entry into the map but only if there was no entry present.
####  2.3.5. <a name='putIfPresentKkeyVvalue'></a>putIfPresent(K key,V value)
Puts an entry into the map but only if there is an entry with the key already.
####  2.3.6. <a name='removeKkey'></a>remove(K key)
Removes the entry with key from the map if it exists.
####  2.3.7. <a name='VgetKkey'></a>V get(K key)
Returns the value with the key.
####  2.3.8. <a name='boolhasKkey'></a>bool has(K key)
Returns whether there is an entry with the key.