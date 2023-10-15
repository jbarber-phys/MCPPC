package net.mcppc.compiler.tokens;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.errors.CompileError;

/**
 * token for vanilla command; can appear in middle of equation/ etc;
 * may have prefix commands added to get returned value
 * example:
 * a=/time query daytime;
 * compiles to:
 * execute store result storage <file_resourcelocation> a int 1 run time query daytime
 * 
 * good practice is to add a $ before the / like: $/kill; but this is only actually required inside of equations in statements that open a code block
 *  (to keep data tags from being mistaken for code block starts and also not confuse with division)
 * @author jbarb_t8a3esk
 *
 */
/*
 * the $ is required because the compiler uses two compilers; the first of which is lazy and cant tell based on context if a / is for division or
 * a cmd unless it knows one of them cannot currently appear; this is only needed when blocks are opened because that is when the compiler will not
 * be able to count on a terminating semicolon or newline (which can be used to skip over nbt tags)
 */
public class CommandToken extends Token{
	public static final boolean CAN_BE_FORMATTED = false;
	static class Factory extends Token.Factory{
		public Factory(Pattern pattern) {super(pattern);}
		@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			c.cursor=matcher.end();
			//TODO format command
			if(CAN_BE_FORMATTED) {
				//TODO
			}
			return new CommandToken(line,col,matcher.group("cmd"));
		}}
	public static final Token.Factory factory=new Factory(Regexes.CMD);
	public static final Token.Factory factorySafe=new Factory(Regexes.CMD_SAFE);
	private final String cmd;
	public CommandToken(int line, int col, String group) {
		super(line,col);
		this.cmd=group;
	}
	
	@Override
	public String asString() {
		return "/...";
	}
	public String inCMD() {
		//with no slash
		return this.cmd;
	}
}