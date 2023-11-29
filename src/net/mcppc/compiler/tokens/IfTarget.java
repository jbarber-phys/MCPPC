package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Function;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.tokens.Statement.CodeBlockOpener;
import net.mcppc.compiler.tokens.Statement.MultiFlow;
/**
 * compiles block only if the version target is met;<p>
 * syntax for first block: target [versionrange] {... <p>
 * syntax for later blocks: else target [versionrange]|default {...<p>
 * 
 * @author RadiumE13
 *
 */
public class IfTarget extends Statement implements MultiFlow, CodeBlockOpener {
	public static IfTarget skipMe(Compiler c, Matcher matcher, int line, int col,boolean isElse) throws CompileError {
		c.cursor=matcher.end();
		VTarget tgt;
		if(Keyword.checkFor(c, matcher, Keyword.DEFAULT)) {
			tgt = null;
		}else {
			VTarget.TargetToken t = (VTarget.TargetToken) c.nextNonNullMatch(Factories.genericLook(VTarget.TargetToken.factory));
			tgt = t.target;
		}
		
		IfTarget me=new IfTarget(c,tgt,isElse);
		me.mySubscope = c.currentScope.subscope(c,me,true);
		Token term=Factories.carefullSkipStm(c, matcher, line, col);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	public static IfTarget makeMe(Compiler c, Matcher matcher, int line, int col,boolean isElse) throws CompileError {
		c.cursor=matcher.end();
		VTarget tgt;
		if(Keyword.checkFor(c, matcher, Keyword.DEFAULT)) {
			tgt = null;
		}else {
			VTarget.TargetToken t = (VTarget.TargetToken) c.nextNonNullMatch(Factories.genericLook(VTarget.TargetToken.factory));
			tgt = t.target;
		}
		
		IfTarget me=new IfTarget(c,tgt,isElse);
		me.mySubscope = c.currentScope.subscope(c,me,false);
		Token term=c.nextNonNullMatch(Factories.nextIsLineEnd);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	Scope mySubscope;
	IfTarget predicessor=null;
	boolean isElse;
	private final VTarget target;//null for default
	public IfTarget(Compiler c,VTarget target,boolean isElse) {
		super(c);
		this.target=target;
		this.isElse = isElse;
	}

	@Override
	public String getFlowType() {
		return "target";
	}

	@Override
	public boolean canBreak() {
		return false;
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

	}

	@Override
	public void addToStartOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		if(this.target!=null)s.restrictTarget(this.target);
	}

	@Override
	public boolean sendForward() {
		return this.target!=null;
	}

	@Override
	public boolean setPredicessor(MultiFlow pred) throws CompileError {
		if(pred instanceof IfTarget) {
			this.predicessor=(IfTarget) pred;
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
		return this.isElse;
	}
	boolean hasOtherBlockCompiled = false;
	private boolean didOtherBlockCompile() {
		if(this.predicessor==null) return this.hasOtherBlockCompiled;
		else return this.predicessor.hasOtherBlockCompiled;
	}
	private void setWillCompile() {
		if(this.predicessor==null) this.hasOtherBlockCompiled = true;
		else this.predicessor.hasOtherBlockCompiled = true;
	}
	private boolean makeWillCompile(VTarget scopeTarget) {
		if(this.didOtherBlockCompile()) return false;
		VTarget tg = this.target==null? scopeTarget:scopeTarget.intersection(this.target);
		if (tg.isEmpty()) return false;
		else {
			this.setWillCompile();
			return true;
		}
	}
	@Override
	public void compileMe(PrintStream f, Compiler c, Scope s) throws CompileError {
		this.willCompile = this.makeWillCompile(s.getTarget());
		if(this.cancelCompilation(c, s)) return;
		else {
			VTarget newTarget =this.target==null? s.getTarget(): s.getTarget().intersection(this.target);
			ResourceLocation mcf=this.mySubscope.getSubRes();
			mcf.run(f, newTarget);
		}

	}

	@Override
	public String asString() {
		return "target ...";
	}
	private boolean willCompile=true;
	@Override
	public boolean cancelCompilation(Compiler c, Scope s) throws CompileError {
		return !this.willCompile;
	}

}
