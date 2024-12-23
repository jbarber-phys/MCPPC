package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.NbtPath;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.BuiltinFunction.BasicArgs;
import net.mcppc.compiler.CMath;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.COption;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.functions.BFReturnVoid;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.target.Version;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Bool;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.TemplateArgsToken;
import net.mcppc.compiler.tokens.Type;
/**
 * struct representing a random number generator;
 * abstraction of a sequenceId;
 * also contains static functions for using a common generator;
 * requires java 1.20.2 or newer;
 * 
 * @author RadiumE13
 *
 */

public class McRandom extends Struct {
	public static final McRandom random = new McRandom("Random");
	public static void registerAll() {
		Struct.register(random);
	}
	public McRandom(String name) {
		super(name);
	}
	private final Map<String,BuiltinFunction> bfs = Map.of(
			Uniform.uniform.name,Uniform.uniform,
			SetSeed.setSeed.name,SetSeed.setSeed
			);
	private final Map<String,BuiltinFunction> staticBfs = Map.of(
			Uniform.uniformStatic.name,Uniform.uniformStatic,
			SetSeed.setSeedStatic.name,SetSeed.setSeedStatic
			);
	public String sequenceId(Variable me) {
		return me.getHolder();
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return null;
	}

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		throw new CompileError("type %s has no precision".formatted(this.name));
	}

	@Override
	public String getPrecisionStr(VarType mytype) {
		return null;
	}

	@Override
	public String getJsonTextFor(Variable variable) throws CompileError {
		throw new CompileError("Random var has no json element");
	}

	@Override
	public int sizeOf(VarType mytype) {
		return 0;
	}

	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted)
			throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}

	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}

	@Override
	public void allocateLoad(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue)
			throws CompileError {
	}

	@Override
	public void allocateCall(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue)
			throws CompileError {
	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		return null;
	}

	@Override
	public boolean hasField(Variable self, String name) {
		return false;
	}

	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}

	
	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		return super.hasBuiltinMethodBasic(name, bfs);
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return super.getBuiltinMethodBasic(self, name, bfs);
	}
	@Override
	public Variable varInit(Variable v, Compiler c, Scope s) throws CompileError {
		//VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_2), s.getTarget(), this.name, c);//not yet, could be compiling conditionally
		String path = c.resourcelocation.path + "__/" + v.name;
		NbtPath address = new NbtPath("");
		return v.maskStorageAllocatable(new ResourceLocation(c.resourcelocation.namespace,path),address );
	}
	@Override
	public boolean hasStaticBuiltinMethod(String name) {
		return super.hasStaticBuiltinMethodBasic(name,staticBfs);
	}
	@Override
	public BuiltinFunction getStaticBuiltinMethod(String name, VarType type) throws CompileError {
		return super.getStaticBuiltinMethodBasic(name, type,staticBfs);
	}
	@Override
	public boolean isDataEquivalent(VarType type) {
		return false;
	}
	@Override
	public boolean canMask(VarType mytype, Mask mask) {
		return mask==Mask.STORAGE;//will ignore the path
	}
	@Override
	public boolean canCompareTags(VarType type, VarType otherType) {
		return false;
	}
	@Override
	public boolean canBeRecursive(VarType type) {
		return false;
	}
	public static class Uniform extends BuiltinFunction{
		public static final Uniform uniform = new Uniform("uniform",false);
		public static final Uniform uniformStatic = new Uniform("uniform",true);
		public static VarType DEFAULT_TYPE = VarType.INT;
		private final boolean isStatic;
		public Uniform(String name,boolean isStatic) {
			super(name);
			this.isStatic= isStatic;
		}

		@Override
		public VarType getRetType(BFCallToken token, Scope s) throws CompileError{
			TemplateArgsToken temp = token.getTemplate();
			if(temp ==null || temp.size()<1) return DEFAULT_TYPE;
			ConstExprToken tp = temp.getArg(0);
			if(tp.constType()!=ConstType.TYPE) 
				throw new CompileError.WrongConstType("%s template".formatted(this.name), ConstType.TYPE, tp);
			return ((Type) tp).type;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
				throws CompileError {
			return BuiltinFunction.tokenizeArgsEquations(c, s, matcher, line, col, stack);
		}
		public Equation getMin( BFCallToken token,Scope s,RStack stack) throws CompileError {
			BasicArgs args = (BasicArgs)token.getArgs();
			VarType type = this.getRetType(token, s);
			if(type.isLogical()) return Equation.toArgueHusk(stack, new Bool(token.line,token.col,false));
			if(args.nargs()==2) {
				return (Equation)args.arg(0);
			}else if(args.nargs()==1) {
				if(type.isNumeric()) return Equation.toArgueHusk(stack, new Num(token.line,token.col,0,type));
				else throw new CompileError("type arg in %s(range...) must be numeric;".formatted(this.name));
			}else {
				if(type.isFloatP()) return Equation.toArgueHusk(stack, new Num(token.line,token.col,0,type));
				else return Equation.toArgueHusk(stack, new Num(token.line,token.col,Integer.MIN_VALUE,type));
			}
		}
		public Equation getMax( BFCallToken token,Scope s,RStack stack) throws CompileError {
			BasicArgs args = (BasicArgs)token.getArgs();
			VarType type = this.getRetType(token, s);
			if(type.isLogical()) return Equation.toArgueHusk(stack, new Bool(token.line,token.col,true));
			if(args.nargs()==2) {
				return (Equation)args.arg(1);
			}else if(args.nargs()==1) {
				return (Equation)args.arg(0);
			}else {
				if(type.isFloatP()) return Equation.toArgueHusk(stack, new Num(token.line,token.col,1.0,type));
				else return Equation.toArgueHusk(stack, new Num(token.line,token.col,Integer.MAX_VALUE,type));
			}
		}
		@Override
		@Targeted
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_2), s.getTarget(), this.name, c);
			BasicArgs args = (BasicArgs)token.getArgs();
			if(args.nargs()>2) throw new CompileError.WrongArgNumber(token, name, "2 or less", args.nargs());
			Equation min = this.getMin(token, s, stack);
			Equation max = this.getMax(token, s, stack);
			min.constify(c, s);
			max.constify(c, s);
			VarType retype = this.getRetType(token, s);
			String seq = "";
			if(!this.isStatic) {
				Variable self = token.getThisBound();
				VarType randType = self.type;
				McRandom struct = (McRandom) randType.struct;
				seq = " %s".formatted(struct.sequenceId(self));
			}
			if(retype.isLogical()) {
				int hm = stack.setNext(retype);
				Register r=stack.getRegister(hm);
				r.setToResult(p, "random value 0..1%s".formatted(seq));
				return;
			}
			else if(min.isConstable() && max.isConstable()) {
				ConstExprToken cmin = min.getConst();
				ConstExprToken cmax = max.getConst();
				int pc = retype.getPrecision(s);
				int imin,imax;
				if(retype.isFloatP()) {
					imin = (int) (((Num)cmin).value.doubleValue() * Math.pow(10, pc));
					imax = (int) (((Num)cmax).value.doubleValue() * Math.pow(10, pc));
				} else {
					imin = ((Num)cmin).value.intValue();
					imax = ((Num)cmax).value.intValue();
				}
				int hm = stack.setNext(retype);
				Register r=stack.getRegister(hm);
				r.setToResult(p, "random value %d..%d%s".formatted(imin,imax,seq));
				//p.printf("random value %d..%d%s\n", );
				return;
			}else {
				//will have to long mult
				if(!s.<Boolean>getOption(COption.DO_LONG_MULT, c.job,c.cursor)) {
					Warnings.warningf(c, "making a dynamic random in a scope that does not allow long multiplication; possible performance loss");
				}
				//2^31 ~ 2.1e9, so actually just make 10^9 random max and it is about correct
				BiOperator add = new BiOperator(token.line,token.col,OpType.ADD);
				BiOperator sub = new BiOperator(token.line,token.col,OpType.SUB);
				BiOperator mul = new BiOperator(token.line,token.col,OpType.MULT)
						.forceLongMult(true).requireFinalType(retype);
				min.compileOps(p, c, s, retype);
				int h1 = min.setReg(p, c, s, retype);
				stack.castRegister(p, s, h1, retype);
				max.compileOps(p, c, s, retype);
				int h2 = max.setReg(p, c, s, retype);
				stack.castRegister(p, s, h2, retype);
				sub.perform(p, c, s, stack, h2, h1);
				int fineness = 9;//
				int h3 = stack.setNext(VarType.DOUBLE.withPrecision(fineness));
				Register r = stack.getRegister(h3);
				r.setToResult(p, "random value 0..%d%s".formatted((int)Math.pow(10, fineness),seq));
				mul.perform(p, c, s, stack, h2, h3);
				add.perform(p, c, s, stack, h1, h2);
				stack.cap(h1);
			}
			
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart,
				VarType typeWanted) throws CompileError {
			assert stack.getTop()==stackstart;
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			int home = stack.getTop();
			v.setMe(p, s, stack, home);
			stack.pop();
			
		}

		@Override
		public void dumpRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			stack.pop();
		}

		@Override
		public Number getEstimate(BFCallToken token, Scope s) throws CompileError {
			VarType type = this.getRetType(token, s);
			if(type.isLogical())return null;
			Equation min = this.getMin(token, s, null);
			Equation max = this.getMax(token, s, null);
			Number emin = null;
			Number emax = null;
			if(min.isConstable()) {
				emin =Math.abs(((Num)min.getConst()).value.doubleValue());
			}else if (min.isConstRefable()) {
				emin =Math.abs(s.getEstimate(min.getConstVarRef()).doubleValue());
			}
			if(max.isConstable()) {
				emax =Math.abs(((Num)max.getConst()).value.doubleValue());
			}else if (min.isConstRefable()) {
				emax =Math.abs(s.getEstimate(max.getConstVarRef()).doubleValue());
			}
			Number e = emin==null? emax: emin;
			if(emax!=null) e = Math.max(emax.doubleValue(), e.doubleValue());
			return e;
		}

		@Override
		public boolean isNonstaticMember() {
			return !this.isStatic;
		}

	}
	public static class SetSeed extends BFReturnVoid{
		public static final SetSeed setSeed = new SetSeed("reset",false,true,true);
		public static final SetSeed setSeedStatic = new SetSeed("reset",true,true,true);
		private final boolean isStatic;
		private final boolean useWorld;
		private final boolean useSequence;
		public SetSeed(String name,boolean isStatic,boolean useWorld, boolean useSequence) {
			super(name);
			this.isStatic=isStatic;
			this.useWorld=useWorld;
			this.useSequence=useSequence;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
				throws CompileError {
			return BuiltinFunction.tokenizeArgsEquations(c, s, matcher, line, col, stack);
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_2), s.getTarget(), this.name, c);
			BasicArgs args = (BasicArgs)token.getArgs();
			String seq = "*";
			if(!this.isStatic) {
				Variable self = token.getThisBound();
				VarType randType = self.type;
				McRandom struct = (McRandom) randType.struct;
				seq =struct.sequenceId(self);
			}
			if(args.nargs()>1) throw new CompileError.WrongArgNumber(token, name, "1 or less", args.nargs());
			if(args.nargs()==0) {
				p.printf("random reset %s\n", seq);
			}else {
				Equation seed = (Equation) args.arg(0);
				seed.constify(c, s);
				if(!seed.isConstable()) seed.throwNotConstError();
				ConstExprToken cseed = seed.getConst();
				if(cseed.constType()!=ConstType.NUM) throw new CompileError.WrongConstType(this.name, ConstType.NUM, cseed);
				int iseed = ((Num) cseed).value.intValue();
				p.printf("random reset %s %d %b %b\n", seq,iseed,this.useWorld,this.useSequence);
			}
		}

		@Override
		public boolean isNonstaticMember() {
			return !this.isStatic;
		}
		
	}
	@Targeted
	public static void uuidRandInt(PrintStream p,Scope s, RStack stack,Variable toset) throws CompileError {
		String mytag = "mcpp:random+uuid";
		Selector me = new Selector("@e", mytag,1);
		Variable uuid3 = new Variable("$uuid",VarType.INT,null,Mask.ENTITY,"","").maskEntity(me, NbtPath.UUID_LAST);
		p.printf("summon %s %s {Tags: [\"%s\"]}\n", "marker","0 0 0",mytag);
		Variable.directSet(p, s, toset, uuid3, stack);
		p.printf("kill %s\n", me.toCMD());
	}
}
