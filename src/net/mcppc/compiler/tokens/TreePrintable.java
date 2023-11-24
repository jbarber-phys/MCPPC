package net.mcppc.compiler.tokens;

import java.io.PrintStream;

//public boolean isOpeningCodeBlock=false;
/**
 * used for token debug output
 * @author RadiumE13
 *
 */
public interface TreePrintable{
	public void printStatementTree(PrintStream p,int tabs);
}