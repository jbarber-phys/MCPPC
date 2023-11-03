package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.OperationOrder;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Token.Factory;

public class UnaryOp extends Token {
	public static enum UOType{
		UMINUS("-",OperationOrder.UNARYMINUS) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home) throws CompileError {
				UnaryOp.unaryNegatize(p, c, s, stack, home);
			}}
		,NOT("!",OperationOrder.UNARYNOT) {
			@Override public void perform(PrintStream p, Compiler c, Scope s, RStack stack, Integer home) throws CompileError {
				UnaryOp.unaryNot(p, c, s, stack, home);
			}};
		static final Map<String,UOType> MAP = new HashMap<String,UOType>();
		public static UOType fromString(String s) {
			return MAP.getOrDefault(s, null);
		}
		
		final OperationOrder order;
		public final String s;
		public abstract void perform(PrintStream p,Compiler c,Scope s, RStack stack,Integer home) throws CompileError;
		UOType(String s,OperationOrder order){
			this.s=s;
			this.order=order;
		}static {for(UOType op:UOType.values()) {
			MAP.put(op.s, op);
		}
			
		}
		
	}
	public static final Factory factory=new Factory(Regexes.ALL_UN_OPERATOR) {
		@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			c.cursor=matcher.end();
			return new UnaryOp(line,col,matcher);
		}};
		public static final Factory uminusfactory=new Factory(Regexes.UNARY_MINUS) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new UnaryOp(line,col,matcher);
			}};
	private final UOType op;
	public UnaryOp(int line, int col,Matcher m) {
		super(line, col);
		this.op=UOType.fromString(m.group());
	}
	public UnaryOp(int line, int col, UOType op) {
		super(line, col);
		this.op=op;
	}
	@Override
	public String asString() {
		return this.op.s;
	}
	public void perform(PrintStream p,Compiler c,Scope s, RStack stack,Integer home) throws CompileError {
		VarType t=stack.getVarType(home);
		if(t.isStruct()) {
			t.doStructUnaryOp(this.op,p, c, s, stack, home);
		}else
			this.op.perform(p, c, s, stack, home);
	}
	public OperationOrder getOpOrder() {
		return this.op.order;
	}
	public boolean isUminus() {
		return this.op==UOType.UMINUS;
	}
	public UOType getOpType() {
		return this.op;
	}
	public static void unaryNegatize(PrintStream p,Compiler c,Scope s, RStack stack,Integer home) throws CompileError {
		VarType type=stack.getVarType(home);
		if(!type.isNumeric())Warnings.warning("tried to negatize non-numeric type", c);
		int free=stack.reserve(1);
		Register hr=stack.getRegister(home);
		Register fr=stack.getRegister(free);
		fr.setValue(p, -1);
		hr.operation(p, "*=", fr);
	}
	public static void unaryNot(PrintStream p,Compiler c,Scope s, RStack stack,Integer home) throws CompileError {
		VarType type=stack.getVarType(home);
		if(!type.isLogical())Warnings.warning("tried to not non-logical type", c);
		//int free=stack.reserve(1);
		Register hr=stack.getRegister(home);
		//Register fr=stack.getRegister(free);
		p.printf("scoreboard players add %s 1\n", hr.inCMD());
		p.printf("execute if score %s matches 2.. run scoreboard players set %s 0\n", hr.inCMD(),hr.inCMD());
	}
}
