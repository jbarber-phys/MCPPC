package net.mcppc.compiler.struct;



import net.mcppc.compiler.*;

public abstract class BuiltinStaticStructMethod extends BuiltinFunction {
	public final VarType mytype;
	public BuiltinStaticStructMethod(String name,VarType type) {
		super(name);
		this.mytype=type;
	}


}
