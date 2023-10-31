package net.mcppc.compiler;
/**
 * defines order of operations
 * 
 * largely based on C:
 * https://en.wikipedia.org/wiki/Order_of_operations
 * 
 * but there are differences:
 * there is no comma operator
 * there are no assignment operators allowed in equations
 * unary not comes after all comparisons
 * xor exists and is between and and or 
 * 
 * for now, extra op orders have been inserted for the possibility of custom operators
 * 
 * @author RadiumE13
 *
 */
public enum OperationOrder{
	//ordinal ordered, starts at zero
	NONE,
	PARENS,
	MEMBER,
	CAST,
	EXP,
	CUSTOMEXP,
	UNARYMINUS,
	MULT,
	CUSTOMMULT,
	ADD,
	CUSTOMADD,
	COMPARE,
	UNARYNOT,
	AND,
	XOR,
	OR,
	CUSTOMLOGICAL,//custom ops are last
	ALL;
	//is already comparable to

}
