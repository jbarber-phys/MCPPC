package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.*;
import net.mcppc.compiler.errors.CompileError;

/**
 * like a token but can generate code
 * examples:
 * code lines
 * equations and equation segments
 * 
 * @author jbarb_t8a3esk
 *
 */
public abstract class Statement extends Token {
	//public boolean isOpeningCodeBlock=false;
	public static interface Headerable {
		public boolean doHeader();
		public void headerMe(PrintStream f) throws CompileError;
	}
	public static interface MultiFlow{
		//implement this if its a flow statement that affects later flow statements: like if -> elif -> else
		public boolean sendForward();//true for if,  elif
		public boolean setPredicessor(MultiFlow pred);//tells this what the previous flow statement was
		public boolean claim();//false; make true if this block becomes the new previous flow
		public boolean recive();//true for elif, else
	}
	public static interface CodeBlockOpener {
		public boolean didOpenCommandBlock();
		public Scope getNewScope();
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
	public void printStatementTree(PrintStream p,int tabs) {
		//for debuging
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		p.printf("%s<%s>;\n", s.toString(),this.getClass().getSimpleName());
	}
	public abstract void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError ;
	/**
	 * comment that will show in both headers and mcfunctions
	 * @author jbarb_t8a3esk
	 *
	 */
	public static class Domment extends Statement implements Headerable{
		public static final Statement.Factory factory=new Statement.Factory(Regexes.DOMMENT) {
			@Override public Statement createStatement(Compiler c, Matcher matcher, int line, int col) {
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
	 * token for vanilla command; can appear in middle of equation/ etc;
	 * may have prefix commands added to get returned value
	 * example:
	 * a=/time query daytime;
	 * compiles to:
	 * execute store result storage <file_resourcelocation> a int 1 run time query daytime
	 * @author jbarb_t8a3esk
	 *
	 */
	public static class CommandToken extends Token{

		public static final Token.Factory factory=new Token.Factory(Regexes.CMD) {
			@Override
			public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new CommandToken(line,col,matcher.group(1));
			}
		};
		final String cmd;
		public CommandToken(int line, int col, String group) {
			super(line,col);
			this.cmd=group;
		}
		
		@Override
		public String asString() {
			return "/...";
		}
		public String inCMD() {
			//with no slash
			return this.cmd;
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
				c.cursor=matcher.end();
				CommandToken t=new CommandToken(line,col,matcher.group(1));
				if(c.nextNonNullMatch(Factories.nextIsLineEnd) instanceof Token.LineEnd)throw new CompileError.UnexpectedToken(t, ";");
				return new CommandStatement(line,col,t);
			}
		};
		final CommandToken command;
		public CommandStatement(int line, int col,CommandToken command) {
			super(line, col);
			this.command=command;
		}
		@Override public void compileMe(PrintStream f,Compiler c,Scope s) {
			//show the slash to show this one is verbatim from src with no pre-conditions added
			f.println("/%s".formatted(this.command.inCMD()));
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
			this.var=var;
			this.eq=eq;
		}
		@Override public void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError {
			this.eq.compileOps(f, c, s, this.var.type);
			this.eq.setVar(f, c, s, this.var);
			this.eq.stack.finish(c.job);
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
	public static class FuncCallStatement extends Statement{
		final Function.FuncCallToken token;
		Scope s;
		public FuncCallStatement(int line, int col,Function.FuncCallToken f,Compiler c) {
			super(line, col);
			this.token=f;
			this.s=c.currentScope;
		}
		@Override
		public String asString() {
			return "%s;".formatted(this.token.asString());
		}
		@Override
		public void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError {
			// TODO Auto-generated method stub
			Register.RStack stack=this.s.getStackFor();
			this.token.call(f, c, s, stack);
			stack.finish(c.job);
		}
		
	}
	public static class BuiltinFuncCallStatement extends Statement{
		final BuiltinFunction.BFCallToken token;
		public BuiltinFuncCallStatement(int line, int col,BuiltinFunction.BFCallToken f) {
			super(line, col);
			this.token=f;
		}
		@Override
		public String asString() {
			return "%s;".formatted(this.token.asString());
		}
		@Override
		public void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError {
			// TODO Auto-generated method stub
			
			//remember to finish any stacks you create
		}
		
	}
}
