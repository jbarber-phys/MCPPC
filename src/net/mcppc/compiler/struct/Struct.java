package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.CompileError.UnsupportedCast;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.functions.FunctionMask;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.TemplateArgsToken;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.UnaryOp;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.UnaryOp.UOType;

/**
 * currently hypothetical
 * 
 * like a class but the workings are hard coded into the compiler
 * the class tells what type-args (like precision) to accept
 * 
 * 
 * TODO it should be possible to also create a class; a struct that will interperet mcpp code as a class template and 
 * determine the behavior at compile time, but functions will be hard as they will need to copy this*
 * 
 * TODO class for Queue, Stack, and List;
 * may want to set up a binary search tree mcfunction
 * 
 * skip fixed size arrays as they always have confounding behavior (Vector, Rotation, UUID, ArmorItems)
 * 
 * FIXED list tags and compound tags need to be initialized or they wont work
 * must run code to initialize these types
 * example for int array (3):
 * /data modify storage mcpptest:vectest upint set value [0,0,0]
 * example for double array (3):
 * /data modify storage mcpptest:vectest up set value [0.0d,0.0d,0.0d]
 * even the suptype must match; cannot set double to float
 * example for compound tag
 * /data modify storage mcpptest:vectest obj set value {objfield: {subfield: {}}}
 * 
 * FIXED doubles & floats also dont play nice;
 * FIXED also the d,f index comes AFTER the sci notation
 * 
 * @author jbarb_t8a3esk
 *
 */
public abstract class Struct {
	
	protected static final Map<String,Struct> STRUCTS=new HashMap<String,Struct>();
	public static void register(Struct s) {
		if(s instanceof IInternalOnly) {
			System.err.printf("struct %s is internal only so cannot be registered; skipping registry;\n",s.name);
			return;
		}
		STRUCTS.put(s.name, s);
	}
	static {
		Vector.registerAll();
		Str.registerAll();
		Entity.registerAll();
		NbtCollection.registerAll();
	}
	public static boolean load() {
		//a dumb method that exists soly to make sure this initializes before something else else
		return true;
	};
	public static boolean isStruct(String name) {
		return STRUCTS.containsKey(name);
	}

	public static Struct getStruct(String name) {
		return STRUCTS.get(name);
	}
	public static String[] getStructNames() {
		return STRUCTS.keySet().toArray(new String[STRUCTS.keySet().size()]);
	}
	@Deprecated private static interface IMCFRetTypeStruct{
		//this wont work; lookbehinds must be fixed length
		public String getGroup();
	}
	public static interface IInternalOnly{}
	
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
	public StructTypeParams tokenizeTypeArgs(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden)throws CompileError {
		//check that there are no args
		Token t=c.nextNonNullMatch(Factories.closeParen);
		if ((!(t instanceof Token.Paren)) || ((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t,"')'");
		else return new StructTypeParams.Blank();
	}
	public StructTypeParams paramsWNoArgs() throws CompileError{
		return new StructTypeParams.Blank();
	}
	
	public VarType withPrecision(VarType vt,int newPrecision) throws CompileError{
		return vt;
	}

	public String asString(VarType varType){//no throws
		return this.name;
	}
	public String headerTypeString(VarType varType){//no throws
		return this.name;
	}
	public abstract int getPrecision(VarType mytype, Scope s) throws CompileError;
	public abstract String getPrecisionStr(VarType mytype) ;
	/**
	 * used for custom print settings for /tellraw
	 * @param variable the variable to be displayed
	 * @return the json text element to be used in /tellraw
	 * @throws CompileError 
	 */
	public abstract String getJsonTextFor(Variable variable) throws CompileError ;//no throws
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
	public void castRegistersFrom(PrintStream p, Scope s,RStack stack,int start,VarType old, VarType mytype) throws CompileError{
		//do nothing
	}
	public void castVarFrom(PrintStream p, Scope s ,RStack stack,Variable vtag,VarType old, VarType mytype) throws CompileError{
		//do nothing
	}
	
	public boolean canCasteTo(VarType to,VarType mytype) { return mytype.equals(mytype); }
	public void castRegistersTo(PrintStream p, Scope s,RStack stack,int start,VarType newType, VarType mytype) throws CompileError{
		//do nothing
	}
	public void castVarTo(PrintStream p, Scope s,RStack stack,Variable vtag,VarType mytype, VarType newType) throws CompileError{
		//do nothing
	}
	/**
	 * sets some registers to the value of the struct
	 * @param p
	 * @param s TODO
	 * @param stack
	 * @param home
	 * @param me
	 * @throws CompileError
	 */
	public abstract void getMe(PrintStream p,Scope s,RStack stack,int home, Variable me)throws CompileError;
	/**
	 * gets some registers to set a variable of this struct
	 * @param p
	 * @param s TODO
	 * @param stack
	 * @param home
	 * @param me
	 * @throws CompileError
	 */
	public abstract void setMe(PrintStream p,Scope s,RStack stack,int home, Variable me)throws CompileError;
	public void getMeDirect(PrintStream p,Scope s,RStack stack,Variable to, Variable me)throws CompileError{
		Struct.basicSetDirect(p, to, me);
	}
	public void setMeDirect(PrintStream p,Scope s,RStack stack,Variable me, Variable from)throws CompileError{
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
	public boolean canMask(VarType mytype, Mask mask) { return mask !=Mask.SCORE; }
	
	public boolean canDoBiOp(BiOperator.OpType op,VarType mytype,VarType other,boolean isFirst)throws CompileError {return false;};
	public void doBiOpFirst(BiOperator.OpType op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2)
			throws CompileError{throw new CompileError.UnsupportedOperation(mytype, op, stack.getVarType(home2));}
	public void doBiOpSecond(BiOperator.OpType op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2)
			throws CompileError{throw new CompileError.UnsupportedOperation(stack.getVarType(home1), op, mytype);}
	
	//the inputs stay off stack but these funcs push a new register (return index) to the stack
	//example: struct comparisons
	public boolean canDoBiOpDirect(BiOperator op,VarType mytype,VarType other,boolean isFirst)throws CompileError {return false;};
	public int doBiOpFirstDirect(BiOperator op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,INbtValueProvider me,INbtValueProvider other)
			throws CompileError{throw new CompileError.UnsupportedOperation(mytype, op, other.getType());}
	public int doBiOpSecondDirect(BiOperator op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,INbtValueProvider other,INbtValueProvider me)
			throws CompileError{throw new CompileError.UnsupportedOperation(other.getType(), op, mytype);}
	protected static int basicDirectEquals(PrintStream p,Compiler c,Scope s, RStack stack,INbtValueProvider first,INbtValueProvider second
			,boolean invert) throws CompileError {
		//compare tags for equality;
		int home=stack.setNext(VarType.BOOL);
		Register reg=stack.getRegister(home);
		
		if (first.equals(second)) {
			//then they are equal
			reg.setValue(p,!invert);
			return home;
		}
		Variable temp = new Variable("\"$temp\"",first.getType(),null,Mask.STORAGE,"mcpp:temp","\"$temp\"");
		String dtemp=temp.dataPhrase();
		String nbtcmd1=first.fromCMDStatement();
		String nbtcmd2=second.fromCMDStatement();
		p.printf("data modify %s set %s\n",dtemp,nbtcmd1);
		//int ex = stack.reserve(1);Register rex = stack.getRegister(ex);
		String cmd = "data modify %s set %s".formatted(dtemp,nbtcmd2);
		reg.setToSuccess(p, cmd); // see if nothing changed
		if(!invert) {
			UnaryOp not = new UnaryOp(-1,-1,UOType.NOT);
			not.perform(p, c, s, stack, home);
		}
		return home;
	}
	
	//the first term is the destination
	//the second term should be immutable
	//these will be avoided if normal ops are possible
	public boolean canDoBiOpDirectOn(BiOperator op,VarType mytype,VarType other)throws CompileError {return false;};
	public void doBiOpFirstDirectOn(BiOperator op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Variable me,INbtValueProvider other)
			throws CompileError{throw new CompileError.UnsupportedOperation(mytype, op, other.getType());}
	
	
	public void exp(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Number exponent) throws CompileError {
		throw new CompileError.UnsupportedOperation(stack.getVarType(home1), OpType.EXP, VarType.INT);
	}
	public boolean canDoUnaryOp(UnaryOp.UOType op,VarType mytype,VarType other)throws CompileError {return false;}
	public void doUnaryOp(UnaryOp.UOType op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer home)
			throws CompileError{throw new CompileError.UnsupportedOperation( op, mytype);}
	
	//literal term must be after for div but can be before for mult
	public boolean canDoLiteralMultDiv(BiOperator op,VarType mytype,Num other)throws CompileError {return false;}
	public void doLiteralMultDiv(BiOperator op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer in,Integer dest,Num other)
			throws CompileError{throw new CompileError.UnsupportedOperation( op, mytype);}
	
	
	public void setVarToNumber(PrintStream p,Compiler c,Scope s, RStack stack,Number val,Variable self) throws CompileError {throw new CompileError.CannotSet(self.type, "number");}
	public void setRegistersToNumber(PrintStream p,Compiler c,Scope s, RStack stack,int home,Number val,VarType myType)throws CompileError {throw new CompileError.CannotSet(myType, "number");}
	public void setVarToBool(PrintStream p,Compiler c,Scope s, RStack stack,boolean val,Variable self)throws CompileError{throw new CompileError.CannotSet(self.type, "bool");}
	public void setRegistersToBool(PrintStream p,Compiler c,Scope s, RStack stack,int home,boolean val,VarType myType)throws CompileError{throw new CompileError.CannotSet(myType, "bool");}
	

	public boolean canBeRecursive(VarType type) {
		return false;
	}
	
	public boolean willAllocateLoad(Variable var, boolean fillWithDefaultvalue){
		return true;
	}
	/**
	 * function to run that will setup all the tags so that this var is safe to set (allowed changes will not fail due to the tag being the wrong type)
	 * 
	 * example for double array (3):
	 * /data modify storage mcpptest:vectest up set value [0.0d,0.0d,0.0d]
	 * even the suptype must match; cannot set double to float
	 * example for compound tag
	 * /data modify storage mcpptest:vectest obj set value {objfield: {subfield: {}}}
	 * @param p
	 * @param var
	 * @param fillWithDefaultvalue
	 * @throws CompileError
	 */
	public abstract void allocateLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError;
	public abstract void allocateCall(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError;
	
	//all the members of the implimentation buffet just affect the member tag;
	//all members of self do not see the recursive nature, they are called as if on load

	protected void allocateArrayLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue,int size,VarType elementType) throws CompileError {
		if(var.isRecursive())
			var.allocateLoadBasic(p, fillWithDefaultvalue, DEFAULT_LIST);
		else {
			this.allocateArray(p, var, fillWithDefaultvalue, size, elementType);
		}
	}
	protected void allocateArrayCall(PrintStream p, Variable var, boolean fillWithDefaultvalue,int size,VarType elementType) throws CompileError {
		if(var.isRecursive()) {
			var.allocateCallBasic(p, fillWithDefaultvalue, DEFAULT_LIST);
			this.allocateArray(p, var, fillWithDefaultvalue, size, elementType);
		}
		else {
			//this should never be called
			this.allocateArray(p, var, fillWithDefaultvalue, size, elementType);
		}
	}
	/**
	 * an implimentation of allocate() for types stored as arrays
	 * @param p
	 * @param var
	 * @param fillWithDefaultvalue
	 * @param size
	 * @param elementType
	 * @throws CompileError 
	 */
	private void allocateArray(PrintStream p, Variable var, boolean fillWithDefaultvalue,int size,VarType elementType) throws CompileError {
		//can set to empty array []
		//then append 1 element to fix the type
		//the type is fixed once the first element is added
		//append ops on an empty tag will make the tag a list and add the first element
		//but index set ops do not create sub list for you
		//cannot change type of first element to change array type
		
		
		if(var.getMaskType()!=Mask.STORAGE) {
			Warnings.warningf("attempted to deallocate %s to non-storage %s;",var.name,var.getMaskType());
			return;
		}
		p.printf("data modify %s set value %s\n",var.dataPhrase(), DEFAULT_LIST);
		//TODO double check that lists within lists can have differente element types
		//TODO allocate member if they are structs
		String subname="\"$%s\".\"$allocate_fill\"".formatted(this.name);
		Variable fill=new Variable(subname, elementType, null, new ResourceLocation("mcppc","load__allocate"));
		if(fill.willAllocateOnLoad(fillWithDefaultvalue)) {
			fill.allocateLoad(p, fillWithDefaultvalue);
			for(int i=0;i<size;i++) p.printf("data modify %s append from %s\n",var.dataPhrase(), fill.dataPhrase());
			fill.deallocateLoad(p);
		}else {
			String fillstr = elementType.defaultValue();
			for(int i=0;i<size;i++) p.printf("data modify %s append value %s\n",var.dataPhrase(), fillstr);
		}
		//(could also prepend / insert)
		
		
	}

	protected void allocateCompoundLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue,List<String> fieldNames)  throws CompileError{
		if(var.isRecursive())
			var.allocateLoadBasic(p, fillWithDefaultvalue, DEFAULT_LIST);
		else {
			this.allocateCompound(p, var, fillWithDefaultvalue, fieldNames);
		}
	}

	protected void allocateCompoundCall(PrintStream p, Variable var, boolean fillWithDefaultvalue,List<String> fieldNames)  throws CompileError{
		if(var.isRecursive()) {
			var.allocateCallBasic(p, fillWithDefaultvalue, DEFAULT_LIST);
			this.allocateCompound(p, var, fillWithDefaultvalue, fieldNames);
		}
		else {
			//this should never be called
			this.allocateCompound(p, var, fillWithDefaultvalue, fieldNames);
		}
	}
	private void allocateCompound(PrintStream p, Variable var, boolean fillWithDefaultvalue,List<String> fieldNames)  throws CompileError{

		//data modify <var> set value {}

		if(var.getMaskType()!=Mask.STORAGE) {
			Warnings.warningf("attempted to deallocate %s to non-storage %s;",var.name,var.getMaskType());
			return;
		}
		p.printf("data modify %s set value %s\n",var.dataPhrase(), DEFAULT_COMPOUND);
		//or data remove <this> will also work, as sets to members will create sub-compounds
		for(String name:fieldNames) if(this.hasField(var, name)){
			Variable field=this.getField(var, name);
			field.allocateLoad(p, fillWithDefaultvalue);
		}
	}
	//use //var.allocateLoadBasic(p, fillWithDefaultvalue, DEFAULT_STRING);
	@Deprecated private void allocateString(PrintStream p, Variable var, boolean fillWithDefaultvalue)  throws CompileError{
		//data modify <var> set value ""
		if(var.getMaskType()!=Mask.STORAGE) {
			Warnings.warningf("attempted to deallocate %s to non-storage %s;",var.name,var.getMaskType());
			return;
		}
		if(fillWithDefaultvalue)p.printf("data modify %s set value %s\n",var.dataPhrase(), DEFAULT_STRING);
		else ;//do nothing
		
	}
	/**
	 * execute to delete storage for this var
	 * DO NOT CALL THIS FOR AN ARRAY MEMBER OR IT WILL CHANGE THE ARRAY SIZE
	 * ALSO DO NOT CALL THIS METHOD FOR SUB FIELDS, it is not needed
	 * @param p
	 * @param var
	 */
	public void deallocateLoad(PrintStream p, Variable var) {
		var.basicdeallocateBoth(p);
		//data remove <var>
	}
	public void deallocateAfterCall(PrintStream p, Variable var) {
		//this one is supposed to change the list size
		var.basicdeallocateBoth(p);
		//data remove <var>
	}
	public abstract String getDefaultValue (VarType var) throws CompileError;
	public static final String DEFAULT_STRING="\"\"";
	public static final String DEFAULT_LIST="[]";
	public static final String DEFAULT_COMPOUND="{}";
	/*
	 * members:
	 */
	public abstract boolean hasField(Variable self,String name);
	public abstract Variable getField(Variable self,String name) throws CompileError;
	
	
	public boolean canIndexMe(Variable self, int i) throws CompileError{
		return false;
	}
	public Variable getIndexRef(Variable self,int index) throws CompileError{throw new CompileError.VarNotFound(this, index);};
	protected Variable basicTagIndexRef(Variable self,int index,VarType elementType) {
		return self.indexMyNBTPathBasic(index, elementType);
	}
	

	public abstract boolean hasBuiltinMethod(Variable self,String name);
	public abstract BuiltinFunction getBuiltinMethod(Variable self,String name) throws CompileError;
	//implementation Buffet
	protected boolean hasBuiltinMethodBasic(String name,Map<String,BuiltinFunction> mds) {
		return mds.containsKey(name);
	}
	protected BuiltinFunction getBuiltinMethodBasic(Variable self,String name,Map<String,BuiltinFunction> mds) throws CompileError {
		return mds.get(name);
	}
	
	
	//currently not possible to make non-builtin methods as they require the instance to be bound or unneccicarily copied;
	//if we make a class struct, it will be unoptimal and so should create its own 
	
	/*
	 * static members:
	 *
	 */
	
	
	public boolean hasStaticBuiltinMethod(String name) {
		return false;
	}
	public BuiltinFunction getStaticBuiltinMethod(String name,VarType type) throws CompileError {
		throw new CompileError.BFuncNotFound(this, name, true);
	}
	protected boolean hasStaticBuiltinMethodBasic(String name,Map<String,FunctionMask> mds) {
		return mds.containsKey(name);
	}
	protected BuiltinFunction getStaticBuiltinMethodBasic(String name,VarType type,Map<String,FunctionMask> mds) throws CompileError {
		return mds.get(name);
	}
	
	public BuiltinConstructor getConstructor(VarType myType) throws CompileError {
		throw new CompileError("no constructor defined for type %s;".formatted(myType.asString()));
	}
	public boolean canSetToExpr(ConstExprToken e) {
		return false;
	}
	public void setMeToExpr(PrintStream p,RStack stack,Variable me, ConstExprToken e) throws CompileError {
		throw new CompileError.UnsupportedCast(e, me.type);
	}

	public boolean isReady(VarType varType) {
		return varType.structArgs==null?true:varType.structArgs.isReady();
	}

	public VarType withTemplatePrecision(VarType varType, String pc) throws CompileError{
		return varType;//do nothing
	}
	public TemplateArgsToken getTemplateArgs(VarType varType, Scope s) throws CompileError {
		return null;
	}
	public void setMeToCmd(PrintStream p, Scope s, Variable variable, String cmd) throws CompileError{
		throw new CompileError.CannotSet(variable.type, "a command output");
	}
}
