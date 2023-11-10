package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.functions.PrintF;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Bool;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Keyword;
import net.mcppc.compiler.tokens.MemberName;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.main.Main;

//note list on data get returns its size
/**
 * Represents a variable in the code; has information about it's type, access, etc.
 * Has lots of usefull methods for things like getting and setting.<p>
 * 
 * Note that currently variable access is not scoped or ordered; 
 * variables defined anywhere within a file are accessible anywhere else in it,
 * with the following exceptions:
 * <ul>
 * 		<li>Function arguments and locals are not accessible outside the function;
 * 		<li>Thread locals are only accessible inside the thread (not including the first block control); private restructs access to the block it is defined in;
 * 
 * </ul>	
 * 
 * @author RadiumE13
 *
 */
public class Variable implements PrintF.IPrintable,INbtValueProvider{
	public final String name;
	public final VarType type;
	public final Keyword access;
	
	//mask parameters (non-final)
	
	public static enum Mask{
		STORAGE(true),ENTITY(true),BLOCK(true),SCORE(false),BOSSBAR(false);
		public final boolean isNbt;
		Mask(boolean isNbt){
			this.isNbt=isNbt;
		}
	}
	Mask pointsTo;
	/**
	 * Typically this is true if the variable's name matches its data path / objective without a custom mask;
	 * used to control if this var will allocate its data;
	 */
	private boolean isbasic=true;
	String holderHeader=null;
	//the resourcelocation or player selector(cmd) 
	String holder;
	//the tag address or score:
	private String address;
	boolean isParameter=false;
	boolean isFuncLocal=false;
	boolean isThreadLocalNonFlowNonVolatile = false;
	private boolean isReference=false;
	private boolean isRecursive=false;
	public boolean isReference() {
		return isReference;
	}
	public void makeFinalThis() {
		this.isReference=false;//dont back copy from a final method
	}
	public boolean isBasic() {
		return this.isbasic;
	}
	public String getHolder() {return this.holder;}
	public String getAddress() {
		//referenced only in compileJob.Namespace.addObjective
		return this.getAddressToGetset();
		//return this.address;
		//should have no index for score
	}
	public String getAddressToGetset() {
		if(this.pointsTo==Mask.SCORE) {
			return this.address;//to recurr score, would have to push vars onto a storage tag
		}
		return this.address + (this.isRecursive? "[0]":"");
	}

	public String getAddressToPrepend() {
		return this.address;
	}
	public Mask getMaskType() {return this.pointsTo;}
	/**
	 * defaults to masking storage
	 * @param name
	 * @param vt
	 * @param access
	 * @param c
	 */
	public Variable(String name,VarType vt, Keyword access, Compiler c) {
		this.name = name;
		this.type = vt;
		this.access=access;
		this.address=name;//scope sensitive
		this.holder=c.resourcelocation.toString();
		this.pointsTo=Mask.STORAGE;
	}
	public Variable(String name,VarType vt, Keyword access, ResourceLocation res) {
		this.name = name;
		this.type = vt;
		this.access=access;
		this.address=name;//scope sensitive
		this.holder=res.toString();
		this.pointsTo=Mask.STORAGE;
	}
	public Variable(String name,VarType vt, Keyword access,Mask pointsTo, String holder,String address) {
		this.name = name;
		this.type = vt;
		this.access=access;
		this.address=address;//scope sensitive
		this.holder=holder;
		this.pointsTo=pointsTo;
	}
	public Variable parameterOf(Function f,boolean ref) throws CompileError{
		this.address="%s.%s".formatted(f.name,this.name);
		this.holder=f.getResoucrelocation().toString();
		this.isParameter=true;
		this.isReference=ref;
		this.isRecursive = f.canRecurr;
		if(this.isRecursive &&!canIBeRecursive())throw new CompileError("Variable %s of type %s cannot be made recursive;".formatted(this.name,this.type.asString()));

		return this;
	}
	public Variable localOf(Function f) throws CompileError{
		this.address="%s.%s".formatted(f.name,this.name);
		this.holder=f.getResoucrelocation().toString();
		this.isReference=false;
		this.isFuncLocal=true;
		this.isRecursive = f.canRecurr;
		if(this.isRecursive &&!canIBeRecursive())throw new CompileError("Variable %s of type %s cannot be made recursive;".formatted(this.name,this.type.asString()));

		return this;
	}public Variable localFlowOf(Function f) throws CompileError{
		//hidden local variables that control flow
		this.address="%s.%s".formatted(f.name,this.name);
		//do not //this.holder=f.getResoucrelocation().toString();
		this.isReference=false;
		this.isFuncLocal=true;
		this.isRecursive = f.canRecurr;
		if(this.isRecursive &&!canIBeRecursive())throw new CompileError("Variable %s of type %s cannot be made recursive;".formatted(this.name,this.type.asString()));

		return this;
	} Variable returnOf(Function f) throws CompileError{
		this.address="%s.%s".formatted(f.name,Function.RET_TAG);
		this.holder=f.getResoucrelocation().toString();
		this.isParameter=true;
		this.isRecursive = f.canRecurr;
		if(this.isRecursive &&!canIBeRecursive())throw new CompileError("Variable %s of type %s cannot be made recursive;".formatted(this.name,this.type.asString()));

		return this;
	} Variable thisOf(Function f) throws CompileError{
		this.address="%s.%s".formatted(f.name,Function.THIS_TAG);
		this.holder=f.getResoucrelocation().toString();
		this.isParameter=true;
		this.isReference=true;//this is always passed by reference unless func is final
		this.isRecursive = f.canRecurr;
		if(this.isRecursive &&!canIBeRecursive())throw new CompileError("Variable %s of type %s cannot be made recursive;".formatted(this.name,this.type.asString()));
		return this;
	}
	Variable stackVarOf(Function f) throws CompileError{
		this.address="%s.%s".formatted(f.name,Function.STACK_TAG);
		this.holder=f.getResoucrelocation().toString();
		this.isParameter=false;
		this.isFuncLocal = true;
		this.isReference=false;//this is always passed by reference unless func is final
		this.isRecursive = true;
		return this;
	}
	private boolean canIBeRecursive() {
		if(this.type.isStruct())return this.type.struct.canBeRecursive(this.type);
		if(this.pointsTo !=Mask.STORAGE)return false;
		return true;
		//if recursive::
		//on load:
		//data modify ns path set value [];
		//on enter function:
		//data modify ns path prepend <default item>
		
		//then run function
			//data get/modify ns path[0] ....
		
		//after leaving function and getting values:
		//data remove ns path[0]
	}
	public Variable maskEntity(Selector s,NbtPath path)  throws CompileError {
		if(this.type.isStruct()) {
			if(!this.type.struct.canMask(this.type, Mask.ENTITY))
				throw new CompileError("cannot mask type %s to a %s;".formatted(this.type.asString(),Mask.ENTITY));
			//else good
		}
		this.isbasic=false;
		this.holder=s.toCMD();
		this.holderHeader=s.toHDR();
		this.address=path.toString();
		this.pointsTo=Mask.ENTITY;
		return this;
	}
	private Selector holderSelfified = null;
	public Variable addSelfification(Selector ats) {
		this.holderSelfified = ats;
		return this;
		
	}
	public boolean canSelfify() {return this.holderSelfified!=null;}
	//change var based on scope if it can be selfified
	public Variable attemptSelfify(Scope s) {
		if(!McThread.DO_SELFIFY) return this;
		//read scope flags to see if this is in thread and not in an as/asat subexecute
		Boolean slf=(Boolean) s.getInheritedParameter(McThread.IS_THREADSELF);
		if(slf==null) return this;
		if(slf && this.canSelfify()) {
			Variable v = new Variable(this.name,this.type,null,this.pointsTo,this.holderSelfified.toCMD(), address);
			v.holderHeader=this.holderSelfified.toCMD();
			return v;
		}
		//Scope::inheritedParams getter
		return this;
	}
	public Variable makeScoreOfThreadRunner() {
		this.isbasic=true;
		return this;
	}
	public Variable makeStorageOfThreadRunner() {
		this.isbasic=true;
		return this;
	}
	public Variable maskBlock(Coordinates pos,NbtPath path) throws CompileError {
		if(this.type.isStruct()) {
			if(!this.type.struct.canMask(this.type, Mask.BLOCK))
				throw new CompileError("cannot mask type %s to a %s;".formatted(this.type.asString(),Mask.BLOCK));
			//else good
		}
		this.isbasic=false;
		this.holder=pos.asString();
		this.address=path.toString();
		this.pointsTo=Mask.BLOCK;
		return this;
	}
	public Variable maskScore(Selector s,String score) throws CompileError {
		if(this.type.isStruct()) {
			if(!this.type.struct.canMask(this.type, Mask.SCORE))
				throw new CompileError("cannot mask type %s to a %s;".formatted(this.type.asString(),Mask.SCORE));
			//else good
		}
		this.isbasic=false;
		this.holder=s.toCMD();
		this.holderHeader=s.toHDR();
		this.address=score;
		this.pointsTo=Mask.SCORE;
		return this;
	}
	public Variable maskEntityScore(Selector s,String score)  throws CompileError {
		if(this.type.isStruct()) {
			if(!this.type.struct.canMask(this.type, Mask.SCORE))
				throw new CompileError("cannot mask type %s to a %s;".formatted(this.type.asString(),Mask.SCORE));
			//else good
		}
		this.isbasic=false;
		this.holder=s.toCMD();
		this.holderHeader=s.toHDR();
		this.address=score;
		this.pointsTo=Mask.SCORE;
		return this;
	}
	public Variable maskStorage(ResourceLocation res,NbtPath path) throws CompileError  {
		if(this.type.isStruct()) {
			if(!this.type.struct.canMask(this.type, Mask.STORAGE))
				throw new CompileError("cannot mask type %s to a %s;".formatted(this.type.asString(),Mask.STORAGE));
			//else good
		}
		this.isbasic=false;
		//equivalent to default (c.res,varname)
		this.holder=res.toString();
		this.address=path.toString();
		this.pointsTo=Mask.STORAGE;
		return this;
	}
	private boolean allocateBossbar = false;
	public Variable maskBossbar(ResourceLocation barId,String defaultNameLit,boolean allocate,boolean isBasic) throws CompileError  {
		if(this.type.isStruct()) {
			if(!this.type.struct.canMask(this.type, Mask.BOSSBAR))
				throw new CompileError("cannot mask type %s to a %s;".formatted(this.type.asString(),Mask.BOSSBAR));
			//else good
		}
		this.isbasic=isBasic;
		this.allocateBossbar=allocate;
		this.holder=barId.toString();
		this.address=defaultNameLit;
		//value|max is stored in the vartype struct
		this.pointsTo=Mask.BOSSBAR;
		return this;
	}
	public Variable maskOtherVar(Variable ref) throws CompileError  {
		if(this.type.isStruct()) {
			if(!this.type.struct.canMask(this.type, ref.pointsTo))
				throw new CompileError("cannot mask type %s to a %s;".formatted(this.type.asString(),ref.pointsTo));
			//else good
		}
		if(!this.type.getNBTTagType().equals(ref.type.getNBTTagType())) 
			throw new CompileError("var (%s) %s cannot mask (%s) %s;"
					.formatted(this.type.getNBTTagType(),this.name,ref.type.getNBTTagType(),ref.name));
			//Warnings.warning("vars %s and %s have different tag types;".formatted(this.name,ref.name), c);
		this.isbasic=false;
		//equivalent to default (c.res,varname)
		this.holder=ref.holder;
		this.holderHeader = ref.holderHeader;
		this.holderSelfified = ref.holderSelfified;
		this.address=ref.address;
		this.pointsTo=ref.pointsTo;
		this.isRecursive=ref.isRecursive; // behave identically to original var
		return this;
	}
	public Variable maskStorageAllocatable(ResourceLocation res,NbtPath path) {
		this.isbasic=true;//can allocate
		//equivalent to default (c.res,varname)
		this.holder=res.toString();
		this.address=path.toString();
		this.pointsTo=Mask.STORAGE;
		return this;
	}

	public void setMe(PrintStream f,Scope s,RStack stack,int home) throws CompileError {
		this.setMe(f,s, stack, home,null);
	}
	public void setMe(PrintStream f,Scope s,RStack stack,int home, VarType regType) throws CompileError {
		//use this one for struct members on stack to stop complaints about register type
		VarType type=(regType==null? stack.getVarType(home):regType);
		if(this.type.isStruct()) {
			Struct struct = this.type.struct;
			if(struct.canCasteFrom(type, this.type)) {
				struct.castRegistersFrom(f, s, stack, home, type, this.type);
			}else if(stack.getVarType(home).isStruct() && stack.getVarType(home).struct.canCasteTo( this.type,type)) {
				stack.getVarType(home).struct.castRegistersTo(f, s, stack, home, this.type, type);
			}else {
				throw new CompileError.UnsupportedCast(this.type, type);
			}
			stack.setVarType(home, this.type);
			struct.setMe(f, s, stack, home, this);
		}else {
			Register reg=stack.getRegister(home);
			VarType regtype=type;
			stack.setVarType(home, this.type);//dont need to actually cast the register, private setMe handles this
			setMe(f,s,reg,regtype);
		}
	}
	public void setMe(PrintStream f,Scope s,Register reg,VarType regType) throws CompileError {
		if(this.type.isVoid())throw new CompileError("Varaible.setMe() not for voids.");
		if(!VarType.areBasicTypesCompadible(this.type, regType)) throw new CompileError.UnsupportedCast(this.type, regType);
		//non structs only; structs need their own routines possibly multiple registers)
		String tagtype=this.type.getNBTTagType();
		double mult=Math.pow(10, -regType.getPrecision(s));//scientific notation may cause problems
		switch (this.pointsTo) {
		case STORAGE:{
			f.println("execute store result storage %s %s %s %s run scoreboard players get %s"
					.formatted(this.holder,this.getAddressToGetset(),tagtype,CMath.getMultiplierFor(mult),reg.inCMD())); 
		}break;
		case BLOCK:{
			f.println("execute store result block %s %s %s %s run scoreboard players get %s"
					.formatted(this.holder,this.getAddressToGetset(),tagtype,CMath.getMultiplierFor(mult),reg.inCMD()));
		}break;
		case ENTITY:{
			f.println("execute store result entity %s %s %s %s run scoreboard players get %s"
					.formatted(this.holder,this.getAddressToGetset(),tagtype,CMath.getMultiplierFor(mult),reg.inCMD()));
		}break;
		case SCORE:{
			int ptot = -regType.getPrecision(s)+this.type.getPrecision(s);
			if(ptot==0) {
				f.printf("scoreboard players operation %s %s = %s\n", this.holder,this.getAddressToGetset(),reg.inCMD());
			} else {

				mult=Math.pow(10, ptot);
				String iholder = "mcppc:scoreholder__/dumps___";
				String ipath = "\"$dump\""; //do not base this on the address
				f.println("execute store result storage %s %s %s %s run scoreboard players get %s"
						.formatted(iholder,ipath,tagtype,CMath.getMultiplierFor(mult),reg.inCMD()));
				f.println("execute store result score %s %s run data get storage %s %s"
						.formatted(this.holder,this.getAddressToGetset(),iholder,ipath));
			}
			
		}break;
		}
	}
	public void setMeToCmd(PrintStream f,Scope s,String cmd) throws CompileError {
		if(this.isStruct()) {
			this.type.struct.setMeToCmd(f,s,this,cmd);
			return;
		}
		String resultsuccess = this.type.isNumeric()? "result":"success";
		this.setMeToCmdBasic(f, s, cmd, resultsuccess);
		
	}
	public void setMeToCmdBasic(PrintStream f,Scope s,String cmd,String resultsuccess) throws CompileError {
		if(this.type.isVoid())throw new CompileError("Varaible.setMe() not for voids.");
		VarType cmdType = this.type;
		//if(!VarType.areBasicTypesCompadible(this.type, cmdType)) throw new CompileError.UnsupportedCast(this.type, cmdType);
		//non structs only; structs need their own routines possibly multiple registers)
		String tagtype=this.type.getNBTTagType();
		double mult=Math.pow(10, -cmdType.getPrecision(s));//scientific notation may cause problems
		switch (this.pointsTo) {
		case STORAGE:{
			f.println("execute store %s storage %s %s %s %s run %s\n"
					.formatted(resultsuccess,this.holder,this.getAddressToGetset(),tagtype,CMath.getMultiplierFor(mult),cmd)); 
		}break;
		case BLOCK:{
			f.println("execute store %s block %s %s %s %s run %s\n"
					.formatted(resultsuccess,this.getAddressToGetset(),tagtype,CMath.getMultiplierFor(mult),cmd));
		}break;
		case ENTITY:{
			f.println("execute store %s entity %s %s %s %s run %s\n"
					.formatted(resultsuccess,this.getAddressToGetset(),tagtype,CMath.getMultiplierFor(mult),cmd));
		}break;
		case SCORE:{
			mult=Math.pow(10, -cmdType.getPrecision(s)+this.type.getPrecision(s));
			String iholder = "mcppc:scoreholder__/dumps___";
			f.println("execute store %s storage %s \"$dumps\".%s %s %s run %s\n"
					.formatted(resultsuccess,iholder,this.name,tagtype,CMath.getMultiplierFor(mult),cmd));
			f.println("execute store result score %s %s run data get storage %s \"$dumps\".%s\n"
					.formatted(this.holder,this.getAddressToGetset(),iholder,this.name));
			
		}break;
		}
	}
	public void getMe(PrintStream p,Scope s,RStack stack,int home) throws CompileError {
		if(this.type.isStruct()) {
			this.type.struct.getMe(p, s, stack, home, this);
		}else {
			Register reg=stack.getRegister(home);
			getMe(p,s,reg);
		}
		
	}
	public void getMe(PrintStream f,Scope s,Register reg) throws CompileError {
		if(this.type.isVoid())throw new CompileError("Varaible.setMe() not for voids.");

		//non structs only; structs need their own routines possibly multiple registers)
		//String tagtype=this.type.type.getTagType();
		double mult=Math.pow(10, this.type.getPrecision(s));//scientific notation IS OK - but NOT HERE, maybe?
		switch (this.pointsTo) {
		case STORAGE:{
			f.println("execute store result score %s run data get storage %s %s %s"
					.formatted(reg.inCMD(),this.holder,this.getAddressToGetset(),CMath.getMultiplierFor(mult)));
		}break;
		case BLOCK:{
			f.println("execute store result score %s run data get block %s %s %s"
					.formatted(reg.inCMD(),this.holder,this.getAddressToGetset(),CMath.getMultiplierFor(mult)));
		}break;
		case ENTITY:{
			f.println("execute store result score %s run data get entity %s %s %s"
					.formatted(reg.inCMD(),this.holder,this.getAddressToGetset(),CMath.getMultiplierFor(mult)));
		}break;
		case SCORE:{
			f.println("scoreboard players operation %s = %s %s".formatted(reg.inCMD(),this.holder,this.getAddressToGetset()));
		}break;
		}
	}
	public String scorePhrase() {
		if(this.getMaskType()!=Mask.SCORE)return null;
		return "%s %s".formatted(this.holder,this.getAddressToGetset());
	}
	public String dataPhrase() {
		switch (this.pointsTo) {
		case STORAGE:{
			return "storage %s %s".formatted(this.holder,this.getAddressToGetset());
		}
		case BLOCK:{
			return "block %s %s".formatted(this.holder,this.getAddressToGetset());
		}
		case ENTITY:{
			return "entity %s %s".formatted(this.holder,this.getAddressToGetset());
		}
		case SCORE:{
			return null;
		} default:return null;
		}
	}
	public String dataGetCmd(double mult) {
		assert this.getMaskType().isNbt;
		return "data get %s %s".formatted(this.dataPhrase(),CMath.getMultiplierFor(mult));
	}

	public String scoreGetCmd() {
		assert this.getMaskType()==Mask.SCORE;
		return "scoreboard players get %s".formatted(this.scorePhrase());
	}
	public String dataPhraseRecursive() {
		switch (this.pointsTo) {
		case STORAGE:{
			return "storage %s %s".formatted(this.holder,this.getAddressToPrepend());
		}
		case BLOCK:{
			return "block %s %s".formatted(this.holder,this.getAddressToPrepend());
		}
		case ENTITY:{
			return "entity %s %s".formatted(this.holder,this.getAddressToPrepend());
		}
		case SCORE:{
			return null;
		} default:return null;
		}
	}
	public String matchesPhrase(String matchtag) {
		//TODO bug {"$printf"."$1": 0} -> {"$printf": {"$1": 0}}
		//also TODO {array[1]: 123} ->? (not array[2]: val)
		String[] names = this.getAddressToGetset().split(".");//TODO stop quote-dots from being problematic
		if(names.length ==0) names = new String[] {this.getAddressToGetset()};
		String tag = matchtag;
		for(int i=names.length-1;i>=0;i--) {
			tag = "{%s: %s}".formatted(names[i],tag);
		}
		//System.err.printf("names(%s) length %s\n",this.getAddressToGetset(), names.length);
		//System.err.printf("names length %s\n", names.length);
		//System.err.printf("tag= %s\n", tag);
		switch (this.pointsTo) {
		case STORAGE:{
			return "data storage %s %s".formatted(this.holder,tag);
		}
		case BLOCK:{
			return "data block %s %s".formatted(this.holder,tag);
		}
		case ENTITY:{
			return "data entity %s %s".formatted(this.holder,tag);
		}
		case SCORE:{
			return null;
		} default:return null;
		}
	}
	public String isTrue() throws CompileError {
		switch (this.pointsTo) {
		case SCORE:return "score %s %s matches 1..".formatted(this.holder,this.getAddressToGetset());

		case BLOCK:
		case ENTITY:
		case STORAGE:
			return this.matchesPhrase("1b");
		case BOSSBAR:
		default:
			throw new CompileError("Variable masking a bossbar has no truth test");
		
		}
	}
	@Override //IPrintable
	public String getJsonTextSafe() {
		try {
			return this.getJsonText();
		} catch (CompileError e) {
			e.printStackTrace();
			return null;
		}
	}
	public String getJsonText() throws CompileError {
		if(this.type.isStruct())return this.type.struct.getJsonTextFor(this);
		if(this.type.isVoid())return "{\"text\": \"<void>\"}";
		return this.getJsonTextBasic();
	}
	public String getJsonTextBasic() throws CompileError {
		String edress = PrintF.ESCAPE_TAG_IN_JSON? Regexes.escape(this.getAddressToGetset()):this.getAddressToGetset();
		//format string is in vartype
		switch(this.pointsTo) {
		case STORAGE: return "{\"storage\": \"%s\", \"nbt\": \"%s\"}".formatted(this.holder,edress);
		case BLOCK:return "{\"block\": \"%s\", \"nbt\": \"%s\"}".formatted(this.holder,edress);
		case ENTITY:return "{\"entity\": \"%s\", \"nbt\": \"%s\"}".formatted(this.holder,edress);
		case SCORE:
			//TODO will need to change selector to not display multiplies value
			Warnings.warning("getJsonText not yet valid for var with mask SCORE", null);
			String score="{\"score\": {\"name\": \"%s\", \"objective\": \"%s\"}}".formatted(this.holder,edress);
			String meta="{\"translate\": \"%%se-%%s\",with: [%s,%s]}".formatted(score,this.type.getPrecisionStr());
			return meta;
		
		}
		return null;
		
	}
	public String toHeader() throws CompileError {
		String refsrt = this.isReference?"ref ":"";
		if(this.isParameter || this.isbasic ) {
			//dont need to print mask if it is basic
			return "%s%s %s".formatted(refsrt,this.type.headerString(),this.name);//mask is inferable
		}
		else switch(this.pointsTo) {
		case BLOCK:
			return "%s%s %s -> %s %s".formatted(refsrt,this.type.headerString(),this.name,this.holder,this.address);//here go with the true address
		case ENTITY:
			return "%s%s %s -> %s.%s".formatted(refsrt,this.type.headerString(),this.name,this.holderHeader,this.address);//here go with the true address
		case SCORE:
			return "%s%s %s -> %s::%s".formatted(refsrt,this.type.headerString(),this.name,this.holderHeader,this.address);//here go with the true address
		case STORAGE:
			return "%s%s %s -> %s.%s".formatted(refsrt,this.type.headerString(),this.name,this.holder,this.address);//here go with the true address
		case BOSSBAR:
			String xtrn = this.allocateBossbar? "":" %s".formatted(Keyword.EXTERN.name);
			return "%s%s %s -> %s---%s%s".formatted(refsrt,this.type.headerString(),this.name,this.holder,this.address,xtrn);//here go with the true address

		default:
			throw new CompileError("null mask");
		
		}
	}
	public void setMeToNumber(PrintStream p,Compiler c,Scope s, RStack stack,Number value) throws CompileError {
		if(!this.type.isNumeric()) throw new CompileError("cannot set variable %s to a number value %s;"
				.formatted(this.toHeader(),value));
		if(this.type.isStruct()) {
			this.type.struct.setVarToNumber(p, c, s, stack, value,this);
		} else {
			//enforce type
			if(this.pointsTo == Mask.SCORE) {
				int pcs = this.type.getPrecision(s);
				int scoreValue = pcs!=0 ? (int) Math.round(value.doubleValue() * Math.pow(10, pcs))
						: value.intValue();
				p.printf("scoreboard players set %s %d\n", this.scorePhrase(),scoreValue);
			}else {
				p.printf("data modify %s set value %s\n", this.dataPhrase(),this.type.numToString(value));
			}
		}
	}
	public void setMeToBoolean(PrintStream p,Compiler c,Scope s, RStack stack,boolean value) throws CompileError {
		if(this.type.isNumeric()) throw new CompileError("cannot set variable %s to a boolean value %s;"
				.formatted(this.toHeader(),value));
		if(this.type.isStruct()) {
			this.type.struct.setVarToBool(p, c, s, stack, value,this);
		}else {
			if(this.pointsTo == Mask.SCORE) {
				int pcs = this.type.getPrecision(s);
				int scoreValue = value? 1 : 0;
				p.printf("scoreboard players set %s %d\n", this.scorePhrase(),scoreValue);
			}else {
				p.printf("data modify %s set value %s\n", this.dataPhrase(),this.type.boolToStringNumber(value));
			}
		}
	}
	public void setMeToNbtValueBasic(PrintStream p,Compiler c,Scope s, RStack stack,String value) throws CompileError {
		if(this.getMaskType() != Mask.STORAGE) throw new CompileError("cannot set nbt value of a score variable %s;".formatted(this.name));
		p.printf("data modify %s set value %s\n", this.dataPhrase(),value);
	}
	public static void directSet(PrintStream f,Scope s,Variable to,Variable from,RStack stack) throws CompileError {
		if(to.type.isVoid())throw new CompileError("Varaible.setMe() not for voids.");
		if(from.type.isVoid())throw new CompileError("Varaible.setMe() not for voids.");
		if(to.type.isStruct() && to.type.struct.canCasteFrom(from.type, to.type)) {
			to.type.struct.setMeDirect(f, s, stack, to, from);
			to.type.struct.castVarFrom(f, s, stack, from, from.type, to.type);
			return;
		}else if(from.type.isStruct() && from.type.struct.canCasteTo(to.type, from.type)) {
			from.type.struct.getMeDirect(f, s, stack, to, from);
			from.type.struct.castVarTo(f, s, stack, from, from.type, to.type);
			return;
		}
			
			
		if(!VarType.areBasicTypesCompadible(to.type, from.type)) throw new CompileError.UnsupportedCast(from.type, to.type);
		
		if(VarType.canDirectSetBasicTypes(to.type, from.type)) {
			trueDirectSet(f,s, to, from, stack);
		}else {
			//resort to indirect set
			castDirectSet(f,s, to, from, stack);
		}
	}

	public static void directOpOn(PrintStream p,Compiler c,Scope s,Variable to,BiOperator op,INbtValueProvider from,RStack stack) throws CompileError {
		Variable.directOpOn(p,c, s, to,op, from, stack,false);
	}
	public static void directOpOn(PrintStream p,Compiler c,Scope s,Variable to,BiOperator op,INbtValueProvider from,RStack stack,boolean mustBeDirect) throws CompileError {
		if(to.type.isVoid())throw new CompileError("Varaible.directOp() not for voids.");
		if(from.getType().isVoid())throw new CompileError("Varaible.directOp() not for voids.");
		if(to.type.isStruct() && to.type.struct.canDoBiOpDirectOn(op, to.type, from.getType())) {
			to.type.struct.doBiOpFirstDirectOn(op, to.type, p, c, s, stack, to, from);
			return;
		}
		if(mustBeDirect)throw new CompileError.UnsupportedOperation(to.type, op, from.getType());
		Token fromt = from instanceof Variable? ((Variable) from).basicMemberName(s) : (Token) from;
		Equation eq=Equation.toAssignHusk(stack, to.basicMemberName(s),op,fromt);
		eq.compileOps(p, c, s, to.type);
		eq.setVar(p, c, s, to);
	}public static int directOp(PrintStream p,Compiler c,Scope s,INbtValueProvider prevVar,BiOperator op,INbtValueProvider newVar,RStack stack) throws CompileError {
		return Variable.directOp(p,c, s, prevVar,op, newVar, stack,true);
	}
	private static int directOp(PrintStream p,Compiler c,Scope s,INbtValueProvider prevVar,BiOperator op,INbtValueProvider newVar,RStack stack,boolean mustBeDirect) throws CompileError {
		if(prevVar.getType().isVoid())throw new CompileError("Varaible.directOp() not for voids.");
		if(newVar.getType().isVoid())throw new CompileError("Varaible.directOp() not for voids.");
		if(prevVar.getType().isStruct() && prevVar.getType().struct.canDoBiOpDirect(op, prevVar.getType(), newVar.getType(), true)) {
			return prevVar.getType().struct.doBiOpFirstDirect( op, prevVar.getType(),p, c, s, stack, prevVar, newVar);
		}else if(newVar.getType().isStruct() && newVar.getType().struct.canDoBiOpDirect(op, newVar.getType(), prevVar.getType(), false)) {
			return prevVar.getType().struct.doBiOpSecondDirect(op,newVar.getType(), p, c, s, stack, prevVar, newVar);
		}
		if(mustBeDirect)throw new CompileError.UnsupportedOperation(prevVar.getType(), op, newVar.getType());
		//throw new CompileError.UnsupportedOperation(prevVar.getType(), op, newVar.getType());
		Token t1 = prevVar instanceof Variable? ((Variable) prevVar).basicMemberName(s) : (Token) prevVar;
		Token t2 = newVar instanceof Variable? ((Variable) newVar).basicMemberName(s) : (Token) newVar;
		Equation eq=Equation.toAssignHusk(stack, t1,op,t2);
		eq.compileOps(p, c, s, null);
		return eq.setReg(p, c, s, eq.retype);
		
	}
	public MemberName basicMemberName(Scope s) {
		MemberName t=new MemberName(-1,-1,this.name);
		t.identifyWith(this);
		return t;
	}
	public MemberName memberName(Scope s,Token pos,String nameOverload) {
		MemberName t=new MemberName(pos.line,pos.col,nameOverload);
		t.identifyWith(this);
		return t;
	}
	private static void castDirectSet(PrintStream f,Scope s,Variable to,Variable from,RStack stack) throws CompileError {
		int i=stack.setNext(from.type);
		from.getMe(f,s, stack, i);
		to.setMe(f,s, stack, i);
		stack.pop();
		// may be able to remove intermediary score but will still need multipliers
		
	}

	private static void trueDirectSet(PrintStream f,Scope s,Variable to,Variable from,RStack stack) throws CompileError {
		boolean floatp = to.type.isFloatP() || from.type.isFloatP();
		if(to.pointsTo==Mask.SCORE) {
			if(from.pointsTo==Mask.SCORE) {
				boolean esgn=(to.type.getPrecision(s)-from.type.getPrecision(s))>=0;
				boolean domult=(to.type.getPrecision(s)-from.type.getPrecision(s))!=0;
				int mult=(int) Math.pow(10, Math.abs(to.type.getPrecision(s)-from.type.getPrecision(s)));//scientific notation may cause problems
				f.printf("scoreboard players operation %s %s =  %s %s\n",to.holder,to.getAddressToGetset(),from.holder,from.getAddressToGetset());
				if(domult && floatp) {
					int extraind=stack.getNext(stack.getTop());Register extra=stack.getRegister(extraind);
					stack.reserve(1);
					f.printf("scoreboard players set %s %d\n",extra.inCMD(),CMath.getMultiplierFor(mult));
					if (esgn) f.printf("scoreboard players operation %s %s *=  %s\n",to.holder,to.getAddressToGetset(),extra.inCMD());
					else f.printf("scoreboard players operation %s %s /=  %s\n",to.holder,to.getAddressToGetset(),extra.inCMD());
				}
			}else {
				String data=from.dataPhrase();
				double mult=Math.pow(10, to.type.getPrecision(s));//scientific notation may cause problems
				f.printf("execute store result score %s %s run data get %s %s\n",to.holder,to.getAddressToGetset(),data,CMath.getMultiplierFor(mult));
			}
		}else if(from.pointsTo==Mask.SCORE) {
			String data=to.dataPhrase();
			double mult=Math.pow(10, -from.type.getPrecision(s));//scientific notation may cause problems
			String type = to.type.getNBTTagType();
			//TODO bugged has no type
			f.printf("execute store result %s %s %s run scoreboard players get %s %s\n", data,type,CMath.getMultiplierFor(mult),from.holder,from.getAddressToGetset());
		} else {
			String dto=to.dataPhrase();
			String dfrom=from.dataPhrase();
			f.printf("data modify %s set from %s\n",dto,dfrom);
		}
		//USE STRING FORMAT EVEN FOR FLOATS - DONT USE %f BECAUSE IT ROUNDS- SCI NOTATION IS OK IN MCF (except for in multipliers - its a bug with mc)
	}
	/**
	 * does not check for indexed refs
	 * @param c
	 * @param s
	 * @param matcher
	 * @param line
	 * @param col
	 * @return
	 * @throws CompileError
	 */
	public static Variable checkForVar(Compiler c,Scope s, Matcher matcher, int line, int col) throws CompileError {
		int start=c.cursor;
		Token vn = c.nextNonNullMatch(Factories.checkForMembName);
		if(!(vn instanceof MemberName)) {
			c.cursor=start; return null;
		}
		
		((MemberName) vn).identify(c, s);
		Variable v=((MemberName) vn).getVar();
		return v;
		
	}

	public boolean canSetToExpr(ConstExprToken e) {
		if(this.type.isStruct()) {
			return this.type.struct.canSetToExpr(e);
		}else if(e.constType()==ConstType.BOOLIT) return this.type.isLogical();
		else if(e.constType()==ConstType.NUM) return this.type.isNumeric();		
		else return false;
	}
	public void setMeToExpr(PrintStream p, RStack stack, ConstExprToken e) throws CompileError {
		if(this.type.isStruct()) {
			this.type.struct.setMeToExpr(p, stack, this, e);
		}else if (e instanceof Bool) {
			this.setMeToBoolean(p, null, null, stack, ((Bool)e).val);
		}else if (e instanceof Num) {
			this.setMeToNumber(p, null, null, stack, ((Num)e).value);
		}
		
	}
	public Variable indexMyNBTPath(int index) throws CompileError {
		if(this.isStruct()) {
			if(this.type.struct.canIndexMe(this, index)) return this.type.struct.getIndexRef(this, index);
			else throw new CompileError("Index %d invalid for type %s;".formatted(index,this.type));
		}else {
			throw new CompileError("Basic type %s is not indexable;".formatted(this.type));
		}
	}
	public Variable indexMyNBTPathBasic(int index,VarType membtype) {
		return new Variable("%s[%s]".formatted(this.name,index),
				membtype,
				this.access,
				this.pointsTo,
				this.holder,
				"%s[%s]".formatted(this.getAddressToGetset(),index)
				);
	}
	public Variable fieldMyNBTPath(String field,VarType type) {
		return new Variable("%s.%s".formatted(this.name,field),
				type,
				this.access,
				this.pointsTo,
				this.holder,
				this.fieldMyNBTPathAddress(field, type) // fields revive the getset address
				);
	}
	public String fieldMyNBTPathAddress(String field,VarType type) {
		return "%s.%s".formatted(this.getAddressToGetset(),field);
	}
	/**
	 * makes a new objective for a static number of elements (such as Vector or Uuid) and returns a variable that masks it
	 * @param index
	 * @param membtype
	 * @return
	 */
	public Variable indexMyScoreBasic(int index,VarType membtype) {
		return new Variable("%s[%s]".formatted(this.name,index),
				membtype,
				this.access,
				this.pointsTo,
				this.holder,
				"%s+_%d".formatted(this.getAddressToGetset(),index)
				);
	}
	public boolean isStruct() {
		return this.type.isStruct();
	}
	public boolean hasField(String name) {
		if(!this.isStruct()) return false;
		return this.type.struct.hasField(this, name);
	}

	public Variable getField(String name) throws CompileError {
		if(!this.isStruct()) throw new CompileError.VarNotFound(type, name);
		return this.type.struct.getField(this, name);
	}
	/**
	 * Variables are considered equal if they point to the same destination and have the same type;
	 * they need not have the same name, finality, refness, ...
	 */
	@Override public boolean equals(Object o) {
		if (o==null) return false;
		if (this ==o)return true;//auto-equal if they point to the same object
		if(o instanceof Variable) {
			Variable ov=(Variable) o;
			return  this.areRefsEqual(ov)&&this.areTypesEqual(ov);
		}else return false;
	}
	public boolean areRefsEqual(Variable ov) {
		return this.pointsTo==ov.pointsTo
				&&this.holder.equals(ov.holder)
				&& this.address.equals(ov.address);//ok
	}
	public boolean areTypesEqual(Variable ov) {
		return this.type.equals(ov.type);
	}
	@Override public int hashCode() {
		return Objects.hash(this.name,this.holder,this.address);//ok
	}
	
	/**
	 * returns true if this var should allocate itself at 
	 * 
	 * @return true if this var has no explicit mask (masks are assumed to already be existing locations)
	 */
	public boolean willAllocateOnLoad(boolean fillWithDefaultvalue) {
		if(this.isRecursive)return true;
		if(this.type.isStruct())
			return (this.isbasic || this.allocateBossbar)  && this.type.struct.willAllocateLoad(this, fillWithDefaultvalue);//usually true
		else if (this.pointsTo==Mask.SCORE && this.isbasic) return true;
		return this.isbasic  && fillWithDefaultvalue;
	}
	/**
	 * returns true if this var should allocate itself before a function call it is local to; used for recursion;
	 * @param fillWithDefaultvalue
	 * @return
	 */
	public boolean willAllocateOnCall(boolean fillWithDefaultvalue) {
		return this.isRecursive;
	}
	/**
	 * Allocates this variable; behavior is controlled by type but typical behavior is as follows:
	 * if it is a score, create the objective;
	 * if it is storage, set a default value and make sure all parent tags are compound;
	 * 
	 * @param p
	 * @param fillWithDefaultvalue
	 * @throws CompileError
	 */
	public void allocateLoad(PrintStream p,boolean fillWithDefaultvalue) throws CompileError {
		if(this.type.isStruct()) {
			this.type.struct.allocateLoad(p, this, fillWithDefaultvalue);
			return;
		}
		if(this.type.isVoid())return;//skip
		this.allocateLoadBasic(p,fillWithDefaultvalue,this.type.defaultValue());
		
	}
	public void allocateLoadBasic(PrintStream p,boolean fillWithDefaultvalue,String defaultValue) throws CompileError  {
		if(!this.isbasic) {
			Warnings.warningf(null,"attempted to allocate %s to non-basic %s;",this.name, this.pointsTo);
			return;
		}
		if(this.pointsTo == Mask.SCORE && this.isbasic) {
			String objective = this.address;
			p.printf("scoreboard objectives add %s dummy\n",objective);
			return;
		}
		if(this.pointsTo!=Mask.STORAGE) {
			Warnings.warningf(null,"attempted to allocate %s to non-storage %s;",this.name, this.pointsTo);
			return;
		}
		String data = this.isRecursive ?
				 this.dataPhraseRecursive()
				:this.dataPhrase();
		String value = this.isRecursive ?
				 Struct.DEFAULT_LIST
				:defaultValue; 
		p.printf("data modify %s set value %s\n",data, value);
	}
	/**
	 * allocates before a function call; typical behavior is as follows:
	 * if the variable is recursive then prepend a default value to a virual NBT 'stack',
	 * else do something similar to {@link Variable#allocateLoad};
	 * @param p
	 * @param fillWithDefaultvalue
	 * @throws CompileError
	 */
	public void allocateCall(PrintStream p,boolean fillWithDefaultvalue) throws CompileError {
		if(this.type.isStruct()) {
			this.type.struct.allocateCall(p, this, fillWithDefaultvalue);
			return;
		}
		if(this.type.isVoid())return;//skip
		this.allocateCallBasic(p, fillWithDefaultvalue,this.type.defaultValue());
		
		
	}
	public void allocateCallBasic(PrintStream p,boolean fillWithDefaultvalue,String defaultValue) throws CompileError {
		if(!this.isbasic) {
			Warnings.warningf(null,"attempted to allocate %s to non-basic %s;",this.name, this.pointsTo);
			return;
		}
		if(this.pointsTo!=Mask.STORAGE) {
			Warnings.warningf(null,"attempted to allocate %s to non-storage %s;",this.name, this.pointsTo);
			return;
		}
		if(this.isRecursive) {
			p.printf("data modify %s prepend value %s\n",this.dataPhraseRecursive(), defaultValue);
		}else {
			p.printf("data modify %s set value %s\n",this.dataPhrase(), defaultValue);
		}
	}
	public void makeObjective(PrintStream p) throws CompileError {
		if(this.type.isStruct()) {
			Warnings.warningf(null,"attempted to make objective %s for struct %s;",this.name, this.type.asString());
			return;
		}
		if(this.pointsTo!=Mask.SCORE) {
			Warnings.warningf(null,"attempted to make objective %s to non-score %s;",this.name, this.pointsTo);
			return;
		}
		p.printf("scoreboard objectives add %s dummy\n",this.address);
		
	}
	public void deallocateLoad(PrintStream p) throws CompileError {
		if(this.type.isStruct()) {
			this.type.struct.deallocateLoad(p, this);
			return;
		}
		this.basicdeallocateBoth(p);
		
	}
	public void deallocateAfterCall(PrintStream p) throws CompileError {
		if(this.type.isStruct()) {
			this.type.struct.deallocateAfterCall(p, this);
			return;
		}
		this.basicdeallocateBoth(p);
		
	}
	public void basicdeallocateBoth(PrintStream p) throws CompileError {
		if(this.pointsTo!=Mask.STORAGE) {
			Warnings.warningf(null,"attempted to deallocate %s to non-storage %s;",this.name, this.pointsTo);
			return;
		}
		if(!this.isbasic) {
			Warnings.warningf(null,"attempted to deallocate %s to non-basic %s;",this.name, this.pointsTo);
			return;
		}
		//this should work for both recursive and nonrecursive vars
		p.printf("data remove %s\n", this.dataPhrase());
		
	}
	public boolean isRecursive() {
		return isRecursive;
	}
	public void makeRecursive() {
		this.isRecursive=true;
	}
	@Override
	public boolean hasData() {
		return this.getMaskType().isNbt;
	}
	@Override
	public String fromCMDStatement() {
		return INbtValueProvider.FROM.formatted(this.dataPhrase());
	}
	@Override
	public VarType getType() {
		return this.type;
	}
	public static Variable getVariable(INbtValueProvider stm) {
		if(stm instanceof Variable)return (Variable) stm;
		else if (stm instanceof Equation && ((Equation) stm).isConstRefable())
			return ((Equation) stm).getConstVarRef();
		else return null;
	}
	
}
