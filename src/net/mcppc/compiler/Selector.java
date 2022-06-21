package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;

public class Selector {

	//replace on group 3
	static final Pattern replace = Pattern.compile("\\\"(\\\\.|[^\\\"\\\\])*\\\"|(\\[\\[)|(\\]\\])");// \"(\\.|[^\"\\])*\"|(\[\[)|(\]\])
	
	//replace must go above the other static vars; consider adding to static block
	public static final Selector AT_S = new Selector("@s");
	public static final Selector AT_P = new Selector("@p");
	public static final Selector AT_A = new Selector("@a");
	public static final Selector AT_R = new Selector("@r");
	public static final Selector AT_E = new Selector("@e");
	public static class SelectorToken extends Const.ConstLiteralToken{
		static class InvalidSelector extends CompileError{
			public InvalidSelector(SelectorToken token) {
				//this is mostly done on the token-factory level now
				super("invalid selector '%s'; non-@ player names may not contain args in the [] (empty [] are ok)"
						.formatted(token.selector().toCMD()));
			}
			
		}
		@Deprecated
		public static final Factory factory_old = new Factory(Regexes.SELECTOR) {
			@Override	public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				SelectorToken me=new SelectorToken(line,col,matcher);
				if(me.selector().hasUnusableArgs())throw new InvalidSelector(me);
				return me;
			}
		};
		public static final Factory carefullfactory = new Factory(Regexes.SELECTOR) {
			@Override	public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				if(matcher.group(1).charAt(0) !='@' && matcher.group(2).length()!=0) {
					//invalid selector or a var with array access
					return new Token.WildChar(line, col, matcher.group(1).substring(0, 1));
				}
				c.cursor=matcher.end();
				SelectorToken me=new SelectorToken(line,col,matcher);
				if(me.selector().hasUnusableArgs())throw new InvalidSelector(me);
				return me;
			}
		};
		public static final Factory factory = carefullfactory;
		private final boolean garbage1232343___ = Struct.load();
		public final VarType type = new VarType(Entity.entities,new StructTypeParams.Blank());
		Selector val;public Selector selector() {return this.val;}
		public SelectorToken(int line, int col,Matcher m) {
			super(line, col,ConstType.SELECTOR);
			val=new Selector(m);
		}
		public SelectorToken(int line, int col,Selector s) {
			super(line, col,ConstType.SELECTOR);
			val=s;
		}
		@Override public String asString() {
			return val.argsHDR;
		}
		@Override
		public String textInHdr() {
			return this.selector().toHDR();
		}

		@Override
		public String resSuffix() {
			return "slctr_%s_%x".formatted(this.val.playerResCase(),this.valueHash());
		}
		@Override
		public int valueHash() {
			return this.val.hashCode();
		}
	}
	String player;
	//ready to be added to cmd
	String argsCMD;
	//skip the replace [[ ]]
	String argsHDR;
	boolean hasArgs() {
		if(this.argsCMD.strip().length()==0)return false;
		return true;
	}
	boolean hasUnusableArgs() {
		//true if this is not an @ selector but still has non-empty []
		if(!this.hasArgs())return false;
		if(player.contains("@"))return false;
		return true;
	}
	public Selector(Matcher m) {
		this.player=m.group(1);
		this.argsHDR=m.group(2);
		this.argsCMD=stripDoubles(m.group(2));
	}
	public static String stripDoubles(String hdr) {
		StringBuffer args=new StringBuffer(hdr);
		Matcher m2 = replace.matcher(args);//this should be OK
		int idx=0;
		while(idx<args.length()&&m2.find(idx)) {
			idx=m2.end();
			if(m2.group(2)!=null) {
				args.replace(m2.start(2), m2.end(2), "[");idx--;
			}else if(m2.group(3)!=null) {
				args.replace(m2.start(3), m2.end(3), "]");idx--;
			}
		}
		return args.toString();
	}
	public Selector(String plr,String argsHDR) {
		this.player=plr;
		this.argsHDR=argsHDR;
		this.argsCMD=stripDoubles(argsHDR);
		
	}
	public Selector(String plr) {
		this(plr,"");
	}
	public String toCMD() {
		if(this.hasArgs())return "%s[%s]".formatted(player,argsCMD);
		else return player;
	}
	public String toHDR() {
		if(this.hasArgs())return "%s[%s]".formatted(player,argsHDR);
		else return player;
	}
	
	public String getJsonText() {
		return "{\"selector\": \"%s\"}".formatted(Regexes.escape(this.toCMD()));
	}
	@Override
	public String toString() {
		return this.toHDR();
	}
	private static final Pattern NONWORD=Pattern.compile("[^\\w]");// [^\w]
	String playerResCase() {
		String s=this.player.toLowerCase();
		String p=s.charAt(0)=='@'?"at":"";
		return  p+NONWORD.matcher(s).replaceAll("_");
	}
	@Override public int hashCode() {
		return Objects.hash(this.player,this.argsCMD);
	}
	public Selector playerify() {
		if(this.player.equals("@e")) return new Selector("@a",this.argsHDR);
		return this;
	}
	public Selector selfify() {
		if(!this.player.equals("@e")) return new Selector("@s",this.argsHDR);
		return this;
	}
	public void kill(PrintStream p) {
		p.printf("kill %s\n", this.toCMD());
	}
	public void addTag(PrintStream p, String tag) {
		p.printf("tag %s add %s\n", this.toCMD(),tag);
	}
	public void removeTag(PrintStream p, String tag) {
		p.printf("tag %s remove %s\n", this.toCMD(),tag);
	}
	
}
