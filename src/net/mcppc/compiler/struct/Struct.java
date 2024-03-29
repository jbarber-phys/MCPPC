package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.INbtValueProvider.Macro;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.CompileError.UnsupportedCast;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.functions.FunctionMask;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.target.Version;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Statement;
import net.mcppc.compiler.tokens.TemplateArgsToken;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.UnaryOp;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.UnaryOp.UOType;

/**
 * similar to a class type but the workings are hard coded into the compiler, and are defined by it (users cannot create them);
 * can accept type arguments determined by the struct;
 * true-classes (types that are definable in mcpp code) may be added in the future;
 * @author RadiumE13
 *
 */
@Targeted
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
		NbtMap.registerAll();
		Uuid.registerAll();
		Bossbar.registerAll();
		NbtCompound.registerAll();
		NbtObject.registerAll();
		McRandom.registerAll();
		
		Singleton.registerAll();
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
	/**
	 * should be unused as long as no structs are direct set from /execute store statements
	 * make a better system, such as enum for type and maybe a format code (normal = 0, ...)
	 * @param varType
	 * @return
	 */
	public abstract String getNBTTagType(VarType varType);
	/**
	 * alters var on declaration; usually does nothing;
	 * this is applied before any masks
	 * @param v
	 * @return
	 * @throws CompileError 
	 */
	public Variable varInit(Variable v,Compiler c,Scope s) throws CompileError {
		return v;
	}
	
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
		return this.headerTypeString(varType);
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
	/**
	 * determines if a variable of this type is equivalent to a const value
	 * this is very uncommmon and is basically only for the Entity types (which can cast to a const selector)
	 * @param type
	 * @return
	 */
	public boolean isConstEquivalent(VarType type) {
		return this.getConstType(type)!=null;
	}
	/**
	 * returns the types const-equivalent, return null if not const equivalent
	 * this is very uncommmon and is basically only for the Entity types (which can cast to a const selector)
	 * @param type
	 * @return
	 */
	public Const.ConstType getConstType(VarType type){
		return null;
	}
	/**
	 * returns the const equivalent of this variable (such as the selector of an Entity); throws if invalid
	 * @param v
	 * @param row
	 * @param col
	 * @return
	 * @throws CompileError 
	 */
	public ConstExprToken getConstEquivalent(Variable v,int row,int col) throws CompileError {
		//this should never be called
		throw new CompileError("cannot convert %s of type %s to a const expression".formatted(v.name,v.getType().asString()));
	}
	/**
	 * returns true if this variable is an abstraction of either data or scores; this is basically only false for Entity
	 * @param type
	 * @return
	 */
	public boolean isDataEquivalent(VarType type) {
		return !this.isConstEquivalent(type);
	}
	
	public void castRegistersFrom(PrintStream p, Scope s,RStack stack,int start,VarType old, VarType mytype) throws CompileError{
		//do nothing
	}
	public void castVarFrom(PrintStream p, Scope s ,RStack stack,Variable vtag,VarType old, VarType mytype) throws CompileError{
		//do nothing
	}
	/**
	 * if true, sets register types to the new type on a cast;
	 * this should only ever be false for NbtObj and should only be referenced before a variable setMe(...)
	 * @return
	 */
	public boolean setRegTypeOnCastFrom() {
		return true;
	}
	public boolean canCasteTo(VarType to,VarType mytype) { return mytype.equals(mytype); }
	
	public void castRegistersTo(PrintStream p, Scope s,RStack stack,int start,VarType newType, VarType mytype) throws CompileError{
		//do nothing
	}
	public void castVarTo(PrintStream p, Scope s,RStack stack,Variable vtag,VarType mytype, VarType newType) throws CompileError{
		//do nothing
	}
	
	/**
	 * allows the struct to auto convert itself if put on the stack
	 * @param mytype
	 * @param typeWanted TODO
	 * @return
	 */
	public VarType getTypeOnStack(VarType mytype, VarType typeWanted) {
		return mytype;
	}
	/**
	 * sets some registers to the value of the struct
	 * @param p
	 * @param s 
	 * @param stack
	 * @param home
	 * @param me
	 * @param typeWanted 
	 * @throws CompileError
	 */
	public abstract void getMe(PrintStream p,Scope s,RStack stack,int home, Variable me, VarType typeWanted)throws CompileError;
	/**
	 * gets some registers to set a variable of this struct
	 * @param p
	 * @param s 
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
	@Targeted
	public static void basicSetDirect(PrintStream p,Variable to,Variable from) {
		String dto=to.dataPhrase();
		String dfrom=from.dataPhrase();
		p.printf("data modify %s set from %s\n",dto,dfrom);
	}
	public boolean canMask(VarType mytype, Mask mask) { return mask.isNbt; }
	
	public boolean canDoBiOp(BiOperator.OpType op,VarType mytype,VarType other,boolean isFirst)throws CompileError {return false;};
	public void doBiOpFirst(BiOperator.OpType op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2)
			throws CompileError{throw new CompileError.UnsupportedOperation(mytype, op, stack.getVarType(home2));}
	public void doBiOpSecond(BiOperator.OpType op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2)
			throws CompileError{throw new CompileError.UnsupportedOperation(stack.getVarType(home1), op, mytype);}
	
	//the inputs stay off stack but these funcs push a new register (return index) to the stack
	//example: struct comparisons
	public boolean canCompareTags(VarType type,VarType otherType) {
		return this.canCasteFrom(otherType, type);
	}
	
	public boolean canDoBiOpDirect(BiOperator op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		if (this.canCompareTags(mytype, other)) ;
		else return false;
		switch(op.op) {
		case EQ:
		case NEQ: return true;
		default: return false;
		}
		
	}

	public int doBiOpFirstDirect(BiOperator op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack,
			INbtValueProvider me, INbtValueProvider other) throws CompileError {
		if(op.op == OpType.EQ || op.op == OpType.NEQ)
			return Struct.basicDirectEquals(p, c, s, stack, me, other, op.op == OpType.NEQ);
		else throw new CompileError.UnsupportedOperation(mytype, op, other.getType());
	}
	public int doBiOpSecondDirect(BiOperator op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,INbtValueProvider other,INbtValueProvider me)
			throws CompileError{throw new CompileError.UnsupportedOperation(other.getType(), op, mytype);}
	@Targeted
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
		if(first.hasMacro() || second.hasMacro()) {
			VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_2), s.getTarget(), "nbt equality test", c);
		}
		Variable temp = new Variable("\"$temp\"",first.getType(),null,Mask.STORAGE,"mcpp:temp","\"$temp\"");
		String dtemp=temp.dataPhrase();
		String nbtcmd1=first.fromCMDStatement(s.getTarget());
		String nbtcmd2=second.fromCMDStatement(s.getTarget());
		String macroprefix1 = first.hasMacro()?"$":"";
		String macroprefix2 = second.hasMacro()?"$":"";
		p.printf("%sdata modify %s set %s\n",macroprefix1,dtemp,nbtcmd1);
		//int ex = stack.reserve(1);Register rex = stack.getRegister(ex);
		String cmd = "data modify %s set %s".formatted(dtemp,nbtcmd2);
		p.print(macroprefix2);
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
	
	
	public void setVarToNumber(PrintStream p,Scope s,RStack stack, Number val,Variable self) throws CompileError {throw new CompileError.CannotSet(self.type, "number");}
	public void setRegistersToNumber(PrintStream p,Compiler c,Scope s, RStack stack,int home,Number val,VarType myType)throws CompileError {throw new CompileError.CannotSet(myType, "number");}
	public void setVarToBool(PrintStream p,Scope s,RStack stack, boolean val,Variable self)throws CompileError{throw new CompileError.CannotSet(self.type, "bool");}
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
	 * @param tg TODO
	 * @param var
	 * @param fillWithDefaultvalue
	 * @throws CompileError
	 */
	public abstract void allocateLoad(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError;
	public abstract void allocateCall(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError;
	
	//all the members of the implimentation buffet just affect the member tag;
	//all members of self do not see the recursive nature, they are called as if on load

	protected void allocateArrayLoad(PrintStream p, VTarget tg, Variable var,boolean fillWithDefaultvalue,int size, VarType elementType) throws CompileError {
		if(var.isRecursive())
			var.allocateLoadBasic(p, fillWithDefaultvalue, DEFAULT_LIST);
		else {
			this.allocateArray(p, tg, var, fillWithDefaultvalue, size, elementType);
		}
		//(could also prepend / insert)
		Variable idxbuff = this.getIndexVarBuff(var, 0);
		if(idxbuff!=null)idxbuff.allocateLoad(p,tg, fillWithDefaultvalue);
	}
	protected void allocateArrayCall(PrintStream p, VTarget tg, Variable var,boolean fillWithDefaultvalue,int size, VarType elementType) throws CompileError {
		if(var.isRecursive()) {
			var.allocateCallBasic(p, tg, fillWithDefaultvalue, DEFAULT_LIST);
			this.allocateArray(p, tg, var, fillWithDefaultvalue, size, elementType);
		}
		else {
			//this should never be called
			this.allocateArray(p, tg, var, fillWithDefaultvalue, size, elementType);
		}
		//(could also prepend / insert)
		Variable idxbuff = this.getIndexVarBuff(var, 0);
		if(idxbuff!=null)idxbuff.allocateCall(p, tg, fillWithDefaultvalue);
	}
	/**
	 * an implimentation of allocate() for types stored as arrays
	 * @param p
	 * @param tg TODO
	 * @param var
	 * @param fillWithDefaultvalue
	 * @param size
	 * @param elementType
	 * @throws CompileError 
	 */
	@Targeted
	private void allocateArray(PrintStream p, VTarget tg, Variable var,boolean fillWithDefaultvalue,int size, VarType elementType) throws CompileError {
		//can set to empty array []
		//then append 1 element to fix the type
		//the type is fixed once the first element is added
		//append ops on an empty tag will make the tag a list and add the first element
		//but index set ops do not create sub list for you
		//cannot change type of first element to change array type
		
		
		if(var.getMaskType()!=Mask.STORAGE) {
			Warnings.warningf(null,"attempted to deallocate %s to non-storage %s;",var.name, var.getMaskType());
			return;
		}
		p.printf("data modify %s set value %s\n",var.dataPhrase(), DEFAULT_LIST);
		if(size==0) return;
		//TODO double check that lists within lists can have differente element types
		//TODO allocate member if they are structs
		String subname="\"$%s\".\"$allocate_fill\"".formatted(this.name);
		Variable fill=new Variable(subname, elementType, null, new ResourceLocation("mcppc","load__allocate"));
		if(fill.willAllocateOnLoad(fillWithDefaultvalue)) {
			fill.allocateLoad(p,tg, fillWithDefaultvalue);
			for(int i=0;i<size;i++) p.printf("data modify %s append from %s\n",var.dataPhrase(), fill.dataPhrase());
			fill.deallocateLoad(p, tg);
		}else {
			String fillstr = elementType.defaultValue();
			for(int i=0;i<size;i++) p.printf("data modify %s append value %s\n",var.dataPhrase(), fillstr);
		}
		
	}

	protected void allocateCompoundLoad(PrintStream p, VTarget tg, Variable var,boolean fillWithDefaultvalue, List<String> fieldNames)  throws CompileError{
		if(var.isRecursive())
			var.allocateLoadBasic(p, fillWithDefaultvalue, DEFAULT_LIST);
		else {
			this.allocateCompound(p, tg, var, fillWithDefaultvalue, fieldNames);
		}
	}

	protected void allocateCompoundCall(PrintStream p, VTarget tg, Variable var,boolean fillWithDefaultvalue, List<String> fieldNames)  throws CompileError{
		if(var.isRecursive()) {
			var.allocateCallBasic(p, tg, fillWithDefaultvalue, DEFAULT_LIST);
			this.allocateCompound(p, tg, var, fillWithDefaultvalue, fieldNames);
		}
		else {
			//this should never be called
			this.allocateCompound(p, tg, var, fillWithDefaultvalue, fieldNames);
		}
	}
	@Targeted
	private void allocateCompound(PrintStream p, VTarget tg, Variable var,boolean fillWithDefaultvalue, List<String> fieldNames)  throws CompileError{

		//data modify <var> set value {}

		if(var.getMaskType()!=Mask.STORAGE) {
			Warnings.warningf(null,"attempted to deallocate %s to non-storage %s;",var.name, var.getMaskType());
			return;
		}
		p.printf("data modify %s set value %s\n",var.dataPhrase(), DEFAULT_COMPOUND);
		//or data remove <this> will also work, as sets to members will create sub-compounds
		for(String name:fieldNames) if(this.hasField(var, name)){
			Variable field=this.getField(var, name);
			field.allocateLoad(p,tg, fillWithDefaultvalue);
		}
	}
	//use //var.allocateLoadBasic(p, fillWithDefaultvalue, DEFAULT_STRING);
	@Deprecated private void allocateString(PrintStream p, Variable var, boolean fillWithDefaultvalue)  throws CompileError{
		//data modify <var> set value ""
		if(var.getMaskType()!=Mask.STORAGE) {
			Warnings.warningf(null,"attempted to deallocate %s to non-storage %s;",var.name, var.getMaskType());
			return;
		}
		if(fillWithDefaultvalue)p.printf("data modify %s set value %s\n",var.dataPhrase(), DEFAULT_STRING);
		else ;//do nothing
		
	}
	//TODO allocate fixed array of scores
	/**
	 * execute to delete storage for this var
	 * DO NOT CALL THIS FOR AN ARRAY MEMBER OR IT WILL CHANGE THE ARRAY SIZE
	 * ALSO DO NOT CALL THIS METHOD FOR SUB FIELDS, it is not needed
	 * @param p
	 * @param tg TODO
	 * @param var
	 * @throws CompileError 
	 */
	public void deallocateLoad(PrintStream p, VTarget tg, Variable var) throws CompileError {
		var.basicdeallocateBoth(p, tg);
		//data remove <var>
	}
	public void deallocateAfterCall(PrintStream p, VTarget tg, Variable var) throws CompileError {
		//this one is supposed to change the list size
		var.basicdeallocateBoth(p, tg);
		//data remove <var>
	}
	public abstract String getDefaultValue (VarType var) throws CompileError;
	@Targeted public static final String DEFAULT_STRING="\"\"";
	@Targeted public static final String DEFAULT_LIST="[]";
	@Targeted public static final String DEFAULT_COMPOUND="{}";
	/*
	 * members:
	 */
	public abstract boolean hasField(Variable self,String name);
	public abstract Variable getField(Variable self,String name) throws CompileError;
	
	public boolean canIndexMe(Variable self, INbtValueProvider index) throws CompileError{
		//default: const int only
		Number num = index instanceof Token? Num.getNumber((Token) index, VarType.INT) : null;
		if(num!=null) {
			return this.canIndexMe(self, num.intValue());
		}
		return false;
	}
	public boolean canIndexMe(Variable self, int i) throws CompileError{
		return false;
	}
	@Deprecated
	public boolean canIndexMe(Variable self, Variable index) throws CompileError{
		return false;
	}
	public Variable getIndexVarBuff(Variable self,int depth) {
		return null;//deprecated so far
	}
	@Deprecated
	public Variable getIndexRef(Variable self,INbtValueProvider index) throws CompileError{
		//default: const int only
		Variable var = Variable.getVariable(index);
		if(var!=null) {
			return this.getIndexRef(self, var);
		}
		Number num = index instanceof Token? Num.getNumber((Token) index, VarType.INT) : null;
		if(num!=null) {
			return this.getIndexRef(self, num.intValue());
		}
		throw new CompileError.VarNotFound(this, (Token)index);
	}
	public Token convertIndexGet(Variable self,Equation index) throws CompileError{
		throw new CompileError.VarNotFound(this, index);
	}
	public Token convertIndexSet(Variable self,Equation index, Equation setTo) throws CompileError{
		//just give the call token, not the statement
		throw new CompileError.VarNotFound(this, index);
	}
	public Variable getIndexRef(Variable self,int index) throws CompileError{throw new CompileError.VarNotFound(this, index);};
	public Variable getIndexRef(Variable self,Variable index) throws CompileError{throw new CompileError.VarNotFound(this, index);};

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
	protected boolean hasStaticBuiltinMethodBasic(String name,Map<String,BuiltinFunction> mds) {
		return mds.containsKey(name);
	}
	protected BuiltinFunction getStaticBuiltinMethodBasic(String name,VarType type,Map<String,BuiltinFunction> mds) throws CompileError {
		return mds.get(name);
	}
	
	public BuiltinConstructor getConstructor(VarType myType) throws CompileError {
		throw new CompileError("no constructor defined for type %s;".formatted(myType.asString()));
	}
	private static final String NEW= "\"$Vector\".\"$new\"";
	public static  Variable newobj(Compiler c,BFCallToken tk) {
		Variable v=new Variable(NEW, tk.getStaticType(), null,c.resourcelocation);
		return v;
	}
	public boolean canSetToExpr(ConstExprToken e) {
		return false;
	}
	public void setMeToExpr(PrintStream p,Scope s,RStack stack, Variable me, ConstExprToken e) throws CompileError {
		throw new CompileError.UnsupportedCast(e, me.type);
	}
	@Targeted
	public void setMeToNbtExprBasic(PrintStream p,VTarget tg,RStack stack, Variable me, ConstExprToken e) throws CompileError {
		assert e.constType() == ConstType.NBT;
		assert me.getMaskType().isNbt;
		p.printf("data modify %s set value %s\n",me.dataPhrase(), e.textInMcf(tg));
		
	}
	public boolean canSetToMacro(Variable me,Macro e) {
		
		if(this.isDataEquivalent(me.type)) return this.canCasteFrom(me.type, e.getType());
		else return false;
	}
	public void setMeToMacro(PrintStream p,Scope s,RStack stack, Variable me, Macro e) throws CompileError {
		if(this.isDataEquivalent(me.type)) this.setMeToMacroBasic(p, s.getTarget(), stack, me, e);
		else throw new CompileError("cannot set non-data type to macro");
	}
	@Targeted
	protected void setMeToMacroBasic(PrintStream p,VTarget tg,RStack stack, Variable me, Macro e) throws CompileError {
		assert me.getMaskType().isNbt;
		p.printf("$data modify %s set %s\n",me.dataPhrase(), e.fromCMDStatement(tg));
		
	}
	@Targeted
	public void trueReturnMe(PrintStream p, Scope s, RStack stack,Variable me) throws CompileError {
		throw new CompileError("cannot extern-return a struct");
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
