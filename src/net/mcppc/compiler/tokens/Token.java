package net.mcppc.compiler.tokens;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.VarType.Builtin;
import net.mcppc.compiler.errors.CompileError;

public abstract class Token {
	public interface ConstexprValue {//extends Token
		//blank
	}
	public static abstract class Factory {
		//may be able to due without template
		public Pattern pattern;
		
		public Factory(Pattern pattern) {
			this.pattern=pattern;
		}
		public abstract Token createToken(Compiler c,Matcher matcher,int line,int col) throws CompileError;
	}
	/**
	 * gets a representation of the token for output to console if unrecognized
	 * 
	 * @return
	 */
	public abstract String asString();
	public int line;
	public int col;
	public Token(int line,int col) {
		this.line=line;
		this.col=col;
	}
	
	public static class LineEnd extends Token{
		public static final Factory factory=new Factory(Regexes.LINE_END) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new LineEnd(line,col);
			}};
		public LineEnd(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return ";";
		}
	}
	public static class ArgEnd extends Token{
		public static final Factory factory=new Factory(Regexes.ARGSEP) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new ArgEnd(line,col);
			}};
		public ArgEnd(int line, int col) {
			super(line, col);
		}
		@Override public String asString() {return ",";
		}
	}
	public static class TagOf extends Token{
		public static final Factory factory=new Factory(Regexes.TAGOF) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new TagOf(line,col);
			}};
		public TagOf(int line, int col) {
			super(line, col);
		}
		@Override public String asString() {return ".";
		}
	}
	public static class ScoreOf extends Token{
		public static final Factory factory=new Factory(Regexes.SCOREOF) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new ScoreOf(line,col);
			}};
		public ScoreOf(int line, int col) {
			super(line, col);
		}
		@Override public String asString() {return "::";
		}
	}
	public static class Member extends Token{
		public static final Factory factory=new Factory(Regexes.MEMBER) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new Member(line,col);
			}};
		public Member(int line, int col) {
			super(line, col);
		}
		@Override public String asString() {return ".";
		}
	}
	
	public static class WildChar extends Token{
		public static final Factory dontPassFactory=new Factory(Regexes.ANY_CHAR) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				//do not change cursor location
				return new WildChar(line,col,matcher.group());
			}};
		final String s;
		public WildChar(int line, int col,String c) {
			super(line, col);
			this.s=c;
		}
		@Override public String asString() {return s;
		}
	}
	public static abstract class AbstractBracket extends Token{
		public AbstractBracket(int line, int col,boolean forward) {
			super(line, col);
			this.forward=forward;
		}
		public final boolean forward;
		
		
		//KEEP THIS AS FALSE;
		public static final boolean ARE_TYPEARGS_PARENS=false;
		public static final Token.Factory[] checkForTypeargBracket = 
			{Factories.newline,Factories.comment,Factories.domment,Factories.space,
					Statement.Domment.factory,
					ARE_TYPEARGS_PARENS?Token.Paren.factory:TypeArgBracket.factory,
					Token.WildChar.dontPassFactory};

		public static  boolean isArgTypeArg(Token t) {
			if(ARE_TYPEARGS_PARENS)return t instanceof Paren;
			else return t instanceof TypeArgBracket;
		}
	}
	public static class Paren extends AbstractBracket{
		public static final Factory factory = new Factory(Regexes.PARENS) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new Paren(line,col,matcher.group(1)!=null);
			}};
		public Paren(int line, int col,boolean forward) {
			super(line, col,forward);
		}
		@Override public String asString() {
			return forward?"(":")";
		}
	}
	public static class TypeArgBracket extends AbstractBracket{
		public static final Factory factory = new Factory(Regexes.ANGLEBRACKETS) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new TypeArgBracket(line,col,matcher.group(1)!=null);
			}};
		public TypeArgBracket(int line, int col,boolean forward) {
			super(line, col,forward);
		}
		@Override public String asString() {
			return forward?"<":">";
		}
	}
	public static class Bool extends Token implements ConstexprValue{
		public static final Factory factory = new Factory(Regexes.BOOL) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new Bool(line,col,matcher.group(1)!=null);
			}};
		public final boolean val;
		public final VarType type=VarType.BOOL;
		public Bool(int line, int col,boolean val) {
			super(line, col);
			this.val=val;
		}
		@Override public String asString() {
			return val?"true":"false";
		}
	}
	public static class CodeBlockBrace extends AbstractBracket{
		public static final  Factory factory = new Factory(Regexes.CODEBLOCKBRACE) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new CodeBlockBrace(line,col,matcher.group(1)!=null);
			}};
		public static final Factory unscopeFactory = new Factory(Regexes.CODEBLOCKBRACE) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				CodeBlockBrace t=new CodeBlockBrace(line,col,matcher.group(1)!=null);
				if(t.forward)throw new CompileError.UnexpectedToken(t,"}");
				c.cursor=matcher.end();
				return t;
			}};
		public CodeBlockBrace(int line, int col,boolean forward) {
			super(line, col,forward);
		}
		@Override public String asString() {
			return forward?"{":"}";
		}
	}
	public static class Assignlike extends Token{
		Kind k;
		public Assignlike(int line, int col,Kind k) {
			super(line, col);
			this.k=k;
		}
		public static class Factory extends Token.Factory{
			Kind k;
			public Factory(Pattern pattern,Kind k) {
				super(pattern);
				this.k=k;
			}
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new Assignlike(line,col,this.k);
			}
			
		}
		public static final Factory factoryAssign=new Factory(Regexes.ASSIGN,Kind.ASSIGNMENT);
		public static final Factory factoryMask=new Factory(Regexes.MASK,Kind.MASK);
		public static final Factory factoryEstimate=new Factory(Regexes.ESTIMATE,Kind.ESTIMATE);
		public static enum Kind{
			ASSIGNMENT(Regexes.ASSIGN.toString()),MASK(Regexes.MASK.toString()),ESTIMATE(Regexes.ESTIMATE.toString());
			public String text;
			Kind(String text){
				this.text=text;
			}
		}
		@Override
		public String asString() {
			return this.k.text;
		}
		
	}
	public static class BasicName extends Token{
		//for a named thing that hasn't been identified yet
		public static final Factory factory = new Factory(Regexes.NAME) {
			@Override
			public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new BasicName(line,col,matcher.group());
			}
		};
		String name;
		public BasicName(int line, int col,String name) {
			super(line, col);
			this.name=name;
		}
		@Override public String asString() {
			return this.name;
		}
	}
	public static class StringToken extends Token{
		//for a named thing that hasn't been identified yet
		public static final Factory factory = new Factory(Regexes.STRLIT) {
			@Override
			public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new StringToken(line,col,matcher.group());
			}
		};
		String literal;
		public StringToken(int line, int col,String name) {
			super(line, col);
			this.literal=name;
		}
		@Override public String asString() {
			return this.literal();
		}
		public String literal() {
			return this.literal;
		}
		public String getJsonText() {
			return this.literal();
		}
	}
	public static class MemberName extends Token implements Identifiable{
		//for a named thing that hasn't been identified yet
		public static final Factory factory = new Factory(Regexes.NAME) {
			static final Factory[] look= {Factories.newline,Factories.comment,Factories.domment,Factories.space,
					Token.BasicName.factory,Token.Member.factory,Token.WildChar.dontPassFactory};
			@Override
			public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				MemberName me= new MemberName(line,col,matcher.group());
				//int pc=c.cursor;//wildchar will be smart
				while(true) {
					Token t=c.nextNonNullMatch(look);
					if(t instanceof Token.WildChar)break;
					else if (t instanceof Token.Member) {
						//move on
					}else throw new CompileError.UnexpectedToken(t,"'.' or non-name");
					Token t2=c.nextNonNullMatch(look);
					if (!(t2 instanceof BasicName))throw new CompileError.UnexpectedToken(t,"name");
					me.names.add(((BasicName)t2).name);
				}
				return me;
			}
		};
		public final List<String> names=new ArrayList<String>();
		public MemberName(int line, int col,String name) {
			super(line, col);
			this.names.add(name);
		}
		public MemberName(BasicName b) {
			this(b.line, b.col,b.name);
		}
		public MemberName addName(String name) {
			this.names.add(name);return this;
		}
		@Override public String asString() {
			return String.join(".", names);
		}
		Variable var=null;
		Number estimate=null;
		@Override
		public int identify(Compiler c,Scope s) throws CompileError {
			this.var=c.myInterface.identifyVariable(this,s);
			if(this.var!=null)this.estimate=s.getEstimate(var);
			return 0;
		}
	}
	public static class Num extends Token  implements ConstexprValue{
		public static final Factory factory = new Factory(Regexes.NUM) {
			@Override
			public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				final int LEADDIGITS=1;
				final int DPLACES=2;
				final int EXP=3;//old 4
				final int TYPE=4; // old 3
				
				c.cursor=matcher.end();
				boolean isFloat = (matcher.group(DPLACES)!=null)||(matcher.group(EXP)!=null);
				VarType.Builtin b=VarType.fromSuffix(matcher.group(TYPE), isFloat);
				
				if (b.isFloatP){
					int precision=0;
					int exp=0;
					if (matcher.group(DPLACES)!=null) {
						precision=matcher.group(DPLACES).length()-1;//correct
					}
					if (matcher.group(EXP)!=null) {
						exp=Integer.parseInt(matcher.group(EXP).substring(1));//correct
					}
					String s=(matcher.group(LEADDIGITS)!=null?matcher.group(LEADDIGITS):"")
							+(matcher.group(DPLACES)!=null?matcher.group(DPLACES):"")
							+(matcher.group(EXP)!=null?matcher.group(EXP):"");
					//CompileJob.compileMcfLog.printf("%s   %s   %s   %s;\n", matcher.group(1),matcher.group(2),matcher.group(3),matcher.group(4));
					//CompileJob.compileMcfLog.printf("%s;\n", s);
					Number value = Double.parseDouble(s);//should work
					return new Num(line,col,value,new VarType(b,precision));
				}
				Number value = Long.parseLong(matcher.group(LEADDIGITS));
				return new Num(line,col,value,new VarType(b));
			}
		};

		public static final Factory nullfactory = new Factory(Regexes.NULL_KEYWORD) {

			@Override
			public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new Num(line,col,null,VarType.DOUBLE);
			}};
		public final Number value;
		public final VarType type;
		public Num(int line, int col,Number num,VarType type) {
			super(line, col);
			this.value=num;
			this.type=type;
		}
		@Override
		public String asString() {
			//TODO show sigfigs and type letter for byte short long float
			if (this.type.isFloatP()) {
				return value.toString();
				
			}else {
				return value.toString();
			}
		}
		public Num withValue(Number val,VarType type) {
			return new Num(this.line,this.col,val,type);
		}
		public Num times(Num other) {
			double n1=this.value.doubleValue();double n2=other.value.doubleValue();
			double result=n1*n2;
			VarType ntype=this.type;
			if(!this.type.isFloatP()) {
				ntype=other.type;
			}else {
				if(other.type.isFloatP() &&!(n1==0 || n2==0)) {
					//2 floats
					//use sig fig rules
					int newPrecision=(int) Math.min(this.type.getPrecision()-Math.log10(n2), other.type.getPrecision()-Math.log10(n1));
					ntype=VarType.doubleWith(newPrecision);
				}
			}
			return new Num(this.line,this.col,result,ntype);
		}
		public Num divby(Num other) {
			double n1=this.value.doubleValue();double n2=other.value.doubleValue();
			double result=n1/n2;
			VarType ntype=this.type;
			if(!this.type.isFloatP()) {
				ntype=other.type;
			}else {
				if(other.type.isFloatP() &&!(n1==0 || n2==0)) {
					//2 floats
					//use sig fig rules
					int newPrecision=(int) Math.min(this.type.getPrecision()+Math.log10(n2), other.type.getPrecision()+2.0*Math.log10(n2)-Math.log10(n1));
					ntype=VarType.doubleWith(newPrecision);
				}
			}
			return new Num(this.line,this.col,result,ntype);
		}
		
	}
	public static List<CharSequence> asStrings(List<Token> ts) {
		List<CharSequence> list=new ArrayList<CharSequence>();
		for(Token t:ts)list.add(t.asString());
		return list;
	}
	public static class RefArg extends Token{
		public static final Factory factory=new Factory(Regexes.REF_PREFIX) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new RefArg(line,col);
			}};
		public RefArg(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return "ref";
		}
	}
}
