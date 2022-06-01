package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CodeGenerator;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.StructTypeParams;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.BuiltinFunction.Args;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.BuiltinFunction.BasicArgs;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.StructTypeParams.MembTypePair;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.Size;
import net.mcppc.compiler.struct.NbtCollection.Clear;
import net.mcppc.compiler.struct.NbtList.ChangeAt;
import net.mcppc.compiler.struct.NbtList.GetAt;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Token;

public class NbtMap extends Struct {
	public static final NbtMap map = new NbtMap("Map");
	
	public static void registerAll() {
		final Size size = new Size("size");
		final Size isFull = new Size.IsFull("isFull");
		map.METHODS = Map.of(
				size.name,size,
				Clear.clear.name,Clear.clear,
				Edit.put.name,Edit.put,
				Edit.putIfAbsent.name,Edit.putIfAbsent,
				Edit.putIfPresent.name,Edit.putIfPresent,
				Edit.remove.name,Edit.remove,
				Get.get.name,Get.get,
				Has.has.name,Has.has
				);
		Struct.register(map);
	}
	
	public NbtMap(String name) {
		super(name);
	}
	
	protected static VarType myKeyType(VarType mytype) {
		return ((MembTypePair) mytype.structArgs).first;
	}
	protected static VarType myValueType(VarType mytype) {
		return ((MembTypePair) mytype.structArgs).second;
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
		return StructTypeParams.MembTypePair.tokenizeTypeArgs(c,s, matcher, line, col, forbidden);
	}
	@Override
	public StructTypeParams paramsWNoArgs() throws CompileError {
		throw new CompileError("struct of type %s needs a member type param".formatted(this.name));
	}
	@Override
	public String headerTypeString(VarType varType) {
		return VarType.HDRFORMATNOTREADY.formatted(this.name,
				String.join(" , ",((MembTypePair)varType.structArgs).first.headerString(),
						((MembTypePair)varType.structArgs).second.headerString())
				);
	}
	@Override public boolean canBeRecursive(VarType type) {
		return true;
	}
	@Override
	public boolean canCasteFrom(VarType from, VarType mytype) {
		if(from.isStruct()&&from.struct instanceof NbtMap){
			return VarType.canCast(NbtMap.myKeyType(from), NbtMap.myKeyType(mytype))
					&& VarType.canCast(NbtMap.myValueType(from), NbtMap.myValueType(mytype))
					;
		}else return false;
		
	}
	@Override
	public boolean canCasteTo(VarType to, VarType mytype) {
		if(to.isStruct()&&to.struct instanceof NbtMap){
			return VarType.canCast(NbtMap.myKeyType(mytype), NbtMap.myKeyType(to))
					&& VarType.canCast(NbtMap.myValueType(mytype), NbtMap.myValueType(to));
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
	private static VarType getNbtCompoundType() {
		return new VarType(TagCompound.tag,new StructTypeParams.Blank());
	}
	@Override
	public void allocateLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateArrayLoad(p, var, fillWithDefaultvalue, 0, getNbtCompoundType());

	}

	@Override
	public void allocateCall(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateArrayCall(p, var, fillWithDefaultvalue, 0, getNbtCompoundType());

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

	public boolean canSetToExpr(ConstExprToken e) {
		return false; // no map const yet
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
		return false;
	}
	@Override
	public BuiltinFunction getStaticBuiltinMethod(String name, VarType type) throws CompileError {
		return null;
	}
	private final MapConstructor init = new MapConstructor(this);
	@Override
	public BuiltinConstructor getConstructor(VarType myType) throws CompileError {
		return this.init;
	}
	public VarType mapOf(VarType key,VarType value) {
		return new VarType(this,new StructTypeParams.MembTypePair(key,value));
	}
	@Override
	public Token convertIndexGet(Variable self, Equation index) throws CompileError {
		BuiltinFunction bf = Get.get;
		BuiltinFunction.BFCallToken bft = new BuiltinFunction.BFCallToken(index.line, index.col, bf);
		bft.withThis(self);
		BasicArgs args = new BasicArgs();
		args.add(index);
		bft.withArgs(args);
		return bft;
	}
	@Override
	public Token convertIndexSet(Variable self, Equation index, Equation setTo) throws CompileError {
		BuiltinFunction bf = Edit.put;
		BuiltinFunction.BFCallToken bft = new BuiltinFunction.BFCallToken(index.line, index.col, bf);
		bft.withThis(self);
		BasicArgs args = new BasicArgs();
		args.add(index);
		args.add(setTo);
		bft.withArgs(args);
		return bft;
	}
	public static final String KEY = "key";
	public static final String VALUE = "value";
	public static class Edit extends BuiltinFunction {
		private static enum PutIf{
			ALWAYS,IFABSENT,IFPRESENT,NEVER
		}
		public static final Edit put = new Edit("put",PutIf.ALWAYS);
		public static final Edit putIfAbsent = new Edit("putIfAbsent",PutIf.IFABSENT);
		public static final Edit putIfPresent = new Edit("putIfPresent",PutIf.IFPRESENT);
		public static final Edit remove = new Edit("remove",PutIf.NEVER);
		public static BuiltinFunction.BFCallToken getPutter(Variable self,Equation key,Equation value) {
			BuiltinFunction.BFCallToken bft=new BuiltinFunction.BFCallToken(key.line, key.col, Edit.put);
			bft.withThis(self);
			bft.withArgs(fromEquations(key,value));
			return bft;
		}
		final PutIf condition;
		public Edit(String name,PutIf pif) {
			super(name);
			this.condition = pif;
		}
		@Override public boolean isNonstaticMember() {return true;}

		@Override
		public VarType getRetType(BFCallToken token) {
			return VarType.VOID;
			
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, matcher, line, col, stack);
			int wnargs = this.condition==PutIf.NEVER? 1:2;
			if(args.nargs()!=wnargs) throw new CompileError("wrong number of args in Map.%s(value); expected %d but got %d;".formatted(this.name,wnargs,args.nargs()));
			return args;
		}
		Variable mapbuff1 = null;
		Variable mapbuff2 = null;
		Variable entrybuff = null;
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {

			Equation key = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Equation value = this.condition == PutIf.NEVER ? null :(Equation) ((BasicArgs)token.getArgs()).arg(1);
			Variable self = token.getThisBound();
			VarType keytype = NbtMap.myKeyType(self.type);
			VarType valuetype = NbtMap.myValueType(self.type);
			Loop loop = Loop.loop(Loop.ADD);
			if (this.condition == PutIf.IFABSENT) loop = Loop.loop(Loop.GET);
			this.entrybuff = loop.getEntryBuff(NbtMap.getNbtCompoundType());
			this.mapbuff1 = loop.getMapBuff1(self.type);
			this.mapbuff2 = loop.getMapBuff2(self.type);
			Variable.directSet(p, s,mapbuff1, self, stack);
			this.mapbuff2.allocateLoad(p, true);
			key.compileOps(p, c, s, keytype);
			key.setVar(p, c, s, this.entrybuff.fieldMyNBTPath(KEY, keytype));
			if(value!=null) {
				value.compileOps(p, c, s, valuetype);
				value.setVar(p, c, s, this.entrybuff.fieldMyNBTPath(VALUE, valuetype));
			}
			loop.call(p);
			Register had = loop.getHasFlag();
			switch(this.condition) {
			case ALWAYS:
				p.printf("data modify %s prepend from %s\n",mapbuff2.dataPhrase(), this.entrybuff.dataPhrase());
				break;
				
			case IFABSENT:
				p.printf("execute if score %s matches 0 run ", had.inCMD());
				p.printf("data modify %s prepend from %s\n",mapbuff2.dataPhrase(), this.entrybuff.dataPhrase());
				break;
			case IFPRESENT:
				p.printf("execute if score %s matches 1.. run ", had.inCMD());
				p.printf("data modify %s prepend from %s\n",mapbuff2.dataPhrase(), this.entrybuff.dataPhrase());
				break;
			case NEVER: // do nothing;
				break;
			default:
				
			}
			
			Variable.directSet(p, s, self, this.mapbuff2, stack);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack) {
		}

		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
	public static class Get extends BuiltinFunction {
		public static final Get get = new Get("get");
		public Get(String name) {
			super(name);
		}
		@Override public boolean isNonstaticMember() {return true;}

		@Override
		public VarType getRetType(BFCallToken token) {
			VarType type = token.getThisBound().type;
			return NbtMap.myValueType(type);
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, matcher, line, col, stack);
			int wnargs = 1;
			if(args.nargs()!=wnargs) throw new CompileError("wrong number of args in Map.%s(value); expected %d but got %d;".formatted(this.name,wnargs,args.nargs()));
			return args;
		}
		Variable mapbuff1 = null;
		Variable mapbuff2 = null;
		Variable entrybuff = null;
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {

			Equation key = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Variable self = token.getThisBound();
			VarType keytype = NbtMap.myKeyType(self.type);
			Loop loop = Loop.loop(Loop.GET);
			this.entrybuff = loop.getEntryBuff(NbtMap.getNbtCompoundType());
			this.mapbuff1 = loop.getMapBuff1(self.type);
			this.mapbuff2 = loop.getMapBuff2(self.type);
			Variable.directSet(p, s,mapbuff1, self, stack);
			this.mapbuff2.allocateLoad(p, true);
			key.compileOps(p, c, s, keytype);
			key.setVar(p, c, s, this.entrybuff.fieldMyNBTPath(KEY, keytype));
			loop.call(p);
			//Register had = loop.getHasFlag();
			
			
			Variable.directSet(p, s, self, this.mapbuff2, stack);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			VarType valuetype = NbtMap.myValueType(token.getThisBound().type);
			Variable value = this.entrybuff.fieldMyNBTPath(VALUE, valuetype);
			value.getMe(p, s, stack, stackstart);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack) throws CompileError {
			VarType valuetype = NbtMap.myValueType(token.getThisBound().type);
			Variable value = this.entrybuff.fieldMyNBTPath(VALUE, valuetype);
			Variable.directSet(p, s, v, value, stack);
		}

		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
	public static class Has extends BuiltinFunction {
		public static final Has has = new Has("hasKey");
		public Has(String name) {
			super(name);
		}
		@Override public boolean isNonstaticMember() {return true;}

		@Override
		public VarType getRetType(BFCallToken token) {
			return VarType.BOOL;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, matcher, line, col, stack);
			int wnargs = 1;
			if(args.nargs()!=wnargs) throw new CompileError("wrong number of args in Map.%s(value); expected %d but got %d;".formatted(this.name,wnargs,args.nargs()));
			return args;
		}
		Variable mapbuff1 = null;
		Variable mapbuff2 = null;
		Variable entrybuff = null;
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {

			Equation key = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Variable self = token.getThisBound();
			VarType keytype = NbtMap.myKeyType(self.type);
			Loop loop = Loop.loop(Loop.HAS);
			this.entrybuff = loop.getEntryBuff(NbtMap.getNbtCompoundType());
			this.mapbuff1 = loop.getMapBuff1(self.type);
			this.mapbuff2 = loop.getMapBuff2(self.type);
			Variable.directSet(p, s,mapbuff1, self, stack);
			this.mapbuff2.allocateLoad(p, true);
			key.compileOps(p, c, s, keytype);
			key.setVar(p, c, s, this.entrybuff.fieldMyNBTPath(KEY, keytype));
			loop.call(p);
			//Register had = loop.getHasFlag();
			
			Variable.directSet(p, s, self, this.mapbuff2, stack);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			Loop loop = Loop.loop(Loop.HAS);
			Register reg = loop.getHasFlag();
			stack.getRegister(stackstart).operation(p, "=", reg);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack) throws CompileError {
			Loop loop = Loop.loop(Loop.HAS);
			Register reg = loop.getHasFlag();
			v.setMe(p, s, reg, this.getRetType(token));
		}

		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
	private static final class Loop extends CodeGenerator {

		private static final int HAS = 0; // == 0
		private static final int ADD = 1; 
		private static final int REMOVE = -1; 
		private static final int GET = 2; //>0
		private static Loop loop = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"map/addremloop"),1);
		private static Loop loophas = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"map/testloop"),HAS);
		private static Loop loopget = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"map/getloop"),GET);
		public static Loop loop(int add) {
			switch(add) {
			case HAS: return loophas;
			case GET: return loopget;
			default: return loop;
			}
		}
		private final int kind;
		public Loop(ResourceLocation res,int kind) {
			super(res);
			this.kind=kind;
		}
		public Variable getMapBuff1(VarType type) {
			return new Variable("\"$mapbuff\"",type,null,this.res);
		}
		public Variable getMapBuff2(VarType type) {
			return new Variable("\"$mapbuff2\"",type,null,this.res);
		}
		public Variable getEntryBuff(VarType type) {
			return new Variable("\"$entry\"",type,null,this.res);
		}
		private Variable getTemp(VarType type) {
			return new Variable("\"$temp\"",type,null,this.res);
		}
		public Register getHasFlag() throws CompileError {
			RStack stack = new RStack(this.res);
			int home = stack.setNext(VarType.INT);
			Register testreg = stack.getRegister(home);
			int home2 = stack.setNext(VarType.INT);
			return stack.getRegister(home2); 
		}
		@Override
		public void build(PrintStream p, CompileJob job, Namespace ns) throws CompileError {
			RStack stack = new RStack(this.res);
			VarType elltype = VarType.BOOL;//internal only
			VarType maptype = map.mapOf(elltype,elltype);//internal only
			int home = stack.setNext(VarType.INT);
			Register testreg = stack.getRegister(home);
			int home2 = stack.setNext(VarType.INT);
			Register donereg = stack.getRegister(home2);
			Variable listbuff1 = this.getMapBuff1(maptype);//internal only
			Variable listbuff2 = this.getMapBuff2(maptype);
			Variable entry = this.getEntryBuff(elltype);
			Variable key = entry.fieldMyNBTPath(KEY, elltype);
			Variable value = entry.fieldMyNBTPath(VALUE, elltype);
			Variable temp = this.getTemp(elltype);
			Variable firstentry = listbuff1.indexMyNBTPathBasic(0,elltype);
			Variable firstkey = firstentry.fieldMyNBTPath(KEY, elltype);
			Variable firstvalue = firstentry.fieldMyNBTPath(VALUE, elltype);
			p.printf("data modify %s set from %s\n",temp.dataPhrase(), firstkey.dataPhrase());
			p.printf("execute store success score %s run data modify %s set from %s\n",testreg.inCMD(),temp.dataPhrase(), key.dataPhrase());
			
			p.printf("execute if score %s matches 0 run scoreboard players set %s 1\n",testreg.inCMD(),donereg.inCMD());
			if(this.kind == GET | this.kind == HAS)p.printf("execute if score %s matches 0 run data modify %s set from %s\n",testreg.inCMD(),value.dataPhrase(),firstvalue.dataPhrase());

			p.printf("execute unless score %s matches 0 run data modify %s append from %s\n",testreg.inCMD(),listbuff2.dataPhrase(), firstentry.dataPhrase());//remove first index tag
			p.printf("data remove %s\n", firstentry.dataPhrase());//remove first index tag
			p.printf("execute if data %s run function %s\n",firstentry.dataPhrase(), this.res);//remove first index tag
			stack.finish(job);
			
		}
		private boolean registered=false;
		public void call(PrintStream p) throws CompileError {
			RStack stack = new RStack(this.res);
			int home = stack.setNext(VarType.INT);
			Register testreg = stack.getRegister(home);//testreg.setValue(p, 0);
			int home2 = stack.setNext(VarType.INT);
			Register donereg = stack.getRegister(home2);donereg.setValue(p, 0);
			VarType listtype = map.mapOf(VarType.BOOL,VarType.BOOL);//internal only
			Variable setbuff1 = this.getMapBuff1(listtype);//internal only
			Variable setbuff2 = this.getMapBuff2(listtype);
			Variable first = setbuff1.indexMyNBTPathBasic(0, VarType.BOOL);
			Variable entry = this.getEntryBuff(VarType.BOOL);
			p.printf("execute if data %s run function %s\n",first.dataPhrase(), this.res);//remove first index tag
			
			if(this.kind == HAS || this.kind == GET) p.printf("execute if score %s matches 1.. run data modify %s prepend from %s\n",donereg.inCMD(),setbuff2.dataPhrase(), entry.dataPhrase());
			
			//bring queried elements to the top
			//self register
			if(!this.registered) {
				CodeGenerator.register(this);this.registered=true;
			}
		}
	}
	public static class MapConstructor extends BuiltinConstructor{
		public MapConstructor(String name) {
			super(name);
		}
		public MapConstructor(NbtMap clazz) {
			this(clazz.name);
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col,RStack stack) throws CompileError {
			BasicArgs a=new BuiltinFunction.BasicArgs().equations(c, line, col, matcher, stack);
			if(a.nargs() % 2 !=0) throw new CompileError("map constructor must have even number of args");
			return a;
		}

		private static final String NEW= "\"$Map\".\"$new\"";
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
			for(int i=0;i<size;i+=2) {
				//Variable arg=NbtCollection.componentOf(obj, i);
				Equation key=(Equation) ((BasicArgs)args).arg(i);
				Equation val=(Equation) ((BasicArgs)args).arg(i+1);
				Edit.getPutter(obj, key,val).call(p, c, s, stack);
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