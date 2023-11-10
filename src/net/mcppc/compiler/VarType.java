package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.TemplateArgsToken;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Type;
import net.mcppc.compiler.tokens.UnaryOp;
/**
 * a variable type, including type parameters like precision;
 * 
 * Note: strings are a struct type
 * 
 * @author RadiumE13
 *
 */
public class VarType {
	public static final VarType BOOL = new VarType(Builtin.BOOL);
	public static final VarType INT = new VarType(Builtin.INT);
	public static final VarType BYTE = new VarType(Builtin.BYTE);
	public static final VarType SHORT = new VarType(Builtin.SHORT);
	public static final VarType LONG = new VarType(Builtin.LONG);
	public static final VarType FLOAT = new VarType(Builtin.FLOAT);
	public static final VarType DOUBLE = new VarType(Builtin.DOUBLE);
	public static final VarType VOID = new VarType(Builtin.VOID);
	
	public static VarType doubleWith(int precison) {
		return new VarType(Builtin.DOUBLE,precison);
	}
	public static VarType floatWith(int precison) {
		return new VarType(Builtin.FLOAT,precison);
	}
	public static enum Builtin{
		BYTE("byte","0b",true,false,false),
		SHORT("short","0s",true,false,false), //
		INT("int","0",true,false,false), //int must not have suffix
		LONG("long","0L",true,false,false),
		
		FLOAT("float","0.0f",true,true,false),
		DOUBLE("double","0.0d",true,true,false),
		
		BOOL("bool","0b",false,false,true),
		
		STRUCT("struct",false,false,false,true,false),
		
		VOID("void",false,false,false,false,true);
		public boolean isNumber;
		public boolean isFloatP;
		public boolean isLogical;
		public boolean isStruct;
		public boolean isVoid;
		int sizeof = 1;//number of scores needed; currently it is always one
		public String typename;
		String defaultValue;
		Builtin(String name,String value,boolean num,boolean flt,boolean logic){
			this.isNumber=num;
			this.isFloatP=flt;
			this.isLogical=logic;
			this.typename=name;
			this.isStruct=false;
			this.isVoid=false;
			this.defaultValue=value;
		}
		Builtin(String name,boolean num,boolean flt,boolean logic,boolean st,boolean isVoid){
			this.isNumber=num;
			this.isFloatP=flt;
			this.isLogical=logic;
			this.typename=name;
			this.isStruct=st;
			this.isVoid=isVoid;
			this.defaultValue="";
		}
		public String getTagType() throws CompileError {
			if (this.isStruct)throw new CompileError("Struct cannot be set directly to tag");
			if (this.isVoid)throw new CompileError("void cannot be set to tag");
			if (this==BOOL)return BYTE.typename;
			else return this.typename;
		}
		public static Builtin[] valuesNonStruct() {
			return new Builtin[]{BYTE,SHORT,INT,LONG,  FLOAT, DOUBLE,  BOOL,  VOID};
		}
	}
	public static final int DEFAULT_PRECISION = 3;
	public final Builtin type;
	
	private final int precision; //does not affect structs
	public final String precisionTemplateName;
	public boolean isReady(){
		if(this.isStruct())return this.struct.isReady(this);
		else return this.precisionTemplateName==null;
	}//false if there are unknown template args
	
	public final StructTypeParams structArgs;
	
	public final Struct struct;//name of the struct if it is one
	//unimplimented; a struct would be a compile-made data type
	public VarType(Builtin type) {
		this.type = type;
		this.precision=type.isFloatP? DEFAULT_PRECISION:0;
		this.precisionTemplateName=null;
		this.struct=null;
		this.structArgs=null;
	}
	public VarType(Builtin type,int precision) {
		this.type = type;
		this.precision=precision;
		this.precisionTemplateName=null;
		this.struct=null;
		this.structArgs=null;
		
	}
	public VarType(Builtin type,String precisionTemplate) {
		this.type = type;
		this.precision=0;
		this.precisionTemplateName=precisionTemplate;
		this.struct=null;
		this.structArgs=null;
		
	}
	public VarType(Struct type,StructTypeParams params) {
		this.type = Builtin.STRUCT;
		this.struct=type;
		this.structArgs=params;
		this.precision=type.isFloatP? DEFAULT_PRECISION:0;
		this.precisionTemplateName=null;
		
	}
	//only for unready types
	public VarType(Builtin type,Const precision) {
		this.type = type;
		
		this.precision=0;
		this.precisionTemplateName=precision.name;
		this.struct=null;
		this.structArgs=null;
		
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
	public boolean isConstReducable(ConstType ctype) {
		if(ctype==null) return this.struct!=null && this.struct.getConstType(this) != null;
		return this.struct!=null && this.struct.getConstType(this) == ctype;
		
	}
	public boolean isDataEquivalent() {
		return this.struct==null || this.struct.isDataEquivalent(this);
		
	}
	public boolean isScoreEquivalent() {
		return this.canMask(Mask.SCORE);
	}
	public boolean canMask(Variable.Mask mask) {
		if(this.struct!=null)return this.struct.canMask(this, mask);
		return true;//builtins can mask all
	}
	//number of registers
	public int sizeOf() {
		if (this.type.isStruct) {
			return this.struct.sizeOf(this);
		}
		//else if (this.isVoid()) return 0;
		else return 1;//for now, extra precision is ignored
	}
	public int getPrecision(Scope s)  throws CompileError{
		if(this.isStruct())return this.struct.getPrecision(this, s);
		if(s!=null && !this.isReady()) {
			Const c= s.checkForTemplateOrLocalConst(this.precisionTemplateName);
			if(c==null) {
				//System.err.printf("%s;\n", this.precisionTemplateName);
				//System.err.printf("%s;\n", s.template.params.stream().map(cv->cv.name).toList());
				throw new CompileError("Vartype %s nont binded in time".formatted(this.asString()));
			}
			if(c.ctype!=ConstType.NUM) throw new CompileError("Vartype %s binded to a non-number const".formatted(this.asString()));
			Num n= (Num) c.getValue();
			return (int) n.value.intValue();
		}
		if (this.type.isStruct)return this.struct.getPrecision(this, null);
		return this.isFloatP()?this.precision:0;
	}
	public String getPrecisionStr(){
		if(this.isStruct())return this.struct.getPrecisionStr(this);
		if(!this.isReady()) {
			return this.precisionTemplateName;
		}else return Integer.toString(this.precision);
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
			return this.struct.withPrecision(this,newPrecision);
		}
		return this.withPrecisionBasic(newPrecision);
	}
	public VarType withPrecisionBasic(int newPrecision) {
		return new VarType(this.type,newPrecision);
	}
	public VarType withTemplatePrecision(String pc) throws CompileError {
		if (this.isStruct()){
			return this.struct.withTemplatePrecision(this,pc);
		}
		return this.withTemplatePrecisionBasic(pc);
	}
	public VarType withTemplatePrecisionBasic(String pc)  {
		return new VarType(this.type,pc);
	}
	public VarType breakTiesToTemplate(Scope s) throws CompileError {
		if(this.isReady()) return this;//no action needed
		return this.withPrecision(this.getPrecision(s));
	}
	public String getNBTTagType() throws CompileError {
		if(this.isStruct())return this.struct.getNBTTagType(this);
		else return this.type.getTagType();
	}
	public VarType onStack() {
		if(this.isStruct()) return this.struct.getTypeOnStack(this);
		else return this;
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
		return Struct.isStruct(type)? Builtin.STRUCT:null;
		
	}
	//must be 
	public static final String HDRFORMAT=Token.AbstractBracket.ARE_TYPEARGS_PARENS?"%s(%d)":"%s<%d>";
	public static final String HDRFORMATNOTREADY=Token.AbstractBracket.ARE_TYPEARGS_PARENS?"%s(%s)":"%s<%s>";
	public String asString(){
		
		if(this.type.isStruct) return this.struct.asString(this);
		else if(this.type.isFloatP) {
			if(this.isReady())return HDRFORMAT.formatted(this.type.typename,this.precision);
			else return HDRFORMATNOTREADY.formatted(this.type.typename,this.precisionTemplateName); 
		}
		else return this.type.typename;
	}
	@Override public String toString() {return this.asString();}
	public String headerString(){
		if(this.type.isStruct) return this.struct.headerTypeString(this);
		else if(this.type.isFloatP) {
			if(this.isReady())return HDRFORMAT.formatted(this.type.typename,this.precision);
			else return HDRFORMATNOTREADY.formatted(this.type.typename,this.precisionTemplateName); 
		}
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
		this.struct.doUnaryOp(op, this, p, c, s, stack, home);
	}
	public boolean supportsBiOp(BiOperator.OpType op,VarType other, boolean first) throws CompileError {
		return this.struct.canDoBiOp(op, this, other, first);
	}
	public void doBiOpFirst(BiOperator.OpType op,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2) throws CompileError {
		this.struct.doBiOpFirst(op, this, p, c, s, stack, home1, home2);
	}
	public void doBiOpSecond(BiOperator.OpType op,PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2) throws CompileError {
		this.struct.doBiOpSecond(op, this, p, c, s, stack, home1, home2);
	}
	public static boolean canCast(VarType from,VarType to) {
		//new type takes priority
		if(to.isStruct()||from.isStruct()) {
			boolean ret=false;
			if(to.isStruct()) {
				ret=ret||to.struct.canCasteFrom(from, to);
			}
			if(from.isStruct()) {
				ret=ret||from.struct.canCasteTo(from, to);
			}
			return ret;
		}
		return VarType.areBasicTypesCompadible(from, to);
	}
	public static boolean areBasicTypesCompadible(VarType t1,VarType t2) {
		if(t1.isVoid()^t2.isVoid())return false;
		if(t1.isNumeric()^t2.isNumeric())return false;
		if(t1.isLogical()^t2.isLogical())return false;
		return true;
	}
	public static boolean canDirectSetBasicTypes(VarType t1,VarType t2) {
		return t1.type==t2.type;//must cast if not the same type; it causes trouble; casting in place not always possible
	}
	@Override public boolean equals(Object other) {
		if (this==other)return true;
		if (other==null)return false;
		if(!(other instanceof VarType))return false;
		VarType v=(VarType) other;
		boolean precisionEqual =( this.isReady() == v.isReady());
		if (precisionEqual) precisionEqual = this.isReady()? (this.precision==v.precision) 
				: this.precisionTemplateName.equals(v.precisionTemplateName);
		return this.type==v.type
				&& this.struct==v.struct
				&& precisionEqual
				&&(this.structArgs==null?
						v.structArgs==null
						:
						this.structArgs.equals(v.structArgs));
		
		
	}
	public static Builtin fromSuffix(String suffix,boolean isFloat) {
		VarType.Builtin b=null;
		if (suffix!=null && suffix.length()>0) switch (suffix.toLowerCase().charAt(0)){
		case 'b': b=VarType.Builtin.BYTE;break;
		case 's': b=VarType.Builtin.SHORT;break;
		case 'i': b=VarType.Builtin.INT;break;
		case 'l': b=VarType.Builtin.LONG;break;
		case 'f': b=VarType.Builtin.FLOAT;break;
		case 'd': b=VarType.Builtin.DOUBLE;break;
		default:{
			b=isFloat?VarType.Builtin.DOUBLE:VarType.Builtin.INT;
		}break;
		}else {
			b=isFloat?VarType.Builtin.DOUBLE:VarType.Builtin.INT;
		}
		return b;
	}
	public String defaultValue() throws CompileError{
		if(this.isStruct())return this.struct.getDefaultValue(this);
		else if (this.isVoid()) throw new CompileError("defaultValue() does not exist for void;");
		else return this.type.defaultValue;
	}
	public String numToString(Number n) {
		String suffix = "";
		switch(this.type) {
		case BOOL:
			suffix="b";
			break;
		case BYTE:
			suffix="b";
			break;
		case DOUBLE:
			suffix="d";
			break;
		case FLOAT:
			suffix="f";
			break;
		case INT:
			suffix="";//mc will interperet 10i as a STRING
			break;
		case LONG:
			suffix="l";
			break;
		case SHORT:
			suffix="s";
			break;
		default:
			break;
		
		}
		return "%s%s".formatted(n,suffix);
	}
	public String boolToStringNumber(boolean n) {
		return n?"1b":"0b";
	}
	public TemplateArgsToken getTemplateArgs(Scope s) throws CompileError{
		if(this.isStruct())
			return this.struct.getTemplateArgs(this,s);
		if(!this.isFloatP())return null;
		if(this.isReady()) {
			TemplateArgsToken tp=new TemplateArgsToken(-1, -1);
			tp.values.add(new Num(-1,-1,this.precision,VarType.INT));
			return tp;
		}else {
			Const c=s.checkForTemplateOrLocalConst(this.precisionTemplateName);
			if(c==null) {
				throw new CompileError("Vartype %s nont binded in time".formatted(this.asString()));
			}
			if(c.ctype!=ConstType.NUM) throw new CompileError("Vartype %s binded to a non-number const".formatted(this.asString()));
			Const.ConstVarToken cvar = new Const.ConstVarToken(-1,-1,c,this.precisionTemplateName);
			TemplateArgsToken tp=new TemplateArgsToken(-1, -1);
			tp.values.add(cvar);
			return tp;
		}
		
	}
}
