package net.mcppc.compiler.struct;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.VarType;

public abstract class BuiltinConstructor  extends BuiltinFunction{
	public final VarType mytype;
	public BuiltinConstructor(String name,VarType mytype) {
		super(name);
		this.mytype=mytype;
	}

}
