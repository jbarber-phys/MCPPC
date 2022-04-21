package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Equation;
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
	 * TODO: hypot, trigs, raycast,printf
	 */
	/*
	 * registration must be done here; standalone classes will never be loaded
	 */

	public static final Map<String,BuiltinFunction> BUILTIN_FUNCTIONS;
	static {
		BUILTIN_FUNCTIONS=new HashMap<String,BuiltinFunction>();
		PrintF.registerAll();
	}
	
	public static boolean isBuiltinFunc(String name) {
		return BUILTIN_FUNCTIONS.containsKey(name);
	}
	public static boolean register(BuiltinFunction func) {
		return BUILTIN_FUNCTIONS.put(func.name, func)==null;
	}
	public static class BFCallToken extends Token{
		public static BFCallToken make(Compiler c,Matcher m,int line, int col,String name) throws CompileError {
			BFCallToken t=new BFCallToken(line,col,BUILTIN_FUNCTIONS.get(name));
			t.args=t.f.tokenizeArgs(c, m, line, col);
			return t;
		}
		final BuiltinFunction f;public BuiltinFunction getBF(){
			return this.f;
		}
		Args args;
		public BFCallToken(int line, int col,BuiltinFunction f) {
			super(line, col);
			this.f=f;
		}
		@Override
		public String asString() {
			return "%s(...)".formatted(f.name);
		}
		public void call(PrintStream p, Compiler c, Scope s,RStack stack) throws CompileError {
			this.f.call(p, c, s, args,stack);
		}
		public void getRet(PrintStream p, Compiler c, Scope s,RStack stack,int stackstart) throws CompileError {
			this.f.getRet(p, c, s, args, stack, stackstart);
		}
		public void getRet(PrintStream p, Compiler c, Scope s,Variable v,RStack stack) throws CompileError {
			this.f.getRet(p, c, s, args, v, stack);
		}
		public VarType getRetType() {
			return this.f.getRetType(args);
		}
		public Number getEstimate() {
			return this.f.getEstimate();
		}
		
	}
	public static interface Args{
		//public static class Blank implements Args{public Blank(){}}
	}
	public static class BasicArgs implements Args{
		public final List<Token> targs=new ArrayList<Token>();
		public BasicArgs(){
		}
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
	public abstract Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col)throws CompileError ;
	
	public abstract void call(PrintStream p, Compiler c, Scope s,Args args,RStack stack) throws CompileError;
	public abstract void getRet(PrintStream p, Compiler c, Scope s,Args args,RStack stack,int stackstart) throws CompileError;
	public abstract void getRet(PrintStream p, Compiler c, Scope s,Args args,Variable v,RStack stack) throws CompileError;
	public abstract Number getEstimate();
	
	/**
	 * serializes argis in a basic way; if t
	 * @param c
	 * @param matcher
	 * @param line
	 * @param col
	 * @param looks lists to look for each token; if null, will find next Equation
	 * @param endEarly
	 * @return
	 * @throws CompileError
	 */
	public static final Args tokenizeArgsBasic(Compiler c, Matcher matcher, int line, int col,List<Token.Factory[]> looks,boolean endEarly)throws CompileError {
		BasicArgs args=new BasicArgs();
		int index=0;
		for(Token.Factory[] look:looks) {
			Token t=look!=null?
					c.nextNonNullMatch(look)
					:
					Equation.toArgue(c.line, c.column(), c, matcher).populate(c, matcher);
			args.targs.add(t);
			if(t instanceof Equation) {
				//TODO test this to make sure it works with eqs
				switch (((Equation)t).end) {
				case ARGSEP:
					break;//keep going
				case CLOSEPAREN:
					throw new CompileError("unexpected equation end paren in builtin function (not enough args);");
				default:
					throw new CompileError("unexpected equation end in builtin function;");
				
				}
			}else {
				Token sep=c.nextNonNullMatch(Factories.argsepOrParen);
				if(sep instanceof Token.Paren) {
					if(((Token.Paren)sep).forward)throw new CompileError.UnexpectedToken(sep,"')'");
					if(index!=looks.size()-1 && !endEarly)throw new CompileError.UnexpectedToken(sep,"more args");
					return args;
				}else if(sep instanceof Token.ArgEnd) {
					//carry on
				}else {
					throw new CompileError.UnexpectedToken(t,"')' or ','");
				}
			}
			index++;
			
		}
		throw new CompileError("too many args in builtin function;");
	}
	public static boolean findArgsep(Compiler c) throws CompileError {
		Token sep=c.nextNonNullMatch(Factories.argsepOrParen);
		if(sep instanceof Token.Paren) {
			if(((Token.Paren)sep).forward)throw new CompileError.UnexpectedToken(sep,"')', ','");
			return false;
		}else if(sep instanceof Token.ArgEnd) {
			return true;
		}else {
			throw new CompileError.UnexpectedToken(sep,"')' or ','");
		}
	}
}
