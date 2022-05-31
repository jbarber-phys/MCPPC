package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.*;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.UnaryOp.UOType;

public class IfElse extends Statement implements Statement.MultiFlow,Statement.CodeBlockOpener{
	private static final MemberName name=new MemberName(-1, -1, "$ifelse");
	public static IfElse skipMe(Compiler c, Matcher matcher, int line, int col,Keyword opener) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow skip ifElse %s;\n", opener);
		Token t;
		Keyword open=opener;
		if(opener==Keyword.ELSE) {
			//test for ifelse
			t=c.nextNonNullMatch(Factories.checkForKeyword);
			if(t instanceof Token.BasicName) {
				//else if -> elif
				if(Keyword.fromString(((Token.BasicName)t).name)!=Keyword.IF) throw new CompileError.UnexpectedToken(t, "if or '('");
				open=Keyword.ELIF;
			}
		}
		if(opener!=Keyword.ELSE) {
			t=c.nextNonNullMatch(Factories.checkForParen);
			//CompileJob.compileMcfLog.println(t.getClass().getName());
			if (!(t instanceof Token.Paren) || !((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t, "'('");
			
		}
		IfElse me=new IfElse(line,col,open,null,null);
		me.mySubscope = c.currentScope.subscope(c,me,true);
		Token term=Factories.carefullSkipStm(c, matcher, line, col);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	public static IfElse makeMe(Compiler c, Matcher matcher, int line, int col,Keyword opener) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow ifElse %s;\n", opener);
		Token t;
		Keyword open=opener;
		if(opener==Keyword.ELSE) {
			//test for ifelse
			t=c.nextNonNullMatch(Factories.checkForKeyword);
			//CompileJob.compileMcfLog.printf("%s, %s\n",matcher.group(),t);
			if(t instanceof Token.BasicName) {
				//else if -> elif
				if(Keyword.fromString(((Token.BasicName)t).name)!=Keyword.IF) throw new CompileError.UnexpectedToken(t, "if or '('");
				open=Keyword.ELIF;
			}
		}
		Equation eq=null;
		RStack stack=c.currentScope.getStackFor();
		if(open!=Keyword.ELSE) {
			t=c.nextNonNullMatch(Factories.checkForParen);
			if (!(t instanceof Token.Paren) || !((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t, "'('");
			Function.FuncCallToken call=Function.FuncCallToken.make(c, line, col, matcher, IfElse.name, stack);
			if(call.args.size()!=1)throw new CompileError("wrong number of args in %s statement; expected 1;".formatted(open.name));
			eq=call.args.get(0);
		}
		
		IfElse me=new IfElse(line,col,open,stack,eq);
		me.mySubscope = c.currentScope.subscope(c,me,false);
		Token term=c.nextNonNullMatch(Factories.nextIsLineEnd);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	final Equation test;
	final Keyword role;
	private final RStack mystack;
	Scope mySubscope;
	IfElse predicessor=null;
	public IfElse(int line, int col,Keyword kw,RStack stack,Equation test) {
		super(line, col);
		this.role=kw;
		this.mystack=stack;
		this.test = test;
	}

	@Override
	public String asString() {
		return "<%s(...)>{...}".formatted(this.role.name);
	}

	@Override
	public boolean sendForward() {
		return this.role!=Keyword.ELSE;
	}

	@Override
	public boolean setPredicessor(MultiFlow pred) {
		if(pred instanceof IfElse) {
			this.predicessor=(IfElse) pred;
			return true;
		}
		else return false;
	}

	@Override
	public boolean claim() {
		return false;
	}

	@Override
	public boolean recive() {
		return this.role!=Keyword.IF;
	}

	@Override
	public String getFlowType() {
		return this.role.name;
	}

	Variable done = null;
	@Override
	public void compileMe(PrintStream p, Compiler c, Scope s) throws CompileError {
		ResourceLocation mcf=this.mySubscope.getSubRes();
		//internal tokens
		BiOperator and = new BiOperator(this.line,-1,OpType.AND);
		BiOperator or = new BiOperator(this.line,-2,OpType.OR);
		UnaryOp not = new UnaryOp(this.line,-3,UOType.NOT);
		if(this.role==Keyword.IF) {
			if(this.done ==null )this.done=this.mySubscope.getIfelseDoneVarExMe(c);
			this.done.setMeToBoolean(p, c, s, mystack, false);
		}else {
			if(this.predicessor==null) throw new CompileError("flow statement %s has no preceding if statement;".formatted(this.role.name));
			this.done=this.predicessor.done;
		}
		Register rdo;
		if(this.role!=Keyword.ELSE) {
			int home1=this.mystack.setNext(VarType.BOOL);
			this.done.getMe(p,s, mystack, home1);
			this.test.compileOps(p, c, s, VarType.BOOL);
			int home2=this.test.setReg(p, c, s, VarType.BOOL);
			this.mystack.castRegister(p,s, home2, VarType.BOOL);
			
			
			
			not.perform(p, c, s, mystack, home1);
			and.perform(p, c, s, mystack, home2, home1);
			not.perform(p, c, s, mystack, home1);
			or.perform(p, c, s, mystack, home1, home2);
			
			rdo=this.mystack.getRegister(home2);
			
			this.done.setMe(p,s, mystack, home1);
		}else {
			int home1=this.mystack.setNext(VarType.BOOL);
			this.done.getMe(p,s, mystack, home1);
			not.perform(p, c, s, mystack, home1);
			this.done.setMe(p,s, mystack, home1);
			rdo=this.mystack.getRegister(home1);
		}
		
		p.printf("execute if %s run function %s\n", rdo.testMeInCMD(),mcf.toString());
		
		this.mystack.clear();
		this.mystack.finish(c.job);
		
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
		return false;
	}
	@Override
	public void addToStartOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
	}
	@Override
	public void addToEndOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
	}


}
