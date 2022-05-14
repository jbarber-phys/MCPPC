package net.mcppc.compiler.tokens;

import java.io.PrintStream;

//public boolean isOpeningCodeBlock=false;
public interface TreePrintable{
	public void printStatementTree(PrintStream p,int tabs);
}