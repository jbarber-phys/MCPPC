package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.CMath;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.NbtCollection;
import net.mcppc.compiler.struct.NbtMap;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.Equation.End;
import net.mcppc.main.Main;

/**
 * a for loop over a collection ; use the final keyword before the collection to ignore back-copying;<p>
 * syntaxes: 
 * <ul>
 * 		<li> for([type] element : collection) {...}
 * 		<li> for([type] element : final collection) {...}
 * 		<li> for([type] key,[type] key : map) {...}
 * </ul>
 * @author RadiumE13
 *
 */
public class ForIterate extends Statement implements Statement.CodeBlockOpener,Statement.Flow {
	public static ForIterate makeMe(Compiler c,Scope subscope, Matcher matcher, int line, int col,RStack stack,
			List<Token> args, List<Type> types,boolean isRef) throws CompileError {
		ForIterate me = new ForIterate(line,col,c.cursor,stack);me.isByRef = isRef;
		me.mySubscope=subscope;//let for smt supply it
		//TODO allow to pass by value
		int index;
		index = args.size()-1;
		Equation veq=(Equation) args.get(index);
		if(!veq.isRefable())throw new CompileError.UnexpectedToken(veq, "variable reference");
		me.collection=veq.getVarRef();
		index=0;
		if(args.size()<2) throw new CompileError("unexpected number of params in for - iterator stm");
		if(args.size()==3) {
			me.isMap=true;
		}
		me.enforceTypes();
		VarType collType = me.collection.type;
		me.structbuff1=me.mySubscope.addLoopLocal("\"$iteratebuff1\"",collType, c);
		if(me.isByRef)me.structbuff2=me.mySubscope.addLoopLocal("\"$iteratebuff2\"",collType, c);
		
		me.element1first=me.getElement1first();
		if(types.get(index)==null) {
			//exising counter var
			veq=(Equation) args.get(index);
			if(!veq.isRefable())throw new CompileError.UnexpectedToken(veq, "variable reference");
			me.element1=veq.getVarRef();
			
		} else {
			String name = args.get(index).asString();
			me.element1=new Variable(name,me.element1first.type,null,c).maskOtherVar(me.element1first);
			me.mySubscope.addLoopLocalRef(me.element1);
			me.newElement1=true;
		}
		if(args.size()==3) {
			index=1;
			me.element2first=me.getElement2first();
			me.element1and2first=me.getElement1and2first();
			if(types.get(index)==null) {
				//exising counter var
				veq=(Equation) args.get(index);
				if(!veq.isRefable())throw new CompileError.UnexpectedToken(veq, "variable reference");
				me.element2=veq.getVarRef();
				
			} else {
				String name = args.get(index).asString();
				me.element2=new Variable(name,me.element2first.type,null,c).maskOtherVar(me.element2first);
				me.mySubscope.addLoopLocalRef(me.element2);
				me.newElement2=true;
			}
		}else {
			me.element1and2first=me.element1first;
		}
		
		Token term=c.nextNonNullMatch(Factories.nextIsLineEnd);
		if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
		return me;
	}
	
	private void enforceTypes() throws CompileError {
		if(this.isMap) {
			if(!(this.collection.type.struct instanceof NbtMap))
				throw new CompileError("type %s not allowed in for(... , ... :...) statement; must be a Map type;".formatted(this.collection.type.asString()));
		}else {
			if(!(this.collection.type.struct instanceof NbtCollection))
				throw new CompileError("type %s not allowed in for(...:...) statement; must be a Collection type;".formatted(this.collection.type.asString()));
		}
		
	}

	private final RStack mystack;
	private Variable element1;
	private Variable element1first;
	private boolean newElement1;
	boolean isMap = false;
	private Variable element2 = null;
	private Variable element2first = null;
	private Variable element1and2first = null;
	private boolean newElement2;
	private Variable collection;
	private boolean isByRef = true;
	
	//collection buffers
	private Variable structbuff1;
	private Variable structbuff2 = null;
	Scope mySubscope;
	List<Number> span;
	public ForIterate(int line, int col,int cursor,RStack stack) {
		super(line, col, cursor);
		this.mystack=stack;
	}
	
	private Variable getElement1first() throws CompileError {
		Struct clazz = this.collection.type.struct;
		if(this.isMap) return ((NbtMap) clazz).getFirstKey(this.structbuff1);
		else return ((NbtCollection) clazz).getFirstElement(this.structbuff1);
	}
	private Variable getElement2first() throws CompileError {
		Struct clazz = this.collection.type.struct;
		if(this.isMap) return ((NbtMap) clazz).getFirstValue(this.structbuff1);
		else throw new CompileError("for map-iterator used on non-map");
	}
	private Variable getElement1and2first() throws CompileError {
		Struct clazz = this.collection.type.struct;
		if(this.isMap) return ((NbtMap) clazz).getFirstEntry(this.structbuff1);
		else throw new CompileError("for map-iterator used on non-map");
	}
	@Override
	public String getFlowType() {
		return "for"; // I think this is OK (same as for loop)
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
	@Targeted
	public void compileMe(PrintStream p, Compiler c, Scope s) throws CompileError {
		ResourceLocation mcf=this.mySubscope.getSubRes();
		Variable mybreak=this.mySubscope.getBreakVarInMe(c);
		//do not allocate element1 or 2
		mybreak.setMeToBoolean(p, s, mystack, false);
		this.structbuff1.allocateCall(p, false);
		Variable.directSet(p, s, this.structbuff1, this.collection, mystack);
		if(this.isByRef) this.structbuff2.allocateCall(p, false);
		p.printf("execute if data %s unless %s run function %s\n",this.element1first.dataPhrase(), mybreak.isTrue(),mcf);
		
		this.structbuff1.deallocateAfterCall(p);
		Variable.directSet(p, s, this.structbuff1, this.collection, mystack);
		if(this.isByRef) {
			Variable.directSet(p, s, this.collection, this.structbuff2, mystack);
			this.structbuff2.deallocateAfterCall(p);
		}
		mystack.finish(c.job);
	}

	@Override
	public String asString() {
		return "<for (... : ...)>";
	}
	@Override
	public boolean canBreak() {
		return true;
	}
	@Override
	public void addToStartOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		if(!this.newElement1) {
			Variable.directSet(p, s, this.element1, this.element1first, mystack);
		}
		if(this.isMap && !this.newElement2) {
			Variable.directSet(p, s, this.element2, this.element2first, mystack);
		}
	}
	@Override
	@Targeted
	public void addToEndOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		Variable mybreak=this.mySubscope.getBreakVarInMe(c);
		ResourceLocation mcf=this.mySubscope.getSubRes();
		if(this.isByRef && !this.newElement1) {
			Variable.directSet(p, s, this.element1first, this.element1, mystack);
		}
		if(this.isByRef && this.isMap && !this.newElement2) {
			Variable.directSet(p, s, this.element2first, this.element2, mystack);
		}
		if(this.isByRef)p.printf("data modify %s append from %s\n",structbuff2.dataPhrase(), this.element1and2first.dataPhrase());//remove first index tag
		p.printf("data remove %s\n", this.element1and2first.dataPhrase());//remove first index tag

		p.printf("execute if data %s unless %s run function %s\n",this.element1first.dataPhrase(), mybreak.isTrue(),mcf);

	}

}
