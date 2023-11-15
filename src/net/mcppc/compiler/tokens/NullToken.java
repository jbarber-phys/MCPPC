package net.mcppc.compiler.tokens;

import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.INbtValueProvider;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.NbtObject;

public class NullToken extends ConstExprToken implements INbtValueProvider{
	public static String NULL_STR = "null";
	public static Token.Factory factory = new Factory(Regexes.NULL_KEYWORD) {
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			c.cursor = matcher.end();
			return new NullToken(line,col);
		}
		
	};
	public NullToken(int line, int col) {
		super(line, col);
	}

	@Override
	public ConstType constType() {
		return ConstType.NULL;
	}

	@Override
	public String textInHdr() {
		return NULL_STR;
	}

	@Override
	public String resSuffix() {
		return NULL_STR;
	}

	@Override
	public String textInMcf() throws CompileError {
		throw new CompileError("cannot print a null to an mcfunction");
		//return NULL_STR;
	}

	@Override
	public String getJsonText() throws CompileError {
		//not the same as what an Obj would print if null
		return "{\"text\": \"%s\"}".formatted(NULL_STR); 
	}

	@Override
	public boolean canCast(VarType type) {
		return type.isVoid() || (type.struct instanceof NbtObject);
	}

	@Override
	public ConstExprToken constCast(VarType type) throws CompileError {
		return this;
	}

	@Override
	public String asString() {
		return NULL_STR;
	}

	@Override
	public boolean hasData() {
		//let it be involved in direct ops that support it
		return true;
	}

	@Override
	public String fromCMDStatement() {
		return "null";
	}

	@Override
	public VarType getType() {
		return VarType.VOID;
	}

}
