package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.McThread;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Entity;
/**
 * call to start, restart or stop a thread
 * 
 * usgage:
 * start <thread name> ([starting point],[entity var to store to]);
 * @author jbarb_t8a3esk
 *
 */
public class ThreadCall extends Statement {
	public Boolean checkForExecutor(Compiler c, Matcher matcher) throws CompileError {
		//nullable
		Selector s = Entity.checkForSelectorOrEntity(c, c.currentScope, matcher, line, col);
		this.me=s;
		if(s==null) return null;
		
		return BuiltinFunction.findArgsep(c);
	}
	private String linebuff = null;
	public Boolean checkForBlock(Compiler c, Matcher matcher) throws CompileError {
		//nullable
		if(this.thread!=null) {
			Integer b = this.thread.checkForBlockNumberName(c, c.currentScope, matcher);
			this.block = b;
			if(b==null) return null;
		}else {
			this.linebuff = McThread.checkForBlockName(c, c.currentScope, matcher);
			if(this.linebuff==null) return null;
		}

		return BuiltinFunction.findArgsep(c);
	}
	public Boolean checkForToset(Compiler c, Matcher matcher) throws CompileError {
		//nullable
		int start = c.cursor;
		Token mn = c.nextNonNullMatch(Factories.checkForMembName);
		if(!(mn instanceof MemberName)) {
			return null;
		}
		Variable v;
		try{
			v= c.myInterface.identifyVariable((MemberName) mn, c.currentScope);
		}catch (CompileError err) {v=null;}
		this.toset = v;
		if(v==null) {
			c.cursor = start;
			return null;
		}
		return BuiltinFunction.findArgsep(c);
	}
	public static final ThreadCall make(Compiler c, Matcher matcher, int line, int col,Keyword w,boolean isPass1,boolean lineEnd) throws CompileError {
		return make(c, matcher, line, col, w,isPass1, lineEnd, null);
	}

	public static final ThreadCall make(Compiler c, Matcher matcher, int line, int col,Keyword w,boolean isPass1,boolean lineEnd,ThreadStm define) throws CompileError {
		//pass 2 only
		c.cursor=matcher.end();
		ThreadCall me = new ThreadCall(line,col,w);
		if(isPass1) {
			Token term=Factories.carefullSkipStm(c, matcher, line, col);
			if((!(term instanceof Token.LineEnd)))throw new CompileError.UnexpectedToken(term,";");
			if(!lineEnd) c.cursor --;
			return me;
		}
		if(define==null && !isPass1) {
			Token name = c.nextNonNullMatch(Factories.checkForMembName);
			if(!(name instanceof MemberName)) throw new CompileError.UnexpectedToken(name, "thread name");
			me.thread = c.myInterface.identifyThread((MemberName) name, c.currentScope);
		}else {
			me.thread = define.myThread; // change later
			me.define=define;
		}
		boolean hasArgs = BuiltinFunction.openIf(c);
		if(hasArgs) {
			switch (w) {
			case START:{
				boolean hasNext=true;
				Boolean flag;
				
				flag = me.checkForBlock(c, matcher);
				if(flag!=null)hasNext=flag;
				if(!hasNext)break;

				flag = me.checkForToset(c, matcher);
				if(flag!=null)hasNext=flag;
				if(hasNext) hasNext = BuiltinFunction.findArgsep(c);
				if(hasNext) throw new CompileError("unexpected lack of ')' in thread call");
			}
				
				break;
			case RESTART:
			case STOP:
			{
				boolean hasNext=true;
				Boolean flag;
				
				if(!me.thread.isGlobal()) {
					flag = me.checkForExecutor(c, matcher);
					if(flag!=null)hasNext=flag;
				} if (w==Keyword.RESTART && hasNext)  {
					flag=me.checkForBlock(c, matcher);
					if(flag!=null)hasNext=flag;
				}
				if(hasNext) hasNext = BuiltinFunction.findArgsep(c);
				if(hasNext) throw new CompileError("unexpected lack of ')' in thread call");
			}
				break;
			default:
				throw new CompileError("invalid Start keyword");
			}
		}
		me.defaultArgs();
		if(lineEnd) {
			Statement.nextIsLineEnder(c, matcher,false);
		}
		return me;
	}
	private void defaultArgs() {
		switch(this.init) {
		case START:{
			//do nothing
		}break;
		case STOP:
		case RESTART: 
		{
			if(this.me==null && this.thread!=null)this.me=this.thread.getAllExecutors();
		}break;
		default:
			break;
		}
	}
	final Keyword init;
	McThread thread;
	ThreadStm define = null;
	Integer block = null;
	Variable toset = null;
	Selector me = null;
	public ThreadCall(int line, int col,Keyword init) {
		super(line, col);
		this.init=init;
	}
	public static void tagAndCall(PrintStream p,Compiler c,Scope s, Selector me,ResourceLocation func) {
		if(me!=null )me.addTag(p,McThread.TEMPTAG);
		func.run(p);
		if(me!=null )me.removeTag(p,McThread.TEMPTAG);
	}

	@Override
	public void compileMe(PrintStream f, Compiler c, Scope s) throws CompileError {
		if(this.define!=null) {
			this.thread = this.define.myThread;
			if(this.linebuff!=null) {
				this.block = this.thread.getBlockNumber(linebuff, s.getThread() == this.thread);
				if(this.block==null)throw new CompileError("thread %s has no accessible block named '%s';".formatted(this.thread.getName(),this.linebuff));
			}
			defaultArgs();
		}
		switch(this.init) {
		case START:{
			RStack stack = s.getStackFor();
			this.thread.start(f, c, s, stack, block, toset);
			stack.clear();stack.finish(c.job);
		}break;
		case STOP:{
			RStack stack = s.getStackFor();
			ThreadCall.tagAndCall(f, c, s, this.me, this.thread.pathStop());
			stack.clear();stack.finish(c.job);
		}break;
		case RESTART:{
			RStack stack = s.getStackFor();
			ThreadCall.tagAndCall(f, c, s, this.me, this.thread.pathRestart());
			stack.clear();stack.finish(c.job);
		}break;
		}
		
		
	}

	@Override
	public String asString() {
		return "<%s>".formatted(this.init.name);
	}

}
