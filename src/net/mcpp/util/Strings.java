package net.mcpp.util;

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
}
