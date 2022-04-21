package net.mcppc.compiler.struct;


import net.mcppc.compiler.*;

public abstract class BuiltinStructMethod extends BuiltinFunction {
	public final Variable self;
	public BuiltinStructMethod(String name,Variable self) {
		super(name);
		this.self=self;
	}
}
