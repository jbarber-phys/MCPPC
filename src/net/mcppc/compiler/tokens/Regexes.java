package net.mcppc.compiler.tokens;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.regex.Pattern;

import net.mcppc.compiler.errors.CompileError;

public final class Regexes {
	//https://regex101.com
	//https://docs.oracle.com/javase/7/docs/api/java/util/regex/package-summary.html
	//instead of python lastgroup, use a series of null checks
	//copy text and paste it in eclipse and it will automatically escape itself
	//if (m.group(5) == null) //not found
	
	//this one is used by some of the others
	private static final String STRLITSTRING = "\\\"(\\\\\\\"|[^\\\"\\n])*\\\"|\\'(\\\\'|[^'\\n])*\\'" ;// \"(\\\"|[^\"\n])*\"|\'(\\'|[^'\n])*\'
	
	
	public static final Pattern ANY_CHAR=Pattern.compile(".");// .

	public static final Pattern PARENS=Pattern.compile("(\\()|(\\))");// (\()|(\))
	public static final Pattern ANGLEBRACKETS=Pattern.compile("(<)|(>)");// (<)|(>)
	public static final Pattern ARGSEP=Pattern.compile(",");// ,
	public static final Pattern SPACE=Pattern.compile("[ \\t]+");// [ \t]+
	public static final Pattern SPACE_NEWLINE=Pattern.compile("\\n");// \n
	public static final Pattern LINE_END=Pattern.compile(";");// ;
	public static final Pattern ASSIGN=Pattern.compile("=");// =
	public static final Pattern ESTIMATE=Pattern.compile("~~");// ~~
	//the ~~ operator is used to estimate the value of a float-p to inform the compilers rounding decisions
	public static final Pattern MASK=Pattern.compile("->");// ->
	//public static final Pattern MEMBER=Pattern.compile("\\.");// \.
	public static final Pattern MEMBER=Pattern.compile("\\.(?=[^.]|$)");// \.(?=[^.]|$)
	public static final Pattern RANGESEP=Pattern.compile("\\.\\.");// \.\.
	public static final Pattern SCOREOF=Pattern.compile("::");// ::
	public static final Pattern TAGOF=Pattern.compile("\\.");// \.
	public static final Pattern NAME=Pattern.compile("[A-Za-z]\\w*");// [A-Za-z]\w*
	public static final Pattern REF_PREFIX=Pattern.compile("ref(?=[^\\w])");// ref(?=[^\w])
	public static final Pattern NULL_KEYWORD=Pattern.compile("null(?=[^\\w])");// null(?=[^\w])
	public static final Pattern CODEBLOCKBRACE=Pattern.compile("(\\{)|(\\})");// (\{)|(\})
	public static final Pattern CODEBLOCKBRACEDOUBLED=Pattern.compile("(\\{\\{)|(\\}\\})");// (\{\{)|(\}\})
	public static final Pattern COMMENT=Pattern.compile("\\/\\/([^\\n\\/][^\\n]*)(?=\\n|$)");// \/\/([^\n\/][^\n]*)(?=\n|$)
	public static final Pattern DOMMENT=Pattern.compile("\\/\\/\\/([^\\n]*)(?=\\n|$)");// \/\/\/([^\n]*)(?=\n|$)
	
	public static final Pattern SKIPLINE_MID=Pattern.compile("((%s)|[^;\\n])*\\n".formatted(STRLITSTRING)); // ((%s)|[^;\n])*\n
	public static final Pattern SKIPLINE_END=Pattern.compile("((%s)|[^;\\n])*;".formatted(STRLITSTRING)); // ((%s)|[^;\n])*;
	public static final Pattern CMD=Pattern.compile("\\$?/(((%s)|[^\\\"';\\n])*)(?=;|\\n|$)".formatted(STRLITSTRING));// \$?/(((%s)|[^\"';\n])*)(?=;|\n|$)
	public static final Pattern CMD_SAFE=Pattern.compile("\\$/(((%s)|[^\\\"';\\n])*)(?=;|\\n|$)".formatted(STRLITSTRING));// \$/(((%s)|[^\"';\n])*)(?=;|\n|$)

	public static final Pattern SELECTOR=Pattern.compile("(@?[\\w-]+)\\s*\\[(((%s)|[^\\\"\\[\\]\\n]|\\[\\[|\\]\\])*)\\]".formatted(STRLITSTRING));// (@?[\w-]+)\s*\[(((%s)|[^\"\[\]\n]|\[\[|\]\])*)\]
	//public static final Pattern COORDS_OLD=Pattern.compile("([\\^~]?[+-]?\\d+)\\s+([\\^~]?[+-]?\\d*)\\s+([\\^~]?[+-]?\\d*)");// ([\^~]?[+-]?\d+)\s+([\^~]?[+-]?\d*)\s+([\^~]?[+-]?\d*)
	
	//ungrouped: use (?<x>%s)
	public static final String TILDE_HAT_NUM = "(?!([\\^~]\\.|[\\^~]-)(?!\\d))([\\^~]\\d*\\.*\\d*)|((?!([\\^~]\\.|[\\^~]-)(?!\\d))(\\d+\\.*\\d*|\\d*\\.*\\d+))";
		// (?!([\^~]\.|[\^~]-)(?!\d))([\^~]\d*\.*\d*)|((?!([\^~]\.|[\^~]-)(?!\d))(\d+\.*\d*|\d*\.*\d+))
	public static final String TILDE_NOHAT_NUM = "(?!([~]\\.|[~]-)(?!\\d))([~]\\d*\\.*\\d*)|((?!([~]\\.|[~]-)(?!\\d))(\\d+\\.*\\d*|\\d*\\.*\\d+))";
		// (?!([~]\.|[~]-)(?!\d))([~]\d*\.*\d*)|((?!([~]\.|[~]-)(?!\d))(\d+\.*\d*|\d*\.*\d+))
	//public static final Pattern COORDS=Pattern.compile("([\\^~]?[+-]?\\d*)\\s+([\\^~]?[+-]?\\d*)\\s+([\\^~]?[+-]?\\d*)");// ([\^~]?[+-]?\d*)\s+([\^~]?[+-]?\d*)\s+([\^~]?[+-]?\d*)
	public static final Pattern COORDS=Pattern.compile("(?<x>%s)\\s+(?<y>%s)\\s+(?<z>%s)".formatted(TILDE_HAT_NUM,TILDE_HAT_NUM,TILDE_HAT_NUM));
		// (?<x>%s)\s+(?<y>%s)\s+(?<z>%s)

	//public static final Pattern ROTATION=Pattern.compile("([~]?[+-]?\\d*)\\s+([~]?[+-]?\\d*)");// ([~]?[+-]?\d*)\s+([~]?[+-]?\d*)
	public static final Pattern ROTATION=Pattern.compile("(?<ang1>%s)\\s+(?<ang2>%s)".formatted(TILDE_NOHAT_NUM,TILDE_NOHAT_NUM));
	// (?<ang1>%s)\s+(?<ang2>%s)
	
	//selector: escape [] for arrays by doubling them: {Pos[[1]]: 0d}
	public static final Pattern STRLIT=Pattern.compile(STRLITSTRING);// string escaping is so important that other regexes get to have it in them
	//I have double checked aand the STRLIT pattern is MC compadible
	public static final Pattern RESOURCELOCATION=Pattern.compile("(?<namespace>\\w+):(?<path>(\\w+\\/)*(?<end>\\w+))");// (?<namespace>\w+):(?<path>(\w+\/)*(?<end>\w+))
	//below only use if you know nbt is terminated by =~;\n
	public static final Pattern NBTPATH=Pattern.compile("((%s)|[^~;=\\n])+".formatted(STRLITSTRING));// ((%s)|[^~;=\n])+
	public static final Pattern STM_SKIP_MSCCHAR=Pattern.compile("[^\\\";{}@\\w/]+");// [^\";{}@\w/]+
	public static final Pattern FSLASH=Pattern.compile("/");// /
	//more carefull nbt tag parsers
	public static final Pattern NBT_OPEN=Pattern.compile("(?<segment>((%s)|[^~;=\\{\\}\\n])+)(\\{)".formatted(STRLITSTRING));// (?<segment>((%s)|[^~;=\{\}\n])+)(\{)
	public static final Pattern NBT_CLOSE=Pattern.compile("(?<segment>((%s)|[^~;=\\{\\}\\n])*)(\\})".formatted(STRLITSTRING));// (?<segment>((%s)|[^~;=\{\}\n])*)(\})
	public static final Pattern NBT_THROUGH=Pattern.compile("(?<segment>((%s)|[^~;=\\{\\}\\n])+)".formatted(STRLITSTRING));// (?<segment>((%s)|[^~;=\{\}\n])+)
	//https://minecraft.fandom.com/wiki/NBT_path_format?so=search
	//terminated by: \n, ; ~ =
	//but it can have nested square brackets or it wont recognize
	//currently allows muliline selector, jsut remember to replace \n's with spaces before inlining

	public static final Pattern OLD_NUM=Pattern.compile("(\\d+)(\\.\\d*)?([fdilsbFDILSB])?([Ee]\\-?\\d+)?");// (\d+)(\.\d*)?([fdilsbFDILSB])?([Ee]\-?\d+)?
	//positive only:
	public static final Pattern NUM=Pattern.compile("(\\d+)(\\.\\d*)?([Ee]\\-?\\d+)?([fdilsbFDILSB])?");// (\d+)(\.\d*)?([Ee]\-?\d+)?([fdilsbFDILSB])?
	//can be negative:
	public static final Pattern NUM_NEG=Pattern.compile("(-?\\d+)(\\.\\d*)?([Ee]\\-?\\d+)?([fdilsbFDILSB])?");// (-?\d+)(\.\d*)?([Ee]\-?\d+)?([fdilsbFDILSB])?

	public static final Pattern BOOL=Pattern.compile("(true)|(false)");// (true)|(false)
	public static final Pattern UNARY_MINUS=Pattern.compile("-");
	public static final Pattern UNARY_NOT=Pattern.compile("!");
	public static final Pattern EFLOP=Pattern.compile("\\^");// \^
	public static final Pattern MFLOP=Pattern.compile("(\\*)|(\\/)|(%)");// (\*)|(\/)|(%)
	public static final Pattern AFLOP=Pattern.compile("(\\+)|(\\-)");// (\+)|(\-)
	public static final Pattern COMPARE=Pattern.compile("(==)|(!=)|(>=|=>)|(<=|=<)|(>)|(<)");// (==)|(!=)|(>=|=>)|(<=|=<)|(>)|(<)
	public static final Pattern BILOGIC=Pattern.compile("(\\|!&)|(&)|(\\|)");// (\|!&)|(&)|(\|)
	
	public static final Pattern ALL_BI_OPERATOR=Pattern.compile("(\\^)|(\\*)|(\\/)|(%)|(\\+)|(\\-)|(==)|(!=)|(>=|=>)|(<=|=<)|(>)|(<)|(\\|!&)|(&)|(\\|)");// (\^)|(\*)|(\/)|(%)|(\+)|(\-)|(==)|(!=)|(>=|=>)|(<=|=<)|(>)|(<)|(\|!&)|(&)|(\|)

	public static final Pattern ALL_UN_OPERATOR=Pattern.compile("(-)|(!)");// (-)|(!)

	public static final Pattern LONE_TILDE=Pattern.compile("~");// ~
	//this is a special unary-like op
	/*
	 * below are for debug only:
	 */
	
	public static final Pattern NEXT_10_CHAR=Pattern.compile(".{1,10}");// .{1,10}
	
	/**
	 * 
	 * escape()
	 *
	 * Escape a give String to make it safe to be printed or stored.
	 * 
	 * Thx Dan: https://stackoverflow.com/questions/2406121/how-do-i-escape-a-string-in-java
	 *
	 * @param s The input String.
	 * @return The output String.
	 **/
	public static String escape(String s){
	  return s.replace("\\", "\\\\")
	          .replace("\t", "\\t")
	          .replace("\b", "\\b")
	          .replace("\n", "\\n")
	          .replace("\r", "\\r")
	          .replace("\f", "\\f")
	          .replace("\'", "\\'")
	          .replace("\"", "\\\"")
	          //DO NOT ESCAPE ESCAPE CHARS
	          //.replace("%", "%%")
	          ;
	}
	/**
	 * unescapes a string
	 * Thx DaoWen: https://stackoverflow.com/questions/3537706/how-to-unescape-a-java-string-literal-in-java
	 * @param s
	 * @return
	 * @throws CompileError
	 */
	public static String unescape(String s) throws CompileError{
		StreamTokenizer parser = new StreamTokenizer(new StringReader(s));
		String result;
		try {
		  parser.nextToken();
		  if (parser.ttype == '"') {
		    result = parser.sval;
		  }
		  else {
			  throw new CompileError("'ERROR' while unescaping string");
		  }
		}
		catch (IOException e) {
		  throw new CompileError("IOException while unescaping string");
		}
		return result;
	}
	public static String formatJsonWith(String format,String... with) {
		return "{\"translate\": \"%s\", \"with\": [%s]}".formatted(escape(format),String.join(" , ", with));
	}
	
}
