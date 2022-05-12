package net.mcppc.compiler.struct;
import net.mcppc.compiler.struct.*;
import net.mcppc.compiler.tokens.Token;

import java.io.PrintStream;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.VarType.Builtin;
import net.mcppc.compiler.errors.CompileError;
/**
 * string tags cannot be appended / inserted ...
 * 
 * @author jbarb_t8a3esk
 *
 */
public class Str extends Struct{
	public static final Str string;
	static {
		string=new Str("String");
		
	}

	public static final VarType STR = new VarType(string,new StructTypeParams.Blank());
	public static void registerAll() {
		Struct.register(string);
	}
	public Str(String name) {
		super(name);
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return "tag_string";
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
	protected String getJsonTextFor(Variable variable) throws CompileError {
		return variable.getJsonTextBasic();
	}
	@Override
	public int sizeOf(VarType mytype) {
		return -1;//cannot stack
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
	public void allocate(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		this.allocateString(p, var, fillWithDefaultvalue);
	}
	@Override
	public String getDefaultValue(VarType var) {
		return Struct.DEFAULT_STRING;
	}
	@Override
	public boolean hasField(String name, VarType mytype) {
		return false;
	}
	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}
	@Override
	public boolean hasBuiltinMethod(String name, VarType mytype) {
		return false;
	}
	@Override
	public BuiltinStructMethod getBuiltinMethod(Variable self, String name) throws CompileError {
		return null;
	}
	@Override
	public boolean canSetToExpr(ConstExprToken e) {
		return e.constType()==ConstType.STRLIT;
	}
	@Override
	public void setMeToExpr(PrintStream p, RStack stack, Variable me, ConstExprToken e) throws CompileError {
		p.printf("data modify %s set value %s\n",me.dataPhrase(), ((Token.StringToken)e).literal());
	}
	
}
