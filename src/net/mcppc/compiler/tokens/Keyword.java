package net.mcppc.compiler.tokens;

import java.util.HashMap;
import java.util.Map;

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
	WHILE("while"),
	EXECUTE("execute"),
	FOR("for"),
	BREAK("break"),
	RETURN("return"),
	CONST("const")
	
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
	
	
	
}
