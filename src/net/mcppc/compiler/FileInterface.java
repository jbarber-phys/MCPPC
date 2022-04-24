package net.mcppc.compiler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Declaration;
import net.mcppc.compiler.tokens.Import;
import net.mcppc.compiler.tokens.Token;

public class FileInterface {
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
	protected Variable identifyVariable(List<String> names,Scope s,int start) throws CompileError {
		if(!this.hasReadLibs)new CompileError("must load libs before identifying variables");
		if(names.size()>2+start) {
			//may be struct member
			//TODO remove this
			throw new CompileError("var had member of member; not yet supported");
		}
		FileInterface lib;
		String name;
		Variable v=null;
		if(names.size()>=2+start) {
			//TODO struct member
			lib=this.libs.get(names.get(0+start));
			
			if(lib==null)throw new CompileError("library %s not found loaded.".formatted(names.get(0+start)));
			name=names.get(1+start);
		}else {
			name=names.get(0+start);
			if(s!=null && s.isInFunctionDefine()) {
				//function args
				Function f=s.function;
				for(Variable arg:f.args) {
					if (arg.name.equals(name)) {
						v=arg;break;
					}
				}
			}
			if(v==null)v=this.varsPrivate.get(name);
			if(v==null)v=this.varsExtern.get(name);
			lib=this;
			//CompileJob.compileMcfLog.printf("\t vars private %s;\n",this.varsPrivate.keySet());
			//CompileJob.compileMcfLog.printf("\t vars extern %s;\n",this.varsExtern.keySet());
		}
		//CompileJob.compileMcfLog.printf("lib: %s; look for var: %s\n",lib.path,name);
		//CompileJob.compileMcfLog.printf("\t vars public %s;\n",lib.varsPublic.keySet());
		if(v==null)v=lib.varsPublic.get(name);
		if(v==null)throw new CompileError("variable %s not found loaded.".formatted(String.join(".", names)));
		return v;
	}
	public  Function identifyFunction(Function.FuncCallToken t) throws CompileError {
		return this.identifyFunction(t.names);
	}
	public  Function identifyFunction(String name) throws CompileError {
		List<String> array=new ArrayList<String>();array.add(name);
		return this.identifyFunction(array);
	}
	public  Function identifyFunction(List<String> names) throws CompileError {
		if(!this.hasReadLibs)new CompileError("must load libs before identifying functions");
			
		if(names.size()>2) {
			//may be struct member
			throw new CompileError("var had member of member; not yet supported");
		}
		FileInterface lib;
		String name;
		Function f=null;
		if(names.size()==2) {
			//TODO struct member
			lib=this.libs.get(names.get(0));if(lib==null)throw new CompileError("library %s not found loaded.".formatted(names.get(0)));
			name=names.get(1);
		}else {
			name=names.get(0);
			f=this.funcsPrivate.get(name);
			if(f==null)f=this.funcsExtern.get(name);
			lib=this;
		}
		if(f==null)f=lib.funcsPublic.get(name);
		if(f==null)throw new CompileError("variable %s not found loaded.".formatted(String.join(".", names)));
		return f;
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
}