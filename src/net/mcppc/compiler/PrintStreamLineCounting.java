package net.mcppc.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;

import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
/**
 * counds newlines that are printed to it
 * @author RadiumE13
 *
 */
public class PrintStreamLineCounting extends PrintStream {
	//to generate large numbers of methods, use context menu > source > generate constrocturs from super

	protected static final PrintStream outputSizeLog=System.out;
	private int lines=0;
	public int getLines() {
		return lines;
	}
	public void announceLines(String name) {
		outputSizeLog.printf("printed %d lines to %s.mcf;\n", this.getLines(),name);
	}

	public PrintStreamLineCounting(OutputStream out) {
		super(out);
	}

	public PrintStreamLineCounting(File file, Charset charset) throws IOException {
		super(file, charset);
	}

	public PrintStreamLineCounting(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
		super(file, csn);
	}

	public PrintStreamLineCounting(File file) throws FileNotFoundException {
		super(file);
	}

	public PrintStreamLineCounting(OutputStream out, boolean autoFlush, Charset charset) {
		super(out, autoFlush, charset);
	}

	public PrintStreamLineCounting(OutputStream out, boolean autoFlush, String encoding)
			throws UnsupportedEncodingException {
		super(out, autoFlush, encoding);
	}

	public PrintStreamLineCounting(OutputStream out, boolean autoFlush) {
		super(out, autoFlush);
	}

	public PrintStreamLineCounting(String fileName, Charset charset) throws IOException {
		super(fileName, charset);
	}

	public PrintStreamLineCounting(String fileName, String csn)
			throws FileNotFoundException, UnsupportedEncodingException {
		super(fileName, csn);
	}

	public PrintStreamLineCounting(String fileName) throws FileNotFoundException {
		super(fileName);
	}

	@Override
	public void print(char c) {
		super.print(c);
		if(c=='\n')this.lines++;
	}
	private void countNLs(String s) {
		for(char c:s.toCharArray())if(c=='\n')this.lines++;
	}
	@Override
	public void print(char[] s) {
		super.print(s);
		for(char c:s)if(c=='\n')this.lines++;
	}

	@Override
	public void print(String s) {
		super.print(s);
		this.countNLs(s);
	}

	@Override
	public void print(Object obj) {
		super.print(obj);
		this.countNLs(String.valueOf(obj));
	}

	@Override
	public void println() {
		super.println();
		this.lines++;
	}

	@Override
	public void println(boolean x) {
		super.println(x);
		this.lines++;
	}

	@Override
	public void println(char x) {
		super.println(x);
		if(x=='\n')this.lines++;
		this.lines++;
	}

	@Override
	public void println(int x) {
		super.println(x);
		this.lines++;
	}

	@Override
	public void println(long x) {
		super.println(x);
		this.lines++;
	}

	@Override
	public void println(float x) {
		super.println(x);
		this.lines++;
	}

	@Override
	public void println(double x) {
		super.println(x);
		this.lines++;
	}

	@Override
	public void println(char[] x) {
		super.println(x);
		for(char c:x)if(c=='\n')this.lines++;
		this.lines++;
	}

	@Override
	public void println(String x) {
		super.println(x);
		this.countNLs(x);
		this.lines++;
	}

	@Override
	public void println(Object x) {
		super.println(x);
		this.countNLs(String.valueOf(x));
		this.lines++;
	}

	@Override
	public PrintStream printf(String format, Object... args) {
		String s=format.formatted(args);
		this.countNLs(s);
		return super.printf(format, args);
		
	}

	@Override
	public PrintStream printf(Locale l, String format, Object... args) {
		try {
			Warnings.warning("PrintStreamLineCounter called printf(Locale,...); number of lines cannot be determined;", null);
		} catch (CompileError e) {
			e.printStackTrace();
		}
		return super.printf(l, format, args);
	}
	
	//methods (>source > ...):
	

}
