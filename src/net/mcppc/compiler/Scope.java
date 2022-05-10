package net.mcppc.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * @author jbarb_t8a3esk
 *
 */
public class Scope {
	final ResourceLocation resBase;
	Function function=null;
	Scope parent=null;
	final Map<Variable,Number> varEstimates=new HashMap<Variable,Number>();
	
	boolean hasExtraSuffix=false;
	final int myFlowNumber;
	String flowType;
	
	//count flows for sub-blocks to use
	private int flowCounter;
	
	private final boolean isBreakable;
	//file IO for compiling mcfs only
	final List<Scope> children = new ArrayList<Scope>();
	public PrintStream out=null;
	boolean isOpen=false;
	private boolean prohibitLongMult = false;
	public boolean isOpen() {
		
		return this.out!=null && this.isOpen;
	}
	public Register.RStack getStackFor() {
		return new Register.RStack(this);
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
		if(this.parent==null)return res;
		StringBuffer path=new StringBuffer();
		path.append(res.path);
		path.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		//System.err.println(path);
		if(function!=null) {
			TemplateDefToken bound = this.templateBoundToMeOrParrent();
			try { Scope.appendSubResSubFunc(path, function,bound==null?null:bound.defaultArgs());}
			catch (CompileError e) {
				e.printStackTrace();
				path.append("_null_");
			}
			
			
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
	}
	private Scope(Scope s,Function f) {
		this.parent=s;
		this.function=f;
		this.template=f.template;
		this.myFlowNumber=0;
		this.resBase=s.resBase;
		s.children.add(this);
		this.isBreakable=false;
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
	}
	private Scope(Scope s,Statement.Flow flow) {
		this.parent=s;
		this.function=s.function;
		this.myFlowNumber=s.makeNextFlowNumber();
		this.resBase=s.resBase;
		this.flowType=flow.getFlowType();
		this.hasExtraSuffix=true;
		this.isBreakable=flow.canBreak();
		s.children.add(this);
	}
	public Scope subscope(Function f) throws CompileError{
		if(this.parent!=null) throw new CompileError("functions inside flow / functions not (yet) supproted");
		if(this.isInFunctionDefine()) throw new CompileError("functions inside functions not yet supproted");
		return new Scope(this,f);
	}
	public Scope subscope(Statement.Flow flow) throws CompileError{
		return new Scope(this,flow);
	}
	
	public Variable getIfelseDoneVarExMe() {
		//external to the if statement
		final Variable done=new Variable("\"$ifelse_done\"",VarType.BOOL,null,this.getSubRes());
		return done;
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
	public Variable getBreakVarInMe() {
		return getBreakVarInMe(0);
	}
	public Variable getBreakVarInMe(int depth) {
		if(this.isBreakable) {
			//internal scope to the loop
			if(depth==0) {
				final Variable bk=new Variable("\"$break\"",VarType.BOOL,null,this.getSubRes());
				return bk;
			}
			if(this.parent==null)return null;
			return this.parent.getBreakVarInMe(depth-1);
		}
		if(this.parent==null)return null;
		return this.parent.getBreakVarInMe(depth);
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
}
