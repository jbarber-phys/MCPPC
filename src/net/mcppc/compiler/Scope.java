package net.mcppc.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.mcppc.compiler.errors.COption;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Statement;
import net.mcppc.compiler.tokens.TemplateArgsToken;
import net.mcppc.compiler.tokens.TemplateDefToken;

/**
 * tells the code inside what stack to use; for flow statements, where to store flags, 
 * also holds var estmates built up sequentially
 * 
 * TODO consider a const ifelse statement
 * @author RadiumE13
 *
 */
public class Scope {
	final ResourceLocation resBase;
	Function function=null;
	McThread thread = null;
	String threadsuffix = null;
	Scope parent=null;
	final Map<Variable,Number> varEstimates=new HashMap<Variable,Number>();
	
	boolean hasExtraSuffix=false;
	final int myFlowNumber;
	String flowType;
	
	//count flows for sub-blocks to use
	private int flowCounter;
	
	private boolean isBreakable; public void setBreakable(boolean b) {this.isBreakable=b;}
	private final boolean isDoneable;
	//file IO for compiling mcfs only
	final List<Scope> children = new ArrayList<Scope>();
	public PrintStream out=null;
	boolean isOpen=false;
	
	//===COMPILER SCOPE PARAMETERS
	//parameters set by the compiler about this scope that are inherited by subs unless they override them
	//example: thread-selfness is overridden by execute as blocks (and etc
	//note that these are not OPTIONAL as they prevent the compiler from breaking
	private Map<String,Object> inheritedParams = new HashMap<String,Object>();
	public Object getInheritedParameter(String key) {
		Object o=this.inheritedParams.get(key);
		if(o!=null) return o;
		if(this.parent!=null) return this.parent.getInheritedParameter(key);
		return null;
	}
	public boolean addInheritedParameter(String key,Object o) {
		return this.inheritedParams.put(key, o)==null;
	}
	//====RUNTIME SCOPE PARAMETERS
	//can be set in the middle of a block
	private Map<COption,TreeMap<Integer,Object>> optionsByCursor = new HashMap<COption,TreeMap<Integer,Object>>();
	
	@SuppressWarnings("unchecked")
	public <V> V getOption(COption<V> option, CompileJob job,int cursor) {
		
		if(!option.doesCompilerGetPriority(job)) {
			TreeMap<Integer,Object> tree = this.optionsByCursor.get(option);
			if(tree!=null) {
				V o =(V) tree.floorEntry(cursor);
				if(o!=null)return o;
			}
			if(this.parent!=null) {
				return this.parent.getOption(option, job, cursor);
			}
		}
		
		return job.getOption(option);
	}
	@SuppressWarnings("unchecked")
	public <V> V setOption(COption<V> option, V value,int cursor) {
		this.optionsByCursor.putIfAbsent(option, new TreeMap<Integer,Object>());
		TreeMap<Integer,Object> tree = this.optionsByCursor.get(option);
		return (V)tree.put(cursor, value);
	}
	//sequence flag applied after command for this scope only but no others (not even subs)
	//maybe put these in some sort of registry in the future, but be carefull to make it so that the sequence is preserved
	private boolean prohibitLongMult = false;
	private boolean debugMode=false;
	
	//==============
	private final Map<String,Variable> loopLocals = new HashMap<String,Variable>();
	public boolean hasLoopLocal(String name) {
		if(this.parent !=null && this.parent.hasLoopLocal(name)) return true;
		return loopLocals.containsKey(name);
	}
	public Variable getLoopLocal(String name) {
		Variable v= loopLocals.get(name);
		if(v!=null) return v;
		if(this.parent !=null && this.parent.hasLoopLocal(name)) return this.parent.getLoopLocal(name);
		return v;
	}
	public Variable addLoopLocal(String name,VarType type,Compiler c) throws CompileError {
		boolean add = true;//? do not call this in pass 1
		Variable var=new Variable(name,type,null,this.getSubResNoTemplate());
		if(this.isInFunctionDefine() && this.function.canRecurr) {
			this.function.withLocalFlowVar(this.myBreakVar, c, add);
		}
		loopLocals.put(var.name,var);
		return var;
	}
	public Variable addLoopLocalRef(Variable var) throws CompileError {
		boolean add = true;//? do not call this in pass 1
		if(this.isInFunctionDefine() && this.function.canRecurr) {
			//this.function.withLocalFlowVar(this.myBreakVar, c, add);
			//function doesn't need to know
		}
		loopLocals.put(var.name,var);
		return var;
	}
	public boolean isOpen() {
		
		return this.out!=null && this.isOpen;
	}
	public RStack getStackFor() {
		return new RStack(this);
	}
	public PrintStream open(CompileJob cj) throws FileNotFoundException {
		this.isOpen=true;
		setProhibitLongMult(false);
		File f=cj.pathForMcf(this.getSubRes(this.resBase)).toFile();
		try {
			f.getParentFile().mkdirs();
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			isOpen=false;
			throw new FileNotFoundException("actually an IOException");
		}
		this.out=new PrintStreamLineCounting(f);
		return this.out;
	}
	public void closeFiles() {
		if(this.out!=null) {
			out.close();
			if(this.out instanceof PrintStreamLineCounting) ((PrintStreamLineCounting) this.out).announceLines(this.getSubRes().toString());
		}
		this.isOpen=false;
		for(Scope k:this.children)k.closeFiles();
	}
	public void closeJustMyFiles() {
		if(this.out!=null)out.close();
		this.isOpen=false;
	}

	private int makeNextFlowNumber() {
		return this.flowCounter++;
	}

	public ResourceLocation getSubRes() {
		return getSubRes(this.resBase);
	}
	public ResourceLocation getSubResNoTemplate() {
		return getSubRes(this.resBase,true);
	}
	public static ResourceLocation getSubRes(ResourceLocation base,Function f) {
		StringBuffer path=new StringBuffer();
		path.append(base.path);
		path.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		path.append(f.name.toLowerCase());//warning: function name case may cause problems
		return new ResourceLocation(base.namespace,path.toString());
	}
	@Deprecated
	public static void appendSubRes(StringBuffer buff,Function f) {
		buff.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		buff.append(f.name.toLowerCase());//warning: function name case may cause problems
	}
	private static void appendSubResSubFunc(StringBuffer buff,Function f, TemplateArgsToken template ) {
		buff.append(f.name.toLowerCase());//warning: function name case may cause problems
		//System.err.println(path);
		if(template!=null) {
			//System.err.printf("templateBound: %s\n",buff.toString());
			TemplateArgsToken tpargs;
			try {
				if(f.template==null) throw new CompileError("catch me");
				tpargs = template;//.defaultArgs();
				Scope.appendTemplate(buff, tpargs);
			} catch (CompileError e) {
				e.printStackTrace();
				buff.append("_null_");
			}
		}
		//System.err.println(path);
	}

	public static void appendTemplate(StringBuffer buff,TemplateArgsToken template) {
		if(template==null)return;
		buff.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		buff.append(template.inresPath());
	}
	
	public static ResourceLocation getSubRes(ResourceLocation base,String fname) {
		StringBuffer path=new StringBuffer();
		path.append(base.path);
		path.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		path.append(fname);
		return new ResourceLocation(base.namespace,path.toString());
	}
	public ResourceLocation getSubRes(ResourceLocation res) {
		return this.getSubRes(res, false);
	}
	public ResourceLocation getSubRes(ResourceLocation res,boolean stripTemplate) {
		if(this.parent==null)return res;
		StringBuffer path=new StringBuffer();
		path.append(res.path);
		path.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		//System.err.println(path);
		if(function!=null) {
			TemplateDefToken bound = this.templateBoundToMeOrParrent();
			
			try { 
				TemplateArgsToken targs=(bound==null || stripTemplate)?null:bound.defaultArgs();
				Scope.appendSubResSubFunc(path, function,targs);
			} catch (CompileError e) {
				e.printStackTrace();
				path.append("_null_");
			}
			
			
		}
		if(thread!=null) {
			this.thread.addToPath(path, this.threadsuffix);
		}
		int index=path.length();
		boolean f=this.appendExSuff(path);
		if(f && function!=null)path.insert(index, "___");
		//System.err.println(path);
		return new ResourceLocation(res.namespace,path.toString().toLowerCase());
	}
	private TemplateDefToken templateBoundToMeOrParrent() {
		if(this.templateBound!=null)return this.templateBound;
		if(this.parent==null)return null;
		return this.parent.templateBoundToMeOrParrent();
	}
	public void appendFunction(StringBuffer path) {
		path.append(this.function.name);
		if(this.templateBound!=null) {
			path.append("_");
			path.append(this.templateBound);
		}
	}
	public boolean appendExSuff(StringBuffer buff) {
		boolean b=false;
		if(this.parent!=null)b=this.parent.appendExSuff(buff);
		if (this.hasExtraSuffix) {
			buff.append("flowblock_%s__%s_".formatted(this.myFlowNumber,this.flowType));
			b=true;
		}return b;
		
	}
	public Scope superscope() {
		return this.parent;
	}
	
	public boolean isInFunctionDefine() {
		return this.function!=null;
	}public Function getFunction() {
		return this.function;
	}
	
	public void addEstimate(Variable var,Number est) {
		varEstimates.put(var, est);
	}
	public Number getEstimate(Variable var) {
		if(this.varEstimates.containsKey(var)) return this.varEstimates.get(var);
		else if (this.parent!=null)return this.parent.getEstimate(var);//ok as long as tokenization is monatomic (it is)
		else return null;
	}
	public Scope(Compiler c) {
		//scope of compiler root;
		this.myFlowNumber=0;
		this.resBase=c.resourcelocation;
		this.isBreakable=false;
		this.isDoneable = false;
	}
	private Scope(Scope s,Function f) {
		this.parent=s;
		this.function=f;
		this.template=f.template;
		this.myFlowNumber=0;
		this.resBase=s.resBase;
		s.children.add(this);
		this.isBreakable=false;
		this.isDoneable = false;
		this.isFuncBase=true;
	}
	private TemplateDefToken template=null;//can be def or args
	private TemplateDefToken templateBound=null;//can be def or args
	private boolean isBound=false;//true if this scope has assigned values for all 
	private boolean isFuncBase=false;
	private Scope(Scope scope, TemplateDefToken templateDef) {
		this.parent=scope.parent;
		this.function=null;
		this.myFlowNumber=scope.myFlowNumber;
		this.resBase=scope.resBase;
		template=templateDef;
		templateBound=template;
		this.isBreakable=false;
		this.isDoneable = false;
	}
	@Deprecated
	private Scope(Scope scope, TemplateArgsToken tempargs) throws CompileError {
		this.parent=scope.parent;
		this.function=scope.function;
		this.myFlowNumber=scope.myFlowNumber;
		this.resBase=scope.resBase;
		template=scope.template.bind(tempargs) ;
		templateBound=template;
		this.isBreakable=false;
		this.isDoneable = false;
	}
	private Scope(Scope s,Statement.Flow flow) {
		this.parent=s;
		this.function=s.function;
		this.thread = s.thread;
		this.threadsuffix = s.threadsuffix;
		this.myFlowNumber=s.makeNextFlowNumber();
		this.resBase=s.resBase;
		this.flowType=flow.getFlowType();
		this.hasExtraSuffix=true;
		this.isBreakable=flow.canBreak();
		if(flow instanceof Statement.MultiFlow) {
			//then it has a done parameter
			this.isDoneable = true;
		}else this.isDoneable = false;
		s.children.add(this);
	}
	private boolean isDirectThreadblock = false;
	private int threadBlockNumber=-1;
	public Scope(Scope s,McThread thread, int block,boolean canBreak) throws CompileError {
		this(s,thread,McThread.BLOCKF.formatted(block),canBreak,block);
		
	}
	public Scope(Scope s,McThread thread, String suffix,boolean canBreak, int block) throws CompileError {
		//TODO add boolean for isStart
		this.parent=s;
		this.function=null;
		this.template=null;
		this.thread = thread;
		this.threadsuffix = suffix;
		this.resBase=s.resBase;
		s.children.add(this);
		this.isBreakable=canBreak;
		this.myFlowNumber = 0;
		this.threadBlockNumber=block;
		this.isDoneable = false;
		this.isFuncBase=false;
		this.isDirectThreadblock = true;
		this.addLoopLocalRef(thread.myGoto());
		this.addLoopLocalRef(thread.waitIn());
		this.addLoopLocalRef(thread.exit());
		if(thread.hasSelf()) {
			this.addInheritedParameter(McThread.IS_THREADSELF, (Boolean)true);
		}
		if(canBreak) {
			//do it later
		}
		
	}
	public Scope subscope(Function f) throws CompileError{
		if(this.parent!=null) throw new CompileError("functions inside flow / functions not (yet) supproted");
		if(this.isInFunctionDefine()) throw new CompileError("functions inside functions not yet supproted");
		return new Scope(this,f);
	}
	public Scope subscope(Compiler c,Statement.Flow flow, boolean isPass1) throws CompileError{
		Scope s =new Scope(this,flow);
		//TODO edit
		//System.err.printf("made flow subscope %s; break: %s, done: %s;\n",flow.getFlowType(),s.isBreakable,s.isDoneable);
		if(s.isBreakable) {
			//TODO this wont work yet; set later in setPredicessor
			s.initializeBreakVarInMe(c,isPass1);
		}
		if(s.isDoneable) {
			if (((Statement.MultiFlow)flow).makeDoneVar())
				s.initializeIfelseDoneVarExMe(c,isPass1);
		}
		//collect lo
		return s;
	}
	
	private Variable flowVarDone = null;
	public Variable initializeIfelseDoneVarExMe(Compiler c,boolean add) throws CompileError {
		if(this.flowVarDone==null) {
			//only initialize once per scope
			this.flowVarDone=new Variable("\"$ifelse_done\"",VarType.BOOL,null,this.getSubResNoTemplate());
			if(this.isInFunctionDefine() && this.function.canRecurr) {
				this.function.withLocalFlowVar(this.flowVarDone, c,add);
			}
		}
		//System.err.printf("donevar: %s.%s\n",this.flowVarDone.holder,this.flowVarDone.getAddressToPrepend());
		return this.flowVarDone;
	}
	public Variable getIfelseDoneVarExMe(Compiler c) throws CompileError {
		//external to the if statement
		if(this.flowVarDone==null) {
			throw new CompileError("tried to initialize ifelse var too late");
		}
		return this.flowVarDone;
	}
	public boolean hasBreak() {
		return hasBreak(0);
	}
	public boolean hasBreak(int depth) {
		if(this.isBreakable) {
			if(depth==0)return true;
			if(this.parent==null)return false;
			return this.parent.hasBreak(depth-1);
		}
		if(this.parent==null)return false;
		return this.parent.hasBreak(depth);
	}
	public Variable getBreakVarInMe(Compiler c) throws CompileError {
		return getBreakVarInMe(c, 0);
	}
	//this is only used by thread loop
	@Deprecated private Variable makeAndgetBreakVarInMe(Compiler c) throws CompileError {
		if(this.isBreakable && this.myBreakVar==null) this.initializeBreakVarInMe(c, false);
		return getBreakVarInMe(c, 0);
	}
	private Variable myBreakVar = null;
	public Variable initializeBreakVarInMe(Compiler c,boolean add) throws CompileError {
		//add = isPass1: (is hdr)
		if(this.isBreakable) {
			if(this.myBreakVar!=null) return this.myBreakVar;
			if (this.isDirectThreadblock) {
				//this.myBreakVar = this.thread.myBreak();//this link breaks the break statements in threads
				this.myBreakVar = this.thread.myBreakVar(this.threadBlockNumber);//TODO test this
				//but this line will break if synchronized
				//this.myBreakVar=new Variable("\"$break\"",VarType.BOOL,null,this.getSubResNoTemplate());
			}
			else  {
				this.myBreakVar=new Variable("\"$break\"",VarType.BOOL,null,this.getSubResNoTemplate());
				if(this.isInFunctionDefine() && this.function.canRecurr) {
					this.function.withLocalFlowVar(this.myBreakVar, c, add);
				}
			}
			
			return this.myBreakVar;
		}return null;
	}
	public Variable reInitialzeBreakVarInMe(Statement.Flow f,Variable v) throws CompileError {
		return this.myBreakVar = v;
	}
	private Variable getBreakVarInMe(Compiler c, int depth) throws CompileError {
		return this.getBreakVarInMe(c, depth, this);
	}
	public Variable getBreakVarInMe(Compiler c, int depth,Scope s) throws CompileError {
		//TODO this is called externally, if so, scope is this
		//add scope arg so var can selfify
		//note: break ignores template
		if(this.isBreakable) {
			//internal scope to the loop
			if(depth==0) {
				if(this.isBreakable && this.myBreakVar==null) this.initializeBreakVarInMe(c, false);
				if(this.myBreakVar==null) {
					//TODO exception here
					throw new CompileError("flowvar break initialized too late at line (%d) scope %s"
					.formatted(c.line(),this.getSubResNoTemplate().toString()));
				}
				
				return this.myBreakVar.attemptSelfify(s);
			}
			if(this.parent==null)return null;
			return this.parent.getBreakVarInMe(c,depth-1);
		}
		if(this.parent==null)return null;
		return this.parent.getBreakVarInMe(c,depth);
	}
	public Scope defTemplateScope(TemplateDefToken templateDef) throws CompileError {
		if(this.parent!=null) throw new CompileError("template defs inside flow / functions not (yet) supproted");
		if(this.isInFunctionDefine()) throw new CompileError("templates inside functions not yet supproted");
		return new Scope(this,templateDef);
	}
	public Const checkForTemplateOrLocalConst(String name) {
		if(this.function!=null)for(Const c:this.function.localConsts)if(c.name.equals(name))return c;
		//if(this.templateBound==null && this.temporaryTemplate==null)return null;
		if(this.temporaryTemplate!=null)for(Const c:this.temporaryTemplate.params)if(c.name.equals(name))return c;
		if(this.templateBound!=null)for(Const c:this.templateBound.params)if(c.name.equals(name))return c;
		if(this.parent==null)return null;
		return this.parent.checkForTemplateOrLocalConst(name);
	}
	public void bindTemplateToMe (TemplateArgsToken args) throws CompileError {
		this.templateBound=this.template.bind(args);
	}
	private TemplateDefToken temporaryTemplate = null;
	public void addTemplateConstsTemporarily (Function f, TemplateArgsToken args) throws CompileError {
		//System.err.printf("f.template = %s;\n args = %s;", f.template.inHDR(),args.values);
		this.temporaryTemplate=f.template.bind(args);
	}
	public void removeTemporaryConsts () throws CompileError {
		this.temporaryTemplate = null;
	}
	
	public boolean hasTemplate() {return this.template!=null;}

	public List<TemplateArgsToken> getAllDefaultTemplateArgs() throws CompileError {
		return getAllDefaultTemplateArgs(true);
	}
	public List<TemplateArgsToken> getNewTemplateArgs() throws CompileError {
		return getAllDefaultTemplateArgs(false);
	}
	public List<TemplateArgsToken> getAllDefaultTemplateArgs(boolean isFirst) throws CompileError {
		List<TemplateArgsToken> all = new ArrayList<TemplateArgsToken>();
		if(isFirst)if(this.template!=null) {
			all.addAll(this.template.getAllDefaultArgs());
			//if(this.function!=null)this.function.fillDefaults();
		}
		if(this.function!=null)all.addAll(this.function.popRequestedTemplateBinds());
		return all;
	}
	//does not affect any subscopes
	public boolean isProhibitLongMult() {
		//boolean up=this.parent!=null ? this.parent.isProhibitLongMult()//parrent scope may not exist anymore
		return this.prohibitLongMult;
	}
	public void setProhibitLongMult(boolean prohibitLongMult) {
		this.prohibitLongMult = prohibitLongMult;
	}
	public void setDebugMode(boolean b) {
		this.debugMode = b;
	} public boolean isDebugMode() {return this.debugMode;}
	public boolean hasThread() {
		return this.thread!=null;
		
	}
	public McThread getThread() {
		return this.thread;
		
	}
	public int getThreadBlock(){
		if(this.threadBlockNumber == -1 && this.parent!=null) return this.parent.getThreadBlock();
		return this.threadBlockNumber;
	}
}
