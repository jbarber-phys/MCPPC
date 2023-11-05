package net.mcppc.compiler.tokens;


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Function.FuncCallToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.struct.*;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Equation.End;
import net.mcppc.compiler.tokens.UnaryOp.UOType;

/**
 * a numeric equation;
 * currently does not support string equations; if its added, it will be a seperate token
 * TODO (a+b).method()
 * @author RadiumE13
 *
 */
public class Equation extends Token  implements TreePrintable,INbtValueProvider{
	private static final boolean PRE_EVAL_EXP = false;
	public static Equation toAssign(int line,int col,Compiler c,Scope s, Matcher m) throws CompileError {
		Equation e=new Equation(line,col,c);
		e.isTopLevel=true;
		e.wasOpenedWithParen=false;
		e=e.populate(c, s, m);
		return e;
	}
	public static Equation toAssignGoto(int line,int col,Compiler c,Matcher m) throws CompileError {
		Equation e=new Equation(line,col,c);
		e.isTopLevel=true;
		e.wasOpenedWithParen=false;
		McThread thread = c.currentScope.getThread();
		if(thread ==null)throw new CompileError("goto cannot be assigned outside a thread");
		Integer i = thread.checkForBlockNumberName(c, c.currentScope, m);
		if(i==null) throw new CompileError("invalid block name at line %s col %s".formatted(line,col));
		Num num = new Num(line,col,i,VarType.INT);
		e.elements.add(num);
		Statement.nextIsLineEnder(c, m, false);
		e.end=End.STMTEND;
		return e;
	}

	public static Equation toAssignHusk(RStack stack,Token... tokens) throws CompileError {
		Equation e=new Equation(-1,-1,stack);
		e.isTopLevel=true;
		e.wasOpenedWithParen=false;
		e.doesAnyOps=tokens.length>=2;
		e.startsWithUniOp = tokens.length==2 && tokens[0] instanceof UnaryOp;
		for(Token t: tokens)e.elements.add(t);
		return e;
	}
	public static Equation toArgue(int line,int col,Compiler c,Matcher m, Scope s) throws CompileError {
		Equation e=new Equation(line,col,c);
		e.isTopLevel=false;
		e.wasOpenedWithParen=false;
		e.isAnArg=true;
		e=e.populate(c, s, m);
		return e;
	}
	public static Equation toArgueHusk(RStack stack,Token... tokens) throws CompileError {
		Equation e=new Equation(-1,-1,stack);
		e.isTopLevel=false;
		e.isAnArg=true;
		e.wasOpenedWithParen=false;
		e.doesAnyOps=tokens.length>=2;
		e.startsWithUniOp = tokens.length==2 && tokens[0] instanceof UnaryOp;
		for(Token t: tokens)e.elements.add(t);
		return e;
	}
	public static Equation subEqHusk(RStack stack,Token... tokens) throws CompileError {
		Equation e=new Equation(-1,-1,stack);
		e.isTopLevel=false;
		e.isAnArg=false;
		e.wasOpenedWithParen=true;
		e.doesAnyOps=tokens.length>=2;
		e.startsWithUniOp = tokens.length==2 && tokens[0] instanceof UnaryOp;
		for(Token t: tokens)e.elements.add(t);
		e.end=End.CLOSEPAREN;
		
		return e;
	}
	public boolean wasLastArg() throws CompileError {
		switch(this.end) {
		case ARGSEP:
			return false;
		case CLOSEPAREN:
			return true;
		default:
			throw new CompileError("equation that was an arg ended with ';' or '{'");
		
		}
	}
	//true if this is the outermost equation
	public boolean isTopLevel=false;
	public boolean wasOpenedWithParen=false;
	public boolean isAnArg=false;
	//true if there are any unneeded parens or if any ops are done; can be used for scoreboard avoidance if equation is trival (eg, a=b but not a=(b))
	boolean doesAnyOps=false;
	//if true, will not allow any stack operations for this equation as it is required to dissolve into a const
	boolean forceConst = false;
	//true if this is an arg AND found a 
	//boolean wasLastArg=true;//dont; this is encapsulated with end
	//no expected type yet; must aggregate first
	public boolean isCast=false;
	public boolean startsWithUniOp=false;

	public static enum End{
		STMTEND,
		ARGSEP,
		CLOSEPAREN,
		BLOCKBRACE,
		INDEXBRACE,
		INOP,//colon
		LATEROP//this one is excluded from token
	}
	public End end=null;
	//End expected_end=null;//this call is made by the enclosing thing
	OperationOrder lastOp=OperationOrder.ALL;
	/**
	 * map containing register_index : vartype
	 */

	
	final RStack stack;
	public Equation(int line, int col,Compiler c) {
		super(line, col);
		this.stack = new RStack(c.resourcelocation,c.currentScope);
		this.isTopLevel=true;
	}
	public Equation(int line, int col,RStack stack) {
		super(line, col);
		this.stack=stack;
		this.isTopLevel=false;
	}
	@Override
	public String asString() {
		return "(+-*/%...)";
	}
	static final Token.Factory[] lookForValue= Factories.genericLook(
			UnaryOp.factory,
			Bool.factory,
			Selector.SelectorToken.factory,//new edition; may interfere with indexing
			MemberName.factory,Num.factory,CommandToken.factory,
			Token.Paren.factory,//sub-eq or caste
			Token.LineEnd.factory, Token.CodeBlockBrace.factory,Token.ArgEnd.factory, //possible terminators; unexpected
			Token.StringToken.factory//just in case of trivial equations
			);
	
	static final Token.Factory[] lookForOperation= Factories.genericLook(
			Token.Member.factory,
			Token.Paren.factory,//func call
			Token.IndexBrace.factory, //index of list-struct
			Token.ForInSep.factory,
			BiOperator.factory,
			Token.LineEnd.factory, Token.CodeBlockBrace.factory,Token.ArgEnd.factory //possible terminators
			);
	public List<Token> elements=new ArrayList<Token>();
	private boolean claimedSubEnd = false;
	
	private Number getNegativeNumberLiteral() {
		if(this.elements.size()!=2)return null;
		if(!(this.elements.get(0) instanceof UnaryOp))return null;
		UnaryOp op=(UnaryOp) this.elements.get(0);
		if(!op.isUminus())return null;
		if(!(this.elements.get(1) instanceof Num))return null;
		Number n1=((Num)this.elements.get(1)).value;
		return CMath.uminus(n1);
	}

	public boolean isConstable() {
		if(this.isNumber())return true;
		//if(this.isSelectable())return true;
		if(this.elements.size()==1) {
			Token in = this.elements.get(0);
			if(in instanceof Const.ConstExprToken) return true;
			if(this.elements.get(0) instanceof MemberName) {
				VarType type=((MemberName)this.elements.get(0)).var.type;
				if (!type.isStruct() || !(type.struct.isConstEquivalent(type)))return false;
				return true;
			}
		}
		return false;
	}
	public Const.ConstExprToken getConst() throws CompileError {
		if(this.isNumber())return this.getNumToken();
		//if(this.isSelectable())return this.getSelectorToken();
		if(this.elements.get(0) instanceof MemberName) {
			Variable v = ((MemberName)this.elements.get(0)).var;
			VarType type=v.type;
			if (type.isStruct() && (type.struct.isConstEquivalent(type)))
				return type.struct.getConstEquivalent(v, line, col);
		}
		return (ConstExprToken) this.elements.get(0);
	}
	public Const.ConstExprToken getConstNbt(){
		if(this.isNumber())return this.getNumToken();
		if(this.isSelectable())return null;
		return (ConstExprToken) this.elements.get(0);
	}
	private Num getNumToken() {
		Number n1=this.getNumber();
		Num old = (Num) this.elements.get(this.elements.size()-1);
		return new Num(this.line,this.col,n1,old.type);
	}

	public boolean isNumber() {
		if(this.elements.size()==1) {
			if(!(this.elements.get(0) instanceof Num))return false;
			return true;
		}
		if(this.elements.size()!=2)return false;
		if(!(this.elements.get(0) instanceof UnaryOp))return false;
		UnaryOp op=(UnaryOp) this.elements.get(0);
		if(!op.isUminus())return false;
		if(!(this.elements.get(1) instanceof Num))return false;
		
		return true;
	}
	public boolean isSelectable() {
		if(this.elements.size()==1) {
			if((this.elements.get(0) instanceof Selector.SelectorToken))return true;
			if(!(this.elements.get(0) instanceof MemberName))return false;
			VarType type=((MemberName)this.elements.get(0)).var.type;
			if (!type.isStruct() || !(type.struct instanceof Entity))return false;
			return true;
		}
		return false;
	}
	@Deprecated
	private Selector getSelector() throws CompileError {
		if(this.elements.size()==1) {
			if((this.elements.get(0) instanceof Selector.SelectorToken))
				return ((Selector.SelectorToken) this.elements.get(0)).selector();
			if(!(this.elements.get(0) instanceof MemberName))return null;
			VarType type=((MemberName)this.elements.get(0)).var.type;
			if (!type.isStruct() || !(type.struct instanceof Entity))return null;
			return ((Entity)type.struct).getSelectorFor(((MemberName)this.elements.get(0)).var);
		}
		return null;
	}
	@Deprecated
	private Selector.SelectorToken getSelectorToken() throws CompileError {
		return new Selector.SelectorToken(line, -1, this.getSelector());
	}
	public Number getNumber() {
		if(this.elements.size()==1) {
			if((this.elements.get(0) instanceof Num))return ((Num)this.elements.get(0)).value;
			return null;
		}
		return this.getNegativeNumberLiteral();
	}
	public boolean isRefable() {
		if(this.elements.size()!=1)return false;
		if(!(this.elements.get(0) instanceof MemberName))return false;
		return true;
	}
	public boolean isConstRefable() {
		if(this.isRefable())return true;
		if(this.elements.size()!=1)return false;
		if(!(this.elements.get(0) instanceof Function.FuncCallToken))return false;
		return true;
	}
	public Variable getVarRef(){
		if(!this.isRefable())return null;
		MemberName core=(MemberName) this.elements.get(0);
		return core.var;
	}
	private Variable getVarRef(Token t){
		if(!(t instanceof MemberName))return null;
		MemberName core=(MemberName) t;
		return core.var;
	}
	public Variable getConstVarRef(){
		//a function return var
		//warning: avoid passing to other functions as other ops may be performed
		if(this.isRefable())return this.getVarRef();
		if(!this.isConstRefable())
			return null;
		Function.FuncCallToken core=(Function.FuncCallToken) this.elements.get(0);
		return core.getRetConstRef();
	}
	private Variable getConstVarRef(Token t) throws CompileError{
		if((t instanceof MemberName)) return getVarRef(t);
		if(!(t instanceof Function.FuncCallToken)) return null;
		Function.FuncCallToken core=(Function.FuncCallToken) t;
		return core.getRetConstRef();
	}
	
	public Equation populate(Compiler c,Scope s, Matcher m) throws CompileError {

		return populate(c,s,m, 0);
	}
	private Token nextValueNoType(Compiler c,Matcher m) throws CompileError {
		Token v=c.nextNonNullMatch(lookForValue);
		//may be wildchar if is indexable invalid selector
		if(v instanceof Token.WildChar) v = c.nextNonNullMatch(Factories.checkForMembName);
		return v;
	}
	/**
	 * fills the equation with members from the matcher.
	 * Also identifies any variables or functions it finds
	 * @param c
	 * @param s 
	 * @param m
	 * @param recurrs
	 * @return
	 * @throws CompileError
	 */
	public Equation populate(Compiler c,Scope s,Matcher m, int recurrs) throws CompileError {
		if(recurrs==10)Warnings.warning("equation recurred 10 times; warning", c);
		if(recurrs==20)throw new CompileError("equation recurred 20 times; overflow for debug purposes;");
		while(true) {
			//look for value / unary
			int pc=c.cursor;
			//Token v=c.nextNonNullMatch(lookForValue);//old
			Token v=Const.checkForExpressionSafe(c,s, m, line, col, ConstType.TYPE); 
			if(v==null) v=this.nextValueNoType(c, m);
			//boolean willAddV=false;

			//if (v instanceof Token.MemberName &&
			//		(((Token.MemberName) v).names.size()==1) &&
			//		VarType.isType(((Token.MemberName) v).names.get(0))) 
			if(v instanceof Type) {
				//typecast or constructor
				//c.cursor=pc;//setback
				//v=Type.tokenizeNextVarType(c, m,v.line , v.col);
				Token close=c.nextNonNullMatch(lookForOperation);
				if(close instanceof Token.Member) {
					//static methods
					if(!((Type)v).type.isStruct()) throw new CompileError("cannot construct non-struct type: %s;".formatted(((Type)v).type.asString()));
					Struct struct=((Type)v).type.struct;
					Token name=this.nextValueNoType(c, m);
					if(name instanceof MemberName &&
							(((MemberName) name).names.size()==1) )
						;
					else throw new CompileError.UnexpectedToken(name, "static function of struct %s".formatted(v.asString()));
					if(!struct.hasStaticBuiltinMethod((((MemberName) name).names.get(0)))) 
						throw new CompileError.UnexpectedToken(name, "static function %s not found in struct %s".formatted(name.asString(),v.asString()));
					BuiltinFunction bf=struct.getStaticBuiltinMethod((((MemberName) name).names.get(0)), ((Type)v).type);
					BuiltinFunction.open(c);
					BuiltinFunction.BFCallToken sub=BuiltinFunction.BFCallToken.make(c, s, m, v.line,v.col, this.stack, bf);
					sub.withTemplate(((Type)v).type.getTemplateArgs(s));//transfer template from arg to function
					if(sub.canConvert()) {
						v=sub.convert(c, s, this.stack);
						if(v instanceof FuncCallToken)
							((FuncCallToken) v).linkMeByForce(c, s);
						if (v instanceof Equation)
							this.doesAnyOps=true;
					}else 
						v=sub;
					//throw new CompileError("static struct members not yet supported");
				}
				else if (!(close instanceof Token.Paren)) throw new CompileError.UnexpectedToken(close,")");
				else{
					if(((Paren)close).forward) {
						//Constructors
						if(!((Type)v).type.isStruct()) throw new CompileError("cannot construct non-struct type: %s;".formatted(((Type)v).type.asString()));
						Struct struct=((Type)v).type.struct;
						BuiltinConstructor cstr=struct.getConstructor(((Type)v).type);
						BuiltinFunction.BFCallToken sub=BuiltinFunction.BFCallToken.make(c, s, m, v.line,v.col, this.stack, cstr);
						sub.withStatic(((Type)v).type);
						if(sub.canConvert()) {
							v=sub.convert(c, s, this.stack);
							if(v instanceof FuncCallToken)
								((FuncCallToken) v).linkMeByForce(c, s);
							if (v instanceof Equation)
								this.doesAnyOps=true;
						}else 
							v=sub;
						//throw new CompileError.UnexpectedToken(close,")","constructors not supported yet");
					}else {
						//typecast
						this.doesAnyOps=true;
						if(!this.wasOpenedWithParen)throw new CompileError("typecast must be of form (type(...)) but was missing open paren;");
						if(this.elements.size()>0)throw new CompileError("typecast must be of form (type(...)) but was missing open paren;");
						this.end=End.CLOSEPAREN;
						this.isCast=true;
						this.elements.add(v);
						return this.collapse();
						
					}
				}
			}
			if (v instanceof Token.Paren) {
				if(!((Paren)v).forward) {
					if(this.elements.size()==0 && this.isAnArg) {
						//empty eq with close paren, allowed in function args
						this.end=End.CLOSEPAREN;
						return this.collapse();
					}
					else throw new CompileError.UnexpectedToken(v,"(");
				}
				this.doesAnyOps=true;
				Equation sub=new Equation(v.line,v.col,this.stack);
				sub.isTopLevel=false;
				sub.wasOpenedWithParen=true;
				sub=sub.populate(c, s,m, recurrs+1);
				if(sub.end!=End.CLOSEPAREN)throw new CompileError("unexpected subeq ending %s, expected a ')'".formatted(sub.end.name()));
				
				if(sub.isCast) {
					Equation sub2=new Equation(v.line,v.col,this.stack);
					sub2.isTopLevel=false;
					sub2.wasOpenedWithParen=false;
					sub2.elements.add(sub);
					sub2.lastOp=OperationOrder.CAST;
					sub2=sub2.populate(c, s,m, recurrs+1);
					v=sub2;
					if(sub2.end!=End.LATEROP) {
						this.elements.add(sub2);
						this.end=sub2.end;
						this.claimedSubEnd =true;
						return this.collapse();
					}
				}else {
					//this.elements.add(sub);//dont add yet
					v=sub;
					//CompileJob.compileMcfLog.print("parsed eq, next text: ");
					//m.usePattern(Regexes.NEXT_10_CHAR);m.lookingAt();
					//CompileJob.compileMcfLog.println(m.group());
					//CompileJob.compileMcfLog.println(Token.asStrings(elements));
					//
				}
			}else if (v instanceof UnaryOp) {
				this.doesAnyOps=true;
				Equation sub=new Equation(v.line,v.col,this.stack);
				sub.isTopLevel=false;
				sub.wasOpenedWithParen=false;
				sub.elements.add(v);
				sub.lastOp=((UnaryOp) v).getOpOrder();
				sub=sub.populate(c, s,m, recurrs+1);
				v=sub;
				if(sub.end!=End.LATEROP) {
					this.elements.add(sub);
					this.end=sub.end;
					this.claimedSubEnd =true;
					return this.collapse();
				}
				//create sub equation w forward unary
			}else if (v instanceof Token.ArgEnd||v instanceof Token.LineEnd||v instanceof Token.CodeBlockBrace) {
				throw new CompileError("unexpected premature equation ending %s, expected a value".formatted(v.asString()));
			}else {
				//willAddV=true;
			}
			
			//now look for operation
			pc=c.cursor;
			//check for function template 
			TemplateArgsToken tempargs=null;
			if(v instanceof MemberName) {
				Function f=c.myInterface.checkForFunctionWithTemplate(((MemberName) v).names, s);
				if(f!=null) {
					tempargs=TemplateArgsToken.checkForArgs(c, s, m);
					if(tempargs==null)c.cursor=pc;
				}
			}
			Token op=c.nextNonNullMatch(lookForOperation);
			if (op instanceof Token.Paren && ((Token.Paren) op).forward) {
				//function call
				//constructor has its own hook
				BuiltinFunction bf = null;
				if(v instanceof MemberName )bf=BuiltinFunction.getBuiltinFunc(((MemberName) v).names, c, s);
				if(bf!=null) {
					//a builtin function
					BuiltinFunction.BFCallToken sub=BuiltinFunction.BFCallToken.make(c, s, m, v.line,v.col, this.stack, bf);
					if(tempargs!=null)sub.withTemplate(tempargs);
					if(bf.isNonstaticMember()) {
						List<String> nms=((MemberName) v).names;nms=nms.subList(0, nms.size()-1);
						Variable self = c.myInterface.identifyVariable(nms, s);
						sub.withThis(self);
						TemplateArgsToken typetemp = self.type.getTemplateArgs(s);
						sub.prependTemplate(typetemp);
					}
					if(sub.canConvert()) {
						v=sub.convert(c, s, this.stack);
						if(v instanceof FuncCallToken)
							((FuncCallToken) v).linkMeByForce(c, s);
						if (v instanceof Equation)
							this.doesAnyOps=true;
					}
					else 
						v=sub;
					//this.elements.add(sub);

				}
				else {
					if(!(v instanceof MemberName)) throw new CompileError.UnexpectedToken(v, "name before '('");
					//a normal function
					Function.FuncCallToken ft=Function.FuncCallToken.make(c, s, v.line, v.col, m, (MemberName) v, this.stack);
					ft.identify(c,s);
					if(tempargs!=null)ft.withTemplate(tempargs);
					ft.linkMe(c,s);
					//this.elements.add(ft);
					v=ft;
				}
				//keep going
				//willAddV=false;
				pc=c.cursor;
				op=c.nextNonNullMatch(lookForOperation);
			}
			else {
				if(v instanceof MemberName) {
					//see if it is a const
					Const cv;
					try{
						cv=c.myInterface.identifyConst((MemberName) v, s);
					}catch  (CompileError e){
						cv=null;
					}
					if(cv==null) {
						//a var
						((MemberName) v).identify(c,s);
						//do not add here
					}else {
						v=cv.getValue();
					}
					
				}
			}
			while (op instanceof Token.IndexBrace && ((Token.IndexBrace) op).forward) {
				//if(!(v instanceof Token.MemberName) )throw new CompileError("%s is not a variable and so cannot be indexed []".formatted(v.asString()));
				Token vet=VariableElementToken.make(c, s,  m, v, this.stack, v.line,v.col, false);
				v=vet;
				if(v instanceof VariableElementToken) v = ((VariableElementToken) v).convertGet(c, s);
				op=c.nextNonNullMatch(lookForOperation);
			}
			oploop: while(op instanceof BiOperator) {
				//will loop again if an equation statement just ended
				this.doesAnyOps=true;
				//willAddV=false;//this block will do it itself
				int cpr=this.lastOp.compareTo(((BiOperator)op).op.order);
				//positive if bigger
				if(cpr>0) {
					//new sub eq
					Equation sub=new Equation(v.line,v.col,this.stack);
					sub.isTopLevel=false;
					sub.wasOpenedWithParen=false;
					sub.elements.add(v);
					sub.elements.add(op);
					sub.lastOp=((BiOperator) op).getOpOrder();
					sub=sub.populate(c, s,m, recurrs+1);
					//this.elements.add(sub);
					v=sub;
					//CompileJob.compileMcfLog.printf("sub of size %s;\n", sub.elements.size());
					if(sub.end!=End.LATEROP) {
						this.elements.add(v);
						this.end=sub.end;
						this.claimedSubEnd =true;
						return this.collapse();
					}
					//keep going for another op; will loop; dont worry about the () function finder because it cannot be activated
					pc=c.cursor;
					op=c.nextNonNullMatch(lookForOperation);
					continue oploop;//retorical
				}else if (cpr<0) {
					//return 
					this.elements.add(v);
					c.cursor=pc;//reset cursor if it is later op
					this.end=End.LATEROP;
					return this.collapse();
				}else {
					//equal
					//add op to elements
					this.elements.add(v);
					this.elements.add(op);
					break oploop;
				}
						
			}
			//CompileJob.compileMcfLog.printf("post sub;\n");
			if (op instanceof Token.Paren && !((Token.Paren) op).forward) {
				//close paren (non-empty)
				this.elements.add(v);
				this.end=End.CLOSEPAREN;
				return this.collapse();
			}else if (op instanceof Token.IndexBrace && !((Token.IndexBrace) op).forward) {
				//close paren (non-empty)
				this.elements.add(v);
				this.end=End.INDEXBRACE;
				return this.collapse();
			}else if(op instanceof Token.ArgEnd) {
				this.elements.add(v);
				this.end=End.ARGSEP;
				return this.collapse();
			}else if(op instanceof Token.LineEnd) {
				this.elements.add(v);
				this.end=End.STMTEND;
				return this.collapse();
			}else if(op instanceof Token.ForInSep) {
				this.elements.add(v);
				this.end=End.INOP;
				return this.collapse();
			}else if(op instanceof Token.CodeBlockBrace) {
				if(!((CodeBlockBrace)op).forward) throw new CompileError.UnexpectedToken(op, "subequation end: '; , ) ] {'");
				this.elements.add(v);
				this.end=End.BLOCKBRACE;
				return this.collapse();
			}
			else if (!(op instanceof BiOperator)){
				if(op instanceof Token.Member) {
					throw new CompileError.UnexpectedToken(op, "(other tokens should have handled the member operator)");
				}
				//a bug
				if(c.cursor>=m.regionEnd()) {
					throw new CompileError.UnexpectedFileEnd(op.line);
				}else if (c.cursor==pc){
					throw new CompileError("infinite loop from blank token");
				}
			}
		}
		
		
	}
	private Equation collapse() {
		if (this.elements.size()==2 &&
				this.elements.get(0) instanceof UnaryOp &&
				((UnaryOp) this.elements.get(0)).isUminus() &&
				this.elements.get(1) instanceof Num) {
			Num n = this.getNumToken();
			this.elements.clear();this.elements.add(n);
		}
		if(this.elements.size()==1 && this.elements.get(0) instanceof Equation && this.claimedSubEnd) {
			//TODO bug: this detoplevels the equation
			//claimedSubEnd should not be needed after this
			Equation eq = (Equation) this.elements.get(0);
			//TODO this causes bug with DoesAnyOps being badly set to false; seems to happen in stdlib template functions
			//lots of inner functions with ops are not being given doesAnyOps=true for some reason
			//I had a look at it and i dont know why they are not being set to true;
			//but not always
			//this fixes it but i still don't know why it was broken to begin with
			eq.isTopLevel=this.isTopLevel;
			eq.isAnArg = this.isAnArg;
			eq.doesAnyOps = eq.elements.size()>=2 || eq.isCast;
			return eq;
		}
		return this;
	}
	public void printTree(PrintStream p) {
		this.printTree(p, 0);
	}
	public void printTree(PrintStream p,int tabs) {
		//for debuging
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		p.printf("%sequation(%b,%b,%b): {\n",s,this.doesAnyOps,this.isTopLevel,this.isAnArg);
		for(Token t:this.elements) {
			if(t instanceof Equation) ((Equation) t).printTree(p, tabs+1);
			else p.printf("%s\t%s\n", s,t.asString());
		}
		String s2 = this.end!=null? this.end.name(): "null";
		p.printf("%s}(ended with a %s)\n",s,s2);
		
	}
	private boolean hasSetToReg=false;
	private Integer homeReg=null;
	public VarType retype=VarType.VOID;
	// register is added by putTokenToRegister if !(in instanceof Equation) (then the sub_equation will do it)
	private int putTokenToRegister(PrintStream p,Compiler c,Scope s,VarType typeWanted,Token in) throws CompileError {
		//to a register
		int regnum;
		if (in instanceof Equation) {
			((Equation) in).compileOps(p, c, s, typeWanted);
			//regnum=((Equation) in).setReg(p, c, s, typeWanted);
			regnum=((Equation) in).setReg(p, c, s, ((Equation) in).retype);
		}else if (in instanceof MemberName) {
			regnum=stack.setNext(((MemberName) in).var.type);
			((MemberName) in).var.getMe(p,s, stack,regnum);
			this.stack.estmiate(regnum, ((MemberName) in).estimate);
		}else if (in instanceof Num) {
			regnum=stack.setNext(((Num) in).type);
			stack.getRegister(regnum).setValue(p,s, ((Num) in).value,((Num) in).type);
			this.stack.estmiate(regnum, ((Num) in).value);
		}else if (in instanceof Bool) {
			regnum=stack.setNext(((Bool) in).type);
			long score=((Bool) in).val?1:0;
			stack.getRegister(regnum).setValue(p,s, score,((Bool) in).type);
			this.stack.estmiate(regnum, null);
		}else if (in instanceof Function.FuncCallToken) {
			regnum=stack.setNext(((Function.FuncCallToken) in).getFunction().retype);
			((Function.FuncCallToken) in).call(p, c, s, stack);
			((Function.FuncCallToken) in).getRet(p, c, s, this.stack,regnum);
			this.stack.estmiate(regnum, ((Function.FuncCallToken) in).getEstimate());
		}else if (in instanceof BuiltinFunction.BFCallToken) {
			//if(((BuiltinFunction.BFCallToken) in).getBF() instanceof BuiltinConstructor)this.printStatementTree(System.err, 0);
			
			regnum=stack.setNext(((BuiltinFunction.BFCallToken) in).getRetType());
			((BuiltinFunction.BFCallToken) in).call(p, c, s, stack);
			((BuiltinFunction.BFCallToken) in).getRet(p, c, s, stack,regnum);
			this.stack.estmiate(regnum, ((BuiltinFunction.BFCallToken) in).getEstimate());
		}else if (in instanceof CommandToken) {
			regnum=this.storeCMD(p, c, s, typeWanted, in);
		}else if (in instanceof Const.ConstExprToken) {
			throw new CompileError.CannotStack((Const.ConstExprToken)in);
		}
		else {
			throw new CompileError.UnexpectedToken(in, "number, equation, variable or function");
		}
		return regnum;
	}
	public int storeCMD(PrintStream p,Compiler c,Scope s,VarType typeWanted,Token in) throws CompileError {
		int regnum;
		VarType mtype=typeWanted.isLogical()?VarType.BOOL:VarType.LONG;
		String cretype=typeWanted.isLogical()?"success":"result";
		regnum=stack.setNext(mtype);
		Register r=stack.getRegister(regnum);
		String preamble = "execute store %s score %s run".formatted(cretype,r.inCMD());
		((CommandToken)in).printToCMD(p, c, s, preamble);
		return regnum;
	}
	private ConstExprToken attemptConstConvert(Token in) throws CompileError {
		if(in instanceof Equation && ((Equation) in).isConstable()) in = ((Equation) in).getConst();
		if(in instanceof MemberName ) {
			Variable v = ((MemberName) in).var;
			VarType t = v.type;
			if(t.isStruct() && t.struct.isConstEquivalent(t)) in = t.struct.getConstEquivalent(v, in.line, in.col);
		}
		if(in instanceof ConstExprToken) return (ConstExprToken) in;
		return null;
	}
	/**
	 * determines if it is possible to collapse this Equation into a const expression, and does so if true
	 * @return weather this equation is now a const
	 * @throws CompileError
	 */
	public boolean constify (Compiler c,Scope s) throws CompileError {
		if(this.isConstable()) return true;
		if(this.isCast) return false;
		for (Token t:this.elements) if(t instanceof Equation){
			((Equation) t).constify(c, s);
		}
		if(this.elements.get(0) instanceof UnaryOp) {
			UnaryOp op=(UnaryOp) this.elements.get(0);
			Token in=this.elements.get(1);
			if(in instanceof Equation && ((Equation) in).isConstable()) in = ((Equation) in).getConst();
			if(in instanceof ConstExprToken && Const.hasUniOp(op.getOpType(), ((ConstExprToken)in).constType())) {
				ConstExprToken out = Const.doUniOp(op.getOpType(), ((ConstExprToken)in));
				this.elements.clear();this.elements.add(out);
				this.doesAnyOps = false;
				return true;
			}else {
				//do nothing
			}
		}
		else if(this.elements.get(0) instanceof Equation &&  ((Equation)this.elements.get(0)).isCast) {
			Type cast=(Type) ((Equation)this.elements.get(0)).elements.get(0);
			Token in=this.elements.get(1);
			//TODO see if a const cast is possible
		}else if (this.elements.size()==1){
			Token in=this.elements.get(0);
			if(in instanceof Equation && ((Equation) in).isConstable()) {
				in =  ((Equation) in).elements.get(0);//get element directly to not break ref
				this.elements.set(0, in);
				//this.doesAnyOps = ((Equation) in).doesAnyOps;
				this.doesAnyOps = false;
				return true;
			}
			//variables know they are const now
		}else if (this.elements.size()==0){
			if(!this.isAnArg)throw new CompileError("unexpected blank equation at line %s col %s".formatted(this.line,this.col));
		}else if (this.elements.size()%2 ==1){
			//remember that exps go in reverse (correct power tower)
			ConstExprToken left = this.attemptConstConvert(this.elements.get(0));
			if(left==null)return false;
			List<Number> exponents=new ArrayList<Number>(); 
			for(int i=1;i<this.elements.size()-1;i+=0) {//list will shrink but i stays the same
				Token opt=this.elements.get(i);if(!(opt instanceof BiOperator))throw new CompileError.UnexpectedToken(opt, "bi-operator");
				BiOperator op=(BiOperator) opt;
				Token nextval=this.elements.get(i+1);
				if(op.op==BiOperator.OpType.EXP) {
					Number e;
					if(nextval instanceof Equation) {
						e=((Equation)nextval).getNegativeNumberLiteral();
						if(e==null) throw new CompileError.UnexpectedToken(nextval, "number or neg-number; avoid nested exp ints;");
					}else if (nextval instanceof Num) {
						e=((Num)nextval).value;
					}else throw new CompileError.UnexpectedToken(nextval, "number", "var-exponents not supported");
					exponents.add(e);
					this.elements.remove(1);//op
					this.elements.remove(1);//right
				}else {
					ConstExprToken right = this.attemptConstConvert(nextval);
					if(right == null) return false;
					if(!Const.hasBiOp(left.constType(), op.op, right.constType())) return false;
					left = Const.doBiOp(left, op.op, right);
					//left -> right operator justified
					this.elements.set(0, left);
					this.elements.remove(1);//op
					this.elements.remove(1);//right
				}
				//System.err.printf("i=%d, len=%d", i,this.elements.size());
				
			}
			if(exponents.size()>0) {
				Collections.reverse(exponents);
				Number e=1;
				for(Number n:exponents) {
					e=CMath.pow(n, e);
				}
				//only number supports pow
				this.elements.clear();
				this.elements.add(((Num)left).toPow(e));
			}
			if(this.elements.size()==1) {
				this.doesAnyOps=false;
				return true; //equation was fully reduced to constants
			}
		}
		return false;
	}
	//flag for if to attempt to do inline mult for literal numbers
	@SuppressWarnings("unused")
	public void compileOps(PrintStream p,Compiler c,Scope s,VarType typeWanted) throws CompileError {
		boolean consted = this.constify(c,s);	
		//System.err.printf("line=%d, toplevel = %b, isArg = %b,doesanyops = %b, address %s\n", this.line,this.isTopLevel,this.isAnArg,this.doesAnyOps,this);
		if (c.resourcelocation.toString().equals("mcpptest:entity/selector_equation") && false) {
			//this.printTree(System.err);
			//System.err.printf("isconst: %b\n", this.isConstable());
		}
		if(s.hasThread()) for (Token t:this.elements) if (t instanceof MemberName){
			//aprove of all thread locals
			s.getThread().approveVar(((MemberName) t).getVar(), s);
			
		}
		if(this.isTopLevel) {
			this.stack.clear();
		}
		if(this.doesAnyOps || !(this.isTopLevel || this.isAnArg)) {
			if(forceConst) throwNotConstError();
			//do sub ops on registers
			if(this.elements.get(0) instanceof UnaryOp) {
				UnaryOp op=(UnaryOp) this.elements.get(0);
				Token in=this.elements.get(1);
				this.homeReg=this.putTokenToRegister(p, c, s, typeWanted, in);
				op.perform(p, c, s, stack, this.homeReg);
			}
			else if(this.elements.get(0) instanceof Equation &&  ((Equation)this.elements.get(0)).isCast) {
				Type cast=(Type) ((Equation)this.elements.get(0)).elements.get(0);
				Token in=this.elements.get(1);
				this.homeReg=this.putTokenToRegister(p, c, s, cast.type, in);
				//cast register
				//System.err.printf("Equation: casting %s -> %s", stack.getVarType(this.homeReg).asString(),cast.type.asString());
				this.stack.castRegister(p,s, this.homeReg, cast.type);
			}else if (this.elements.size()==1){
				Token in=this.elements.get(0);
				this.homeReg=this.putTokenToRegister(p, c, s, typeWanted, in);
			}else if (this.elements.size()==0){
				if(!this.isAnArg)throw new CompileError("unexpected blank equation at line %s col %s".formatted(this.line,this.col));
			}else {
				Token first=this.elements.get(0);
				//CompileJob.compileMcfLog.println(Token.asStrings(elements));
				CompileError.CannotStack nostack1=null; boolean hasHome1=false;
				try{
					this.homeReg=this.putTokenToRegister(p, c, s, typeWanted, first);//may be redundant but that is OK
					hasHome1=true;
				}catch (CompileError.CannotStack e) {
					nostack1=e;
				}
				
				if(this.elements.size()%2==0)throw new CompileError.UnexpectedTokens(elements, "an odd number of tokens alternating value, operation");
				List<Number> exponents=new ArrayList<Number>(); 
				Number prevNum=null;if(first instanceof Num) prevNum=((Num) first).value;
				//prevNum and nextnum are nonnull if there are const nums in the Equation;
				
				INbtValueProvider prevVar = this.getConstVarRef(first); prevVar = (prevVar==null) ?
						((first instanceof INbtValueProvider && ((INbtValueProvider) first).hasData())
						? (INbtValueProvider) first : null) : prevVar;
				for(int i=1;i<this.elements.size()-1;i+=2) {
					Token opt=this.elements.get(i);if(!(opt instanceof BiOperator))throw new CompileError.UnexpectedToken(opt, "bi-operator");
					BiOperator op=(BiOperator) opt;
					Token nextval=this.elements.get(i+1);
					if(op.op==BiOperator.OpType.EXP) {
						Number e;
						if(nextval instanceof Equation) {
							e=((Equation)nextval).getNegativeNumberLiteral();
							if(e==null) throw new CompileError.UnexpectedToken(nextval, "number or neg-number; avoid nested exp ints;");
						}else if (nextval instanceof Num) {
							e=((Num)nextval).value;
						}else throw new CompileError.UnexpectedToken(nextval, "number", "var-exponents not supported");
						exponents.add(e);
					}else {
						Number newnum=null;if(nextval instanceof Num) newnum=((Num) nextval).value;
						INbtValueProvider newVar = this.getConstVarRef(nextval); newVar = (newVar==null) ?
								((nextval instanceof INbtValueProvider && ((INbtValueProvider) nextval).hasData())
								? (INbtValueProvider) nextval : null) : newVar;
						boolean didDirect = false;
						if(prevVar !=null && newVar !=null) {
							if(prevVar.getType().isStruct() && prevVar.getType().struct.canDoBiOpDirect(op, prevVar.getType(), newVar.getType(), true)) {
								if(hasHome1) stack.pop();
								this.homeReg=Variable.directOp(p, c, s, prevVar, op, newVar, stack);
								didDirect=true;
							}else if (newVar.getType().isStruct() && newVar.getType().struct.canDoBiOpDirect(op, newVar.getType(), prevVar.getType(), false)) {
								if(hasHome1) stack.pop();
								this.homeReg=Variable.directOp(p, c, s, prevVar, op, newVar, stack);
								didDirect=true;
							}
							
							prevVar=null;
							if(nostack1!=null && !didDirect) throw nostack1;
							nostack1=null;
						}else {
							if(nostack1!=null && !didDirect) throw nostack1;
						}
						if (didDirect) {
							//already done
						}
						else if((newnum==null && prevNum==null) | (!op.canLiteralMult())) {
							//TODO wrong token type to register
							int nextreg=this.putTokenToRegister(p, c, s, typeWanted, nextval);
							//stack.debugOut(System.err);
							op.perform(p, c, s, stack, this.homeReg, nextreg);
							//CompileJob.compileMcfLog.printf("homeReg: %s;\n", this.homeReg);
							stack.cap(this.homeReg);//remove unused register IF it was created
						}else if (prevNum!=null && newnum==null ) {
							//first term is number
							//TODO move some of this into op functions
							int nextreg=this.putTokenToRegister(p, c, s, typeWanted, nextval);
							if(op.op==OpType.MULT)op.literalMultOrDiv(p, c, s, stack, nextreg,this.homeReg, (Num)first);
							else op.perform(p, c, s, stack, this.homeReg, nextreg);// const / var;
							stack.cap(this.homeReg);//
						}else if (prevNum==null && newnum!=null ) {
							//second term is number
							op.literalMultOrDiv(p, c, s, stack, this.homeReg,this.homeReg,(Num) nextval);
							//stack.cap(this.homeReg);//safe but unneeded
						}else  {//two numbers
							//may have double set homeReg but that is OK
							first=op.literalMultOrDiv(p, c, s, this.stack, this.homeReg,(Num) first, (Num)nextval);
							//stack.cap(this.homeReg);//safe but unneeded
						}
					}
				}
				if(exponents.size()>0) {
					Collections.reverse(exponents);
					Number e=1;
					for(Number n:exponents) {
						e=CMath.pow(n, e);
					}
					if(first instanceof Num && Equation.PRE_EVAL_EXP) {
						e=CMath.pow(((Num)first).value, e);
						int precison=(int) (5-Math.round(Math.log10( e.doubleValue())));
						stack.getRegister(this.homeReg).setValue(p,s, e,VarType.doubleWith(precison));
						this.stack.estmiate(this.homeReg, e);
					}else {
						BiOperator.exp(p, c, s, stack, homeReg, e);
					}
					stack.cap(this.homeReg);//remove old register
				}
				
			}
			this.hasSetToReg=true;
			this.retype=stack.getVarType(this.homeReg);
		}else {
			if (this.elements.size()==0)return ;//done
			if(this.elements.size()>1) {
				this.printTree(System.err);
				throw new CompileError("eq mistaken for a does no ops eq.");
			}
			Token e=this.elements.get(0);
			if(e instanceof MemberName) {
				//do nothing

				this.retype=((MemberName)e).var.type;
			}else if(e instanceof Num) {
				//do nothing
				this.retype=((Num)e).type;
			}else if(e instanceof Bool) {
				//do nothing
				this.retype=((Bool)e).type;
			
			}else if(e instanceof Token.StringToken) {
				//do nothing
				this.retype=((Token.StringToken)e).type;
			
			}else if(e instanceof Selector.SelectorToken) {
				//do nothing
				this.retype=((Selector.SelectorToken)e).type;
			}
			else if(e instanceof Function.FuncCallToken) {
				((Function.FuncCallToken)e).call(p, c,s,this.stack);
				this.retype=((Function.FuncCallToken)e).getFunction().retype;
			} else if(e instanceof BuiltinFunction.BFCallToken) {
				((BuiltinFunction.BFCallToken)e).call(p, c,s,this.stack);
				this.retype=((BuiltinFunction.BFCallToken)e).getRetType();
			} else  if (e instanceof CommandToken){
				this.homeReg=this.storeCMD(p, c, s, typeWanted, e);
				this.retype=stack.getVarType(this.homeReg);
				this.hasSetToReg=true;
			}
		}
	}
	public void setVar(PrintStream p,Compiler c,Scope s,Variable v) throws CompileError {
		if(this.hasSetToReg) {
			//CompileJob.compileMcfLog.println("#homereg set from");
			v.setMe(p,s, stack,this.homeReg);
			this.stack.pop();
		}else {
			if (this.elements.size()==0)return;//done
			if(this.elements.size()>1)throw new CompileError("eq mistaken for a does no ops eq.");
			Token e=this.elements.get(0);
			if(e instanceof MemberName) {
				Variable from=((MemberName) e).var;
				Variable.directSet(p,s, v, from, this.stack);
			}else if(e instanceof Num) {
				v.setMeToNumber(p, c, s, stack, ((Num)e).value);
			}else if(e instanceof Bool) {
				v.setMeToBoolean(p, c, s, stack, ((Bool)e).val);
			}else if(e instanceof Function.FuncCallToken) {
				((Function.FuncCallToken)e).getRet(p, c, s, v, stack);
			}else if(e instanceof Const.ConstExprToken) {
				
				if(v.canSetToExpr((Const.ConstExprToken)e)) {
					v.setMeToExpr(p,this.stack,(Const.ConstExprToken)e);
				}else throw new CompileError.UnsupportedCast((Const.ConstExprToken)e, v.type);
			}
			else if(e instanceof BuiltinFunction.BFCallToken) {
				((BuiltinFunction.BFCallToken)e).getRet(p, c, s, v,stack);
			}
		}
	}
	public int setReg(PrintStream p,Compiler c,Scope s,VarType typeWanted) throws CompileError {
		if(forceConst) throwNotConstError();
		if(this.hasSetToReg) {
			//already done
		}else {
			//this case will also add a register to the stack
			this.homeReg=this.stack.setNext(typeWanted==null? this.retype:typeWanted);
			if (this.elements.size()==0)throw new CompileError("unexpected empty equation asked to set to reg");
			if(this.elements.size()>1)throw new CompileError("eq mistaken for a does no ops eq.");
			Token e=this.elements.get(0);
			if(e instanceof MemberName) {
				Variable from=((MemberName) e).var;
				from.getMe(p,s, stack, this.homeReg);
			}else if(e instanceof Num) {
				stack.getRegister(this.homeReg).setValue(p,s, ((Num)e).value, ((Num)e).type);
			}else if(e instanceof Bool) {
				stack.getRegister(this.homeReg).setValue(p, ((Bool)e).val);
			}else if(e instanceof Function.FuncCallToken) {
				((Function.FuncCallToken)e).getRet(p, c, s, stack, this.homeReg);
			} else if(e instanceof BuiltinFunction.BFCallToken) {
				((BuiltinFunction.BFCallToken)e).getRet(p, c, s, stack, this.homeReg);
			}else if (e instanceof Const.ConstExprToken) {
				throw new CompileError.CannotStack((Const.ConstExprToken)e);
			}
			
		}
		return this.homeReg;
	}
	public void ignoreGet(PrintStream p,Compiler c,Scope s) throws CompileError {
		if(this.hasSetToReg) {
			//already done
		}else {
			if (this.elements.size()==0)throw new CompileError("unexpected empty equation asked to set to reg");
			if(this.elements.size()>1)throw new CompileError("eq mistaken for a does no ops eq.");
			Token e=this.elements.get(0);
			if(e instanceof MemberName) {
				//Variable from=((Token.MemberName) e).var;
				//from.getMe(p,s, stack, this.homeReg);
			}else if(e instanceof Num) {
				//stack.getRegister(this.homeReg).setValue(p,s, ((Num)e).value, ((Num)e).type);
			}else if(e instanceof Bool) {
				//stack.getRegister(this.homeReg).setValue(p, ((Token.Bool)e).val);
			}else if(e instanceof Function.FuncCallToken) {
				((Function.FuncCallToken)e).dumpRet(p, c, s, stack);
			} else if(e instanceof BuiltinFunction.BFCallToken) {
				((BuiltinFunction.BFCallToken)e).dumpRet(p, c, s, stack);
			}else if (e instanceof Const.ConstExprToken) {
				throw new CompileError.CannotStack((Const.ConstExprToken)e);
			}
			
		}
	}

	@Override
	public void printStatementTree(PrintStream p, int tabs) {
		this.printTree(p, tabs);
		
	}

	@Override
	public boolean hasData() {
		if(this.isConstable()) {
			Const.ConstExprToken c = this.getConstNbt();
			if (c!=null && c instanceof INbtValueProvider) {
				return ((INbtValueProvider) c).hasData();
			}
			
		} if (this.isConstRefable()) {
			return this.getConstVarRef().hasData();
		}
		return false;
	}
	

	@Override
	public String fromCMDStatement() {
		if(this.isConstable()) {
			Const.ConstExprToken c = this.getConstNbt();
			if (c!=null && c instanceof INbtValueProvider) {
				return ((INbtValueProvider) c).fromCMDStatement();
			}
			
		} if (this.isConstRefable()) {
			return this.getConstVarRef().fromCMDStatement();
		}
		return null;
	}

	@Override
	public VarType getType() {
		if(this.isConstable()) {
			Const.ConstExprToken c = this.getConstNbt();
			if (c!=null && c instanceof INbtValueProvider) {
				return ((INbtValueProvider) c).getType();
			}
			
		} if (this.isConstRefable()) {
			return this.getConstVarRef().type;
		}
		return null;
	}
	public void transferRegValue(PrintStream p,int home,Register to) throws CompileError {
		to.operation(p, "=", this.stack.getRegister(home));
		this.stack.cap(home-1);
	}
	public boolean isEmpty() {return this.elements.isEmpty();}

	public void throwNotConstError() throws CompileError {
		throw new CompileError("Equation at line %s col %s failed to evaluate to a compile time constant".formatted(this.line,this.col));
	}
}
