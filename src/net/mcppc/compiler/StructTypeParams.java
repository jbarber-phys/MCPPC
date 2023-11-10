package net.mcppc.compiler;

import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Type;
/**
 * the type arguments used by a struct type; the stuff inside the `<>`
 * @author RadiumE13
 *
 */
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
		
		public String getPrecisionStr() {
			if(!this.isReady()) {
				return this.precisionTemplateName;
			}else return Integer.toString(this.precision);
		}
		public VarType floatify(VarType basicType) {
			if(!this.isReady()) {
				return basicType.withTemplatePrecisionBasic(precisionTemplateName);
			}else return basicType.withPrecisionBasic(precision);
		}
	}
	public static class MembTypePair implements StructTypeParams{
		public final VarType first;
		public final VarType second;
		public MembTypePair(VarType t1,VarType t2) {
			this.first=t1;
			this.second=t2;
		}
		@Override public boolean equals(Object other) {
			return other instanceof MembTypePair && this.first.equals(((MembTypePair)other).first)
					&& this.second.equals(((MembTypePair)other).second);
		}
		
		public static MembTypePair tokenizeTypeArgs(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError {
			Type t1=Type.tokenizeNextVarType(c,s, matcher, line, col,forbidden);
			if(!Type.findTypeArgsep(c)) throw new CompileError("expected a second type argument;");
			Type t2=Type.tokenizeNextVarType(c,s, matcher, line, col,forbidden);
			Type.closeTypeArgs(c, matcher, line, col);
			//System.err.printf("type Map<%s , %s>\n", t1.asString(),t2.asString());
			return new MembTypePair(t1.type,t2.type);
		}

		@Override public boolean isReady() {
			return this.first.isReady() && this.second.isReady();
		}
	}
}