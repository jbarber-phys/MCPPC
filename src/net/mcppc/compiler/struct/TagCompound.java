package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.List;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.StructTypeParams;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
//internal only, dont register
public class TagCompound extends Struct {
	public static final TagCompound tag = new TagCompound("$tag");
	public static final VarType TAG_COMPOUND = new VarType(tag,new StructTypeParams.Blank() );
	public TagCompound(String name) {
		super(name);
	}

	@Override
	public String getNBTTagType(VarType varType) {
		return "tag_compound";
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
		return variable.getJsonTextBasic();
	}

	@Override
	public int sizeOf(VarType mytype) {
		return 0;
	}

	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}
	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}

	@Override
	public void allocateLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateCompoundLoad(p, var, fillWithDefaultvalue, List.of());
	}

	@Override
	public void allocateCall(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateCompoundCall(p, var, fillWithDefaultvalue, List.of());
	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		return Struct.DEFAULT_COMPOUND;
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
		return false;
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return null;
	}

}
