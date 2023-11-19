package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.FunctionMask;
import net.mcppc.compiler.functions.Particles;
import net.mcppc.compiler.target.VTarget;

/**
 * a singleton struct; by convention, make these lowercase;
 * @author RadiumE13
 *
 */
public class Singleton extends Struct {
	public static void registerAll() {
		Particles.registerAll();
	}
	public static class NoSingletonInstance extends CompileError{
		public NoSingletonInstance(Singleton struct) {
			super("cannot instantiate singleton struct %s".formatted(struct.name));
		}
		
	}
	public Singleton(String name) {
		super(name);
	}

	private Map<String,BuiltinFunction> funcs = new HashMap<String,BuiltinFunction>();
	public BuiltinFunction put(BuiltinFunction bf) {
		return this.funcs.put(bf.name, bf);
	}
	

	@Override
	public boolean hasStaticBuiltinMethod(String name) {
		return super.hasStaticBuiltinMethodBasic(name, this.funcs);
	}
	@Override
	public BuiltinFunction getStaticBuiltinMethod(String name, VarType type) throws CompileError {
		return super.getStaticBuiltinMethodBasic(name, type, funcs);
	}
	
	
	//nonstatic stuff that doesn't exist
	@Override
	public String getNBTTagType(VarType varType) {
		return null;
	}

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		throw new NoSingletonInstance(this);
	}

	@Override
	public String getPrecisionStr(VarType mytype) {
		return null;
	}

	@Override
	public String getJsonTextFor(Variable variable) throws CompileError {
		throw new NoSingletonInstance(this);
	}

	@Override
	public int sizeOf(VarType mytype) {
		return 0;
	}

	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted) throws CompileError {
		throw new NoSingletonInstance(this);
	}

	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new NoSingletonInstance(this);
	}

	@Override
	public void allocateLoad(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		throw new NoSingletonInstance(this);
	}

	@Override
	public void allocateCall(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		throw new NoSingletonInstance(this);
	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		throw new NoSingletonInstance(this);
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
