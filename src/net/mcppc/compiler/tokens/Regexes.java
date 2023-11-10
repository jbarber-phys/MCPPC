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
	public static class Strs {
		public static final String STRLITSTRING = "\\\"(\\\\\\\"|[^\\\"\\n])*\\\"|\\'(\\\\'|[^'\\n])*\\'" ;// \"(\\\"|[^\"\n])*\"|\'(\\'|[^'\n])*\'
		public static final String BASIC_FUNC = "([a-zA-Z]\\w*)(?=\\s*\\()"; // ([a-zA-Z]\w*)(?=\s*\()
		public static final String NAME = "[A-Za-z]\\w*";// [A-Za-z]\w*;
		public static final String COMMENT_LINE=("\\/\\/([^\\n\\/][^\\n]*)(?=\\n|$)");// \/\/([^\n\/][^\n]*)(?=\n|$)
		public static final String DOMMENT_LINE=("\\/\\/\\/([^\\n]*)(?=\\n|$)");// \/\/\/([^\n]*)(?=\n|$)
		public static final String COMMENT_BLOCK=("/\\*(?!\\*)(([^\\*]|\\*(?!(/)))*)\\*/");// /\*(?!\*)(([^\*]|\*(?!(/)))*)\*/
		public static final String DOMMENT_BLOCK=("/\\*\\*(([^\\*]|\\*(?!(/)))*)\\*/");// /\*\*(([^\*]|\*(?!(/)))*)\*/
		
		public static final String CMDGROUP = "(?<cmd>((%s)|[^\\\"';\\\\\\n])*)".formatted(Strs.STRLITSTRING); 
		// (?<cmd>((%s)|[^\"';\\\n])*)

		public static final int cmd_group = 1;
		public static final String CMD_SAFE=("\\$/%s(?=;|\\n|$)".formatted(CMDGROUP));// \$/%s(?=;|\n|$)

		public static final String CMD_FORMATTED_START = "\\$\\(";//\$\(
		public static final String CMD_FORMATTED_END = "\\)";//\)
		
		
		public static final int player_group = 1;
		public static final String SELECTOR=("(@?[\\w-]+)\\s*\\[(((%s)|[^\\\"\\[\\]\\n]|\\[\\[|\\]\\])*)\\]".formatted(Strs.STRLITSTRING));// (@?[\w-]+)\s*\[(((%s)|[^\"\[\]\n]|\[\[|\]\])*)\]
		public static final String SELECTOR_ATONLY=("(@[\\w-]+)\\s*\\[(((%s)|[^\\\"\\[\\]\\n]|\\[\\[|\\]\\])*)\\]".formatted(Strs.STRLITSTRING));// (@[\w-]+)\s*\[(((%s)|[^\"\[\]\n]|\[\[|\]\])*)\]
		public static final String SELECTOR_NOAT=("([\\w-]+)\\s*\\[()\\]");// ([\w-]+)\s*\[()\]
		
		//confused with arrays
		//public static final String SELECTOR_START = "(@?[\\w-]+)\\s*\\[" ;//(@?[\w-]+)\s*\[
		public static final String SELECTOR_ATONLY_START = "(@[\\w-]+)\\s*\\[" ;//(@[\w-]+)\s*\[
		public static final String SELECTOR_ATONLY_ARGOPEN = "\\s*\\[" ;//\s*\[
		public static final String SELECTOR_ATONLY_NOARG = "(@[\\w-]+)" ;//(@[\w-]+)
		public static final String SELECTOR_END = "\\]" ;//\]
		
		public static final int selector_key_group = 1;
		public static final String SELECTOR_KEY = "\\s*([\\w/:\\.\\-+]+)\\s*=\\s*" ;//\s*([\w/:\.\-+]+)\s*=\s*
		
		public static final String SELECTOR_VAL_BASIC = "[!\\w\\.+\\-:/]*" ;//[!\w\.+\-:/]*
		public static final String SELECTOR_SEP = "\\s*,\\s*" ;//\s*,\s*


		public static final String NUM_NEG=("(-?\\d+)(\\.\\d*)?([Ee]\\-?\\d+)?([fdilsbFDILSB])?");// (-?\d+)(\.\d*)?([Ee]\-?\d+)?([fdilsbFDILSB])?
		//public static final String BOOL=("\\b(true)|(false)\\b");// \b(true)|(false)\b (\btrue\b)|(\bfalse\b)
		public static final String BOOL=("(\\btrue\\b)|(\\bfalse\\b)");// (\btrue\b)|(\bfalse\b)
		public static final String NULL=("\\bnull\\b");// \bnull\b

		private static final int tildehatnumsize=6;
		public static final int x_group = 1;
		public static final int y_group = 1 + (tildehatnumsize + 1);
		public static final int z_group = 1+ (tildehatnumsize + 1)*2;
		public static final int ang1group = x_group;
		public static final int ang2group = y_group;
		public static final String COORDS=("(?<x>%s)\\s+(?<y>%s)\\s+(?<z>%s)".formatted(TILDE_HAT_NUM,TILDE_HAT_NUM,TILDE_HAT_NUM));
		// (?<x>%s)\s+(?<y>%s)\s+(?<z>%s)
		public static final String ROTATION=("(?<ang1>%s)\\s+(?<ang2>%s)".formatted(TILDE_NOHAT_NUM,TILDE_NOHAT_NUM));
		// (?<ang1>%s)\s+(?<ang2>%s)
		
		public static final String RESOURCELOCATION=("(?<namespace>\\w+):(?<path>(\\w+\\/)*(?<end>\\w+))");// (?<namespace>\w+):(?<path>(\w+\/)*(?<end>\w+))
		
		public static final String ESTIMATE = "~~";
		
		public static final String CODELINE = ".*\\n|.+$"; //.*\n|.+$
	}
	public static final Pattern ANY_CHAR=Pattern.compile(".");// .

	public static final Pattern PARENS=Pattern.compile("(\\()|(\\))");// (\()|(\))
	public static final Pattern ANGLEBRACKETS=Pattern.compile("(<)|(>)");// (<)|(>)
	public static final Pattern ARGSEP=Pattern.compile(",");// ,
	public static final Pattern SPACE=Pattern.compile("[ \\t]+");// [ \t]+
	public static final Pattern SPACE_NEWLINE=Pattern.compile("\\n");// \n
	public static final Pattern LINE_END=Pattern.compile(";");// ;
	public static final Pattern COLON=Pattern.compile(":");// :
	public static final Pattern ASSIGN=Pattern.compile("=");// =
	public static final Pattern ESTIMATE=Pattern.compile(Strs.ESTIMATE);// ~~
	//the ~~ operator is used to estimate the value of a float-p to inform the compilers rounding decisions
	public static final Pattern MASK=Pattern.compile("->");// ->
	//public static final Pattern MEMBER=Pattern.compile("\\.");// \.
	public static final Pattern MEMBER=Pattern.compile("\\.(?=[^.]|$)");// \.(?=[^.]|$)
	public static final Pattern RANGESEP=Pattern.compile("\\.\\.");// \.\.
	public static final Pattern SCOREOF=Pattern.compile("::");// ::
	public static final Pattern TAGOF=Pattern.compile("\\.");// \.
	public static final Pattern BOSSBAR_W_NAME=Pattern.compile("---");// ---
	public static final Pattern NAME=Pattern.compile(Strs.NAME);
	public static final Pattern REF_PREFIX=Pattern.compile("ref(?=[^\\w])");// ref(?=[^\w])
	public static final Pattern NULL_KEYWORD=Pattern.compile(Strs.NULL);// null(?=[^\w])
	public static final Pattern CODEBLOCKBRACE=Pattern.compile("(\\{)|(\\})");// (\{)|(\})
	public static final Pattern CODEBLOCKBRACEDOUBLED=Pattern.compile("(\\{\\{)|(\\}\\})");// (\{\{)|(\}\})
	public static final Pattern INDEXBRACE=Pattern.compile("(\\[)|(\\])");// (\[)|(\])
	public static final Pattern COMMENT=Pattern.compile(Strs.COMMENT_LINE);
	public static final Pattern DOMMENT=Pattern.compile(Strs.DOMMENT_LINE);
	public static final Pattern COMMENT_BLOCK=Pattern.compile(Strs.COMMENT_BLOCK);
	public static final Pattern DOMMENT_BLOCK=Pattern.compile(Strs.DOMMENT_BLOCK);
	
	public static final Pattern SKIPLINE_MID=Pattern.compile("((%s)|[^;\\n])*\\n".formatted(Strs.STRLITSTRING)); // ((%s)|[^;\n])*\n
	public static final Pattern SKIPLINE_END=Pattern.compile("((%s)|[^;\\n])*;".formatted(Strs.STRLITSTRING)); // ((%s)|[^;\n])*;
	
	
	// (?<cmd>((%s)|[^\\\"';\\n])*) old
	public static final Pattern CMD=Pattern.compile("((?<=^|[^/])|\\$)/%s(?=;|\\n|$)".formatted(Strs.CMDGROUP));// ((?<=^|[^/])|\$)/%s(?=;|\n|$)

	public static final Pattern CMD_SAFE=Pattern.compile(Strs.CMD_SAFE);
	
	//a lookahead, priority below string literal
	public static final Pattern CMD_TERMINATOR = Pattern.compile("(?=;|\\n|$)");//(?=;|\n|$)
	public static final Pattern CMD_FORMATTED_START = Pattern.compile(Strs.CMD_FORMATTED_START);//\$\(

	
	@Deprecated
	public static final Pattern SELECTOR=Pattern.compile(Strs.SELECTOR);
	//public static final Pattern COORDS_OLD=Pattern.compile("([\\^~]?[+-]?\\d+)\\s+([\\^~]?[+-]?\\d*)\\s+([\\^~]?[+-]?\\d*)");// ([\^~]?[+-]?\d+)\s+([\^~]?[+-]?\d*)\s+([\^~]?[+-]?\d*)
	
	public static final Pattern SELECTOR_NAME=Pattern.compile(Strs.SELECTOR_ATONLY_NOARG);
	public static final Pattern SELECTOR_ARGOPEN=Pattern.compile(Strs.SELECTOR_ATONLY_ARGOPEN);
	public static final Pattern SELECTOR_KEY=Pattern.compile(Strs.SELECTOR_KEY);
	public static final Pattern SELECTOR_VAL_BASIC=Pattern.compile(Strs.SELECTOR_VAL_BASIC);
	public static final Pattern SELECTOR_SEP=Pattern.compile(Strs.SELECTOR_SEP);
	//also nbt and strlit
	public static final Pattern SELECTOR_END=Pattern.compile(Strs.SELECTOR_END);

	public static final Pattern SELECTOR_SCORE_START=Pattern.compile("\\s*\\{\\s*");//\s*\{\s*
	public static final Pattern SELECTOR_SCORE_END=Pattern.compile("\\s*\\}\\s*");//\s*\}\s*
	
	//ungrouped: use (?<x>%s)
	public static final String TILDE_HAT_NUM = "(-?(\\d+(\\.\\d*|)|\\.\\d+))|[\\^~](-?(\\d+(\\.\\d*|)|\\.\\d+))|[\\^~]";
		// (-?(\d+(\.\d*|)|\.\d+))|[\^~](-?(\d+(\.\d*|)|\.\d+))|[\^~]
		// 6 groups
	public static final String TILDE_NOHAT_NUM = "(-?(\\d+(\\.\\d*|)|\\.\\d+))|[~](-?(\\d+(\\.\\d*|)|\\.\\d+))|[~]";
		// (-?(\d+(\.\d*|)|\.\d+))|[~](-?(\d+(\.\d*|)|\.\d+))|[~]
	//public static final Pattern COORDS=Pattern.compile("([\\^~]?[+-]?\\d*)\\s+([\\^~]?[+-]?\\d*)\\s+([\\^~]?[+-]?\\d*)");// ([\^~]?[+-]?\d*)\s+([\^~]?[+-]?\d*)\s+([\^~]?[+-]?\d*)
	public static final Pattern COORDS=Pattern.compile(Strs.COORDS);

	//public static final Pattern ROTATION=Pattern.compile("([~]?[+-]?\\d*)\\s+([~]?[+-]?\\d*)");// ([~]?[+-]?\d*)\s+([~]?[+-]?\d*)
	public static final Pattern ROTATION=Pattern.compile(Strs.ROTATION);
	
	//selector: escape [] for arrays by doubling them: {Pos[[1]]: 0d}
	public static final Pattern STRLIT=Pattern.compile(Strs.STRLITSTRING);// string escaping is so important that other regexes get to have it in them
	//I have double checked aand the STRLIT pattern is MC compadible
	public static final Pattern RESOURCELOCATION=Pattern.compile(Strs.RESOURCELOCATION);
	//below only use if you know nbt is terminated by =~;\n
	public static final Pattern NBTPATH=Pattern.compile("((%s)|[^~;=\\n])+".formatted(Strs.STRLITSTRING));// ((%s)|[^~;=\n])+
	//public static final Pattern STM_SKIP_MSCCHAR=Pattern.compile("[^\\\";{}@\\w/]+");// [^\";{}@\w/]+
	public static final Pattern STM_SKIP_MSCCHAR=Pattern.compile("[^\\\";{}@\\w/$]+");// [^\";{}@\w/$]+
	
	public static final Pattern FSLASH=Pattern.compile("/");// /
	//more carefull nbt tag parsers
	public static final Pattern NBT_OPEN=Pattern.compile("(?<segment>((%s)|[^~;=\\{\\}\\n])+)(\\{)".formatted(Strs.STRLITSTRING));// (?<segment>((%s)|[^~;=\{\}\n])+)(\{)
	public static final Pattern NBT_CLOSE=Pattern.compile("(?<segment>((%s)|[^~;=\\{\\}\\n])*)(\\})".formatted(Strs.STRLITSTRING));// (?<segment>((%s)|[^~;=\{\}\n])*)(\})
	public static final Pattern NBT_THROUGH=Pattern.compile("(?<segment>((%s)|[^~;=\\{\\}\\n])+)".formatted(Strs.STRLITSTRING));// (?<segment>((%s)|[^~;=\{\}\n])+)
	//https://minecraft.fandom.com/wiki/NBT_path_format?so=search
	//terminated by: \n, ; ~ =
	//but it can have nested square brackets or it wont recognize
	//currently allows muliline selector, jsut remember to replace \n's with spaces before inlining

	public static final Pattern OLD_NUM=Pattern.compile("(\\d+)(\\.\\d*)?([fdilsbFDILSB])?([Ee]\\-?\\d+)?");// (\d+)(\.\d*)?([fdilsbFDILSB])?([Ee]\-?\d+)?
	//positive only:
	public static final Pattern NUM=Pattern.compile("(\\d+)(\\.\\d*)?([Ee]\\-?\\d+)?([fdilsbFDILSB])?");// (\d+)(\.\d*)?([Ee]\-?\d+)?([fdilsbFDILSB])?
	//can be negative:
	public static final Pattern NUM_NEG=Pattern.compile(Strs.NUM_NEG);

	public static final Pattern BOOL=Pattern.compile(Strs.BOOL);
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
	
	public static final Pattern CODELINE = Pattern.compile(Strs.CODELINE);
	public static final Pattern NEXT_10_CHAR=Pattern.compile(".{1,10}");// .{1,10}
	public static final Pattern NEXT_20_CHAR=Pattern.compile(".{1,20}");// .{1,10}
	
	/**
	 * 
	 * escape()
	 *
	 * Escape a give String to make it safe to be printed or stored.
	 * does not add quotes on ends
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
