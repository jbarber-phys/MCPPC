package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CMath;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.PrintF.PrintContext;
import net.mcppc.compiler.functions.PrintF.PrintfArgs;
import net.mcppc.compiler.struct.Str;
import net.mcppc.compiler.tokens.Token;
/**
 * formats the arguments similar to printf(...) but yields the json text element instead of printing; also has no selector;
 * {@link FormatF.format} just yields a raw json text element;
 * {@link FormatF.formatLit} yields a string literal of the json instead,
 * which could be added to nbt data that accepts it (such as a book);
 * @author RadiumE13
 *
 */
public class FormatF extends BuiltinFunction {
	public static final FormatF format = new FormatF("format",false);
	public static final FormatF formatLit = new FormatF("formatLit",true);
	private final boolean isLiteral;
	public FormatF(String name,boolean isLiteral) {
		super(name);
		this.isLiteral = isLiteral;
	}

	@Override
	public VarType getRetType(BFCallToken token, Scope s) {
		return this.isLiteral? Str.STR : VarType.VOID;
	}

	public boolean willYieldConstOrJson( Scope s,BFCallToken token) {
		return true;
	}
	@Override
	public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
			throws CompileError {
		return PrintF.tokenizeFormatArgs(c, s, matcher, line, col, stack, false);
	}

	@Override
	public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		//create print context
		PrintfArgs pargs=(PrintfArgs) token.getArgs();
		PrintContext context = token.getPrintContext();
		if(context==null) context = new PrintContext(this.name,!this.isLiteral);
		String json = PrintF.compileJsonTextElement(p, c, s, pargs, stack, null,context);
		if(this.isLiteral) {
			ConstExprToken ce = new Token.StringToken(token.line,token.col,CMath.getStringLiteral(json));
			token.yieldConst(ce);
			//System.err.printf("yielded literal %s\n",ce.asString());
		}else {
			token.yieldJsonText(json);
			//System.err.printf("yielded json %s\n",json);
		}
	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart,
			VarType typeWanted) throws CompileError {
		// do nothing

	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
			throws CompileError {
		if(!this.isLiteral)return;
		ConstExprToken ret = token.getYieldedConst();
		v.setMeToExpr(p, s, stack, ret);
		
	}

	@Override
	public Number getEstimate(BFCallToken token, Scope s) {
		return null;
	}

	@Override
	public void dumpRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		// still do nothing
		//Equation will also call this if a printf gets the json text
		super.dumpRet(p, c, s, token, stack);
	}

}
