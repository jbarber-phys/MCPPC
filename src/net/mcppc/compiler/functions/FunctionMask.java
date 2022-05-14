package net.mcppc.compiler.functions;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.FileInterface;
import net.mcppc.compiler.Function;
import net.mcppc.compiler.Function.FuncCallToken;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.FunctionMask.MCFArgs;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Token;

public class FunctionMask extends BuiltinFunction {
	public static class MCFArgs implements Args{
		public final List<Equation> args=new ArrayList<Equation>();
		public MCFArgs (){
		}
		public void add(Function.FuncCallToken ft) {
			ft.args.addAll(args);
		}
	}
	final ResourceLocation lib;
	final String fname;
	final boolean isNonstaticMethod;
	public List<Token> defaultArgs=null;
	public FunctionMask(String name,ResourceLocation lib,String fname,boolean isNonstaticMethod,List<Token> defaultArgs) {
		super(name);
		this.lib=lib;
		this.fname=fname;
		this.defaultArgs=defaultArgs;
		this.isNonstaticMethod=isNonstaticMethod;
	}
	public FunctionMask(String name,ResourceLocation lib,String fname,boolean isNonstaticMethod) {
		this(name,lib,fname,isNonstaticMethod,null);
	}
	public FunctionMask(String name,ResourceLocation lib,String fname,List<Token> defaultArgs) {
		this(name,lib,fname,false,defaultArgs);
	}
	public FunctionMask(String name,ResourceLocation lib,String fname) {
		this(name,lib,fname,false,null);
	}

	public boolean isNonstaticMember() {
		return this.isNonstaticMethod;
	}
	@Override
	public VarType getRetType(BFCallToken token) {
		// should never be called
		return null;
	}

	public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
		//default
		MCFArgs args=new MCFArgs();
		Function.FuncCallToken.addArgs(c, line, col, matcher, stack, args.args);
		if(this.defaultArgs!=null && this.defaultArgs.size()>args.args.size()) for(int i=args.args.size();i<this.defaultArgs.size();i++) {
			Token deft=this.defaultArgs.get(i);
			if(deft!=null && !(deft instanceof Token.NullArgDefault)){
				Equation eq = Equation.toArgueHusk(stack, this.defaultArgs.get(i));
				if(eq!=null)
					args.args.add(i, eq);
			}
		}
			
		return args;
	}

	@Override
	public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		// should never be called
		throw new CompileError("unwanted call to FunctionMask");

	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
			throws CompileError {
		throw new CompileError("unwanted call to FunctionMask");

	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
			throws CompileError {
		throw new CompileError("unwanted call to FunctionMask");

	}

	@Override
	public Number getEstimate(BFCallToken token) {
		// TODO should never be called
		return null;
	}

	@Override
	public boolean canConvert(BFCallToken token) {
		return true;
	}

	@Override
	public FuncCallToken convert(BFCallToken token, Compiler c, Scope s, RStack stack) throws CompileError {
		FileInterface fi;
		try {
			fi= c.job.getFileInterfaceFor(lib, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new CompileError("fileNotFoundException");
		} 
		Function f=fi.identifyFunction(fname, s);
		Function.FuncCallToken ft= new Function.FuncCallToken(token.line, token.col, this.fname,f) ;
		ft.withThis(token.getThisBound());
		ft.withTemplate(token.getTemplate());
		((MCFArgs) token.getArgs()).add(ft);
		//System.err.printf("converted %s -> %s\n", token.asString(),ft.asString());
		return ft;
	}
	

}
