package net.mcppc.compiler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.tokens.Declaration;
import net.mcppc.compiler.tokens.Import;
import net.mcppc.compiler.tokens.Keyword;
import net.mcppc.compiler.tokens.MemberName;

/**
 * contains all of the fields used by a mcpp file;
 * 
 * TODO figure out how templates
 * add which params accepted to FileInterface
 * add tokens with template params to other stuff
 * allow Compiler::compile2 to be bound to template args
 * @author jbarb_t8a3esk
 *
 */
public class FileInterface {
	//if this is set to true, all mask-free vars will reset on datapack load
	public static final boolean ALLOCATE_WITH_DEFAULT_VALUES=false;
	public FileInterface(ResourceLocation f) {
		this.path=f;
	}
	boolean hasReadLibs=false;
	public final ResourceLocation path;
	public final Map<String, Variable> varsPublic = new HashMap<String, Variable>();
	public final Map<String, Function> funcsPublic = new HashMap<String, Function>();
	//headers to read
	final Map<String, ResourceLocation> imports = new HashMap<String, ResourceLocation>();
	final Map<String, Boolean> importsStrict = new HashMap<String, Boolean>();
	final Map<String, ResourceLocation> runs = new HashMap<String, ResourceLocation>();
	//then make interfaces in
	final Map<String, FileInterface> libs = new HashMap<String, FileInterface>();

	//member
	final Map<String,Variable> varsPrivate=new HashMap<String, Variable>();
	final Map<String,Function> funcsPrivate=new HashMap<String, Function>();
	

	final Map<String,Variable> varsExtern=new HashMap<String, Variable>();
	final Map<String,Function> funcsExtern=new HashMap<String, Function>();
	
	//constants

	final Map<String,Const> constsPublic=new HashMap<String, Const>();
	final Map<String,Const> constsPrivate=new HashMap<String, Const>();
	
	//threads

		final Map<String,McThread> threadsPublic=new HashMap<String, McThread>();
		final Map<String,McThread> threadsPrivate=new HashMap<String, McThread>();
	//template: const value is their default
	@Deprecated
	private final List<Const> template=new ArrayList<Const>();//warning: unused, template is at function level
	
	
	//hypothetical: classes
	//final Map<String,Struct> classes=new HashMap<String, Struct>();
	public boolean hasClass(String name) {
		//unimplimented
		return false;
	}
	
	public boolean add(Declaration dec) {
		//cnannot match any other var/func
		switch(dec.getObjType()) {
		case CONST:
			for(Const cv:this.template)if(cv.name.equals(dec.getConst().name))return false;
			if(constsPrivate.containsKey(dec.getConst().name))return false;
			if(constsPublic.containsKey(dec.getConst().name))return false;
			break;
		case FUNC:
			if(funcsPrivate.containsKey(dec.getFunction().name))return false;
			if(funcsPublic.containsKey(dec.getFunction().name))return false;
			if(funcsExtern.containsKey(dec.getFunction().name))return false;
			break;
		case VAR:
			if(varsPrivate.containsKey(dec.getVariable().name))return false;
			if(varsPublic.containsKey(dec.getVariable().name))return false;
			if(varsExtern.containsKey(dec.getVariable().name))return false;
			break;
		default:
			break;
		
		}
		//CompileJob.compileMcfLog.printf("\t not a double\n");
		switch (dec.access) {
		case EXTERN: switch(dec.getObjType()) {
			case CONST: return false;
			case FUNC:return this.funcsExtern.putIfAbsent(dec.getFunction().name, dec.getFunction())==null;
			case VAR: return this.varsExtern.putIfAbsent(dec.getVariable().name, dec.getVariable())==null;
			}return false;
		case PRIVATE:switch(dec.getObjType()) {
			case CONST: return this.constsPrivate.putIfAbsent(dec.getConst().name, dec.getConst())==null;
			case FUNC:return this.funcsPrivate.putIfAbsent(dec.getFunction().name, dec.getFunction())==null;
			case VAR: return this.varsPrivate.putIfAbsent(dec.getVariable().name, dec.getVariable())==null;
			}return false;
		case PUBLIC:switch(dec.getObjType()) {
			case CONST: return this.constsPublic.putIfAbsent(dec.getConst().name, dec.getConst())==null;
			case FUNC:return this.funcsPublic.putIfAbsent(dec.getFunction().name, dec.getFunction())==null;
			case VAR: return this.varsPublic.putIfAbsent(dec.getVariable().name, dec.getVariable())==null;
			}
		default:return false;
			
		
		}
	}
	public boolean add(Import imp) {
		if(imp.willRun())this.runs.putIfAbsent(imp.getAlias(), imp.getLib());
		this.importsStrict.putIfAbsent(imp.getAlias(), imp.isStrict);
		return this.imports.putIfAbsent(imp.getAlias(), imp.getLib())==null;
	}
	public boolean add(McThread th){
		switch(th.access) {
		case PUBLIC:
			return this.threadsPublic.putIfAbsent(th.name, th)==null;
		case PRIVATE:
			return this.threadsPrivate.putIfAbsent(th.name, th)==null;
		default:
			Warnings.warningf("Warning: attempted to define thread %s with invalid access %s", th.name,th.access.name);
			return false;
		
		}
	}
	public boolean attemptLoadLibs(CompileJob job) {
		boolean success=true;
		for(String alias:this.imports.keySet()) {
			if(this.libs.containsKey(alias)) continue;//already loaded
			ResourceLocation res=this.imports.get(alias);
			boolean isStrict = this.importsStrict.get(alias);
			if(job.hasFileInterfaceFor(res)) {
				try {
					this.libs.put(alias, job.getFileInterfaceFor(res,isStrict));
				} catch (FileNotFoundException | CompileError e) {
					//will never happen if used correctly
					e.printStackTrace();
				}
			}else {
				success=false;
			}
		}
		if(success)this.hasReadLibs=true;
		return success;
	}
	public void forceLoadLibs(CompileJob job) throws FileNotFoundException, CompileError {
		for(String alias:this.imports.keySet()) {
			if(this.libs.containsKey(alias) && this.libs.get(alias)!=null) continue;//already loaded
			ResourceLocation res=this.imports.get(alias);
			boolean isStrict = this.importsStrict.get(alias);
			this.libs.put(alias, job.getFileInterfaceFor(res,isStrict));
		}
		this.hasReadLibs=true;
	}
	public  Variable identifyVariable(MemberName t,Scope s) throws CompileError {
		return this.identifyVariable(t.names,s);
	}
	public  Variable identifyVariable(String name,Scope s) throws CompileError {//in declaration
		List<String> array=new ArrayList<String>();array.add(name);
		return this.identifyVariable(array,s);
	}

	public  Variable identifyVariable(List<String> names,Scope s) throws CompileError {
		return this.identifyVariable(names, s, 0);
	}
	public boolean isSelf(Scope s) {
		if(s==null)return true;
		else return this.path.equals(s.resBase);
	}

	public boolean hasVar(String name,Scope s) {
		boolean isSelf= this.isSelf(s);
		if(isSelf) {
			if(s.hasLoopLocal(name)) return true;
		}
		if(isSelf && s!=null && s.isInFunctionDefine()) {
			//function args
			Function f=s.function;
			if(f.hasThis() && name.equals(Keyword.THIS.name)) {
				return true;
			}
			for(Variable arg:f.args) {
				if (arg.name.equals(name)) {
					return true;
				}
			}
			if (f.locals.containsKey(name)) return true;
			//keep going
		}
		boolean ret=this.varsPublic.containsKey(name);
		if(isSelf)ret=ret || this.varsPrivate.containsKey(name);
		if(isSelf)ret=ret || this.varsExtern.containsKey(name);
		return ret;
	}
	public boolean hasFunc(String name,Scope s) {
		boolean isSelf= this.isSelf(s);
		boolean ret=this.funcsPublic.containsKey(name);
		if(isSelf)ret=ret || this.funcsPrivate.containsKey(name);
		if(isSelf)ret=ret || this.funcsExtern.containsKey(name);
		return ret;
	}
	public boolean hasTheFunc(Function f) {
		switch (f.access) {
		case EXTERN:
			return this.funcsExtern.containsKey(f.name) && this.funcsExtern.get(f.name) == f; // do compare address
		case PRIVATE:
			return this.funcsPrivate.containsKey(f.name) && this.funcsPrivate.get(f.name) == f;
		case PUBLIC:
			return this.funcsPublic.containsKey(f.name) && this.funcsPublic.get(f.name) == f;
		default:
			break;
		
		}
		return false;
	}
	public boolean hasConst(String name,Scope s) {
		boolean isSelf= this.isSelf(s);
		if(s.checkForTemplateOrLocalConst(name)!=null)return true;
		if(isSelf && s!=null && s.isInFunctionDefine()) {
			//function args
			Function f=s.function;
			if(f.template!=null)for(Const arg:f.template.params) {
				if (arg.name.equals(name)) {
					return true;
				}
			}//keep going
		}
		boolean ret=this.constsPublic.containsKey(name);
		if(isSelf)ret=ret || this.constsPrivate.containsKey(name);
		if(isSelf && !ret) {
			//template
			for(Const c:this.template)if(c.name.equals(name))return true;
		}
		//no extern constants
		return ret;
	}
	public boolean hasThread(String name,Scope s) {
		boolean isSelf= this.isSelf(s);
		boolean ret=this.threadsPublic.containsKey(name);
		if(isSelf)ret=ret || this.threadsPrivate.containsKey(name);
		//no extern threads
		return ret;
	}
	public boolean hasLib(String name,Scope s) {
		if(this.isSelf(s))return this.libs.containsKey(name);
		else return false;
	}
	public FileInterface getDirectLib(String name,Scope s) {
		if(this.isSelf(s))return this.libs.get(name);
		else return null;
	}
	protected Variable getVar(String name,Scope s) {
		boolean isSelf= this.isSelf(s);
		if(isSelf) {
			if(s.hasLoopLocal(name)) return s.getLoopLocal(name);
		}
		if(isSelf && s!=null && s.isInFunctionDefine()) {
			//function args
			Function f=s.function;
			if(f.hasThis() && name.equals(Keyword.THIS.name)) {
				return f.self;
			}
			for(Variable arg:f.args) {
				if (arg.name.equals(name)) {
					return arg;
				}
			}
			if (f.locals.containsKey(name)) return f.locals.get(name);
			//keep going
		}
		if(this.varsPublic.containsKey(name)) return this.varsPublic.get(name);
		if(isSelf && this.varsPrivate.containsKey(name)) return this.varsPrivate.get(name);
		if(isSelf && this.varsExtern.containsKey(name)) return this.varsExtern.get(name);
		return null;
	}
	protected Function getFunc(String name,Scope s) {
		boolean isSelf= this.isSelf(s);
		if(this.funcsPublic.containsKey(name)) return this.funcsPublic.get(name);
		if(isSelf && this.funcsPrivate.containsKey(name)) return this.funcsPrivate.get(name);
		if(isSelf && this.funcsExtern.containsKey(name)) return this.funcsExtern.get(name);
		return null;
	}

	protected Const getConst(String name,Scope s) {
		boolean isSelf= this.isSelf(s);
		if(isSelf && s!=null && s.isInFunctionDefine()) {
			//function args
			Function f=s.function;
			if(f.template!=null)for(Const arg:f.template.params) {
				if (arg.name.equals(name)) {
					return arg;
				}
			}//keep going
		}
		if(isSelf)for(Const cv:this.template)if(cv.name.equals(name))return cv;
		if(this.constsPublic.containsKey(name)) return this.constsPublic.get(name);
		if(isSelf && this.constsPrivate.containsKey(name)) return this.constsPrivate.get(name);
		return null;
	}
	public McThread getThread(String name,Scope s) {
		boolean isSelf= this.isSelf(s);
		if(this.threadsPublic.containsKey(name)) return this.threadsPublic.get(name);
		if(isSelf && this.threadsPrivate.containsKey(name)) return this.threadsPrivate.get(name);
		//no extern threads
		return null;
	}
	protected Variable identifyVariable(List<String> names,Scope s,int start) throws CompileError {
		if(!this.hasReadLibs)new CompileError("must load libs before identifying variables");
		
		String name=names.get(0+start);
		boolean isSelf=this.isSelf(s);
		boolean isLib=this.libs.containsKey(name) && isSelf;
		boolean isVar=this.hasVar(name, s);

		if(names.size()>=2+start && isLib) {
			return this.libs.get(name).identifyVariable(names, s, start+1);
		}else if (isVar){
			Variable v=this.getVar(name, s);
			//check for fields
			for(int i=1+start;i<names.size();i++) {
				name=names.get(i);
				if(!v.hasField(name))throw new CompileError.VarNotFound(v.type, name);
				v=v.getField(name);
			}
			return v;
		}else {
			if (!isVar && names.size()>=2+start)throw new CompileError("library (or variable) %s not found loaded in scope %s.".formatted(name,s.resBase));
			else throw new CompileError("variable (or library) %s not found in scope %s".formatted(String.join(".", names),s.getSubRes()));
		}
	}

	public  void linkFunction(Function.FuncCallToken t,Compiler c, Scope s) throws CompileError {
		this.linkFunction(t, c, s, false);
	}
	public  void linkFunction(Function.FuncCallToken t,Compiler c, Scope s,boolean forceStrict) throws CompileError {
		if(!t.func.hasTemplate())return;
		//add link request
		List<String> names = t.names;
		String name=names.get(0);
		boolean isSelf=this.isSelf(s);
		boolean isLibStrict=this.libs.containsKey(name) && this.importsStrict.get(name) && isSelf;
		isLibStrict= isLibStrict || forceStrict;
		if(isLibStrict) {
			ResourceLocation res=t.getMyMCF();
			c.job.externalImportsStrict.add(res);
		}
		
		return;
	}
	public  Function identifyFunction(Function.FuncCallToken t,Scope s) throws CompileError {
		return this.identifyFunction(t.names,s);
	}
	
	public  Function identifyFunction(String name,Scope s) throws CompileError {
		List<String> array=new ArrayList<String>();array.add(name);
		return this.identifyFunction(array,s);
	}

	public  Function identifyFunction(List<String> names,Scope s) throws CompileError {
		return this.identifyFunction(names, s, 0);
	}
	public  Function identifyFunction(List<String> names,Scope s,int start) throws CompileError {
		if(!this.hasReadLibs)new CompileError("must load libs before identifying functions");
			
		if(names.size()>2) {
			//may be struct member
			throw new CompileError("var had member of member; not yet supported");
		}
		String name=names.get(0+start);
		boolean hasNext = names.size()>start+1;
		boolean isSelf=this.isSelf(s);
		boolean isLib=this.libs.containsKey(name) && isSelf;
		boolean isVar=this.hasVar(name, s)&& hasNext && false;
		boolean isFunc=this.hasFunc(name, s) ;
		//boolean isVar=this.hasVar(name, s);
		if(names.size()>=2+start && isLib) {
			return this.libs.get(name).identifyFunction(names, s, start+1);
		}else if (isFunc){
			Function f=this.getFunc(name, s);
			return f;
		}else if (isVar){
			//nonstatic members
			Variable v = this.getVar(name, s);
			if(!v.type.isStruct()) throw new CompileError("type %s has no members;".formatted(v.type.asString()));
			Struct clazz = v.type.struct;
			String nextname = names.get(1+start);
			//unreached,
			throw new CompileError("memb funcs do not exist yet unless builtin");
			
		} else {
			if (!isFunc && names.size()>=2+start)throw new CompileError("library %s not found loaded in scope %s.".formatted(name,s.resBase));
			else throw new CompileError("function %s not found in scope %s".formatted(String.join(".", names),s.resBase));
		}
	}
	public  Function checkForFunctionWithTemplate(List<String> names,Scope s) {
		try {
			Function f=this.identifyFunction(names, s, 0);
			if(f.hasTemplate())return f;
			else return null;
		} catch (CompileError e) {
			return null;
		}
		
	}

	public  Const identifyConst(MemberName t,Scope s) throws CompileError {
		return this.identifyConst(t.names,s);
	}
	public  Const identifyConst(String name,Scope s) throws CompileError {
		List<String> array=new ArrayList<String>();array.add(name);
		return this.identifyConst(array,s);
	}

	public Const identifyConst(List<String> names,Scope s) throws CompileError {
		return this.identifyConst(names, s, 0);
	}
	public  Const identifyConst(List<String> names,Scope s,int start) throws CompileError {
		if(start==0 && names.size()==1) {
			Const cv=s.checkForTemplateOrLocalConst(names.get(0));
			if(cv!=null)return cv;
		}
		if(!this.hasReadLibs)new CompileError("must load libs before identifying consts");
			
		if(names.size()>2) {
			//may be struct member
			throw new CompileError("var had member of member; not yet supported");
		}
		String name=names.get(0+start);
		boolean isLib=this.libs.containsKey(name);
		boolean isConst=this.hasConst(name, s);
		//boolean isVar=this.hasVar(name, s);
		if(names.size()>=2+start && isLib) {
			return this.libs.get(name).identifyConst(names, s, start+1);
		}else if (isConst){
			Const cv=this.getConst(name, s);
			return cv;
		}else {
			if (!isConst && names.size()>=2+start)throw new CompileError("library %s not found loaded in scope %s.".formatted(name,s.resBase));
			else {
				throw new CompileError("const '%s' not found in scope %s".formatted(String.join(".", names),s.resBase));
			}
		}
	}
	public  McThread identifyThread(MemberName t,Scope s) throws CompileError {
		return this.identifyThread(t.names,s);
	}
	public  McThread identifyThread(String name,Scope s) throws CompileError {
		List<String> array=new ArrayList<String>();array.add(name);
		return this.identifyThread(array,s);
	}

	public McThread identifyThread(List<String> names,Scope s) throws CompileError {
		return this.identifyThread(names, s, 0);
	}
	public  McThread identifyThread(List<String> names,Scope s,int start) throws CompileError {
		if(!this.hasReadLibs)new CompileError("must load libs before identifying threads");
			
		if(names.size()>2) {
			//may be struct member
			throw new CompileError("var had member of member; not yet supported");
		}
		String name=names.get(0+start);
		boolean isLib=this.libs.containsKey(name);
		boolean isThread=this.hasThread(name, s);
		//boolean isVar=this.hasVar(name, s);
		if(names.size()>=2+start && isLib) {
			return this.libs.get(name).identifyThread(names, s, start+1);
		}else if (isThread){
			McThread th=this.getThread(name, s);
			return th;
		}else {
			if (!isThread && names.size()>=2+start)throw new CompileError("library %s not found loaded in scope %s.".formatted(name,s.resBase));
			else {
				throw new CompileError("thread '%s' not found in scope %s".formatted(String.join(".", names),s.resBase));
			}
		}
	}
	@Deprecated
	public String getNameOfConstIn(Const cv,Scope s) throws CompileError {
		if( this.isSelf(s))
			return cv.name;
		else {
			if(cv.access!=Keyword.PUBLIC)throw new CompileError("cannot access non-public const %s.%s from %s".formatted(cv.path,cv.name,s.resBase));
			for (Entry<String, ResourceLocation> entry : this.imports.entrySet()) {
		        //
				String alias=entry.getKey();
				ResourceLocation path = entry.getValue();
				if (path.equals(cv.path))return "%s.%s".formatted(alias,cv.name);
		    }
		}
		throw new CompileError("cannot find accessible const %s.%s from %s".formatted(cv.path,cv.name,s.resBase));
	}
	public void printmefordebug(PrintStream p) {
		printmefordebug(p,1,0);
	}
	public void printmefordebug(PrintStream p,int depth,int tabs) {
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		p.printf("%sinterface %s\n",s.toString(), this.path);
		//p.printf("%s\t template: %s;\n",s.toString(), this.template.stream().map(f->f.name));
		if(!this.constsPublic.isEmpty())p.printf("%s\t public consts: %s;\n",s.toString(), this.constsPublic.keySet());
		if(!this.constsPrivate.isEmpty())p.printf("%s\t private consts: %s\n",s.toString(), this.constsPrivate.keySet());
		
		if(!this.funcsPublic.isEmpty())p.printf("%s\t public funcs: %s;\n",s.toString(), this.funcsPublic.entrySet().stream().map(f -> f.getKey()+(f.getValue().template==null?"":"<...>")).toList());
		//p.printf("%s\t public funcs: %s;\n",s.toString(), this.funcsPublic.keySet());
		if(!this.funcsPrivate.isEmpty())p.printf("%s\t private funcs: %s;\n",s.toString(), this.funcsPrivate.entrySet().stream().map(f -> f.getKey()+(f.getValue().template==null?"":"<...>")).toList());
		if(!this.funcsExtern.isEmpty())p.printf("%s\t extern funcs: %s;\n",s.toString(), this.funcsExtern.entrySet().stream().map(f -> f.getKey()+(f.getValue().template==null?"":"<...>")).toList());

		if(!this.varsPublic.isEmpty())p.printf("%s\t public vars: %s;\n",s.toString(), this.varsPublic.keySet());
		if(!this.varsPrivate.isEmpty())p.printf("%s\t private vars: %s\n",s.toString(), this.varsPrivate.keySet());
		if(!this.varsExtern.isEmpty())p.printf("%s\t extern vars: %s;\n",s.toString(), this.varsExtern.keySet());

		if(!this.threadsPublic.isEmpty())p.printf("%s\t public threads: %s;\n",s.toString(), this.threadsPublic.keySet());
		if(!this.threadsPrivate.isEmpty())p.printf("%s\t private threads: %s\n",s.toString(), this.threadsPrivate.keySet());
		if(depth<=0) {
			p.printf("%s\t libs %s;\n", s.toString(),this.libs.keySet());
			
		}else {
			for (FileInterface itf:this.libs.values()) {
				itf.printmefordebug(p, depth-1, tabs+1);
			}
		}
	
	}
	public void allocateAll(PrintStream p,Namespace ns) throws CompileError {
		//System.err.printf("allocate vars in %s;\n", this.path);
		for(Variable v:this.varsPublic.values()) if (v.willAllocateOnLoad(ALLOCATE_WITH_DEFAULT_VALUES)) v.allocateLoad(p, ALLOCATE_WITH_DEFAULT_VALUES);
		for(Variable v:this.varsPrivate.values()) if (v.willAllocateOnLoad(ALLOCATE_WITH_DEFAULT_VALUES)) v.allocateLoad(p, ALLOCATE_WITH_DEFAULT_VALUES);
		//for(Variable v:this.varsExtern.values()) if (v.willAllocateOnLoad()) v.allocate(p, ALLOCATE_WITH_DEFAULT_VALUES); //do not load externs
		if(true) {
			for(Function f:this.funcsPublic.values()) f.allocateMyLocalsLoad(p);
			for(Function f:this.funcsPrivate.values()) f.allocateMyLocalsLoad(p);
			//for(Function f:this.funcsExtern.values()) f.allocateMyLocals(p); // do not load externs
		}
	}
}