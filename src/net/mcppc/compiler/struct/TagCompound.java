package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
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
import net.mcppc.compiler.tokens.Equation;
//internal only, dont register
/**
 * represents a generic compound-nbt tag variable;
 * this used to be internal only but now it is exposed;
 * this is not to be confused with the TODO NbtObject class, which has dynamic tag type;
 * @author RadiumE13
 *
 */
public class TagCompound extends Struct {
	//public static final TagCompound tag = new TagCompound("$tag");
	public static final TagCompound tag = new TagCompound("TagCompound");
	public static final VarType TAG_COMPOUND = new VarType(tag,new StructTypeParams.Blank() );
	
	
	public static void registerAll() {
		tag.bfs = Map.of(Merge.merge.name,Merge.merge);
		Struct.register(tag);
	}
	public TagCompound(String name) {
		super(name);
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
				ConstExprToken tg = eq.getConst();
				if(tg.constType()!=ConstType.NBT) throw new CompileError("cannot merge a %s value into a tag".formatted(tg.constType().name));
				mergeTags(p,s,self,(NbtPath.NbtPathToken)tg);
			}else if (eq.isConstRefable()) {
				Variable other = eq.getConstVarRef();
				mergeTags(p,s,self,other);
			}else throw new CompileError("could not merge expression");
			
		}
		public static void mergeTags(PrintStream p,Scope s,Variable self,Variable other) {
			boolean isOtherObj = false;
			if(isOtherObj) {
				//TODO use other.value
			}
			String dto=self.dataPhrase();
			String dfrom=other.dataPhrase();
			p.printf("data modify %s merge from %s\n",dto,dfrom);
			
		}
		public static void mergeTags(PrintStream p,Scope s,Variable self,NbtPath.NbtPathToken tag) {
			String dto=self.dataPhrase();
			p.printf("data modify %s merge value %s\n",dto,tag.textInMcf());
			
		}
		
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return "tag_compound";
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
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
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
