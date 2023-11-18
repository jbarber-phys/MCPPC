package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.INbtValueProvider;
import net.mcppc.compiler.NbtPath;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.StructTypeParams;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.BFReturnVoid;
import net.mcppc.compiler.functions.Size;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.BiOperator.OpType;
//internal only, dont register
/**
 * represents a generic compound-nbt tag variable;
 * this used to be internal only but now it is exposed;
 * this is not to be confused with the NbtObject class, which has dynamic tag type;
 * @author RadiumE13
 *
 */
public class NbtCompound extends Struct {
	//public static final TagCompound tag = new TagCompound("$tag");
	public static final NbtCompound tag = new NbtCompound("Compound");
	public static final VarType TAG_COMPOUND = new VarType(tag,new StructTypeParams.Blank() );
	
	
	public static void registerAll() {
		final Size size = new Size("size");//number of direct childeren
		tag.bfs = Map.of(
				Merge.merge.name,Merge.merge
				,size.name,size);
		Struct.register(tag);
	}
	public NbtCompound(String name) {
		super(name);
	}

	@Override
	public boolean canDoBiOpDirect(BiOperator op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		if (other.isStruct() && other.struct.getNBTTagType(other).equals(this.getNBTTagType(mytype))
				&& !(other.struct instanceof NbtObject));
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
	@Override
	public int doBiOpSecondDirect(BiOperator op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack,
			INbtValueProvider other, INbtValueProvider me) throws CompileError {
		return super.basicDirectEquals(p, c, s, stack,other, me, op.op == OpType.NEQ);
	}
	@Override
	public boolean canSetToExpr(ConstExprToken e) {
		return e.constType() == ConstType.NBT;
	}
	@Override
	public void setMeToExpr(PrintStream p, Scope s, RStack stack, Variable me, ConstExprToken e) throws CompileError {
		super.setMeToNbtExprBasic(p, stack, me, e);
	}
	private Map<String,BuiltinFunction> bfs;
	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		return super.hasBuiltinMethodBasic(name, bfs);
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return super.getBuiltinMethodBasic(self, name, bfs);
	}
	
	public static class Merge extends BFReturnVoid{
		public static final Merge merge = new Merge("merge");
		public Merge(String name) {
			super(name);
		}
		@Override public boolean isNonstaticMember(){return true;}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
				throws CompileError {
			return BuiltinFunction.tokenizeArgsEquations(c, s, matcher, line, col, stack);
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			BasicArgs args = (BasicArgs) token.getArgs();
			Variable self = token.getThisBound();
			if(args.nargs()!=1) throw new CompileError("function %s needed 1 arg but got %s".formatted(token.asString(),args.nargs()));
			Equation eq = (Equation) args.arg(0);
			eq.constify(c, s);
			if(eq.isConstable()) {
				//ConstExprToken tg = eq.getConst();
				ConstExprToken tg = Equation.constifyAndGet(p, eq, c, s, stack, ConstType.NBT);
				//if(tg.constType()!=ConstType.NBT) throw new CompileError("cannot merge a %s value into a tag".formatted(tg.constType().name));
				mergeTags(p,s,self,(NbtPath.NbtPathToken)tg);
			}else if (eq.isConstRefable()) {
				Variable other = eq.getConstVarRef();
				mergeTags(p,s,self,other);
			}else throw new CompileError("could not merge expression");
			
		}
		@Targeted
		public static void mergeTags(PrintStream p,Scope s,Variable self,Variable other) {
			if(other.isStruct() && other.type.struct instanceof NbtObject) {
				NbtObject clazz = (NbtObject) other.type.struct;
				//use its value
				other = clazz.getValue(self, self.type);
			}
			String dto=self.dataPhrase();
			String dfrom=other.dataPhrase();
			p.printf("data modify %s merge from %s\n",dto,dfrom);
			
		}
		@Targeted
		public static void mergeTags(PrintStream p,Scope s,Variable self,NbtPath.NbtPathToken tag) {
			String dto=self.dataPhrase();
			p.printf("data modify %s merge value %s\n",dto,tag.textInMcf());
			
		}
		
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return VarType.Builtin.NBT_COMPOUND;
	}

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return 0;
	}

	@Override
	public String getPrecisionStr(VarType mytype) {
		return null;
	}

	@Override
	public String getJsonTextFor(Variable variable) throws CompileError {
		return variable.getJsonTextBasic();
	}

	@Override
	public int sizeOf(VarType mytype) {
		return 0;
	}

	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}
	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);
	}

	@Override
	public void allocateLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateCompoundLoad(p, var, fillWithDefaultvalue, List.of());
	}

	@Override
	public void allocateCall(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateCompoundCall(p, var, fillWithDefaultvalue, List.of());
	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		return Struct.DEFAULT_COMPOUND;
	}

	@Override
	public boolean hasField(Variable self, String name) {
		return false;
	}

	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}


}
