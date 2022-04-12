package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.VarType.StructTypeParams;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Token;

/**
 * represents an object that is called like a function but it is able to interact directly with the compiler
 * is able to take unexpected expressions like target selectors and tag addresses as arguments
 * @author jbarb_t8a3esk
 *
 */
public abstract class BuiltinFunction {
	/*
	 * TODO: hypot, trigs, raycast
	 */
	public static class BFCallToken extends Token{
		public static BFCallToken make(Compiler c,Matcher m,int line, int col,String name) throws CompileError {
			BFCallToken t=new BFCallToken(line,col,BUILTIN_FUNCTIONS.get(name));
			t.args=t.f.tokenizeArgs(c, m, line, col);
			return t;
		}
		final BuiltinFunction f;
		Args args;
		public BFCallToken(int line, int col,BuiltinFunction f) {
			super(line, col);
			this.f=f;
		}
		@Override
		public String asString() {
			return "%s(...)".formatted(f.name);
		}
		public void call(PrintStream p, Compiler c, Scope s,RStack stack,int homeReg) {
			this.f.call(p, c, s, args,stack,homeReg);
		}
		public void getRet(PrintStream p, Compiler c, Scope s,RStack stack,int stackstart) {
			this.f.getRet(p, c, s, args, stack, stackstart);
		}
		public void getRet(PrintStream p, Compiler c, Scope s,Variable v,RStack stack) {
			this.f.getRet(p, c, s, args, v, stack);
		}
		public VarType getRetType() {
			return this.f.getRetType(args);
		}
		public Number getEstimate() {
			return this.f.getEstmate();
		}
		
	}
	public static interface Args{
		public static class Blank implements Args{public Blank(){}}
	}
	public static final Map<String,BuiltinFunction> BUILTIN_FUNCTIONS=new HashMap<String,BuiltinFunction>();
	public static boolean isBuiltinFunc(String name) {
		return BUILTIN_FUNCTIONS.containsKey(name);
	}
	public abstract VarType getRetType(Args a);
	public final String name;
	public BuiltinFunction(String name) {
		this.name=name;
	}
	/**
	 * tokenizes the type arguments, leaving cursor after the closing paren;
	 * can be used to take unexpected objects like tags + selectors as args (not allowed for normal functions)
	 * @param c
	 * @param matcher
	 * @param line
	 * @param col
	 * @return
	 */
	public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col)throws CompileError {
		//check that there are no args
		Token t=c.nextNonNullMatch(Factories.closeParen);
		if ((!(t instanceof Token.Paren)) || ((Token.Paren)t).forward)throw new CompileError.UnexpectedToken(t,"')'");
		else return new Args.Blank();
	}
	public abstract void call(PrintStream p, Compiler c, Scope s,Args args,RStack stack,int homeReg);
	public abstract void getRet(PrintStream p, Compiler c, Scope s,Args args,RStack stack,int stackstart);
	public abstract void getRet(PrintStream p, Compiler c, Scope s,Args args,Variable v,RStack stack);
	public abstract Number getEstmate();
}
