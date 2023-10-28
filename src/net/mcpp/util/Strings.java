package net.mcpp.util;

import java.util.regex.Pattern;

import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;

public abstract class Strings {
	public static String getLiteral(String s){
		return getLiteral(s,"\"");
	}
	public static String getLiteral(String s,String quote){
		  s=s.replace("\\", "\\\\")
		          .replace("\t", "\\t")
		          .replace("\b", "\\b")
		          .replace("\n", "\\n")
		          .replace("\r", "\\r")
		          .replace("\f", "\\f")
		          //.replace("\'", "\\'")
		          .replace("\"", "\\\"")
		          //DO NOT ESCAPE ESCAPE CHARS
		          //.replace("%", "%%")
		          ;
		  return quote + s + quote;
		}
	

	public static final Pattern TAG_CHAR_NOTALLOWED = Pattern.compile("[^\\w.+-]"); // [^\w.+-]
	
	public static final Pattern OBJECTIVE_CHAR_NOTALLOWED= TAG_CHAR_NOTALLOWED;
	
	public static final String TAG_CHAR_SUBSTITUTE = "+";
	public static final String OBJECTIVE_CHAR_SUBSTITUTE = "+";
	
	public static String getTagSafeString(String in) {
		return TAG_CHAR_NOTALLOWED.matcher(in).replaceAll(TAG_CHAR_SUBSTITUTE);
	}
	public static String getObjectiveSafeString(String in) {
		return OBJECTIVE_CHAR_NOTALLOWED.matcher(in).replaceAll(OBJECTIVE_CHAR_SUBSTITUTE);
	}
}
