package net.mcppc.compiler.tokens;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Token.Factory;

public class Num extends Const.ConstLiteralToken{
	//do not use these functiosn in equations

	public static Num tokenizeNextNumNonNull(Compiler c, Matcher matcher, int line, int col) throws CompileError  {
		Num ret=(Num) Const.checkForExpression(c, matcher, line, col, ConstType.NUM);
		if (ret.value==null)throw new CompileError.UnexpectedToken(ret,"non null number literal/constant");
		return ret;
	}
	public static Num tokenizeNextNumNonNull(Compiler c, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError  {
		Num ret=(Num) Const.checkForExpression(c, matcher, line, col, forbidden, ConstType.NUM);
		if (ret.value==null)throw new CompileError.UnexpectedToken(ret,"non null number literal/constant");
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
				int newPrecision=(int) Math.min(this.type.getPrecision()-Math.log10(n2), other.type.getPrecision()-Math.log10(n1));
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
				int newPrecision=(int) Math.min(this.type.getPrecision()+Math.log10(n2), other.type.getPrecision()+2.0*Math.log10(n2)-Math.log10(n1));
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
	
}