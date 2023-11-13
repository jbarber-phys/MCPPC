package net.mcppc.compiler.functions;

import java.io.PrintStream;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.Function.FuncCallToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.TemplateArgsToken;
import net.mcppc.compiler.tokens.Token;

public abstract class AbstractCallToken extends Token {

	public AbstractCallToken(int line, int col) {
		super(line, col);
	}
	abstract public void call(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError;
	abstract public void getRet(PrintStream p,Compiler c,Scope s,RStack stack,int home, VarType typeWanted) throws CompileError ;
	abstract public void getRet(PrintStream p,Compiler c,Scope s,Variable v,RStack stack) throws CompileError ;
	abstract public VarType getRetType();
	abstract public Number getEstimate();
	abstract public AbstractCallToken withTemplate(TemplateArgsToken tgs) ;
	abstract public boolean hasTemplate() ;
	
	Variable thisBound=null;
	public boolean hasThisBound() {return thisBound!=null;
	}
	public Variable getThisBound() {
		return thisBound;
	}
	VarType staticType=null;
	public boolean hasStaticType() {return staticType!=null;
	}
	public VarType getStaticType() {
		return staticType;
	}
	public void withStatic(VarType of) {
		this.staticType=of;
	}
	public void withThis(Variable self) {
		thisBound=self;
	}
	public abstract void dumpRet(PrintStream p, Compiler c, Scope s, RStack stack) throws CompileError ;
}
