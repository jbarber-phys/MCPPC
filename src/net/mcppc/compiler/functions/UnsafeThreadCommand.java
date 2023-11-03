package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.McThread;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Num;
/**
 * functions that are not memory safe inside threads with uuid lookup tables for local vars
 * @author RadiumE13
 *
 */
public abstract class UnsafeThreadCommand extends BuiltinFunction {
	public static boolean RETURN_EXISTS = false;//TODO target this java 1.20.3
	
	public static void registerAll() {
		BuiltinFunction.register(kill);
		if(RETURN_EXISTS) BuiltinFunction.register(returnGuard);
	}
	public static final UnsafeThreadCommand kill = new UnsafeThreadCommand("kill",1) {

		@Override
		public boolean doFinalize(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			BasicArgs args = (BasicArgs) token.getArgs();
			if(args.nargs()<1) return true;
			Equation arg = (Equation) args.arg(0);
			arg.constify(c, s);
			if(!arg.isConstable()) arg.throwNotConstError();
			ConstExprToken cst = arg.getConst();
			if(cst.constType()!=ConstType.SELECTOR) throw new CompileError("arg of kill must be selector-equivalent");
			Selector sf = ((Selector.SelectorToken)cst).selector();
			return s.getThread().isSelectorMySelf(sf);
		}

		@Override
		public void actualCall(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			BasicArgs args = (BasicArgs) token.getArgs();
			Selector sf = null;
			if(args.nargs()<1) {
				//skip
			}else {
				Equation arg = (Equation) args.arg(0);
				arg.constify(c, s);
				if(!arg.isConstable()) arg.throwNotConstError();
				ConstExprToken cst = arg.getConst();
				if(cst.constType()!=ConstType.SELECTOR) throw new CompileError("arg of kill must be selector-equivalent");
				sf = ((Selector.SelectorToken)cst).selector();
			}
			if(sf==null) p.printf("kill\n");
			else p.printf("kill %s\n",sf.toCMD());
		}
		
	};
	public static final UnsafeThreadCommand returnGuard = new UnsafeThreadCommand("return",1) { //may have to alter name

		@Override
		public boolean doFinalize(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) {
			return true;
		}

		@Override
		public void actualCall(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			Warnings.warning("Warning: function return(...) should not be used as mcpp might use '/return' it for its own thing", c);
			BasicArgs args = (BasicArgs) token.getArgs();
			if(args.nargs()<1) {
				p.printf("return 1\n");
				return;
			}else {
				Equation arg = (Equation) args.arg(0);
				arg.constify(c, s);
				if(!arg.isConstable()) {
					arg.compileOps(p, c, s, VarType.INT);
					int i=arg.setReg(p, c, s, VarType.INT);
					Register ret = stack.getRegister(i);
					stack.clear();stack.finish(c.namespace);
					p.printf("return run  ");ret.getValueCmd(p);
					return;
				}
				ConstExprToken cst = arg.getConst();
				if(cst.constType()!=ConstType.NUM) throw new CompileError("arg of kill must be selector-equivalent");
				int ret = ((Num)cst).value.intValue();
				p.printf("return %d\n",ret);
			}
			
		}
		
	};
	
	private final int maxargs;
	public UnsafeThreadCommand(String name,int maxArgs) {
		super(name);
		this.maxargs = maxArgs;
	}

	@Override
	public VarType getRetType(BFCallToken token) {
		return VarType.VOID;
	}

	@Override
	public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
			throws CompileError {
		BasicArgs basic = BuiltinFunction.tokenizeArgsEquations(c, s, matcher, line, col, stack);
		if (basic.nargs()> maxargs) throw new CompileError("too many args (%d) given to function '%s'".formatted(basic.nargs(),this.name));
		return basic;
	}

	@Override
	public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		if(s.hasThread() && s.getThread().hasLookup()) {
			if(this.doFinalize(p,c,s,token,stack))s.getThread().finalizeLookup(p, c, s, stack);
		}
		this.actualCall(p, c, s, token, stack);

	}
	public abstract boolean doFinalize(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError ;
	public abstract void actualCall(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError ;

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
			throws CompileError {
	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
			throws CompileError {
	}

	@Override
	public Number getEstimate(BFCallToken token) {
		return null;
	}

}
