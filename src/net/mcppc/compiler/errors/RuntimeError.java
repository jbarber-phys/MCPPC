package net.mcppc.compiler.errors;

import java.io.PrintStream;

import net.mcppc.compiler.Selector;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.functions.PrintF;
import net.mcppc.compiler.functions.PrintF.IPrintable;
import net.mcppc.compiler.tokens.Regexes;

public abstract class RuntimeError {
	static final Selector TARGET = Selector.AT_A;
	public static void printf(PrintStream p,String prefix, String format,IPrintable... args) throws CompileError {
		PrintF.stderr.printf(p, prefix, TARGET, '"'+Regexes.escape(format)+'"', args);
	}
}
