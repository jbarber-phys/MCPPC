package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction.Args;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Function.FuncCallToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.AbstractCallToken;
import net.mcppc.compiler.functions.PrintCode;
import net.mcppc.compiler.functions.PrintF;
import net.mcppc.compiler.functions.Tp;
import net.mcppc.compiler.functions.UnsafeThreadCommand;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.tokens.Bool;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.TemplateArgsToken;
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
		registerAll();
		PrintF.registerAll();
		Tp.registerAll();
		PrintCode.registerAll();
		UnsafeThreadCommand.registerAll();
	}
	private static void registerAll() {
		register(SetScopeFlag.stopLongMult);
		register(SetScopeFlag.debug);
	}
	
	private static boolean isBuiltinFunc(List<String> names,Compiler c,Scope s) {
		return BuiltinFunction.getBuiltinFunc(names, c, s)!=null;
	}

	public static BuiltinFunction getBuiltinFunc(List<String> names,Compiler c,Scope s) {
		//return null if absent
		BuiltinFunction bf=null;
		bf = BUILTIN_FUNCTIONS.get(names.get(0));
		if(bf!=null || names.size()<=1)return bf;
		List<String> subnames = names.subList(0, names.size()-1);String mname=names.get(names.size()-1);
		//System.err.printf("check for var methods\n");
		Variable v;
		try {
			v=c.myInterface.identifyVariable(subnames, s);
		} catch (CompileError e) {
			//System.err.printf("v=null\n");
			v=null;
		}
		if(v!=null && v.type.isStruct()) {
			Struct st=v.type.struct;
			if(st.hasBuiltinMethod(v, mname)){
				try {
					bf=st.getBuiltinMethod(v, mname);
				} catch (CompileError e) {
					//leave null
				}
			}
		}
		
		return bf;
	}
	public static boolean register(BuiltinFunction func) {
		return BUILTIN_FUNCTIONS.put(func.name, func)==null;
	}
	public static boolean alias(BuiltinFunction func,String alias) {
		return BUILTIN_FUNCTIONS.put(alias, func)==null;
	}

	public static String[] getFuncNames() {
		return BUILTIN_FUNCTIONS.keySet().toArray(new String[BUILTIN_FUNCTIONS.keySet().size()]);
	}
	public static class BFCallToken extends AbstractCallToken{
		@Deprecated
		private static BFCallToken make(Compiler c,Scope s,Matcher m,int line, int col,RStack stack,List<String> names) throws CompileError {
			BFCallToken t=new BFCallToken(line,col,BuiltinFunction.getBuiltinFunc(names, c, c.currentScope));
			t.args=t.f.tokenizeArgs(c, s, m, line,col, stack);
			return t;
		}
		public static BFCallToken make(Compiler c,Scope s,Matcher m, int line,int col,RStack stack, BuiltinFunction func) throws CompileError {
			BFCallToken t=new BFCallToken(line,col,func);
			t.args=t.f.tokenizeArgs(c, s, m, line,col, stack);
			return t;
		}
		final BuiltinFunction f;public BuiltinFunction getBF(){
			return this.f;
		}
		Args args;
		private TemplateArgsToken tempArgs=null;
		public BFCallToken(int line, int col,BuiltinFunction f) {
			super(line, col);
			this.f=f;
		}
		@Override
		public String asString() {
			String ths=this.hasThisBound()?this.getThisBound().name+".":"";
			return "%s%s(...)".formatted(ths,f.name);
		}
		@Override
		public void call(PrintStream p, Compiler c, Scope s,RStack stack) throws CompileError {
			this.f.call(p, c, s, this,stack);
		}
		@Override
		public void getRet(PrintStream p, Compiler c, Scope s,RStack stack,int stackstart) throws CompileError {
			this.f.getRet(p, c, s, this, stack, stackstart);
		}
		@Override
		public void getRet(PrintStream p, Compiler c, Scope s,Variable v,RStack stack) throws CompileError {
			this.f.getRet(p, c, s, this, v, stack);
		}
		@Override
		public VarType getRetType() {
			return this.f.getRetType(this);
		}
		@Override
		public Number getEstimate() {
			return this.f.getEstimate(this);
		}
		@Override
		public boolean hasTemplate() {
			return this.tempArgs!=null;
		}
		@Override
		public BFCallToken withTemplate(TemplateArgsToken tgs) {
			this.tempArgs=tgs;return this;
		}
		// for calls to (obj of type ...<args1...>).func<args2...>(); if converted, the total template is <args1...,args2...>
		public BFCallToken prependTemplate(TemplateArgsToken tgs) {
			if(this.tempArgs==null)this.tempArgs=tgs;
			else if(tgs!=null){
				this.tempArgs.values.addAll(0, tgs.values);
			}
			
			return this;
		}
		public TemplateArgsToken getTemplate() {
			return this.tempArgs;
		}
		public boolean canConvert() {
			return this.getBF().canConvert(this);
		}
		public Token convert(Compiler c, Scope s, RStack stack) throws CompileError{
			return this.getBF().convert(this, c, s, stack);
		}
		public Args getArgs() {
			return this.args;
		}
		public void withArgs(Args args) {
			this.args=args;
		}
		@Override
		public void dumpRet(PrintStream p, Compiler c, Scope s, RStack stack) throws CompileError {
			// do nothing
			this.getBF().dumpRet(p, c, s, this, stack);
		}
		
	}
	public static interface Args{
		//public static class Blank implements Args{public Blank(){}}
	}
	public static class BasicArgs implements Args{
		private final List<Token> targs=new ArrayList<Token>();
		//optional
		private final Map<String,Integer> argmap=new HashMap<String, Integer>();
		public Token arg(int index){
			if(index<targs.size() && index>=0)return this.targs.get(index);
			else return null;
		}
		public Token arg(String key){
			Integer index=this.argmap.get(key);
			if(index==null)return null;
			else return this.arg(index);
		}
		public void add(String key,Token token) {
			int index=this.targs.size();
			this.argmap.put(key, index);
			this.targs.add(token);
		}
		public boolean has(int i) {
			return this.targs.size()>i && i>=0;
		}
		public boolean has(String key) {
			return this.argmap.containsKey(key);
		}
		public void add(Token token) {
			this.targs.add(token);
		}
		public int nargs() {return this.targs.size();}
		public BasicArgs(){
		}
		public BasicArgs equations(Compiler c,Scope s,int line,int col,Matcher m, RStack stack) throws CompileError {
			Function.FuncCallToken.addArgs(c, s, line, col, m, stack, this.targs);
			return this;
		}
		public boolean isEmpty() {
			return this.targs.isEmpty();
		}
	}
	public abstract VarType getRetType(BFCallToken token);
	public final String name;
	public boolean isNonstaticMember() {
		return false;
	}
	public BuiltinFunction(String name) {
		this.name=name;
	}
	/**
	 * tokenizes the type arguments, leaving cursor after the closing paren;
	 * can be used to take unexpected objects like tags + selectors as args (not allowed for normal functions)
	 * @param c
	 * @param s TODO
	 * @param matcher
	 * @param line
	 * @param col
	 * @return
	 */
	public abstract Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line,int col, RStack stack)throws CompileError ;
	
	public abstract void call(PrintStream p, Compiler c, Scope s,BFCallToken token,RStack stack) throws CompileError;
	public abstract void getRet(PrintStream p, Compiler c, Scope s,BFCallToken token,RStack stack,int stackstart) throws CompileError;
	public abstract void getRet(PrintStream p, Compiler c, Scope s,BFCallToken token,Variable v,RStack stack) throws CompileError;
	public void dumpRet(PrintStream p,Compiler c,Scope s, BFCallToken token,RStack stack) throws CompileError  {
		//default to doing nothing
	}
	public abstract Number getEstimate(BFCallToken token);
	
	
	
	/**
	 * serializes argis in a basic way; if t
	 * @param c
	 * @param s TODO
	 * @param matcher
	 * @param line
	 * @param col
	 * @param looks lists to look for each token; if null, will find next Equation
	 * @param endEarly
	 * @return
	 * @throws CompileError
	 */
	public static final Args tokenizeArgsBasic(Compiler c, Scope s, Matcher matcher, int line,int col,List<Token.Factory[]> looks, boolean endEarly)throws CompileError {
		BasicArgs args=new BasicArgs();
		int index=0;
		for(Token.Factory[] look:looks) {
			Token t=look!=null?
					c.nextNonNullMatch(look)
					:
					Equation.toArgue(c.line, c.column(), c, matcher, s);//.populate(c, matcher);
			args.add(t);
			if(t instanceof Equation) {
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
	public static final BasicArgs tokenizeArgsEquations(Compiler c, Scope s, Matcher matcher, int line,int col, RStack stack)throws CompileError {
		BasicArgs args = new BasicArgs();
		args.equations(c, s, line, col, matcher, stack);
		return args;
		
	}
	public static final BasicArgs fromEquations(Equation... eqs) {
		BasicArgs args = new BasicArgs();
		for(Equation eq:eqs)args.targs.add(eq);
		return args;
		
	}
	public static final Args tokenizeArgsConsts(Compiler c, Matcher matcher, int line, int col,List<Const.ConstType> types,boolean endEarly)throws CompileError {
		if(types.size()==0)return BuiltinFunction.tokenizeArgsNone(c, matcher, line, col);
		BasicArgs args=new BasicArgs();
		for(Const.ConstType ct:types) {
			ConstExprToken t=Const.checkForExpressionSafe(c, c.currentScope, matcher, line, col, ct);
			if(t==null && !endEarly)throw new CompileError.UnexpectedToken(t,"more args");
			args.add(t);
			if(findArgsep(c)) {
				continue;
			}else {
				//warn of more args
				return args;
			}
			
		}
		
		throw new CompileError("too many args in builtin function;");
	}
	public static final Args tokenizeArgsNone(Compiler c, Matcher matcher, int line, int col)throws CompileError {
		BasicArgs args=new BasicArgs();
		if(findArgsep(c)) {
			throw new CompileError("too many args in builtin function;");
		}else {
			return args;
		}
	}
	/**
	 * passes over next open paren
	 * @param c
	 * @throws CompileError
	 */
	public static void open(Compiler c) throws CompileError {
		Token sep=c.nextNonNullMatch(Factories.argsepOrParen);
		if(sep instanceof Token.Paren) {
			if(!((Token.Paren)sep).forward)throw new CompileError.UnexpectedToken(sep,"')', ','");
			return;
		}else {
			throw new CompileError.UnexpectedToken(sep,"')' or ','");
		}
	}
	public static boolean openIf(Compiler c) throws CompileError {
		Token sep=c.nextNonNullMatch(Factories.checkForParen);
		if(sep instanceof Token.Paren) {
			if(!((Token.Paren)sep).forward)return false;
			return true;
		}else {
			return false;
		}
	}
	/**
	 * passes over next closeparen or comma; return
	 * @param c
	 * @return true if it is a comma, or false if it is a close paren (true if continue)
	 * @throws CompileError
	 */
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

	public boolean canConvert(BFCallToken token) {
		return false;
	}
	public Token convert(BFCallToken token, Compiler c, Scope s, RStack stack) throws CompileError{
		throw new CompileError("cannot convert %s to mcf;".formatted(token.getBF().name));
	}
	public void printStatementTree(PrintStream p,BFCallToken token,int tabs) {
		//for debuging
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		p.printf("%s... %s(...);\n",s.toString(), this.name);
	}
	public static class SetScopeFlag extends BuiltinFunction {
		@FunctionalInterface
		private static interface ScopeFlagSet {
			public void set(Scope s,boolean flag);
		}
		public static final SetScopeFlag stopLongMult = new SetScopeFlag("stopLongMult", (s,b)->s.setProhibitLongMult(b),true);
		public static final SetScopeFlag debug = new SetScopeFlag("debug", (s,b)->s.setDebugMode(b),true);
		private final ScopeFlagSet setter;
		private final boolean defaultFlag;
		public SetScopeFlag(String name,ScopeFlagSet setter,boolean defaultFlag) {
			super(name);
			this.setter=setter;
			this.defaultFlag=defaultFlag;
		}
		public SetScopeFlag(String name,ScopeFlagSet setter) {
			this(name,setter,true);
		}

		@Override
		public VarType getRetType(BFCallToken token) {
			return VarType.VOID;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			List<Const.ConstType> atps=new ArrayList<Const.ConstType>();
			atps.add(ConstType.BOOLIT);
			return BuiltinFunction.tokenizeArgsConsts(c, matcher, line, col, atps, true);
			//TODO allow a precompiled condition dependante on templates;
			//example: (precision<=4)
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			boolean b;
			 Args args = token.args;
			if(((BasicArgs)args).isEmpty() || ((BasicArgs)args).arg(0)==null )b=true;
			else{
				Bool t = (Bool) ((BasicArgs)args).arg(0);
				b=t.val;
			}
			//s.setProhibitLongMult(b);//edit the scope
			this.setter.set(s, b);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {//void
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			// void
		}

		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
	public class SingleLine extends BuiltinFunction {
		public final VarType rtype;
		public final String cmd;
		public SingleLine(String name,VarType retype,String cmd) {
			super(name);
			this.rtype=retype;
			this.cmd=cmd;
			assert !this.rtype.isFloatP();
		}

		@Override
		public VarType getRetType(BFCallToken token) {
			return this.rtype;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			return BuiltinFunction.tokenizeArgsNone(c, matcher, line, col);
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			if(this.rtype.isVoid())p.printf("%s\n", this.cmd);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			if(this.rtype.isVoid())return;
			Register r=stack.getRegister(stackstart);
			stack.setVarType(stackstart, this.rtype);
			if(this.rtype.isLogical())  r.setToSuccess(p, cmd);
			else if(this.rtype.isNumeric())  r.setToResult(p,cmd);
			
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			if(this.rtype.isVoid())return;
			int home = stack.setNext(this.rtype);
			this.getRet(p, c, s, token, stack, home);
			stack.pop();
			
		}

		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
}
