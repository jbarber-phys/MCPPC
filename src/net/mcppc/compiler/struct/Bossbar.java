package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CMath;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.StructTypeParams;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.StructTypeParams.MembType;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Type;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Num;


/**
 * variable that masks a bossbar value;
 * @author RadiumE13
 *
 */

public class Bossbar extends Struct {
	public static final Bossbar bossbar = new Bossbar("Bossbar");
	private static final Bossbar bossbarMax = new Bossbar("BossbarMax",false);//don't register this
	public static final int DEFAULT_PRECISION = 0;//seperate from fixed floats default
	public static final Set<String> BOSSBAR_COLORS = Set.of("blue","green","pink","purple","red","white","yellow");
	public static final Set<String> BOSSBAR_STYLES = Set.of("notched_6","notched_10","notched_12","notched_20","progress");
	public static void registerAll() {
		Struct.register(bossbar);
		//don't register max; it is internal only
	}
	private final boolean isValue;
	public Bossbar(String name) {
		this(name, true);
	}
	public Bossbar(String name,boolean isValue) {
		//numeric and floatp;
		//but precision could be zero (and defaults to that)
		super(name, true, true, false);
		this.isValue = isValue;
	}

	@Override
	public String getNBTTagType(VarType varType) {
		return VarType.Builtin.INT.getTagTypeSafe();
	}

	public boolean canMask(VarType mytype, Mask mask) {
		return mask ==Mask.BOSSBAR;
	}
	public StructTypeParams tokenizeTypeArgs(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError {
		//precision
		StructTypeParams.PrecisionType pc=StructTypeParams.PrecisionType.tokenizeTypeArgs(c,s, matcher, line, col,forbidden);
		return pc;
	}
	@Override
	public StructTypeParams paramsWNoArgs() throws CompileError {
		return new StructTypeParams.PrecisionType(DEFAULT_PRECISION);
	}
	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return ((StructTypeParams.PrecisionType) mytype.structArgs).precision;
	}
	@Override
	public VarType withPrecision(VarType vt,int newPrecision) throws CompileError {
		StructTypeParams pms=new StructTypeParams.PrecisionType(newPrecision);
		return new VarType(this, pms);
	}
	public VarType withPrecision(int newPrecision) throws CompileError {
		StructTypeParams pms=new StructTypeParams.PrecisionType(newPrecision);
		return new VarType(this, pms);
	}
	@Override
	public String getPrecisionStr(VarType mytype)  {
		StructTypeParams.PrecisionType pms=((StructTypeParams.PrecisionType) mytype.structArgs);
		return pms.getPrecisionStr();
	}

	@Override
	public VarType withTemplatePrecision(VarType vt,String pc) throws CompileError {
		StructTypeParams pms=new StructTypeParams.PrecisionType(pc);
		return new VarType(this, pms);
	}
	@Override
	public String asString(VarType varType) {
		return this.headerTypeString(varType);
	}
	@Override
	public String getJsonTextFor(Variable variable) throws CompileError {
		throw new CompileError("cannot json-print the bossbar value of %s".formatted(variable.name));
	}
	@Override public boolean canBeRecursive(VarType type) {
		return false;
	}
	@Override
	public int sizeOf(VarType mytype) {
		return 1;
	}
	public VarType floatify(VarType type) {
		StructTypeParams.PrecisionType pms=((StructTypeParams.PrecisionType) type.structArgs);
		return pms.floatify(VarType.DOUBLE);
	}
	@Override
	public boolean canCasteFrom(VarType from, VarType mytype) {
		//the first line could cause problems if: bossbar = bossbarmax;
		if(from.struct instanceof Bossbar ) {
			if(((Bossbar) from.struct).isValue !=this.isValue)return false;
			from = this.floatify(from);
		}
		mytype = this.floatify(mytype);
		return VarType.canCast(from, mytype);
		
	}
	@Override
	public boolean canCasteTo(VarType to, VarType mytype) {
		if(to.struct instanceof Bossbar ) {
			if(((Bossbar) to.struct).isValue !=this.isValue)return false;
			to = this.floatify(to);
		}
		mytype = this.floatify(mytype);
		return VarType.canCast(mytype, to);
	}
	@Override
	public void castRegistersTo(PrintStream p, Scope s, RStack stack, int start, VarType newType, VarType mytype)
			throws CompileError {
		VarType to = newType;
		if(to.struct instanceof Bossbar ) {
			to = this.floatify(to);
		}
		mytype = this.floatify(mytype);
		stack.castRegister(p, s, start, to);
	}

	@Override
	public void castRegistersFrom(PrintStream p, Scope s, RStack stack, int start, VarType old, VarType mytype)
			throws CompileError {
		VarType from = old;
		if(from.struct instanceof Bossbar ) {
			from = this.floatify(from);
		}
		mytype = this.floatify(mytype);
		stack.castRegister(p, s, start, mytype);
	}
	//convert self to a float if put on the stack
	@Override public VarType getTypeOnStack(VarType mytype, VarType typeWanted) {
		return this.floatify(mytype);
	}
	/*
	 * 
	 * holder is ID, address is default name
	 * 		and the associated regexes
	 * 
	 */
	@Override public Variable varInit(Variable v,Compiler c,Scope s) throws CompileError {
		String path = c.resourcelocation.path + "__/" + v.name;
		String vname = "\"%s\"".formatted(v.name);
		return v.maskBossbar(new ResourceLocation(c.resourcelocation.namespace,path),vname , true,true);
	}
	public String getBossBarId(Variable self) {
		return self.getHolder();
	}
	public String getBossBarNameLiteral(Variable self) {
		return self.getAddress();
	}
	public String getBossBarField(Variable self) {
		return this.isValue? "value": "max";
	}
	public String getBossbarGet(Variable self,Scope s) {
		
		return "bossbar get %s %s".formatted(this.getBossBarId(self),this.getBossBarField(self));
	}
	public String getBossbarStore(Variable self,Scope s) {
		
		return "execute store result bossbar %s %s".formatted(this.getBossBarId(self),this.getBossBarField(self));
	}
	
	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted) throws CompileError {
		String bid = this.getBossBarId(me);
		String subvar = this.getBossBarField(me);
		Register reg = stack.getRegister(home);
		String subcmd = this.getBossbarGet(me, s);
		reg.setToResult(p, subcmd);
		
	}

	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		String store = this.getBossbarStore(me, s);
		VarType regType = stack.getVarType(home);
		VarType myType = me.getType();

		Register reg = stack.getRegister(home);
		int ptot = -regType.getPrecision(s)+myType.getPrecision(s);
		if(ptot==0) {
			p.printf("%s run ", store);
				reg.getValueCmd(p);
		} else {

			double mult=Math.pow(10, ptot);
			String iholder = "mcppc:scoreholder__/dumps___";
			String ipath = "\"$dump\""; //do not base this on the address
			p.println("execute store result storage %s %s %s %s run scoreboard players get %s"
					.formatted(iholder,ipath,this.getNBTTagType(myType),CMath.getMultiplierFor(mult),reg.inCMD()));
			p.println("%s run data get storage %s %s"
					.formatted(store,iholder,ipath));
		}
		

	}

	@Override
	public void getMeDirect(PrintStream p,Scope s,RStack stack,Variable to, Variable me)throws CompileError{
		//int ptot = -regType.getPrecision(s)+myType.getPrecision(s);
		int ptot;
		String bossbarGet = this.getBossbarGet(me, s);
		if(to.isStruct() && to.type.struct instanceof Bossbar) {
			String bossbarStore = ((Bossbar)to.type.struct).getBossbarStore(to, s);
			ptot = -me.type.getPrecision(s)+to.type.getPrecision(s);
			if(ptot==0) {
				p.printf("%s run %s\n",bossbarStore, bossbarGet);
			} else {

				double mult=Math.pow(10, ptot);
				String iholder = "mcppc:scoreholder__/dumps___";
				String ipath = "\"$dump\""; //do not base this on the address
				p.printf("execute store result storage %s %s %s %s run %s\n",
						iholder,ipath,this.getNBTTagType(to.type),CMath.getMultiplierFor(mult),bossbarGet);
				p.printf("%s run data get storage %s %s\n",
						bossbarStore,iholder,ipath);
			}
		} else switch( to.getMaskType()){
		case BLOCK:
		case ENTITY:
		case STORAGE: {
			ptot = me.type.getPrecision(s);
			double mult = Math.pow(10, -ptot);//To NBT
			String subcmd = bossbarGet;
			p.printf("execute store result %s %s %s run %s\n", to.dataPhrase(),to.type.getNBTTagType(),CMath.getMultiplierFor(mult),subcmd);
		}
			break;
		case SCORE:{
			ptot = -me.type.getPrecision(s)+to.type.getPrecision(s);
			if(ptot==0) {
				p.printf("execute store result score %s %s run %s\n",to.scorePhrase(), bossbarGet);
			} else {

				double mult=Math.pow(10, ptot);
				String iholder = "mcppc:scoreholder__/dumps___";
				String ipath = "\"$dump\""; //do not base this on the address
				p.printf("execute store result storage %s %s %s %s run %s\n",
						iholder,ipath,this.getNBTTagType(to.type),CMath.getMultiplierFor(mult),bossbarGet);
				p.printf("execute store result score %s run data get storage %s %s\n",
						to.scorePhrase(),iholder,ipath);
			}
		}
			break;
		default:
			break;
		
		}
	}
	@Override
	public void setMeDirect(PrintStream p,Scope s,RStack stack,Variable me, Variable from)throws CompileError{
		String bossbarStore = this.getBossbarStore(me, s);
		int ptot;
		if(from.isStruct() && from.type.struct instanceof Bossbar) {
			((Bossbar)from.type.struct).getMeDirect(p, s, stack, me, from);
			return;
		}else switch( from.getMaskType()){
		case BLOCK:
		case ENTITY:
		case STORAGE: {
			ptot = me.type.getPrecision(s);
			double mult = Math.pow(10, ptot);//From NBT
			String subcmd = from.dataGetCmd(mult);
			p.printf("%s run %s\n", bossbarStore,subcmd);
		}
			break;
		case SCORE:{
			ptot = -from.type.getPrecision(s)+me.type.getPrecision(s);
			if(ptot==0) {
				p.printf("%s run %s\n",bossbarStore, from.scoreGetCmd());
			} else {

				double mult=Math.pow(10, ptot);
				String iholder = "mcppc:scoreholder__/dumps___";
				String ipath = "\"$dump\""; //do not base this on the address
				p.printf("execute store result storage %s %s %s %s run %s\n",
						iholder,ipath,this.getNBTTagType(me.type),CMath.getMultiplierFor(mult),from.scoreGetCmd());
				p.printf("%s run data get storage %s %s\n",
						bossbarStore,iholder,ipath);
			}
		}
			break;
		default:
			break;
		
		}
	}
	@Override public boolean canSetToExpr(ConstExprToken e) {
		return e.constType()==ConstType.NUM;
	}
	@Override public void setMeToExpr(PrintStream p,Scope s,RStack stack,Variable v, ConstExprToken t) throws CompileError {
		Number num = ((Num)t).value;
		int pc = this.getPrecision(v.type, null);
		double mult = Math.pow(10, pc);
		int value = (int)(num.doubleValue()*mult);
		p.printf("bossbar set %s %s %d\n", this.getBossBarId(v),this.getBossBarField(v),value);
	}
	public void setVarToNumber(PrintStream p,Scope s,RStack stack, Number val,Variable self) throws CompileError {
		int pc = this.getPrecision(self.type, s);
		double mult = Math.pow(10, pc);
		int value = (int)(val.doubleValue()*mult);
		p.printf("bossbar set %s %s %d\n", this.getBossBarId(self),this.getBossBarField(self),value);
	}

	@Override
	public boolean canDoBiOp(OpType op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		return op.isNumeric;
	}

	@Override
	public void doBiOpFirst(OpType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		this.doBiOp(op, p, c, s, stack, home1, home2);
	}

	@Override
	public void doBiOpSecond(OpType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		this.doBiOp(op, p, c, s, stack, home1, home2);
	}
	private void doBiOp(OpType op, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		VarType type1 = stack.getVarType(home1);
		VarType type2 = stack.getVarType(home2);
		if(type1.isStruct() && type1.struct instanceof Bossbar) {
			type1 = this.floatify(type1);
			stack.setVarType(home1, type1);
		}
		if(type2.isStruct() && type2.struct instanceof Bossbar) {
			type2 = this.floatify(type2);
			stack.setVarType(home2, type2);
		}
		BiOperator opt = new BiOperator(-1,-1,op);
		//run op on floats
		op.perform(p, c, s, stack, home1, home2, opt);
		
	}
	
	@Override
	public void allocateLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		p.printf("bossbar remove %s\n", this.getBossBarId(var));
		p.printf("bossbar add %s %s\n", this.getBossBarId(var),this.getBossBarNameLiteral(var));
		p.printf("bossbar set %s visible false\n", this.getBossBarId(var));
	}

	@Override
	public void allocateCall(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		// do nothing
	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		// should never be used
		return null;
	}
	private static final String MAX = "max";
	@Override
	public boolean hasField(Variable self, String name) {
		if (!this.isValue) return false;
		return name.equals(MAX);
	}

	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		if (!this.isValue) throw new CompileError("bossbar max has no sub fields");
		VarType maxtype = new VarType(Bossbar.bossbarMax,self.type.structArgs);
		return new Variable("max",maxtype,null,Mask.BOSSBAR,self.getHolder(),self.getAddress());
	}
	private final Map<String,BuiltinFunction> bfs = Map.of(
			SetConstMember.setName.name,SetConstMember.setName,
			SetConstMember.setPlayers.name,SetConstMember.setPlayers,
			SetConstMember.setVisible.name,SetConstMember.setVisible,
			SetMember.color.name,SetMember.color,
			SetMember.style.name,SetMember.style,
			SetMember.show.name,SetMember.show,
			SetMember.hide.name,SetMember.hide
			);
	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		//System.err.printf("method %s,%s()\n".formatted(self.name,name));
		if (!this.isValue) return false;
		return bfs.containsKey(name);
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		if (!this.isValue) throw new CompileError("bossbar max has no sub fields");
		return bfs.get(name);
	}
	public static abstract class SetConstMember extends BuiltinFunction{
		public static final SetConstMember setName = new SetConstMember("setName","field",true,ConstType.STRLIT) {
			@Override protected String defaultValue(Compiler c, Scope s, BFCallToken token, RStack stack) {
				BasicArgs args = (BasicArgs) token.getArgs();
				Variable self = token.getThisBound();
				Bossbar struct = (Bossbar) self.type.struct;
				return struct.getBossBarNameLiteral(self);
			}};
		public static final SetConstMember setVisible = new SetConstMember("setVisible","visible",true,ConstType.BOOLIT) {
			@Override protected String defaultValue(Compiler c, Scope s, BFCallToken token, RStack stack) {
				return "true";
			}};
		public static final SetConstMember setPlayers = new SetConstMember("setPlayers","players",true,ConstType.SELECTOR) {
			@Override protected String defaultValue(Compiler c, Scope s, BFCallToken token, RStack stack) {
				return Selector.AT_A.toCMD();
			}};
		private final Const.ConstType ctype;
		private final boolean hasArg;
		private final String field;
		public SetConstMember(String name,String field,boolean hasArg,ConstType ctype) {
			super(name);
			this.ctype=ctype;
			this.hasArg = hasArg;
			this.field = field;
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

		protected abstract String defaultValue(Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError;
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			BasicArgs args = (BasicArgs) token.getArgs();
			Variable self = token.getThisBound();
			Bossbar struct = (Bossbar) self.type.struct;
			if(args.nargs()==0) {
				p.printf("bossbar set %s %s %s\n",struct.getBossBarId(self), this.field,this.defaultValue(c, s, token, stack));
			}else if (!this.hasArg) throw new CompileError("too many args in %s.%s()".formatted(struct.name,this.name));
			Equation val = (Equation) args.arg(0);
			val.constify(c, s);
			if(!val.isConstable()) throw new CompileError("could not get const value for arg to %s.%s(...)".formatted(struct.name,this.name));
			ConstExprToken ce = val.getConst();
			if(ce.constType()!=this.ctype) throw new CompileError("arg to %s.%s(...) had wrong type %s, needed %s"
					.formatted(struct.name,this.name,ce.constType().name,ctype.name));
			String str = ce.textInMcf();
			p.printf("bossbar set %s %s %s\n",struct.getBossBarId(self), this.field,str);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {}

		@Override public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {}

		@Override public Number getEstimate(BFCallToken token) { return null; }

		@Override public boolean isNonstaticMember() {
			return true;
		}
		
	}
	public static class SetMember extends BuiltinFunction{
		public static final SetMember show = new SetMember("show","visible","true",null);
		public static final SetMember hide = new SetMember("hide","visible","false",null);
		public static final SetMember color = new SetMember("color","color",null,Bossbar.BOSSBAR_COLORS);
		public static final SetMember style = new SetMember("style","style",null,Bossbar.BOSSBAR_STYLES);
		private final String value;
		private final String field;
		private final Set<String> allowed;
		public SetMember(String name,String field,String value,Set<String> vals) {
			super(name);
			this.value=value;
			this.field = field;
			this.allowed=vals;
		}
		@Override
		public VarType getRetType(BFCallToken token) {
			return VarType.VOID;
		}
		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
				throws CompileError {
			if(this.value!=null) return BuiltinFunction.tokenizeArgsNone(c, matcher, line, col);
			BuiltinFunction.NameArgs args = new BuiltinFunction.NameArgs();
			args.names(c, s, line, col, matcher, stack);
			int wargs = this.value==null? 1:0;
			if(wargs!=args.nargs()) throw new CompileError("wrong number of args in %s; expected %d but got %d;".formatted(this.name,wargs,args.nargs()));
			return args;
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			Args args =  token.getArgs();
			Variable self = token.getThisBound();
			Bossbar struct = (Bossbar) self.type.struct;
			String val = this.value;
			if(val==null) val = ((NameArgs) args).arg(0);
			if(this.allowed!=null && !this.allowed.contains(val)) throw new CompileError("invalid %s of %s given".formatted(this.field,val));
			p.printf("bossbar set %s %s %s\n",struct.getBossBarId(self), this.field,val);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
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
		@Override
		public boolean isNonstaticMember() {
			return true;
		}
		
	}
}
