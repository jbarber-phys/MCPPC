package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.UnaryOp;

import net.mcppc.compiler.struct.*;

/**
 * currently hypothetical
 * 
 * like a class but the workings are hard coded into the compiler
 * the class tells what type-args (like precision) to accept
 * 
 * 
 * TODO it should be possible to also create a class; a struct that will interperet mcpp code as a class template and 
 * determine the behavior at compile time
 * @author jbarb_t8a3esk
 *
 */
public abstract class Struct {
	//TODO String and Vector structs
	
	public static final Map<String,Struct> STRUCTS=new HashMap<String,Struct>();
	public static boolean isStruct(String name) {
		return STRUCTS.containsKey(name);
	}
	public final String name;
	public final boolean isNumeric;
	public final boolean isFloatP;
	public final boolean isLogical;
	public Struct(String name,boolean isNumeric, boolean isFloatP, boolean isLogical)  {
		this.name=name;
		this.isNumeric=isNumeric;
		this.isFloatP=isFloatP;
		this.isLogical=isLogical;
	}
	public Struct(String name) {
		this(name,false,false,false);
	}
	public abstract String getNBTTagType(VarType varType);
	
	/**
	 * tokenizes the type arguments, leaving cursor after the closing paren
	 * @param c
	 * @param matcher
	 * @param line
	 * @param col
	 * @return
	 */
	public StructTypeParams tokenizeTypeArgs(Compiler c, Matcher matcher, int line, int col)throws CompileError {
		//check that there are no args
		Token t=c.nextNonNullMatch(Factories.closeParen);
		if ((!(t instanceof Token.Paren)) || ((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t,"')'");
		else return new StructTypeParams.Blank();
	}
	public StructTypeParams paramsWNoArgs() throws CompileError{
		return new StructTypeParams.Blank();
	}
	
	public StructTypeParams withPrecision(StructTypeParams vt) throws CompileError{
		return vt;
	}

	public String asString(VarType varType){//no throws
		return this.name;
	}
	public String headerTypeString(VarType varType){//no throws
		return this.name;
	}
	public abstract int getPrecision(VarType mytype);//no throws
	/**
	 * used for custom print settings for /tellraw
	 * @param variable the variable to be displayed
	 * @return the json text element to be used in /tellraw
	 */
	protected abstract String getJsonTextFor(Variable variable) ;//no throws
	/**
	 * the number of registers this object takes up
	 * @param structArgs
	 * @return number of registers; negative if it can't be put on register
	 */
	public abstract int sizeOf(VarType mytype);
	
	//caste from takes precedent over caste to;
	public boolean canCasteFrom(VarType from,VarType mytype) { 
		//do nothing if they are equal
		return from.equals(mytype);
		}
	public void castRegistersFrom(PrintStream p, RStack stack,int start,VarType old,VarType mytype) throws CompileError{
		//do nothing
	}
	protected void castVarFrom(PrintStream p, RStack stack,Variable vtag,VarType old,VarType mytype) throws CompileError{
		//do nothing
	}
	
	public boolean canCasteTo(VarType to,VarType mytype) { return mytype.equals(mytype); }
	public void castRegistersTo(PrintStream p, RStack stack,int start,VarType newType,VarType mytype) throws CompileError{
		//do nothing
	}
	protected void castVarTo(PrintStream p, RStack stack,Variable vtag,VarType mytype,VarType newType) throws CompileError{
		//do nothing
	}
	/**
	 * sets some registers to the value of the struct
	 * @param p
	 * @param stack
	 * @param home
	 * @param me
	 * @throws CompileError
	 */
	public abstract void getMe(PrintStream p,RStack stack,int home,Variable me)throws CompileError;
	/**
	 * gets some registers to set a variable of this struct
	 * @param p
	 * @param stack
	 * @param home
	 * @param me
	 * @throws CompileError
	 */
	public abstract void setMe(PrintStream p,RStack stack,int home,Variable me)throws CompileError;
	public void getMeDirect(PrintStream p,RStack stack,Variable to,Variable me)throws CompileError{
		Struct.basicSetDirect(p, to, me);
	}
	public void setMeDirect(PrintStream p,RStack stack,Variable me,Variable from)throws CompileError{
		Struct.basicSetDirect(p, me, from);
	}
	///public abstract boolean isCompadible(VarType type2)throws CompileError;//same as can cast to
	
	/**
	 * an implimentation of direct get/set that will usually work unless there is casting
	 * @param p
	 * @param stack
	 * @param to
	 * @param from
	 */
	public static void basicSetDirect(PrintStream p,Variable to,Variable from) {
		String dto=to.dataPhrase();
		String dfrom=from.dataPhrase();
		p.printf("data modify %s set from %s\n",dto,dfrom);
	}
	public boolean canMaskScore(VarType mytype) { return false; }
	
	public boolean canDoBiOp(BiOperator.OpType op,VarType mytype,VarType other,boolean isFirst)throws CompileError {return false;};
	public void doBiOpFirst(BiOperator.OpType op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2)
			throws CompileError{throw new CompileError.UnsupportedOperation(mytype, op, stack.getVarType(home2));}
	public void doBiOpSecond(BiOperator.OpType op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2)
			throws CompileError{throw new CompileError.UnsupportedOperation(stack.getVarType(home1), op, mytype);}

	public boolean canDoUnaryOp(BiOperator.OpType op,VarType mytype,VarType other)throws CompileError {return false;}
	public void doUnaryOp(UnaryOp.UOType op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer home)
			throws CompileError{throw new CompileError.UnsupportedOperation( op, mytype);}

	
	
	public void setVarToNumber(PrintStream p,Compiler c,Scope s, RStack stack,Number val,VarType myType) throws CompileError {throw new CompileError.CannotSet(myType, "number");}
	public void setRegistersToNumber(PrintStream p,Compiler c,Scope s, RStack stack,int home,Number val,VarType myType)throws CompileError {throw new CompileError.CannotSet(myType, "number");}
	public void setVarToBool(PrintStream p,Compiler c,Scope s, RStack stack,boolean val,VarType myType)throws CompileError{throw new CompileError.CannotSet(myType, "bool");}
	public void setRegistersToBool(PrintStream p,Compiler c,Scope s, RStack stack,int home,boolean val,VarType myType)throws CompileError{throw new CompileError.CannotSet(myType, "bool");}
	
	/*
	 * members:
	 */
	protected final Map<String,VarType> fields=new HashMap<String,VarType>();
	public abstract boolean hasField(String name,VarType mytype);
	public abstract Variable getField(Variable self,String name) throws CompileError;
	
	
	public boolean canIndexMe(int i) throws CompileError{
		return false;
	}
	public Variable getIndexRef(Variable self,int index) throws CompileError{throw new CompileError.VarNotFound(this, index);};
	protected Variable basicTagIndexRef(Variable self,int index,VarType type) {
		return self.indexMyNBTPath(index, type);
	}
	
	//TODO allow builtin member methods
	
	public abstract boolean hasBuiltinMethod(String name,VarType mytype);
	public abstract BuiltinStructMethod getBuiltinMethod(Variable self,String name) throws CompileError;
	
	
	//currently not possible to make non-builtin methods as they require the instance to be bound or unneccicarily copied;
	//if we make a class struct, it will be unoptimal and so should create its own 
	
	/*
	 * static members:
	 *
	 */
	

	public abstract boolean hasStaticBuiltinMethod(String name,VarType mytype);
	public abstract BuiltinFunction geStatictBuiltinMethod(String name) throws CompileError;
	
}
