package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CMath;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.VarType.Builtin;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.Struct;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.RuntimeError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Type;
import net.mcppc.compiler.tokens.UnaryOp.UOType;

/**
 * dymanic precision float; intended to replace templates (cast everything to this for unknown precision)
 * 
 * unfinished, dont use; use templates instead
 * @author jbarb_t8a3esk
 *
 */
@Deprecated
public class FloatpN extends Struct {
	public static FloatpN doubleN = new FloatpN("DoubleN",VarType.DOUBLE);

	static final Variable S_precision = new Variable("\"$PL_PREC\"", VarType.INT,null,Mask.SCORE,"#$MCPPC::FloatpN", "PL_PRECISION");
	static final Variable S_mult = new Variable("\"$PL_PREC\"", VarType.INT,null,Mask.SCORE,"#$MCPPC::FloatpN", "PL_MULT");
	static final Variable S_opflag = new Variable("\"$PL_PREC\"", VarType.BOOL,null,Mask.SCORE,"#$MCPPC::FloatpN", "PL_OPFLAG");
	
	static final Variable T_var = new Variable("\"$PL_PREC\"", VarType.INT,null,Mask.SCORE,"#$MCPPC::FloatpN", "PL_PRECISION");
	public static void registerAll() {
		//Struct.register(doubleN);
		//TODO subscibe all code generators for this, if any
	}
	
	final VarType tagtype;
	static final String VALUE="value";
	static final String PRECISION="precision";
	//static final String MULT="mult";//dont
	//static final String MULT_INV="mult_inv";
	public final int MAXPRECISION = 6;
	public final int MINPRECISION = Type.ALLOW_NEGATIVE_PRECISION?-3:0;
	public FloatpN(String name,VarType tagtype) {
		super(name, true, true, false);
		this.tagtype=tagtype;
	}

	@Override
	public String getNBTTagType(VarType varType) {
		return "tag_compound";
	}

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		// should never be called
		return 0;
	}

	@Override
	public String getPrecisionStr(VarType mytype)  {
		//should never be called
		return "";
	}

	@Override
	protected String getJsonTextFor(Variable self) throws CompileError {
		return this.varValue(self).getJsonText();
	}

	@Override
	public int sizeOf(VarType mytype) {
		return 2;
	}

	public void getMyVal(PrintStream p, Variable val,Register pcr, Register out) throws CompileError {
		for(int pc=this.MINPRECISION;pc<=this.MAXPRECISION;pc++) {
			double mult=Math.pow(10, pc);
			p.printf("execute if score %s matches %s "
			+"store result score %s run "
					+" data get %s %s\n"
					,pcr.inCMD(),pc
					,out.inCMD()
					,val.dataPhrase(),CMath.getMultiplierFor(mult));
		}
		String prefix = "execute unless score %s matches %s..%s run ".formatted(pcr.inCMD(),this.MINPRECISION,this.MAXPRECISION);
		RuntimeError.printf(p, prefix, "unexpected precision outside %s..%s".formatted(this.MINPRECISION,this.MAXPRECISION));
	}
	public void getMyValDirect(PrintStream p, Variable val,Register pcr, Variable out) throws CompileError {
		for(int pc=this.MINPRECISION;pc<=this.MAXPRECISION;pc++) {
			double mult=Math.pow(10, pc);
			p.printf("execute if score %s matches %s "
			+"store result %s %s 1 run "
					+" data get %s %s\n"
					,pcr.inCMD(),pc
					,out.dataPhrase(),out.type.getNBTTagType()
					,val.dataPhrase(),CMath.getMultiplierFor(mult));
		}
		String prefix = "execute unless score %s matches %s..%s run ".formatted(pcr.inCMD(),this.MINPRECISION,this.MAXPRECISION);
		RuntimeError.printf(p, prefix, "unexpected precision outside %s..%s".formatted(this.MINPRECISION,this.MAXPRECISION));
	}
	public void setMyVal(PrintStream p, Variable out,Register pcr, Register in) throws CompileError {
		for(int pc=this.MINPRECISION;pc<=this.MAXPRECISION;pc++) {
			double mult=Math.pow(10, -pc);
			p.println("execute if %s matches %s "
			+"store result %s %s %s run scoreboard players get %s"
					.formatted(pcr.inCMD(),pc,
							out.dataPhrase(),tagtype,CMath.getMultiplierFor(mult),in.inCMD())); 
		}
		String prefix = "execute unless score %s matches %s..%s run ".formatted(pcr.inCMD(),this.MINPRECISION,this.MAXPRECISION);
		RuntimeError.printf(p, prefix, "unexpected precision outside %s..%s".formatted(this.MINPRECISION,this.MAXPRECISION));
	}
	public void setMyValDirect(PrintStream p, Variable out,Register pcr, Variable in) throws CompileError {
		for(int pc=this.MINPRECISION;pc<=this.MAXPRECISION;pc++) {
			double mult=Math.pow(10, -pc);
			p.println("execute if %s matches %s "
			+"store result %s %s %s run data get %s 1"
					.formatted(pcr.inCMD(),pc,
							out.dataPhrase(),tagtype,CMath.getMultiplierFor(mult),in.dataPhrase())); 
		}
		String prefix = "execute unless score %s matches %s..%s run ".formatted(pcr.inCMD(),this.MINPRECISION,this.MAXPRECISION);
		RuntimeError.printf(p, prefix, "unexpected precision outside %s..%s".formatted(this.MINPRECISION,this.MAXPRECISION));
	}
	public void fixPrecision(PrintStream p, int precout,Register pcr, Register val,RStack stack) throws CompileError {
		for(int pc=this.MINPRECISION;pc<=this.MAXPRECISION;pc++) {
			String op=(precout-pc)>0?"*=":"/=";
			long mult=Math.round(Math.pow(10, Math.abs(precout-pc)));
			if(precout-pc ==0) continue;//can skip
			int mr=stack.reserve(1);Register rmult=stack.getRegister(mr);
			rmult.setValue(p, mult);
			p.printf("execute if %s matches %s "
					+"scoreboard players operation %s %s %s\n"
							,pcr.inCMD(),pc
							,val,op,rmult); 
		}
		String prefix = "execute unless score %s matches %s..%s run ".formatted(pcr.inCMD(),this.MINPRECISION,this.MAXPRECISION);
		RuntimeError.printf(p, prefix, "unexpected precision outside %s..%s".formatted(this.MINPRECISION,this.MAXPRECISION));
	}
	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		// inline currently
		Variable val=this.varValue(me);
		Variable prc=this.varPrecision(me);
		//val.getMe(p,stack,home);
		prc.getMe(p,s, stack, home+1);
		Register pcr=stack.getRegister(home+1);
		Register out=stack.getRegister(home);
		this.getMyVal(p, val, pcr, out);
	}

	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		VarType vt=stack.getVarType(home);
		Variable val=this.varValue(me);
		//Variable prec=this.varPrecision(me);//dont set precision;
		if(vt.struct instanceof FloatpN) {
			Register in=stack.getRegister(home);
			Register pcr=stack.getRegister(home+1);
			this.setMyVal(p, val, pcr, in);
		}else {
			val.setMe(p,s, stack, home);
		}

	}

	@Override
	public void allocate(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		this.allocateCompound(p, var, fillWithDefaultvalue, this.fields);

	}

	@Override
	public String getDefaultValue(VarType var) {
		return Struct.DEFAULT_COMPOUND;
	}
	//does not explicitly expose fields
	public final List<String> fields=new ArrayList<String>();//empty
	private Variable varValue(Variable self) {
		return self.fieldMyNBTPath(VALUE, this.tagtype);
	}
	private Variable varPrecision(Variable self) {
		return self.fieldMyNBTPath(PRECISION, VarType.INT);
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
		return false;
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return null;
	}

	@Override
	public VarType withPrecision(VarType vt, int newPrecision) throws CompileError {
		return vt;
	}

	@Override
	public boolean canCasteFrom(VarType from, VarType mytype) {
		if(from.isStruct()) return from.struct instanceof FloatpN;
		else  return from.isNumeric();
	}

	@Override
	public void castRegistersFrom(PrintStream p, Scope s, RStack stack, int start, VarType old, VarType mytype)
			throws CompileError {
		//caller makes sure there is enough space
		if(old.isStruct()) {
			//do nothing
		}else {
			this.castRegistersFromB(p,s, stack, start, old, mytype);
		}
			
	}
	private void castRegistersFromB(PrintStream p,Scope s, RStack stack, int start, VarType old, VarType mytype)
			throws CompileError {
		int prc=start+1;
		stack.getRegister(prc).setValue(p, old.getPrecision(s));
		//done
	}

	@Override
	public boolean canCasteTo(VarType to, VarType mytype) {
		if(to.isStruct()) return to.struct instanceof FloatpN;
		else  return to.isNumeric();
	}

	@Override
	public void castRegistersTo(PrintStream p, Scope s, RStack stack, int start, VarType newType, VarType mytype)
			throws CompileError {
		if(newType.isStruct()) {
			//do nothing
		}else {
			this.castRegistersToB(p,s, stack, start, newType, mytype);
		}
	}
	private void castRegistersToB(PrintStream p,Scope s, RStack stack, int start, VarType newType, VarType mytype)
			throws CompileError {
		int precout = newType.getPrecision(s);
		Register val=stack.getRegister(start);
		Register pcr=stack.getRegister(start+1);
		this.fixPrecision(p, precout, pcr, val, stack);
	}

	@Override
	public void getMeDirect(PrintStream p, Scope s, RStack stack, Variable to, Variable me) throws CompileError {
		if(to.isStruct()) {
			this.setDirectFN(p,s, stack, to, me, false);
		}else {
			Variable prec=this.varPrecision(me);
			Variable inval=this.varValue(me);
			int ph=stack.reserve(1);
			Register pcr=stack.getRegister(ph);
			prec.getMe(p,s, stack, ph);
			this.getMyValDirect(p, inval, pcr, to);
		}
	}

	@Override
	public void setMeDirect(PrintStream p, Scope s, RStack stack, Variable me, Variable from) throws CompileError {
		if(from.isStruct()) {
			this.setDirectFN(p,s, stack, me, from, false);
		}else {
			Variable outval=this.varValue(me);
			Variable.directSet(p,s, outval, from, stack);
		}
	}
	public void setDirectFN(PrintStream p,Scope s, RStack stack, Variable to, Variable from, boolean setPrecision) throws CompileError {
		Variable val1=this.varValue(to);
		Variable val2=this.varValue(from);
		Variable.directSet(p,s, val1, val2, stack);
		if(setPrecision) {
			Variable p1=this.varPrecision(to);
			Variable p2=this.varPrecision(from);
			Variable.directSet(p,s, val1, val2, stack);
		}
	}
	public void setPrecision(PrintStream p,Compiler c,Scope s, Variable me,VarType type) throws CompileError {
		Variable prec = this.varPrecision(me);
		prec.setMeToNumber(p, c, s, s.getStackFor(), type.getPrecision(s));
	}
	@Override
	public boolean canMask(VarType mytype, Mask mask) {
		return false;
	}

	@Override
	public boolean canDoUnaryOp(UOType op, VarType mytype, VarType other) throws CompileError {
		switch(op) {
		case UMINUS:return true;
		default: return false;
		}
	}

	@Override
	public void doUnaryOp(UOType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home)
			throws CompileError {
		if(op!=UOType.UMINUS)throw new CompileError.UnsupportedOperation( op, mytype);
		this.doUnaryMin(mytype, p, c, s, stack, home);
	}
	public void doUnaryMin( VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home)
			throws CompileError {
		VarType type=stack.getVarType(home);
		if(!type.isNumeric())Warnings.warning("tried to negatize non-numeric type");
		int free=stack.reserve(1);
		Register hr=stack.getRegister(home);
		Register fr=stack.getRegister(free);
		fr.setValue(p, -1);
		hr.operation(p, "*=", fr);
	}

	@Override
	public boolean canDoLiteralMultDiv(BiOperator op, VarType mytype, Num other) throws CompileError {
		VarType memb=this.tagtype;
		return memb.isStruct()?memb.struct.canDoLiteralMultDiv(op, memb, other):memb.isNumeric();
	}

	@Override
	public void doLiteralMultDiv(BiOperator op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack,
			Integer in, Integer dest, Num other) throws CompileError {
		VarType itype=stack.getVarType(in);
		double factor=op.op==OpType.MULT?other.value.doubleValue():1.0/other.value.doubleValue();
		
		Register regIn=stack.getRegister(in);
		Register regOut=stack.getRegister(dest);
		stack.setVarType(dest,itype);
		//regIn.multByFloatUsingRam(p, stack, mult);//this is redundant
		int pin=in+1;
		int pout=dest+1;
		if(pin==dest) {
			stack.getRegister(pout).operation(p, "=", stack.getRegister(pin));
			pin=pout;
		}
		p.printf("execute store result storage %s double %s run scoreboard players get %s\n", stack.getTempRamInCmd(),factor,regIn.inCMD());
		p.printf("execute store result score %s run data get storage %s %s\n", regOut.inCMD(),stack.getTempRamInCmd(),1);
		if(pin!=pout) {
			stack.getRegister(pout).operation(p, "=", stack.getRegister(pin));
		}
		if(stack.getEstimate(in)!=null)stack.estmiate(in, stack.getEstimate(in).doubleValue()*factor);
		op.literalMultOrDiv(p, c, s, stack, in, dest, other);
	}

	@Override
	public void setVarToNumber(PrintStream p, Compiler c, Scope s, RStack stack, Number val, Variable self)
			throws CompileError {
		this.varValue(self).setMeToNumber(p, c, s, stack, val);
	}

	@Override
	public void setRegistersToNumber(PrintStream p, Compiler c, Scope s, RStack stack, int home, Number val,
			VarType myType) throws CompileError {
		stack.getRegister(home).setValue(p,s, val, myType);
		stack.getRegister(home+1).setValue(p, myType.getPrecision(s));
	}

	@Override
	public boolean canSetToExpr(ConstExprToken e) {
		return e instanceof Num;
	}

	@Override
	public void setMeToExpr(PrintStream p, RStack stack, Variable me, ConstExprToken e) throws CompileError {
		this.varValue(me).setMeToExpr(p, stack, e);
	}

	@Override
	public boolean canDoBiOp(OpType op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		boolean oIsNum=other.isNumeric();

		switch(op.order) {
		case ADD:
		case COMPARE:
		case MULT: return oIsNum;
		case EXP:
		default:
			return false;
		}
	}

	@Override
	public void doBiOpFirst(OpType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		// TODO Auto-generated method stub
		switch (op) {
		case ADD:
			this.addsub(p, c, s, stack, home1, home2, "+=");
			break;
		case SUB:
			this.addsub(p, c, s, stack, home1, home2, "-=");
			break;
		case MULT://TODO
			break;
		case DIV:
			break;
		case MOD:
			break;
		case EQ:
			break;
		case GT:
			break;
		case GTEQ:
			break;
		case LT:
			break;
		case LTEQ:
			break;
		case NEQ:
			break;
		default:
			break;
		
		}
		
	}

	@Override
	public void doBiOpSecond(OpType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		// TODO Auto-generated method stub
		
	}
	@Override public void exp(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Number exponent) throws CompileError {
		//TODO
	}
	private void adjustPrec(PrintStream p,Scope s, RStack stack,Register val, VarType ftype, Register iprec) throws CompileError {
		int extra=stack.reserve(2);
		Register mr=stack.getRegister(extra);
		Register dprec=stack.getRegister(extra+1);
		for(int pc=this.MINPRECISION;pc<=this.MAXPRECISION;pc++) {
			//boolean ismul=ftype.getPrecision()-pc>0;
			long mult=Math.round(Math.pow(10, Math.abs(ftype.getPrecision(s)-pc)));
			p.printf("execute if score %s matches %s "
			+"scoreboard players set %s %s\n"
					,iprec.inCMD(),pc
					,mr.inCMD(), CMath.getMultiplierFor(mult));
		}
		dprec.setValue(p, ftype.getPrecision(s));
		dprec.operation(p, "-", iprec);
		String prefixif = "execute if score %s matches %s..%s run ".formatted(iprec.inCMD(),this.MINPRECISION,this.MAXPRECISION);
		p.printf("%s if %s matches 1.. run scoreboard players operation %s *= %s\n", prefixif,dprec.inCMD(),val.inCMD(),mr.inCMD());
		p.printf("%s if %s matches ..-1 run scoreboard players operation %s /= %s\n", prefixif,dprec.inCMD(),val.inCMD(),mr.inCMD());
		String prefix = "execute unless score %s matches %s..%s run ".formatted(iprec.inCMD(),this.MINPRECISION,this.MAXPRECISION);
		RuntimeError.printf(p, prefix, "unexpected precision outside %s..%s".formatted(this.MINPRECISION,this.MAXPRECISION));
	}
	private void adjustPrec(PrintStream p,Scope s, RStack stack,Register val, Register fprec, VarType itype) throws CompileError {
		int extra=stack.reserve(2);
		Register mr=stack.getRegister(extra);
		Register dprec=stack.getRegister(extra+1);
		for(int pc=this.MINPRECISION;pc<=this.MAXPRECISION;pc++) {
			//boolean ismul=pc-ftype.getPrecision()>0;
			long mult=Math.round(Math.pow(10, Math.abs(pc-itype.getPrecision(s))));
			p.printf("execute if score %s matches %s "
			+"scoreboard players set %s %s\n"
					,fprec.inCMD(),pc
					,mr.inCMD(), CMath.getMultiplierFor(mult));
		}
		dprec.operation(p, "=", fprec);
		dprec.decrement(p, itype.getPrecision(s));
		String prefixif = "execute if score %s matches %s..%s run ".formatted(fprec.inCMD(),this.MINPRECISION,this.MAXPRECISION);
		p.printf("%s if %s matches 1.. run scoreboard players operation %s *= %s\n", prefixif,dprec.inCMD(),val.inCMD(),mr.inCMD());
		p.printf("%s if %s matches ..-1 run scoreboard players operation %s /= %s\n", prefixif,dprec.inCMD(),val.inCMD(),mr.inCMD());
		String prefix = "execute unless score %s matches %s..%s run ".formatted(fprec.inCMD(),this.MINPRECISION,this.MAXPRECISION);
		RuntimeError.printf(p, prefix, "unexpected precision outside %s..%s".formatted(this.MINPRECISION,this.MAXPRECISION));
	}
	private void adjustPrec(PrintStream p,Scope s, RStack stack,Register val, Register pout, Register pin) throws CompileError {
		int extra=stack.reserve(2);
		Register mr=stack.getRegister(extra);
		Register dprec=stack.getRegister(extra+1);
		dprec.operation(p, "=", pout);
		dprec.operation(p, "-=", pin);
		int floor=2*this.MINPRECISION-this.MAXPRECISION;
		int ceil=2*this.MAXPRECISION-this.MINPRECISION;
		for(int pc=floor;pc<=ceil;pc++) {
			//boolean ismul=pc-ftype.getPrecision()>0;
			long mult=Math.round(Math.pow(10, Math.abs(pc)));
			p.printf("execute if score %s matches %s "
			+"scoreboard players set %s %s\n"
					,dprec.inCMD(),pc
					,mr.inCMD(), CMath.getMultiplierFor(mult));
		}
		String prefixif = "execute if score %s matches %s..%s run ".formatted(dprec.inCMD(),floor,ceil);
		p.printf("%s if %s matches 1.. run scoreboard players operation %s *= %s\n", prefixif,dprec.inCMD(),val.inCMD(),mr.inCMD());
		p.printf("%s if %s matches ..-1 run scoreboard players operation %s /= %s\n", prefixif,dprec.inCMD(),val.inCMD(),mr.inCMD());
		String prefix = "execute unless score %s matches %s..%s run ".formatted(dprec.inCMD(),floor,ceil);
		RuntimeError.printf(p, prefix, "unexpected precision outside %s..%s".formatted(floor,ceil));
	}
	private void addsub(PrintStream p, Compiler c, Scope s, RStack stack, Integer home1, Integer home2,String aop) throws CompileError {
		VarType type1=stack.getVarType(home1);
		VarType type2=stack.getVarType(home2);
		Register h1=stack.getRegister(home1);
		Register h2=stack.getRegister(home2);
		//this.assertNumeric(type1, type2);

		VarType typef=type1;
		boolean isTypef2=false;
		if(!type1.isStruct() && type2.isStruct()) {
			typef=type2;
			isTypef2=true;
		}
		int dp1=typef.getPrecision(s)-type1.getPrecision(s);
		long mult1=Math.round(Math.pow(10, Math.abs(dp1)));
		String pop1=dp1>0?"*=":"/=";
		int dp2=typef.getPrecision(s)-type2.getPrecision(s);
		long mult2=Math.round(Math.pow(10, Math.abs(dp2)));
		String pop2=dp2>0?"*=":"/=";
		
		if(type1.isStruct() && type2.isStruct()) {
			//both FpN
			Register pcr1=stack.getRegister(home1+1);
			Register pcr2=stack.getRegister(home2+1);
			this.adjustPrec(p,s, stack, h2, pcr1, pcr2);
		}else if (type1.isStruct() ) {
			//type 1 is FN
			//reg 2 * = 10^(P2-p1)
			Register pcr=stack.getRegister(home1+1);
			this.adjustPrec(p,s, stack, h2, type2, pcr);
		} else {
			//type 2 is FN
			Register pcr=stack.getRegister(home2+1);
			this.adjustPrec(p,s, stack, h1, pcr, type1);
		}
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
		if(isTypef2) {
			if(home1+1!=home2 && stack.getExtra()>home1+1) {
				stack.pad(p, home1, 1, 2);
			}
			Register hp=stack.getRegister(home1+1);
			Register oldp=stack.getRegister(home2+1);
			hp.operation(p, "=", oldp);
			stack.setVarType(home1, typef);
		}
		if(stack.getEstimate(home1)!=null&&stack.getEstimate(home2)!=null)
			stack.estmiate(home1, stack.getEstimate(home1).doubleValue()+stack.getEstimate(home2).doubleValue());
		else stack.estmiate(home1, null);
		
	}
}
