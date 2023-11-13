package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
/**
 * a builtin function that returns void but is otherwise normal;
 * this declutters other code
 * @author RadiumE13
 *
 */
public abstract class BFReturnVoid extends BuiltinFunction {

	public BFReturnVoid(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public VarType getRetType(BFCallToken token) {
		return VarType.VOID;
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
