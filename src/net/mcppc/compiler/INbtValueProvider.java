package net.mcppc.compiler;
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
	static final String FROM = "from %s";
	// value ###
	static final String VALUE = "value %s";
	public String fromCMDStatement();
	public VarType getType();
}
