package net.mcppc.compiler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.*;

/**
 * stores all the information a file exports in its header
 * @author jbarb_t8a3esk
 *
 */
public class FileInterface {
	public final ResourceLocation path;
	public final Map<String, Variable> varsPublic = new HashMap<String, Variable>();
	public final Map<String, Function> funcsPublic = new HashMap<String, Function>();
	
	public boolean add(Declaration dec) {
		if(dec.isPublic()) {
			if(dec.isFunction())return this.funcsPublic.putIfAbsent(dec.getFunction().name, dec.getFunction())==null;
			else return this.varsPublic.putIfAbsent(dec.getVariable().name, dec.getVariable())==null;
		}else {
			//CompileJob.compileMcfLog.printf("\t public failed to be public;\n");
			return false;
		
		}
	}
	void printToHeader(PrintStream h) {//depricated
		//this is done by statements
	}
	public FileInterface(ResourceLocation f) {
		this.path=f;
	}
	public static class SelfInterface extends FileInterface{
		public SelfInterface(ResourceLocation f) {
			super(f);
		}
		boolean hasReadLibs=false;
		//headers to read
		final Map<String, ResourceLocation> imports = new HashMap<String, ResourceLocation>();
		//then make interfaces in
		final Map<String, FileInterface> libs = new HashMap<String, FileInterface>();

		//member
		final Map<String,Variable> varsPrivate=new HashMap<String, Variable>();
		final Map<String,Function> funcsPrivate=new HashMap<String, Function>();
		

		final Map<String,Variable> varsExtern=new HashMap<String, Variable>();
		final Map<String,Function> funcsExtern=new HashMap<String, Function>();
		@Override public boolean add(Declaration dec) {
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
			case PUBLIC:return super.add(dec);
			default:
				//CompileJob.compileMcfLog.printf("\t keyword mystery;\n");
				return false;
			
			}
		}
		public boolean add(Import imp) {
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
			if(!this.hasReadLibs)new CompileError("must load libs before identifying variables");
			if(names.size()>2) {
				//may be struct member
				throw new CompileError("var had member of member; not yet supported");
			}
			FileInterface lib;
			String name;
			Variable v=null;
			if(names.size()==2) {
				//TODO struct member
				lib=this.libs.get(names.get(0));if(lib==null)throw new CompileError("library %s not found loaded.".formatted(names.get(0)));
				name=names.get(1);
			}else {
				name=names.get(0);
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
			p.printf("interface %s\n", this.path);
			p.printf("\t libs %s;\n", this.libs.keySet());
			p.printf("\t public funcs: %s;\n", this.funcsPublic.keySet());
			p.printf("\t private funcs: %s;\n", this.funcsPrivate.keySet());
			p.printf("\t extern funcs: %s;\n", this.funcsExtern.keySet());

			p.printf("\t public vars: %s;\n", this.varsPublic.keySet());
			p.printf("\t private vars: %s\n", this.varsPrivate.keySet());
			p.printf("\t extern vars: %s;\n", this.varsExtern.keySet());
		
		}
	}
}
