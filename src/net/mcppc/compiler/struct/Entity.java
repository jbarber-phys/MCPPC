package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.Selector.SelectorToken;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.Struct;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;

/**
 * a struct that uses tags to create an assignable selector abstraction
 * can be set to track a single entity or multiple (can then append to)
 * @author jbarb_t8a3esk
 *
 */
public class Entity extends Struct {
	public static final Entity entity;
	public static final Entity entities;
	static {
		entity = new Entity("Entity");
		entities = new Entity("Entities");
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
	public String getNBTTagType(VarType varType) {
		return null;//this should never be called
		//if it is called, let the nullpointer crash the program as punishment
	}
	//private static final Pattern SLASH = Pattern.compile("\\/"); // \/
	private static final Pattern NOTALLOWED = Pattern.compile("[^\\w.+-]"); // [^\w.+-]

	/**
	 * a unique tag that this var uses as a tracker
	 * In Java Edition, it must be in a single word (Allowed characters include: -, +, ., _, A-Z, a-z, and 0-9)
	 * see https://minecraft.fandom.com/wiki/Commands/tag#Arguments
	 * @param self
	 * @return
	 */
	public String getScoreTag(Variable self) throws CompileError{
		String s=self.getHolder()+"."+self.getAddress();
		return NOTALLOWED.matcher(s).replaceAll("+");
	}

	@Override public boolean canMask(VarType mytype, Mask mask) { return false; }

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return 0;
	}


	@Override
	protected String getJsonTextFor(Variable self) throws CompileError {
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

	@Override
	public void allocate(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		if(fillWithDefaultvalue)this.clear(p, var);
	}

	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		throw new CompileError("defaultValue() does not exist for void;");
	}

	@Override
	public boolean hasField(String name, VarType mytype) {
		return false;
	}

	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}

	@Override
	public boolean hasBuiltinMethod(String name, VarType mytype) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public BuiltinStructMethod getBuiltinMethod(Variable self, String name) throws CompileError {
		// TODO Auto-generated method stub
		return null;
	}
	public static Selector checkForSelectorOrEntity(Compiler c,Scope s, Matcher matcher, int line, int col) throws CompileError {
		ConstExprToken slc=Const.checkForExpressionSafe(c, s, matcher, line, col, ConstType.SELECTOR);
		if(slc!=null) return ((Selector.SelectorToken)slc ).selector(); 
		return Entity.checkForEntityVar(c, s, matcher, line, col);
		
	}
	public static Selector checkForEntityVar(Compiler c,Scope s, Matcher matcher, int line, int col) throws CompileError {
		int start=c.cursor;
		Token vn = c.nextNonNullMatch(Factories.checkForMembName);
		if(!(vn instanceof Token.MemberName)) {
			c.cursor=start; return null;
		}
		
		((Token.MemberName) vn).identify(c, s);
		Variable v=((Token.MemberName) vn).getVar();
		//TODO functions;
		if(v.type.isStruct() && v.type.struct instanceof Entity) {
			return ((Entity)v.type.struct).getSelectorFor(v);
		}else return null;
		
	}
	public static Selector summonTemp(PrintStream p,Compiler c,Scope s,Object... hash) throws CompileError {
		String tag = "mcppc+temp%x".formatted(Objects.hash(hash));
		final String type = "marker";//https://minecraft.fandom.com/wiki/Marker
		String args = "type=%s,tag=%s,limit=1".formatted(type,tag);
		p.printf("summon %s ~ ~ ~ {Tags: [\"%s\"]}\n", type,tag);
		Selector e= new Selector("@e",args);
		return e;
	}

}
