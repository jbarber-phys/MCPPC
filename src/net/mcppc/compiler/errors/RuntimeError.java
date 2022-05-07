package net.mcppc.compiler.errors;

import java.io.PrintStream;

import net.mcppc.compiler.PrintF;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.Variable;

public abstract class RuntimeError {
	static final Selector TARGET = Selector.AT_A;
	public static void printf(PrintStream p,String prefix, String format,Variable... args) throws CompileError {
		PrintF.stderr.printf(p, prefix, TARGET, format, args);
	}
}
