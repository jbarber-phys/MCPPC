package net.mcppc.compiler.errors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class OutputDump extends OutputStream {
	public static final PrintStream out=new PrintStream(new OutputDump());
	@Override
	public void write(int b) throws IOException {
		// do nothing
	}
}
