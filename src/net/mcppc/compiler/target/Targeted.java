package net.mcppc.compiler.target;

/**
 * put this annotation on any function that makes mcfunction code directly 
 * (containing format strings that are printed to mcfunctions);
 * 
 * if a class has a lot of functions that do this (like {@link Variable}), then also put it on the class;
 * String constants that are format strings for insertion into mcfunctions should also be annotated;
 * @author RadiumE13
 *
 */
public @interface Targeted {
	//nothing
}
