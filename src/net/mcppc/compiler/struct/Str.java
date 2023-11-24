package net.mcppc.compiler.struct;
import net.mcppc.compiler.struct.*;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Token;

import java.io.PrintStream;
import java.util.Map;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.VarType.Builtin;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.Size;
/**
 * struct type for a string;
 * @author RadiumE13
 *
 */
/*
 * string tags cannot be appended / inserted ...
 * TODO see if strings ... can get substring
 */
public class Str extends Struct{
	public static final Str string;
	static {
		string=new Str("String");
	}

	public static final VarType STR = new VarType(string,new StructTypeParams.Blank());
	public static void registerAll() {
		final Size size = new Size("size");
		final Size isFull = new Size.IsFull("isFull");
		string.METHODS = Map.of(size.name,size,isFull.name,isFull);
		Struct.register(string);
	}
	public Str(String name) {
		super(name);
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return VarType.Builtin.NBT_STRING;
	}
	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return 0;
	}

	@Override
	public String getPrecisionStr(VarType mytype)  {
		return "";
	}
	@Override
	public String getJsonTextFor(Variable variable) throws CompileError {
		return variable.getJsonTextBasic();
	}
	@Override
	public int sizeOf(VarType mytype) {
		return -1;//cannot stack
	}
	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}
	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}

	@Override public boolean canBeRecursive(VarType type) {
		return true;
	}
	@Override
	public void allocateLoad(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		var.allocateLoadBasic(p, fillWithDefaultvalue, Struct.DEFAULT_STRING);
	}
	@Override
	public void allocateCall(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		var.allocateCallBasic(p, tg, fillWithDefaultvalue, Struct.DEFAULT_STRING);
	}
	@Override
	public String getDefaultValue(VarType var) {
		return Struct.DEFAULT_STRING;
	}
	@Override
	public boolean hasField(Variable self, String name) {
		return false;
	}
	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}

	private Map<String,BuiltinFunction> METHODS;
	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		return super.hasBuiltinMethodBasic(name, METHODS);
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return super.getBuiltinMethodBasic(self, name, METHODS);
	}
	@Override
	public boolean canSetToExpr(ConstExprToken e) {
		return e.constType()==ConstType.STRLIT;
	}
	@Override
	@Targeted
	public void setMeToExpr(PrintStream p, Scope s, RStack stack, Variable me, ConstExprToken e) throws CompileError {
		p.printf("data modify %s set value %s\n",me.dataPhrase(), ((Token.StringToken)e).literal());
	}
	@Override
	public boolean canDoBiOpDirect(BiOperator op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		if (other.isStruct() && other.struct instanceof Str) ;
		else return false;
		switch(op.op) {
		case EQ:
		case NEQ: return true;
		default: return false;
		}
		
	}
	@Override
	public int doBiOpFirstDirect(BiOperator op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack,
			INbtValueProvider me, INbtValueProvider other) throws CompileError {
		return super.basicDirectEquals(p, c, s, stack, me, other, op.op == OpType.NEQ);
	}
	
}
