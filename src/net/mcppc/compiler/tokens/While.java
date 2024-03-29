package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.regex.Matcher;


import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.*;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.Targeted;
/**
 * a while statement; identical to java;
 * @author RadiumE13
 *
 */
public class While extends Statement implements Statement.CodeBlockOpener,Statement.Flow {
	private static final MemberName name=new MemberName(-1, -1, "$while");
	public static While skipMe(Compiler c, Matcher matcher, int line, int col,Keyword w) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow skip ifElse %s;\n", opener);
		Token t;
		t=c.nextNonNullMatch(Factories.checkForParen);
		//CompileJob.compileMcfLog.println(t.getClass().getName());
		if (!(t instanceof Token.Paren) || !((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t, "'('");
		While me=new While(line,col,c.cursor,null, null);
		me.mySubscope = c.currentScope.subscope(c,me,true);
		Token term=Factories.carefullSkipStm(c, matcher, line, col);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	public static While makeMe(Compiler c, Matcher matcher, int line, int col,Keyword opener) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow ifElse %s;\n", opener);
		Token t;
		Equation eq=null;
		RStack stack=c.currentScope.getStackFor();
		t=c.nextNonNullMatch(Factories.checkForParen);
		if (!(t instanceof Token.Paren) || !((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t, "'('");
		Function.FuncCallToken call=Function.FuncCallToken.make(c, c.currentScope, line, col, matcher, While.name, stack);
		if(call.args.size()!=1)throw new CompileError("wrong number of args in while statement; expected 1;");
		eq=call.args.get(0);
		While me=new While(line,col,c.cursor,stack, eq);
		me.mySubscope = c.currentScope.subscope(c,me,false);
		Token term=c.nextNonNullMatch(Factories.nextIsLineEnd);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}

	final Equation test;
	private final RStack mystack;
	Scope mySubscope;
	//IfElse predicessor=null;
	public While(int line, int col,int cursor,RStack stack, Equation test) {
		super(line, col, cursor);
		this.mystack=stack;
		this.test = test;
	}
	@Override
	public String asString() {
		return "<while(...)>";
	}
	@Override
	@Targeted
	public void compileMe(PrintStream p, Compiler c, Scope s) throws CompileError {
		RStack stack=mystack;
		ResourceLocation mcf=this.mySubscope.getSubRes();
		Variable mybreak=this.mySubscope.getBreakVarInMe(c);
		mybreak.setMeToBoolean(p, s, stack, false);//on start only
		this.test.compileOps(p, c, s, VarType.BOOL);
		int ts=this.test.setReg(p, c, s, VarType.BOOL);
		Register tr=stack.getRegister(ts);
		p.printf("execute if %s unless %s run function %s\n",tr.testMeInCMD() ,mybreak.isTrue() , mcf.toString());
		this.mystack.pop();
		this.mystack.finish(c.job);
	}


	@Override
	public String getFlowType() {
		return "while";
	}
	@Override
	public boolean didOpenCodeBlock() {
		return true;
	}

	@Override
	public Scope getNewScope() {
		return this.mySubscope;
	}
	@Override
	public boolean canBreak() {
		return true;
	}
	@Override public boolean canRecurr() {
		return true;
	}
	@Override
	public void addToStartOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
	}
	@Override
	@Targeted
	public void addToEndOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		RStack stack=mystack;
		ResourceLocation mcf=this.mySubscope.getSubRes();
		Variable mybreak=this.mySubscope.getBreakVarInMe(c);
		this.test.compileOps(p, c, s, VarType.BOOL);
		int ts=this.test.setReg(p, c, s, VarType.BOOL);
		Register tr=stack.getRegister(ts);
		p.printf("execute if %s unless %s run function %s\n",tr.testMeInCMD() ,mybreak.isTrue() , mcf.toString());
		this.mystack.pop();
		this.mystack.finish(c.job);
	}
	
}
