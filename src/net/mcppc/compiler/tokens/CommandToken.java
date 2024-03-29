package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.CMath;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Function;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.Token.Factory;
import net.mcppc.compiler.tokens.Token.LineEnd;

/**
 * token for vanilla command; must begin with a '$/'; can appear in middle of equation/ etc;
 * may have prefix commands added to get returned value (numeric is result, bool is success);<p>
 * example:<br>
 * a=/time query daytime;<br>
 * compiles to:<br>
 * execute store result storage [file_resourcelocation] a int 1 run time query daytime<br>
 * 
 * @author RadiumE13
 *
 */
/*
 * the $ before the / is required because the compiler uses two compiler passes, the first of which is lazy and can't tell based on context if a / is for division or
 * a cmd unless it knows one of them cannot currently appear; this is only needed when blocks are opened because that is when the compiler will not
 * be able to count on a terminating semicolon or newline (which can be used to skip over nbt tags)
 */
public class CommandToken extends Token{
	public static final boolean CAN_BE_FORMATTED = true;
	
	public static CommandToken formatted(Compiler c,Scope s,Matcher m,int line,int col,boolean attemptFormat) throws CompileError {
		//command does not capture leading ; , \n those are evaluated later (there is a lookahead statement)
		boolean debug = false;
		
		if (CAN_BE_FORMATTED && attemptFormat) {
			String full = m.group("cmd");
			int sizeTotal = full.length();
			int end = m.end();
			c.cursor = m.start("cmd");
			//read for formatted sections
			Token.Factory[] startlook = {FormatIgnore.factory,WildChar.passFactory};
			Token.Factory[] look = {Token.StringToken.factory,FormatOpener.factory,
					Terminator.factory,EscapedNewline.factory,WildChar.passFactory};// strlit ,;,nl, $(,
			//no escape test needed; the sequence $(...) should never appear outside a string literal in a mcfunction so we are safe
			List<Equation> formats = new ArrayList<Equation>();
			StringBuffer buffCmd = new StringBuffer();
			StringBuffer buffPart = new StringBuffer();
			int escstart = c.cursor;
			boolean canFormat=true;
			if(c.nextNonNullMatch(startlook) instanceof WildChar) {
				c.cursor = escstart;
			}else {
				canFormat=false;
			}
			boolean usedEscape=false;
			while(c.cursor < end) {
				Token t = c.nextNonNullMatch(look);
				c.cursor = m.end();
				if(debug) {
					//System.err.printf("%s / %s = %s\n",c.cursor,end,t.asString());
				}
				if(t instanceof Terminator) {
					//should not happen in loop
					throw new CompileError("unexpectedly encountered a command terminator inside a mcf command while formatting");
				}
				if (t instanceof FormatOpener) {
					//begin formatting
					if(canFormat) {
						RStack stack = null; //stack usage is not allowed in const math
						int li = formats.size();
						Function.FuncCallToken.addArgs(c, s, line, col, m, stack, formats);
						if(formats.size()!=li+1)
							throw new CompileError("a number of eqs other than 1 found in mcf format statement at line %s col %s".formatted(t.line,t.col));
						Equation eq = formats.get(li);
						eq.forceConst= true;
						String add = CMath.escepePercents(buffPart.toString());
						buffPart.setLength(0);
						buffCmd.append(add);
						buffCmd.append("%s");
					}else {
						//dont do format args if macros are in the cmd
						buffPart.append(m.group());
					}
					
				}else if(t instanceof EscapedNewline) {
					buffPart.append(((EscapedNewline) t).inCmd());
					usedEscape=true;
					
				} else {
					buffPart.append(m.group());
				}
			}
			//if(!canFormat) System.err.printf("%s\n", full);
			if (formats.isEmpty()) {
				if(usedEscape)full=buffPart.toString();
				//no formatting
				c.cursor=m.end();
				return new CommandToken(line,col,full);
				
			}else {
				String add = CMath.escepePercents(buffPart.toString());
				buffPart.setLength(0);
				buffCmd.append(add);
				CommandToken cmd = new CommandToken(line,col,buffCmd.toString());
				cmd.formatting = formats;
				return cmd;
			}
			
		}else {
			//old:
			c.cursor=m.end();
			return new CommandToken(line,col,m.group("cmd"));
		}
	}
	
	static class Factory extends Token.Factory{
		private final boolean attemptFormat;
		public Factory(Pattern pattern, boolean attemptFormat) {
			super(pattern);
			this.attemptFormat = attemptFormat;
		}
		@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			return formatted(c,c.currentScope, matcher, line, col, attemptFormat);
		}}
	
	public static final Token.Factory factory=new Factory(Regexes.CMD,true);
	public static final Token.Factory factorySafeSkip=new Factory(Regexes.CMD_SAFE,false);//skip formatting if in safe only mode (hdr pass)
	
	public static class Terminator extends Token{
		public static final Factory factory=new Factory(Regexes.CMD_TERMINATOR) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new Terminator(line,col);
			}};
		public Terminator(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return "<end of mcf>";
		}
	}
	public static class FormatOpener extends Token{
		// $( ... )
		public static final Equation.End END = Equation.End.CLOSEPAREN;
		public static final Factory factory=new Factory(Regexes.CMD_FORMATTED_START) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new FormatOpener(line,col);
			}};
		public FormatOpener(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return "$(...)";
		}
	}
	public static class FormatIgnore extends Token{
		public static final Factory factory=new Factory(Regexes.CMD_MACRO_ESCAPE) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new FormatIgnore(line,col);
			}};
		public FormatIgnore(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return "$";
		}
	}
	public static class EscapedNewline extends Token{
		public static final Factory factory=new Factory(Regexes.CMD_NL_ESCAPE) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				c.newLine(matcher.start()+2);
				return new EscapedNewline(line,col);
			}};
		public EscapedNewline(int line, int col) {
			super(line, col);
		}
		@Override public String asString() { return "\\ \\n";
		}
		public String inCmd() {return " ";}
	}
	
	
	private final String cmd;
	
	private List<Equation> formatting = null;
	private boolean hasFormat() {return formatting !=null;}
	private CommandToken(int line, int col, String group) {
		//this should stay private to force external stuff to use formatted(...)
		super(line,col);
		this.cmd=group;
	}
	
	@Override
	public String asString() {
		return "/...";
	}
	public void printToCMD(PrintStream p, Compiler c,Scope s) throws CompileError {
		this.printToCMD(p, c, s, null);
	}
	@Targeted // the op strings are printed to an mcf
	public void printToCMD(PrintStream p, Compiler c,Scope s, String preamble) throws CompileError {
		String s1 = this.cmd;
		if(c.resourcelocation.toString().equals("mcpptest:entity/selector_equation")) {
			//System.err.printf("%s\n", s1);
		}
		if(this.hasFormat()) {
			Object[] args = new Object[this.formatting.size()];
			for (int i=0;i<args.length;i++) {
				Equation eq = this.formatting.get(i);
				eq.constify(c, s);
				if(!eq.isConstable()) {
					//eq.printTree(System.err);
					eq.compileOps(p, c, s, null);
					if(eq.didBFMakeJsonText()) {
						String json = eq.getGeneratedJsonText(p, c, s);
						args[i]=json;
					}else if (eq.didBFMakeConst()) {
						ConstExprToken cs = eq.getYieldedConst(p, c, s);
						args[i] = cs.textInMcf(s.getTarget());
					}else {
						eq.throwNotConstError();
					}
				}else {
					ConstExprToken cst = eq.getConst();
					args[i] = cst.textInMcf(s.getTarget());
				}
			}
			s1 = s1.formatted(args);//unpack
		}
		if(preamble!=null) {
			s1 ="%s %s".formatted(preamble,s1);
		}
		p.printf("%s\n", s1);
	}
}