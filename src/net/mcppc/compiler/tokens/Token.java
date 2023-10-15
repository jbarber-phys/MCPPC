package net.mcppc.compiler.tokens;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.INbtValueProvider;
import net.mcppc.compiler.VarType.Builtin;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Str;

public abstract class Token {
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
	public static class ForInSep extends Token{
		public static final Factory factory=new Factory(Regexes.COLON) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new ForInSep(line,col);
			}};
		public ForInSep(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return ":";
		}
	}
	//internal use only - marks an optional argument as having no default;
	public static class NullArgDefault extends Token{
		public static final NullArgDefault instance = new NullArgDefault(-1,-1);
		public NullArgDefault(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return "null-default-arg";
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
		public static final Factory dontPassFactory10=new Factory(Regexes.NEXT_10_CHAR) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				//do not change cursor location
				return new WildChar(line,col,matcher.group());
			}};
		public static final Factory dontPassFactory20=new Factory(Regexes.NEXT_20_CHAR) {
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
		public static final String TYPEOPEN = ARE_TYPEARGS_PARENS? "(":"<";
		public static final String TYPECLOSE = ARE_TYPEARGS_PARENS? ")":">";
		public static final Token.Factory[] checkForTypeargBracket = 
			{Factories.newline,Factories.comment,Factories.domment,Factories.space,
					Statement.Domment.factory,
					ARE_TYPEARGS_PARENS?Token.Paren.factory:TypeArgBracket.factory,
					Token.WildChar.dontPassFactory};
		public static final Token.Factory[] checkForTypeargSep = 
			{Factories.newline,Factories.comment,Factories.domment,Factories.space,
					Statement.Domment.factory,
					ARE_TYPEARGS_PARENS?Token.Paren.factory:TypeArgBracket.factory,
							Token.ArgEnd.factory,
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
	public static class IndexBrace extends AbstractBracket{
		public static final  Factory factory = new Factory(Regexes.INDEXBRACE) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new IndexBrace(line,col,matcher.group(1)!=null);
			}};
		public IndexBrace(int line, int col,boolean forward) {
			super(line, col,forward);
		}
		@Override public String asString() {
			return forward?"[":"]";
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
		public final String name;
		public BasicName(int line, int col,String name) {
			super(line, col);
			this.name=name;
		}
		public MemberName toMembName() {
			return new MemberName(this.line,this.col,this.name);
		}
		@Override public String asString() {
			return this.name;
		}
	}
	public static class StringToken extends Const.ConstLiteralToken implements INbtValueProvider{
		public static final Factory factory = new Factory(Regexes.STRLIT) {
			@Override
			public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new StringToken(line,col,matcher.group());
			}
		};
		public final VarType type=Str.STR;
		public final String literal;
		public StringToken(int line, int col,String name) {
			super(line, col,ConstType.STRLIT);
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
		@Override
		public String textInHdr() {
			return this.literal();
		}
		@Override
		public int valueHash() {
			return this.literal.hashCode();
		}

		private static final Pattern NONWORD=Pattern.compile("[^\\w]");// [^\w]
		String resCase() {
			String s=this.literal.toLowerCase();
			return  NONWORD.matcher(s).replaceAll("_");
		}
		@Override
		public String resSuffix() {
			// TODO Auto-generated method stub
			return "str_%s".formatted(this.resCase());
		}

		@Override
		public boolean hasData() {
			return true;
		}
		@Override
		public String fromCMDStatement() {
			return INbtValueProvider.VALUE.formatted(this.literal);
		}

		@Override
		public VarType getType() {
			return this.type;
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
	public static class RangeSep extends Token{
		public static final Factory factory=new Factory(Regexes.RANGESEP) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new RangeSep(line,col);
			}};
		public RangeSep(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return "..";
		}
	}
	public static class LoneTilde extends Token{
		public static final Factory factory=new Factory(Regexes.LONE_TILDE) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new LoneTilde(line,col);
			}};
		public static boolean testFor(Compiler c,Matcher m,int line,int col) throws CompileError {
			final Factory[] look= Factories.genericCheck(factory);
			Token t=c.nextNonNullMatch(look);
			return t instanceof LoneTilde;
		}
		public LoneTilde(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return "~";
		}
	}
	public static class CodeLine extends Token{
		public static final Factory factory=new Factory(Regexes.CODELINE) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new CodeLine(line,col,matcher.group());
			}};
		public final String content;
		public CodeLine(int line, int col,String content) {
			super(line, col);
			this.content=content;
		}
		@Override public String asString() { return this.content;
		}
	}
}
