package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.CompileError.UnexpectedToken;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Bool;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Keyword;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Token.BasicName;
import net.mcppc.compiler.tokens.Token.Factory;
import net.mcppc.compiler.tokens.Type;
import net.mcppc.compiler.tokens.UnaryOp;

/**
 * a Const is a const-expression that can be evaluated at compile time; its not just a var that cannot be changed,
 * note: consts set to other consts will not work if the other one is from a different file
 * 
 * consts live in their own type system (seperate from the VarType system). The types are:
 *     num : a number
 *     flag: a const bool
 *     text: a const string
 *     selector: a target selector
 *     nbt: a nbt tag or path
 *     coords: cordinates, which may be in caret or tilde notation
 *     rot: similar to coords but for rotations
 * 
 * 
 * const params can appear in templates
 * 
 * some templates may need to make multiple files namespace__/template_...,
 * but precision N can be put into tags valued N, 1eN, 1e-N and 1 file is maintained
 * basic types may be workable as well or at very least format stringable
 * 
 * side note: based on this, precision should also be settable at runtime
 * 
 * @author RadiumE13
 *
 */
public class Const {
	/**
	 * if true, disables all un-nececary compile time arithmatic
	 * this means all arithmatic on const bools and nums
	 * math on all other const types are considered neccecary (example: selector)
	 */
	public static final boolean DISABLE_NON_NECECARY_CONST_MATH = false;
	@FunctionalInterface
	public interface ConstBiOp { ConstExprToken op(ConstExprToken left,ConstExprToken right) throws CompileError; }
	public record ConstBiOpType(ConstType a,BiOperator.OpType op,ConstType b) {}
	@FunctionalInterface
	public interface ConstUniOp { ConstExprToken op(ConstExprToken in) throws CompileError; }
	public record ConstUniOpType(UnaryOp.UOType op,ConstType in) {}
	
	private static final Map<ConstBiOpType,ConstBiOp> CONST_BI_OPS = new HashMap<ConstBiOpType,ConstBiOp>();
	private static final Map<ConstUniOpType,ConstUniOp> CONST_UNI_OPS = new HashMap<ConstUniOpType,ConstUniOp>();
	public static void RegisterAllConstOps() {
		Selector.registerOps();//selector math is considered nececcary
		
		if(!DISABLE_NON_NECECARY_CONST_MATH) {
			//all of the arithmetic types
			Num.registerOps();
			Bool.registerOps();
		}
	}
	static {
		RegisterAllConstOps();
	}
	public static boolean registerBiOp(ConstType a,BiOperator.OpType op,ConstType b,ConstBiOp func) {
		return CONST_BI_OPS.put(new ConstBiOpType(a,op,b), func) !=null;
	}
	public static boolean registerUniOp(UnaryOp.UOType op,ConstType in,ConstUniOp func) {
		return CONST_UNI_OPS.put(new ConstUniOpType(op,in), func) !=null;
	}
	public static boolean hasUniOp(UnaryOp.UOType op,ConstType in) {
		return CONST_UNI_OPS.containsKey(new ConstUniOpType(op,in));
	}
	public static ConstExprToken doUniOp(UnaryOp.UOType op,ConstExprToken in) throws CompileError {
		return CONST_UNI_OPS.get(new ConstUniOpType(op,in.constType())).op(in);
	}
	public static boolean hasBiOp(ConstType a,BiOperator.OpType op,ConstType b) {
		return CONST_BI_OPS.containsKey(new ConstBiOpType(a,op,b));
	}
	public static ConstExprToken doBiOp(ConstExprToken a,BiOperator.OpType op,ConstExprToken b) throws CompileError {
		return CONST_BI_OPS.get(new ConstBiOpType(a.constType(),op,b.constType())).op(a,b);
	}
	//const names can match actual tag literals: normally literals are prioritized but this flag turns it around for NBT addresses;
	public static boolean CHECK_FOR_CONSTS_ON_TAGS = true;
	public static Const.ConstExprToken checkForExpressionSafe(Compiler c,Scope s, Matcher matcher, int line, int col,ConstType... types){
		List<Const> forbidden=new ArrayList<Const>();
		int start=c.cursor;
		try {
			return Const.checkForExpression(c,s, matcher, line, col, forbidden, types);
		} catch (CompileError e) {
			c.cursor=start;
			return null;
		}
	}
	static final ConstType[] CONSTTYPESINORDER = ConstType.values();//values are in order
	public static Const.ConstExprToken checkForExpressionAny(Compiler c,Scope s, Matcher matcher, int line, int col){
		return Const.checkForExpressionSafe(c,s, matcher, line, col, CONSTTYPESINORDER);
	}
	public static Const.ConstExprToken checkForExpression(Compiler c,Scope s, Matcher matcher, int line, int col,ConstType... types) throws CompileError{
		List<Const> forbidden=new ArrayList<Const>();
		return Const.checkForExpression(c,s, matcher, line, col, forbidden, types);
	}

		
	public static Const.ConstExprToken checkForExpression(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden,ConstType... types) throws CompileError{
		Token.Factory[] look ;//= new Token.Factory[4+types.length+1];
		//look[0]=Factories.newline;
		//look[1]=Factories.space;
		//look[2]=Factories.domment;
		//look[3]=Factories.comment;
		int itype=-1;
		Token.Factory[] look2=null;
		Token.Factory[] extraLook1 = new Token.Factory[types.length+1];
		for(int i=0;i<types.length;i++) {
			if(types[i]!=ConstType.TYPE)
				//look[4+i]=types[i].factory;
				extraLook1[i]=types[i].factory;
			else {
				itype=i;
				//look[4+i]=Token.WildChar.dontPassFactory;
				extraLook1[i]=Token.WildChar.dontPassFactory;
			}
		}
		//look[4+types.length]=Token.WildChar.dontPassFactory;
		extraLook1[types.length]=Token.WildChar.dontPassFactory;
		look = Factories.genericLook((Token.Factory[])extraLook1);
		
		if (itype>=0){
			look2=new Token.Factory[types.length-itype-1];
			for(int i=itype+1;i<types.length;i++) {
				if(types[i]!=ConstType.TYPE)look2[i-itype-1]=types[i].factory;
				else {
					throw new CompileError("two type terms in checkForExpression");
				}
			}
		}
		boolean hasTagType=false;for(ConstType i:types)if(i==ConstType.NBT) {hasTagType=true;break;}
		if(Const.CHECK_FOR_CONSTS_ON_TAGS && hasTagType) {
			int start=c.cursor;
			ConstVarToken cvt=ConstVarToken.checkFor(c,s, matcher, line, col);
			if(cvt!=null)return cvt.constv.value;
			else c.cursor=start;
		}
		
		Token t=c.nextNonNullMatch(look);
		if(t instanceof Token.WildChar && itype>=0) {
			t=Type.tokenizeNextVarTypeNullable(c,s, matcher, line, col,forbidden);
			if(t!=null)return (ConstExprToken) t;
			t=c.nextNonNullMatch(look2);
		}
		if(!(t instanceof Token.WildChar)) {
			return (ConstExprToken) t;
		}

		ConstVarToken cvt=Const.identifyConst(c,s, matcher, line, col, forbidden, types);
		Const vc=cvt.constv;
		//System.err.printf("fount const %s = %s; %s;\n",vc.name,vc.value==null?"null":vc.value.asString(),vc.refsTemplate());
		if(vc.refsTemplate())
			return cvt;
		else return vc.value;
	}
	public static ConstVarToken identifyConst(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden,ConstType... types) throws CompileError {
		
		ConstVarToken cvt=Const.ConstVarToken.checkFor(c,s, matcher, line, col);
		if(cvt==null) {
			Token t=c.nextNonNullMatch(Factories.checkForMembName);
			String[] ns=new String[types.length];for(int i=0;i<types.length;i++) ns[i]=types[i].name;
			throw new CompileError.UnexpectedToken(t, String.join(",", ns));
		}
		Const cv=cvt.constv;
		if(cv==null) {
			Token t=c.nextNonNullMatch(Factories.checkForMembName);
			String[] ns=new String[types.length];for(int i=0;i<types.length;i++) ns[i]=types[i].name;
			throw new CompileError.UnexpectedToken(t, String.join(",", ns));
		}
		if(forbidden.contains(cv)) {
			//const circular reference
			forbidden.add(cv);
			@SuppressWarnings("unchecked")
			List<String> st=(List<String>) forbidden.stream().map(f->f.name);
			throw new CompileError("Circular const dependance: %s".formatted(st));
		}
		if(cv.refsTemplate()) {
			//throw new CompileError("templates not yet supported TODOo");
			//should be OK
		}
		boolean correctType=false;
		for(ConstType ct:types)if(ct==cv.ctype) {correctType=true;break;}
		if(!correctType)throw new CompileError.UnsupportedCast(cv.ctype, types);
		return cvt;
	}
	public static abstract class ConstExprToken extends Token{
		public abstract ConstType constType() ;
		public boolean refsTemplate() {return false;}
		public abstract String textInHdr();
		public abstract String resSuffix() ;//used in streams: cannot throw
		protected boolean equals(ConstExprToken o) {
			return this.resSuffix()==o.resSuffix();
		}
		@Override public boolean equals(Object o) {
			if(this==o)return true;
			if(this==null || o==null)return false;
			if(o instanceof ConstExprToken) return this.equals((ConstExprToken)o);
			return false;
		}
		public ConstExprToken(int line, int col) {
			super(line, col);
		}
		
		/**
		 * what to print in a mcfunction if inlined
		 * @return
		 * @throws CompileError 
		 */
		public abstract String textInMcf() throws CompileError;
		
	}
	/**
	 * token type enclosing literal expressions that Consts can evaluate to
	 * @author RadiumE13
	 *
	 */
	public static abstract class ConstLiteralToken extends ConstExprToken{
		
		private final ConstType constType__;
		@Override public ConstType constType() {
			return this.constType__;
		}
		public ConstLiteralToken(int line, int col, ConstType ctype) {
			super(line, col);
			this.constType__=ctype;
		}
		public abstract int valueHash();
		
	}
	//unused until templates
	public static class ConstVarToken extends ConstExprToken{
		public static ConstVarToken checkFor(Compiler c,Scope s, Matcher m, int line, int col) throws CompileError {
			final Factory[] lookdot=Factories.genericCheck(Token.Member.factory);
			final Factory[] lookname=Factories.checkForBasicName;
			int start=c.cursor;
			Token t0=c.nextNonNullMatch(Factories.checkForBasicName);
			if(!(t0 instanceof Token.BasicName)) {
				return null;
			}
			List<String> names = new ArrayList<String>();names.add(((Token.BasicName) t0).name);
			FileInterface itf= c.myInterface;
			while(true) {
				String last=names.get(names.size()-1);
				if(itf.hasLib(last, s)) {
					itf = itf.getDirectLib(last, s);
				}else {

					try{
						Const cv=c.myInterface.identifyConst(names,s);
						String path = names.stream().collect(Collectors.joining("."));
						return new ConstVarToken(line,col,cv,path);
					}catch (CompileError p) {
						break;
					}
				}
				Token t=c.nextNonNullMatch(lookdot);
				if(t instanceof Token.WildChar)break;
				else if (t instanceof Token.Member) {
					//move on
				}else throw new CompileError.UnexpectedToken(t,"'.' or non-name");
				Token t2=c.nextNonNullMatch(lookname);
				if (!(t2 instanceof BasicName))throw new CompileError.UnexpectedToken(t,"name");
				names.add(((BasicName)t2).name);
			}

			c.cursor=start;
			return null;
		}
		public final Const constv;
		//the path used to find the string
		public final String refpath;
		public ConstVarToken(int line, int col, Const cnst,String refpath) {
			super(line, col);
			this.constv=cnst;
			this.refpath=refpath;
		}
		@Override
		public ConstType constType() {
			return this.constv.ctype;
		}
		@Override
		public String asString() {
			return this.constv.name;
		}

		public boolean refsTemplate() {return constv.refsTemplate();}
		@Override
		public String textInHdr() {
			return this.constv.inHeader();
		}
		@Override public String resSuffix() {
			if(this.constv.value!=null)return this.constv.value.resSuffix();
			else return "null";
		}
		@Override public String textInMcf() throws CompileError {
			return this.constv.getValue().textInMcf();
		}
	}
	public enum ConstType{
		//values are in order that prevents conflict
		COORDS("coords",Coordinates.CoordToken.factory),
		NUM("num",Num.factoryneg,true),
		BOOLIT("flag",Bool.factory,true),
		STRLIT("text",Token.StringToken.factory),
		TYPE("type",null),
		SELECTOR("selector",Selector.SelectorToken.factory),
		NBT("nbt",NbtPath.NbtPathToken.factory),
		ROT("rot",Rotation.RotToken.factory), // not accessible in a find any operation
		;
		//all calls to the factories should now be const-safe
		//refs are not known at precomp-time
		//may need to resolve them afterward
		//REF, 
		//FREF
		public final String name;
		public final boolean stackable;
		public final Token.Factory factory;
		ConstType(String name,Token.Factory factory){
			this(name,factory,false);
		}
		ConstType(String name,Token.Factory factory,boolean stackable){
			this.name=name;
			this.stackable=stackable;
			this.factory=factory;
		}
		public static ConstType get(String s) {
			for(ConstType t:values()) if(t.name.equals(s))return t;
			return null;
		}
	}
	public final String name;
	public final ResourceLocation path;
	public final ConstType ctype;
	public final Keyword access;
	ConstExprToken  value;
	public boolean isTemplate=false;//make refs to template be private
	public boolean refsTemplate() {
		return this.isTemplate;
	}
	public Const(String name, ResourceLocation path,ConstType type,Keyword access, ConstExprToken value) {
		this.name=name;
		this.path = path;
		this.ctype=type;
		this.access=access;
		this.value=value;
	} 
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof Const))return false;
		return this.name.equals(((Const)other).name) && this.path.equals(((Const)other).path);
	}


	public ConstExprToken getValue() {
		return value;
	}
	public String inHeader()  {
		//yes, this is scope-safe as out of scope vars never are in headers
		return this.name;
	}
	public void headerDeclaration(PrintStream p) throws CompileError {
		p.printf("%s %s %s %s = %s;\n", this.access.name,Keyword.CONST.name,this.ctype.name,this.name,this.value.textInHdr());
	}
}
