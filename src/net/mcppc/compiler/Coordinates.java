package net.mcppc.compiler;

import java.util.regex.Matcher;

import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;

/**
 * coordinates that could appear in a command; there are 3; in mcpp they can be extra spaced but no newlines
 * coordinates move with the at/facing, they are not fixed
 * they 
 * @author jbarb_t8a3esk
 *
 */
public class Coordinates {
	public static class CoordToken extends Token{
		public static final Factory factory=new Factory(Regexes.COORDS) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new CoordToken(line,col,matcher);
			}
		};
		public final Coordinates pos;
		public CoordToken(int line, int col,Matcher m) {
			super(line, col);
			this.pos=new Coordinates(m);
		}
		@Override public String asString() {
			return pos.asString();
		}
		
	}
	final String[] vec;
	Coordinates(Matcher m){
		vec=new String[]{m.group(1),m.group(2),m.group(3)};
	}
	public String asString() {
		return "%s %s %s".formatted(vec[0],vec[1],vec[2]);
	}
	
}
