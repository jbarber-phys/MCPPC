package net.mcppc.compiler.tokens;

import java.util.Objects;
import java.util.regex.Matcher;

import net.mcpp.vscode.JsonMaker;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.INbtValueProvider;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.tokens.Token.Factory;

public class Bool extends Const.ConstLiteralToken implements INbtValueProvider{
	public static final Factory factory = new Factory(Regexes.BOOL) {
		@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			c.cursor=matcher.end();
			return new Bool(line,col,matcher.group(1)!=null);
		}};
	public final boolean val;
	public final VarType type=VarType.BOOL;
	public Bool(int line, int col,boolean val) {
		super(line, col,ConstType.BOOLIT);
		this.val=val;
	}
	@Override public String asString() {
		return val?"true":"false";
	}
	@Override
	public String textInHdr(){
		return this.asString();
	}
	@Override
	public int valueHash() {
		return Objects.hash(val);
	}
	@Override
	public String resSuffix() {
		return "bool_%s".formatted(this.asString());
	}

	@Override
	public boolean hasData() {
		return true;
	}
	@Override
	public String fromCMDStatement(VTarget tg) {
		return INbtValueProvider.VALUE.formatted(this.type.boolToStringNumber(this.val, tg));
	}
	@Override
	public String textInMcf(VTarget tg) {
		return this.type.boolToStringNumber(this.val, tg);//should always appear as data of 0b or 1b
	}
	@Override
	@Targeted // the op strings are printed to an mcf
	public String getJsonText() throws CompileError {
		String txt = this.type.boolToStringTrueFalse(val);
		return "{\"text\": \"%s\"}".formatted(txt);//true or false
	}
	@Override
	public String getJsonArg() throws CompileError {
		String txt = this.type.boolToStringTrueFalse(val);
		return txt;//true or false
	}
	
	
	@Override
	public VarType getType() {
		return this.type;
	}
	@Override
	public boolean canCast(VarType type) {
		return type.isLogical();
	}
	@Override
	public ConstExprToken constCast(VarType type) throws CompileError {
		if(!type.isLogical())throw new CompileError.UnsupportedCast( this.constType(),type);
		return this;
	}
	public static Bool and(ConstExprToken a,ConstExprToken b) { return new Bool(a.line,a.col,((Bool) a).val && ((Bool) b).val); }
	public static Bool or(ConstExprToken a,ConstExprToken b) {  return new Bool(a.line,a.col,((Bool) a).val || ((Bool) b).val); }
	public static Bool xor(ConstExprToken a,ConstExprToken b) { return new Bool(a.line,a.col,((Bool) a).val  ^ ((Bool) b).val); }
	public static Bool not(ConstExprToken a) { return new Bool(a.line,a.col,! ((Bool) a).val); }
	public static void registerOps() {
		ConstType bool=ConstType.BOOLIT;
		Const.registerBiOp(bool, BiOperator.OpType.AND, bool, Bool::and);
		Const.registerBiOp(bool, BiOperator.OpType.OR, bool, Bool::or);
		Const.registerBiOp(bool, BiOperator.OpType.XOR, bool, Bool::xor);
		
		Const.registerUniOp( UnaryOp.UOType.NOT, bool, Bool::not);
	}
}