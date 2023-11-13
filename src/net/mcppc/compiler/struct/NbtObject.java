package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.List;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;

public class NbtObject extends Struct {
	public static final NbtObject obj = new NbtObject("Obj");
	
	public NbtObject(String name) {
		super(name);
	}

	public static final String VALUE = "value";
	public Variable getValue(Variable self,VarType type) {
		return self.fieldMyNBTPath(VALUE, type);
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return "tag_compound";
	}
	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		return Struct.DEFAULT_COMPOUND;
	}

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return 0;
	}

	@Override
	public String getPrecisionStr(VarType mytype) {
		return null;
	}

	@Override
	public String getJsonTextFor(Variable variable) throws CompileError {
		//TODO get value
		return variable.getJsonTextBasic();
	}

	@Override
	public int sizeOf(VarType mytype) {
		return 0;//size is not known at compile time, so cannot stack
	}

	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}

	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}
	//TODO direct set and ops
	@Override
	public void allocateLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateCompoundLoad(p, var, fillWithDefaultvalue, List.of());
	}

	@Override
	public void allocateCall(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateCompoundCall(p, var, fillWithDefaultvalue, List.of());
	}


	@Override
	public boolean hasField(Variable self, String name) {
		return false;
	}

	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}

	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		//TODO isnull method
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		// TODO Auto-generated method stub
		return null;
	}

}
