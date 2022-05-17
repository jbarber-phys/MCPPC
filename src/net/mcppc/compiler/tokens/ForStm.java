package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;


import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.*;
import net.mcppc.compiler.errors.CompileError;

public class ForStm extends Statement implements Statement.CodeBlockOpener,Statement.Flow{
	private static final Token.MemberName name=new Token.MemberName(-1, -1, "$for");
	public static ForStm skipMe(Compiler c, Matcher matcher, int line, int col,Keyword w) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow skip ifElse %s;\n", opener);
		Token t;
		t=c.nextNonNullMatch(Factories.checkForParen);
		//CompileJob.compileMcfLog.println(t.getClass().getName());
		if (!(t instanceof Token.Paren) || !((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t, "'('");
		ForStm me=new ForStm(line,col,null);
		me.mySubscope = c.currentScope.subscope(c,me,true);
		Token term=Factories.carefullSkipStm(c, matcher, line, col);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	
	public static ForStm makeMe(Compiler c, Matcher matcher, int line, int col,Keyword w) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow %s;\n", w);
		Token t;
		
		RStack stack=c.currentScope.getStackFor();

		ForStm me=new ForStm(line,col,stack);
		me.mySubscope = c.currentScope.subscope(c,me,false);
		t=c.nextNonNullMatch(Factories.checkForParen);
		if (!(t instanceof Token.Paren) || !((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t, "'('");
		Function.FuncCallToken call=Function.FuncCallToken.make(c, line, col, matcher, ForStm.name, stack);
		int asz=call.args.size();
		if(asz<1)throw new CompileError("wrong number of args in %s statement; expected 1 or more;".formatted(w.name));
		Equation veq=call.args.get(0);
		if(!veq.isRefable())throw new CompileError.UnexpectedToken(t, "variable reference");
		me.counter=veq.getVarRef();
		
		Number startI;
		Number stopE;
		Number step;
		//find range
		if(asz<2)throw new CompileError("wrong number of args in %s statement; expected 2 or more;".formatted(w.name));
		if(asz>4)throw new CompileError("wrong number of args in %s statement; expected 4 or less;".formatted(w.name));
		Equation e1=call.args.get(1);
		if(!e1.isNumber()) {
			throw new CompileError.UnexpectedToken(e1, "number");
		}
		if(asz==2) {
			startI=(int)0;
			stopE=e1.getNumber();
			step=(int)1;
		}else {
			startI=e1.getNumber();
			Equation e2=call.args.get(2);
			if(!e2.isNumber()) {
				throw new CompileError.UnexpectedToken(e2, "number");
			}
			stopE=e2.getNumber();
			if(asz==4) {
				Equation e3=call.args.get(3);
				if(!e3.isNumber()) {
					throw new CompileError.UnexpectedToken(e3, "number");
				}
				step=e3.getNumber();
			}else {
				step=(int)1;
			}
		}
		//CompileJob.compileMcfLog.printf("%s, %s, %s;\n", startI,stopE,step);
		me.makeSpan(c,startI, stopE, step);
		//CompileJob.compileMcfLog.printf("%s;\n", me.span);
		//CompileJob.compileMcfLog.printf("%s;\n", me.span.get(0));
		//CompileJob.compileMcfLog.printf("%s;\n", me.isRangeInt);
		//end
		
		Token term=c.nextNonNullMatch(Factories.nextIsLineEnd);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	private final RStack mystack;
	private Variable counter;
	private boolean isRangeInt=true;
	Scope mySubscope;
	List<Number> span;
	public ForStm(int line, int col,RStack stack) {
		super(line, col);
		this.mystack=stack;
	}
	public void makeSpan(Compiler c,Number startI,Number stopE,Number step) throws CompileError {
		this.isRangeInt=CMath.isNumberInt(startI)&&CMath.isNumberInt(stopE)&&CMath.isNumberInt(step);
		this.span=new ArrayList<Number>();
		Double estLength=(stopE.doubleValue()-startI.doubleValue())/step.doubleValue();
		if(!Double.isFinite(estLength) || estLength>c.job.MAX_NUM_CMDS  || estLength<=0)
			throw new CompileError("bad, empty, or too-big range (%s,%s,%s) in for() stm;".formatted(startI,stopE,step));
		for(int i=0;i<estLength;i++) {
			Number n;
			if(this.isRangeInt)n=(Long)(startI.longValue()+step.longValue()*i);
			else n= (Double)(startI.doubleValue()+step.doubleValue()*i);
			//ternary op will mess with type
			this.span.add(n);
		}
	}

	@Override
	public String getFlowType() {
		return "for";
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
	public void compileMe(PrintStream p, Compiler c, Scope s) throws CompileError {
		ResourceLocation mcf=this.mySubscope.getSubRes();
		Variable mybreak=this.mySubscope.getBreakVarInMe(c);
		mybreak.setMeToBoolean(p, c, s, mystack, false);
		for(Number n:this.span) {
			this.counter.setMeToNumber(p, c, s, mystack, n);
			p.printf("execute unless %s run function %s\n", mybreak.isTrue(),mcf);
		}
		
		mystack.finish(c.job);
	}

	@Override
	public String asString() {
		return "<for (...)>";
	}
	@Override
	public boolean canBreak() {
		return true;
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
