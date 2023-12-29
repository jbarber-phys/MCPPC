# Vector Structs
All of the vector structs represent 3d vectors in space.
<!-- vscode-markdown-toc -->
* 1. [Component type `T`](#ComponenttypeT)
* 2. [Constructor `(T x,T y, T z)`](#ConstructorTxTyTz)
* 3. [Fields](#Fields)
	* 3.1. [T x,y,z](#Txyz)
* 4. [Operations](#Operations)
	* 4.1. [Vector + Vector](#VectorVector)
	* 4.2. [Vector - Vector](#Vector-Vector)
	* 4.3. [Vector * Vector](#VectorVector-1)
	* 4.4. [Vector * Scalar (commutative)](#VectorScalarcommutative)
	* 4.5. [Vector / Scalar](#VectorScalar)
	* 4.6. [Vector % Vector](#VectorVector-1)
* 5. [`Vector<type T>`](#VectortypeT)
* 6. [`Vec3i`](#Vec3i)
	* 6.1. [Methods](#Methods)
		* 6.1.1. [`int sqrMag()`](#intsqrMag)
* 7. [`Vec3d<num p = 3>`](#Vec3dnump3)
	* 7.1. [Methods](#Methods-1)
		* 7.1.1. [`Vec3d<p> norm()`](#Vec3dpnorm)
		* 7.1.2. [`double<p> mag()`](#doublepmag)
		* 7.1.3. [`double<p> sqrMag()`](#doublepsqrMag)
	* 7.2. [Static Methods](#StaticMethods)
		* 7.2.1. [`Vec3d<p> lookAt(Entity from,Entity to,int pow (optional))`](#Vec3dplookAtEntityfromEntitytointpowoptional)
		* 7.2.2. [`Vec3d<p> looking(Entity me (optional))`](#Vec3dplookingEntitymeoptional)

<!-- vscode-markdown-toc-config
	numbering=true
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

##  1. <a name='ComponenttypeT'></a>Component type `T`
All vector structs will have a numeric component type `T`, given by the type arguments.
##  2. <a name='ConstructorTxTyTz'></a>Constructor `(T x,T y, T z)`
All vector structs have a constructor that takes 3 arguments, 1 for each component.


##  3. <a name='Fields'></a>Fields
###  3.1. <a name='Txyz'></a>T x,y,z
 the components of the vector. (Vectors are not indexable)

##  4. <a name='Operations'></a>Operations

###  4.1. <a name='VectorVector'></a>Vector + Vector
Adds the two vectors.
###  4.2. <a name='Vector-Vector'></a>Vector - Vector
Subtracts the two vectors.
###  4.3. <a name='VectorVector-1'></a>Vector * Vector
Takes the dot product of two vectors.
###  4.4. <a name='VectorScalarcommutative'></a>Vector * Scalar (commutative)
Scales the vector by a scalar coeficient. `Scalar` can be any numeric type.
###  4.5. <a name='VectorScalar'></a>Vector / Scalar
Divides the vector by a scalar coeficient. `Scalar` can be any numeric type.
###  4.6. <a name='VectorVector-1'></a>Vector % Vector
Takes the cross product of two vectors. 

The cross product obeys the right hand rule. Minecraft's coordinate system is right handed (unlike most other games).
##  5. <a name='VectortypeT'></a>`Vector<type T>`
Base vector struct with components of type `T`. This should be avoided as it is missing all methods.
##  6. <a name='Vec3i'></a>`Vec3i`
similar to `Vector<int>`, but with methods.
###  6.1. <a name='Methods'></a>Methods
####  6.1.1. <a name='intsqrMag'></a>`int sqrMag()`
Returns the magnitude squared of this vector as an `int`.
##  7. <a name='Vec3dnump3'></a>`Vec3d<num p = 3>`
similar to `Vector<double<p>>`, but with methods.

###  7.1. <a name='Methods-1'></a>Methods
####  7.1.1. <a name='Vec3dpnorm'></a>`Vec3d<p> norm()`
Returns this vector but normalized.
####  7.1.2. <a name='doublepmag'></a>`double<p> mag()`
Returns the magnitude of this vector.
####  7.1.3. <a name='doublepsqrMag'></a>`double<p> sqrMag()`
Returns the magnitude squared of this vector.

###  7.2. <a name='StaticMethods'></a>Static Methods

####  7.2.1. <a name='Vec3dplookAtEntityfromEntitytointpowoptional'></a>`Vec3d<p> lookAt(Entity from,Entity to,int pow (optional))`
Returns the displacement vector between two entities, with its magnitude raised to some power `pow` (default 1).

####  7.2.2. <a name='Vec3dplookingEntitymeoptional'></a>`Vec3d<p> looking(Entity me (optional))`
returns the forward unit vector of the entity `me` (default `@s`).