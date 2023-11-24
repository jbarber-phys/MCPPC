package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.Arrays;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.VTarget;

/**
 * used internally to store the objective registers during a call within a recursive function;
 * unregistered; do not use this in mcpp code;
 * @author RadiumE13
 *
 */
public class StackStorage extends Struct implements Struct.IInternalOnly {
	public static final VarType SCORETYPE = (Register.SCORE_BITS<=32) ?VarType.INT : VarType.LONG;
	public static final StackStorage stackStorage = new StackStorage("$stackStorage");
	public static final VarType STACKTYPE = new VarType(stackStorage,new StructTypeParams.Blank());
	
	//DO NOT REGISTER ME -- INTERNAL USE ONLY
	public StackStorage(String name) {
		super(name);
	}

	@Override
	public String getNBTTagType(VarType varType) {
		return VarType.Builtin.NBT_LIST;
	}

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return 0;
	}

	@Override
	public String getPrecisionStr(VarType mytype) {
		return "";
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
	public boolean canBeRecursive(VarType type) {
		return true;
	}
	@Override
	public void allocateLoad(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		// TODO Auto-generated method stub
		super.allocateArrayLoad(p, tg, var, fillWithDefaultvalue, 0, SCORETYPE);
	}

	@Override
	public void allocateCall(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateArrayCall(p, tg, var, fillWithDefaultvalue, 0, SCORETYPE);
	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		return Struct.DEFAULT_LIST;
	}

	@Override
	public boolean hasField(Variable self, String name) {
		return false;
	}

	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		throw new CompileError("no fields / methods in internal type StackStorage");
	}

	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		return false;
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		throw new CompileError("no fields / methods in internal type StackStorage");
	}

	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}

	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}

	@Override
	public void getMeDirect(PrintStream p, Scope s, RStack stack, Variable to, Variable me) throws CompileError {
		throw new CompileError("StackStorage is not gettable / settable");
	}

	@Override
	public void setMeDirect(PrintStream p, Scope s, RStack stack, Variable me, Variable from) throws CompileError {
		throw new CompileError("StackStorage is not gettable / settable");
	}

	@Override
	public boolean canIndexMe(Variable self, int i) throws CompileError {
		return true;
	}

	@Override
	public Variable getIndexRef(Variable self, int index) throws CompileError {
		return super.basicTagIndexRef(self, index, SCORETYPE);
	}

	@Override
	public boolean canSetToExpr(ConstExprToken e) {
		return false;
	}
	
	//does not allocate
	//does not affect stack types in any way
	public static int storeStack(PrintStream p,Compiler c,Scope s,RStack stack,Variable store) throws CompileError{
		if(!store.type.isStruct() || !(store.type.struct instanceof StackStorage)) throw new CompileError("store var was not a StackStorage");
		StackStorage thiss=(StackStorage) store.type.struct;
		int size = stack.size();
		if (size==0)return size;
		String[] defaults = new String[size]; Arrays.fill(defaults, SCORETYPE.defaultValue());
		String deflt = "[%s]".formatted(String.join(",", defaults));
		store.setMeToNbtValueBasic(p, s, stack, deflt);
		for(int i=0;i<size;i++) {
			Variable sub = thiss.getIndexRef(store, i);
			sub.setMe(p, s, stack.getRegister(i), SCORETYPE);
		}
		return size;
		
	}
	public static void restoreStack(PrintStream p,Compiler c,Scope s,RStack stack,Variable store,int size) throws CompileError{
		if(!store.type.isStruct() || !(store.type.struct instanceof StackStorage)) throw new CompileError("store var was not a StackStorage");
		StackStorage thiss=(StackStorage) store.type.struct;
		if(size==0)return;
		for(int i=0;i<size;i++) {
			Variable sub = thiss.getIndexRef(store, i);
			sub.getMe(p, s, stack.getRegister(i));
		}
		store.setMeToNbtValueBasic(p, s, stack, Struct.DEFAULT_LIST);
	}
	

}
