package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.mcppc.compiler.McThread;
import net.mcppc.compiler.NbtPath;
import net.mcppc.compiler.StructTypeParams.MembTypePair;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.RuntimeError;
import net.mcppc.compiler.functions.Size;
import net.mcppc.compiler.struct.NbtCollection.Clear;
import net.mcppc.compiler.struct.NbtList.ChangeAt;
import net.mcppc.compiler.struct.NbtList.GetAt;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Keyword;
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

	public Variable getFirstKey(Variable self) throws CompileError {
		return self.indexMyNBTPathBasic( 0,NbtMap.getNbtCompoundType()).fieldMyNBTPath(KEY, NbtMap.myKeyType(self.type));
	}

	public Variable getFirstValue(Variable self) throws CompileError {
		return self.indexMyNBTPathBasic( 0,NbtMap.getNbtCompoundType()).fieldMyNBTPath(VALUE, NbtMap.myValueType(self.type));
	}
	public Variable getFirstEntry(Variable self) throws CompileError {
		return self.indexMyNBTPathBasic( 0,NbtMap.getNbtCompoundType());
	}
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
			//TODO make it bool instead
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, s, matcher, line, col, stack);
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
			
			//put if absent will still bring the entry to the top, but let it get overwritten by the value already there
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
			//TODO test this to see if terms ever duplicate or fail to add
			//remove first element of 
			
			Variable.directSet(p, s, self, this.mapbuff2, stack);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			//TODO allow ret value (bool) and allow optional arg for ref-value removed / old value
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
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, s, matcher, line, col, stack);
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
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, s, matcher, line, col, stack);
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
	/**
	 * takes in map buffer 1 as input and removes the front elements (putting them into buffer 2)
	 * until the first remaining element matches the key (then exit);
	 * also sets a flag for if a key was detected;
	 * any recombination or back copying is done by the caller;
	 * @author jbarb_t8a3esk
	 *
	 */
	private static final class Loop extends CodeGenerator {
		//iterates over a list, splitting it into the front (buff 2) and the element of inter
		private static final int HAS = 0; // == 0
		private static final int ADD = 1; 
		private static final int REMOVE = -1; 
		private static final int GET = 2; //>0
		private static Loop loopadd = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"map/addloop"),ADD);
		private static Loop loopremove = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"map/remloop"),REMOVE);
		private static Loop loophas = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"map/testloop"),HAS);
		private static Loop loopget = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"map/getloop"),GET);
		public static Loop loop(int add) {
			switch(add) {
			case HAS: return loophas;
			case GET: return loopget;
			case REMOVE: return loopremove;
			default: return loopadd;
			}
		}
		private final int kind;
		public Loop(ResourceLocation res,int kind) {
			super(res);
			this.kind=kind;
		}
		public Variable getMapBuff1(VarType type) {
			//this buffer will have the first elements removed until a match is found
			//initially a copy of input
			return new Variable("\"$mapbuff\"",type,null,this.res);
		}
		public Variable getMapBuff2(VarType type) {
			//initially empty
			//will have unused elements appended to it
			return new Variable("\"$mapbuff2\"",type,null,this.res);
		}
		public Variable getEntryBuff(VarType type) {
			//the entry input to test for
			//the key to search for and possibly also a value to insert
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
			//the below line back copies the entry value for reading
			if(this.kind == GET | this.kind == HAS | this.kind == REMOVE)p.printf("execute if score %s matches 0 run data modify %s set from %s\n",testreg.inCMD(),value.dataPhrase(),firstvalue.dataPhrase());
			
			//now copy over UNLESS the key did match
			p.printf("execute unless score %s matches 0 run data modify %s append from %s\n",testreg.inCMD(),listbuff2.dataPhrase(), firstentry.dataPhrase());//append first to buff2
			p.printf("data remove %s\n", firstentry.dataPhrase());//remove first index tag from buff1
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
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line,int col, RStack stack) throws CompileError {
			BasicArgs a=new BuiltinFunction.BasicArgs().equations(c, s, line, col, matcher, stack);
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
	public static class ThreadUuidLookupTable {
		
		
		//will leak memory if entity dies unexpectedly but can occasionally clean the list by as @e[tag=threadtag]: if uuid matches; maintaing;; else remove
		
		private Variable lookupTable;
		private Variable defaultVariables;
		private Variable myEntry;
		private Variable myUuid;//this.UUID
		
		private McThread thread;
		
		
		public ThreadUuidLookupTable(McThread thread) throws CompileError {
			String holder = thread.getStoragePath();
			VarType compound = TagCompound.TAG_COMPOUND;
			VarType uuid = Uuid.uuid.getType();
			VarType lookupType = map.mapOf(uuid, compound);
			this.lookupTable = new Variable("$lookup",lookupType,null,Mask.STORAGE, holder,"\"$lookup\"");
			this.defaultVariables = new Variable("$default",compound,null,Mask.STORAGE, holder,"\"$defaults\"");
			this.myEntry = new Variable("$running",compound,null,Mask.STORAGE, holder,"\"$running\"");
			this.myUuid = new Variable("$uuid",uuid,null,thread.getBasePath());
			thread.thisNbt(this.myUuid, NbtPath.UUID);
			this.thread=thread;
		}
		
		private Map<Integer,Variable> privateBlockAllocators = new HashMap<Integer,Variable>();
		private List<Variable> varAllocators = new ArrayList<Variable>();
		
		public Variable editSubVar(Variable v, int block) throws CompileError {
			if(v.access != Keyword.PRIVATE) block=-1;
			VarType valuetype = NbtMap.myValueType(lookupTable.type);
			String name = v.name;
			VarType type = v.type;
			Variable base = this.myEntry.fieldMyNBTPath(VALUE, valuetype);
			Variable allocatorBase=this.defaultVariables;
			if(block!=-1) {
				String blocktag = "\"$%s\"".formatted(block);
				base = base.fieldMyNBTPath(blocktag, valuetype);
				allocatorBase = base.fieldMyNBTPath(blocktag, valuetype);
				if (!privateBlockAllocators.containsKey(block)) 
					privateBlockAllocators.put(block, allocatorBase);
				
			}
			//add allocator
			Variable allocator = allocatorBase.fieldMyNBTPath(name, type).makeStorageOfThreadRunner();
			this.varAllocators.add(allocator);
			
			
			return v.maskOtherVar(base.fieldMyNBTPath(name, type));
		}
		public Variable getBlockAllocator(int block) {
			assert block!=-1;
			//all with nonvolatile private vars
			VarType valuetype = NbtMap.myValueType(lookupTable.type);
			Variable base=this.defaultVariables.fieldMyNBTPath(String.valueOf(block), valuetype);
			return base.makeStorageOfThreadRunner();
		}
		//internal
		
		public void onLoad(PrintStream p) throws CompileError {
			//clean up any leaked entries from before the reload
			this.lookupTable.allocateLoad(p, true);
			this.defaultVariables.allocateLoad(p, true);
			
			//clean this as well by removing it
			this.myEntry.deallocateLoad(p);
			
			//then call allocate for all allocator blocks and variables
			for(Variable b:this.privateBlockAllocators.values()) b.allocateLoad(p, true);
			for(Variable v:this.varAllocators) v.allocateLoad(p, true);
			
		}
		public void initMyVars(Compiler c,Scope s,PrintStream p,RStack stack) throws CompileError {

			VarType compound = TagCompound.TAG_COMPOUND;
			VarType uuid = Uuid.uuid.getType();
			Variable truestartUuid = new Variable("$uuidstart",uuid,null,thread.getBasePath());
			thread.thisNbtTruestart(truestartUuid, NbtPath.UUID);
			this.myEntry.deallocateLoad(p);
			Variable key = this.myEntry.fieldMyNBTPath(KEY, uuid);
			Variable val = this.myEntry.fieldMyNBTPath(VALUE, compound);
			
			this.myEntry.allocateLoad(p, true);
			Variable.directSet(p, s, key, truestartUuid, stack);
			Variable.directSet(p, s, val, this.defaultVariables, stack);
			p.printf("data modify %s prepend from %s\n",this.lookupTable.dataPhrase(), this.myEntry.dataPhrase());
			this.myEntry.deallocateLoad(p);
		}
		public void retrieve(Compiler c,Scope s,PrintStream p,RStack stack) throws CompileError {
			
			VarType keytype = NbtMap.myKeyType(lookupTable.type);
			Loop loop = Loop.loop(Loop.REMOVE);//remove the value involved and copy it to entry buffer
			Variable entrybuff = loop.getEntryBuff(NbtMap.getNbtCompoundType());
			Variable keyBuff = entrybuff.fieldMyNBTPath(KEY, keytype);
			Variable valueBuff = entrybuff.fieldMyNBTPath(VALUE, keytype);
			Variable mapbuff1 = loop.getMapBuff1(lookupTable.type);
			Variable mapbuff2 = loop.getMapBuff2(lookupTable.type);
			Variable.directSet(p, s,mapbuff1, lookupTable, stack);

			
			mapbuff2.allocateLoad(p, true);
			RuntimeError.printf(p, "execute if data %s run ".formatted(this.myEntry.dataPhrase()),
					"error: thread failed to finish and push its local vars; values will be lost;");
			
			Variable.directSet(p, s,keyBuff, myUuid, stack);

			 //Register sizebuff = stack.getRegister(stack.setNext(VarType.INT));
			 //Size.lengthOf(p, sizebuff, mapbuff1);
			 //RuntimeError.printf(p, "", "lookup length before: %s", sizebuff);
			 //RuntimeError.printf(p, "", "lookup before: %s", mapbuff1);
			
			loop.call(p); 
			
			 //Size.lengthOf(p, sizebuff, mapbuff2);
			 //RuntimeError.printf(p, "", "lookup length after: %s", sizebuff);
			 //RuntimeError.printf(p, "", "lookup after: %s", mapbuff2);
			 //RuntimeError.printf(p, "", "entry after: %s", entrybuff);
			
			
			Register had = loop.getHasFlag();//was it found
			//if not then throw exception
			//RuntimeError.printf(p, "execute if score %s matches 0 run ".formatted(had.inCMD()), "made vars for %s",this.myUuid);
			//if absent: allocate default vars:
			p.printf("execute if score %s matches 0 run ", had.inCMD()); 
				Variable.directSet(p, s, valueBuff, this.defaultVariables, stack);
			
			
			
			//back copy
			Variable.directSet(p, s, this.myEntry, entrybuff, stack);
			Variable.directSet(p, s, this.lookupTable, mapbuff2, stack);
			//RuntimeError.printf(p,"", "finished pull");
		}
		public void reinsert(Compiler c,Scope s,PrintStream p,RStack stack) throws CompileError {
			p.printf("data modify %s prepend from %s\n",this.lookupTable.dataPhrase(), this.myEntry.dataPhrase());
			//RuntimeError.printf(p, "", "entry pushed: %s", this.myEntry);
			this.myEntry.deallocateLoad(p);
			//RuntimeError.printf(p,"", "finished push");
		}
		public void finalizeMe(Compiler c,Scope s,PrintStream p,RStack stack) throws CompileError {
			//call this before calling kill command on executor
			//prevents memory leaks
			this.myEntry.deallocateLoad(p);
			//RuntimeError.printf(p, "", "finalized %s",this.myUuid);
		}
		//thread might need to generate a clean event
	}
}
