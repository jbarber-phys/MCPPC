package net.mcppc.compiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Coordinates.CoordToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Token.Factory;

/**
 * represents a facing direction; can be in absolute or tilde notation;
 * @author RadiumE13
 *
 */
@Targeted
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
		public RotToken(int line, int col,Rotation r) {
			super(line, col);
			this.rot=r;
		}
		@Override public String asString() {
			return rot.asString();
		}
		@Override
		public ConstType constType() {
			return ConstType.ROT;
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
		public String getJsonText() throws CompileError {
			throw new CompileError("cannot print an %s".formatted(this.constType().name));
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
	public Rotation(String pitch,String yaw){
		angs=new String[]{pitch,yaw};
	}
	public static RotToken unslice(Num ang1,String ang2) {
		Rotation r= new Rotation(CMath.getMultiplierFor(ang1.value.doubleValue()),
				ang2);
		return new RotToken(ang1.line,ang1.col,r);
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
