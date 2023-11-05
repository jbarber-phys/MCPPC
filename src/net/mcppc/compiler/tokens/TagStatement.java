package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Function;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Statement.CodeBlockOpener;
import net.mcppc.compiler.tokens.Statement.Flow;
/**
 * used to make tagged code
 * 
 * usage:
 * tag <thetag> ;
 * tags the mcfunction it is in
 * 
 * or:
 * tag <thetaag> {...
 * to tag the following block of code
 * 
 * can also use tick or load instead of tag minecraft:tick or tag minecraft:load
 * @author RadiumE13
 *
 */
public class TagStatement extends Statement implements Flow, CodeBlockOpener {
	private static final String FLOWTYPE = "tag";
	public static TagStatement skipMe(Compiler c, Matcher matcher, int line, int col,Keyword opener) throws CompileError {
		return makeMe(c,matcher,line,col,opener,true);
	}
	public static TagStatement makeMe(Compiler c, Matcher matcher, int line, int col,Keyword opener) throws CompileError {
		return makeMe(c,matcher,line,col,opener,false);
	}
	private static TagStatement makeMe(Compiler c, Matcher matcher, int line, int col,Keyword opener,boolean ispass1) throws CompileError {
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow skip ifElse %s;\n", opener);
		Token t;
		ResourceLocation tag = getNamedRes(opener);
		if(tag==null) {
			tag = ResourceLocation.getNext(c).res;
		}
		TagStatement me=new TagStatement(line,col,tag, c.cursor);
		Token term=Factories.carefullSkipStm(c, matcher, line, col);
		if(term instanceof Token.CodeBlockBrace) {
			if((!((Token.CodeBlockBrace)term).forward)) throw new CompileError.UnexpectedToken(term,"{ or ;");
			me.openedBlock=true;
			me.taggedScope = c.currentScope.subscope(c,me,ispass1);
		} else {
			//stay false
			me.taggedScope = c.baseScope; // use base scope, not current one; functions cannot be tagged;
		}
		return me;
	}
	public final ResourceLocation tag;
	Scope taggedScope;
	boolean openedBlock=false;
	public TagStatement(int line, int col,ResourceLocation tag, int cursor) {
		super(line, col, cursor);
		this.tag=tag;
	}
	private static ResourceLocation getNamedRes(Keyword opener) {
		switch (opener) {
		case TICK: return CompileJob.TAG_TICK;
		case LOAD: return CompileJob.TAG_LOAD;
		default: return null;
		}
	}

	@Override
	public boolean didOpenCodeBlock() {
		return this.openedBlock;
	}

	@Override
	public Scope getNewScope() {
		return this.taggedScope;
	}

	@Override
	public void addToEndOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
	}

	@Override
	public void addToStartOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
	}

	@Override
	public void compileMe(PrintStream f, Compiler c, Scope s) throws CompileError {
		//System.err.printf("compiling tag %s\n", this.tag);
		c.job.addTaggedFunction(this.tag, this.taggedScope.getSubRes());
	}

	@Override
	public String asString() {
		return "tag %s %s".formatted(this.tag, this.openedBlock ? "{" : ";") ;
	}
	@Override
	public String getFlowType() {
		return FLOWTYPE;
	}
	@Override
	public boolean canBreak() {
		return false;
	}

}
