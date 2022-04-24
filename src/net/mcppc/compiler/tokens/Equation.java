package net.mcppc.compiler.tokens;


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.struct.*;
import net.mcppc.compiler.tokens.BiOperator.OpType;

/**
 * a numeric equation;
 * currently does not support string equations; if its added, it will be a seperate token
 * @author jbarb_t8a3esk
 *
 */
public class Equation extends Token {
	private static final boolean PRE_EVAL_EXP = false;
	public static Equation toAssign(int line,int col,Compiler c,Matcher m) throws CompileError {
		Equation e=new Equation(line,col,c);
		e.isTopLevel=true;
		e.wasOpenedWithParen=false;
		e.populate(c, m);
		return e;
	}
	public static Equation toArgue(int line,int col,Compiler c,Matcher m) throws CompileError {
		Equation e=new Equation(line,col,c);
		e.isTopLevel=false;
		e.wasOpenedWithParen=false;
		e.isAnArg=true;
		e.populate(c, m);
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
	static final Token.Factory[] lookForValue= {
			Factories.newline,Factories.comment,Factories.domment,Factories.space,
			UnaryOp.factory,
			Token.Bool.factory,
			Token.MemberName.factory,Token.Num.factory,Statement.CommandToken.factory,
			Token.Paren.factory,//sub-eq or caste
			Token.LineEnd.factory, Token.CodeBlockBrace.factory,Token.ArgEnd.factory //possible terminators; unexpected
	};
	static final Token.Factory[] lookForOperation= {
			Factories.newline,Factories.comment,Factories.domment,Factories.space,
			Token.Member.factory,
			Token.Paren.factory,//func call
			BiOperator.factory,
			Token.LineEnd.factory, Token.CodeBlockBrace.factory,Token.ArgEnd.factory //possible terminators
			
	};
	public List<Token> elements=new ArrayList<Token>();
	
	private Number getNegativeNumberLiteral() {
		if(this.elements.size()!=2)return null;
		if(!(this.elements.get(0) instanceof UnaryOp))return null;
		UnaryOp op=(UnaryOp) this.elements.get(0);
		if(!op.isUminus())return null;
		if(!(this.elements.get(1) instanceof Token.Num))return null;
		Number n1=((Token.Num)this.elements.get(1)).value;
		return CMath.uminus(n1);
	}

	public boolean isNumber() {
		if(this.elements.size()==1) {
			if(!(this.elements.get(0) instanceof Token.Num))return false;
			return true;
		}
		if(this.elements.size()!=2)return false;
		if(!(this.elements.get(0) instanceof UnaryOp))return false;
		UnaryOp op=(UnaryOp) this.elements.get(0);
		if(!op.isUminus())return false;
		if(!(this.elements.get(1) instanceof Token.Num))return false;
		
		return true;
	}
	public Number getNumber() {
		if(this.elements.size()==1) {
			if((this.elements.get(0) instanceof Token.Num))return ((Token.Num)this.elements.get(0)).value;
			return null;
		}
		return this.getNegativeNumberLiteral();
	}
	public boolean isRefable() {
		if(this.elements.size()!=1)return false;
		if(!(this.elements.get(0) instanceof Token.MemberName))return false;
		return true;
	}
	public Variable getVarRef() throws CompileError{
		if(!this.isRefable())throw new CompileError("attempted to pass non-trivial equation as a ref to function demanding ref arg on line %d col %d;"
				.formatted(this.line,this.col));
		Token.MemberName core=(MemberName) this.elements.get(0);
		return core.var;
	}
	
	public Equation populate(Compiler c,Matcher m) throws CompileError {
		return populate(c,m,0);
	}
	public Equation populate(Compiler c,Matcher m,int recurrs) throws CompileError {
		if(recurrs==10)Warnings.warning("equation recurred 10 times; warning");
		if(recurrs==20)throw new CompileError("equation recurred 20 times; overflow for debug purposes;");
		while(true) {
			//look for value / unary
			int pc=c.cursor;
			Token v=c.nextNonNullMatch(lookForValue);
			//boolean willAddV=false;
			if (v instanceof Token.MemberName &&
					(((Token.MemberName) v).names.size()==1) &&
					VarType.isType(((Token.MemberName) v).names.get(0))) {
				//typecast or constructor
				c.cursor=pc;//setback
				v=Type.tokenizeNextVarType(c, m,v.line , v.col);
				this.doesAnyOps=true;
				Token close=c.nextNonNullMatch(lookForOperation);
				if(close instanceof Token.Member) {
					//TODO support static members of structs - test this
					if(!((Type)v).type.isStruct()) throw new CompileError("cannot construct non-struct type: %s;".formatted(((Type)v).type.asString()));
					Struct struct=((Type)v).type.struct;
					Token name=c.nextNonNullMatch(lookForValue);
					if(v instanceof Token.MemberName &&
							(((Token.MemberName) v).names.size()==1) )
						;
					else throw new CompileError.UnexpectedToken(name, "static function of struct %s".formatted(v.asString()));
					if(!struct.hasStaticBuiltinMethod((((Token.MemberName) v).names.get(0)))) 
						throw new CompileError.UnexpectedToken(name, "static function %s not found in struct %s".formatted(name.asString(),v.asString()));
					BuiltinStaticStructMethod bf=struct.getStatictBuiltinMethod((((Token.MemberName) v).names.get(0)), ((Type)v).type);
					BuiltinFunction.BFCallToken sub=BuiltinFunction.BFCallToken.make(c, m, v.line, v.col,this.stack, bf);
					v=sub;
					//throw new CompileError("static struct members not yet supported");
				}
				if (!(close instanceof Token.Paren)) throw new CompileError.UnexpectedToken(close,")");
				else{
					if(((Paren)close).forward) {
						//TODO support constructors; figure out syntax
						if(!((Type)v).type.isStruct()) throw new CompileError("cannot construct non-struct type: %s;".formatted(((Type)v).type.asString()));
						Struct struct=((Type)v).type.struct;
						BuiltinConstructor cstr=struct.getConstructor(((Type)v).type);
						BuiltinFunction.BFCallToken sub=BuiltinFunction.BFCallToken.make(c, m, v.line, v.col,this.stack, cstr);
						v=sub;
						//throw new CompileError.UnexpectedToken(close,")","constructors not supported yet");
					}else {
						//typecast
						if(!this.wasOpenedWithParen)throw new CompileError("typecast must be of form (type(...)) but was missing open paren;");
						if(this.elements.size()>0)throw new CompileError("typecast must be of form (type(...)) but was missing open paren;");
						this.end=End.CLOSEPAREN;
						this.isCast=true;
						this.elements.add(v);
						return this;
						
					}
				}
			}
			if (v instanceof Token.Paren) {
				if(!((Paren)v).forward) {
					if(this.elements.size()==0 && this.isAnArg) {
						//empty eq with close paren, allowed in function args
						this.end=End.CLOSEPAREN;
						return this;
					}
					else throw new CompileError.UnexpectedToken(v,"(");
				}
				this.doesAnyOps=true;
				Equation sub=new Equation(v.line,v.col,this.stack);
				sub.isTopLevel=false;
				sub.wasOpenedWithParen=true;
				sub.populate(c, m,recurrs+1);
				if(sub.end!=End.CLOSEPAREN)throw new CompileError("unexpected subeq ending %s, expected a ')'".formatted(sub.end.name()));
				
				if(sub.isCast) {
					Equation sub2=new Equation(v.line,v.col,this.stack);
					sub2.isTopLevel=false;
					sub2.wasOpenedWithParen=false;
					sub2.elements.add(sub);
					sub2.lastOp=OperationOrder.CAST;
					sub2.populate(c, m,recurrs+1);
					v=sub2;
					if(sub2.end!=End.LATEROP) {
						this.elements.add(sub2);
						this.end=sub2.end;
						return this;
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
				sub.populate(c, m,recurrs+1);
				v=sub;
				if(sub.end!=End.LATEROP) {
					this.elements.add(sub);
					this.end=sub.end;
					return this;
				}
				//create sub equation w forward unary
			}else if (v instanceof Token.ArgEnd||v instanceof Token.LineEnd||v instanceof Token.CodeBlockBrace) {
				throw new CompileError("unexpected premature equation ending %s, expected a value".formatted(v.asString()));
			}else {
				//willAddV=true;
			}
			
			//now look for operation
			pc=c.cursor;
			Token op=c.nextNonNullMatch(lookForOperation);
			if (op instanceof Token.Paren && ((Token.Paren) op).forward) {
				//function call
				//constructor has its own hook
				if(v instanceof Token.MemberName &&
						(((Token.MemberName) v).names.size()==1) &&
						BuiltinFunction.isBuiltinFunc((((Token.MemberName) v).names.get(0)))) {
					//a builtin function
					BuiltinFunction.BFCallToken sub=BuiltinFunction.BFCallToken.make(c, m, v.line, v.col,this.stack, ((Token.MemberName) v).names.get(0));
					//this.elements.add(sub);
					v=sub;

				}
				else {
					if(!(v instanceof Token.MemberName)) throw new CompileError.UnexpectedToken(v, "name before '('");
					//a normal function
					Function.FuncCallToken ft=Function.FuncCallToken.make(c, v.line, v.col, m, (MemberName) v, this.stack);
					ft.identify(c,c.currentScope);
					//this.elements.add(ft);
					v=ft;
				}
				//keep going
				//willAddV=false;
				pc=c.cursor;
				op=c.nextNonNullMatch(lookForOperation);
			}else {
				if(v instanceof Token.MemberName) {
					//a var
					((Token.MemberName) v).identify(c,c.currentScope);
					//do not add here
				}
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
					sub.populate(c, m,recurrs+1);
					//this.elements.add(sub);
					v=sub;
					//CompileJob.compileMcfLog.printf("sub of size %s;\n", sub.elements.size());
					if(sub.end!=End.LATEROP) {
						this.elements.add(v);
						this.end=sub.end;
						return this;
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
					return this;
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
				return this;
			}else if(op instanceof Token.ArgEnd) {
				this.elements.add(v);
				this.end=End.ARGSEP;
				return this;
			}else if(op instanceof Token.LineEnd) {
				this.elements.add(v);
				this.end=End.STMTEND;
				return this;
			}else if(op instanceof Token.CodeBlockBrace) {
				if(!((CodeBlockBrace)op).forward) throw new CompileError.UnexpectedToken(op, "subequation end: '; , ) {'");
				this.elements.add(v);
				this.end=End.BLOCKBRACE;
				return this;
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
	public void printTree(PrintStream p) {
		this.printTree(p, 0);
	}
	public void printTree(PrintStream p,int tabs) {
		//for debuging
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		p.printf("%sequation: {\n",s);
		for(Token t:this.elements) {
			if(t instanceof Equation) ((Equation) t).printTree(p, tabs+1);
			else p.printf("%s\t%s\n", s,t.asString());
		}
		p.printf("%s}(ended with a %s)\n",s,this.end.name());
		
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
			regnum=((Equation) in).setReg(p, c, s, typeWanted);
		}else if (in instanceof Token.MemberName) {
			regnum=stack.setNext(((Token.MemberName) in).var.type);
			((Token.MemberName) in).var.getMe(p, stack,regnum);
			this.stack.estmiate(regnum, ((Token.MemberName) in).estimate);
		}else if (in instanceof Token.Num) {
			regnum=stack.setNext(((Token.Num) in).type);
			stack.getRegister(regnum).setValue(p, ((Token.Num) in).value,((Token.Num) in).type);
			this.stack.estmiate(regnum, ((Token.Num) in).value);
		}else if (in instanceof Token.Bool) {
			regnum=stack.setNext(((Token.Bool) in).type);
			long score=((Token.Bool) in).val?1:0;
			stack.getRegister(regnum).setValue(p, score,((Token.Bool) in).type);
			this.stack.estmiate(regnum, null);
		}else if (in instanceof Function.FuncCallToken) {
			regnum=stack.setNext(((Function.FuncCallToken) in).getFunction().retype);
			((Function.FuncCallToken) in).call(p, c, s, stack);
			((Function.FuncCallToken) in).getRet(p, c, s, this.stack,regnum);
			this.stack.estmiate(regnum, ((Function.FuncCallToken) in).getEstimate());
		}else if (in instanceof BuiltinFunction.BFCallToken) {
			regnum=stack.setNext(((BuiltinFunction.BFCallToken) in).getRetType());
			((BuiltinFunction.BFCallToken) in).call(p, c, s, stack);
			((BuiltinFunction.BFCallToken) in).getRet(p, c, s, stack,regnum);
			this.stack.estmiate(regnum, ((BuiltinFunction.BFCallToken) in).getEstimate());
		}else if (in instanceof Statement.CommandToken) {
			regnum=this.storeCMD(p, c, s, typeWanted, in);
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
		p.printf("execute store %s score %s run %s\n", cretype,r.inCMD(),((Statement.CommandToken)in).inCMD());
		return regnum;
	}
	//flag for if to attempt to do inline mult for literal numbers
	@SuppressWarnings("unused")
	public void compileOps(PrintStream p,Compiler c,Scope s,VarType typeWanted) throws CompileError {
		if(this.doesAnyOps || !(this.isTopLevel || this.isAnArg)) {
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
				this.stack.castRegister(p, this.homeReg, cast.type);
			}else if (this.elements.size()==1){
				Token in=this.elements.get(0);
				this.homeReg=this.putTokenToRegister(p, c, s, typeWanted, in);
			}else if (this.elements.size()==0){
				if(!this.isAnArg)throw new CompileError("unexpected blank equation at line %s col %s".formatted(this.line,this.col));
			}else {
				Token first=this.elements.get(0);
				//CompileJob.compileMcfLog.println(Token.asStrings(elements));
				this.homeReg=this.putTokenToRegister(p, c, s, typeWanted, first);//may be redundant but that is OK
				if(this.elements.size()%2==0)throw new CompileError.UnexpectedTokens(elements, "an odd number of tokens alternating value, operation");
				List<Number> exponents=new ArrayList<Number>(); 
				Number prevNum=null;if(first instanceof Token.Num) prevNum=((Token.Num) first).value;
				for(int i=1;i<this.elements.size()-1;i+=2) {
					Token opt=this.elements.get(i);if(!(opt instanceof BiOperator))throw new CompileError.UnexpectedToken(opt, "bi-operator");
					BiOperator op=(BiOperator) opt;
					Token nextval=this.elements.get(i+1);
					if(op.op==BiOperator.OpType.EXP) {
						Number e;
						if(nextval instanceof Equation) {
							e=((Equation)nextval).getNegativeNumberLiteral();
							if(e==null) throw new CompileError.UnexpectedToken(nextval, "number or neg-number; avoid nested exp ints;");
						}else if (nextval instanceof Token.Num) {
							e=((Token.Num)nextval).value;
						}else throw new CompileError.UnexpectedToken(nextval, "number", "var-exponents not supported");
						exponents.add(e);
					}else {
						Number newnum=null;if(nextval instanceof Token.Num) newnum=((Token.Num) nextval).value;
						if((newnum==null && prevNum==null) | (!op.canLiteralMult())) {
							int nextreg=this.putTokenToRegister(p, c, s, typeWanted, nextval);
							op.perform(p, c, s, stack, this.homeReg, nextreg);
							CompileJob.compileMcfLog.printf("homeReg: %s;\n", this.homeReg);
							stack.cap(this.homeReg);//remove unused register IF it was created
						}else if (prevNum!=null && newnum==null ) {
							//first term is number
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
						stack.getRegister(this.homeReg).setValue(p, e,VarType.doubleWith(precison));
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
			if(this.elements.size()>1)throw new CompileError("eq mistaken for a does no ops eq.");
			Token e=this.elements.get(0);
			if(e instanceof Token.MemberName) {
				//do nothing

				this.retype=((Token.MemberName)e).var.type;
			}else if(e instanceof Token.Num) {
				//do nothing
				this.retype=((Token.Num)e).type;
			}else if(e instanceof Token.Bool) {
				//do nothing
				this.retype=((Token.Bool)e).type;
			}else if(e instanceof Function.FuncCallToken) {
				((Function.FuncCallToken)e).call(p, c,s,this.stack);
				this.retype=((Function.FuncCallToken)e).getFunction().retype;
			} else if(e instanceof BuiltinFunction.BFCallToken) {
				((BuiltinFunction.BFCallToken)e).call(p, c,s,this.stack);
				this.retype=((BuiltinFunction.BFCallToken)e).getRetType();
			} else  if (e instanceof Statement.CommandToken){
				this.homeReg=this.storeCMD(p, c, s, typeWanted, e);
				this.retype=stack.getVarType(this.homeReg);
				this.hasSetToReg=true;
			}
		}
	}
	public void setVar(PrintStream p,Compiler c,Scope s,Variable v) throws CompileError {
		if(this.hasSetToReg) {
			//CompileJob.compileMcfLog.println("#homereg set from");
			v.setMe(p, stack,this.homeReg);
			this.stack.pop();
		}else {
			if (this.elements.size()==0)return;//done
			if(this.elements.size()>1)throw new CompileError("eq mistaken for a does no ops eq.");
			Token e=this.elements.get(0);
			if(e instanceof Token.MemberName) {
				Variable from=((Token.MemberName) e).var;
				Variable.directSet(p, v, from, this.stack);
			}else if(e instanceof Token.Num) {
				v.setMeToNumber(p, c, s, stack, ((Token.Num)e).value);
			}else if(e instanceof Token.Bool) {
				v.setMeToBoolean(p, c, s, stack, ((Token.Bool)e).val);
			}else if(e instanceof Function.FuncCallToken) {
				((Function.FuncCallToken)e).getRet(p, c, s, v, stack);
			} else if(e instanceof BuiltinFunction.BFCallToken) {
				((BuiltinFunction.BFCallToken)e).getRet(p, c, s, v,stack);
			}
		}
	}
	public int setReg(PrintStream p,Compiler c,Scope s,VarType typeWanted) throws CompileError {
		if(this.hasSetToReg) {
			//already done
		}else {
			//this case will also add a register to the stack
			this.homeReg=this.stack.setNext(typeWanted);
			if (this.elements.size()==0)throw new CompileError("unexpected empty equation asked to set to reg");
			if(this.elements.size()>1)throw new CompileError("eq mistaken for a does no ops eq.");
			Token e=this.elements.get(0);
			if(e instanceof Token.MemberName) {
				Variable from=((Token.MemberName) e).var;
				from.getMe(p, stack, this.homeReg);
			}else if(e instanceof Token.Num) {
				stack.getRegister(this.homeReg).setValue(p, ((Token.Num)e).value, ((Token.Num)e).type);
			}else if(e instanceof Token.Bool) {
				stack.getRegister(this.homeReg).setValue(p, ((Token.Bool)e).val);
			}else if(e instanceof Function.FuncCallToken) {
				((Function.FuncCallToken)e).getRet(p, c, s, stack, this.homeReg);
			} else if(e instanceof BuiltinFunction.BFCallToken) {
				((BuiltinFunction.BFCallToken)e).getRet(p, c, s, stack, this.homeReg);
			}
			
		}
		return this.homeReg;
	}
}
