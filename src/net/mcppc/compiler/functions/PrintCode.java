package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcpp.vscode.HighlightCode;
import net.mcpp.vscode.JsonMaker;
import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Token.CodeLine;

public class PrintCode extends BuiltinFunction {
	public static final PrintCode printNextLine = new PrintCode("printnextline",1);
	public static void registerAll() {
		BuiltinFunction.register(printNextLine);
	}
	private final int skips;
	public PrintCode(String name,int skipnewlines) {
		super(name);
		skips=skipnewlines;
	}

	@Override
	public VarType getRetType(BFCallToken token) {
		return VarType.VOID;
	}
	private String content;
	@Override
	public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
		BasicArgs args = new BasicArgs(); //BuiltinFunction.tokenizeArgsEquations(c, matcher, line, col, stack);
		Token me = Entity.checkForSelectorOrEntityToken(c, c.currentScope, matcher, line, col);
		if(me!=null)args.add("entity", me);
		if(BuiltinFunction.findArgsep(c)) throw new CompileError("unexpected ',', expected a ')'");
		int cursor = c.cursor;
		Token.Factory[] look =  {Token.CodeLine.factory};
		for(int i=0;i<skips;i++) {
			c.nextNonNullMatch(look);
		}
		Token.CodeLine t=(CodeLine) c.nextNonNullMatch(look);
		this.content = t.content;
		c.cursor=cursor;
		return args;
	}

	@Override
	public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		BasicArgs args = (BasicArgs) token.getArgs();
		Selector to = args.isEmpty() ? Selector.AT_S : 
			Entity.getSelectorFor((args.arg(0)));
		to = to.playerify();
		//System.err.printf("%s :: content: %s", c.resourcelocation,this.content);
		Map<String,Object> json = HighlightCode.highlight(this.content);
		p.printf("tellraw %s ", to.toCMD());
		JsonMaker.printAsJson(p, json, false, 0);
		p.printf("\n");
	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
			throws CompileError {

	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
			throws CompileError {

	}

	@Override
	public Number getEstimate(BFCallToken token) {
		return null;
	}

}
