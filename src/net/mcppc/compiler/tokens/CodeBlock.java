package net.mcppc.compiler.tokens;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.errors.CompileError;

public class CodeBlock extends Statement {
	public final Scope scope;
	public final Statement.CodeBlockOpener opener;
	
	final List<Statement> statements=new ArrayList<Statement>();

	public CodeBlock(int line, int col,Scope s,CodeBlockOpener opener) {
		super(line, col);
		this.scope=s;
		this.opener= opener;
	}
	public void addStatement(Statement sm) {
		this.statements.add(sm);
	}
	@Override
	public void compileMe(PrintStream f,Compiler c,Scope s) {
		// do nothing; code will be compiled in different file
	}
	public void compileMyBlock(Compiler c) throws CompileError, FileNotFoundException{
		c.currentScope=this.scope;
		this.scope.open(c.job);
		for(Statement s:this.statements) {
			//s.printStatementTree(CompileJob.compileMcfLog, 0);
			s.compileMe(this.scope.out, c, scope);
		}
		if(this.opener!=null)opener.addToEndOfMyBlock(this.scope.out, c, this.scope);
		
		this.scope.closeJustMyFiles();
		for(Statement block:this.statements) if (block instanceof CodeBlock){
			((CodeBlock) block).compileMyBlock(c);
		}
	}

	@Override
	public String asString() {

		return "{...}";
	}
	@Override public void printStatementTree(PrintStream p,int tabs) {
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		p.printf("%s{\n", s.toString());
		for(Statement sm:this.statements) {
			sm.printStatementTree(CompileJob.compileMcfLog, tabs+1);
		}
		p.printf("%s}\n", s.toString());
	}

}
