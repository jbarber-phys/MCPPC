package net.mcppc.compiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;

/** represents coordinates; can be in absolute, tilde, or caret notation;

 * @author RadiumE13
 *
 */
/*
 * NOTE: integer absolute coordinates will actually center on the block (add a half integer to x and/or z):
 * 			 0 0 0 = 0.5 0 0.5
 * 			0 0 0.1 = 0.5 0 0.1
 */
@Targeted
public class Coordinates {
	public static final CoordToken ZERO =new CoordToken(-1,-1, new Coordinates("0.0","0.0","0.0"));
	public static final CoordToken ZEROBLOCK =new CoordToken(-1,-1, new Coordinates("0","0","0"));
	public static final CoordToken ATME =new CoordToken(-1,-1, new Coordinates("~","~","~"));
	public static final CoordToken FORWARD =new CoordToken(-1,-1, new Coordinates("^","^","^1"));
	
	public static enum CoordSystem {
		ABS,TILDE,CARET;
	}
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
		@Override public String textInMcf(VTarget tg) {
			return this.pos.inCMD();
		}
		@Override
		public String getJsonText() throws CompileError {
			throw new CompileError("cannot print an %s".formatted(this.constType().name));
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

		@Override public boolean canCast(VarType type) {
			return false;
		}
		@Override
		public ConstExprToken constCast(VarType type) throws CompileError {
			throw new CompileError.UnsupportedCast( this.constType(),type);
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
	private Coordinates(String[] vec){
		this.vec=vec;
	}
	public static Coordinates asAbs(double x,double y,double z) {
		return new Coordinates("%s".formatted(CMath.getMultiplierFor(x)),
				"%s".formatted(CMath.getMultiplierFor(y)),
				"%s".formatted(CMath.getMultiplierFor(z)));
	}
	public static Coordinates asTildes(double x,double y,double z) {
		return new Coordinates("~%s".formatted(CMath.getMultiplierFor(x)),
				"~%s".formatted(CMath.getMultiplierFor(y)),
				"~%s".formatted(CMath.getMultiplierFor(z)));
	}
	public static Coordinates asCarets(double x,double y,double z) {
		return new Coordinates("^%s".formatted(CMath.getMultiplierFor(x)),
				"^%s".formatted(CMath.getMultiplierFor(y)),
				"^%s".formatted(CMath.getMultiplierFor(z)));
	}
	public static CoordToken unslice(Num x,String y,String z) {
		Coordinates c= new Coordinates(CMath.getMultiplierFor(x.value.doubleValue()),
				y, z);
		return new CoordToken(x.line,x.col,c);
	}
	public String asString() {
		return this.inHDR();
	}

	public String inHDR() {
		return "%s %s %s".formatted(vec[0],vec[1],vec[2]);
	}
	@Targeted 
	public String inCMD() {
		return this.inHDR();
	}
	public CoordSystem getSystem(int i) {
		switch (this.vec[i].charAt(0)) {
		case '~': return CoordSystem.TILDE;
		case '^': return CoordSystem.CARET;
		default: return CoordSystem.ABS;
		}
	}
	public double numCoord(int i) {
		String s=this.vec[i];
		if(this.getSystem(i)!=CoordSystem.ABS) s = s.substring(1);
		if(s.length()==0) return 0;
		return Double.valueOf(s);
	}
	private String preffix(int i) {
		if(this.getSystem(i)!=CoordSystem.ABS)return this.vec[i].substring(0, 1);
		return "";
	}
	/**
	 * takes an affine combination of coordinates
	 * @param v1
	 * @param v2
	 * @param a
	 * @return
	 * @throws CompileError
	 */
	public static Coordinates interpolate(Coordinates v1,Coordinates v2,double a) throws CompileError{
		String[] vc = new String[3];
		for(int i=0;i<3;i++) {
			if(v1.getSystem(i)!=v2.getSystem(i)) throw new CompileError("Cannot interpolate coordinates %s and %s".formatted(v1.asString(),v2.asString()));
			double v3i = v1.numCoord(i)*(1.-a) + v2.numCoord(i) * a;
			vc[i] = "%s%s".formatted(v1.preffix(i),v3i);
		}
		
		return new Coordinates(vc);
		
	}
}
