package net.mcppc.compiler.tokens;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.errors.CompileError;

/**
 * Keywords appear usually at the start of a statement and are not function-formatted usually
 * @author jbarb_t8a3esk
 *
 */

public enum Keyword {
	IF("if"),
	ELSE("else"),
	ELIF("elif"),
	PRIVATE("private"),
	PUBLIC("public"),
	EXTERN("extern"),
	IMPORT("import"),
	EXPORT("export"),
	WHILE("while"),
	EXECUTE("execute"),
	FOR("for"),
	BREAK("break"),
	RETURN("return"),
	THIS("this"),
	CONST("const"),
	REF("ref"),
	FINAL("final"),
	RECURSIVE("recursive"),
	THREAD("thread") ,//thread define
	NEXT("next") ,//thread subblock
	THEN("then") ,//thread subblock
	SYNCHRONIZED("synchronized") ,//states that a thread cannot have multiple running instances
	START("start"), //call to start a thread;
	RESTART("restart"), //call to restart a thread;
	STOP("stop"), //call to stop a thread;
	GOTO("goto"),
	WAIT("wait"),
	;
	
	public static final Map<String,Keyword> VALUES = new HashMap<String, Keyword>();
	static {
		for(Keyword k: Keyword.values()) {
			VALUES.put(k.name, k);	
		}
	}
	public final String name;
	Keyword(String name){
		this.name=name;
	}
	
	public static Keyword fromString(String text) {
        for (Keyword b : Keyword.values()) {
            if (b.name.equals(text)) {
                return b;
            }
        }
        return null;
    }
	public static boolean isKeyWord(String text) {
		return (fromString(text)!=null);
	}
	public static boolean checkFor(Compiler c, Matcher matcher,Keyword w) throws CompileError {
		int start=c.cursor;
		Token t=c.nextNonNullMatch(Factories.checkForKeyword);
		if(t instanceof Token.BasicName) {
			String name = ((Token.BasicName) t).name;
			Keyword k = fromString(name);
			boolean ret = k==w;
			if(!ret)c.cursor = start;
			return  ret;
		} else return false;
	}
	public static Keyword checkFor(Compiler c, Matcher matcher,Keyword... w) throws CompileError {
		int start=c.cursor;
		Token t=c.nextNonNullMatch(Factories.checkForKeyword);
		if(t instanceof Token.BasicName) {
			String name = ((Token.BasicName) t).name;
			Keyword k = fromString(name);
			for(int i=0;i<w.length;i++)if(k==w[i]) return w[i];
			c.cursor=start;
			return null;
		} else return null;
	}
	
	
	
}
