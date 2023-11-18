package net.mcppc.compiler;

import net.mcppc.compiler.target.Targeted;

/**
 * interface for something that can give an nbt value;
 * also caries information about whether to get it using from or value
 * can be a const nbt tag (which uses the value prefix),
 * or could be a variable (which would use the from prefix)
 * @author RadiumE13
 *
 */
public interface INbtValueProvider {
	//TODO incorperate data ... string ... somehow
	public boolean hasData ();
	//super Variable and some ConstExprs
	//data phrase:
	// from {...}
	@Targeted static final String FROM = "from %s";
	// value ###
	@Targeted static final String VALUE = "value %s";
	public String fromCMDStatement();
	public VarType getType();
}
