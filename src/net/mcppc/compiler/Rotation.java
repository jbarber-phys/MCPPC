package net.mcppc.compiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Coordinates.CoordToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Token.Factory;

public class Rotation {
	public static class RotToken extends Const.ConstExprToken{
		public static final Factory factory=new Factory(Regexes.ROTATION) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new RotToken(line,col,matcher);
			}
		};
		public final Rotation rot;
		public RotToken(int line, int col,Matcher m) {
			super(line, col);
			this.rot=new Rotation(m);
		}
		@Override public String asString() {
			return rot.asString();
		}
		@Override
		public ConstType constType() {
			return ConstType.COORDS;
		}
		@Override
		public String textInHdr() {
			return this.rot.inHDR();
		}
		private static final Pattern TILDE=Pattern.compile("~");// ~
		private static final Pattern CARET=Pattern.compile("\\^");// \^
		private static final Pattern NONWORD=Pattern.compile("[^\\w]");// [^\w]
		String resCase() {
			String s=this.rot.toString().toLowerCase();
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
		@Override
		public String textInMcf() {
			return this.rot.inCMD();
		}
		@Override
		public boolean canCast(VarType type) {
			return false;
		}
		@Override
		public ConstExprToken constCast(VarType type) throws CompileError {
			throw new CompileError.UnsupportedCast( this.constType(),type);
		}
		
	}
	final String[] angs;
	Rotation(Matcher m){
		angs=new String[]{m.group("ang1"),m.group("ang2")};
	}
	public Rotation(Num yaw,Num pitch){
		angs=new String[]{CMath.getMultiplierFor(yaw.value.floatValue())
				,CMath.getMultiplierFor(pitch.value.floatValue())};
	}
	public String asString() {
		return this.inHDR();
	}

	public String inHDR() {
		return "%s %s".formatted(angs[0],angs[1]);
	}
	public String inCMD() {
		return this.inHDR();
	}
	

	public static Variable ang1Of(Selector s,int precision) throws CompileError {//yaw
		VarType vt = VarType.floatWith(precision);
		Variable v = new Variable("$anonyawof",vt,null,new ResourceLocation("mcppc","rotation"));
		return v.maskEntity(s, NbtPath.ROT1);
	}
	public static Variable ang2Of(Selector s,int precision) throws CompileError {//yaw
		VarType vt = VarType.floatWith(precision);
		Variable v = new Variable("$anonpitchof",vt,null,new ResourceLocation("mcppc","rotation"));
		return v.maskEntity(s, NbtPath.ROT2);
	}
}
