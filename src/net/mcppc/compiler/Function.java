package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Identifiable;
import net.mcppc.compiler.tokens.Keyword;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Equation.End;
import net.mcppc.compiler.tokens.Token.BasicName;
import net.mcppc.compiler.tokens.Token.Factory;
import net.mcppc.compiler.tokens.Token.MemberName;

/**
 * declaration of a function;
 * 
 * TODO allow args to be declared "ref" to back-copy after function call is over
 * @author jbarb_t8a3esk
 *
 */
public class Function {
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
		Function func; public Function getFunction() {return this.func;}
		public FuncCallToken(int line, int col,MemberName fname) {
			super(line, col);
			this.names=fname.names;
		}
		public FuncCallToken addArg(Equation arg) {
			this.args.add(arg);return this;
		}
		@Override public String asString() {
			return "%s(%s)".formatted(String.join(".", names),"(...)");
		}
		@Override
		public int identify(Compiler c,Scope s) throws CompileError {
			this.func=c.myInterface.identifyFunction(this);
			//TODO recursion warnings
			if(this.func.args.size()!=this.args.size())throw new CompileError("wrong number of args, %d,  in function %s which takes %d args"
					.formatted(this.args.size(),this.func.args.size(),this.func.name));
			return 0;
		}

		public void call(PrintStream p,Compiler c,Scope s,RStack stack) throws CompileError {
			//todo call this.func.mcf
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
			p.printf("function %s\n", this.func.mcf);
			
			for(int i=0;i<this.func.args.size();i++) {
				Variable arg=this.func.args.get(i);
				if(arg.isReference()) {
					//back copy after func call
					 Variable ref=((Equation)this.args.get(i)).getVarRef();
					 Variable.directSet(p, ref, arg, stack);
				}
			}
			//set retval later
			
		} 
		public void getRet(PrintStream p,Compiler c,Scope s,RStack stack,int home) throws CompileError {
			this.func.returnV.getMe(p, stack,home);
		}
		public void getRet(PrintStream p,Compiler c,Scope s,Variable v,RStack stack) throws CompileError {
			Variable.directSet(p, v, this.func.returnV, stack);
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
	public ResourceLocation mcf;
	public final VarType retype;
	public final Keyword access;
	public final List<Variable> args=new ArrayList<Variable>();
	public final Variable returnV;
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
	public ResourceLocation getResoucrelocation() {return this.resoucrelocation;}
	public void setResourceLocation(ResourceLocation path) {
		this.resoucrelocation=path;
		for(Variable b:this.args) {
			b.holder=path.toString();
		}
	}
	
	public String toHeader() throws CompileError {
		String[] argss=new String[this.args.size()];
		for(int i=0;i<argss.length;i++)argss[i]=this.args.get(i).toHeader();
		return "%s %s (%s)".formatted(this.retype.headerString(),this.name,
				String.join(" , ", argss)
				);
	}
	
	public static final String RET_TAG="\"$return\"";
	@Deprecated public String returnDataPhrase() {
		//use this.
		return "storage %s %s.%s".formatted(this.resoucrelocation,this.name,RET_TAG);
	}
}
