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
	private boolean canRequest=false;
	public void compileMyBlock(Compiler c) throws CompileError, FileNotFoundException{
		Scope s=this.scope;
		//System.err.printf("block at %s\n",this.scope.getSubRes());
		if(this.canRequest=s.hasTemplate())  
		for(TemplateArgsToken targs:s.getAllDefaultTemplateArgs()) {
			//System.err.printf("block has an %dth template template\n",i++);
			s.bindTemplateToMe(targs);
			compileAScope(c,s);
		}else {
			//System.err.printf("block has no template\n");
			compileAScope(c,s);
		}
	}
	public void compileMyBlockMore(Compiler c) throws CompileError, FileNotFoundException{
		Scope s=this.scope;
		//System.err.printf("block at %s\n",this.scope.getSubRes());
		if(s.hasTemplate())  
		for(TemplateArgsToken targs:s.getAllDefaultTemplateArgs()) {
			//System.err.printf("block has an %dth template template\n",i++);
			s.bindTemplateToMe(targs);
			compileAScope(c,s);
		}else {
			//do nothing
		}
	}
	private void compileAScope(Compiler c,Scope s) throws CompileError, FileNotFoundException{
		c.currentScope=s;
		s.open(c.job);
		for(Statement st:this.statements) {
			c.compileLine(s.out, s, st);
			//st.compileMe(s.out, c, s);
		}
		if(this.opener!=null)opener.addToEndOfMyBlock(s.out, c, s);
		
		s.closeJustMyFiles();
		
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
	public boolean canRequest() {
		return canRequest;
	}

}
