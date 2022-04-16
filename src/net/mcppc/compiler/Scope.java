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
import net.mcppc.compiler.tokens.Statement;

/**
 * tells the code inside what stack to use; for flow statements, where to store flags, 
 * also holds var estmates built up sequentially
 * @author jbarb_t8a3esk
 *
 */
public class Scope {
	final ResourceLocation resBase;
	Function function=null;
	Scope parent=null;
	final Map<Variable,Number> varEstimates=new HashMap<Variable,Number>();
	//TODO flow file name information
	boolean hasExtraSuffix=false;
	final int myFlowNumber;
	String flowType;
	
	//count flows for sub-blocks to use
	private int flowCounter;
	
	
	//file IO for compiling mcfs only
	final List<Scope> children = new ArrayList<Scope>();
	public PrintStream out=null;
	boolean isOpen=false;
	
	public boolean isOpen() {
		
		return this.out!=null && this.isOpen;
	}
	public Register.RStack getStackFor() {
		return new Register.RStack(this);
	}
	public PrintStream open(CompileJob cj) throws FileNotFoundException {
		this.isOpen=true;
		File f=cj.pathForMcf(this.getSubRes(this.resBase)).toFile();
		try {
			f.getParentFile().mkdirs();
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			isOpen=false;
			throw new FileNotFoundException("actually an IOException");
		}
		this.out=new PrintStream(f);
		return this.out;
	}
	public void closeFiles() {
		if(this.out!=null)out.close();
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
		path.append(f.name);
		return new ResourceLocation(base.path,path.toString());
	}
	public static ResourceLocation getSubRes(ResourceLocation base,String fname) {
		StringBuffer path=new StringBuffer();
		path.append(base.path);
		path.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		path.append(fname);
		return new ResourceLocation(base.path,path.toString());
	}
	public ResourceLocation getSubRes(ResourceLocation res) {
		if(this.parent==null)return res;
		StringBuffer path=new StringBuffer();
		path.append(res.path);
		path.append(CompileJob.FILE_TO_SUBDIR_SUFFIX);
		
		if(function!=null) {
			path.append(this.function.name);
			
		}
		int index=path.length();
		boolean f=this.appendExSuff(path);
		if(f && function!=null)path.insert(index, "___");
		
		return new ResourceLocation(res.namespace,path.toString());
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
	}
	private Scope(Scope s,Function f) {
		this.parent=s;
		this.function=f;
		this.myFlowNumber=0;
		this.resBase=s.resBase;
		s.children.add(this);
	}
	private Scope(Scope s,Statement.Flow flow) {
		this.parent=s;
		this.function=s.function;
		this.myFlowNumber=s.makeNextFlowNumber();
		this.resBase=s.resBase;
		this.flowType=flow.getFlowType();
		this.hasExtraSuffix=true;
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
	public Variable getBreakVarInMe() {
		//internal scope to the loop
		final Variable bk=new Variable("\"$break\"",VarType.BOOL,null,this.getSubRes());
		return bk;
	}
}
