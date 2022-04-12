package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.VarType.StructTypeParams;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.UnaryOp;
/**
 * a variable type, incliding type parameters like precision;
 * 
 * Note: strings can be set to values from const or other tags with command so string vars are addable but maybe should be introduced as a struct
 * 
 * @author jbarb_t8a3esk
 *
 */
public class VarType {
	public enum Builtin{
		BYTE("byte",true,false,false,false),
		SHORT("short",true,false,false,false),
		INT("int",true,false,false,false),
		LONG("long",true,false,false,false),
		
		FLOAT("float",true,true,false,false),
		DOUBLE("double",true,true,false,false),
		
		BOOL("bool",false,false,true,false),
		
		STRUCT("struct",false,false,false,true),
		
		VOID("void",false,false,false,false,true);
		public boolean isNumber;
		public boolean isFloatP;
		public boolean isLogical;
		public boolean isStruct;
		public boolean isVoid;
		int sizeof = 1;//number of scores needed; currently it is always one
		String typename;
		Builtin(String name,boolean num,boolean flt,boolean logic,boolean st){
			this.isNumber=num;
			this.isFloatP=flt;
			this.typename=name;
			this.isVoid=false;
		}
		Builtin(String name,boolean num,boolean flt,boolean logic,boolean st,boolean isVoid){
			this.isNumber=num;
			this.isFloatP=flt;
			this.typename=name;
			this.isVoid=isVoid;
		}
		public String getTagType() throws CompileError {
			if (this.isStruct)throw new CompileError("Struct cannot be set directly to tag");
			if (this.isVoid)throw new CompileError("void cannot be set to tag");
			if (this==BOOL)return BYTE.typename;
			else return this.typename;
		}
	}
	public static interface StructTypeParams{
		public static final class Blank implements StructTypeParams{
			public Blank(){}
		}
	}
	public static final int DEFAULT_PRECISION = 3;
	final Builtin type;
	
	private final int precision; //does not affect structs
	protected final StructTypeParams structArgs;
	
	final Struct struct;//name of the struct if it is one
	//unimplimented; a struct would be a compile-made data type
	public VarType(Builtin type) {
		this.type = type;
		this.precision=type.isFloatP? DEFAULT_PRECISION:0;
		this.struct=null;
		this.structArgs=null;
	}
	public VarType(Builtin type,int precision) {
		this.type = type;
		this.precision=precision;
		this.struct=null;
		this.structArgs=null;
		
	}
	public VarType(Struct type,StructTypeParams params) {
		this.type = Builtin.STRUCT;
		this.struct=type;
		this.structArgs=params;
		this.precision=type.isFloatP? DEFAULT_PRECISION:0;
		
	}
	public boolean isNumeric() {
		if (this.type.isStruct) {
			return this.struct.isNumeric;
		}
		else return this.type.isNumber;
	}
	public boolean isFloatP() {
		if (this.type.isStruct) {
			return this.struct.isFloatP;
		}
		else return this.type.isFloatP;
	}
	public boolean isLogical() {
		if (this.type.isStruct) {
			return this.struct.isLogical;
		}
		else return this.type.isLogical;
	}
	public boolean isStruct() {
		return this.type.isStruct;
	}
	public boolean isVoid() {
		return this.type.isVoid;
	}
	//number of registers
	public int sizeOf() {
		if (this.type.isStruct) {
			return this.struct.sizeOf(this.structArgs);
		}
		//else if (this.isVoid()) return 0;
		else return 1;//for now, extra precision is ignored
	}
	public int getPrecision() {
		if (this.type.isStruct)return this.struct.getPrecision(this.structArgs);
		return this.isFloatP()?this.precision:0;
	}
	public VarType floatify() {
		switch(this.type) {
		case BYTE: return new VarType(Builtin.FLOAT);
		case SHORT: return new VarType(Builtin.FLOAT);
		case INT: return new VarType(Builtin.FLOAT);
		case LONG: return new VarType(Builtin.DOUBLE);
		//case struct
		default: return new VarType(this.type,this.precision);
		}
	}
	public VarType floatify(int newPrecision) {
		switch(this.type) {
		case BYTE: return new VarType(Builtin.FLOAT,newPrecision);
		case SHORT: return new VarType(Builtin.FLOAT,newPrecision);
		case INT: return new VarType(Builtin.FLOAT,newPrecision);
		case LONG: return new VarType(Builtin.DOUBLE,newPrecision);
		//case struct
		default: return new VarType(this.type,newPrecision);
		}
	}
	public VarType withPrecision(int newPrecision) throws CompileError {
		if (this.isStruct()){
			StructTypeParams pms=this.struct.withPrecision(this.structArgs);
		}
		return new VarType(this.type,newPrecision);
	}
	
	
	public static boolean isType(String type) {
		for (Builtin t: Builtin.values()) {
			if ((!t.isStruct) && t.typename.equals(type)) return true;
		}
		return Struct.isStruct(type);
		
	}public static Builtin getBuiltinType(String type) {
		for (Builtin t: Builtin.values()) {
			if ((!t.isStruct) && t.typename.equals(type)) return t;
		}
		return Struct.STRUCTS.containsKey(type)? Builtin.STRUCT:null;
		
	}
	//must be 
	public String asString(){
		if(this.type.isStruct) return this.struct.asString(this);
		else if(this.type.isFloatP) return "%s(%d)".formatted(this.type.typename,this.precision);
		else return this.type.typename;
	}
	public String headerString(){
		if(this.type.isStruct) return this.struct.headerTypeString(this);
		else if(this.type.isFloatP) return "%s(%d)".formatted(this.type.typename,this.precision);
		else return this.type.typename;
	}
	public String getFormatString() {
		if(this.isStruct())return "%s";
		if(this.isLogical())return "%s";
		if(this.isFloatP()) {
			if(this.precision<=0)return "%%.%df".formatted(this.precision);
			else return "%e";
		}else {
			//ints
			return "%d";
		}
	}
	public void doStructUnaryOp(UnaryOp.UOType op,PrintStream p,Compiler c,Scope s, RStack stack,Integer home) throws CompileError {
		this.struct.doUnaryOp(op, this.structArgs, p, c, s, stack, home);
	}
	public boolean supportsBiOp(BiOperator.OpType op,VarType other, boolean first) throws CompileError {
		return this.struct.canDoBiOp(op, this.structArgs, other, first);
	}
	public void doBiOpFirst(BiOperator.OpType op,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2) throws CompileError {
		this.struct.doBiOpFirst(op, this.structArgs, p, c, s, stack, home1, home2);
	}
	public void doBiOpSecond(BiOperator.OpType op,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2) throws CompileError {
		this.struct.doBiOpSecond(op, this.structArgs, p, c, s, stack, home1, home2);
	}
}
