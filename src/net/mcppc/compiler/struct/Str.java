package net.mcppc.compiler.struct;
import net.mcppc.compiler.struct.*;
import net.mcppc.compiler.struct.Vector.Constructor;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.target.Version;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Token;

import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.BuiltinFunction.BasicArgs;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.INbtValueProvider.Macro;
import net.mcppc.compiler.INbtValueProvider.SubstringProvider;
import net.mcppc.compiler.VarType.Builtin;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.Size;
/**
 * struct type for a string;
 * @author RadiumE13
 *
 */
/*
 * string tags cannot be appended / inserted ...
 * TODO substring; TEST sub and constructor
 * strings <location> start end; returns a substring of the string given as data;
 * does not work on lists
 * substring can get a string from a number;
 * TODO constructor method using modify ... set string <path> , but no bounds
 */
public class Str extends Struct{
	public static final Str string;
	static {
		string=new Str("String");
	}

	public static final VarType STR = new VarType(string,new StructTypeParams.Blank());
	public static void registerAll() {
		final Size size = new Size("size");
		final Size isFull = new Size.IsFull("isFull");
		string.METHODS = Map.of(
				size.name,size,
				isFull.name,isFull,
				Substring.substring.name,Substring.substring);
		Struct.register(string);
	}
	public Str(String name) {
		super(name);
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return VarType.Builtin.NBT_STRING;
	}
	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return 0;
	}

	@Override
	public String getPrecisionStr(VarType mytype)  {
		return "";
	}
	@Override
	public String getJsonTextFor(Variable variable) throws CompileError {
		return variable.getJsonTextBasic();
	}
	@Override
	public int sizeOf(VarType mytype) {
		return -1;//cannot stack
	}
	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}
	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}

	@Override public boolean canBeRecursive(VarType type) {
		return true;
	}
	@Override
	public void allocateLoad(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		var.allocateLoadBasic(p, fillWithDefaultvalue, Struct.DEFAULT_STRING);
	}
	@Override
	public void allocateCall(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		var.allocateCallBasic(p, tg, fillWithDefaultvalue, Struct.DEFAULT_STRING);
	}
	@Override
	public String getDefaultValue(VarType var) {
		return Struct.DEFAULT_STRING;
	}
	@Override
	public boolean hasField(Variable self, String name) {
		return false;
	}
	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}

	private Map<String,BuiltinFunction> METHODS;
	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		return super.hasBuiltinMethodBasic(name, METHODS);
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return super.getBuiltinMethodBasic(self, name, METHODS);
	}
	@Override
	public boolean canSetToExpr(ConstExprToken e) {
		return e.constType()==ConstType.STRLIT;
	}
	@Override
	@Targeted
	public void setMeToExpr(PrintStream p, Scope s, RStack stack, Variable me, ConstExprToken e) throws CompileError {
		p.printf("data modify %s set value %s\n",me.dataPhrase(), ((Token.StringToken)e).literal());
	}
	@Override
	public boolean canDoBiOpDirect(BiOperator op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		if (other.isStruct() && other.struct instanceof Str) ;
		else return false;
		switch(op.op) {
		case EQ:
		case NEQ: return true;
		default: return false;
		}
		
	}
	@Override
	public int doBiOpFirstDirect(BiOperator op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack,
			INbtValueProvider me, INbtValueProvider other) throws CompileError {
		return super.basicDirectEquals(p, c, s, stack, me, other, op.op == OpType.NEQ);
	}
	public static class SubstringCodegen extends CodeGenerator{
		public static final SubstringCodegen first = new SubstringCodegen("substringmacro1",false);
		public static final SubstringCodegen both = new SubstringCodegen("substringmacro2",true);
		public static final String START = "start";
		public static final String END = "end";
		public final boolean second;
		public SubstringCodegen(String name,boolean second) {
			super(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"strings/%s".formatted(name)),true);
			this.second=second;
		}
		@Override
		public void build(PrintStream p, CompileJob job, Namespace ns) throws CompileError {
			Macro m1 = new Macro(START,VarType.INT);
			Macro m2 = second? new Macro(END,VarType.INT) : null;
			Variable ret = Substring.substring.getRetBuff();
			Variable self = Substring.substring.getSelfBuff();
			SubstringProvider val = new SubstringProvider(self,m1,m2);
			Variable.trueDirectSetBasicNbt(p,job.getTarget(),  ret, val);
		}
		private boolean registered = false;
		@Targeted
		public static  void call(PrintStream p,Compiler c,Scope s,Equation start,Equation end) throws CompileError {
			SubstringCodegen gen = end==null? first:both;
			Variable mcros = gen.macroTag();
			mcros.allocateCall(p, s.getTarget(), true);
			start.compileOps(p, c, s, VarType.INT); start.setVar(p, c, s, mcros.fieldMyNBTPath(START, VarType.INT));
			if(end!=null){
				end.compileOps(p, c, s, VarType.INT); end.setVar(p, c, s, mcros.fieldMyNBTPath(END, VarType.INT));
			}
			p.println(gen.getCall());
			if(!gen.registered) {
				CodeGenerator.register(gen);gen.registered=true;
			}
		}
	}
	public static class Substring extends BuiltinFunction{
		public static final Substring substring = new Substring("substring");
		public static final ResourceLocation PATH = new ResourceLocation("mcppc","strings");
		public Substring(String name) {
			super(name);
		}

		@Override
		public VarType getRetType(BFCallToken token, Scope s) throws CompileError {
			return Str.STR;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
				throws CompileError {
			return BuiltinFunction.tokenizeArgsEquations(c, s, matcher, line, col, stack);
		}
		public Variable getRetBuff() throws CompileError {
			return Variable.returnOfPath(PATH, name, STR);
		}

		public Variable getSelfBuff() throws CompileError {
			return new Variable("$this",STR,null,Mask.STORAGE,PATH.toString(),"%s.\"$this\"".formatted(this.name));
		}
		
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			BasicArgs args =(BasicArgs)  token.getArgs();
			VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_2),s.getTarget(), "directSetNbt", c);
			if(args.nargs()>2 || args.isEmpty()) throw new CompileError.WrongArgNumber(token, name, "1 or 2",args.nargs() );
			Equation start =(Equation) args.arg(0);
			Equation end =args.nargs()>1 ?(Equation) args.arg(1): null;
			start.constify(c, s);
			if(end!=null)end.constify(c, s);
			boolean isendconst = end==null?true:end.isConstable();
			if(start.isConstable() && isendconst) {
				p.printf("#const substring bounds\n");
				Variable self = token.getThisBound();
				Variable retBuff = this.getRetBuff();
				ConstExprToken c1 = start.getConst();
				ConstExprToken c2 = end==null? null:end.getConst();
				if(c1.constType()!=ConstType.NUM) throw new CompileError.WrongConstType(this.name, ConstType.NUM, c1);
				if(c2!=null && c2.constType()!=ConstType.NUM) throw new CompileError.WrongConstType(this.name, ConstType.NUM, c2);
				int st = ((Num)c1).value.intValue();
				Integer ed =c2!=null? ((Num)c2).value.intValue(): null;
				INbtValueProvider.SubstringProvider sub = new INbtValueProvider.SubstringProvider(self,st,ed);
				Variable.trueDirectSetBasicNbt(p, s.getTarget(), retBuff, sub);
			}else {
				p.printf("#variable substring bounds\n");
				Variable self = token.getThisBound();
				Variable selfbuff = this.getSelfBuff();
				Variable.directSet(p, s, selfbuff, self, stack);
				SubstringCodegen.call(p, c, s, start, end);
			}
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart,
				VarType typeWanted) throws CompileError {
			Variable retBuff = Variable.returnOfPath(PATH, name, STR);
			retBuff.getMe(p, s, stack, stackstart, typeWanted);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			Variable retBuff = Variable.returnOfPath(PATH, name, STR);
			Variable.directSet(p, s, v, retBuff, stack);
		}

		@Override
		public Number getEstimate(BFCallToken token, Scope s) throws CompileError {
			return null;
		}

		@Override public boolean isNonstaticMember() { return true; }
		
	}
	private final Constructor init = new Constructor(this);
	@Override public BuiltinConstructor getConstructor(VarType myType) throws CompileError {
		return this.init;
	}
	/**
	 * converts another var into a new string value
	 * @author RadiumE13
	 *
	 */
	public static class Constructor extends BuiltinConstructor{

		public Constructor(Struct clazz) {
			super(clazz.name);
		}


		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
				throws CompileError {
			return BuiltinFunction.tokenizeArgsEquations(c, s, matcher, line, col, stack);
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_2), s.getTarget(), "string constructor", c);
			BasicArgs args = (BasicArgs) token.getArgs();
			if(args.nargs()!=1) throw new CompileError.WrongArgNumber(token, name, "1", args.nargs());
			Equation eq = (Equation) args.arg(0);eq.constify(c, s);
			Variable obj=Struct.newobj(c,token);
			obj.allocateLoad(p, s.getTarget(), false);
			if(eq.isConstable()) {
				ConstExprToken ce = eq.getConst();
				if(ce.constType()==ConstType.STRLIT) {
					//do nothing
				}else {
					String txt = ce.textInMcf(s.getTarget());
					String literal = CMath.getStringLiteral(txt);
					ce = new Token.StringToken(ce.line,ce.col,literal);
				}
				Variable.trueDirectSetBasicNbt(p, s.getTarget(), obj,(Token.StringToken) ce);
			}else {
				Variable v = null;
				if(eq.isConstRefable()) {
					Variable veq= eq.getConstVarRef();
					if(veq.hasData()) v = veq;
				}
				if(v==null) {
					eq.compileOps(p, c, s, STR);
					v = new Variable("$buff",eq.retype,null,Mask.STORAGE, "","")
							.maskStorage(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"strings/new"), new NbtPath("\"$buff\""));
					eq.setVar(p, c, s, v);
				}
				INbtValueProvider sub = new INbtValueProvider.SubstringProvider(v);
				Variable.trueDirectSetBasicNbt(p, s.getTarget(), obj, sub);
			}
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {
			Variable obj=Struct.newobj(c,token);
			obj.getMe(p,s, stack, stackstart, typeWanted);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			Variable obj=Struct.newobj(c,token);
			Variable.directSet(p,s, v, obj, stack);
		}

		@Override
		public Number getEstimate(BFCallToken token, Scope s) throws CompileError {
			return null;
		}
		
	}
}
