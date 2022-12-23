package net.mcppc.compiler.tokens;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.McThread;
import net.mcppc.compiler.PrintStreamLineCounting;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.BuiltinFunction.BasicArgs;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Execute.Subexecute;
import net.mcppc.compiler.tokens.Execute.Subgetter;
import net.mcppc.compiler.tokens.Statement.CodeBlockOpener;
/**
 * defines a thread
 * syntax:
 * thread [synchronized] [public/private <name>] [as() / asat()] [<controls>] {}
 * next [<controls>] {}
 * next [<controls>] then
 * next stop/restart [start ...];
 * 
 * <controls>:
 * wait(<delay>) //wait until next block
 * while / until (<condition>) wait (<delay>) // wait until next loop
 * stop [start...] //end thread
 * @author jbarb_t8a3esk
 *
 */
public class ThreadStm extends Statement implements Statement.IFunctionMaker,
									Statement.Headerable
									,CodeBlockOpener
									,Statement.MultiFlow {
	public static final Map<String,BlockControlGetter> BLOCK_CONTROLS = new HashMap<String,BlockControlGetter>();
	public static void register(String name, BlockControlGetter g) {
		BLOCK_CONTROLS.put(name, g);
	}
	 static {
		 register(Delay.name,Delay::make);
		 register(Loop.loopwhile,Loop::makeWhile);
		 register(Loop.loopuntil,Loop::makeUntil);
		 register(End.endstop,End::makeStop);
		 register(End.endrestart,End::makeRestart);
		}
	public static abstract class ThreadBlock extends Token {
		public final String name;
		public final int index;
		public ThreadBlock(int line, int col,int index,String name) {//
			super(line, col);
			this.name=name;
			this.index=index;
		}
		@Override
		public String asString() {
			return "-%s ...".formatted(this.name);
		}
		public boolean isEnd() {return false;}
		public void addToEndOfBeforeBlock(PrintStream p, Compiler c, Scope s,ThreadStm stm) throws CompileError{
			
		}
		public void addToStartOfAfterBlock(PrintStream p, Compiler c, Scope s,ThreadStm stm) throws CompileError{
		}
		public void addToEndOfAfterBlock(PrintStream p, Compiler c, Scope s,ThreadStm stm) throws CompileError{
			
		}
		public void addToDeclaration(PrintStream p, Compiler c, Scope s,ThreadStm stm) throws CompileError{
			//do nothing
		}
		
	}
	@FunctionalInterface
	public static interface BlockControlGetter {
		//start after the name of this
		public ThreadBlock make(Compiler c, Matcher matcher, int line, int col,ThreadStm stm,int index, boolean isPass1) throws CompileError;
	}
	
	public static ThreadStm skipMe(Compiler c, Matcher matcher, int line, int col,Keyword w) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		ThreadStm me=new ThreadStm(line,col,w,null);
		me.outerScope = c.currentScope;
		//me.mySubscope = c.currentScope.subscope(c,me,true);
		if(me.isFirst) {
			me.myThread = new McThread().populate(c, matcher, line, col,me,true);
		}else {
			//do later in setPredicessor(...)
		}
		me.makeBlockControl(c, matcher, line, col, true);
		//Token term=Factories.carefullSkipStm(c, matcher, line, col);
		//if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");

		me.endStatement(c,matcher);
		return me;
	}
	public static ThreadStm makeMe(Compiler c, Matcher matcher, int line, int col,Keyword opener) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		//CompileJob.compileMcfLog.printf("flow ifElse %s;\n", opener);
		//Equation eq=null;
		RStack stack=c.currentScope.getStackFor();
		ThreadStm me=new ThreadStm(line,col,opener,stack);
		me.outerScope = c.currentScope;
		//me.mySubscope = c.currentScope.subscope(c,me,false);
		if(me.isFirst) {
			me.myThread = new McThread().populate(c, matcher, line, col,me,false);
			me.startedBy = me;
		} else {
			//do later in setPredicessor(...)
		}
		me.makeBlockControl(c, matcher, line, col, false);
		me.endStatement(c,matcher);
		return me;
	}
	private void endStatement(Compiler c, Matcher matcher) throws CompileError{
		if(this.isLast) {
			Statement.nextIsLineEnder(c, matcher,false);
		}else if (this.hasBlock) {
			Statement.nextIsLineEnder(c, matcher,true);
		}else {
			if(!Keyword.checkFor(c, matcher, Keyword.THEN))
				throw new CompileError("expected a '%s' keyword terminating a blockless thread statement;".formatted(Keyword.THEN.name));
		}
	}
	private boolean hasAddedBlockNumberToThread=false;
	private void addBlockNumberToThread(Scope s) throws CompileError {
		this.myThread.ensureSize(this, this.blockNumber);
		if(!this.hasAddedBlockNumberToThread && this.myThread!=null) {
			//System.err.printf("thread scope being set\n");
			this.mySubscope = new Scope(s,this.myThread,this.blockNumber,this.canBreak());
			if(this.blockName!=null ) {
				this.myThread.addBlockName(blockName, blockAccess, blockNumber);
			}
		}
		
	}
	public void makeBlockControl(Compiler c, Matcher matcher, int line, int col,boolean isPass1) throws CompileError {
		//find control statements for block
		this.blockAccess=Keyword.checkFor(c, matcher, Keyword.PUBLIC,Keyword.PRIVATE);
		if(this.blockAccess!=null) {
			Token name = c.nextNonNullMatch(Factories.checkForBasicName);
			if(!(name instanceof Token.BasicName)) throw new CompileError.UnexpectedToken(name, "name");
			this.blockName = ((Token.BasicName) name).name;
		}
		if(this.isFirst) {
			this.blockNumber=1;
			this.addBlockNumberToThread(this.outerScope);
		}
		boolean last=false;
		while(true) {
			int cs = c.cursor;
			Token t = c.nextNonNullMatch(Factories.checkForBasicName);
			if(!(t instanceof Token.BasicName)) break;
			String name=((Token.BasicName) t).name;
			BlockControlGetter g = ThreadStm.BLOCK_CONTROLS.get(name);
			if (g==null) {
				c.cursor=cs;
				break;
			}
			ThreadBlock sub=g.make(c, matcher, line, col,this,this.blockControls.size(),isPass1);
			this.blockControls.add(sub);
			if(sub.isEnd())break;
		}
	}
	private final RStack mystack;
	private final boolean isFirst;
	private boolean isLast=false;
	private boolean hasBlock=true;
	Scope outerScope;
	Scope mySubscope;
	McThread myThread;
	
	int blockNumber = 1;//starts at 1
	public int getBlockNumber() {return this.blockNumber;}
	String blockName;
	Keyword blockAccess;
	
	public final List<ThreadBlock> blockControls = new ArrayList<>();
	
	
	private String loopIf = null;
	public ThreadStm(int line, int col,Keyword opener,RStack stack) {
		super(line, col);
		this.mystack=stack;
		this.isFirst = opener == Keyword.THREAD;
	}

	@Override
	public boolean didOpenCodeBlock() {
		return this.hasBlock;
	}

	@Override
	public Scope getNewScope() {
		return this.mySubscope;
	}
	@Override
	public void addToStartOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {

		this.myThread.myGoto(this.myThread.getSelf()).setMeToNumber(p, c, s, this.mystack, (Integer)this.blockNumber+1);
		this.myThread.wait(this.myThread.getSelf()).setMeToNumber(p, c, s, this.mystack, (Integer)0);
		for(ThreadBlock block: this.blockControls) {
			block.addToStartOfAfterBlock(p, c, s, this);
		}
		this.mystack.clear();this.mystack.finish(c.job);
	}
	@Override
	public void addToEndOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//System.err.println("adding to block end from after statements " + s.getSubRes().toString());
		if(this.afterMe==null) {
			//you forgot to end the thread statement
			throw new CompileError("thread %s.%s ended without a 'next start / restart;' statement"
					.formatted(c.resourcelocation,this.myThread.getName()));
		}
		for(ThreadBlock block: this.afterMe.blockControls) {
			block.addToEndOfBeforeBlock(p, c, s, this);
		}
		//System.err.println("adding to block end from before statements " + s.getSubRes().toString());
		for(ThreadBlock block: this.blockControls) {
			block.addToEndOfAfterBlock(p, c, s, this);
		}
		this.mystack.clear();this.mystack.finish(c.job);
		String exitCmd = this.myThread.pathExit().toString();
		String exitif = this.myThread.exit().isTrue();
		p.printf("execute if %s run function %s\n", exitif,exitCmd);
		
		
	}
	@Override
	public void compileMe(PrintStream p, Compiler c, Scope s) throws CompileError {
		//System.err.printf("thread %s\n", this.myThread);
		c.namespace.fillMaxThreadBreaks(this.blockNumber);
		if(this.isLast) {
			for(ThreadBlock block: this.blockControls) {
				block.addToDeclaration(p, c, s, this);
			}
		}
	}

	@Override
	public String asString() {
		return "<thread ...>";
	}
	@Override
	public String getFlowType() {
		//deprecated
		return "thread";
	}
	private boolean loop=false;
	private int loopindex=-1;
	private void setLoop(boolean loop,int index) {
		this.loop=loop;
		this.loopindex=index;
		//TODO subscope may be null right now
		if(this.mySubscope!=null)this.mySubscope.setBreakable(loop);
	}
	@Override
	public boolean canBreak() {
		return loop;
	}
	@Override
	public boolean sendForward() {
		return !this.isLast;
	}

	ThreadStm startedBy;
	ThreadStm afterMe=null;
	public boolean hasAnonamousBlock = false;
	@Override
	public boolean setPredicessor(MultiFlow pred) throws CompileError {
		if(pred instanceof ThreadStm) {
			ThreadStm startedBy = ((ThreadStm) pred).startedBy ==null ? (ThreadStm) pred :((ThreadStm) pred).startedBy;
			/*
			System.err.printf("%s ... %s {} %s %s\n",startedBy.isFirst ? "thread" : "next",
				((ThreadStm) pred).isFirst ? "thread" : "next",
						this.isFirst ? "thread" : "next",
							this.isLast ? "stop" : ""
					);
			*/
			this.startedBy=startedBy;
			this.myThread = ((ThreadStm) pred).myThread;
			((ThreadStm) pred).afterMe = this;
			this.blockNumber = ((ThreadStm) pred).blockNumber+((ThreadStm) pred).size();
			this.addBlockNumberToThread(this.outerScope);
			return true;
		}
		else return false;
	}
	private int size() {
		//number of block # spacing
		return 1;
	}
	@Override
	public boolean claim() {
		return true;
	}
	@Override
	public boolean recive() {
		return !this.isFirst;
	}
	public static class Lines extends ThreadBlock {
		public static Lines make(Compiler c, Matcher matcher, int line, int col,ThreadStm stm, int index,boolean isPass1) throws CompileError{
			Lines me = new Lines(line,col,index,name);
			Num size =(Num) Num.tokenizeNextNumNonNull(c, c.currentScope, matcher, line, col);
			stm.myThread.ensureSize(stm, size.value.intValue());
			while(true) {
				Token t = c.nextNonNullMatch(Factories.checkForBasicName);
				if(!(t instanceof Token.BasicName)) break;
				String name = ((BasicName) t).name;
				Num num =(Num) Num.tokenizeNextNumNonNull(c, c.currentScope, matcher, line, col);
				int i=num.value.intValue();
				me.data.put(name, i);
				stm.myThread.addBlockName(name, Keyword.PUBLIC, i);
			}
			return me;
		}
		//for mch
		public static final String name = "lines";
		public final Map<String,Integer> data = new HashMap<String, Integer>();
		public Lines(int line, int col,int index, String name) {
			super(line, col,index, name);
		}
		
	}
	public static class Delay extends ThreadBlock {
		public static Delay make(Compiler c, Matcher matcher, int line, int col,ThreadStm stm,int index,boolean isPass1) throws CompileError{
			Delay me = new Delay(line,col,index,name);
			//System.err.printf("Delay::make(...)\n");
			BuiltinFunction.open(c);
			BasicArgs args=BuiltinFunction.tokenizeArgsEquations(c, matcher, line, col, stm.mystack);
			if(args.nargs()!=1) throw new CompileError("wrong number of args in wait(...) statement; expected 1 but got %d;"
					.formatted(args.nargs()));
			me.delay=(Equation) args.arg(0);
			me.isAfterLoop = stm.loop;
			return me;
		}
		//for mch
		public static final String name = "wait";
		private Equation delay;
		private boolean isAfterLoop = false;
		public Delay(int line, int col,int index, String name) {
			super(line, col,index, name);
		}
		@Override
		public void addToEndOfBeforeBlock(PrintStream p, Compiler c, Scope s, ThreadStm stm) throws CompileError {
			if(!stm.loop || stm.loopindex > this.index) this.add(p, c, s, stm,true);
		}
		
		public void add(PrintStream p, Compiler c, Scope s, ThreadStm stm, boolean before) throws CompileError {
			//System.err.println("wait compiling before = " + before + ", loop = " + this.isAfterLoop);
			Variable time = stm.myThread.waitIn();
			//p.printf("#enter Delay::add ; time = %s :: %s\n",time.getHolder(),time.getAddress());
			this.delay.compileOps(p, c, s, VarType.INT);
			//dont set the wait if we are AFTER a loop control but editing the block BEFORE
			if (!before) {
				if(stm.loopIf!=null)
					p.printf(stm.loopIf);
				this.delay.setVar(p, c, s, time);
			}else if (!isAfterLoop){
				if(stm.loopIf!=null)
					p.printf(stm.loopIf);//unreachable
				this.delay.setVar(p, c, s, time);
			}else {
				System.err.println("skipped delay");
			}
			//p.printf("#exit Delay::add ;\n");
		}
		@Override
		public void addToEndOfAfterBlock(PrintStream p, Compiler c, Scope s, ThreadStm stm) throws CompileError {
			if(stm.loop && stm.loopindex < this.index) this.add(p, c, s, stm,false);
		}
		
	}
	public static class Loop extends ThreadBlock {
		public static Loop makeWhile(Compiler c, Matcher matcher, int line, int col,ThreadStm stm,int index,boolean isPass1) throws CompileError{
			return Loop.make(c, matcher, line, col, stm,index, loopwhile,false, isPass1);
		}
		public static Loop makeUntil(Compiler c, Matcher matcher, int line, int col,ThreadStm stm,int index,boolean isPass1) throws CompileError{
			return Loop.make(c, matcher, line, col, stm, index, loopuntil,true, isPass1);
		}
		private static Loop make(Compiler c, Matcher matcher, int line, int col,ThreadStm stm, int index,String name,boolean inverted,boolean isPass1) throws CompileError{
			Loop me = new Loop(line,col,index,name,inverted,stm);
			BuiltinFunction.open(c);
			BasicArgs args=BuiltinFunction.tokenizeArgsEquations(c, matcher, line, col, stm.mystack);
			if(args.nargs()!=1) throw new CompileError("wrong number of args in wait(...) statement; expected 1 but got %d;"
					.formatted(args.nargs()));
			me.test=(Equation) args.arg(0);
			stm.setLoop(true,index);
			return me;
		}
		//for mch
		public static final String loopwhile = "while";
		public static final String loopuntil = "until";
		private Equation test;
		private final boolean inverted;
		private final ThreadStm loopBlock;
		public Loop(int line, int col,int index, String name,boolean inverted,ThreadStm loopStm) {
			super(line, col, index,name);
			this.inverted=inverted;
			this.loopBlock = loopStm;
		}
		@Override
		public void addToEndOfBeforeBlock(PrintStream p, Compiler c, Scope s, ThreadStm stm) throws CompileError {
			stm.mySubscope.setBreakable(true);
			Variable breakv = stm.mySubscope.makeAndgetBreakVarInMe(c);
			breakv.setMeToBoolean(p, c, s, this.test.stack, false);
			this.add(p, c, s, stm,true);
		}
		@Override
		public void addToStartOfAfterBlock(PrintStream p, Compiler c, Scope s, ThreadStm stm) throws CompileError {
			Variable breakv = stm.mySubscope.makeAndgetBreakVarInMe(c);
			breakv.setMeToBoolean(p, c, s, this.test.stack, false);
		}
		@Override
		public void addToEndOfAfterBlock(PrintStream p, Compiler c, Scope s, ThreadStm stm) throws CompileError {
			this.add(p, c, s, stm,false);
		}
		public void add(PrintStream p, Compiler c, Scope s, ThreadStm stm,boolean isBefore) throws CompileError {
			//System.err.println("loop compiling " );
			Variable block = stm.myThread.myGoto();
			Variable breakv = stm.mySubscope.makeAndgetBreakVarInMe(c);
			int myblock = this.loopBlock.blockNumber;
			int afterblock = this.loopBlock.blockNumber + 1;
			if(isBefore) {
				//skip looping if there was a goto
				String condition = String.format("execute if score %s matches %d run ",block.scorePhrase(),myblock);
				p.printf(condition);
			}
			if(isBefore)block.setMeToNumber(p, c, s, this.test.stack, afterblock);//halt
			this.test.compileOps(p, c, s, VarType.INT);
			int home = this.test.setReg(p, c, s, VarType.INT);
			String ifu= !this.inverted? "if" : "unless";
			Register flag = this.test.stack.getRegister(home);
			String precondition = "";
			String precondition2 = "";
			if(isBefore) {
				//skip looping if there was a goto
				precondition = String.format("if score %s matches %s ",block.scorePhrase(),afterblock);
				precondition2 = String.format("if score %s matches %s ",block.scorePhrase(),myblock);
			}
			String condition = String.format("execute %s%s %s unless %s run ",precondition, ifu,flag.testMeInCMD(),breakv.isTrue());
			String condition2 = String.format("execute %s%s %s unless %s run ",precondition2, ifu,flag.testMeInCMD(),breakv.isTrue());

			p.printf(condition);
			stm.loopIf = condition2;
			block.setMeToNumber(p, c, s, this.test.stack, myblock);//halt
			
			//share stack
			//TODO Fix loop and wait
			
		}
		
	}
	public static class End extends ThreadBlock {
		public static End makeStop(Compiler c, Matcher matcher, int line, int col,ThreadStm stm,int index,boolean isPass1) throws CompileError{
			return End.make(c, matcher, line, col, stm, index, endstop,false, isPass1);
		}
		public static End makeRestart(Compiler c, Matcher matcher, int line, int col,ThreadStm stm,int index,boolean isPass1) throws CompileError{
			return End.make(c, matcher, line, col, stm, index, endrestart,true, isPass1);
		}
		private static End make(Compiler c, Matcher matcher, int line, int col,ThreadStm stm,int index,String name,boolean restart,boolean isPass1) throws CompileError{
			End me = new End(line,col,index,name,restart);
			stm.hasBlock = false;
			stm.isLast = true;
			stm.hasAnonamousBlock  = true;
			
			boolean start = Keyword.checkFor(c, matcher, Keyword.START);
			if(start) {
				me.call = ThreadCall.make(c, matcher, line, col, Keyword.START,isPass1,false,stm);
			}
			return me;
		}
		//for mch
		public static final String endstop = "stop";
		public static final String endrestart = "restart";
		private Equation test;
		private final boolean restart;
		private ThreadCall call = null;
		public End(int line, int col,int index, String name,boolean restart) {
			super(line, col,index, name);
			this.restart=restart;
		}
		@Override
		public void addToStartOfAfterBlock(PrintStream p, Compiler c, Scope s, ThreadStm stm) throws CompileError {
			if(this.restart) {
				stm.myThread.restart(p, c, s, stm.mystack, stm.myThread.getSelf(), null);
			}else {
				stm.myThread.stop(p, c, s, stm.mystack, stm.myThread.getSelf());
			}
		}
		@Override
		public void addToDeclaration(PrintStream p, Compiler c, Scope s, ThreadStm stm) throws CompileError {
			if(this.call!=null) {
				this.call.compileMe(p, c, s);
			}
		}
		
	}

	@Override
	public boolean doHeader() {
		return this.isFirst && this.myThread !=null && this.myThread.doHdr();
	}
	@Override
	public void headerMe(PrintStream f) throws CompileError {
		this.myThread.inHdr(f);
		
	}
	@Override
	public boolean willMakeBlocks() throws CompileError {
		return this.isFirst ||
				(this.hasAnonamousBlock)
				|| this.blockAccess ==Keyword.PUBLIC;
	}
	@Override
	public void compileMyBlocks(Compiler c) throws CompileError {
		if(this.isFirst) {
			this.myThread.createSubsBlocks(c);
		}
		if(this.hasAnonamousBlock) {
			//make my block
			try {
				PrintStream p = this.mySubscope.open(c.job);
				this.addToStartOfMyBlock(p, c, mySubscope);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new CompileError("File not found for %s".formatted(this.mySubscope.getSubRes().toString()));
			}

			this.mySubscope.closeFiles();
		}
		if(this.blockAccess ==Keyword.PUBLIC) {
			// start at or restart at
			Scope s = new Scope(c.currentScope,this.myThread,McThread.ENTRYF.formatted(this.blockNumber),this.canBreak(),this.blockNumber);
			try {
				PrintStream p = s.open(c.job);
				this.myThread.truestart(p, c, s, mystack, this.myThread.truestartSelf(), blockNumber);
				for(ThreadBlock block: this.blockControls) {
					block.addToEndOfBeforeBlock(p, c, s, this);
				}
				this.mystack.clear();this.mystack.finish(c.job);
				String exitCmd = this.myThread.pathExit().toString();
				String exitif = this.myThread.exit().isTrue();
				p.printf("execute if %s run function %s\n", exitif,exitCmd);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new CompileError("File not found for %s".formatted(this.mySubscope.getSubRes().toString()));
			}

			s.closeFiles();
		}
	}
	public Scope getOuterScope() {
		return outerScope;
	}
}
