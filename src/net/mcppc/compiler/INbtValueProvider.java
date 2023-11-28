package net.mcppc.compiler;

import java.io.PrintStream;

import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;
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
	public boolean hasData ();
	//super Variable and some ConstExprs
	//data phrase:
	// from {...}
	@Targeted static final String FROM = "from %s";
	// value ###
	@Targeted static final String VALUE = "value %s";
	
	@Targeted static final String STRING = "string %s%s%s";
	public String fromCMDStatement(VTarget tg);
	public VarType getType();
	

	public default boolean hasMacro() {
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
		@Override public boolean hasMacro() {
			return true;
		}
		@Override
		public String fromCMDStatement(VTarget tg) {
			return VALUE.formatted(MACRO_FORMAT.formatted(this.name));
		}
		public String getMacroString() {
			return MACRO_FORMAT.formatted(this.name);
		}

		@Override
		public VarType getType() {
			return this.type;
		}
		
	}
	
	public static class SubstringProvider implements INbtValueProvider{
		private static final String MACRO_FORMAT = "$(%s)";
		
		private final String data;
		private final VarType type;
		private final Object start;
		private final Object end;
		private final boolean hasMacro;
		public SubstringProvider(Variable var) {
			this.data=var.dataPhrase();
			this.type=var.type;
			this.start=null;
			this.end=null;
			this.hasMacro=false;
		}
		public SubstringProvider(Variable var,Integer start,Integer end) {
			this.data=var.dataPhrase();
			this.type=var.type;
			this.start=start;
			this.end=end;
			this.hasMacro=false;
		}
		public SubstringProvider(Variable var,Macro start,Macro end) {
			this.data=var.dataPhrase();
			this.type=var.type;
			this.start=start.getMacroString();
			this.end=end.getMacroString();
			this.hasMacro=true;
		}
		@Override
		public boolean hasData() {
			return true;
		}
		@Override public boolean hasMacro() {
			return this.hasMacro;
		}
		@Override
		public String fromCMDStatement(VTarget tg) {
			String s1 = this.start==null? "": " %s".formatted(this.start);
			String s2 = this.end==null? "": " %s".formatted(this.end);
			return STRING.formatted(this.data,s1,s2);
		}

		@Override
		public VarType getType() {
			return this.type;
		}
		
	}
}
