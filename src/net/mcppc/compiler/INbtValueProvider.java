package net.mcppc.compiler;
/**
 * hook for assigning nbt data
 * used for basic comparisons
 * @author jbarb_t8a3esk
 *
 */
public interface INbtValueProvider {
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
