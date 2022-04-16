package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.Objects;

import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Keyword;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;

public class Variable {
	public final String name;
	public final VarType type;
	public final Keyword access;
	
	//mask parameters (non-final)
	
	public static enum Mask{
		STORAGE,ENTITY,BLOCK,SCORE;
	}
	Mask pointsTo;
	/**
	 * below true if var is storage at namespace of parrent file with tag same as varname, or is param
	 */
	private boolean isbasic=true;
	String holderHeader=null;
	//the resourcelocation or player selector(cmd) 
	String holder;
	//the tag address or score:
	String address;
	boolean isParameter=false;
	private boolean isReference=false;
	public boolean isReference() {
		return isReference;
	}
	public String getHolder() {return this.holder;}
	public String getAddress() {return this.holder;}
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
	public Variable parameterOf(Function f,boolean ref) {
		this.address="%s.%s".formatted(f.name,this.name);
		this.holder=f.getResoucrelocation().toString();
		this.isParameter=true;
		this.isReference=ref;
		return this;
	}
	public Variable returnOf(Function f) {
		this.address="%s.%s".formatted(f.name,Function.RET_TAG);
		this.holder=f.getResoucrelocation().toString();
		this.isParameter=true;
		return this;
	}
	public Variable maskEntity(Selector s,NbtPath path) {
		this.isbasic=false;
		this.holder=s.toCMD();
		this.holderHeader=s.toHDR();
		this.address=path.toString();
		this.pointsTo=Mask.ENTITY;
		return this;
	}
	public Variable maskBlock(Coordinates pos,NbtPath path) {
		this.isbasic=false;
		this.holder=pos.asString();
		this.address=path.toString();
		this.pointsTo=Mask.BLOCK;
		return this;
	}
	public Variable maskScore(Selector s,String score) throws CompileError {
		if(this.type.isStruct()) throw new CompileError("cannot mask a struct / class type to scoreboard;");
		this.isbasic=false;
		this.holder=s.toCMD();
		this.holderHeader=s.toHDR();
		this.address=score;
		this.pointsTo=Mask.SCORE;
		return this;
	}
	public Variable maskStorage(ResourceLocation res,NbtPath path) {
		this.isbasic=false;
		//equivalent to default (c.res,varname)
		this.holder=res.toString();
		this.address=path.toString();
		this.pointsTo=Mask.STORAGE;
		return this;
	}
	public void setMe(PrintStream f,RStack stack,int home) throws CompileError {
		if(this.type.isStruct()) {
			if(this.type.struct.canCasteFrom(stack.getVarType(home), this.type.structArgs)) {
				this.type.struct.castRegistersFrom(f, stack, home, stack.getVarType(home), this.type.structArgs);
			}else if(stack.getVarType(home).struct.canCasteTo( this.type,stack.getVarType(home).structArgs)) {
				stack.getVarType(home).struct.castRegistersTo(f, stack, home, this.type, stack.getVarType(home).structArgs);
			}else {
				throw new CompileError.UnsupportedCast(this.type, stack.getVarType(home));
			}
			this.type.struct.setMe(f, stack, home, this);
		}else {
			Register reg=stack.getRegister(home);
			VarType regtype=stack.getVarType(home);
			setMe(f,reg,regtype);
		}
	}
	private void setMe(PrintStream f,Register reg,VarType regType) throws CompileError {
		if(this.type.isVoid())throw new CompileError("Varaible.setMe() not for voids.");
		if(!VarType.areBasicTypesCompadible(this.type, regType)) throw new CompileError.UnsupportedCast(this.type, regType);
		//non structs only; structs need their own routines possibly multiple registers)
		String tagtype=this.type.type.getTagType();
		double mult=Math.pow(10, -regType.getPrecision());//scientific notation may cause problems
		
		switch (this.pointsTo) {
		case STORAGE:{
			f.println("execute store result storage %s %s %s %s run scoreboard players get %s"
					.formatted(this.holder,this.address,tagtype,mult,reg.inCMD()));
		}break;
		case BLOCK:{
			f.println("execute store result block %s %s %s %s run scoreboard players get %s"
					.formatted(this.holder,this.address,tagtype,mult,reg.inCMD()));
		}break;
		case ENTITY:{
			f.println("execute store result entity %s %s %s %s run scoreboard players get %s"
					.formatted(this.holder,this.address,tagtype,mult,reg.inCMD()));
		}break;
		case SCORE:{
			mult=Math.pow(10, -regType.getPrecision()+this.type.getPrecision());
			f.println("execute store result storage %s \"$dumps\".%s %s %s run scoreboard players get %s"
					.formatted(this.holder,this.name,tagtype,mult,reg.inCMD()));
			f.println("execute store result score %s %s run data get storage %s \"$dumps\".%s"
					.formatted(this.holder,this.address,this.holder,this.name));
			
		}break;
		}
	}
	public void getMe(PrintStream p,RStack stack,int home) throws CompileError {
		if(this.type.isStruct()) {
			this.type.struct.getMe(p, stack, home, this);
		}else {
			Register reg=stack.getRegister(home);
			getMe(p,reg);
		}
		
	}
	private void getMe(PrintStream f,Register reg) throws CompileError {
		if(this.type.isVoid())throw new CompileError("Varaible.setMe() not for voids.");

		//non structs only; structs need their own routines possibly multiple registers)
		//String tagtype=this.type.type.getTagType();
		double mult=Math.pow(10, this.type.getPrecision());//scientific notation IS OK
		switch (this.pointsTo) {
		case STORAGE:{
			f.println("execute store result score %s run data get storage %s %s %s"
					.formatted(reg.inCMD(),this.holder,this.address,mult));
		}break;
		case BLOCK:{
			f.println("execute store result score %s run data get block %s %s %s"
					.formatted(reg.inCMD(),this.holder,this.address,mult));
		}break;
		case ENTITY:{
			f.println("execute store result score %s run data get entity %s %s %s"
					.formatted(reg.inCMD(),this.holder,this.address,mult));
		}break;
		case SCORE:{
			f.println("scoreboard players operation %s = %s %s".formatted(reg.inCMD(),this.holder,this.address));
		}break;
		}
	}
	public String dataPhrase() {
		switch (this.pointsTo) {
		case STORAGE:{
			return "storage %s %s".formatted(this.holder,this.address);
		}
		case BLOCK:{
			return "block %s %s".formatted(this.holder,this.address);
		}
		case ENTITY:{
			return "entity %s %s".formatted(this.holder,this.address);
		}
		case SCORE:{
			return null;
		} default:return null;
		}
	}
	public String matchesPhrase(String matchtag) {
		switch (this.pointsTo) {
		case STORAGE:{
			return "storage %s {%s: %s}".formatted(this.holder,this.address,matchtag);
		}
		case BLOCK:{
			return "block %s {%s: %s}".formatted(this.holder,this.address,matchtag);
		}
		case ENTITY:{
			return "entity %s {%s: %s}".formatted(this.holder,this.address,matchtag);
		}
		case SCORE:{
			return null;
		} default:return null;
		}
	}
	public String isTrue() {
		if(this.pointsTo==Mask.SCORE) {
			return "score %s %s matches 1..".formatted(this.holder,this.address);
			
		}
		return this.matchesPhrase("1b");
	}
	public String getJsonText() {
		String edress = PrintF.ESCAPE_TAG_IN_JSON? Regexes.escape(this.address):this.address;
		if(this.type.isStruct())return this.type.struct.getJsonTextFor(this);
		if(this.type.isVoid())return "{\"text\": \"<void>\"}";
		//format string is in vartype
		switch(this.pointsTo) {
		case STORAGE: return "{\"storage\": \"%s\", \"nbt\": \"%s\"}".formatted(this.holder,edress);
		case BLOCK:return "{\"block\": \"%s\", \"nbt\": \"%s\"}".formatted(this.holder,edress);
		case ENTITY:return "{\"entity\": \"%s\", \"nbt\": \"%s\"}".formatted(this.holder,edress);
		case SCORE:
			//TODO will need to change selector to not display multiplies value
			Warnings.warning("getJsonText not yet valid for var with mask SCORE");
			String score="{\"score\": {\"name\": \"%s\", \"objective\": \"%s\"}}".formatted(this.holder,edress);
			String meta="{\"translate\": \"%%se-%%s\",with: [%s,%s]}".formatted(score,this.type.getPrecision());
			return meta;
		
		}
		return null;
		
	}
	public String toHeader() throws CompileError {
		String refsrt = this.isReference?"ref ":"";
		if(this.isParameter || this.isbasic) {
			//dont need to print mask if it is basic
			return "%s%s %s".formatted(refsrt,this.type.asString(),this.name);//mask is inferable
		}
		else switch(this.pointsTo) {
		case BLOCK:
			return "%s%s %s -> %s %s".formatted(refsrt,this.type.asString(),this.name,this.holder,this.address);
		case ENTITY:
			return "%s%s %s -> %s.%s".formatted(refsrt,this.type.asString(),this.name,this.holderHeader,this.address);
		case SCORE:
			return "%s%s %s -> %s::%s".formatted(refsrt,this.type.asString(),this.name,this.holderHeader,this.address);
		case STORAGE:
			return "%s%s %s -> %s.%s".formatted(refsrt,this.type.asString(),this.name,this.holder,this.address);
		default:
			throw new CompileError("null mask");
		
		}
	}
	@Override public boolean equals(Object o) {
		if (o==null) return false;
		if (this ==o)return true;//auto-equal if they point to the same object
		if(o instanceof Variable) {
			Variable ov=(Variable) o;
			return this.name.equals(ov.name)
					&& this.holder.equals(ov.holder)
					&& this.address.equals(ov.address);
		}else return false;
	}
	@Override public int hashCode() {
		return Objects.hash(this.name,this.holder,this.address);
	}
	public void setMeToNumber(PrintStream p,Compiler c,Scope s, RStack stack,Number value) throws CompileError {
		if(!this.type.isNumeric()) throw new CompileError("cannot set variable %s to a number value %s;"
				.formatted(this.toHeader(),value));
		if(this.type.isStruct()) {
			this.type.struct.setVarToNumber(p, c, s, stack, value,this.type.structArgs);
		} else {
			//enforce type
			Number n;
			if(this.type.isFloatP())n=value.doubleValue();
			else n=value.longValue();
			p.printf("data modify %s set value %s\n", this.dataPhrase(),n);
		}
	}
	public void setMeToBoolean(PrintStream p,Compiler c,Scope s, RStack stack,boolean value) throws CompileError {
		if(this.type.isNumeric()) throw new CompileError("cannot set variable %s to a boolean value %s;"
				.formatted(this.toHeader(),value));
		if(this.type.isStruct()) {
			this.type.struct.setVarToBool(p, c, s, stack, value,this.type.structArgs);
		}else {
			p.printf("data modify %s set value %s\n", this.dataPhrase(),value?1:0);
		}
	}
	public static void directSet(PrintStream f,Variable to,Variable from,RStack stack) throws CompileError {
		if(to.type.isStruct())throw new CompileError("Varaible.setMe() does not yet work for structs.");
		if(to.type.isVoid())throw new CompileError("Varaible.setMe() not for voids.");
		if(from.type.isStruct())throw new CompileError("Varaible.setMe() does not yet work for structs.");
		if(from.type.isVoid())throw new CompileError("Varaible.setMe() not for voids.");
		if(to.type.isStruct() && to.type.struct.canCasteFrom(from.type, to.type.structArgs)) {
			to.type.struct.setMeDirect(f, stack, to, from);
			return;
		}else if(from.type.isStruct() && from.type.struct.canCasteTo(to.type, from.type.structArgs)) {
			from.type.struct.getMeDirect(f, stack, to, from);
			return;
		}
			
			
		if(to.type.isNumeric() ^ to.type.isNumeric()) throw new CompileError.UnsupportedCast(from.type, to.type);
		
		boolean floatp = to.type.isFloatP() || from.type.isFloatP();
		if(to.pointsTo==Mask.SCORE) {
			if(from.pointsTo==Mask.SCORE) {
				boolean esgn=(to.type.getPrecision()-from.type.getPrecision())>=0;
				boolean domult=(to.type.getPrecision()-from.type.getPrecision())!=0;
				int mult=(int) Math.pow(10, Math.abs(to.type.getPrecision()-from.type.getPrecision()));//scientific notation may cause problems
				f.printf("scoreboard players operation %s %s =  %s %s\n",to.holder,to.address,from.holder,from.address);
				if(domult && floatp) {
					int extraind=stack.getNext(stack.getTop());Register extra=stack.getRegister(extraind);
					stack.reserve(1);
					f.printf("scoreboard players set %s %d\n",extra.inCMD(),mult);
					if (esgn) f.printf("scoreboard players operation %s %s *=  %s\n",to.holder,to.address,extra.inCMD());
					else f.printf("scoreboard players operation %s %s /=  %s\n",to.holder,to.address,extra.inCMD());
				}
			}else {
				String data=from.dataPhrase();
				double mult=Math.pow(10, to.type.getPrecision());//scientific notation may cause problems
				f.printf("execute store result score %s %s run data get %s %s\n",to.holder,to.address,data,mult);
			}
		}else if(from.pointsTo==Mask.SCORE) {
			String data=to.dataPhrase();
			double mult=Math.pow(10, -from.type.getPrecision());//scientific notation may cause problems
			f.printf("execute store result %s %s run scoreboard players get %s %s\n", data,mult,from.holder,from.address);
		} else {
			String dto=to.dataPhrase();
			String dfrom=from.dataPhrase();
			f.printf("data modify %s set from %s\n",dto,dfrom);
		}
		//USE STRING FORMAT EVEN FOR FLOATS - DONT USE %f BECAUSE IT ROUNDS- SCI NOTATION IS OK IN MCF
	}
}
