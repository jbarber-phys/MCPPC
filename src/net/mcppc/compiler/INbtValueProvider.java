package net.mcppc.compiler;

import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.target.VTarget;

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
	public String fromCMDStatement(VTarget tg);
	public VarType getType();
	

	public default boolean isMacro() {
		return false;
	}
	
	public static class Macro implements INbtValueProvider{
		private static final String MACRO_FORMAT = "$(%s)";
		
		private final String name;
		private final VarType type;
		public Macro(String name,VarType type) {
			this.name=name;
			this.type=type;
		}
		@Override
		public boolean hasData() {
			return true;
		}
		@Override public boolean isMacro() {
			return true;
		}
		@Override
		public String fromCMDStatement(VTarget tg) {
			return VALUE.formatted(MACRO_FORMAT.formatted(this.name));
		}

		@Override
		public VarType getType() {
			return this.type;
		}
		
	}
}
