package net.mcppc.compiler.struct;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.VarType;

public abstract class BuiltinConstructor  extends BuiltinFunction{
	
	//public final VarType mytype; //this is now in the token
	public BuiltinConstructor(String name) {
		super(name);
		//this.mytype=mytype;
	}
	@Override
	public VarType getRetType(BFCallToken token) {
		return token.getStaticType();
	}
	

}
