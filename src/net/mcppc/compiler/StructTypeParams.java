package net.mcppc.compiler;

import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Type;

public interface StructTypeParams{

	@Override public boolean equals(Object other) ;
	public boolean isReady();
	public static final class Blank implements StructTypeParams{
		public Blank(){}
		@Override public boolean equals(Object other) {
			return other instanceof Blank;
		}
		@Override public boolean isReady() {return true;}
	}
	public static class MembType implements StructTypeParams{
		public final VarType myType;
		public MembType(VarType vt) {
			myType=vt;
		}
		@Override public boolean equals(Object other) {
			return other instanceof MembType && this.myType.equals(((MembType)other).myType);
		}
		
		public static MembType tokenizeTypeArgs(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError {
			Type t=Type.tokenizeNextVarType(c,s, matcher, line, col,forbidden);
			Type.closeTypeArgs(c, matcher, line, col);
			return new MembType(t.type);
		}

		public MembType withPrecision(int precision) throws CompileError {
			return new MembType(this.myType.withPrecision(precision));
		}

		@Override public boolean isReady() {
			return this.myType.isReady();
		}
		public StructTypeParams withTemplatePrecision(String pc) throws CompileError {
			return new MembType(this.myType.withTemplatePrecision(pc));
		}
	}
	public static class PrecisionType implements StructTypeParams{
		public final int precision;
		public final String precisionTemplateName;
		public PrecisionType(int precision) {
			this.precision=precision;
			this.precisionTemplateName=null;
		}
		public PrecisionType(String name) {
			this.precision=0;
			this.precisionTemplateName=name;
		}
		@Override public boolean isReady(){return this.precisionTemplateName==null;}
		@Override public boolean equals(Object other) {
			return other instanceof PrecisionType && this.precision==((PrecisionType)other).precision;
		}
		
		public static PrecisionType tokenizeTypeArgs(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError {
			//Token t=c.nextNonNullMatch(Factories.nextNum);
			//if(!(t instanceof Num)) throw new CompileError.UnexpectedToken(t,"number");
			Const.ConstExprToken t=Num.tokenizeNextNumNonNull(c,s, matcher, line, col, forbidden);
			Type.closeTypeArgs(c, matcher, line, col);
			if(t instanceof Num) {
				Number n=((Num)t).value;
				return new PrecisionType(n.intValue());
			}else {
				String ps=t.textInHdr();
				return new PrecisionType(ps);
			}
		}
		public VarType impose(VarType defaulttype) throws CompileError {
			if(this.isReady())return defaulttype.withPrecision(this.precision);
			else return defaulttype.withTemplatePrecision(this.precisionTemplateName);
		}
	}
}