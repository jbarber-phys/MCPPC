package net.mcppc.compiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.errors.CompileError;
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
	public static class SelectorToken extends Token{
		public static final Factory factory = new Factory(Regexes.SELECTOR) {
			@Override	public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new SelectorToken(line,col,matcher);
			}
		};
		Selector val;public Selector selctor() {return this.val;}
		public SelectorToken(int line, int col,Matcher m) {
			super(line, col);
			val=new Selector(m);
		}
		@Override public String asString() {
			return val.argsHDR;
		}
	}
	String player;
	//ready to be added to cmd
	String argsCMD;
	//skip the replace [[ ]]
	String argsHDR;
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
	private Selector(String plr,String argsHDR) {
		this.player=plr;
		this.argsHDR=argsHDR;
		this.argsCMD=stripDoubles(argsHDR);
		
	}
	private Selector(String plr) {
		this(plr,"");
	}
	public String toCMD() {return "%s[%s]".formatted(player,argsCMD);}
	public String toHDR() {return "%s[%s]".formatted(player,argsHDR);}
	
	public String getJsonText() {
		return "{\"selector\": \"%s\"}".formatted(Regexes.escape(this.toCMD()));
	}
	@Override
	public String toString() {
		return this.toHDR();
	}
	
}
