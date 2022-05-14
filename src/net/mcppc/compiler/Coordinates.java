package net.mcppc.compiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;

/**
 * coordinates that could appear in a command; there are 3; in mcpp they can be extra spaced but no newlines
 * coordinates move with the at/facing, they are not fixed
 * NOTE: integer absolute coordinates will actually center on the block (add a half integer to x and/or z):
 * 			 0 0 0 = 0.5 0 0.5
 * 			0 0 0.1 = 0.5 0 0.1
 * @author jbarb_t8a3esk
 *
 */
public class Coordinates {
	public static final CoordToken ZERO =new CoordToken(-1,-1, new Coordinates("0.0","0.0","0.0"));
	public static final CoordToken ZEROBLOCK =new CoordToken(-1,-1, new Coordinates("0","0","0"));
	public static final CoordToken ATME =new CoordToken(-1,-1, new Coordinates("~","~","~"));
	public static final CoordToken FORWARD =new CoordToken(-1,-1, new Coordinates("^","^","^1"));
	public static class CoordToken extends Const.ConstExprToken{
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

		public CoordToken(int line, int col,Coordinates pos) {
			super(line, col);
			this.pos=pos;
		}
		@Override public String asString() {
			return pos.asString();
		}
		@Override
		public ConstType constType() {
			return ConstType.COORDS;
		}
		@Override
		public String textInHdr() {
			return this.pos.inHDR();
		}
		private static final Pattern TILDE=Pattern.compile("~");// ~
		private static final Pattern CARET=Pattern.compile("\\^");// \^
		private static final Pattern NONWORD=Pattern.compile("[^\\w]");// [^\w]
		String resCase() {
			String s=this.pos.toString().toLowerCase();
			//System.err.printf("num value: %s -> '%s'\n",s,NONWORD.matcher(s).replaceAll("_"));
			s=TILDE.matcher(s).replaceAll("td");
			s=CARET.matcher(s).replaceAll("ct");
			s=NONWORD.matcher(s).replaceAll("_");
			return  s;
		}
		@Override
		public String resSuffix() {
			return "coords_%s".formatted(this.resCase());
		}
		
	}
	final String[] vec;
	Coordinates(Matcher m){
		vec=new String[]{m.group("x"),m.group("y"),m.group("z")};
	}
	Coordinates(String x,String y,String z){
		vec=new String[]{x,y,z};
	}
	public Coordinates(Num x,Num y,Num z){
		vec=new String[]{CMath.getMultiplierFor(x.value.doubleValue())
				,CMath.getMultiplierFor(y.value.doubleValue())
				,CMath.getMultiplierFor(z.value.doubleValue())};
	}
	public String asString() {
		return this.inHDR();
	}

	public String inHDR() {
		return "%s %s %s".formatted(vec[0],vec[1],vec[2]);
	}
	public String inCMD() {
		return this.inHDR();
	}
	
}
