package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.BuiltinFunction.BasicArgs;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Function;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Statement.MultiFlow;
import net.mcppc.compiler.tokens.UnaryOp.UOType;
/*
 * TODO test this
 */
/**
 * A switch and case statement, obeys rules similar to java swithc/case (such as breaking behavior);<br>
 * syntax:  switch(...) case(...,...) {} case (...) {} default (...) {} <br>
 * 
 * @author RadiumE13
 *
 */
public class Switch extends Statement implements MultiFlow,Statement.CodeBlockOpener {
	public static Switch skipMe(Compiler c, Matcher matcher, int line, int col,Keyword opener) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow skip ifElse %s;\n", opener);
		Token t;
		Keyword open=opener;
		if(open!=Keyword.DEFAULT) {
			t=c.nextNonNullMatch(Factories.checkForParen);
			//CompileJob.compileMcfLog.println(t.getClass().getName());
			if (!(t instanceof Token.Paren) || !((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t, "'('");
		}

		RStack stack=c.currentScope.getStackFor();
		Switch me=new Switch(line,col,c.cursor,open, stack);
		me.mySubscope = c.currentScope.subscope(c,me,true);
		me.breakv = me.mySubscope.getBreakVarInMe(c);
		Token term=Factories.carefullSkipStm(c, matcher, line, col);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	public static Switch makeMe(Compiler c, Matcher matcher, int line, int col,Keyword opener) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow ifElse %s;\n", opener);
		Token t;
		Keyword open=opener;
		RStack stack=c.currentScope.getStackFor();
		Switch me=new Switch(line,col,c.cursor,opener, stack);
		me.mySubscope = c.currentScope.subscope(c,me,false);
		me.breakv = me.mySubscope.getBreakVarInMe(c);
		if(opener==Keyword.SWITCH) {
			t=c.nextNonNullMatch(Factories.checkForParen);
			//CompileJob.compileMcfLog.println(t.getClass().getName());
			if (!(t instanceof Token.Paren) || !((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t, "'('");
			BasicArgs b=BuiltinFunction.tokenizeArgsEquations(c, c.currentScope, matcher, line, col, stack);
			if(b.nargs()>1) throw new CompileError("too many args in switch(...) statement, expected 1 but got %d".formatted(b.nargs()));
			me.switchEq = (Equation) b.arg(0);
			//will always be followed by a case statement
			boolean cs = Keyword.checkFor(c, matcher, Keyword.CASE);
			if(!cs) throw new CompileError("switch(...) must be followed by a case(...) statement");
			open = Keyword.CASE;
		}
		if (open == Keyword.CASE){
			t=c.nextNonNullMatch(Factories.checkForParen);
			//CompileJob.compileMcfLog.println(t.getClass().getName());
			if (!(t instanceof Token.Paren) || !((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t, "'('");
			BasicArgs b=BuiltinFunction.tokenizeArgsEquations(c, c.currentScope, matcher, line, col, stack);
			//if(b.nargs()>1) throw new CompileError("too many args in case(...) statement, expected 1 but got %d".formatted(b.nargs()));
				//actually, allow multiple args for multiple cases
			me.caseEqs = b;
		}//else default
		
		
		Token term=c.nextNonNullMatch(Factories.nextIsLineEnd);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	Keyword role;
	RStack myStack;//same as used by eqs
	Variable breakv;//set 
	
	Equation switchEq = null;
	BasicArgs caseEqs = null;
	public Switch(int line, int col,int cursor,Keyword role, RStack stack) {
		super(line, col, cursor);
		this.role = role;
		this.myStack=stack;
	}

	@Override
	public String getFlowType() {
		return this.role.name;
	}

	@Override
	public boolean canBreak() {
		return true;
	}

	@Override
	public boolean sendForward() {
		return this.role!=Keyword.DEFAULT;
	}
	Switch predicessor;
	Scope mySubscope;

	Variable value = null;
	Variable matchFlag =null;
	@Override
	public boolean setPredicessor(MultiFlow pred) throws CompileError {
		if(pred instanceof Switch) {
			this.predicessor=(Switch) pred;
			this.breakv = ((Switch) pred).breakv;
			this.mySubscope.reInitialzeBreakVarInMe(this, breakv);
			return true;
		}
		return false;
	}

	@Override
	public boolean claim() {
		return false;
	}

	@Override
	public boolean recive() {
		return this.role!=Keyword.SWITCH;
	}

	@Override
	public void compileMe(PrintStream f, Compiler c, Scope s) throws CompileError {
		RStack stack = this.myStack;//shared with eqs
		if(this.role == Keyword.SWITCH) {
			this.switchEq.compileOps(f, c, s, null);
			VarType tp = this.switchEq.retype;
			this.value = s.addLoopLocal("\"$value\"", tp, c);
			this.switchEq.setVar(f, c, s, this.value);
			this.breakv.setMeToBoolean(f, c, s, stack, false);
			this.makeMatchFlag(c, s).setMeToBoolean(f, c, s, stack, false);
		}
		Variable val = this.getValue(c, s);
		VarType type = val.type;
		ResourceLocation block = this.mySubscope.getSubRes();
		if(this.role==Keyword.DEFAULT) {
			int i= stack.setNext(VarType.BOOL);
			Register bv = stack.getRegister(i);
			this.breakv.getMe(f, s, bv);
			block.runUnless(f, bv);
			stack.pop();
		}else {
			Variable doflag = this.makeMatchFlag(c, s);
			
			int dor = stack.setNext(doflag.type);
			doflag.getMe(f, s,stack, dor);
			BiOperator op = new BiOperator(this.line, -1, BiOperator.OpType.EQ);
			BiOperator or = new BiOperator(this.line, -3, BiOperator.OpType.OR);
			BiOperator and = new BiOperator(this.line, -4, BiOperator.OpType.AND);
			UnaryOp not = new UnaryOp(this.line, -2,UOType.NOT);
			for(int i=0;i<caseEqs.nargs();i++) {
				Equation ci = (Equation) this.caseEqs.arg(i);
				Equation eq=Equation.toArgueHusk(stack, val.basicMemberName(s),op,ci);
				assert eq.stack == this.myStack;
				//eq.stack.debugOut(System.err);
				eq.compileOps(f, c, s, VarType.BOOL);
				
				int req = eq.setReg(f, c, s, eq.retype);//this news a reg
				//System.out.println(req);
				or.perform(f, c, s, stack, dor, req);
				//eq.stack.debugOut(System.err);
				stack.pop();
				//eq.stack.debugOut(System.err);
				
			}
			int br = stack.setNext(this.breakv.type);
			this.breakv.getMe(f, s, stack, br);
			not.perform(f, c, s, stack, br);
			and.perform(f, c, s, stack, dor, br);
			doflag.setMe(f, s, stack, dor);
			block.runIf(f, stack.getRegister(dor));
			stack.pop(2);
			
			
		}
		stack.clear();stack.finish(c.job);

	}

	@Override
	public String asString() {
		return this.role + " ... ";
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
	public void addToEndOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
	}

	@Override
	public void addToStartOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
	}
	private Variable getValue(Compiler c, Scope s) throws CompileError {
		if(this.predicessor!=null) return this.predicessor.value;
		return this.value;
	}
	private Variable makeMatchFlag(Compiler c, Scope s) throws CompileError {
		if(this.predicessor!=null) return this.predicessor.makeMatchFlag(c, s);
		if(this.matchFlag==null) this.matchFlag = s.addLoopLocal("\"$match\"", VarType.BOOL, c);
		return this.matchFlag;
	}

}
