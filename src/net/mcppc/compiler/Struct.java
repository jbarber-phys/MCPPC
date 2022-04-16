package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.VarType.StructTypeParams;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.UnaryOp;

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
	public Struct(String name,boolean isNumeric, boolean isFloatP, boolean isLogical) throws CompileError {
		this.name=name;
		this.isNumeric=isNumeric;
		this.isFloatP=isFloatP;
		this.isLogical=isLogical;
	}
	public Struct(String name)throws CompileError {
		this(name,false,false,false);
	}
	
	/**
	 * tokenizes the type arguments, leaving cursor after the closing paren
	 * @param c
	 * @param matcher
	 * @param line
	 * @param col
	 * @return
	 */
	public VarType.StructTypeParams tokenizeTypeArgs(Compiler c, Matcher matcher, int line, int col)throws CompileError {
		//check that there are no args
		Token t=c.nextNonNullMatch(Factories.closeParen);
		if ((!(t instanceof Token.Paren)) || ((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t,"')'");
		else return new StructTypeParams.Blank();
	}
	public VarType.StructTypeParams paramsWNoArgs() throws CompileError{
		return new StructTypeParams.Blank();
	}
	
	public VarType.StructTypeParams withPrecision(VarType.StructTypeParams vt) throws CompileError{
		return vt;
	}

	public String asString(VarType varType){//no throws
		return this.name;
	}
	public String headerTypeString(VarType varType){//no throws
		return this.name;
	}
	public abstract int getPrecision(StructTypeParams structArgs);//no throws
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
	public abstract int sizeOf(StructTypeParams structArgs);
	
	//caste from takes precedent over caste to;
	public boolean canCasteFrom(VarType from,VarType.StructTypeParams myArgs) { return false; }
	public abstract void castRegistersFrom(PrintStream p, RStack stack,int start,VarType old,VarType.StructTypeParams myArgs) throws CompileError;
	
	public boolean canCasteTo(VarType to,VarType.StructTypeParams myArgs) { return false; }
	public abstract void castRegistersTo(PrintStream p, RStack stack,int start,VarType newType,VarType.StructTypeParams myArgs) throws CompileError;
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
		Struct.basicSetDirect(p, stack, to, me);
	}
	public void setMeDirect(PrintStream p,RStack stack,Variable me,Variable from)throws CompileError{
		Struct.basicSetDirect(p, stack, me, from);
	}
	/**
	 * an implimentation of direct get/set that will usually work unless there is casting
	 * @param p
	 * @param stack
	 * @param to
	 * @param from
	 */
	public static void basicSetDirect(PrintStream p,RStack stack,Variable to,Variable from) {
		String dto=to.dataPhrase();
		String dfrom=from.dataPhrase();
		p.printf("data modify %s set from %s\n",dto,dfrom);
	}

	
	public abstract boolean canDoBiOp(BiOperator.OpType op,StructTypeParams tps,VarType other,boolean isFirst)throws CompileError;
	public abstract void doBiOpFirst(BiOperator.OpType op,StructTypeParams tps,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2)throws CompileError;
	public abstract void doBiOpSecond(BiOperator.OpType op,StructTypeParams tps,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2)throws CompileError;

	public abstract void doUnaryOp(UnaryOp.UOType op,StructTypeParams tps,PrintStream p,Compiler c,Scope s, RStack stack,Integer home)throws CompileError;

	
	
	public abstract void setVarToNumber(PrintStream p,Compiler c,Scope s, RStack stack,Number val,StructTypeParams tps)throws CompileError;
	public abstract void setRegistersToNumber(PrintStream p,Compiler c,Scope s, RStack stack,int home,Number val,StructTypeParams tps)throws CompileError;
	public abstract void setVarToBool(PrintStream p,Compiler c,Scope s, RStack stack,boolean val,StructTypeParams tps)throws CompileError;
	public abstract void setRegistersToBool(PrintStream p,Compiler c,Scope s, RStack stack,int home,boolean val,StructTypeParams tps)throws CompileError;
	
	
	public abstract boolean isCompadible(VarType type2)throws CompileError;
	public abstract void directSet(PrintStream p,Variable to, Variable from,boolean toMe)throws CompileError;
	public abstract void toScore(PrintStream p,RStack s,Integer  to, Variable from,boolean toMe)throws CompileError;
	public void directSetTag(PrintStream p,Variable to, Variable from)throws CompileError{
		
	}
}
