package net.mcppc.compiler;

import java.util.regex.Matcher;

import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Type;

public interface StructTypeParams{

	@Override public boolean equals(Object other) ;
	public static final class Blank implements StructTypeParams{
		public Blank(){}
		@Override public boolean equals(Object other) {
			return other instanceof Blank;
		}
	}
	public static class MembType implements StructTypeParams{
		public final VarType myType;
		public MembType(VarType vt) {
			myType=vt;
		}
		@Override public boolean equals(Object other) {
			return other instanceof MembType && this.myType.equals(((MembType)other).myType);
		}
		
		public static MembType tokenizeTypeArgs(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			Type t=Type.tokenizeNextVarType(c, matcher, line, col);
			Type.closeTypeArgs(c, matcher, line, col);
			return new MembType(t.type);
		}

		public MembType withPrecision(int precision) throws CompileError {
			return new MembType(this.myType.withPrecision(precision));
		}
	}
	public static class PrecisionType implements StructTypeParams{
		public final int precision;
		public PrecisionType(int precision) {
			this.precision=precision;
		}
		@Override public boolean equals(Object other) {
			return other instanceof PrecisionType && this.precision==((PrecisionType)other).precision;
		}
		
		public static PrecisionType tokenizeTypeArgs(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			Token t=c.nextNonNullMatch(Factories.nextNum);
			if(!(t instanceof Token.Num)) throw new CompileError.UnexpectedToken(t,"number");
			Type.closeTypeArgs(c, matcher, line, col);
			Number n=((Token.Num)t).value;
			return new PrecisionType(n.intValue());
		}
	}
}