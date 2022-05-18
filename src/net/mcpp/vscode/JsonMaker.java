package net.mcpp.vscode;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.mcppc.compiler.tokens.Regexes;

public class JsonMaker {
	public static String fillString(int length, char charToFill) {
		//THX unwind : https://stackoverflow.com/questions/1802915/java-create-a-new-string-instance-with-specified-length-and-filled-with-specif
		  if (length > 0) {
		    char[] array = new char[length];
		    Arrays.fill(array, charToFill);
		    return new String(array);
		  }
		  return "";
		}
	private static String escapeJson(String s){
		  return s.replace("\\", "\\\\")
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
		}
	public static void printAsJson(PrintStream file, Object in, boolean format, int tabs) {
		String nlt = format? "\n" +fillString(tabs,'\t'): "";
		String comma =format?","+nlt: ", ";
		String colon = ": ";
		String tab = format?"\t":"";
		if(in instanceof String) {
			file.printf("\"%s\"",escapeJson((String) in));
		} else if (in instanceof Number) {
			file.printf("%s",in.toString());
		} else if (in instanceof Boolean) {
			file.printf("%s",in.toString());
		} else if (in instanceof Map) {
			file.printf("{%s",nlt);
			Iterator entryset = ((Map) in).entrySet().iterator();
			while(entryset.hasNext()) {
				Entry e=(Entry) entryset.next();
				if(!(e.getKey() instanceof String)) {
					throw new IllegalArgumentException("map keys must be strings for json");
				}
				file.printf(tab);
				file.printf("\"%s\"%s",Regexes.escape((String) e.getKey()),colon);
				printAsJson(file,e.getValue(),format,tabs+1);
				if(entryset.hasNext()) {
					file.print(comma);
				}else {
					file.print(nlt);
				}
			}
			file.printf("}");
		}
		else if (in instanceof Collection) {
			file.printf("[%s",nlt);
			Iterator it = ((Collection)in).iterator();
			while(it.hasNext()) {
				Object ell = it.next();
				file.printf(tab);
				printAsJson(file,ell,format,tabs+1);
				if(it.hasNext()) {
					file.print(comma);
				}else {
					file.print(nlt);
				}
			}
			file.printf("]");
		}
	}
}
