package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.*;
import net.mcppc.compiler.tokens.Equation.End;

/**
 * declaration of a function;
 * 
 * TODO allow args to be declared "ref" to back-copy after function call is over
 * 
 * TODO allow func-local vars
 * TODO add option recursive to switch vars to stacks (on call, prepend the value, then access [0] to read/write; when finished,remove [0])
 * TODO add inline option; compiles to a .mcfunction.inline file and contains fstrings
 * @author jbarb_t8a3esk
 *
 */
public class Function {
	public static final boolean ALLOCATE_ON_CALL = false;//this is false to save lines
	public static class FuncCallToken extends Token implements Identifiable{
		public static FuncCallToken make(Compiler c,int line,int col,Matcher m,Token.MemberName func,RStack stack) throws CompileError {
			FuncCallToken f=new FuncCallToken(line,col,func);
			FuncCallToken.addArgs(c, line, col, m,  stack, f.args);
			
			return f;
		}
		public static List<? super Equation> addArgs(Compiler c,int line,int col,Matcher m,RStack stack,List<? super Equation> args) throws CompileError {
			fargs: while(true) {
				Equation arg=new Equation(line, col, stack);
				arg.isAnArg=true;
				arg.isTopLevel=false;
				arg.populate(c, m);
				if(arg.elements.size()==0) {
					break fargs;
				}
				args.add(arg);
				if(arg.end==End.CLOSEPAREN)break fargs;
				else if(arg.end!=End.ARGSEP)throw new CompileError("unexpected arg list ended with a %s.".formatted(arg.end.name()));
			}
			return args;
		}
		
		final List<String> names;
		public final List<Equation> args=new ArrayList<Equation>();
		TemplateArgsToken tempArgs=null;
		Function func; public Function getFunction() {return this.func;}
		public FuncCallToken(int line, int col,MemberName fname) {
			super(line, col);
			this.names=fname.names;
		}
		public FuncCallToken addArg(Equation arg) {
			this.args.add(arg);return this;
		}
		public FuncCallToken withTemplate(TemplateArgsToken tgs) {
			this.tempArgs=tgs;return this;
		}

		public boolean hasTemplate() {
			return this.tempArgs!=null;
		}
		@Override public String asString() {
			return "%s(%s)".formatted(String.join(".", names),"(...)");
		}
		@Override
		public int identify(Compiler c,Scope s) throws CompileError {
			this.func=c.myInterface.identifyFunction(this,s);
			//c.myInterface.linkFunction(this,c,s);
			//TODO recursion warnings
			if(this.func.args.size()!=this.args.size())throw new CompileError("wrong number of args, %d,  in function %s which takes %d args"
					.formatted(this.args.size(),this.func.args.size(),this.func.name));
			return 0;
		}
		public void linkMe(Compiler c,Scope s) throws CompileError {
			c.myInterface.linkFunction(this,c,s);
		}

		public void call(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError {
			//todo call this.func.mcf
			if(ALLOCATE_ON_CALL) this.func.allocateMyLocals(p);
			for(int i=0;i<this.func.args.size();i++) {
				Variable arg=this.func.args.get(i);
				Equation eq=this.args.get(i);
				if(arg.isReference() && !eq.isRefable())
					throw new CompileError("attempted to pass non-trivial equation as a ref to function %s(...) on line %d col %d;"
						.formatted(this.func.name,this.line,this.col));
				eq.compileOps(p, c, s,arg.type);
				eq.setVar(p, c, s, arg);
			}

			//actually call the function
			//TODO request template
			if(this.tempArgs!=null)this.func.requestTemplate(tempArgs, s);
			p.printf("function %s\n", this.getMyMCF());
			
			for(int i=0;i<this.func.args.size();i++) {
				Variable arg=this.func.args.get(i);
				if(arg.isReference()) {
					//back copy after func call
					 Variable ref=((Equation)this.args.get(i)).getVarRef();
					 Variable.directSet(p,s, ref, arg, stack);
				}
			}
			//set retval later
			
		} 
		public ResourceLocation getMyMCF() {
			return this.func.getMCF(this.tempArgs);
		}
		public void getRet(PrintStream p,Compiler c,Scope s,RStack stack,int home) throws CompileError {
			this.func.returnV.getMe(p,s, stack,home);
		}
		public void getRet(PrintStream p,Compiler c,Scope s,Variable v,RStack stack) throws CompileError {
			
			if(func.template!=null)s.addTemplateConstsTemporarily(func, tempArgs);
			Variable.directSet(p,s, v, this.func.returnV, stack);
			s.removeTemporaryConsts();
		}
		public Variable getRetConstRef() {
			return this.func.returnV;
		}
		public Number getEstimate() {
			return null;//TODO
		}
	}
	ResourceLocation resoucrelocation;
	//the name used to call the function
	public final String name;
	//the name of the mcfunction that will be called
	//public String mcfName;
	private ResourceLocation mcf;
	public final VarType retype;
	public final Keyword access;
	public final List<Variable> args=new ArrayList<Variable>();
	public final Map<String, Variable> locals=new HashMap<String, Variable>();
	public final List<Const> localConsts=new ArrayList<Const>();
	public final Variable returnV;
	TemplateDefToken template=null;
	private List<TemplateArgsToken> requestedBinds=new ArrayList<TemplateArgsToken>();
	private final List<TemplateArgsToken> requestedBindsFilled=new ArrayList<TemplateArgsToken>();
	public Function(String name,VarType ret,Keyword access, Compiler c) {
		this.name=name;
		this.retype=ret;
		this.access=access;
		this.resoucrelocation=c.resourcelocation;
		this.returnV=new Variable("$return", ret, access, c).returnOf(this);
		this.mcf=Scope.getSubRes(c.resourcelocation, this);
	}
	public Function withArg(Variable var, Compiler c,boolean isRef) {
		this.args.add(var.parameterOf(this,isRef));
		return this;
	}
	public Function withLocalVar(Variable var, Compiler c) {
		if(var.isBasic()) var.localOf(this);
		this.locals.put(var.name,var);
		return this;
	}
	public void addConst(Const c) {
		this.localConsts.add(c);
	}
	public Function withMCFName(String n) {
		//must be extern
		this.mcf=Scope.getSubRes(this.resoucrelocation, n);
		return this;
	}
	public Function withMCF(ResourceLocation n) {
		//must be extern
		this.mcf=n;
		
		return this;
	}
	public ResourceLocation getMCF() {
		return this.mcf;
	}
	public ResourceLocation getMCF(TemplateArgsToken template) {
		if(template==null)return getMCF();
		StringBuffer path=new StringBuffer(mcf.path);
		Scope.appendTemplate(path, template);
		return new ResourceLocation(mcf.namespace,path.toString());
		
	}
	public Function withTemplate(TemplateDefToken tp) throws CompileError {
		this.template=tp;
		this.fillDefaults();
		return this;
	}
	public boolean hasTemplate() {
		return this.template!=null;
	}

	public String getTemplateInH() {
		return this.template.inHDR();
	}
	public boolean requestTemplate(TemplateArgsToken args,Scope s)throws CompileError {
		//not needed for default args
		if(s!=null &&s.getFunction()==this) {
			//skip to prefent infinite loop
			return false;
		}
		if(this.requestedBinds.contains(args) ||this.requestedBindsFilled.contains(args))return false;
		return this.requestedBinds.add(args);
	}
	private void fillDefaults()throws CompileError {
		//not needed for default args
		if(this.template==null)return;
		this.requestedBindsFilled.addAll(this.template.getAllDefaultArgs());
	}
	public List<TemplateArgsToken> getRequestedTemplateBinds(){
		return this.requestedBinds;
	}
	public List<TemplateArgsToken> popRequestedTemplateBinds(){
		List<TemplateArgsToken> list = this.requestedBinds;
		this.requestedBindsFilled.addAll(list);
		this.requestedBinds=new ArrayList<TemplateArgsToken>();
		return list;
	}
	public ResourceLocation getResoucrelocation() {return this.resoucrelocation;}
	public void setResourceLocation(ResourceLocation path) {
		this.resoucrelocation=path;
		for(Variable b:this.args) {
			b.holder=path.toString();
		}
	}
	
	public String toHeader() throws CompileError {
		String tmp = this.hasTemplate()? this.getTemplateInH()+" ":"";
		String[] argss=new String[this.args.size()];
		for(int i=0;i<argss.length;i++)argss[i]=this.args.get(i).toHeader();
		return "%s%s %s (%s)".formatted(tmp,this.retype.headerString(),this.name,
				String.join(" , ", argss)
				);
	}
	
	public static final String RET_TAG="\"$return\"";
	@Deprecated public String returnDataPhrase() {
		//use this.
		return "storage %s %s.%s".formatted(this.resoucrelocation,this.name,RET_TAG);
	}
	
	public void allocateMyLocals(PrintStream p) throws CompileError {
		for(Variable arg:this.args)if(arg.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))arg.allocate(p, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
		if(this.returnV.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.returnV.allocate(p, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
	}
}
