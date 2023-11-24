package net.mcppc.compiler;
/**
 * defines order of operations;
 * 
 * largely based on C with small differences:
 * {@link https://en.wikipedia.org/wiki/Order_of_operations}<br>
 * for now, extra op orders have been inserted for the possibility of custom operators;
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
	CUSTOMLOGICAL,//custom ops are last compared to their normal analog
	ALL;
	//is already comparable to

}
