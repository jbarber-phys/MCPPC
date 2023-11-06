package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.NbtPath;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.TemplateArgsToken;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Type;
/**
 * an abstract call token for a random number getter
 * @author RadiumE13
 *
 */
/*
 * TODO:
 * (ignore random0: put it in stdlib)
 * make a BF for setting seed (make it a member of the instance
 */
public abstract class AbstractRandom extends BuiltinFunction{
	public static VarType DEFAULT_TYPE = VarType.INT;
	public static VarType DEFAULT_FLOAT_P = VarType.DOUBLE;
	
	public final AbstractRandom.SetSeed setSeed;
	public AbstractRandom(String name) {
		super(name);
		if(this.hasSeed()) {
			this.setSeed = new SetSeed("seed",this);
		}else this.setSeed= null;
	}
	protected abstract boolean hasSeed();
	VarType readType(BFCallToken token) {
		if(token.hasTemplate() ) {
			TemplateArgsToken tp = token.getTemplate();
			if(tp.values.size()!=1) return DEFAULT_TYPE;
			Token t = tp.values.get(0);
			if(t instanceof Type) {
				return ((Type) t).type;
			}else if (t instanceof Num) {
				int p = ((Num) t).value.intValue();
				try {
					return DEFAULT_FLOAT_P.withPrecision(p);
				} catch (CompileError e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return DEFAULT_TYPE;
	}
	@Override
	public VarType getRetType(BFCallToken token) {
		return this.readType(token);
	}

	@Override
	public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
			throws CompileError {
		BasicArgs args = BuiltinFunction.tokenizeArgsEquations(c, s, matcher, line, col, stack);
		return args;
	}
	public Equation getMin( BFCallToken token) {
		//null if absent
		BasicArgs args = (BasicArgs)token.getArgs();
		if(args.nargs()==2) {
			return (Equation)args.arg(0);
		}else if(args.nargs()==1) {
			return null;
		}else return null;
	}
	public Equation getMax( BFCallToken token) {
		//null if absent
		BasicArgs args = (BasicArgs)token.getArgs();
		if(args.nargs()==2) {
			return (Equation)args.arg(1);
		}else if(args.nargs()==1) {
			return (Equation)args.arg(0);
		}else return null;
	}

	@Override
	public Number getEstimate(BFCallToken token) {
		//TODO this needs a scope
		Equation max = this.getMax(token);
		if(max!=null) {
			//equations cannot be estimated beforehand (yet) if they are complex
			//if(max.isConstRefable()) return max.getConstVarRef(). ****
			
			return null;
		}
		VarType type = this.readType(token);
		if(type.isFloatP()) return 1.;
		else return Integer.MAX_VALUE;
	}
	
	public boolean hasLoad() {return true;}
	
	public void setSeed(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack,Equation toset) throws CompileError {
		Variable seed = this.getSeed(p, c, s, token, stack);
		if(seed==null)return;
		toset.compileOps(p, c, s, this.getRetType(token));
		toset.setVar(p, c, s, seed);
	}
	public static void uuidRandInt(PrintStream p,Scope s, RStack stack,Variable toset) throws CompileError {
		String mytag = "mcpp:random+uuid";
		Selector me = new Selector("@e", mytag,1);
		Variable uuid3 = new Variable("$uuid",VarType.INT,null,Mask.ENTITY,"","").maskEntity(me, NbtPath.UUID_LAST);
		p.printf("summon %s %s {Tags: [\"%s\"]}\n", "marker","0 0 0",mytag);
		Variable.directSet(p, s, toset, uuid3, stack);
		p.printf("kill %s\n", me.toCMD());
	}

	@Override public abstract void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError ;
	@Override public abstract void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart) throws CompileError ;
	@Override public abstract void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack);
	@Override public abstract void onLoad(PrintStream p,CompileJob job,Namespace ns) throws CompileError;
	public abstract Variable getSeed(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError;//nullable
	
	public static class SetSeed extends BuiltinFunction{
		private final AbstractRandom rand;
		public SetSeed(String name,AbstractRandom rand) {
			super(name);
			this.rand=rand;
		}
		@Override
		public VarType getRetType(BFCallToken token) {
			return VarType.VOID;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
				throws CompileError {
			return BuiltinFunction.tokenizeArgsEquations(c, s, matcher, line, col, stack);
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			BasicArgs args = (BasicArgs)token.getArgs();
			if(args.nargs()==0) {
				Variable seed = this.rand.getSeed(p, c, s, token, stack);
				if(seed==null)return;
				AbstractRandom.uuidRandInt(p, s, stack, seed);
				return;
			}
			this.rand.setSeed(p, c, s, token, stack, (Equation) args.arg(0));
		}

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


}
