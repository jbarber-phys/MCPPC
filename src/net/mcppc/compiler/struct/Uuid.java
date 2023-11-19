package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.INbtValueProvider;
import net.mcppc.compiler.NbtPath;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.StructTypeParams;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.Size;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.BiOperator.OpType;
/**
 * struct representing a UUID of an entity; equivalent to an nbt int[4].
 * 
 * @author RadiumE13
 *
 */
public class Uuid extends Struct {
	public static Uuid uuid;
	static {
		uuid=new Uuid("Uuid");
	}
	private static final Integer DEFAULTFILL = null;
	private static final VarType ELEMENT_TYPE = VarType.INT;
	private static final int DIM=4;
	
	public static void registerAll() {
		uuid.METHODS = Map.of(HashUUID.hash.name,HashUUID.hash);
		Struct.register(uuid);
	}
	public Uuid(String name) {
		super(name);
	}
	public VarType getType() {
		return new VarType(this,new StructTypeParams.Blank());
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return VarType.Builtin.NBT_LIST;
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
	public String getJsonTextFor(Variable self) throws CompileError {
		//minecraft ONLY SUPPORTS "%s", no others
		String[] cpnts=new String[DIM];
		for(int i=0;i<DIM;i++)cpnts[i]=this.getComponent(self, i).getJsonText();
		return Regexes.formatJsonWith("Uuid(%s,%s,%s,%s)", cpnts);
	}
	private Variable getComponent(Variable self, int i) throws CompileError {
		if(i<0)throw new CompileError.VarNotFound(this, name);
		return self.indexMyNBTPathBasic(i, ELEMENT_TYPE);
	}

	@Override
	public int sizeOf(VarType mytype) {
		return 0;
		//return 4;
	}

	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted) throws CompileError {
		throw new CompileError.CannotStack(me.type);

	}

	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		throw new CompileError.CannotStack(me.type);

	}
	//allocate empty arrays
	@Override
	public void allocateLoad(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		if(DEFAULTFILL==null) super.allocateArrayLoad(p, tg, var, false, 0, ELEMENT_TYPE);
		else super.allocateArrayLoad(p, tg, var, fillWithDefaultvalue, DIM, ELEMENT_TYPE);
	}

	@Override
	public void allocateCall(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		if(DEFAULTFILL==null) super.allocateArrayCall(p, tg, var, false, 0, ELEMENT_TYPE);
		else super.allocateArrayCall(p, tg, var, fillWithDefaultvalue, DIM, ELEMENT_TYPE);
	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		// default to empty list so that default != any valid UUID
		if(DEFAULTFILL!=null) {
			return "[%d,%d,%d,%d]".formatted(DEFAULTFILL,DEFAULTFILL,DEFAULTFILL,DEFAULTFILL);
		}else return Struct.DEFAULT_LIST;
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
	public Variable UuidOf(Selector s) throws CompileError {
		VarType vt = this.getType();
		Variable v = new Variable("$anonuuidof",vt,null,new ResourceLocation("mcppc","uuid"));
		return v.maskEntity(s, NbtPath.UUID);
	}
	protected static class HashUUID extends BuiltinFunction{
		public static final HashUUID hash = new HashUUID("hash");
		public HashUUID(String name) {
			super(name);
		}
		@Override public boolean isNonstaticMember() {return true;}
		@Override public VarType getRetType(BFCallToken token) {
			return VarType.INT;
		}
		@Override public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			return BuiltinFunction.tokenizeArgsNone(c, matcher, line, col);
		}
		@Override public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			// do nothing here
		}
		@Override public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {
			Variable self = token.getThisBound();
			Variable smallest = ((Uuid) self.type.struct).getComponent(self, 3);
			smallest.getMe(p, s, stack, stackstart, typeWanted);
		}
		@Override public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			Variable self = token.getThisBound();
			Variable smallest = ((Uuid) self.type.struct).getComponent(self, 3);
			Variable.directSet(p, s, v, smallest, stack);
		}

		@Override public Number getEstimate(BFCallToken token) {
			return Integer.MAX_VALUE;
		}
		
	}
}
