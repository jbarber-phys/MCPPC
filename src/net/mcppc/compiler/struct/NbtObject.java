package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.List;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.INbtValueProvider;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Num;

public class NbtObject extends Struct {
	public static final NbtObject obj = new NbtObject("Obj");
	public static void registerAll() {
		Struct.register(obj);
	}
	
	public NbtObject(String name) {
		super(name,true,true,true);//pretend to be everything
	}

	public static final String VALUE = "value";
	public Variable getValue(Variable self,VarType type) {
		return self.fieldMyNBTPath(VALUE, type);
	}
	@Override
	public String getNBTTagType(VarType varType) {
		return VarType.Builtin.NBT_COMPOUND;
	}
	@Override
	public String getDefaultValue(VarType var) throws CompileError {
		return Struct.DEFAULT_COMPOUND;
	}

	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return 0;
	}

	@Override
	public String getPrecisionStr(VarType mytype) {
		return null;
	}

	@Override
	public String getJsonTextFor(Variable variable) throws CompileError {
		//just try to print the value
		return this.getValue(variable, VarType.VOID).getJsonTextBasic();//type does not matter
	}

	@Override
	public int sizeOf(VarType mytype) {
		//size is not known at compile time, but just pretend so that it will be tolerated temporarily
		return 1;
	}

	@Override public boolean setRegTypeOnCastFrom() {
		return false;
	}
	@Override public VarType getTypeOnStack(VarType mytype, VarType typeWanted) {
		return typeWanted;
	}
	@Override public boolean canCasteTo(VarType to,VarType mytype) { 
		return to.isDataEquivalent();
	}
	public boolean canCasteFrom(VarType from,VarType mytype) { 
		return from.isDataEquivalent();
	}
	@Override
	public void castVarFrom(PrintStream p, Scope s, RStack stack, Variable vtag, VarType old, VarType mytype)
			throws CompileError {
		// do nothing; cast was already done
	}
	@Override
	public void castVarTo(PrintStream p, Scope s, RStack stack, Variable vtag, VarType mytype, VarType newType)
			throws CompileError {
		// do nothing; cast was already done
	}
	
	
	
	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted) throws CompileError {
		//throw new CompileError.CannotStack(me.type);
		if(typeWanted.struct instanceof NbtObject) throw new CompileError("cannot force an Obj onto the stack without converting");
		Variable val = this.getValue(me, typeWanted);
		val.getMe(p, s, stack, home, typeWanted);
	}

	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		//throw new CompileError.CannotStack(me.type);
		VarType regtype = stack.getVarType(home);//this has not been casted; which is good
		if(regtype.struct instanceof NbtObject) throw new CompileError("cannot get an Obj off the stack with unknown type");
		Variable val = this.getValue(me, regtype);
		val.setMe(p, s, stack, home, regtype);
		stack.setVarType(home, me.type);
	}

	@Override
	public void getMeDirect(PrintStream p, Scope s, RStack stack, Variable to, Variable me) throws CompileError {
		if(to.type.struct instanceof NbtObject) {
			super.basicSetDirect(p, to, me);
			return;
		}
		Variable meval = this.getValue(me, to.type);
		Variable.directSet(p, s, to, meval, stack);
		
	}
	@Override
	public void setMeDirect(PrintStream p, Scope s, RStack stack, Variable me, Variable from) throws CompileError {
		if(from.type.struct instanceof NbtObject) {
			super.basicSetDirect(p, me, from);
			return;
		}
		Variable meval = this.getValue(me, from.type);
		Variable.directSet(p, s, meval, from, stack);
	}
	
	//see if value can do the operation

	@Override
	public boolean canDoBiOp(OpType op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		return false;//should never go on stack without converting iteslf
	}
	@Override
	public boolean canDoBiOpDirect(BiOperator op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		if(other.struct instanceof NbtObject) {
		}else if (other.isDataEquivalent()) {
		}else return false;
		switch(op.op) {
			case EQ:
			case NEQ: return true;
			default: return false;
		}
	}
	@Override
	public boolean canDoBiOpDirectOn(BiOperator op, VarType mytype, VarType other) throws CompileError {
		return false;
	}
	@Override
	public int doBiOpFirstDirect(BiOperator op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack,
			INbtValueProvider me, INbtValueProvider other) throws CompileError {
		assert me instanceof Variable;//no such thing as a const Obj
		if(other.getType().struct instanceof NbtObject) {
			assert other instanceof Variable;
			return super.basicDirectEquals(p, c, s, stack, me, other, op.op == OpType.NEQ);
		}else if(other instanceof Num &&  ((Num) other).value==null) {
			return this.getIsNull(p, c, s, stack, (Variable) me, op.op == OpType.NEQ);
		}
		Variable value = this.getValue((Variable) me, other.getType());
		return super.basicDirectEquals(p, c, s, stack, value, other, op.op == OpType.NEQ);
	}

	@Override
	public int doBiOpSecondDirect(BiOperator op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack,
			INbtValueProvider other, INbtValueProvider me) throws CompileError {
		assert me instanceof Variable;//no such thing as a const Obj
		if(other.getType().struct instanceof NbtObject) {
			assert other instanceof Variable;
			return super.basicDirectEquals(p, c, s, stack, other, me, op.op == OpType.NEQ);
		}else if(other instanceof Num &&  ((Num) other).value==null) {
			return this.getIsNull(p, c, s, stack, (Variable) me, op.op == OpType.NEQ);
		}
		Variable value = this.getValue((Variable) me, other.getType());
		return super.basicDirectEquals(p, c, s, stack, other, value, op.op == OpType.NEQ);
	}
	public int getIsNull(PrintStream p, Compiler c, Scope s, RStack stack,
			Variable self,boolean invert) throws CompileError {
		int home=stack.setNext(VarType.BOOL);
		Register reg=stack.getRegister(home);
		reg.setToSuccess(p, "data get %s".formatted(self.dataPhrase()));
		return home;
	}
	
	@Override
	public void setVarToNumber(PrintStream p, Scope s, RStack stack, Number val, Variable self) throws CompileError {
		VarType tp = VarType.fromNumber(val);
		if(tp.isVoid()) {
			//null: delete self
			Variable value = this.getValue(self, tp);
			value.basicdeallocateBoth(p);
			return;
		}
		Variable value = this.getValue(self, tp);
		value.setMeToNumber(p, s, stack, val);
	}
	@Override
	public void setVarToBool(PrintStream p, Scope s, RStack stack, boolean val, Variable self) throws CompileError {
		Variable value = this.getValue(self, VarType.BOOL);
		value.setMeToBoolean(p, s, stack, val);
	}
	@Override
	public boolean canSetToExpr(ConstExprToken e) {
		switch(e.constType()) {
		case BOOLIT:
		case NBT:
		case NUM:
		case STRLIT:
			return true;
		case TYPE:
		case COORDS:
		case ROT:
		case SELECTOR:
		default:
			return false;
		
		}
	}
	@Override
	public void setMeToExpr(PrintStream p, Scope s, RStack stack, Variable me, ConstExprToken e) throws CompileError {
		VarType type;
		switch(e.constType()) {
		case BOOLIT:
			this.setVarToBool(p, s, stack, isLogical, me);
			return;
		case NUM:
			this.setVarToNumber(p, s, stack, ((Num) e).value, me);
			return;
		case NBT:
			type=NbtCompound.TAG_COMPOUND;
			break;
		case STRLIT:
			type=Str.STR;
			break;
		case TYPE:
		case COORDS:
		case ROT:
		case SELECTOR:
		default:
			throw new CompileError.CannotSet(me.type, e.asString());
		
		}
		Variable value = this.getValue(me,type );
		super.setMeToNbtExprBasic(p, stack, value, e);
	}
	@Override
	public void setMeToCmd(PrintStream p, Scope s, Variable variable, String cmd) throws CompileError {
		Variable value = this.getValue(variable, VarType.INT);//assume result
		value.setMeToCmd(p, s, cmd);
	}
	@Override
	public void allocateLoad(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateCompoundLoad(p, var, fillWithDefaultvalue, List.of());
	}

	@Override
	public void allocateCall(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		super.allocateCompoundCall(p, var, fillWithDefaultvalue, List.of());
	}


	@Override
	public boolean hasField(Variable self, String name) {
		return false;
	}

	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		return null;
	}

	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		//can just compare to null
		return false;
	}

	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return null;
	}
	@Override
	public boolean canMask(VarType mytype, Mask mask) {
		return mask.isNbt;
	}

}
