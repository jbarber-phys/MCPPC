package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.*;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.AbstractCallToken;
import net.mcppc.compiler.tokens.Equation.End;
import net.mcppc.compiler.tokens.Token.Assignlike;
import net.mcppc.compiler.tokens.Token.Assignlike.Kind;

/**
 * like a token but can generate code
 * examples:
 * code lines
 * equations and equation segments
 * 
 * @author jbarb_t8a3esk
 *
 */
public abstract class Statement extends Token implements TreePrintable{
	public static interface Headerable {
		public boolean doHeader();
		public void headerMe(PrintStream f) throws CompileError;
	}
	public static interface Flow{
		public String getFlowType();
		default public boolean canRecurr() {
			return false;
		}
		public boolean canBreak();
	}
	public static interface MultiFlow extends Flow{
		//implement this if its a flow statement that affects later flow statements: like if -> elif -> else
		public boolean sendForward();//true for if,  elif
		public boolean setPredicessor(MultiFlow pred) throws CompileError ;//tells this what the previous flow statement was; return success flag
		public boolean claim();//false; make true if this block becomes the new previous flow
		public boolean recive();//true for elif, else
		
		default public boolean makeDoneVar() {
			return this.claim() || !this.recive();
		}
	}
	public static interface CodeBlockOpener {
		public boolean didOpenCodeBlock();
		public Scope getNewScope();
		public void addToEndOfMyBlock(PrintStream p, Compiler c, Scope s)throws CompileError;
		public void addToStartOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError;
	}
	public static interface IFunctionMaker{
		public boolean willMakeBlocks() throws CompileError;
		public void compileMyBlocks(Compiler c) throws CompileError;
	}
	public static abstract class Factory extends Token.Factory {
		public abstract Statement createStatement(Compiler c, Matcher matcher, int line, int col) throws CompileError;
		
		public Factory(Pattern type) {
			super(type);
		}

		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			return this.createStatement(c, matcher, line, col);
		}
		
	}
	public Statement(int line, int col) {
		super(line, col);
	}
	@Override public void printStatementTree(PrintStream p,int tabs) {
		//for debuging
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		p.printf("%s<%s>;\n", s.toString(),this.getClass().getSimpleName());
	}
	public abstract void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError ;
	
	
	public static boolean nextIsLineEnder(Compiler c,Matcher m) throws CompileError {
		Token term=c.nextNonNullMatch(Factories.nextIsLineEnd);
		if(term instanceof Token.CodeBlockBrace) {
			if((!((Token.CodeBlockBrace)term).forward)) throw new CompileError.UnexpectedToken(term,"{");
			return true;
		}else {
			return false;
		}

	}
	public static boolean nextIsLineEnder(Compiler c,Matcher m,boolean openBlock) throws CompileError {
		Token term=c.nextNonNullMatch(Factories.nextIsLineEnd);
		if(term instanceof Token.CodeBlockBrace) {
			if(!openBlock)throw new CompileError.UnexpectedToken(term,";");
			if((!((Token.CodeBlockBrace)term).forward)) throw new CompileError.UnexpectedToken(term,"{");
			return true;
		}else {
			if(openBlock)throw new CompileError.UnexpectedToken(term,"{");
			return false;
		}

	}
	/**
	 * comment that will show in both headers and mcfunctions
	 * @author jbarb_t8a3esk
	 *
	 */
	public static class Domment extends Statement implements Headerable{
		public static final Statement.Factory factory=new Statement.Factory(Regexes.DOMMENT) {
			@Override public Statement createStatement(Compiler c, Matcher matcher, int line, int col) {
				if(CompileJob.dommentLog!=null)CompileJob.dommentLog.printf("///%s\n", matcher.group(1));
				c.cursor=matcher.end(); return new Domment(line,col,matcher.group(1));
			}
		};
		String message;
		public Domment(int line, int col,String message) {
			super(line, col);
			this.message=message;
		}
		@Override public void compileMe(PrintStream f,Compiler c,Scope s) {
			f.println("#%s".formatted(this.message));
		}
		@Override
		public String asString() {
			return "///...";
		}
		public String inHeader() {
			return "///%s".formatted(this.message);
		}
		public String inCMD() {
			return "#%s".formatted(this.message);
		}
		@Override
		public boolean doHeader() {
			return true;
		}
		@Override
		public void headerMe(PrintStream f) throws CompileError {
			f.println(this.inHeader());
			
		}
		
	}
	/**
	 * statement for vanilla command; can only appear as a line start (has no prefix command);
	 * must be semicolon terminated;
	 * @author jbarb_t8a3esk
	 */
	public static class CommandStatement extends Statement{
		public static final Statement.Factory factory=new Statement.Factory(Regexes.CMD) {
			@Override public Statement createStatement(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				CommandToken t=CommandToken.formatted(c, matcher, line, col, true);
				if(c.nextNonNullMatch(Factories.nextIsLineEnd) instanceof Token.LineEnd)
					c.cursor=matcher.end();
				else throw new CompileError.UnexpectedToken(t, ";");
				return new CommandStatement(line,col,t);
			}
		};
		final CommandToken command;
		public CommandStatement(int line, int col,CommandToken command) {
			super(line, col);
			this.command=command;
		}
		@Override public void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError {
			//show the slash to show this one is verbatim from src with no pre-conditions added
			//but actually dont do this, it causes a problem;
			//java.util.concurrent.CompletionException: java.lang.IllegalArgumentException:
			//Unknown or invalid command '/say "hello from line 8"' on line 1 
			//(did you mean 'say'? Do not use a preceding forwards slash.)
			//f.println("/%s".formatted(this.command.inCMD()));
			this.command.printToCMD(f, c, s);
		}
		@Override
		public String asString() {
			return "/...;";
		}
		
	}
	public static class Assignment extends Statement{
		final Variable var;
		final Equation eq;
		public Assignment(int line, int col,Variable var,Equation eq) {
			super(line, col);
			this.eq=eq;
			this.var=var;
		}
		@Override public void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError {
			if(this.var!=null) {
				this.eq.compileOps(f, c, s, this.var.type);
				this.eq.setVar(f, c, s, this.var);
				this.eq.stack.finish(c.job);
			}else {
				//never
			}
		}
		@Override
		public String asString() {
			return "%s=...;".formatted(this.var.name);
		}
		@Override public void printStatementTree(PrintStream p,int tabs) {
			super.printStatementTree(p, tabs);
			StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
			this.eq.printTree(p, tabs+1);
		}
		public static Assignment makeReturn(Compiler c,Matcher m,int line,int col) throws CompileError {
			c.cursor=m.end();
			Token asn=c.nextNonNullMatch(Factories.checkForAssignlike);
			if(!c.currentScope.isInFunctionDefine()) throw new CompileError("unexpected return statement outside of function;");
			Variable ret=c.currentScope.getFunction().returnV;
			//calls to makeReturn are not allowed
			//? =
			if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ASSIGNMENT) {
				Equation eq=Equation.toAssign(line, col, c, m);
				//equation finds the semicolon;
				if(eq.end !=End.STMTEND) throw new CompileError("assignment ended with a non-';'");
				return new Statement.Assignment(line, col, ret, eq);
			}else
				throw new CompileError.UnexpectedToken(asn, "'=' or '~~' or '('","return statements must be assignlike as they do not affect flow; example: return = 23;");
			
			
		}
		//break has optional arg for depth; zero depth is default; break = true; break(0)=true;
		public static Assignment makeBreak(Compiler c,Matcher m,int line,int col) throws CompileError {
			c.cursor=m.end();
			Token t=c.nextNonNullMatch(Factories.checkForParen);
			int depth=0;
			if(t instanceof Token.Paren) {
				if(!((Token.Paren) t).forward)throw new CompileError.UnexpectedToken(t, "'(' or '='");
				t=c.nextNonNullMatch(Factories.nextNum);
				if(!(t instanceof Num))throw new CompileError.UnexpectedToken(t, "positive int number");
				Number n=((Num)t).value;
				if(!CMath.isNumberInt(n)) throw new CompileError.UnexpectedToken(t, "positive int number");
				depth=n.intValue();
				if (depth<0)throw new CompileError.UnexpectedToken(t, "positive int number");
				t=c.nextNonNullMatch(Factories.closeParen);
				
			}
			Token asn=c.nextNonNullMatch(Factories.checkForAssignlike);
			if(!c.currentScope.hasBreak(depth)) throw new CompileError("tried to break with depth %d that did not exist;".formatted(depth));
			Variable brk=c.currentScope.getBreakVarInMe(c,depth);
			//? =
			if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ASSIGNMENT) {
				Equation eq=Equation.toAssign(line, col, c, m);
				//equation finds the semicolon;
				if(eq.end !=End.STMTEND) throw new CompileError("assignment ended with a non-';'");
				return new Statement.Assignment(line, col, brk, eq);
			}else
				throw new CompileError.UnexpectedToken(asn, "'=' or '~~' or '('","return statements must be assignlike as they do not affect flow; example: return = 23;");
			
			
		}
		
	}
	public static class Estimate extends Statement{
		final Variable var;
		final Number estimate;
		public Estimate(int line, int col,Variable var,Number num) {
			super(line, col);
			this.var=var;
			this.estimate=num;
		}
		@Override public void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError{
			//this is handled at tokenization phase
			
		}
		@Override
		public String asString() {
			return "%s~~...;".formatted(this.var.name);
		}
		
	}
	public static class CallStatement extends Statement{
		final AbstractCallToken token;
		public CallStatement(int line, int col,AbstractCallToken f,Compiler c) {
			super(line, col);
			this.token=f;
		}
		@Override
		public String asString() {
			return "%s;".formatted(this.token.asString());
		}
		@Override
		public void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError {
			RStack stack=s.getStackFor();
			this.token.call(f, c, s, stack);
			this.token.dumpRet(f, c, s, stack);
			stack.finish(c.job);
		}
		@Override 
		public void printStatementTree(PrintStream p,int tabs) {
			//for debuging
			if(this.token instanceof BuiltinFunction.BFCallToken)
				((BFCallToken) this.token).getBF().printStatementTree(p, (BFCallToken) this.token, tabs);
			else super.printStatementTree(p, tabs);
		}
		
	}
}
