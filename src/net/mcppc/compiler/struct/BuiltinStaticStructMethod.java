package net.mcppc.compiler.struct;



import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.errors.CompileError;

public abstract class BuiltinStaticStructMethod extends BuiltinFunction {
	public final VarType mytype;
	public BuiltinStaticStructMethod(String name,VarType type) {
		super(name);
		this.mytype=type;
	}

	public static class StdLibMethod extends BuiltinStaticStructMethod {
		Function myfunc;
		public StdLibMethod(String name, VarType type) {
			super(name, type);
			// TODO Auto-generated constructor stub
		}
		@Override
		public VarType getRetType(Args a) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public void call(PrintStream p, Compiler c, Scope s, Args args, RStack stack) throws CompileError {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, Args args, RStack stack, int stackstart)
				throws CompileError {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, Args args, Variable v, RStack stack)
				throws CompileError {
			// TODO Auto-generated method stub
			
		}
		@Override
		public Number getEstimate(Args args) {
			// TODO Auto-generated method stub
			return null;
		}
		
		
	}
}
