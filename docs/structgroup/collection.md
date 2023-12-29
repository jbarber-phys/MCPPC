# Collection Structs
Several structs are for Collection types: sets, lists, etc.
Currently, all of them represent nbt lists and so can reference each other, but this is discouraged as they perform different operations on the list.

<!-- vscode-markdown-toc -->
* 1. [Type arguments: `<type T>`](#Typearguments:typeT)
* 2. [Constructors](#Constructors)
* 3. [Stack](#Stack)
	* 3.1. [Methods](#Methods)
* 4. [Queue](#Queue)
	* 4.1. [Methods](#Methods-1)
* 5. [Staque](#Staque)
	* 5.1. [Methods](#Methods-1)
* 6. [Set](#Set)
	* 6.1. [Lookup Times](#LookupTimes)
	* 6.2. [Methods](#Methods-1)
* 7. [List](#List)
	* 7.1. [Lookup Times](#LookupTimes-1)
	* 7.2. [Methods](#Methods-1)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->
##  1. <a name='Typearguments:typeT'></a>Type arguments: `<type T>`
All collections have 1 type argument for the value type. Elements of a Collection must have the same type (if you want a collection with different typed elements, make it an array of [Obj](/docs/struct/obj.md)'s).
##  2. <a name='Constructors'></a>Constructors
All collection types have a constructor that just takes the elements of the list as arguments (as many as desired). Array literals (with brackets) do not exist yet.
##  3. <a name='Stack'></a>Stack
A stack of nbt values. Elements are added and removed from the "top" of the stack. The top of the stack is also the zeroth element (but it is not indexable).
###  3.1. <a name='Methods'></a>Methods
 - push(T element): pushes `element` onto the stack
 - add(T element): same as push(element)
 - T pop(): removes an element from the stack and returns it
 - int size(): returns the number of elements in the collection
 - clear(): clears the collection
 - bool hasNext(): returns true if there is an element to pop from the collection
##  4. <a name='Queue'></a>Queue
A queue of nbt values. Elements are added at the "bottom" (end) of the queue but are read and removed from the "top" (start) of the stack. The top of the queue is the zeroth element (but it is not indexable).
###  4.1. <a name='Methods-1'></a>Methods
 - enqueue(T element): enques `element` into the queue
 - add(T element): same as enqueue(element)
 - T pop(): removes an element from the queue and returns it
 - int size(): returns the number of elements in the collection
 - clear(): clears the collection
 - bool hasNext(): returns true if there is an element to pop from the collection
##  5. <a name='Staque'></a>Staque
Functions as a stack and queue.
###  5.1. <a name='Methods-1'></a>Methods
 - push(T element): pushes `element` onto the stack
 - enqueue(T element): enques `element` into the queue
 - T pop(): removes an element from the queue/stack and returns it
 - int size(): returns the number of elements in the collection
 - clear(): clears the collection
 - bool hasNext(): returns true if there is an element to pop from the collection
##  6. <a name='Set'></a>Set
A Set datatype. Contains several unordered, unique values.

###  6.1. <a name='LookupTimes'></a>Lookup Times
currently the set is just a "simple list set". There is no hashing or sorting, so the lookup time is linear with size. Any member that is added or read gets moved to the front of the list (so frequently used elements can be looked up faster).

More efficient implementations could be added in the future (and used in `Set`).
###  6.2. <a name='Methods-1'></a>Methods
 - int size(): returns the number of elements in the collection
 - bool isFull(): returns true if the collection is not empty
 - add(T element): adds `element` to the set
 - remove(T element): removes `element` to the set
 - add(T element): adds `element` to the set
 - bool has(T element): returns true if `element` is in the set
 - clear(): clears the collection
##  7. <a name='List'></a>List
An ordered list of non-unique values. Thys type is indexable with square brackets.
###  7.1. <a name='LookupTimes-1'></a>Lookup Times
Before Java 1.20.3, the list lookup time depends on whether the index is constant: if it is, constant time; else it is linear with size.

After Java 1.20.3, macros are used so that the lookup time is always constant.
###  7.2. <a name='Methods-1'></a>Methods
 - append(T element): appends an element to the list
 - prepend(T element): prepends an element to the list
 - int size(): returns the number of elements in the collection
 - clear(): clears the collection
 - get(int/num index): gets the element at the index
 - set(int/num index,T value): sets the value at the index
 - insert(int/num index,T value): inserts the value at the index
 - remove(int/num index): removes the element at the index