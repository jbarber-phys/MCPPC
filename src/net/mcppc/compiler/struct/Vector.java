package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.UnaryOp.UOType;

public abstract class Vector extends Struct {
	//public static final Vector vector;
	//public static final Vector vec3d;
	//public static final Vector vec3i;
	static {
		//vector=new Vector("vector");
		//vec3d=new Vector("vec3d",VarType.DOUBLE);
		//vec3i=new Vector("vec3i",VarType.LONG);
	}
	//vector type can be set by type arg or by naming vector subtype
	//if null, must supply args
	//else must not supply args
	public final VarType defaulttype;
	public Vector(String name)  {
		super(name);
		this.defaulttype=null;
	}
	public Vector(String name,VarType dt)  {
		super(name);
		this.defaulttype=dt;
	}
	@Override
	public String getNBTTagType(VarType varType) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public StructTypeParams tokenizeTypeArgs(Compiler c, Matcher matcher, int line, int col) throws CompileError {
		// TODO Auto-generated method stub
		return super.tokenizeTypeArgs(c, matcher, line, col);
	}
	@Override
	public StructTypeParams paramsWNoArgs() throws CompileError {
		// TODO Auto-generated method stub
		return super.paramsWNoArgs();
	}
	@Override
	public StructTypeParams withPrecision(StructTypeParams vt) throws CompileError {
		// TODO Auto-generated method stub
		return super.withPrecision(vt);
	}
	@Override
	public String asString(VarType varType) {
		// TODO Auto-generated method stub
		return super.asString(varType);
	}
	@Override
	public String headerTypeString(VarType varType) {
		// TODO Auto-generated method stub
		return super.headerTypeString(varType);
	}
	@Override
	public int getPrecision(VarType mytype) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	protected String getJsonTextFor(Variable variable) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int sizeOf(VarType mytype) {
		return 3;
	}
	@Override
	public boolean canCasteFrom(VarType from, VarType mytype) {
		// TODO Auto-generated method stub
		return super.canCasteFrom(from, mytype);
	}
	@Override
	public void castRegistersFrom(PrintStream p, RStack stack, int start, VarType old, VarType mytype)
			throws CompileError {
		// TODO Auto-generated method stub
		super.castRegistersFrom(p, stack, start, old, mytype);
	}
	@Override
	public boolean canCasteTo(VarType to, VarType mytype) {
		// TODO Auto-generated method stub
		return super.canCasteTo(to, mytype);
	}
	@Override
	public void castRegistersTo(PrintStream p, RStack stack, int start, VarType newType, VarType mytype)
			throws CompileError {
		// TODO Auto-generated method stub
		super.castRegistersTo(p, stack, start, newType, mytype);
	}
	@Override
	public void getMe(PrintStream p, RStack stack, int home, Variable me) throws CompileError {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setMe(PrintStream p, RStack stack, int home, Variable me) throws CompileError {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean canDoBiOp(OpType op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		// TODO Auto-generated method stub
		return super.canDoBiOp(op, mytype, other, isFirst);
	}
	@Override
	public void doBiOpFirst(OpType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		// TODO Auto-generated method stub
		super.doBiOpFirst(op, mytype, p, c, s, stack, home1, home2);
	}
	@Override
	public void doBiOpSecond(OpType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		// TODO Auto-generated method stub
		super.doBiOpSecond(op, mytype, p, c, s, stack, home1, home2);
	}
	@Override
	public boolean canDoUnaryOp(OpType op, VarType mytype, VarType other) throws CompileError {
		// TODO Auto-generated method stub
		return super.canDoUnaryOp(op, mytype, other);
	}
	@Override
	public void doUnaryOp(UOType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home)
			throws CompileError {
		// TODO Auto-generated method stub
		super.doUnaryOp(op, mytype, p, c, s, stack, home);
	}
	@Override
	public boolean hasField(String name, VarType mytype) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean hasBuiltinMethod(String name, VarType mytype) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public BuiltinStructMethod getBuiltinMethod(Variable self, String name) throws CompileError {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean hasStaticBuiltinMethod(String name, VarType mytype) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public BuiltinFunction geStatictBuiltinMethod(String name) throws CompileError {
		// TODO Auto-generated method stub
		return null;
	}
	
	//TODO stuff
	

}
