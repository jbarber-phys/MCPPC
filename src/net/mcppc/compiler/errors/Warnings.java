package net.mcppc.compiler.errors;

import java.io.PrintStream;
import net.mcppc.compiler.Compiler;

/**
 * indicate problems with the code that do not rise to the level of an error
 * @author RadiumE13
 *
 */
public class Warnings {
	static PrintStream warn=System.err;
	//eclipse shows all err output in red
	public static enum OneTimeWarnings{
		TYPE64BIT("Warning: used datatype double or long, but these types may be downcasted due to minecraft scores being only 32 bits;"),
		LIKELYOVERFLOW("Warning: likely overflow of data in scoreboard;")
		
		;
		final String message;
		final MutableBoolean done;
		OneTimeWarnings(String message) {
			this.message=message;
			this.done=new MutableBoolean(false);
		}
		public static void reset() {
			for(OneTimeWarnings w:OneTimeWarnings.values()) {
				w.done.value=false;
			}
		}
	}
	public static void warning(String message, Compiler c) throws CompileError{
		warn.println(message);
		if(c!=null) {
			c.job.warn(message,c);
		}
	}
	public static void warningf(Compiler c,String message, Object... args) throws CompileError {
		warn.println(message.formatted(args));
		if(c!=null) {
			c.job.warn(message.formatted(args),c);
		}
	}
	public static void warn(OneTimeWarnings w,Compiler c)  throws CompileError{
		if(!w.done.value) {
			warn.println(w.message);
			
			if(c!=null) {
				c.job.warn(w.message,c);
			}
		}
		w.done.value=true;
	}
	
	public static class WError extends CompileError {
		public WError(String string) {
			super("(upgraded warning) " +string);
		}
		
	}
}
