package net.mcppc.compiler.errors;

import java.io.PrintStream;

public class Warnings {
	static PrintStream warn=System.err;
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
	public static void warn(OneTimeWarnings w) {
		if(!w.done.value)warn.println(w.message);
		w.done.value=true;
	}
}
