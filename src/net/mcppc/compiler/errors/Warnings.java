package net.mcppc.compiler.errors;

import java.io.PrintStream;

/**
 * indicate problems with the code that do not rise to the level of an error
 * @author RadiumE13
 *
 */
//TODO collect a copy of all errors and warnings at the end of output
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
	public static void warning(String message) {
		warn.println(message);
	}
	public static void warningf(String message,Object... args) {
		warn.println(message.formatted(args));
	}
	public static void warn(OneTimeWarnings w) {
		if(!w.done.value)warn.println(w.message);
		w.done.value=true;
	}
}
