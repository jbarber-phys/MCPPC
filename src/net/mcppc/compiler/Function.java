package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.INbtValueProvider.Macro;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.functions.AbstractCallToken;
import net.mcppc.compiler.struct.StackStorage;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.target.Version;
import net.mcppc.compiler.tokens.*;
import net.mcppc.compiler.tokens.Equation.End;

/**
 * represents an mcpp function;
 * @author RadiumE13
 *
 */
public class Function {
	//this no longer applies with functions being recursive now
	@Deprecated public static final boolean ALLOCATE_ON_CALL = false;//this is false to save lines
	public static class FuncCallToken extends AbstractCallToken implements Identifiable{
		public static FuncCallToken make(Compiler c,Scope s,int line,int col,Matcher m,MemberName func, RStack stack) throws CompileError {
			FuncCallToken f=new FuncCallToken(line,col,func);
			FuncCallToken.addArgs(c, s, line, col,  m, stack, f.args);
			
			return f;
		}
		public static List<? super Equation> addArgs(Compiler c,Scope s,int line,int col,Matcher m,RStack stack, List<? super Equation> args) throws CompileError {
			fargs: while(true) {
				Equation arg=new Equation(line, col, stack);
				arg.isAnArg=true;
				arg.isTopLevel=false;
				arg=arg.populate(c, s, m);
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
		boolean isIdentified=false;
		public FuncCallToken(int line, int col,String fname,Function f,Compiler c,Scope s) {
			super(line, col);
			this.names=List.of(fname);
			this.func=f;
			this.isIdentified=true;
			if(s.isInFunctionDefine()) c.addCrossCall(s.function, this.func,s);
		}
		public FuncCallToken addArg(Equation arg) {
			this.args.add(arg);return this;
		}
		@Override public FuncCallToken withTemplate(TemplateArgsToken tgs) {
			this.tempArgs=tgs;return this;
		}

		@Override public boolean hasTemplate() {
			return this.tempArgs!=null;
		}
		@Override public String asString() {
			String ths=this.hasThisBound()?this.getThisBound().name+".":"";
			return "%s%s(%s)".formatted(ths,String.join(".", names),"...");
		}
		@Override
		public int identify(Compiler c,Scope s) throws CompileError {
			if(this.isIdentified)return 0;
			this.func=c.myInterface.identifyFunction(this,s);
			//c.myInterface.linkFunction(this,c,s);
			//TODO recursion warnings
			if(this.func.args.size()!=this.args.size())throw new CompileError("wrong number of args, %d,  in function %s which takes %d args"
					.formatted(this.args.size(),this.func.name,this.func.args.size()));
			if(s.isInFunctionDefine()) c.addCrossCall(s.function, this.func,s);
			this.isIdentified=true;
			return 0;
		}
		private Boolean ifLinkWillForce = null;
		@Override
		public void rebindTemplatesBeforeCompile(Compiler c,Scope s) throws CompileError {
			if(this.tempArgs==null) return;
			//before call, must identify all templates
			if(this.ifLinkWillForce!=null) {
				this.tempArgs.rebind(c,s);
				if(this.ifLinkWillForce) {
					this.linkMeByForceNow(c, s);
				} else {
					this.linkMeNow(c, s);
				}
			}
		}
		private void linkMeNow(Compiler c,Scope s) throws CompileError {
			c.myInterface.linkFunction(this,c,s);
		}
		public void linkMe(Compiler c,Scope s) throws CompileError {
			if(this.tempArgs==null || !this.tempArgs.dependsOnOtherTemplate()) {
				this.linkMeNow(c, s);
			} else {
				this.ifLinkWillForce = false;
			}
		}
		private void linkMeByForceNow(Compiler c,Scope s) throws CompileError {
			c.myInterface.linkFunction(this,c,s,true);
		}
		public void linkMeByForce(Compiler c,Scope s) throws CompileError {
			if(this.tempArgs==null || !this.tempArgs.dependsOnOtherTemplate()) {
				this.linkMeByForceNow(c, s);
			} else {
				this.ifLinkWillForce = true;
			}
		}
		private Variable getTempArg(PrintStream p,Compiler c,Scope s,RStack stack,int index) throws CompileError   {
			String nm = "\"$temp_%d\"".formatted(index);
			VarType type = this.func.getArg(index).type;
			return new Variable(nm, type, null, this.getMyMCF());
		}
		private Variable getTempRet(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError   {
			String nm = "\"$temp_return\"";
			VarType type = this.func.retype;
			return new Variable(nm, type, null, this.getMyMCF());
		}
		private Variable getTempSelf(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError   {
			String nm = "\"$temp_this\"";
			VarType type = this.func.self.type;
			return new Variable(nm, type, null, this.getMyMCF());
		}
		private boolean isInSelf = false;
		@Override
		public void call(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError {
			//todo call this.func.mcf
			if(s.isInFunctionDefine() && s.function == this.func) {
				isInSelf=true;
				if(!this.func.canRecurr) {
					Warnings.warningf(c,"function %s is not recursive but was found in itself; possible bad behavior;", this.func.name);
					this.isInSelf=false;
				}
			}
			if(!this.isInSelf)this.func.allocateMyLocalsCallOutside(p, s);
			for(int i=0;i<this.func.args.size();i++) {
				Variable arg=this.isInSelf?
						this.getTempArg(p, c, s, stack, i)
						:this.func.args.get(i);
				if(this.func.isExtern && arg.isReference()) throw new CompileError("extern func cannot pass args by reference");
				Equation eq=this.args.get(i);
				if(arg.isReference() && !eq.isRefable())
					throw new CompileError("attempted to pass non-trivial equation as a ref to function %s(...) on line %d col %d;"
						.formatted(this.func.name,this.line,this.col));
				eq.compileOps(p, c, s,arg.type);
				eq.setVar(p, c, s, arg);
			}
			//System.err.printf("%s\n", this.asString());
			if(this.hasThisBound()) {
				if(this.func.isExtern) throw new CompileError("extern func cannot be a nonstatic member");
				if(!this.func.hasThis())
					throw new CompileError("cannot call %s as a member of an object, it is not a nonstatic member;".formatted(func.name));
				Variable self = this.isInSelf? this.getTempSelf(p, c, s, stack): this.func.self;
				Variable.directSet(p, s, self, this.getThisBound(), stack);
			}else if(this.func.hasThis())
					throw new CompileError("cannot call %s in a global / static context as it is a nonstatic member".formatted(func.name));
			if(this.isInSelf) {
				this.func.allocateMyLocalsCallOutside(p, s);
				for(int i=0;i<this.func.args.size();i++) {
					Variable arg=this.func.args.get(i);
					Variable temp = this.getTempArg(p, c, s, stack, i);
					Variable.directSet(p, s, arg, temp, stack);
				}if(this.hasThisBound()) {
					Variable self = this.func.self;
					Variable temp = this.getTempSelf(p, c, s, stack);
					Variable.directSet(p, s, self, temp, stack);
				}
				
			}
			int stacksize=0;
			if(this.func.canRecurr) {
				stacksize=StackStorage.storeStack(p, c, s, stack, this.func.stackBackup);
			}
			//actually call the function
			this.requestTemplate(s);
			
			//TODO if extern, get true return
			boolean externMcf = this.func.isExtern;
			boolean hasRet = !this.getRetType(s).isVoid();
			boolean hasArgs = this.args.size()>0;
			if(this.getRetType(s).isVoid() && this.args.size()==0) externMcf=false;//can skip macros + return
			if(this.func.isExtern) {
				if(hasRet)VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_3_SNAP), s.getTarget(), "call to extern", c);
				else VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_2), s.getTarget(), "call to extern", c);
				Variable macrotag = hasArgs? Variable.macrosTag(this.func) : null;//already set
				Variable externRet = hasRet? this.func.returnV : null;
				this.getMyMCF().runExtern(p,s,macrotag,externRet);
			}else {
				//normal func call
				this.getMyMCF().run(p,s.getTarget());
			}
			
			if(this.func.canRecurr) {
				StackStorage.restoreStack(p, c, s, stack, this.func.stackBackup,stacksize);
			}
			if(this.isInSelf) {
				for(int i=0;i<this.func.args.size();i++) {
					Variable arg=this.func.args.get(i);
					if(arg.isReference()) {
						//back copy after func call
						 Variable ref= this.getTempArg(p, c, s, stack, i);
								 //((Equation)this.args.get(i)).getVarRef();
						 Variable.directSet(p,s, ref, arg, stack);
					}
				}
				if(this.hasThisBound() && this.func.self.isReference()) {
					if(!this.func.hasThis())
						throw new CompileError("cannot call %s as a member of an object, it is not a nonstatic member;".formatted(func.name));
					Variable tempself = this.getTempSelf(p, c, s, stack);
					Variable.directSet(p, s, tempself , this.func.self, stack);
				}
				if(!this.getRetType(s).isVoid() ) {
					Variable tempret = this.getTempRet(p, c, s, stack);
					if(this.hasTemplate())s.addTemplateConstsTemporarily(func, tempArgs);
					Variable.directSet(p, s, tempret , this.func.returnV, stack);
					if(this.hasTemplate())s.removeTemporaryConsts();
				}
				this.func.deallocateLocalAfterCallOutside(p, s);
			}
			for(int i=0;i<this.func.args.size();i++) {
				if(this.func.args.get(i).isReference()) {
					 Variable arg=this.isInSelf?
							this.getTempArg(p, c, s, stack, i)
							:this.func.args.get(i);
					 //back copy after func call
					 Variable ref=((Equation)this.args.get(i)).getVarRef();
					 Variable.directSet(p,s, ref, arg, stack);
				}
			}
			//backcopy this if present
			if(this.hasThisBound() && this.func.self.isReference()) {
				if(!this.func.hasThis())
					throw new CompileError("cannot call %s as a member of an object, it is not a nonstatic member;".formatted(func.name));
				Variable from = this.isInSelf? this.getTempSelf(p, c, s, stack) : this.func.self;
				Variable.directSet(p, s, this.getThisBound() , from, stack);
			}
			//set retval later
			
		} 
		
		
		public void requestTemplate(Scope s) throws CompileError {
			if(this.tempArgs!=null)this.func.requestTemplate(tempArgs, s);
		}
		public ResourceLocation getMyMCF() {
			return this.func.getMCF(this.tempArgs);
		}
		@Override
		public void getRet(PrintStream p,Compiler c,Scope s,RStack stack,int home, VarType typeWanted) throws CompileError {
			//TODO
			Variable ret = this.isInSelf ? this.getTempRet(p, c, s, stack) : this.func.returnV;
			if(this.hasTemplate())s.addTemplateConstsTemporarily(func, tempArgs);
			ret.getMe(p,s, stack,home, typeWanted);
			if(this.hasTemplate()) {
				//convert type;
				stack.setVarType(home, this.getRetType(s).breakTiesToTemplate(s).onStack(typeWanted));
				s.removeTemporaryConsts();
			}
			this.cleanupAfter(p, c, s, stack);
		}
		@Override
		public void getRet(PrintStream p,Compiler c,Scope s,Variable v,RStack stack) throws CompileError {
			Variable ret = this.isInSelf ? this.getTempRet(p, c, s, stack) : this.func.returnV;
			if(this.hasTemplate())s.addTemplateConstsTemporarily(func, tempArgs);
			Variable.directSet(p,s, v, ret, stack);
			if(this.hasTemplate())s.removeTemporaryConsts();
			this.cleanupAfter(p, c, s, stack);
		}
		@Override
		public void dumpRet(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError  {
			this.cleanupAfter(p, c, s, stack);
		}
		private void cleanupAfter(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError  {
			if(!this.isInSelf)this.func.deallocateLocalAfterCallOutside(p, s);
		}
		public Variable getRetConstRef() {
			return this.func.returnV;
		}
		@Override
		public Number getEstimate(Scope s) {
			return null;
		}
		@Override
		public VarType getRetType(Scope s) {
			return this.func.retype;
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
	public final boolean canRecurr;
	public final boolean isExtern;
	public final List<Variable> args=new ArrayList<Variable>();
	public final Map<String, Variable> locals=new HashMap<String, Variable>();
	public final Set<Variable> localFlowVars=new HashSet< Variable>();
	public final List<Const> localConsts=new ArrayList<Const>();
	public final Variable returnV;
	public final Variable self; //nullable
	private final Variable stackBackup; //nullable
	TemplateDefToken template=null;
	private List<TemplateArgsToken> requestedBinds=new ArrayList<TemplateArgsToken>();
	private final List<TemplateArgsToken> requestedBindsFilled=new ArrayList<TemplateArgsToken>();
	public Function(String name,VarType ret,VarType thisType,Keyword access, Compiler c,boolean canRecurr,boolean isExtern) throws CompileError{
		this.name=name;
		this.retype=ret;
		this.access=access;
		this.resoucrelocation=c.resourcelocation;
		this.canRecurr=canRecurr;//must be before vars
		this.isExtern=isExtern;
		
		this.returnV=new Variable("$return", ret, access, c).returnOf(this);
		if(thisType!=null)this.self = new Variable("$this", thisType, access, c).thisOf(this);
		else this.self=null;
		if(this.canRecurr)this.stackBackup = new Variable("$stack", StackStorage.STACKTYPE, access, c).stackVarOf(this);
		else this.stackBackup=null;
		this.mcf=Scope.getSubRes(c.resourcelocation, this);
	}
	public Function withArg(Variable var, Compiler c,boolean isRef)  throws CompileError{
		this.args.add(var.parameterOf(this,isRef));
		return this;
	}
	public Variable getArg(int i)  throws CompileError{
		return this.args.get(i);
	}
	public Function withLocalVar(Variable var, Compiler c,boolean isVolatile)  throws CompileError{
		if(var.isBasic()) var.localOf(this,isVolatile);
		this.locals.put(var.name,var);
		return this;
	}

	public Function withLocalFlowVar(Variable var, Compiler c,boolean add)  throws CompileError{
		//System.err.printf("%s() with local flow %s\n", this.name,var.holder + "." + var.getAddressToPrepend());
		if(var.isBasic()) var.localFlowOf(this);
		if(add) {
			boolean success=this.localFlowVars.add(var);
			if(!success) throw new CompileError("tried to double add flowvar %s.%s".formatted(var.holder,var.getAddressToPrepend()));
		}
		return this;
	}
	public void addConst(Const c) {
		this.localConsts.add(c);
	}
	private String altname = null;
	private boolean hasMask = false;
	public void setLocation(ResourceLocation path,String subname) {
		if(subname==null) this.mcf = path;
		else this.mcf=Scope.getSubRes(path, subname);
		this.resoucrelocation=path;
		this.altname=subname;
		this.hasMask=true;
		for(Variable b:this.args) {
			b.maskedFunction(this, subname);
		}
		this.returnV.maskedReturn(this, subname);
	}
	public static final String MACROPATH = "\"$macros\"";
	public boolean hasThis() {
		return this.self!=null;
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
	/**
	 * requests a new template to be made that is not in the default range;
	 * @param args
	 * @param s
	 * @return
	 * @throws CompileError
	 */
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
	
	public String toHeader() throws CompileError {
		String rcrs = this.canRecurr? Keyword.RECURSIVE.name+" ":"";
		String tmp = this.hasTemplate()? this.getTemplateInH()+" ":"";
		String thisdot = this.hasThis()? this.self.type.headerString()+".":"";
		boolean isFinal = this.hasThis()? !this.self.isReference() : false;
		String fnl = isFinal? " %s ".formatted(Keyword.FINAL.name):"";
		String mask = "";
		if(this.hasMask) {
			if(this.altname==null) mask = " -> %s".formatted(this.resoucrelocation);
			else mask = " -> %s.%s".formatted(this.resoucrelocation,this.altname);
		}
		String[] argss=new String[this.args.size()];
		for(int i=0;i<argss.length;i++)argss[i]=this.args.get(i).toHeader();
		return "%s%s%s %s%s (%s)%s%s".formatted(rcrs,tmp,this.retype.headerString(),thisdot,this.name,
				String.join(" , ", argss)
				,fnl,
				mask
				);
	}
	
	public static final String RET_TAG="\"$return\"";
	public static final String THIS_TAG="\"$this\"";
	public static final String STACK_TAG="\"$stack\"";


	public void collectFlowVars (Compiler c,Scope s) {
		
		//System.err.printf("collectFlowVars : %s\n", this.localFlowVars.stream().map(v ->v.holder + "."+v.getAddressToPrepend()).toList());

	}
	public void allocateMyLocalsLoad(PrintStream p, Namespace ns) throws CompileError {
		if(this.canRecurr) {
			//System.err.printf("allocateMyLocalsLoad, flowvars: %s\n", this.localFlowVars.stream().map(v ->v.holder + "."+v.getAddressToPrepend()).toList());
		}
		for(Variable arg:this.args)if(arg.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))arg.allocateLoad(p,ns.target, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
		if(this.returnV.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.returnV.allocateLoad(p,ns.target, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
		if(this.hasThis() &&this.self.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.self.allocateLoad(p,ns.target, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);

		for(Variable local:this.locals.values())if(local.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))local.allocateLoad(p,ns.target, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
		if(this.canRecurr &&this.stackBackup.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.stackBackup.allocateLoad(p,ns.target, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);

		if(this.canRecurr)for(Variable local:this.localFlowVars)if(local.willAllocateOnLoad(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))local.allocateLoad(p,ns.target, FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);

	}
	public void allocateMyLocalsCallOutside(PrintStream p, Scope s) throws CompileError {
		if(this.canRecurr) {
			//System.err.printf("allocateMyLocalsCallOutside, flowvars: %s\n", this.localFlowVars.stream().map(v ->v.holder + "."+v.getAddressToPrepend()).toList());
		}
		for(Variable arg:this.args)if(arg.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))arg.allocateCall(p, s.getTarget(), FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
		if(this.returnV.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.returnV.allocateCall(p, s.getTarget(), FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
		if(this.hasThis() &&this.self.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.self.allocateCall(p, s.getTarget(), FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);


	}
	public void allocateMyLocalsCallInside(PrintStream p, Scope s, Compiler c) throws CompileError {
		if(this.isExtern && this.args.size()>0) {
			RStack stack = s.getStackFor();
			VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_2), s.getTarget(), this.name, c);
			for(Variable arg: this.args) {
				//unpack macros
				String name = arg.name;
				Macro macro = new Macro(name,arg.type);
				arg.setMeToMacro(p, s, stack, macro);
			}
			stack.clear();stack.finish(c.namespace);
		}
		if(this.canRecurr) {
			//System.err.printf("allocateMyLocalsCallInside, flowvars: %s\n", this.localFlowVars.stream().map(v ->v.holder + "."+v.getAddressToPrepend()).toList());
		}
		for(Variable local:this.locals.values())if(local.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))local.allocateCall(p, s.getTarget(), FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);

		if(this.canRecurr &&this.stackBackup.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.stackBackup.allocateCall(p, s.getTarget(), FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);
		
		if(this.canRecurr)for(Variable local:this.localFlowVars)if(local.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))local.allocateCall(p, s.getTarget(), FileInterface.ALLOCATE_WITH_DEFAULT_VALUES);

	}
	public void deallocateLocalAfterCallOutside(PrintStream p, Scope s) throws CompileError {
		if(this.canRecurr) {
			//System.err.printf("deallocateAfterCallInside, flowvars: %s\n", this.localFlowVars.stream().map(v ->v.holder + "."+v.getAddressToPrepend()).toList());
		}
		for(Variable arg:this.args)if(arg.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))arg.deallocateAfterCall(p, s.getTarget());
		if(this.returnV.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.returnV.deallocateAfterCall(p, s.getTarget());
		if(this.hasThis() &&this.self.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.self.deallocateAfterCall(p, s.getTarget());

	}
	public void deallocateLocalAfterCallInside(PrintStream p, Compiler c, Scope s) throws CompileError {
		if(this.canRecurr) {
			//System.err.printf("deallocateAfterCallOutside, flowvars: %s\n", this.localFlowVars.stream().map(v ->v.holder + "."+v.getAddressToPrepend()).toList());
		}
		for(Variable local:this.locals.values())if(local.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))local.deallocateAfterCall(p, s.getTarget());
		if(this.canRecurr &&this.stackBackup.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))this.stackBackup.deallocateAfterCall(p, s.getTarget());
		
		if(this.canRecurr)for(Variable local:this.localFlowVars)if(local.willAllocateOnCall(FileInterface.ALLOCATE_WITH_DEFAULT_VALUES))local.deallocateAfterCall(p,s.getTarget());
		if(this.isExtern && !this.returnV.type.isVoid()) {
			VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_3_SNAP), s.getTarget(), this.name, c);
			RStack stack = s.getStackFor();
			this.returnV.trueReturnMe(p, s, null);
			stack.clear();stack.finish(c.namespace);
		}
	}
}
