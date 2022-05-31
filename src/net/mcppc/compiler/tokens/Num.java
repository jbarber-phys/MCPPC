package net.mcppc.compiler.tokens;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.INbtValueProvider;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Token.Factory;

public class Num extends Const.ConstLiteralToken implements INbtValueProvider{
	//do not use these functiosn in equations
	public static class Numrange extends Num{
		///use with care, not all behavior is polymorphic;
		//use for ints only
		//use only in TemplateDefToken 's
		Num end;
		public Numrange(Num prev, Num end) {
			super(prev.line, prev.col, prev.value, prev.type);
			this.end=end;
		}
		@Override
		public String textInHdr() {
			String s = super.textInHdr();
			String s2= end.textInHdr();
			return "%s .. %s".formatted(s,s2);
		}
		public List<Num> getAll(){
			//inclusive
			Number n1=this.value;
			Number n2=end.value;
			List<Num> all = new ArrayList<Num>();
			if(Math.round(n2.doubleValue()-n1.doubleValue()) ==0) {
				all.add(end);return all;
			}
			int step = (int) Math.round(Math.signum(n2.doubleValue()-n1.doubleValue()));
			int n=n1.intValue();
			while ((n-n2.intValue()*step <= 0)) {
				all.add(new Num(this.line,this.col,n,VarType.INT));
				n+=step;
			}
			return all;
		}
	}
	public static ConstExprToken tokenizeNextNumNonNull(Compiler c,Scope s, Matcher matcher, int line, int col) throws CompileError  {
		ConstExprToken ret= Const.checkForExpression(c,s, matcher, line, col, ConstType.NUM);
		if (ret instanceof Num && ((Num) ret).value==null)throw new CompileError.UnexpectedToken(ret,"non null number literal/constant");
		return ret;
	}
	@Deprecated
	public static Num tokenizeNextNumNonNull2(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError  {
		Num ret=(Num) Const.checkForExpression(c,s, matcher, line, col, forbidden, ConstType.NUM);
		if (ret.value==null)throw new CompileError.UnexpectedToken(ret,"non null number literal/constant");
		return ret;
	}
	public static ConstExprToken tokenizeNextNumNonNull(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError  {
		ConstExprToken ret=Const.checkForExpression(c,s, matcher, line, col, forbidden, ConstType.NUM);
		return ret;
	}
	public static final class Factory extends Token.Factory{

		public Factory(Pattern pattern) {
			super(pattern);
		}
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			final int LEADDIGITS=1;
			final int DPLACES=2;
			final int EXP=3;//old 4
			final int TYPE=4; // old 3
			
			c.cursor=matcher.end();
			boolean isFloat = (matcher.group(DPLACES)!=null)||(matcher.group(EXP)!=null);
			VarType.Builtin b=VarType.fromSuffix(matcher.group(TYPE), isFloat);
			
			if (b.isFloatP){
				int precision=0;
				int exp=0;
				if (matcher.group(DPLACES)!=null) {
					precision=matcher.group(DPLACES).length()-1;//correct
				}
				if (matcher.group(EXP)!=null) {
					exp=Integer.parseInt(matcher.group(EXP).substring(1));//correct
				}
				String s=(matcher.group(LEADDIGITS)!=null?matcher.group(LEADDIGITS):"")
						+(matcher.group(DPLACES)!=null?matcher.group(DPLACES):"")
						+(matcher.group(EXP)!=null?matcher.group(EXP):"");
				//CompileJob.compileMcfLog.printf("%s   %s   %s   %s;\n", matcher.group(1),matcher.group(2),matcher.group(3),matcher.group(4));
				//CompileJob.compileMcfLog.printf("%s;\n", s);
				Number value = Double.parseDouble(s);//should work
				return new Num(line,col,value,new VarType(b,precision));
			}
			Number value = Long.parseLong(matcher.group(LEADDIGITS));
			return new Num(line,col,value,new VarType(b));
		}
		
	}
	public static final Factory factory = new Factory(Regexes.NUM) ;
	
	//do not use this one in equations
	public static final Factory factoryneg = new Factory(Regexes.NUM_NEG) ;

	public static final Token.Factory nullfactory = new Token.Factory(Regexes.NULL_KEYWORD) {
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			c.cursor=matcher.end();
			return new Num(line,col,null,VarType.DOUBLE);
		}};
	public final Number value;
	public final VarType type;
	public Num(int line, int col,Number num,VarType type) {
		super(line, col,ConstType.NUM);
		this.value=num;
		this.type=type;
	}
	@Override
	public String asString() {
		//TODO show sigfigs and type letter for byte short long float
		if (this.type.isFloatP()) {
			return value.toString();
			
		}else {
			return value.toString();
		}
	}
	public Num withValue(Number val,VarType type) {
		return new Num(this.line,this.col,val,type);
	}
	public Num times(Num other) {
		double n1=this.value.doubleValue();double n2=other.value.doubleValue();
		double result=n1*n2;
		VarType ntype=this.type;
		if(!this.type.isFloatP()) {
			ntype=other.type;
		}else {
			if(other.type.isFloatP() &&!(n1==0 || n2==0)) {
				//2 floats
				//use sig fig rules
				int newPrecision;
				try {
					newPrecision = (int) Math.min(this.type.getPrecision(null)-Math.log10(n2), other.type.getPrecision(null)-Math.log10(n1));
				} catch (CompileError e) {
					e.printStackTrace();
					newPrecision=0;
				}
				ntype=VarType.doubleWith(newPrecision);
			}
		}
		return new Num(this.line,this.col,result,ntype);
	}
	public Num divby(Num other) {
		double n1=this.value.doubleValue();double n2=other.value.doubleValue();
		double result=n1/n2;
		VarType ntype=this.type;
		if(!this.type.isFloatP()) {
			ntype=other.type;
		}else {
			if(other.type.isFloatP() &&!(n1==0 || n2==0)) {
				//2 floats
				//use sig fig rules
				int newPrecision;
				try {
					newPrecision = (int) Math.min(this.type.getPrecision(null)+Math.log10(n2), other.type.getPrecision(null)+2.0*Math.log10(n2)-Math.log10(n1));
				} catch (CompileError e) {
					e.printStackTrace();
					//unreachable
					newPrecision=0;
				}
				ntype=VarType.doubleWith(newPrecision);
			}
		}
		return new Num(this.line,this.col,result,ntype);
	}
	@Override
	public String textInHdr() {
		if(this.value==null)return "null";
		return this.type.numToString(this.value);
	}
	@Override
	public int valueHash() {
		return this.value.hashCode();
	}

	private static final Pattern NONWORD=Pattern.compile("[^\\w]");// [^\w]
	String resCase() {
		String s=this.value.toString().toLowerCase();
		//System.err.printf("num value: %s -> '%s'\n",s,NONWORD.matcher(s).replaceAll("_"));
		return  NONWORD.matcher(s).replaceAll("_");
	}
	@Override
	public String resSuffix() {
		return "num%s".formatted(this.resCase());
	}
	@Override
	public boolean hasData() {
		return true;
	}
	@Override
	public String fromCMDStatement() {
		return INbtValueProvider.VALUE.formatted(this.type.numToString(this.value));
	}
	@Override
	public VarType getType() {
		return this.type;
	}

	public static final Number getNumber(Token t) {
		return getNumber(t,null);
	}
	public static final Number getNumber(Token t, VarType requireType) {
		if(t==null)return null;
		if(t instanceof Num) {
			if(requireType==null ||((Num) t).type.type == requireType.type)
				return ((Num) t).value;
			else return null;
		}else if (t instanceof Const.ConstVarToken) {
			return getNumber(((Const.ConstVarToken) t).constv.getValue(),requireType);
		}else if (t instanceof Equation && ((Equation) t).isNumber()) {
			Equation eq = ((Equation) t);
			
			Const.ConstExprToken ce=eq.getConstNbt();
			return getNumber(ce,requireType);
		}
		return null;
	}
}