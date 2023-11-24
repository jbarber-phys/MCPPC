package net.mcppc.compiler.tokens;
import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.errors.CompileError;
/**
 * a token that must be identified (variable, function, const, etc)
 * @author RadiumE13
 *
 */
public interface Identifiable {
	public int identify(Compiler c,Scope s) throws CompileError;
}
