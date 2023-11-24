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
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Token;
/**
 * a builtin function that will convert itself into an equation<p>
 * example: someVector.sqrmag() -> (someVector * someVector)
 * @author RadiumE13
 *
 */
public abstract class EquationMask extends BuiltinFunction {

	public EquationMask(String name) {
		super(name);
	}

	@Override public VarType getRetType(BFCallToken token, Scope s) {
		//is never called; (called only when compiling)
		new CompileError("EquationMask::getRetType should never have been reached").printStackTrace();
		return null;
	}
		

	@Override
	public abstract Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError ;

	@Override
	public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		throw new CompileError("EquationMask::call should not be reached;");
		
	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
			throws CompileError {
		throw new CompileError("EquationMask::getRet should not be reached;");
	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
			throws CompileError {
		throw new CompileError("EquationMask::getRet should not be reached;");
		
	}

	@Override
	public Number getEstimate(BFCallToken token, Scope s) {
		// should not be reached
		return null;
	}

	@Override
	public boolean canConvert(BFCallToken token) {
		return true;
	}

	@Override
	public abstract Equation convert(BFCallToken token, Compiler c, Scope s, RStack stack) throws CompileError;

}
