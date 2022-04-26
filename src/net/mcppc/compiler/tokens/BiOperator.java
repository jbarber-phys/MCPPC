package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.OperationOrder;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.CompileError.UnsupportedOperation;
import net.mcppc.compiler.errors.Warnings.OneTimeWarnings;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.tokens.Token.Factory;
import net.mcppc.compiler.tokens.Token.LineEnd;

public class BiOperator extends Token{
	//TODO add seperate class for custom operator types;
	public static enum OpType{
		//flops:
		ADD("+",OperationOrder.ADD) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.addsub(p, c, s, stack, home1, home2,"+=");
			}},
		SUB("-",OperationOrder.ADD) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.addsub(p, c, s, stack, home1, home2,"-=");
			}},
		MULT("*",OperationOrder.MULT) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.mult(p, c, s, stack, home1, home2);
			}},
		DIV("/",OperationOrder.MULT) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.div(p, c, s, stack, home1, home2);
			}},
		MOD("%",OperationOrder.MULT) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.mod(p, c, s, stack, home1, home2);
			}},
		EXP("^",OperationOrder.EXP) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				throw new CompileError("OpType.EXP.perform(...) should not be called; handle this elsewhere;");
			}},
		
		EQ("==",OperationOrder.COMPARE) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.compare(p, c, s, stack, home1, home2,"=");
			}},
		NEQ("!=",OperationOrder.COMPARE) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.compareNot(p, c, s, stack, home1, home2,"=");
			}},
		GTEQ(">=",OperationOrder.COMPARE) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.compare(p, c, s, stack, home1, home2,">=");
			}},
		LTEQ("<=",OperationOrder.COMPARE) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.compare(p, c, s, stack, home1, home2,"<=");
			}},
		GT(">",OperationOrder.COMPARE) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.compare(p, c, s, stack, home1, home2,">");
			}},
		LT("<",OperationOrder.COMPARE) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.compare(p, c, s, stack, home1, home2,"<");
			}},
		XOR("|!&",OperationOrder.XOR) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.xor(p, c, s, stack, home1, home2);
			}},
		AND("&",OperationOrder.AND) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.and(p, c, s, stack, home1, home2);
			}},
		OR("|",OperationOrder.OR) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,BiOperator token) throws CompileError {
				token.or(p, c, s, stack, home1, home2);
			}};
		static final Map<String,OpType> MAP = new HashMap<String,OpType>();
		public static final OpType[] GROUPS= {null,EXP,MULT,DIV,MOD,ADD,SUB,EQ,NEQ,GTEQ,LTEQ,GT,LT,XOR,AND,OR};
		// (\^)|(\*)|(\/)|(%)|(\+)|(\-)|(==)|(!=)|(>=|=>)|(<=|=<)|(>)|(<)|(\|!&)|(&)|(\|)
		@Deprecated private static OpType fromString(String s) {
			return MAP.getOrDefault(s, null);
		}
		public static OpType fromMatch(Matcher m) {
			m.groupCount();
			for(int i=1;i<GROUPS.length;i++) {
				if(m.group(i)!=null)return GROUPS[i];
			}
			return null;
		}
		public final String s;
		final OperationOrder order;
		OpType(String s,OperationOrder op){
			this.s=s;
			this.order=op;
		} static {
			for(OpType op:OpType.values()) {
				MAP.put(op.s, op);
			}
		}
		public abstract void perform(PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2,BiOperator token) throws CompileError;
	}
	public static final Factory factory=new Factory(Regexes.ALL_BI_OPERATOR) {
		@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			c.cursor=matcher.end();
			return new BiOperator(line,col,matcher);
		}};
	final OpType op;
	public BiOperator(int line, int col,Matcher m) {
		super(line, col);
		this.op=OpType.fromMatch(m);
	}
	public BiOperator(int line, int col,OpType op) {
		super(line, col);
		this.op=op;
	}
	@Override
	public String asString() {
		return this.op.s;
	}
	public void perform(PrintStream p,Compiler c,Scope s, RStack stack,Integer home1,Integer home2) throws CompileError {
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		if(type1.isStruct() && type1.supportsBiOp(this.op, type2, true)) {
			type1.doBiOpFirst(this.op, p, c, s, stack, home1, home2);
		} else if(type2.isStruct() && type2.supportsBiOp(this.op, type1, false)) {
			type2.doBiOpSecond(this.op, p, c, s, stack, home1, home2);
		} else {
			this.op.perform(p, c, s, stack, home1, home2,this);
		}
	}
	private void assertNumeric(VarType t1,VarType t2) throws CompileError {
		if(t1.isNumeric()&&t2.isNumeric()) ;else throw new CompileError.UnsupportedOperation(t1, this, t2);}
	private void assertLogical(VarType t1,VarType t2) throws CompileError {
		if(t1.isLogical()&&t2.isLogical()) ;else throw new CompileError.UnsupportedOperation(t1, this, t2);}
	private void addsub(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,String aop) throws CompileError {
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register h1=stack.getRegister(home1);
		Register h2=stack.getRegister(home2);
		this.assertNumeric(type1, type2);

		VarType typef=type1;
		if(!type1.isFloatP() && type2.isFloatP())typef=type2;//log estimate correction
		int dp1=typef.getPrecision()-type1.getPrecision();
		long mult1=Math.round(Math.pow(10, Math.abs(dp1)));
		String pop1=dp1>0?"*=":"/=";
		int dp2=typef.getPrecision()-type2.getPrecision();
		long mult2=Math.round(Math.pow(10, Math.abs(dp2)));
		String pop2=dp2>0?"*=":"/=";
		
		if(dp1!=0){
			int extra=stack.reserve(1);Register rex=stack.getRegister(extra);
			rex.setValue(p, mult1);
			h1.operation(p, pop1, rex);
		}
		if(dp2!=0){
			int extra=stack.reserve(1);Register rex=stack.getRegister(extra);
			rex.setValue(p, mult2);
			h2.operation(p, pop2, rex);
		}
		h1.operation(p, aop, h2);
		stack.setVarType(home1, typef);
		if(stack.getEstimate(home1)!=null&&stack.getEstimate(home2)!=null)
			stack.estmiate(home1, stack.getEstimate(home1).doubleValue()+stack.getEstimate(home2).doubleValue());
		else stack.estmiate(home1, null);
	}
	private void mult(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2) throws CompileError {
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		this.assertNumeric(type1, type2);
		double mult = Math.pow(10, type1.getPrecision()+type2.getPrecision());
		boolean hasafloat = type1.isFloatP() || type2.isFloatP();
		if( (	stack.getEstimate(home1)!=null
				&&stack.getEstimate(home2)!=null
				&&stack.getEstimate(home1).doubleValue()*stack.getEstimate(home2).doubleValue()*mult<Register.score_max(2) 
				)
				||!hasafloat
				) {
			this.shortmult(p, c, s, stack, home1, home2);
		}else {
			this.longMult(p, c, s, stack, home1, home2);
		}
	}
	private void longMult(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2) throws CompileError {
		// / and % are a complete ops in mcfunction, see div() and mod()
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register h1=stack.getRegister(home1);
		Register h2=stack.getRegister(home2);
		//this.assertNumeric(type1, type2);
		
		//long multiplication; produces ~32 cmds
		
		int extra=stack.reserve(8);//may have an extra
		// o: small place; x:high place; position: from 1,2
		VarType finalType=type1;
		if(!type1.isFloatP() && type2.isFloatP())finalType=type2;//log estimate correction
		//CompileJob.compileMcfLog.printf("%s * %s = %s;\n", type1,type2,finalType);
		Register reg_1o=stack.getRegister(extra);
		Register reg_1x=stack.getRegister(extra+1);
		Register reg_2o=stack.getRegister(extra+2);
		Register reg_2x=stack.getRegister(extra+3);

		Register reg_wild=stack.getRegister(extra+4);
		Register reg_place=stack.getRegister(extra+5);
		Register reg_mult_1=stack.getRegister(extra+6);
		Register reg_mult_2=stack.getRegister(extra+7);
		//may be able to eliminate a few
		//long pmult1=Math.round(Math.pow(10, stack.getVarType(home1).getPrecision()));
		//String pop1=type1.getPrecision()>0?"/=":"*=";
		//boolean doPrecision1=type1.getPrecision()!=0;
		//long mult1 = Math.round(Math.pow(10, Math.abs(type1.getPrecision())));
		
		//long pmult2=Math.round(Math.pow(10, stack.getVarType(home2).getPrecision()));
		//String pop2=type2.getPrecision()>0?"/=":"*=";
		//boolean doPrecision2=type2.getPrecision()!=0;
		//long mult2 = Math.round(Math.pow(10, Math.abs(type2.getPrecision())));
		
		double finpmult = Math.pow(10, finalType.getPrecision()-type1.getPrecision()-type2.getPrecision());
		
		double placeMult=Math.pow(2, ((Register.SCORE_BITS-2)/2)); //assert placeMult%2==0;//check for rounding errors; should be 32768
		//use Math.round(
		
		//reg_place.setValue(p, Math.round(placeMult));//unused
		//reg_mult_1.setValue(p, mult1);
		//reg_mult_2.setValue(p, mult2);
		
		reg_1x.operation(p, "=", h1);
		reg_1x.operation(p, "/=", reg_place);
		reg_1o.operation(p, "=", h1);
		reg_1o.operation(p, "%=", reg_place);
		//dont: execute if score reggie reg5 < reggie reg4 unless score reggie reg6 = reggie reg4 run scoreboard players remove reggie reg5 1
		reg_2x.operation(p, "=", h2);
		reg_2x.operation(p, "/=", reg_place);
		reg_2o.operation(p, "=", h2);
		reg_2o.operation(p, "%=", reg_place);
		//dont: execute if score reggie reg5 < reggie reg4 unless score reggie reg6 = reggie reg4 run scoreboard players remove reggie reg5 1
		
		//renaming; recall: o: small place; x:high place; position: from 1,2
		Register reg_oo=h1;
		Register reg_ox=reg_1o;
		Register reg_xo=reg_2o;
		Register reg_xx=reg_2x;
		reg_oo.operation(p, "=", reg_1o);
		reg_oo.operation(p, "*=", reg_2o);
		reg_ox.operation(p, "*=", reg_2x);
		reg_xo.operation(p, "*=", reg_1x);
		reg_xx.operation(p, "*=", reg_1x);
		
		reg_oo.multByFloatUsingRamToRaw(p, stack, finpmult, h1);
		reg_ox.multByFloatUsingRam(p, stack, finpmult*placeMult);
		reg_xo.multByFloatUsingRam(p, stack, finpmult*placeMult);
		reg_xx.multByFloatUsingRam(p, stack, finpmult*placeMult*placeMult);
		
		//h1=reg_oo
		h1.operation(p, "+=", reg_ox);
		h1.operation(p, "+=", reg_xo);
		h1.operation(p, "+=", reg_xx);
		//operations done
		stack.setVarType(home1, finalType);
		if(stack.getEstimate(home1)!=null&&stack.getEstimate(home2)!=null)
			stack.estmiate(home1, stack.getEstimate(home1).doubleValue()*stack.getEstimate(home2).doubleValue());
		else stack.estmiate(home1, null);
	}

	private void shortmult(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2) throws CompileError {
		// executes 1-3 cmds
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register h1=stack.getRegister(home1);
		Register h2=stack.getRegister(home2);
		//this.assertNumeric(type1, type2);
		VarType finalType=type1;
		if(!type1.isFloatP() && type2.isFloatP())finalType=type2;//log estimate correction
		//CompileJob.compileMcfLog.printf("shortMult typef %s;\n", finalType.asString());
		
		int fppow=finalType.getPrecision()-type1.getPrecision()-type2.getPrecision();
		String precop=fppow>0?"*=":"/=";
		long finpmult = Math.round(Math.pow(10, Math.abs(fppow)));
		boolean doPrecision=fppow!=0;
		
		h1.operation(p, "*=", h2);
		if(doPrecision) {
			h2.setValue(p, finpmult);
			h1.operation(p, precop, h2);
		}
		//operations done
		stack.setVarType(home1, finalType);
		if(stack.getEstimate(home1)!=null&&stack.getEstimate(home2)!=null)
			stack.estmiate(home1, stack.getEstimate(home1).doubleValue()*stack.getEstimate(home2).doubleValue());
		else stack.estmiate(home1, null);
	}
	public void literalMultOrDiv(PrintStream p, Compiler c, Scope s, RStack stack, Integer in, Integer dest,Num other ) throws CompileError {
		VarType itype=stack.getVarType(in);
		if(itype.isStruct()) {
			if(itype.struct.canDoLiteralMultDiv(this, itype, other)) {
				itype.struct.doLiteralMultDiv(this, itype, p, c, s, stack,in, dest,other);
				return;
			}else throw new CompileError.UnsupportedOperation( itype,this,other); 
		}
		VarType otype=itype;
		if(!itype.isNumeric())throw new CompileError.UnsupportedOperation( itype,this,other);
		if(other.type.isFloatP() & !itype.isFloatP()) {
			otype=other.type;//log correction
		}
		double mult=this.op==OpType.MULT?other.value.doubleValue():1.0/other.value.doubleValue();
		double timespow=Math.pow(10, otype.getPrecision()-itype.getPrecision());
		Register regIn=stack.getRegister(in);
		Register regOut=stack.getRegister(dest);
		stack.setVarType(dest,otype);
		//regIn.multByFloatUsingRam(p, stack, mult);//this is redundant
		p.printf("execute store result storage %s double %s run scoreboard players get %s\n", stack.getTempRamInCmd(),mult,regIn.inCMD());
		p.printf("execute store result score %s run data get storage %s %s\n", regOut.inCMD(),stack.getTempRamInCmd(),timespow);
		if(stack.getEstimate(in)!=null)stack.estmiate(in, stack.getEstimate(in).doubleValue()*mult);
	}
	public Token literalMultOrDiv(PrintStream p, Compiler c, Scope s, RStack stack,  Integer dest,Num n1,Num n2) throws CompileError {
		Num result ;
		if(this.op==OpType.MULT)result=n1.times(n2);
		else if(this.op==OpType.DIV)result=n1.divby(n2);
		else throw new CompileError("op %s was not * or /;".formatted(this.op.name()));
		VarType otype=result.type;
		Register regOut=stack.getRegister(dest);
		regOut.setValue(p, result.value,result.type);
		stack.setVarType(dest,otype);
		stack.estmiate(dest, result.value);
		return result;
	}
	public static final boolean DO_NUM_LIT_OPT=true;
	public boolean canLiteralMult() {
		return ((this.op==OpType.MULT)||(this.op==OpType.DIV))&&DO_NUM_LIT_OPT;
	}
	/*
	 * divide and mods numbers
	 * note: in mcfunction scoreboard /= and %=, the ops always go such that if q=a/b and r=a%b then a=bq+r
	 * #in mc
		# -3 % 7 = 4
		# -3 / 7 = -1
		
		#  3 % -7 = -4
		#  3 / -7 = -1
		
		#  -3 % -7 = -3
		#  -3 / -7 = 0
	 */
	private void div(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2) throws CompileError {
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register h1=stack.getRegister(home1);
		Register h2=stack.getRegister(home2);
		this.assertNumeric(type1, type2);
		VarType typef=type1;
		if(!type1.isFloatP() && type2.isFloatP())typef=type2;//log estimate correction
		int topplace=typef.getPrecision()-type1.getPrecision()+type2.getPrecision();
		int bottomPlace=0;
		if(stack.getEstimate(home1)!=null) {
			double est1=stack.getEstimate(home1).doubleValue();
			int maxTopPlace = (int) (Math.log10(2)*(Register.SCORE_BITS-1-2)-Math.log10(est1));
			if(maxTopPlace<topplace) {
				bottomPlace+=(maxTopPlace-topplace);
				topplace=maxTopPlace;
			}
		}
		long mult1=Math.round(Math.pow(10, Math.abs(topplace)));
		String pop1=topplace>0?"*=":"/=";
		long mult2=Math.round(Math.pow(10, Math.abs(bottomPlace)));
		String pop2=bottomPlace>0?"*=":"/=";
		if(topplace!=0){
			int extra=stack.reserve(1);Register rex=stack.getRegister(extra);
			rex.setValue(p, mult1);
			h1.operation(p, pop1, rex);
		}
		if(bottomPlace!=0){
			int extra=stack.reserve(1);Register rex=stack.getRegister(extra);
			rex.setValue(p, mult2);
			h2.operation(p, pop2, rex);
		}
		h1.operation(p, "/=", h2);
		stack.setVarType(home1, typef);
		stack.estmiate(home1, null);//divisor could be near zero
	}
	private void mod(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2) throws CompileError {
		//test: mc /= does ...
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register h1=stack.getRegister(home1);
		Register h2=stack.getRegister(home2);
		this.assertNumeric(type1, type2);
		//let first operand decide the type of
		// (a 10^-p)%(b 10^-q) = 10^-w (a 10^(-p+w)%b 10^(-q+w))
		VarType typef=type1;
		if(!type1.isFloatP() && type2.isFloatP())typef=type2;//log estimate correction
		int dp1=typef.getPrecision()-type1.getPrecision();
		long mult1=Math.round(Math.pow(10, Math.abs(dp1)));
		String pop1=dp1>0?"*=":"/=";
		int dp2=typef.getPrecision()-type2.getPrecision();
		long mult2=Math.round(Math.pow(10, Math.abs(dp2)));
		String pop2=dp2>0?"*=":"/=";
		
		if(dp1!=0){
			int extra=stack.reserve(1);Register rex=stack.getRegister(extra);
			rex.setValue(p, mult1);
			h1.operation(p, pop1, rex);
		}
		if(dp2!=0){
			int extra=stack.reserve(1);Register rex=stack.getRegister(extra);
			rex.setValue(p, mult2);
			h2.operation(p, pop2, rex);
		}
		h1.operation(p, "%=", h2);
		stack.setVarType(home1, typef);
		if(stack.getEstimate(home2)!=null) {
			if(stack.getEstimate(home1)!=null && stack.getEstimate(home1).doubleValue() < stack.getEstimate(home2).doubleValue())
				//stack.estmiate(home1, stack.getEstimate(home1))//does nothing
				;
			else stack.estmiate(home1, stack.getEstimate(home2));
		}//else leave alone
	}
	//fractional exponents not supported
	//note for sqrt(1), a and a^-1 are equally good guesses
	public static void exp(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Number exponent) throws CompileError {
		VarType type1=stack.getVarType(home1);
		Register reg1=stack.getRegister(home1);
		int exp=exponent.intValue();
		if(Math.abs(exp-exponent.doubleValue())>0.1)Warnings.warning("Warning: rounded exponent %s to an integer;".formatted(exponent));
		if(stack.getEstimate(home1)!=null && Math.log(stack.getEstimate(home1).doubleValue())*exp>=(Register.SCORE_BITS-1-2)*Math.log(2)) {
			//do divisions first
			Warnings.warn(OneTimeWarnings.LIKELYOVERFLOW);
		}
		if(Math.abs(exp)<Register.SCORE_BITS) {
			//repeat mult
			String expop=exp>0?"*=":"/=";
			String pop=type1.getPrecision()*exp>0?"/=":"*=";
			boolean doPrecision=type1.getPrecision()!=0;
			boolean reverse = (type1.getPrecision()>0) && exp<0;
			long mult = (long) Math.pow(10, Math.abs(type1.getPrecision()));
			int extra1=stack.reserve(2);
			int extra2=extra1+1;
			Register rex1=stack.getRegister(extra1);
			Register rex2=stack.getRegister(extra2);
			if(doPrecision)rex2.setValue(p, mult);
			rex1.setValue(p, mult);
			for(int i=1;i<=Math.abs(exp);i++) {
				if((stack.getEstimate(home1)!=null && Math.log(stack.getEstimate(home1).doubleValue())*i+Math.log(mult)>=(Register.SCORE_BITS-1-2)*Math.log(2))
						^ reverse
						) {
					//div first to prevent overflow
					if(doPrecision)rex1.operation(p, pop, rex2);
					rex1.operation(p, expop, reg1);
				}else {
					//mult first to preserve precision
					rex1.operation(p, expop, reg1);
					if(doPrecision)rex1.operation(p, pop, rex2);
				}
			}
			reg1.operation(p, "=", rex1);
		}else {
			//successive squaring is garbage as it will always overflow; don't encourage them;
			throw new CompileError("exponent %s is way too big and will definetly overflow if base >= 2;"
					.formatted(exp));
		}
		if(stack.getEstimate(home1)!=null)stack.estmiate(home1, Math.pow(stack.getEstimate(home1).doubleValue(), exp));
		
	}
	private void compare(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,String cpop) throws CompileError {
		this.compareGen(p, c, s, stack, home1, home2, cpop, false);
	}
	private void compareNot(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,String cpopnot) throws CompileError {
		this.compareGen(p, c, s, stack, home1, home2, cpopnot, true);
		
	}
	private void compareGen(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,String cpop,boolean invert) throws CompileError {
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register reg1=stack.getRegister(home1);
		Register reg2=stack.getRegister(home2);
		VarType typef=VarType.BOOL;
		this.assertNumeric(type1, type2);
		int extra=stack.reserve(1);Register regE=stack.getRegister(extra);
		//prep reg for compare
		int p1=type1.getPrecision();
		int p2=type2.getPrecision();
		if(p1!=p2){
			int spreg=p1<p2?home1:home2;
			double spest=stack.getEstimate(spreg).doubleValue();
			int maxps=(int) Math.round((Register.SCORE_BITS-1)*Math.log10(2)-Math.log10(spest));
			int pf=Math.max(Math.min(Math.max(p1, p2), maxps),Math.min(p1, p2));
			//CompileJob.compileMcfLog.printf("maxps: %s; pf: %s;\n", maxps,pf);
			if(p1!=pf) {
				String pop=(pf-p1)>0?"*=":"/=";
				long mult=Math.round(Math.pow(10, Math.abs(pf-p1)));
				regE.setValue(p, mult);
				reg1.operation(p, pop, regE);
			}if(p2!=pf) {
				String pop=(pf-p2)>0?"*=":"/=";
				long mult=Math.round(Math.pow(10, Math.abs(pf-p2)));
				regE.setValue(p, mult);
				reg2.operation(p, pop, regE);
			}
		}
		//now compare
		regE.setValue(p,invert);
		p.printf("execute if %s run %s\n", reg1.compare(cpop, reg2),regE.setValueStr(!invert));
		reg1.operation(p, "=", regE);
		stack.setVarType(home1, typef);
	}
	//Note: xor, and, or; are all commutitive+asociative with themselves
	private void xor(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2) throws CompileError {
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register reg1=stack.getRegister(home1);
		Register reg2=stack.getRegister(home2);
		this.assertLogical(type1, type2);
		//xor is the same as a cnot 2->1 out 1
		p.printf("execute if score %s matches 1.. run scoreboard players add %s 1\n", reg2.inCMD(),reg1.inCMD());
		p.printf("execute if score %s matches 2.. run scoreboard players set %s 0\n", reg1.inCMD(),reg1.inCMD());
	}
	private void and(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2) throws CompileError {
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register reg1=stack.getRegister(home1);
		Register reg2=stack.getRegister(home2);
		this.assertLogical(type1, type2);
		reg1.operation(p, "<", reg2);
	}
	private void or(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2) throws CompileError {
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register reg1=stack.getRegister(home1);
		Register reg2=stack.getRegister(home2);
		this.assertLogical(type1, type2);
		reg1.operation(p, ">", reg2);
	}
	public OperationOrder getOpOrder() {
		return this.op.order;
	}
	
}