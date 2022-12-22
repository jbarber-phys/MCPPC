package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Coordinates;
import net.mcppc.compiler.Coordinates.CoordToken;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.Selector.SelectorToken;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.BuiltinFunction.BasicArgs;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.ResourceLocation.ResourceToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.MemberName;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;

/**
 * a struct that uses tags to create an assignable selector abstraction
 * can be set to track a single entity or multiple (can then append to)
 * @author jbarb_t8a3esk
 *
 *MAYBETODO make recurivable using a score for my entities;
 */
public class Entity extends Struct {
	public static final Entity entity;
	public static final Entity entities;
	static {
		entity = new Entity("Entity",false);
		entities = new Entity("Entities",true);
	}
	public static void registerAll() {
		//TODO java.lang.NullPointerException: Cannot read field "name" because "net.mcppc.compiler.struct.Entity.entity" is null
		//System.err.println(entity.name);
		Struct.register(entity);
		Struct.register(entities);
	}
	
	
	private final boolean many;
	public Entity(String name) {
		this(name,false);
	}
	public Entity(String name,boolean many) {
		super(name);
		this.many=many;
	}
	@Override 
	public boolean canCompareTags(VarType type,VarType otherType) {
		return false;
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return null;//this should never be called
		//if it is called, let the nullpointer crash the program as punishment
	}
	//private static final Pattern SLASH = Pattern.compile("\\/"); // \/
	public static final Pattern TAGCHAR_NOTALLOWED = Pattern.compile("[^\\w.+-]"); // [^\w.+-]

	/**
	 * a unique tag that this var uses as a tracker
	 * In Java Edition, it must be in a single word (Allowed characters include: -, +, ., _, A-Z, a-z, and 0-9)
	 * see https://minecraft.fandom.com/wiki/Commands/tag#Arguments
	 * @param self
	 * @return
	 */
	public String getScoreTag(Variable self) throws CompileError{
		String s=self.getHolder()+"."+self.getAddressToGetset(); //ignore the index
		if(self.getHolder().length()==0) s=self.getAddressToGetset();
		return TAGCHAR_NOTALLOWED.matcher(s).replaceAll("+");
	}

	@Override public boolean canMask(VarType mytype, Mask mask) { return false; }

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return 0;
	}
	@Override
	public String getPrecisionStr(VarType mytype)  {
		return "";
	}


	@Override
	public String getJsonTextFor(Variable self) throws CompileError {
		return this.getSelectorFor(self).getJsonText();
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
	@Override public void getMeDirect(PrintStream p,Scope s,RStack stack,Variable to, Variable me)throws CompileError{
		Entity.directSet(p, s, stack, to, me);
	}
	@Override public void setMeDirect(PrintStream p,Scope s,RStack stack,Variable me, Variable from)throws CompileError{
		Entity.directSet(p, s, stack, me, from);
	}
	
	public static void directSet(PrintStream p,Scope s,RStack stack,Variable to, Variable from)throws CompileError{
		if(!from.type.isStruct() ||!(from.type.struct instanceof Entity))throw new CompileError.UnsupportedCast(from.type, to.type);
		if(!to.type.isStruct() ||!(to.type.struct instanceof Entity))throw new CompileError.UnsupportedCast(from.type, to.type);
		Entity st1 = (Entity) to.type.struct;
		Entity st2 = (Entity) from.type.struct;
		if(!st1.many && st2.many)throw new CompileError.UnsupportedCast(from.type, to.type);
		st1.clear(p, to);
		st1.add(p, to, st2.getSelectorFor(from));
		
	}

	@Override public boolean canSetToExpr(ConstExprToken e) {
		return e.constType()==ConstType.SELECTOR;
	}
	@Override public void setMeToExpr(PrintStream p,RStack stack,Variable self, ConstExprToken e) throws CompileError {
		if(e.constType()!=ConstType.SELECTOR)throw new CompileError.UnsupportedCast(e, self.type);
		Selector.SelectorToken t = (SelectorToken) e;
		this.clear(p, self);
		this.add(p, self, t.selector());
	}
	private void clear(PrintStream p,Variable self) throws CompileError {
		p.printf("tag @e[] remove %s\n", this.getScoreTag(self));
	}
	private void add(PrintStream p,Variable self,Selector entity) throws CompileError {
		p.printf("tag %s add %s\n",entity.toCMD(), this.getScoreTag(self));
	}
	private void sub(PrintStream p,Variable self,Selector entity) throws CompileError {
		p.printf("tag %s remove %s\n",entity.toCMD(), this.getScoreTag(self));
	}
	private static final String TEMP="mcppc+entity+temp__";
	private static final String TEMP2="mcppc+entity+temp2__";
	private void compareEqual(PrintStream p, Compiler c, Scope s, RStack stack, int dest, Selector left, Selector right,boolean not) throws CompileError {
		Register out=stack.getRegister(dest);
		out.setValue(p, !not);
		String prefix = "execute if entity @e[tag=%s] run ".formatted(TEMP);
		p.printf("tag * remove %s\n", TEMP);
		p.printf("tag %s add %s\n",right.toCMD(), TEMP);
		p.printf("tag %s remove %s\n",left.toCMD(), TEMP);
		p.printf(prefix);out.setValue(p, not);
		p.printf("tag * remove %s\n", TEMP);
		p.printf("tag %s add %s\n",left.toCMD(), TEMP);
		p.printf("tag %s remove %s\n",right.toCMD(), TEMP);
		p.printf(prefix);out.setValue(p, not);
		p.printf("tag * remove %s\n", TEMP);
	}
	private void compareOverlap(PrintStream p, Compiler c, Scope s, RStack stack, int dest, Selector left, Selector right,boolean not) throws CompileError {
		Register out=stack.getRegister(dest);
		out.setValue(p, not);
		String slctr = "@e[tag=%s]".formatted(TEMP);
		String prefix = "execute if entity @e[tag=%s,tag=%s] run ".formatted(TEMP,TEMP2);
		p.printf("tag * remove %s\n", TEMP);
		p.printf("tag * remove %s\n", TEMP2);
		p.printf("tag %s add %s\n",left.toCMD(), TEMP);
		p.printf("tag %s add %s\n",right.toCMD(), TEMP2);
		p.printf(prefix);out.setValue(p, !not);
		p.printf("tag * remove %s\n", TEMP);
		p.printf("tag * remove %s\n", TEMP2);
	}
	public Selector getSelectorFor(Variable self) throws CompileError {
		String tag="tag=%s".formatted(this.getScoreTag(self));
		String args = tag;
		if (!this.many) args+= " , "+"limit=1";
		return new Selector("@e", args);
	}

	@Override public boolean canBeRecursive(VarType type) {
		return false;
	}
	@Override
	public void allocateLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		if(fillWithDefaultvalue)this.clear(p, var);
	}
	@Override
	public void allocateCall(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		//do nothing yet
	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		throw new CompileError("defaultValue() does not exist for void;");
	}

	@Override
	public boolean hasField(Variable self, String name) {
		return false;
	}

	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}
	public static Map<String,BuiltinFunction> BFS = Map.of(
			Summon.instance.name,Summon.instance
			,Kill.instance.name,Kill.instance
			,Count.exists.name,Count.exists
			,Count.count.name,Count.count
			,Clear.instance.name,Clear.instance
			);
	@Override 
	public boolean hasBuiltinMethod(Variable self, String name) {
		return super.hasBuiltinMethodBasic(name, BFS);
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return super.getBuiltinMethodBasic(self,name, BFS);
	}
	public static Selector checkForSelectorOrEntity(Compiler c,Scope s, Matcher matcher, int line, int col) throws CompileError {
		ConstExprToken slc=Const.checkForExpressionSafe(c, s, matcher, line, col, ConstType.SELECTOR);
		if(slc!=null) return ((Selector.SelectorToken)slc ).selector(); 
		return Entity.checkForEntityVar(c, s, matcher, line, col);
		
	}
	public static Token checkForSelectorOrEntityToken(Compiler c,Scope s, Matcher matcher, int line, int col) throws CompileError {
		ConstExprToken slc=Const.checkForExpressionSafe(c, s, matcher, line, col, ConstType.SELECTOR);
		if(slc!=null) return ((Selector.SelectorToken)slc ); 
		return Entity.checkForEntityToken(c, s, matcher, line, col);
		
	}
	public static Selector getSelectorFor(Token t) throws CompileError {
		return getSelectorFor(t,false);
	}
	public static Selector getSelectorFor(Token t,boolean requireSingle) throws CompileError {
		if (t instanceof MemberName) {
			if(!(((MemberName) t).getVar().type.isStruct()) || !(((MemberName) t).getVar().type.struct instanceof Entity))
				throw new CompileError.UnsupportedCast(((MemberName) t).getVar().type, ConstType.SELECTOR);
			Entity struct=(Entity) ((MemberName) t).getVar().type.struct;
			if(struct.many && requireSingle) throw new CompileError("Token %s refers to many entities, must only refer to one".formatted(t.asString()));
			return t==null?null:((Entity)((MemberName) t).getVar().type.struct).getSelectorFor(((MemberName) t).getVar());
		}else if (t instanceof Selector.SelectorToken) {
			return ((Selector.SelectorToken)t ).selector();
		}else throw new CompileError.UnexpectedToken(t, "entity / selector");
	}
	public static Selector checkForEntityVar(Compiler c,Scope s, Matcher matcher, int line, int col) throws CompileError {
		MemberName vt=Entity.checkForEntityToken(c, s, matcher, line, col);
		return vt==null?null:((Entity)vt.getVar().type.struct).getSelectorFor(vt.getVar());
	}
	public static MemberName checkForEntityToken(Compiler c,Scope s, Matcher matcher, int line, int col) throws CompileError {
		int start=c.cursor;
		Token vn = c.nextNonNullMatch(Factories.checkForMembName);
		if(!(vn instanceof MemberName)) {
			c.cursor=start; return null;
		}
		
		if(!((MemberName) vn).identifySafe(c, s)) {
			c.cursor=start;
			return null;
		}
		Variable v=((MemberName) vn).getVar();
		//TODO functions;
		if(v.type.isStruct() && v.type.struct instanceof Entity) {
			return (MemberName) vn;//((Entity)v.type.struct).getSelectorFor(v);
		}else {
			c.cursor=start;
			return null;
		}
		
	}
	public static final ResourceLocation ENTITY_TYPE_MARKER = new ResourceLocation(CompileJob.MINECRAFT,"marker");
	//TODO improve hash
	public static Selector summonTemp(PrintStream p,Compiler c,Scope s,Object... hash) throws CompileError {
		String tag = "mcppc+temp%x".formatted(Objects.hash(hash));
		//final String type = "marker";//https://minecraft.fandom.com/wiki/Marker
		String args = "type=%s,tag=%s,limit=1".formatted(ENTITY_TYPE_MARKER,tag);
		p.printf("summon %s ~ ~ ~ {Tags: [\"%s\"]}\n", ENTITY_TYPE_MARKER,tag);
		Selector e= new Selector("@e",args);
		return e;
	}
	public static class Summon extends BuiltinFunction {
		public static Summon instance = new Summon("summon");
		public Summon(String name) {
			super(name);
		}
		public boolean isNonstaticMember() {
			return true;
		}

		@Override
		public VarType getRetType(BFCallToken token) {
			return VarType.VOID;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			final Token.Factory[] lookEtype = Factories.genericCheck(ResourceLocation.ResourceToken.factory);
			BasicArgs args=new BasicArgs();
			Token etype=c.nextNonNullMatch(lookEtype);
			boolean comma=false;
			if(etype instanceof ResourceLocation.ResourceToken) {
				args.add("etype", etype);
				if(!BuiltinFunction.findArgsep(c)) {
					return args;
				}
				comma=true;
			}else {
				etype = new ResourceLocation.ResourceToken(-1,-1,ENTITY_TYPE_MARKER);
				args.add("etype", etype);
			}
			final Token.Factory[] wild20 = {Token.WildChar.dontPassFactory20};
			//System.err.printf("pos: '%s';\n", c.nextNonNullMatch(wild20).asString());
			ConstExprToken pos = Const.checkForExpressionSafe(c, c.currentScope, matcher, line, col, ConstType.COORDS);
			if(pos==null) {
				pos = Coordinates.ATME;
				if (comma) {
					throw new CompileError("unexpected ',' after entity type but no pos after");
				}
			}
			args.add("pos",pos);
			
			if(!BuiltinFunction.findArgsep(c)) {
				return args;
			}
			throw new CompileError("unexpected ',' after args");
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			Variable v=token.getThisBound();
			if(v==null)throw new CompileError("function %s must be called as a nonstaic member".formatted(this.name));
			if(!(v.isStruct() && v.type.struct instanceof Entity))throw new CompileError("function %s must be called from an entity object".formatted(this.name));
			Entity clazz = (Entity) v.type.struct;
			ResourceLocation.ResourceToken etype = (ResourceToken) ((BasicArgs) token.getArgs()).arg("etype");
			//System.out.printf("etype = %s\n", etype.asString());
			Coordinates.CoordToken pos = (CoordToken) ((BasicArgs) token.getArgs()).arg("pos");
			String tag = clazz.getScoreTag(v);
			if(!clazz.many) {
				//free existing
				clazz.clear(p, v);
			}
			p.printf("summon %s %s {Tags: [\"%s\"]}\n", etype.asString(),pos.pos.inCMD(),tag);
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
	public static class Kill extends BuiltinFunction{
		public static Kill instance = new Kill("kill");
		public Kill(String name) {
			super(name);
		}
		public boolean isNonstaticMember() {
			return true;
		}
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
			Variable v=token.getThisBound();
			if(v==null)throw new CompileError("function %s must be called as a nonstaic member".formatted(this.name));
			if(!(v.isStruct() && v.type.struct instanceof Entity))throw new CompileError("function %s must be called from an entity object".formatted(this.name));
			Entity clazz = (Entity) v.type.struct;
			Selector slc = clazz.getSelectorFor(v);
			p.printf("kill %s\n", slc.toCMD());
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
	public static class Count extends BuiltinFunction{
		public static final String SUCCESS="success";
		public static final String RESULT="result";
		public static Count exists = new Count("exists",VarType.BOOL,SUCCESS);
		public static Count count = new Count("count",VarType.INT,RESULT);
		public final VarType rtype;
		public final String cmdRetType;
		public Count(String name,VarType ret, String cmdRetType) {
			super(name);
			this.rtype=ret;
			this.cmdRetType=cmdRetType;
			
		}
		public boolean isNonstaticMember() {
			return true;
		}
		@Override
		public VarType getRetType(BFCallToken token) {
			return this.rtype;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			return BuiltinFunction.tokenizeArgsNone(c, matcher, line, col);
		}

		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			//do nothing (yet)
		}
		/*
		 * https://gaming.stackexchange.com/questions/365931/how-to-count-entities-with-commands-check-if-there-are-only-one-or-a-certain-num
		 * /execute store result score @s entities if entity @e
		 * the last bit acts as a testfor statement
		 */
		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			Variable self=token.getThisBound();
			if(self==null)throw new CompileError("function %s must be called as a nonstaic member".formatted(this.name));
			if(!(self.isStruct() && self.type.struct instanceof Entity))throw new CompileError("function %s must be called from an entity object".formatted(this.name));
			Entity clazz = (Entity) self.type.struct;
			Selector slc = clazz.getSelectorFor(self);
			int home=stackstart;
			Register h=stack.getRegister(home);
			//mc 1.13 has no testfor equivalent
			//p.printf("execute store %s score %s run data get entity %s\n",this.cmdRetType, h.inCMD(),slc.toCMD());
			p.printf("execute store %s score %s if entity %s\n",this.cmdRetType, h.inCMD(),slc.toCMD());
			stack.setVarType(home, this.getRetType(token));
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			Variable self=token.getThisBound();
			if(self==null)throw new CompileError("function %s must be called as a nonstaic member".formatted(this.name));
			if(!(self.isStruct() && self.type.struct instanceof Entity))throw new CompileError("function %s must be called from an entity object".formatted(this.name));
			Entity clazz = (Entity) self.type.struct;
			Selector slc = clazz.getSelectorFor(self);
			int home=stack.reserve(1);
			Register h=stack.getRegister(home);
			p.printf("execute store %s score %s if entity %s\n",this.cmdRetType, h.inCMD(),slc.toCMD());
			stack.setVarType(home, this.getRetType(token));
			v.setMe(p, s, stack, home);
		}

		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		
	}
	public static class Clear extends BuiltinFunction{
		public static Clear instance = new Clear("clear");
		public Clear(String name) {
			super(name);
		}
		public boolean isNonstaticMember() {
			return true;
		}
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
			Variable v=token.getThisBound();
			if(v==null)throw new CompileError("function %s must be called as a nonstaic member".formatted(this.name));
			if(!(v.isStruct() && v.type.struct instanceof Entity))throw new CompileError("function %s must be called from an entity object".formatted(this.name));
			Entity clazz = (Entity) v.type.struct;
			Selector slc = clazz.getSelectorFor(v);
			clazz.clear(p, v);
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
