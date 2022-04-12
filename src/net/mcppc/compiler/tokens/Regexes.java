package net.mcppc.compiler.tokens;

import java.util.regex.Pattern;

public final class Regexes {
	//https://regex101.com
	//https://docs.oracle.com/javase/7/docs/api/java/util/regex/package-summary.html
	//instead of python lastgroup, use a series of null checks
	//if (m.group(5) == null) //not found
	public static final Pattern ANY_CHAR=Pattern.compile(".");// .

	public static final Pattern PARENS=Pattern.compile("(\\()|(\\))");// (\()|(\))
	public static final Pattern ARGSEP=Pattern.compile(",");// ,
	public static final Pattern SPACE=Pattern.compile("[ \\t]+");// [ \t]+
	public static final Pattern SPACE_NEWLINE=Pattern.compile("\\n");// \n
	public static final Pattern LINE_END=Pattern.compile(";");// ;
	public static final Pattern ASSIGN=Pattern.compile("=");// =
	public static final Pattern ESTIMATE=Pattern.compile("~~");// ~~
	//the ~~ operator is used to estimate the value of a float-p to inform the compilers rounding decisions
	public static final Pattern MASK=Pattern.compile("->");// ->
	public static final Pattern MEMBER=Pattern.compile("\\.");// \.
	public static final Pattern SCOREOF=Pattern.compile("::");// ::
	public static final Pattern TAGOF=Pattern.compile("\\.");// \.
	public static final Pattern NAME=Pattern.compile("[A-Za-z]\\w*");// [A-Za-z]\w*
	public static final Pattern REF_PREFIX=Pattern.compile("ref(?=[^\\w])");// ref(?=[^\w])
	public static final Pattern NULL_KEYWORD=Pattern.compile("null(?=[^\\w])");// null(?=[^\w])
	public static final Pattern CODEBLOCKBRACE=Pattern.compile("(\\{)|(\\})");// (\{)|(\})
	public static final Pattern COMMENT=Pattern.compile("\\/\\/([^\\n\\/][^\\n]*)(?=\\n|$)");// \/\/([^\n\/][^\n]*)(?=\n|$)
	public static final Pattern DOMMENT=Pattern.compile("\\/\\/\\/([^\\n]*)(?=\\n|$)");// \/\/\/([^\n]*)(?=\n|$)
	
	public static final Pattern SKIPLINE_MID=Pattern.compile("(\\\"(\\\\.|[^\\\"\\\\])*\\\"|[^;\\n])*\\n"); // (\"(\\.|[^\"\\])*\"|[^;\n])*\n
	public static final Pattern SKIPLINE_END=Pattern.compile("(\\\"(\\\\.|[^\\\"\\\\])*\\\"|[^;\\n])*;"); // (\"(\\.|[^\"\\])*\"|[^;\n])*;
	public static final Pattern CMD=Pattern.compile("/((\\\"(\\\\.|[^\\\"\\\\])*\\\"|[^\\\";\\n])*)(?=;|\\n|$)");// /((\"(\\.|[^\"\\])*\"|[^\";\n])*)(?=;|\n|$)
	public static final Pattern SELECTOR=Pattern.compile("(@?\\w+)\\s*\\[((\\\"(\\\\.|[^\\\"\\\\])*\\\"|[^\\\"\\[\\]\\n]|\\[\\[|\\]\\])*)\\]");//selector prototype: (@?\w+)\s*\[((\"(\\.|[^\"\\])*\"|[^\"\[\]\n]|\[\[|\]\])*)\]
	public static final Pattern COORDS=Pattern.compile("([\\^~]?[+-]?\\d+)\\s+([\\^~]?[+-]?\\d*)\\s+([\\^~]?[+-]?\\d*)");// ([\^~]?[+-]?\d+)\s+([\^~]?[+-]?\d*)\s+([\^~]?[+-]?\d*)
	//selector: escape [] for arrays by doubling them: {Pos[[1]]: 0d}
	public static final Pattern STRLIT=Pattern.compile("\\\"[^\\\"]*\\\"|\\'[^\\']*\\'");// \"[^\"]*\"|\'[^\']*\'
	public static final Pattern RESOURCELOCATION=Pattern.compile("(?<namespace>\\w+):(?<path>(\\w+\\/)*(?<end>\\w+))");// (?<namespace>\w+):(?<path>(\w+\/)*(?<end>\w+))
	//below only use if you know nbt is terminated by =~;\n
	public static final Pattern NBTPATH=Pattern.compile("(\\\"(\\\\[^\\n]|[^\\\"\\\\])*\\\"|[^~;=\\n])+");// (\"(\\[^\n]|[^\"\\])*\"|[^~;=\n])+
	
	//more carefull nbt tag parsers
	public static final Pattern NBT_OPEN=Pattern.compile("(?<segment>(\\\"(\\\\[^\\n]|[^\\\"\\\\])*\\\"|[^~;=\\{\\}\\n])+)(\\{)");// (?<segment>(\"(\\[^\n]|[^\"\\])*\"|[^~;=\{\}\n])+)(\{)
	public static final Pattern NBT_CLOSE=Pattern.compile("(?<segment>(\\\"(\\\\[^\\n]|[^\\\"\\\\])*\\\"|[^~;=\\{\\}\\n])*)(\\})");// 
	public static final Pattern NBT_THROUGH=Pattern.compile("(?<segment>(\\\"(\\\\[^\\n]|[^\\\"\\\\])*\\\"|[^~;=\\{\\}\\n])+)");// (?<segment>(\"(\\[^\n]|[^\"\\])*\"|[^~;=\{\}\n])+)
	//https://minecraft.fandom.com/wiki/NBT_path_format?so=search
	//terminated by: \n, ; ~ =
	//but it can have nested square brackets or it wont recognize
	//currently allows muliline selector, jsut remember to replace \n's with spaces before inlining

	public static final Pattern NUM=Pattern.compile("(\\d+)(\\.\\d*)?([fdilsbFDILSB])?([Ee]\\-?\\d+)?");// (\d+)(\.\d*)?([fdilsbFDILSB])?([Ee]\-?\d+)?
	public static final Pattern BOOL=Pattern.compile("true|false");// true|false
	public static final Pattern UNARY_MINUS=Pattern.compile("-");
	public static final Pattern UNARY_NOT=Pattern.compile("!");
	public static final Pattern EFLOP=Pattern.compile("\\^");// \^
	public static final Pattern MFLOP=Pattern.compile("(\\*)|(\\/)|(%)");// (\*)|(\/)|(%)
	public static final Pattern AFLOP=Pattern.compile("(\\+)|(\\-)");// (\+)|(\-)
	public static final Pattern COMPARE=Pattern.compile("(==)|(!=)|(>=|=>)|(<=|=<)|(>)|(<)");// (==)|(!=)|(>=|=>)|(<=|=<)|(>)|(<)
	public static final Pattern BILOGIC=Pattern.compile("(\\|!&)|(&)|(\\|)");// (\|!&)|(&)|(\|)
	
	public static final Pattern ALL_BI_OPERATOR=Pattern.compile("(\\^)|(\\*)|(\\/)|(%)|(\\+)|(\\-)|(==)|(!=)|(>=|=>)|(<=|=<)|(>)|(<)|(\\|!&)|(&)|(\\|)");// (\^)|(\*)|(\/)|(%)|(\+)|(\-)|(==)|(!=)|(>=|=>)|(<=|=<)|(>)|(<)|(\|!&)|(&)|(\|)

	public static final Pattern ALL_UN_OPERATOR=Pattern.compile("(-)|(!)");// (-)|(!)
	
	/*
	 * below are for debug only:
	 */
	
	public static final Pattern NEXT_10_CHAR=Pattern.compile(".{1,10}");// .{1,10}
	
	
	
}
