package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.function.Function; //qualify
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Function;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Coordinates;
import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Rotation;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.struct.Vector;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Statement.CodeBlockOpener;

/**
 * block opener for various facets of the /execute command: https://minecraft.fandom.com/wiki/Commands/execute
 * will have repeated func-like statements; example:
 * execute as (@r) at (Vec3d) facing (Entity) {...
 * (still lead with execute)
 * will have: align, anchored, as, at, facing, in, positioned (might merge with at), rotated, 
 * (note, rot angles are floats)
 * will not have: if, unless, store; these are reduntant / unnedded due to mcpp language features 
 * 
 * TODO consider allowing true equations in places
 * @author jbarb_t8a3esk
 *
 */
public class Execute extends Statement implements CodeBlockOpener,Statement.Flow  {
	public static final Map<String,Subgetter> SUBS = new HashMap<String,Subgetter>();
	public static void register(String name, Subgetter g) {
		SUBS.put(name, g);
	}
	 static {
			Execute.register(Align.NAME, Align::make);
			Execute.register(Anchored.NAME, Anchored::make);
			Execute.register(In.NAME, In::make);
			Execute.register(As.NAME, As::make);
			Execute.register(At.NAME, At::make);
			Execute.register(Asat.NAME, Asat::make);
			Execute.register(Positioned.NAME, Positioned::make);
			Execute.register(Facing.NAME, Facing::make);
			Execute.register(Rotated.NAME, Rotated::make);
		}
	public static abstract class Subexecute extends Token {
		public final String name;
		public Subexecute(int line, int col,String name) {//
			super(line, col);
			this.name=name;
		}
		@Override
		public String asString() {
			return "-%s ...".formatted(this.name);
		}
		public abstract void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError;
		public abstract String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError;
		public abstract void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError;
		public Anchor getNewAnchor(Compiler c, Scope s,Anchor previous, int index) throws CompileError {return previous;}
		
		
	}
	@FunctionalInterface
	public static interface Subgetter {
		//start after the name of this
		public Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError;
	}
	private static final Token.MemberName name=new Token.MemberName(-1, -1, "$execute");
	public static Execute skipMe(Compiler c, Matcher matcher, int line, int col,Keyword w) throws CompileError {
		//test for else if
		c.cursor=matcher.end();
		Execute me=new Execute(line,col,null);
		me.mySubscope = c.currentScope.subscope(me);
		Token term=Factories.carefullSkipStm(c, matcher, line, col);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	public static Execute makeMe(Compiler c, Matcher matcher, int line, int col,Keyword opener) throws CompileError {
		//test for else if
		if(opener == Keyword.EXECUTE)c.cursor=matcher.end();
		else ;//was lead with a sub name; optional behavior
		//CompileJob.compileMcfLog.printf("flow ifElse %s;\n", opener);
		Token t;
		//Equation eq=null;
		RStack stack=c.currentScope.getStackFor();
		Execute me=new Execute(line,col,stack);
		me.mySubscope = c.currentScope.subscope(me);
		final Token.Factory[] lookForSubs = Factories.genericLook(Token.BasicName.factory,Token.LineEnd.factory,Token.CodeBlockBrace.factory);
		while(true) {
			t=c.nextNonNullMatch(lookForSubs);
			if(t instanceof Token.BasicName) {
				
				//make a sub
				String name=((Token.BasicName) t).name;
				Subgetter g = SUBS.get(name);
				if (g==null)throw new CompileError.UnexpectedToken(t, "execute sub-statement name or '{'");
				Subexecute sub=g.make(c, matcher, line, col);
				me.terms.add(sub);
			}
			else if (t instanceof Token.CodeBlockBrace) {
				if((!((Token.CodeBlockBrace)t).forward))throw new CompileError.UnexpectedToken(t,"{");
				break;
			}else throw new CompileError.UnexpectedToken(t,"{");
			
		}
		return me;
	}

	//final Equation test;
	private final RStack mystack;
	Scope mySubscope;
	final List<Subexecute> terms = new ArrayList<Subexecute>();
	public Execute(int line, int col,RStack stack) {
		super(line, col);
		this.mystack=stack;
	}

	@Override
	public boolean didOpenCodeBlock() {
		return true;
	}

	@Override
	public Scope getNewScope() {
		return this.mySubscope;
	}

	@Override
	public void addToEndOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
	}

	@Override
	public void compileMe(PrintStream p, Compiler c, Scope s) throws CompileError {
		int index=0;
		for(Subexecute sub:this.terms) {
			sub.prepare(p, c, s, index++);
		}
		Anchor anchor = Anchor.DEFAULT;
		p.printf("execute ");
		index=0;
		for(Subexecute sub:this.terms) {
			p.printf("%s ", sub.getPrefix(c, s,anchor, index++));
			anchor = sub.getNewAnchor(c, s, anchor, index++);
		}
		p.printf("run ");
		p.printf("function %s\n", this.mySubscope.getSubRes());
		index=0;
		for(Subexecute sub:this.terms) {
			sub.finish(p, c, s, index++);
		}
	}

	@Override
	public String asString() {
		return "<execute ...>";
	}
	@Override
	public String getFlowType() {
		return "execute";
	}
	@Override
	public boolean canBreak() {
		return false;
	}
	public static enum Anchor{
		FEET,EYES;
		public static final Anchor DEFAULT=FEET;
		//anchor defaults to feet
		//tp always teleports your feet to the dest; execute anchored head
		@Override public String toString() {
			return super.toString().toLowerCase();
		}
		public static Anchor getNext(Compiler c,Matcher m,int line,int col) throws CompileError{
			Token t = c.nextNonNullMatch(Factories.checkForBasicName);
			if(!(t instanceof Token.BasicName)) throw new CompileError.UnexpectedToken(t, "anchor");
			String name = ((Token.BasicName) t).name;
			for(Anchor a:Anchor.values())if(a.toString().equals(name))return a;
			throw new CompileError.UnexpectedToken(t, "anchor");
		}
	}
	public static record Swizzle (boolean x,boolean y,boolean z){
		@Override public String toString() { return inCMD(); }
		public String inCMD() {
			return (x?"x":"") + (y?"y":"") + (z?"z":"");
		}
		public boolean hollow() {return !(x||y||z);}
		public Swizzle(String s) {
			this(s.contains("x"),s.contains("y"),s.contains("z"));
		}
		public static Swizzle getNext(Compiler c,Matcher m,int line,int col) throws CompileError{
			Token t = c.nextNonNullMatch(Factories.checkForBasicName);
			if(!(t instanceof Token.BasicName)) throw new CompileError.UnexpectedToken(t, "swizzle");
			String name = ((Token.BasicName) t).name;
			//no safeguards
			Swizzle s=new Swizzle(name);
			if(s.hollow()) throw new CompileError.UnexpectedToken(t, "swizzle");
			return s;
		}
	}
	public static final ResourceLocation OVERWORLD = new ResourceLocation("overworld");
	public static final ResourceLocation NETHER = new ResourceLocation("the_nether");
	public static final ResourceLocation END = new ResourceLocation("the_end");
	// The standard dimensions in the minecraft namespace are "overworld", "the_nether", and "the_end".
	public static final Map<String,ResourceLocation> DIMENSIONS = 
			Map.of("overworld",OVERWORLD
					,"nether",NETHER
					,"end",END
					//abbrevs
					,"ovw",OVERWORLD
					,"ntr",NETHER
					//vanilla names
					,"the_nether",NETHER
					,"the_end",END
					);
	public static class Align extends Subexecute {
		public static final String NAME = "align";
		public static Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError{
			BuiltinFunction.open(c);
			Swizzle a=Swizzle.getNext(c, matcher, line, col);
			if (a == null)throw new CompileError("as statment needs an anchor as input");
			boolean comma=BuiltinFunction.findArgsep(c);
			if(comma) throw new CompileError("unexpected ',' in as statement, expected a ')'");
			return new Align(line,col,a);
		}
		final Swizzle axes;
		public Align(int line, int col,Swizzle a) {
			super(line, col, NAME);
			this.axes=a;
		}
		@Override public void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		} @Override public void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		}
		@Override public String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError {
			return "align %s".formatted(this.axes);
		}
		
	}
	public static class Anchored extends Subexecute {
		public static final String NAME = "anchored";
		public static Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError{
			BuiltinFunction.open(c);
			Anchor a=Anchor.getNext(c, matcher, line, col);
			if (a == null)throw new CompileError("as statment needs an anchor as input");
			boolean comma=BuiltinFunction.findArgsep(c);
			if(comma) throw new CompileError("unexpected ',' in as statement, expected a ')'");
			return new Anchored(line,col,a);
		}
		final Anchor anchor;
		public Anchored(int line, int col,Anchor a) {
			super(line, col, NAME);
			this.anchor=a;
		}
		@Override public void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		} @Override public void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		}
		@Override public String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError {
			return "anchored %s".formatted(this.anchor);
		}
		@Override
		public Anchor getNewAnchor(Compiler c, Scope s, Anchor previous, int index) throws CompileError {
			return this.anchor;
		}
		
	}
	public static class In extends Subexecute {
		public static final String NAME = "in";
		public static Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError{
			BuiltinFunction.open(c);
			Token.Factory[] look = Factories.genericLook(ResourceLocation.ResourceToken.factory,Token.BasicName.factory);
			Token t=c.nextNonNullMatch(look);
			ResourceLocation dim =null;
			if(t instanceof ResourceLocation.ResourceToken) dim = ((ResourceLocation.ResourceToken)t).res;
			else if(t instanceof BasicName)dim=DIMENSIONS.get(((BasicName) t).name);
			
			if (dim == null)throw new CompileError.UnexpectedToken(t, "dimension");
			boolean comma=BuiltinFunction.findArgsep(c);
			if(comma) throw new CompileError("unexpected ',' in as statement, expected a ')'");
			return new In(line,col,dim);
		}
		final ResourceLocation dim;
		public In(int line, int col,ResourceLocation dim) {
			super(line, col, NAME);
			this.dim=dim;
		}
		@Override public void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		} @Override public void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		}
		@Override public String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError {
			return "in %s".formatted(this.dim);
		}
		
	}
	public static class As extends Subexecute {
		public static final String NAME = "as";
		public static Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError{
			BuiltinFunction.open(c);
			Selector sl = Entity.checkForSelectorOrEntity(c, c.currentScope, matcher, line, col);
			if (sl == null)throw new CompileError("as statment needs an entity as input");
			boolean comma=BuiltinFunction.findArgsep(c);
			if(comma) throw new CompileError("unexpected ',' in as statement, expected a ')'");
			return new As(line,col,sl);
		}
		final Selector entity;
		public As(int line, int col,Selector s) {
			super(line, col, NAME);
			this.entity=s;
		}
		@Override public void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		} @Override public void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		}
		@Override public String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError {
			return "as %s".formatted(this.entity.toCMD());
		}
		
	}
	public static class At extends Subexecute {
		public static final String NAME = "at";
		public static Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError{
			BuiltinFunction.open(c);
			Selector sl = Entity.checkForSelectorOrEntity(c, c.currentScope, matcher, line, col);
			if (sl == null)throw new CompileError("at statment needs an entity as input");
			boolean comma=BuiltinFunction.findArgsep(c);
			if(comma) throw new CompileError("unexpected ',' in as statement, expected a ')'");
			return new At(line,col,sl);
		}
		final Selector entity;
		public At(int line, int col,Selector s) {
			super(line, col, NAME);
			this.entity=s;
		}
		@Override public void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		} @Override public void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		}
		@Override public String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError {
			return "at %s".formatted(this.entity.toCMD());
		}
		
	}
	public static class Asat extends Subexecute {
		public static final String NAME = "asat";
		public static Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError{
			BuiltinFunction.open(c);
			Selector sl = Entity.checkForSelectorOrEntity(c, c.currentScope, matcher, line, col);
			if (sl == null)throw new CompileError("asat statment needs an entity as input");
			boolean comma=BuiltinFunction.findArgsep(c);
			if(comma) throw new CompileError("unexpected ',' in as statement, expected a ')'");
			return new Asat(line,col,sl);
		}
		final Selector entity;
		public Asat(int line, int col,Selector s) {
			super(line, col, NAME);
			this.entity=s;
		}
		@Override public void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		} @Override public void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			//do nothing
		}
		@Override public String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError {
			return "as %s at %s".formatted(this.entity.toCMD(),this.entity.toCMD());
		}
		
	}
	public static class Positioned extends Subexecute {
		public static final String NAME = "positioned";
		public static Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError{
			BuiltinFunction.open(c);
			ConstExprToken a1=Const.checkForExpressionSafe(c, c.currentScope, matcher, line, col, ConstType.COORDS,ConstType.NUM);
			Positioned at;
			if(a1 instanceof Coordinates.CoordToken) {
				at =new Positioned(line,col,((Coordinates.CoordToken) a1).pos);
			}
			else if(a1 instanceof Num) {
				boolean comma=BuiltinFunction.findArgsep(c);
				if(!comma) throw new CompileError("unexpected ')' in as statement, expected a ','");
				Num a2 = (Num) Num.tokenizeNextNumNonNull(c, c.currentScope, matcher, line, col);
				comma=BuiltinFunction.findArgsep(c);
				if(!comma) throw new CompileError("unexpected ')' in as statement, expected a ','");
				Num a3 = (Num) Num.tokenizeNextNumNonNull(c, c.currentScope, matcher, line, col);
				Coordinates coords = new Coordinates((Num) a1,a2,a3);
				at =new Positioned(line,col,coords);
			}else {
				int start=c.cursor;
				Selector sl = Entity.checkForSelectorOrEntity(c, c.currentScope, matcher, line, col);
				if (sl == null) {
					c.cursor=start;
					boolean relative = Token.LoneTilde.testFor(c, matcher, line, col);
					Variable vec = Variable.checkForVar(c, c.currentScope, matcher, line, col);
					if(vec==null )throw new CompileError("at statement needs a coords, 3 nums, an entity, or a Vector var to work");
					if(!vec.type.isStruct() || !(vec.type.struct instanceof Vector)) throw new CompileError("at statement needs a coords, 3 nums, an entity, or a Vector var to work");
					at = new Positioned(line,col,vec,relative); 
				}
				else at =new Positioned(line,col,sl);
			}

			boolean comma=BuiltinFunction.findArgsep(c);
			//if(comma) {
			//	at.anchor= Anchor.getNext(c, matcher, line, col);
			//	comma=BuiltinFunction.findArgsep(c);
			//} //not here
			if(comma) throw new CompileError("unexpected ',' in as statement, expected a ')'");
			return at;
		}
		private Selector entity=null;
		private Coordinates coords=null;
		//private Anchor anchor = null;
		private Variable vec = null;
		private boolean vecRelative = false;
		public Positioned(int line, int col,Selector s) {
			super(line, col, NAME);
			this.entity=s;
		}
		public Positioned(int line, int col,Coordinates pos) {
			super(line, col, NAME);
			this.coords=pos;
		}
		public Positioned(int line, int col,Variable v,boolean relative) {
			super(line, col, NAME);
			this.vec=v;
			this.vecRelative=relative;
		}

		@Override
		public void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			if(this.vec!=null) {
				this.entity=Entity.summonTemp(p, c, s, NAME,"execute");
				int prec=this.vec.type.getPrecision(s);
				Variable pos=Vector.positionOf(this.entity, prec);
				RStack stack=s.getStackFor();
				if(this.vecRelative) {
					BiOperator plus = new BiOperator(-1,-1,OpType.ADD);
					Variable.directOp(p, c, s, pos,plus, this.vec, stack);
				}else {
					Variable.directSet(p, s, pos, this.vec, stack);
				}
				stack.finish(c.job);
			}
		}

		@Override
		public String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError {
			if (this.entity!=null)return "%s as %s".formatted(NAME,this.entity.toCMD());
			else return "%s %s".formatted(NAME,this.coords.inCMD());
		}

		@Override
		public void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			if(this.vec!=null) {
				this.entity.kill(p);
			}
		}
		
	}
	public static class Facing extends Subexecute {
		public static final String NAME = "facing";
		public static Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError{
			BuiltinFunction.open(c);
			ConstExprToken a1=Const.checkForExpressionSafe(c, c.currentScope, matcher, line, col, ConstType.COORDS,ConstType.NUM);
			Facing at;
			boolean canAnchor=false;
			if(a1 instanceof Coordinates.CoordToken) {
				at =new Facing(line,col,((Coordinates.CoordToken) a1).pos);
			}
			else if(a1 instanceof Num) {
				boolean comma=BuiltinFunction.findArgsep(c);
				if(!comma) throw new CompileError("unexpected ')' in as statement, expected a ','");
				Num a2 = (Num) Num.tokenizeNextNumNonNull(c, c.currentScope, matcher, line, col);
				comma=BuiltinFunction.findArgsep(c);
				if(!comma) throw new CompileError("unexpected ')' in as statement, expected a ','");
				Num a3 = (Num) Num.tokenizeNextNumNonNull(c, c.currentScope, matcher, line, col);
				Coordinates coords = new Coordinates((Num) a1,a2,a3);
				at =new Facing(line,col,coords);
			}else {
				int start=c.cursor;
				Selector sl = Entity.checkForSelectorOrEntity(c, c.currentScope, matcher, line, col);
				if (sl == null) {
					c.cursor=start;
					boolean relative = Token.LoneTilde.testFor(c, matcher, line, col);
					Variable vec = Variable.checkForVar(c, c.currentScope, matcher, line, col);
					if(vec==null )throw new CompileError("at statement needs a coords, 3 nums, an entity, or a Vector var to work");
					if(!vec.type.isStruct() || !(vec.type.struct instanceof Vector)) throw new CompileError("at statement needs a coords, 3 nums, an entity, or a Vector var to work");
					at = new Facing(line,col,vec,relative); 
				}
				else {
					at =new Facing(line,col,sl);
					canAnchor=true;
				}
			}

			boolean comma=BuiltinFunction.findArgsep(c);
			if(comma && canAnchor) {
				at.anchor= Anchor.getNext(c, matcher, line, col);
				comma=BuiltinFunction.findArgsep(c);
			} 
			if(comma) throw new CompileError("unexpected ',' in as statement, expected a ')'");
			return at;
		}
		private Selector entity=null;
		private Coordinates coords=null;
		private Anchor anchor = null;
		private Variable vec = null;
		private boolean vecRelative = false;
		public Facing(int line, int col,Selector s) {
			super(line, col, NAME);
			this.entity=s;
		}
		public Facing(int line, int col,Coordinates pos) {
			super(line, col, NAME);
			this.coords=pos;
		}
		public Facing(int line, int col,Variable v,boolean relative) {
			super(line, col, NAME);
			this.vec=v;
			this.vecRelative=relative;
		}

		@Override
		public void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			if(this.vec!=null) {
				this.entity=Entity.summonTemp(p, c, s, NAME,"execute");
				int prec=this.vec.type.getPrecision(s);
				Variable pos=Vector.positionOf(this.entity, prec);
				RStack stack=s.getStackFor();
				if(this.vecRelative) {
					BiOperator plus = new BiOperator(-1,-1,OpType.ADD);
					Variable.directOp(p, c, s, pos,plus, this.vec, stack);
				}else {
					Variable.directSet(p, s, pos, this.vec, stack);
				}
				stack.finish(c.job);
			}
		}

		@Override
		public String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError {
			Anchor anchor = this.anchor==null?previous:this.anchor;
			if (this.entity!=null)return "%s entity %s %s".formatted(NAME,this.entity.toCMD(),anchor);
			else return "%s %s".formatted(NAME,this.coords.inCMD());
		}

		@Override
		public void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			if(this.vec!=null) {
				this.entity.kill(p);
			}
		}
		
	}
	public static class Rotated extends Subexecute {
		public static final String NAME = "rotated";
		public static Subexecute make(Compiler c, Matcher matcher, int line, int col) throws CompileError{
			BuiltinFunction.open(c);
			ConstExprToken a1=Const.checkForExpressionSafe(c, c.currentScope, matcher, line, col, ConstType.ROT,ConstType.NUM);
			Rotated at;
			if(a1 instanceof Rotation.RotToken) {
				at =new Rotated(line,col,((Rotation.RotToken) a1).rot);
			}
			else if(a1 instanceof Num) {
				boolean comma=BuiltinFunction.findArgsep(c);
				if(!comma) throw new CompileError("unexpected ')' in as statement, expected a ','");
				Num a2 = (Num) Num.tokenizeNextNumNonNull(c, c.currentScope, matcher, line, col);
				Rotation rot = new Rotation((Num) a1,a2);
				at =new Rotated(line,col,rot);
			}else {
				int start=c.cursor;
				Selector sl = Entity.checkForSelectorOrEntity(c, c.currentScope, matcher, line, col);
				if (sl == null) {
					c.cursor=start;
					Variable v1 = Variable.checkForVar(c, c.currentScope, matcher, line, col);
					boolean comma=BuiltinFunction.findArgsep(c);
					if(!comma) throw new CompileError("unexpected ')' in as statement, expected a ','");
					Variable v2 = Variable.checkForVar(c, c.currentScope, matcher, line, col);
					if(v1==null ||v2==null)throw new CompileError("at statement needs a coords, 3 nums, an entity, or a Vector var to work");
					if(!v1.type.isNumeric()) throw new CompileError("at statement needs a coords, or 2 numbers");
					if(!v2.type.isNumeric()) throw new CompileError("at statement needs a coords, or 2 numbers");
					at = new Rotated(line,col,v1,v2); 
				}
				else at =new Rotated(line,col,sl);
			}

			boolean comma=BuiltinFunction.findArgsep(c);
			//if(comma) {
			//	at.anchor= Anchor.getNext(c, matcher, line, col);
			//	comma=BuiltinFunction.findArgsep(c);
			//} //not here
			if(comma) throw new CompileError("unexpected ',' in as statement, expected a ')'");
			return at;
		}
		private Selector entity=null;
		private Rotation rot=null;
		//private Anchor anchor = null;
		private Variable rot1 = null;//yaw
		private Variable rot2 = null;//pitch
		public Rotated(int line, int col,Selector s) {
			super(line, col, NAME);
			this.entity=s;
		}
		public Rotated(int line, int col,Rotation rot) {
			super(line, col, NAME);
			this.rot=rot;
		}
		public Rotated(int line, int col,Variable v1,Variable v2) {
			super(line, col, NAME);
			this.rot1=v1;
			this.rot2=v2;
		}

		@Override
		public void prepare(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			if(this.rot1!=null) {
				this.entity=Entity.summonTemp(p, c, s, NAME,"execute");
				int prec1=this.rot1.type.getPrecision(s);
				Variable ang1=Rotation.ang1Of(entity, prec1);
				int prec2=this.rot1.type.getPrecision(s);
				Variable ang2=Rotation.ang2Of(entity, prec2);
				RStack stack=s.getStackFor();
				Variable.directSet(p, s, ang1, this.rot1, stack);
				Variable.directSet(p, s, ang2, this.rot2, stack);
				stack.finish(c.job);
			}
		}

		@Override
		public String getPrefix(Compiler c, Scope s,Anchor previous, int index) throws CompileError {
			if (this.entity!=null)return "%s as %s".formatted(NAME,this.entity.toCMD());
			else return "%s %s".formatted(NAME,this.rot.inCMD());
		}

		@Override
		public void finish(PrintStream p, Compiler c, Scope s, int index) throws CompileError {
			if(this.rot1!=null) {
				this.entity.kill(p);
			}
		}
		
	}
}
