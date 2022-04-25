package net.mcppc.compiler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Declaration;
import net.mcppc.compiler.tokens.Import;
import net.mcppc.compiler.tokens.Token;

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
	final Map<String, ResourceLocation> runs = new HashMap<String, ResourceLocation>();
	//then make interfaces in
	final Map<String, FileInterface> libs = new HashMap<String, FileInterface>();

	//member
	final Map<String,Variable> varsPrivate=new HashMap<String, Variable>();
	final Map<String,Function> funcsPrivate=new HashMap<String, Function>();
	

	final Map<String,Variable> varsExtern=new HashMap<String, Variable>();
	final Map<String,Function> funcsExtern=new HashMap<String, Function>();
	public boolean add(Declaration dec) {
		//cnannot match any other var/func
		if(dec.isFunction()) {
			//CompileJob.compileMcfLog.printf("src added adding a func %s.%s(...);\n",dec.getFunction().resoucrelocation,dec.getFunction().name);
			if(funcsPrivate.containsKey(dec.getFunction().name))return false;
			if(funcsPublic.containsKey(dec.getFunction().name))return false;
			if(funcsExtern.containsKey(dec.getFunction().name))return false;
		}
		if(!dec.isFunction()) {
			//CompileJob.compileMcfLog.printf("src added adding a var %s.%s;\n",dec.getVariable().holder,dec.getVariable().name);
			if(varsPrivate.containsKey(dec.getVariable().name))return false;
			if(varsPublic.containsKey(dec.getVariable().name))return false;
			if(varsExtern.containsKey(dec.getVariable().name))return false;
			
		}
		//CompileJob.compileMcfLog.printf("\t not a double\n");
		switch (dec.access) {
		case EXTERN:
			if(dec.isFunction())return this.funcsExtern.putIfAbsent(dec.getFunction().name, dec.getFunction())==null;
			else return this.varsExtern.putIfAbsent(dec.getVariable().name, dec.getVariable())==null;
		case PRIVATE:
			if(dec.isFunction())return this.funcsPrivate.putIfAbsent(dec.getFunction().name, dec.getFunction())==null;
			else return this.varsPrivate.putIfAbsent(dec.getVariable().name, dec.getVariable())==null;
		case PUBLIC:{
			if(dec.isFunction())return this.funcsPublic.putIfAbsent(dec.getFunction().name, dec.getFunction())==null;
			else return this.varsPublic.putIfAbsent(dec.getVariable().name, dec.getVariable())==null;
		}
		default:
			//CompileJob.compileMcfLog.printf("\t keyword mystery;\n");
			return false;
		
		}
	}
	public boolean add(Import imp) {
		if(imp.willRun())this.runs.putIfAbsent(imp.getAlias(), imp.getLib());
		return this.imports.putIfAbsent(imp.getAlias(), imp.getLib())==null;
	}
	public boolean attemptLoadLibs(CompileJob job) {
		boolean success=true;
		for(String alias:this.imports.keySet()) {
			if(this.libs.containsKey(alias)) continue;//already loaded
			ResourceLocation res=this.imports.get(alias);
			if(job.hasFileInterfaceFor(res)) {
				try {
					this.libs.put(alias, job.getFileInterfaceFor(res));
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
			this.libs.put(alias, job.getFileInterfaceFor(res));
		}
		this.hasReadLibs=true;
	}
	public  Variable identifyVariable(Token.MemberName t,Scope s) throws CompileError {
		return this.identifyVariable(t.names,s);
	}
	public  Variable identifyVariable(String name) throws CompileError {//in declaration
		List<String> array=new ArrayList<String>();array.add(name);
		return this.identifyVariable(array,null);
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
		if(isSelf && s!=null && s.isInFunctionDefine()) {
			//function args
			Function f=s.function;
			for(Variable arg:f.args) {
				if (arg.name.equals(name)) {
					return true;
				}
			}//keep going
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
	public boolean hasLib(String name,Scope s) {
		if(this.isSelf(s))return this.libs.containsKey(name);
		else return false;
	}
	protected Variable getVar(String name,Scope s) {
		boolean isSelf= this.isSelf(s);
		if(isSelf && s!=null && s.isInFunctionDefine()) {
			//function args
			Function f=s.function;
			for(Variable arg:f.args) {
				if (arg.name.equals(name)) {
					return arg;
				}
			}//keep going
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
	protected Variable identifyVariable(List<String> names,Scope s,int start) throws CompileError {
		if(!this.hasReadLibs)new CompileError("must load libs before identifying variables");
		
		String name=names.get(0+start);
		boolean isLib=this.libs.containsKey(name);
		boolean isVar=this.hasVar(name, s);

		if(names.size()>=2+start && isLib) {
			return this.libs.get(name).identifyVariable(names, s, start+1);
		}else if (isVar){
			Variable v=this.getVar(name, s);
			
			for(int i=1+start;i<names.size();i++) {
				name=names.get(i);
				if(!v.hasField(name))throw new CompileError.VarNotFound(v.type, name);
				v=v.getField(name);
			}
			return v;
		}else {
			if (!isVar && names.size()>=2+start)throw new CompileError("library (or variable) %s not found loaded in scope %s.".formatted(name,s.resBase));
			else throw new CompileError("variable (or library) %s not found in scope %s".formatted(String.join(".", names),s.resBase));
		}
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
		FileInterface lib;
		String name=names.get(0+start);
		boolean isLib=this.libs.containsKey(name);
		boolean isFunc=this.hasFunc(name, s);
		//boolean isVar=this.hasVar(name, s);
		if(names.size()>=2+start && isLib) {
			return this.libs.get(name).identifyFunction(names, s, start+1);
		}else if (isFunc){
			Function f=this.getFunc(name, s);
			return f;
		}else {
			if (!isFunc && names.size()>=2+start)throw new CompileError("library %s not found loaded in scope %s.".formatted(name,s.resBase));
			else throw new CompileError("function %s not found in scope %s".formatted(String.join(".", names),s.resBase));
		}
	}
	public void printmefordebug(PrintStream p) {
		printmefordebug(p,1,0);
	}
	public void printmefordebug(PrintStream p,int depth,int tabs) {
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		p.printf("%sinterface %s\n",s.toString(), this.path);
		p.printf("%s\t public funcs: %s;\n",s.toString(), this.funcsPublic.keySet());
		p.printf("%s\t private funcs: %s;\n",s.toString(), this.funcsPrivate.keySet());
		p.printf("%s\t extern funcs: %s;\n",s.toString(), this.funcsExtern.keySet());

		p.printf("%s\t public vars: %s;\n",s.toString(), this.varsPublic.keySet());
		p.printf("%s\t private vars: %s\n",s.toString(), this.varsPrivate.keySet());
		p.printf("%s\t extern vars: %s;\n",s.toString(), this.varsExtern.keySet());
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
		for(Variable v:this.varsPublic.values()) if (v.willAllocateOnLoad(ALLOCATE_WITH_DEFAULT_VALUES)) v.allocate(p, ALLOCATE_WITH_DEFAULT_VALUES);
		for(Variable v:this.varsPrivate.values()) if (v.willAllocateOnLoad(ALLOCATE_WITH_DEFAULT_VALUES)) v.allocate(p, ALLOCATE_WITH_DEFAULT_VALUES);
		//for(Variable v:this.varsExtern.values()) if (v.willAllocateOnLoad()) v.allocate(p, ALLOCATE_WITH_DEFAULT_VALUES); //do not load externs
		if(!Function.ALLOCATE_ON_CALL) {
			for(Function f:this.funcsPublic.values()) f.allocateMyLocals(p);
			for(Function f:this.funcsPrivate.values()) f.allocateMyLocals(p);
			//for(Function f:this.funcsExtern.values()) f.allocateMyLocals(p); // do not load externs
		}
	}
}