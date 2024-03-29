package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.*;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.Targeted;

/**
 * returns the size of a string, compound, or list (collection);
 * @author RadiumE13
 *
 */
public class Size extends BuiltinFunction{
	//gets the size of a String or list (nbt tag)
	//is a nonstatic member
	public Size(String name) {
		super(name);
	}
	@Override public boolean isNonstaticMember() {return true;}

	@Override
	public VarType getRetType(BFCallToken token, Scope s) {
		return VarType.INT;
	}

	@Override
	public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
		return BuiltinFunction.tokenizeArgsNone(c, matcher, line, col);
	}

	@Override
	public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		//nothing yet
	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
			throws CompileError {
		
		Size.lengthOf(p,  stack.getRegister(stackstart), token.getThisBound());
	}
	@Targeted
	public static void lengthOf(PrintStream p, Register to,Variable var) {
		String cmd = "data get %s".formatted(var.dataPhrase());
		String line = "execute store result score %s run %s\n".formatted(to.inCMD(),cmd);
		p.printf(line);
	}
	@Targeted
	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
			throws CompileError {
		String cmd = "data get %s".formatted(token.getThisBound().dataPhrase());
		v.setMeToCmd(p, s, cmd);
		
	}
	@Override
	public void dumpRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		//do nothing
	}
	@Override
	public Number getEstimate(BFCallToken token, Scope s) {
		return null;
	}
	public static class IsFull extends Size{
		public IsFull(String name) {
			super(name);
		}

		@Override
		public VarType getRetType(BFCallToken token, Scope s) {
			return VarType.BOOL;
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {
			super.getRet(p, c, s, token, stack, stackstart, typeWanted);
			stack.getRegister(stackstart).capValue(p, 1);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			int home = stack.reserve(1);
			this.getRet(p, c, s, token, stack, home, VarType.BOOL);
			v.setMe(p, s, stack.getRegister(home), this.getRetType(token, s));
		}
		
	}
}