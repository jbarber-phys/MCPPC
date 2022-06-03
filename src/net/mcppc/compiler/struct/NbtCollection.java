package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.BuiltinFunction.Args;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.BuiltinFunction.BasicArgs;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Function;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.StructTypeParams;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.StructTypeParams.MembType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.FunctionMask;
import net.mcppc.compiler.functions.Size;
import net.mcppc.compiler.functions.FunctionMask.MCFArgs;
import net.mcppc.compiler.struct.Vector.Constructor;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Type;

/**
 * one big class for Stack, Queue, and List; size changing nbt array types;
 * dont use for vectors, directions, nbt tags, or armor inventories;
 * @author jbarb_t8a3esk
 *
 *TODO binary lookup, add seperate class for long sets / maps
 *TODO comparisons
 */
public class NbtCollection extends Struct {
	public static final NbtCollection stack = new NbtCollection("Stack");
	public static final NbtCollection queue = new NbtCollection("Queue");
	public static final NbtCollection staque = new NbtCollection("Staque");//double stack and queue
	
	public static void registerAll() {
		final Size size = new Size("size");
		final Size isFull = new Size.IsFull("isFull");
		final Size hasNext = new Size.IsFull("hasNext");
		stack.METHODS = Map.of(
				"push",EndPend.prepend, //multiple aliases
				"add",EndPend.prepend,
				"pop",Pop.pop,
				size.name,size,
				Clear.clear.name,Clear.clear,
				hasNext.name,hasNext
				);
		queue.METHODS = Map.of(
				"enqueue",EndPend.append, //multiple aliases
				"add",EndPend.append,
				"pop",Pop.pop,
				size.name,size,
				Clear.clear.name,Clear.clear,
				hasNext.name,hasNext
				);
		staque.METHODS = Map.of(
				"enqueue",EndPend.append, //no generic
				"push",EndPend.prepend,
				"pop",Pop.pop,
				size.name,size,
				Clear.clear.name,Clear.clear,
				hasNext.name,hasNext
				);
		Struct.register(stack);
		Struct.register(queue);
		Struct.register(staque);
		NbtList.registerAll();
		NbtSet.registerAll();
	}
	
	public NbtCollection(String name) {
		super(name);
	}
	protected static VarType myMembType(VarType mytype) {
		return ((MembType) mytype.structArgs).myType;
	}

	@Override
	public String getNBTTagType(VarType varType) {
		return "tag_list";
	}

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return 0;
	}

	@Override
	public String getPrecisionStr(VarType mytype) {
		return "";
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
	public StructTypeParams tokenizeTypeArgs(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError {
		return StructTypeParams.MembType.tokenizeTypeArgs(c,s, matcher, line, col, forbidden);
	}
	@Override
	public StructTypeParams paramsWNoArgs() throws CompileError {
		throw new CompileError("struct of type %s needs a member type param".formatted(this.name));
	}
	@Override
	public String headerTypeString(VarType varType) {
		return VarType.HDRFORMATNOTREADY.formatted(this.name,((MembType)varType.structArgs).myType.headerString());
	}
	@Override public boolean canBeRecursive(VarType type) {
		return true;
	}
	@Override
	public boolean canCasteFrom(VarType from, VarType mytype) {
		if(from.isStruct()&&from.struct instanceof NbtCollection){
			return VarType.canCast(NbtCollection.myMembType(from), NbtCollection.myMembType(mytype));
		}else return false;
		
	}
	@Override
	public boolean canCasteTo(VarType to, VarType mytype) {
		if(to.isStruct()&&to.struct instanceof NbtCollection){
			return VarType.canCast(NbtCollection.myMembType(mytype), NbtCollection.myMembType(to));
		}else return false;
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
		super.allocateArrayLoad(p, var, fillWithDefaultvalue, 0, NbtCollection.myMembType(var.type));

	}

	@Override
	public void allocateCall(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateArrayCall(p, var, fillWithDefaultvalue, 0, NbtCollection.myMembType(var.type));

	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		return super.DEFAULT_LIST;
	}

	
	@Override
	public void castVarFrom(PrintStream p, Scope s, RStack stack, Variable vtag, VarType old, VarType mytype)
			throws CompileError {
		// super behavior
		super.castVarFrom(p, s, stack, vtag, old, mytype);
	}
	@Override
	public void castVarTo(PrintStream p, Scope s, RStack stack, Variable vtag, VarType mytype, VarType newType)
			throws CompileError {
		// super behavior
		super.castVarTo(p, s, stack, vtag, mytype, newType);
	}
	@Override
	public void getMeDirect(PrintStream p, Scope s, RStack stack, Variable to, Variable me) throws CompileError {
		// super
		super.getMeDirect(p, s, stack, to, me);
	}
	@Override
	public void setMeDirect(PrintStream p, Scope s, RStack stack, Variable me, Variable from) throws CompileError {
		//super
		super.setMeDirect(p, s, stack, me, from);
	}
	public boolean canIndexMe(Variable self, int i) throws CompileError{
		return true;
	}
	@Override
	public Variable getIndexRef(Variable self, int index) throws CompileError {
		return super.basicTagIndexRef(self, index, NbtCollection.myMembType(self.type));
	}

	public boolean canSetToExpr(ConstExprToken e) {
		return false; // no list const yet
	}
	@Override
	public void setMeToExpr(PrintStream p, RStack stack, Variable me, ConstExprToken e) throws CompileError {
		// no list consts yet
		super.setMeToExpr(p, stack, me, e);
	}
	@Override
	public boolean hasField(Variable self, String name) {
		return false;
	}

	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}

	protected Map<String,BuiltinFunction> METHODS;
	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		return super.hasBuiltinMethodBasic(name, METHODS);
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return super.getBuiltinMethodBasic(self, name, METHODS);
	}
	//private Map<String,FunctionMask> STATICMETHODS;
	@Override
	public boolean hasStaticBuiltinMethod(String name) {
		return false;//TODO
	}
	@Override
	public BuiltinFunction getStaticBuiltinMethod(String name, VarType type) throws CompileError {
		return null;//TODO
	}
	private final Constructor init = new Constructor(this);
	@Override
	public BuiltinConstructor getConstructor(VarType myType) throws CompileError {
		return this.init;
	}
	public Variable getFirstElement(Variable self) throws CompileError {
		return this.getIndexRef(self, 0);
	}
	
	public static void endpend(PrintStream p, Compiler c, Scope s, RStack stack,
			Variable self,Equation ell,String pend) throws CompileError {
		Variable temp = new Variable("\"$temp\"",NbtCollection.myMembType(self.type), null, Mask.STORAGE, "mcpp:nbtcollection", "\"$temp\"");
		temp.allocateLoad(p, false);
		ell.compileOps(p, c, s, temp.type);
		ell.setVar(p, c, s, temp);
		p.printf("data modify %s %s from %s\n", self.dataPhrase(),pend,temp.dataPhrase());
	}
	public static class EndPend extends BuiltinFunction {
		public static final String APPEND = "append";
		public static final String PREPEND = "prepend";
		public static final EndPend append = new EndPend("append",APPEND);
		public static final EndPend prepend = new EndPend("prepend",PREPEND); //may multi-alias
		final String pend;
		public EndPend(String name,String pend) {
			super(name);
			this.pend=pend;
		}

		@Override public boolean isNonstaticMember() {return true;}
		@Override
		public VarType getRetType(BFCallToken token) {
			return VarType.VOID;
		}
		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			MCFArgs args=new MCFArgs();
			Function.FuncCallToken.addArgs(c, line, col, matcher, stack, args.args);
			if(args.args.size()!=1)throw new CompileError("function instanceof EndPend must have 1 arg;");
			return args;
		}
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			// do nothing yet
			Variable self = token.getThisBound();
			Equation eq = ((MCFArgs) token.getArgs()).args.get(0);
			NbtCollection.endpend(p, c, s, stack, self, eq, this.pend);
		}
		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			//
			
		}
		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			//
		}
		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
	public static class Pop extends BuiltinFunction{
		public static Pop pop = new Pop("pop");
		public Pop(String name) {
			super(name);
		}
		@Override public boolean isNonstaticMember() {return true;}
		@Override
		public VarType getRetType(BFCallToken token) {
			return NbtCollection.myMembType(token.getThisBound().type);
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			return BuiltinFunction.tokenizeArgsNone(c, matcher, line, col);
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			//nothing yet
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			Variable start = token.getThisBound().indexMyNBTPath(0);
			start.getMe(p, s, stack.getRegister(stackstart));
			postcall(p, c, s, token, stack);
			
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			Variable start = token.getThisBound().indexMyNBTPath(0);
			Variable.directSet(p, s, v, start, stack);
			postcall(p, c, s, token, stack);
			
		}
		@Override
		public void dumpRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			postcall(p, c, s, token, stack);
		}
		public void postcall(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			Variable start = token.getThisBound().indexMyNBTPath(0);
			p.printf("data remove %s\n", start.dataPhrase());//remove first index tag
		}
		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
	public static class Clear extends BuiltinFunction{
		public static Clear clear = new Clear("clear");
		public Clear(String name) {
			super(name);
		}
		@Override public boolean isNonstaticMember() {return true;}
		@Override
		public VarType getRetType(BFCallToken token) {
			return VarType.VOID;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			return BuiltinFunction.tokenizeArgsNone(c, matcher, line, col);
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			Variable self = token.getThisBound();
			self.setMeToNbtValueBasic(p, c, s, stack, DEFAULT_LIST);
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
		public void dumpRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		}
		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
	public static Variable componentOf(Variable self,int i)throws CompileError {
		return ((NbtCollection)self.type.struct).getIndexRef(self, i);
	}
	public static class Constructor extends BuiltinConstructor{
		public Constructor(String name) {
			super(name);
		}
		public Constructor(NbtCollection clazz) {
			this(clazz.name);
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col,RStack stack) throws CompileError {
			BasicArgs a=new BuiltinFunction.BasicArgs().equations(c, line, col, matcher, stack);
			//any number of args is OK
			return a;
		}

		private static final String NEW= "\"$Set\".\"$new\"";
		private Variable newobj(Compiler c,BFCallToken tk) {
			Variable v=new Variable(NEW, tk.getStaticType(), null,c.resourcelocation);
			return v;
		}
		@Override
		public void call(PrintStream p, Compiler c, Scope s,  BFCallToken token, RStack stack) throws CompileError {
			//default to storage
			BasicArgs args = (BasicArgs)token.getArgs();
			Variable obj=this.newobj(c,token);
			obj.allocateLoad(p, false);//anon must think its loaded
			int size = args.nargs();
			for(int i=0;i<size;i++) {
				//Variable arg=NbtCollection.componentOf(obj, i);
				Equation eq=(Equation) ((BasicArgs)args).arg(i);
				NbtCollection.endpend(p, c, s, stack, obj, eq, EndPend.APPEND);
			}
			
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			//will throw
			Variable obj=this.newobj(c,token);
			obj.getMe(p,s, stack, stackstart);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			Variable obj=this.newobj(c,token);
			Variable.directSet(p,s, v, obj, stack);
		}

		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
}
