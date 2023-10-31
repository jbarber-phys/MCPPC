package net.mcppc.compiler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import net.mcpp.util.Strings;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.struct.NbtMap;
import net.mcppc.compiler.tokens.Declaration;
import net.mcppc.compiler.tokens.Execute;
import net.mcppc.compiler.tokens.Execute.Subexecute;
import net.mcppc.compiler.tokens.Execute.Subgetter;
import net.mcppc.compiler.tokens.ThreadStm.ThreadBlock;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Keyword;
import net.mcppc.compiler.tokens.MemberName;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.ThreadStm;
import net.mcppc.compiler.tokens.Token;

/**
 * similar to a function but executes over multiple ticks
 * 
 *     
 * 
 * @author RadiumE13
 *
 */
/*
 * 
 * scopes are created as follows:
 * McThread::createSubsBlocks : creates start, stop, restart functions (called by thread callers); use truestart format
 * ThreadStm::compileMyBlocks : if public, genrates entry_%d functions; use truestart format; sim to restart but checks for exit after
 * ThreadStm::addBlockNumberToThread sets this.mySubscope, which is used in:
 * * * anononamous blocks
 * * * loops and ThreadStm::setLoop
 * * * compiler gives this to code blocks, where it is used by actual thread code block_%n
 * * * (tokenize only) thread controls call to get a subscope with null block number
 * 
 *  TODO thread local vars:
 *  TODO give locals to control blocks; current bug: controls lack access thread while they are being created
 *  because the thread is not given untel setPredicessor(), which happens after token creation
 *  will need to alter compile2 to allow earlier setPredicessor() TODO test
 *  also need to propagate some scope arguments through builtin function;
 * 
 *  
 *  
 *  add option to enable uuid tables but leave it off by default for optimization
 *  lookup index at start of block, back copy after: time O(N_thread)
 *  add flag for completion, throw runtime error if it fails
 *  score is preffered, do this only if a local is non-stackable
 *  
 *  
 */
/**
 * A thread that runs mcfunctions over multiple ticks; can also run multiple threads at once (but only 1 on each entity)
 * @author RadiumE13
 *
 */
public class McThread {
	public static final String TAG_CURRENT = "mcpp+thread+current_executor";
	public static final Selector SELF = new Selector("@e", TAG_CURRENT,1);
	public static final Selector SELF_S = new Selector("@s", TAG_CURRENT,null);
	public static final String TEMPTAG = "mcpp+threadstart";
	public static final Selector SELFTEMP = new Selector("@e",TEMPTAG,null); //no limit; could have multiple
	public static final Selector SELF_START = Selector.AT_S;//no limit, do not change this, it doesnt actually fix it
	
	/**
	 * name of a scope inherited parameter for thread-selfness
	 */
	public static final String IS_THREADSELF = "is_threadself";
	//TODO enable this and test again
	public static final boolean DO_SELFIFY = true;
	
	
	public static final boolean ALLOW_UUID_MAPPING = true;
	private McThread() {}//construction is done post-init
	Keyword access = null;
	ResourceLocation path = null;
	String name = null;
	boolean isAnonamous = false;
	boolean isSynchronized;
	boolean madeExecs = false;
	//this is true if not all execute terms were resolvable at compile-1 time
	boolean isUnresolvedComp1 = false;
	//this is true if there are exec statements that have a lot of logic that is unwanted in a tick script
	boolean hasComplicatedExect = false;
	//if isUnresolvedComp1 | hasComplicatedExect, a run event will call a wrapped function with the execs inside it 
	List<Execute.Subexecute> execs;
	
	Map<String,Integer> namedBlocksPublic = new HashMap<String,Integer>();
	Map<String,Integer> namedBlocksPrivate = new HashMap<String,Integer>();
	
	ThreadStm firstControl=null;
	
	int numBlocks = 0;
	
	boolean killOnStop = false;
	
	//vars found in all blocks
	private Map<String,Variable> varsPublic = new HashMap<String,Variable>();
	//vars findable in only one block
	private Map<Integer,Map<String,Variable>> varsPrivate = new HashMap<Integer,Map<String,Variable>>();
	
	private Map<String,Const> constsPublic = new HashMap<String,Const>();
	//vars findable in only one block
	private Map<Integer,Map<String,Const>> constsPrivate = new HashMap<Integer,Map<String,Const>>();
	
	//this is all still TODO
	/**
	 * adds a thread-local define to this thread;<p>
	 *  public vars can be accessed in any block but private vars are lmited to this block;<p>
	 * will modify the vars mask to make it thread-safe according to the following rules: TODO <br>
	 * <ul>
	 *  <li>if thread is synchronized, or if it is not data equivalent, change nothing
	 *  <li> private ... = ...; normal behavior
	 *   <li>... volatile ...; normal behavior
	 *   <li>... `->` ...; normal behavior
	 *   <li>public ...; new behavior, score of executor / data in uuid table
	 *  </ul>
	 * 
	 * @param dec
	 * @param c
	 * @param s
	 * @param block
	 * @return
	 * @throws CompileError
	 */
	public boolean add(Declaration dec,Compiler c,Scope s,int block) throws CompileError {
		//System.err.printf("%s::%s thread var name: %s, blocknum = %s;\n",s.resBase,this.makeName(), dec.getVariable().name,block);
		//if(false) return c.myInterface.add(dec);
		switch(dec.getObjType()) {
		case VAR:break;//see below
		case CONST: {
			Const cv = dec.getConst();
			switch (dec.access) {
			case PUBLIC: {
				return this.constsPublic.putIfAbsent(cv.name, cv)==null;
			}
			case PRIVATE: {
				this.constsPrivate.putIfAbsent(block, new HashMap<String,Const>());
				Map<String,Const> blockLocals = this.constsPrivate.get(block);
				return blockLocals.putIfAbsent(cv.name, cv)==null;
			}
			default:throw new CompileError("access %s not allowed for thread const".formatted(dec.access));
			}
		}case FUNC: {
			throw new CompileError("functions not allowed in threads");//could change this later, TODO let local funcs access thread vars
		}
		}
		Variable var = dec.getVariable();
		//TODO stop truestart from using local vars incorrectly (do not allow this)
		boolean modify = (!this.isSynchronized);
		modify = modify && var.type.isDataEquivalent();
		modify = modify && !dec.hdrHasMask;
		modify = modify && !dec.isVolatile;
		modify = modify && !(dec.access==Keyword.PRIVATE && dec.hdrHasAssign);
		if(modify) {
			var.isThreadLocalNonFlowNonVolatile=true;//could also allow true-selfification during make start
			//System.err.printf("CHANGED: %s to mask this ::\n", var.name);
			if(var.type.isScoreEquivalent()) {
				ResourceLocation prefix = c.resourcelocation;
				String threadname = this.getName();
				String objective = "%s.%s.%s".formatted(Strings.getObjectiveSafeString(prefix.toString()),threadname,var.name);
				Selector e=this.getSelf();
				var.maskScore(e, objective).addSelfification(e.selfify()).makeScoreOfThreadRunner();
			} else {
				if(!ALLOW_UUID_MAPPING)throw new CompileError("could not apply thread-local score mask to %s of type %s; shared vars should me marked as 'volatile'"
						.formatted(var.name,var.type.asString()));
				else Warnings.warningf("a thread %s required a uuid lookup table; possible performance loss\n",this.name);
				
				//NOTE: alternatively could also use helmet nbt or something (if living)
				
				//ensure and then add to a lookup table
				//TODO test this when disables, then enable and test again (missile)
				this.makeLookup();
				int lookBlock = var.access == Keyword.PRIVATE ? block: -1;
				this.uuidLookups.editSubVar(var, lookBlock);//alters var
			}
		}else {
			//assume volatile or local var;
			//do nothing
			//System.err.printf("IGNORED: %s to stay the same ::\n", var.name);
		}
		
		switch (dec.access) {
		case PUBLIC: return this.varsPublic.putIfAbsent(var.name, var)==null;
		case PRIVATE: {
			this.varsPrivate.putIfAbsent(block, new HashMap<String,Variable>());
			Map<String,Variable> blockLocals = this.varsPrivate.get(block);
			return blockLocals.putIfAbsent(var.name, var)==null;
		}
		default:throw new CompileError("access %s not allowed for thread var".formatted(dec.access));
		}
	}

	public boolean hasVar(String name,Scope s) {
		int block = s.getThreadBlock();
		//System.err.printf("McThread::hasVar %s  in block %d\n", name, block);
		if(this.varsPublic.containsKey(name)) return true;
		this.varsPrivate.putIfAbsent(block, new HashMap<String,Variable>());
		Map<String,Variable> blockLocals = this.varsPrivate.get(block);
		if(blockLocals.containsKey(name))return true;
		return false;
	}
	public Variable getVar(String name,Scope s) {
		int block = s.getThreadBlock();
		if(this.varsPublic.containsKey(name)) return this.varsPublic.get(name);
		this.varsPrivate.putIfAbsent(block, new HashMap<String,Variable>());
		Map<String,Variable> blockLocals = this.varsPrivate.get(block);
		if(blockLocals.containsKey(name))return blockLocals.get(name);
		return null;
	}
	public Variable getVarInTruestart(String name,Scope s) {
		int block = s.getThreadBlock();
		if(this.varsPublic.containsKey(name)) return this.varsPublic.get(name);
		this.varsPrivate.putIfAbsent(block, new HashMap<String,Variable>());
		Map<String,Variable> blockLocals = this.varsPrivate.get(block);
		if(blockLocals.containsKey(name))return blockLocals.get(name);
		return null;
	}
	public void approveVar(Variable v,Scope s) throws CompileError{
		if(!v.isThreadLocalNonFlowNonVolatile) return;
		if(!s.hasThread()) return;
		if(this.isCompilingStart) {
			throw new CompileError("thread-local non-volatile var %s cannot be used on the first thread control as it has not been initialized");
		}
	}
	public boolean hasConst(String name, Scope s) {
		int block = s.getThreadBlock();
		if(this.constsPublic.containsKey(name)) return true;
		this.constsPrivate.putIfAbsent(block, new HashMap<String,Const>());
		Map<String,Const> blockLocals = this.constsPrivate.get(block);
		if(blockLocals.containsKey(name))return true;
		return false;
	}
	public Const getConst(String name, Scope s) {
		int block = s.getThreadBlock();
		if(this.constsPublic.containsKey(name)) return this.constsPublic.get(name);
		this.constsPrivate.putIfAbsent(block, new HashMap<String,Const>());
		Map<String,Const> blockLocals = this.constsPrivate.get(block);
		if(blockLocals.containsKey(name))return blockLocals.get(name);
		return null;
	}
	public void allocateMyLocalsLoad(PrintStream p) throws CompileError {
		if(this.hasLookup()) {
			this.uuidLookups.onLoad( p);
		}
		
		for(Map<String,Variable> subBlock: this.varsPrivate.values())for(Variable v: subBlock.values()) 
			if (v.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES)) v.allocateLoad(p, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
		for(Variable v: this.varsPublic.values()) 
			if (v.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES)) v.allocateLoad(p, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
	}
	
	
	
	public void ensureSize(ThreadStm stm, int index) {
		this.numBlocks = Math.max(this.numBlocks, index);
	}
	public void addBlockName(String name,Keyword access,int index) {
		switch(access) {
		case PUBLIC:
			this.namedBlocksPublic.put(name, index);
			break;
		case PRIVATE:
			this.namedBlocksPrivate.put(name, index);
			break;
		default: break;
			
		}
	}
	public Integer getBlockNumber(String name,boolean inside) {
		//System.err.printf("block name: %s, %s\n", name, inside);
		//System.err.printf("blocks: %s, %s\n", this.namedBlocksPublic,this.namedBlocksPrivate);
		if(inside) {
			Integer b = this.namedBlocksPrivate.get(name);
			if(b!=null)return b;
		}
		Integer b = this.namedBlocksPublic.get(name);
		return b;
	}
	public Integer checkForBlockNumberName(Compiler c,Scope s,Matcher m) throws CompileError {
		boolean inside = this == s.thread;
		int start = c.cursor;
		Token t= c.nextNonNullMatch(Factories.checkForMembName);
		if(!(t instanceof MemberName)) {
			return null;
			//c.cursor=start;
		}
		if (((MemberName) t).names.size()!=1) {
			//a var name
			c.cursor=start;
			return null;
		}
		String name = ((MemberName) t).names.get(0);
		Integer b = this.getBlockNumber(name, inside);
		if(b==null) {
			c.cursor=start;
		}
		return b;
	}
	public static String checkForBlockName(Compiler c,Scope s,Matcher m) throws CompileError {
		boolean inside = false;
		int start = c.cursor;
		Token t= c.nextNonNullMatch(Factories.checkForMembName);
		if(!(t instanceof MemberName)) {
			return null;
			//c.cursor=start;
		}
		if (((MemberName) t).names.size()!=1) {
			//a var name
			c.cursor=start;
			return null;
		}
		String name = ((MemberName) t).names.get(0);
		return name;
	}
	private void makeName(int line, int col) {
		//note this is OK for name but dont use line,col for lookup
		if(this.name==null) {
			this.name="thread__anon__%d__%d".formatted(line,col);
			this.isAnonamous=true;
		}
	}
	public void addToPath(StringBuffer buff,String suffix) {
		String nm = this.getName();
		buff.append(nm);
		buff.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		buff.append(suffix);
	}
	boolean hasPass2 = false;
	public static McThread populate(Compiler c, Matcher matcher, int line, int col,ThreadStm stm,boolean isPass1) throws CompileError {
		McThread self = new McThread();//if double-newed, will only recive name and access before death + replacement
		if(c.currentScope.parent!=null) throw new CompileError("threads must be defined in global scope");
		self.isSynchronized = Keyword.checkFor(c, matcher, Keyword.SYNCHRONIZED);
		self.path=c.resourcelocation;
		
		self.checkForName(c, matcher, line, col);
		if(self.name!=null) {
			if(!isPass1) {
				//substitute self for registered thing
				self = c.myInterface.identifyThread(self.name, c.currentScope);
			}else {
				c.myInterface.add(self);//by ref
			}
		} else {
			//anonamous thread
			
			if(!isPass1) {
				//substitute self for registered thing
				self = c.myInterface.identifyThreadAnonamous(c.currentScope, stm.startCursor);
			}else {
				c.myInterface.addAnonamous(self,stm.startCursor);//by ref
			}
			self.makeName(line,col);
		}
		//System.err.printf("thread populate ()%s;\n",isPass1);
		if(!isPass1) {
			//System.err.printf("setting first control %s;\n", stm);
			self.firstControl = stm;
		}
		self.populateMe(c, matcher, line, col, isPass1);
		if(!isPass1) {
			self.hasPass2=true;
			c.namespace.threads.add(self); // add to list for generating ticks
		}
		return self;
	}
	private void populateMe(Compiler c, Matcher matcher, int line, int col,boolean isPass1) throws CompileError {
		CompileError.AThingNotFoundYet e=null;
		if(!this.madeExecs)try {
			this.execs = new ArrayList<Subexecute>();
			this.checkForExecutes(c, matcher, line, col,this.execs);
			this.madeExecs=true;
		}catch (CompileError.AThingNotFoundYet e1) {
			e=e1;
			//wait till pass 2
			if(!isPass1)throw e;//the exception will maintain its original stacktrace
			else {
				this.isUnresolvedComp1=true;//try again later
				//throw new CompileError("could not find all vars/funcs for thread declaration %s on compile-1 pass;");
				Warnings.warningf("could not find all vars/funcs for thread declaration %s on compile-1 pass;".formatted(this.name)
						+ "will try again on pass compile-2;");
			}
		}else {
			//skip
			this.checkForExecutes(c, matcher, line, col,new ArrayList<Subexecute>());
		}
		if(!isPass1 && !this.madeExecs) throw e!=null ? e : new CompileError("never resolved executes for thread at line %s col %s".formatted(line,col));
		this.hasComplicatedExect=false;
		for(Subexecute sub:this.execs) {
			this.hasComplicatedExect=this.hasComplicatedExect || sub.isCompilated();
		}
	}
	private void checkForName(Compiler c, Matcher matcher, int line, int col) throws CompileError {
		this.access=Keyword.checkFor(c, matcher, Keyword.PUBLIC,Keyword.PRIVATE);
		if(this.access!=null) {
			Token name = c.nextNonNullMatch(Factories.checkForBasicName);
			if(!(name instanceof Token.BasicName)) throw new CompileError.UnexpectedToken(name, "name");
			this.name = ((Token.BasicName) name).name;
		}
		
	}
	private Selector executeAs = null;
	private boolean hasAsButNull = false;
	private List<Subexecute> checkForExecutes(Compiler c, Matcher matcher, int line, int col,List<Subexecute> execs) throws CompileError {
		while(true) {
			int cs = c.cursor;
			Token t = c.nextNonNullMatch(Factories.checkForBasicName);
			if(!(t instanceof Token.BasicName)) break;
			String name=((Token.BasicName) t).name;
			if(name.equals(Execute.As.NAME) || name.equals(Execute.Asat.NAME)) {
				this.hasAsButNull =true;
			}
			Subgetter g = Execute.SUBS.get(name);
			if (g==null) {
				c.cursor=cs;
				break;
			}
			Subexecute sub=g.make(c, matcher, line, col);
			if (this.executeAs ==null) {
				this.executeAs=sub.executeAs();
			}
			execs.add(sub);
		}
		return execs;
		
	}
	private String myTag = null;
	public String getTag() {
		if(myTag==null) {
			String s=this.path.toString()+"."+this.name; //ignore the index
			myTag = Strings.getTagSafeString(s);
			//myTag= Entity.TAGCHAR_NOTALLOWED.matcher(s).replaceAll("+");
		}return myTag;
	}
	//vars that are reserved words
	public static final String GOTO= Keyword.GOTO.name;//"goto";
	public static final String WAIT= Keyword.WAIT.name;//"wait";
	public static final String EXIT= Keyword.EXIT.name;//"exit";
	//public static final String GOTO= "goto";
	//public static final String WAIT= "wait";
	//public static final String EXIT= "exit";
	
	public static final String BREAK_F= "thread.break_%d";
	public static final String OBJ_GOTO= "mcpp.goto";
	public static final String OBJ_WAIT= "mcpp.wait";
	public static final String OBJ_EXIT= "mcpp.exit";
	public static final String OBJ_BREAK_F= "mcpp.thread.break_%d";
	public static String getObjBreak(int block) {return OBJ_BREAK_F.formatted(block);};
	public static String getBreak(int block) {return BREAK_F.formatted(block);};
	
	
	public Selector summonMe(PrintStream p) {
		p.printf("summon minecraft:marker 0 0 0 {Tags: [\"%s\"]}\n", TEMPTAG);
		return SELFTEMP.limited(1);
	}
	public Const getThis(Scope s) {
		//returns selector for self but be smart about @e -> @s conversion for efficiency
		Selector sf = getSelf(s);
		Selector.SelectorToken st = new Selector.SelectorToken(-1,-1,sf);
		Const c = new Const("$this",s.getSubRes(),ConstType.SELECTOR,Keyword.PRIVATE,st);
		return c;
		
	}
	public boolean hasSelf() {
		if(this.isSynchronized && this.executeAs==null) {
			return false;
		}else {
			return true;
		}
	}
	/**
	 * gets the selector for the executor in the given scope
	 * @return
	 */
	public Selector getSelf(Scope s) {
		Selector sf = getSelf();
		if(!McThread.DO_SELFIFY) return sf;
		if(sf==null) return null;
		Boolean selfify = (Boolean) s.getInheritedParameter(IS_THREADSELF);
		boolean sff = selfify==null? false: selfify;
		//the other function should do this at the Variable level instead
		if(sff) sf = sf.selfify();
		return sf;
	}
	/**
	 * gets the selector for the executor; does not selfify it
	 * should stay package access (friend Variable)
	 * @return
	 */
	Selector getSelf() {
		//friend Variable
		if(this.isSynchronized && this.executeAs==null) {
			return null;
		}else {
			if(this.isCompilingStart) return SELF_START;
			if(this.executeAs!=null && this.executeAs.isPlayer()) {
				return SELF.playerify();
			}
			return SELF;
		}
	}
	public Selector getAllExecutors() {
		//this stays no scope
		if(this.isSynchronized && this.executeAs==null) {
			return null;
		}else return new Selector("@e",this.getTag(),null);
	}
	public Variable myGoto() throws CompileError {
		return this.myGoto(this.getSelf());
	}
	public Variable waitIn() throws CompileError {
		return this.wait(this.getSelf());
	}
	public Variable myBreakVar(int block) throws CompileError {
		return this.myBreakVar(this.getSelf(),block);
	}
	public Variable exit() throws CompileError {
		return this.exit(this.getSelf());
	}
	public Variable myGoto(Selector e) throws CompileError {
		if(e==null) {
			return new Variable(GOTO,VarType.INT,null,Mask.SCORE,this.getStoragePath(),OBJ_GOTO);
		}
		return new Variable(GOTO,VarType.INT,null,Mask.SCORE,"","").maskEntityScore(e, OBJ_GOTO).addSelfification(e.selfify());
	}
	public Variable wait(Selector e) throws CompileError {
		if(e==null) {
			return new Variable(WAIT,VarType.INT,null,Mask.SCORE,this.getStoragePath(),OBJ_WAIT);
		}
		return waitStatic(e);
		//return new Variable(WAIT,VarType.INT,null,Mask.SCORE,"","").maskEntityScore(e, OBJ_WAIT).addSelfification(e.selfify());
	}
	public Variable myBreakVar(Selector e, int block) throws CompileError {
		if(e==null) {
			return new Variable(getBreak(block),VarType.BOOL,null,Mask.SCORE,this.getStoragePath(),getObjBreak(block));
		}
		return new Variable(getBreak(block),VarType.BOOL,null,Mask.SCORE,"","").maskEntityScore(e, getObjBreak(block)).addSelfification(e.selfify());
	}
	public static Variable waitStatic(Selector e) throws CompileError {
		//same as nonstatic for sub case that e!=null
		return new Variable(WAIT,VarType.INT,null,Mask.SCORE,"","").maskEntityScore(e, OBJ_WAIT).addSelfification(e.selfify());
	}
	public Variable exit(Selector e) throws CompileError {
		if(e==null) {
			return new Variable(EXIT,VarType.BOOL,null,Mask.SCORE,this.getStoragePath(),OBJ_EXIT);
		}
		return new Variable(EXIT,VarType.BOOL,null,Mask.SCORE,"","").maskEntityScore(e, OBJ_EXIT).addSelfification(e.selfify());
	}
	public ResourceLocation getBasePath() {
		return this.path;
	} 
	public String getStoragePath() {
		return this.path + "/" + this.name;
	}
	public Variable thisNbt(Variable v, NbtPath path) throws CompileError {
		Selector e = this.getSelf();
		return v.maskEntity(e, path).addSelfification(e.selfify());
	}
	public Variable thisNbtTruestart(Variable v, NbtPath path) throws CompileError {
		Selector e = this.truestartSelf();
		return v.maskEntity(e, path);
	}
	public Variable thisScore(Variable v, String objective) throws CompileError {
		Selector e = this.getSelf();
		return v.maskEntityScore(e, objective).addSelfification(e.selfify());
	}
	public  boolean isSelectorMySelf(Selector s) {
		return s.hasTag(TAG_CURRENT);
		
	}
	//public Variable self;
	public boolean doHdr() {
		return this.access == Keyword.PUBLIC;
	}
	public void inHdr(PrintStream p) {
		if(this.access!=Keyword.PUBLIC)return;
		String as = "";
		if(this.executeAs!=null) {
			as = "as (%s)".formatted(this.executeAs.toHDR());
		}
		List<String> lines = this.namedBlocksPublic.entrySet().stream().map(e -> "%s %s".formatted(e.getKey(),e.getValue())).toList();
		String[] lns = new String[lines.size()]; lines.toArray(lns);
		String g = "lines %s %s".formatted(this.numBlocks,String.join(" ", lns));
		p.printf("thread public %s %s %s;\n", this.name,as,g);
	}
	private Scope controllerScopeTokenize = null;
	public void cleanControllerScope() {
		this.controllerScopeTokenize = null;
	}
	public Scope getTokenizeScopeForControls(ThreadStm stm) throws CompileError {
		if(this.controllerScopeTokenize==null) {//skip if pass 1
			//TOOD nullpointer on firstControl; change where calls occur to get around this
			this.controllerScopeTokenize = new Scope(stm.getOuterScope(),this,"control",false,-1);
		}
		
		return this.controllerScopeTokenize;
		
	}
	
	public void truestart(PrintStream p,Compiler c,Scope s,RStack stack, Selector executor,int gotob,ThreadStm gotoStm) throws CompileError {
		this.truestart(p, c, s, stack, executor, ((Integer) gotob),gotoStm,true);
	}
	public void truestart(PrintStream p,Compiler c,Scope s,RStack stack, Selector executor) throws CompileError {
		this.truestart(p, c, s, stack, executor, ((Integer) null),this.firstControl,true);
	}
		
	private void truestart(PrintStream p,Compiler c,Scope s,RStack stack, Selector executor,Object gotoCache,ThreadStm gotoStm, boolean start) throws CompileError {
		//must be pass 2-version
		boolean tagme = !this.isSynchronized || this.executeAs!=null;
		if(tagme && start)executor.addTag(p, this.getTag());
		if(gotoCache==null) {
			this.myGoto(executor).setMeToNumber(p, c, s, stack, 1);
		}else if (gotoCache instanceof Integer){
			this.myGoto(executor).setMeToNumber(p, c, s, stack, ((Integer) gotoCache));
		}else if (gotoCache instanceof Register){
			//this is deprecated, never use this
			this.myGoto(executor).setMe(p, s, ((Register) gotoCache), VarType.INT);
		} else {
			throw new CompileError("MCPP::invalidargument McThread::truestart()");
		}
		this.wait(executor).setMeToNumber(p, c, s, stack, (Integer)0);
		this.exit(executor).setMeToBoolean(p, c, s, stack, false);
		isCompilingStart = true;
		
		if(!this.isSynchronized && this.hasLookup()) {
			this.uuidLookups.initMyVars(c, s, p, stack);
		}
		//ThreadStm gotoStm = this.firstControl;// if goto is null
		if(gotoCache==null)for(ThreadBlock block: gotoStm.blockControls) {
			//only do this if it is first
			block.addToEndOfBeforeBlock(p, c, s, gotoStm,true);
		}
		isCompilingStart = false;
		
	}
	public boolean isCompilingStart = false;
	public Selector truestartSelf() throws CompileError {
		return truestartSelf(false);//old default was true
	}
	public Selector truestartSelf(boolean entry) throws CompileError {
		//must be pass 2-version
		if(this.isSynchronized && this.executeAs==null) return null;
		return (this.executeAs==null || !entry)?
				 SELF_START
				:this.executeAs;
		
	}
	public void start(PrintStream p,Compiler c,Scope s,RStack stack, Integer block,Variable getMe) throws CompileError {
		if(this.executeAs==null && this.isUnresolvedComp1)
			throw new CompileError("could not resolve thread %s;".formatted(this.name));
		
		if( !this.isSynchronized || this.executeAs !=null) {
			Selector me;
			boolean doGetMe = (getMe!=null);
			boolean doSummon = (this.executeAs==null);
			if(doSummon) {
				me = this.summonMe(p);
			}else {
				me = this.executeAs;
				if(this.isSynchronized) {
					//remove all current executors
					Selector old = this.getAllExecutors();
					this.myGoto(old).setMeToNumber(p, c, s, stack, 0);
					this.wait(old).setMeToNumber(p, c, s, stack, (Integer)0);
					this.exit(old).setMeToBoolean(p, c, s, stack, false);
					old.removeTag(p, this.getTag());
					
					me = me.limited(1);
				}
				if(doGetMe) {
					me.addTag(p, TEMPTAG);
					me = SELFTEMP;
				}
			}
			me.runAt(p, this.pathStart(block));
			if(doGetMe) {
				if(getMe.isStruct() && getMe.type.struct instanceof Entity) {
					Entity clazz = (Entity) getMe.type.struct;
					String to = clazz.getScoreTag(getMe);
					me.addTag(p, to);
				}
			}
			if(doGetMe || doSummon) me.removeTag(p, TEMPTAG);
		}else {
			this.pathStart(block).run(p);
		}
		
	}
	//used for calls to a restart or a stop
	public void tagAndCall(PrintStream p,Compiler c,Scope s, Selector me,ResourceLocation func) throws CompileError {
		//if(me!=null )me.addTag(p,McThread.TEMPTAG);
		if(me!=null) {
			if(!this.hasSelf()) throw new CompileError("bad call to %s for non-self thread".formatted(func.toString()));
			me.runAt(p, func);
		}
		else func.run(p);
		//Selector out = this.truestartSelf();
		//if(me!=null )me.removeTag(p,McThread.TEMPTAG);
	}
	public void restart(PrintStream p,Compiler c,Scope s,RStack stack, Selector executor) throws CompileError {
		this.truestart(p, c, s, stack, executor,null,this.firstControl,false);
	}
	public void stop(PrintStream p,Compiler c,Scope s,RStack stack, Selector executor, boolean kill) throws CompileError {
		if(!this.isSynchronized && this.hasLookup() && s.hasThread() && s.getThread() == this) {
			this.uuidLookups.finalizeMe(c, s, p, stack);
		}
		if((!this.isSynchronized && this.executeAs==null )|| kill) {
			executor.kill(p);
		}else {
			this.myGoto(executor).setMeToNumber(p, c, s, stack, 0);
			this.wait(executor).setMeToNumber(p, c, s, stack, (Integer)0);
			if(executor!=null)executor.removeTag(p, this.getTag());
		}
	}
	private ResourceLocation subpath(String suff) {
		StringBuffer buff = new StringBuffer(this.path.path);
		buff.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		buff.append(this.getName());
		buff.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		buff.append(suff);
		return new ResourceLocation(this.path.namespace,buff.toString());
	}
	public ResourceLocation pathExit() {
		//now that entry exits are asat, exit = stop
		return this.pathStop();
	}
	public static final String BLOCKF = "block_%d";
	public static final String ENTRYF = "entry_%d";
	public static final String START = "start";
	public static final String STOP = "stop";
	public static final String RESTART = "restart";
	public static final String TICK = "tick";
	public static final String PULL = "pull_vars";
	public static final String PUSH = "push_vars";
	private ResourceLocation pathStart(Integer block) {
		return block ==null? this.subpath(START)
				: this.subpath(ENTRYF.formatted(block));
	}
	public ResourceLocation pathRestart() {
		Integer block = null; //not supported yet
		return block ==null? this.subpath(RESTART)
				: this.subpath(ENTRYF.formatted(block));
		//startwith takes selector & entry point arg
	}
	public ResourceLocation pathRestart(Integer block) {
		return block ==null? this.subpath(RESTART)
				: this.subpath(ENTRYF.formatted(block));
		//startwith takes selector & entry point arg
	}
	public ResourceLocation pathBlock(int block) {
		return this.subpath(BLOCKF.formatted(block));
		//startwith takes selector & entry point arg
	}
	public ResourceLocation pathStop() {
		return this.subpath(STOP);
	}
	public ResourceLocation pathTick() {
		return this.subpath(TICK);
	}
	
	public ResourceLocation pathLookupPull() {
		return this.subpath(PULL);
	}
	public ResourceLocation pathLookupPush() {
		return this.subpath(PUSH);
	}
	public void createSubsBlocks(Compiler c) throws CompileError {
		
		Scope start = new Scope(this.firstControl.getOuterScope(),this,START,false,this.firstControl.getBlockNumber());
		Selector self = this.truestartSelf();
		start.addInheritedParameter(IS_THREADSELF, false);
		//don't selfify unless starts are in execute as form
		try {
			PrintStream p = start.open(c.job);
			RStack stack = start.getStackFor();
			this.truestart(p, c, start,stack , self);
			stack.clear();stack.finish(c.job);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new CompileError("File not found for %s".formatted(start.getSubRes().toString()));
		}
		start.closeFiles();
		
		Scope stop = new Scope(this.firstControl.getOuterScope(),this,STOP,false,this.firstControl.getBlockNumber());
		stop.addInheritedParameter(IS_THREADSELF, false);
		try {
			PrintStream p = stop.open(c.job);
			RStack stack = stop.getStackFor();
			this.stop(p, c, stop, stack, self,this.killOnStop);
			stack.clear();stack.finish(c.job);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new CompileError("File not found for %s".formatted(stop.getSubRes().toString()));
		}
		stop.closeFiles();
		
		Scope restart = new Scope(this.firstControl.getOuterScope(),this,RESTART,false,this.firstControl.getBlockNumber());
		restart.addInheritedParameter(IS_THREADSELF, false);
		try {
			PrintStream p = restart.open(c.job);
			RStack stack = restart.getStackFor();
			this.restart(p, c, restart, stack, self);
			stack.clear();stack.finish(c.job);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new CompileError("File not found for %s".formatted(restart.getSubRes().toString()));
		}
		restart.closeFiles();
		Scope tick = new Scope(this.firstControl.getOuterScope(),this,TICK,false,this.firstControl.getBlockNumber());
		try {
			PrintStream p = tick.open(c.job);
			RStack stack = tick.getStackFor();
			this.tickManage(p, c, tick,stack);
			stack.clear();stack.finish(c.job);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new CompileError("File not found for %s".formatted(restart.getSubRes().toString()));
		}
		tick.closeFiles();
		
		if (!this.isSynchronized && this.hasLookup()){
			Scope pull = new Scope(this.firstControl.getOuterScope(),this,PULL,false,this.firstControl.getBlockNumber());
			pull.addInheritedParameter(IS_THREADSELF, true);
			try {
				PrintStream p = pull.open(c.job);
				RStack stack = pull.getStackFor();
				this.uuidLookups.retrieve(c, pull, p, stack);
				stack.clear();stack.finish(c.job);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new CompileError("File not found for %s".formatted(restart.getSubRes().toString()));
			}
			pull.closeFiles();
			Scope push = new Scope(this.firstControl.getOuterScope(),this,PUSH,false,this.firstControl.getBlockNumber());
			push.addInheritedParameter(IS_THREADSELF, true);
			try {
				PrintStream p = push.open(c.job);
				RStack stack = push.getStackFor();
				this.uuidLookups.reinsert(c, push, p, stack);
				stack.clear();stack.finish(c.job);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new CompileError("File not found for %s".formatted(restart.getSubRes().toString()));
			}
			push.closeFiles();
		}
	}
	//
	public void decrementDelaySync(PrintStream p) throws CompileError {
		Variable wait = this.wait(null);
		String score = wait.scorePhrase();
		p.printf("execute if score %s matches 1.. run scoreboard players remove %s 1\n",score,score);
	}
	public static void decrementDelay(PrintStream p) throws CompileError {
		Variable wait = McThread.waitStatic(Selector.AT_S);
		String score = wait.scorePhrase();
		p.printf("execute if score %s matches 1.. run scoreboard players remove %s 1\n",score,score);
	}
	public void executeTick(PrintStream p,CompileJob job,Namespace ns) {
		//clock is handled before this
		p.printf("execute as @s[tag = %s,scores = {%s = 0}] at @s run ",this.getTag(),McThread.OBJ_WAIT);
			this.pathTick().run(p);
	}
	public boolean isGlobal() {
		return this.isSynchronized && this.executeAs==null ;
	}
	public void tickManage(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError {
		//tag me so subscopes know how to flow properly
		boolean isGlobal = this.isGlobal();
		//String me = isGlobal? this.path.toString() : Selector.AT_S.toCMD();
		if(!isGlobal)p.printf("tag @s add %s\n", McThread.TAG_CURRENT);
		//skip delay, it was already done
		//p.printf("#this.numBlocks = %d;\n", this.numBlocks);
		Selector self = isGlobal? null : Selector.AT_S;
		Variable gotov = this.myGoto(self);
		Variable waitv = this.wait(self);
		
		for(int i=1;i<=this.numBlocks;i++) {
			p.printf("execute if score %s matches 0 if score %s matches %d run "
					,waitv.scorePhrase(),gotov.scorePhrase(),i);
				this.pathBlock(i).run(p);
		}
		
		if(!isGlobal)p.printf("tag @s remove %s\n", McThread.TAG_CURRENT);
	}
	public static void onLoad(PrintStream p, CompileJob job,Namespace ns) {
		p.printf("scoreboard objectives add %s dummy\n", OBJ_GOTO);
		p.printf("scoreboard objectives add %s dummy\n", OBJ_EXIT);
		p.printf("scoreboard objectives add %s dummy\n", OBJ_WAIT);
		for(int i=1;i<=ns.maxThreadBreaks;i++) {
			p.printf("scoreboard objectives add %s dummy\n", getObjBreak(i));
		}
	}
	public static boolean onTick(PrintStream p, CompileJob job,Namespace ns) throws CompileError {
		//System.out.printf("making %d threads in namespace %s;\n", ns.threads.size(),ns.name);
		if(ns.threads.isEmpty()) return false;
		boolean hasAsync = false;
		for(McThread self : ns.threads) if(self.isSynchronized && self.executeAs == null) {
			self.decrementDelaySync(p);
			Variable block = self.myGoto((Selector)null);
			Variable delay = self.wait((Selector)null);
			ResourceLocation res = self.pathTick();
			p.printf("execute if score %s matches 1.. if score %s matches 0 run "
					, block.scorePhrase(),delay.scorePhrase());
				res.run(p);
		}else {
			hasAsync = true;
		}
		if(hasAsync) {
			ns.addEntityTick();
			//TODO consider command number overflow guard:
			//remove @e[tag=!] the thread-executor tag to prevent overflow bugs
			//but this would cost 1 @e per tick
			
			p.printf("execute as @e[tag=!,scores = {%s = 1..}] at @s run ", McThread.OBJ_GOTO);
				ns.getEntityTickFunction().run(p);
		}
		return true;
	}
	public static void onEntityTick(PrintStream p, CompileJob job,Namespace ns) throws CompileError {
		McThread.decrementDelay(p);
		for(McThread self : ns.threads) if(!(self.isSynchronized && self.executeAs == null)) {
			self.executeTick(p, job, ns);
		}
	}
	
	private NbtMap.ThreadUuidLookupTable uuidLookups = null;
	public boolean hasLookup() {return this.uuidLookups!=null && !this.isSynchronized;}
	private void makeLookup() throws CompileError {
		if(this.uuidLookups==null) this.uuidLookups = new NbtMap.ThreadUuidLookupTable(this);
	}
	public void finalizeLookup(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError {
		this.uuidLookups.finalizeMe(c,s,p,stack);
	}
	
	public String getName() {
		return name;
	}
	public void setToKill() {
		this.killOnStop=true;
	}
}
