package net.mcpp.vscode;
//whenever eclipse goes bad do the following
/*
 * https://dev-answers.blogspot.com/2009/06/eclipse-build-errors-javalangobject.html
 * goto project > Properties
 * View the "Libraries" tab in the "Build Path" section
 * remove JRE system library
 * add library: JRE system library
 * apply and close
 * this should fix it
 */
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.tokens.Execute;
import net.mcppc.compiler.tokens.Keyword;
import net.mcppc.compiler.tokens.Regexes;
/**
 * creates a Textmate language json file for this language<p>
 * 
 * this will basically handle all nonn-programatic hilighting except for the following:<br>
 * * ./language-configuration.json has info on comments and brackets / auto closing pairs<br>
 * * ./package.json might be able to define custom colors, but right now it does not work<p>
 * 
 * currently, the vscode extension is a seperate project that uses the tmLanguage.json generated here<p>
 * compilation instruction notes (in a vscode-ext project):<br>
 * extension might not work in testing; if so, uninstall and repackage to test<br>
 * *	if worried, backup the old .vsix file<br>
 * to package >>> vsce package<br>
 * 		makes a ./package-name.vsix file<br>
 * to install >>> code --install-extension .\package-name.vsix
 * 
 * @author RadiumE13
 *
 */
public class MakeTmLanguage extends Regexes.Strs{
	/*
	 * theme: Dark+ (vscode default)
	 * font colors allowed:
	 * keyword.control : light purple
	 * keyword: blue
	 * string: orange
	 * character.escape : gold
	 * entity.name.type: aqua
	 * entity.name.function: light yellow
	 * comment : green
	 * variable : light cyan
	 * invalid.illegal : red
	 * 
	 * markup.underline : underlines text (can also apply a color)
	 * markup.italic : italics text (can also apply a color)
	 * markup.bold : blue AND bolded
	 */
	/*
	 * TextMate cannot capture named groups but can caputure the number of a named group: example (?<txt>asdf) can be captured as group 1;
	 * the innermost color takes precedent but font / color can mix
	 */
	
	private static List<Map> patterns;
	private static Map<String,Object> repository;
	
	public static final String CMD_START = "\\$/";
	public static final String CMD_END= "(?=;|\n)"; //may want to use lookahead
	
	public static final String SELECTOR_BASIC= "(@p|@r|@a|@e|@s)\\b"; // (@p|@r|@a|@e|@s)\b
	
	
	static final String keyword = "keyword.control.mcpp";
	static final String basictype = "keyword.mcpp";
	static final String consttype = basictype;//apply the bold after
	static final String struct = "entity.name.type.mcpp";
	static final String string = "string.mcpp";
	static final String escapeChar = "constant.character.escape.mcpp";
	static final String function = "entity.name.function.mcpp";
	static final String comment = "comment.line.mcpp";
	static final String domment = "comment.block.documentation.mcpp";
	static final String variable = "variable.mcpp";
	static final String invalid = "invalid.illegal.mcpp";
	static final String number = "constant.numeric.mcpp"; // color is different than text just barely
	
	//no color:
	public static final String operator = "operator.mcpp";
	
	//shoehorned:
	private static final String bool = keyword; 
	private static final String nulls = keyword; 
	private static final String selector = struct; // this is what mcf does
	private static final String color_x = invalid;
	private static final String color_y = comment;
	private static final String color_z = basictype;
	/* yellow purple
	private static final String color_ang1 = function;
	private static final String color_ang2 = keyword;
	*/
	private static final String color_ang1 = comment; //same as y-axis: pitch + right hand rule
	private static final String color_ang2 = keyword; // a mix of blue and red;
	

	static final String italic = "markup.italic.mcpp";
	static final String underlined = "markup.underline.mcpp";
	static final String bold_blue = "markup.bold.mcpp";//Vscode always blues the bold
	
	public static void make (String path) {
		Map<String,Object> json = makeTmLang();
		PrintStream file=null;
		try{
			file = getOutput(path);
			JsonMaker.printAsJson(file, json, true, 0);
			System.out.println("successfully made vscode extension");
			System.out.printf("path: %s\n",path);
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(file!=null)file.close();
		}
	}
	private static final String INCLUDABLE_MAIN = "global_patterns";
	@SuppressWarnings("unused")
	public static Map<String,Object> makeTmLang () {
		System.out.println("making vscode extension");
		Map<String,Object> json = new HashMap<String,Object>();
		String scopeName = "source.mcpp";
		String name = "Mcpp";
		patterns = new ArrayList<Map>();
		repository = new HashMap<String,Object>();
		//the name of the gobal pattern; if null then inline into patterns instead (the old way)
		
		//names of other included patterns in global
		final String stringLits = "strings";	
		final String resourcelocations="resourcelocations";
		final String targetSelectors = "targetselectors";
		
		//sub repo names
		final String selectorArgs = "selector_args";
		//add to repo;
		//comments + domments
		addToRepo("domments",namedMatch(domment,DOMMENT_LINE,italic));
		addToRepo("comments",namedMatch(comment,COMMENT_LINE));
		
		// these do not work for some reason (they are not recognized by vscode)
		//addToRepo("domments_block",namedMatch(domment,DOMMENT_BLOCK,italic));
		//addToRepo("comments_block",namedMatch(comment,COMMENT_BLOCK));
		//but thse work:
		Map bcomment =enclosed( comment, COMMENT_BLOCK_BEGIN, CDOMMENT_BLOCK_END,
				List.of(namedMatch(comment,COMMENT_BLOCK_CHAR),
						namedMatch(comment,COMMENT_BLOCK_NEWLINE)
						) );
		Map bdomment =enclosed( comment, DOMMENT_BLOCK_BEGIN, CDOMMENT_BLOCK_END,
				List.of(namedMatch(italic,COMMENT_BLOCK_CHAR),
						namedMatch(italic,COMMENT_BLOCK_NEWLINE)
						),italic );
		addToRepo("domments_block",bdomment);
		addToRepo("comments_block",bcomment);
		
		//estimate operator must be escaped
		addToRepo("operators",namedMatch(operator,ESTIMATE));
		//coords
		addToRepo("coords",namedMatch(underlined, COORDS,
				Map.of(x_group,color_x , y_group,color_y , z_group,color_z))
				);
		//rot
		addToRepo("rots",namedMatch(underlined, ROTATION,
				Map.of(ang1group,color_ang1 , ang2group,color_ang2))
				);
		//number
		addToRepo("numbers",namedMatch(number,NUM_NEG));
		//strings
		//TODO prohibit newlines
		Map strings_double = enclosed("string.quoted.double.mcpp", "\"", "\"", List.of(namedMatch("constant.character.escape.mcpp","\\\\.")));
		Map strings_single = enclosed("string.quoted.single.mcpp",  "'",  "'", List.of(namedMatch("constant.character.escape.mcpp","\\\\.")));
			
		addToRepo(stringLits,patterns(List.of(strings_double,strings_single)));
		//bools
		addToRepo("bools",namedMatch(bool,BOOL));
		addToRepo("nulls",namedMatch(nulls,NULL));
		//selector
		// split into @ and non-@ cases
		//addToRepo("targetselectors",namedMatch(selector,SELECTOR,italic));
		//addToRepo("targetselectors_at",namedMatch(selector,SELECTOR_ATONLY,italic)); //was replaced by new thing
		//addToRepo("targetselectors_noat",namedMatch(selector,SELECTOR_NOAT,italic));
		
		addSubRepo(selectorArgs, patterns(List.of(include(stringLits),
				enclosed(selector,"\\[","\\]",List.of(include(selectorArgs))))
				));//include strings and selectorArgPatterns
		Map selectorNewAt = enclosed(selector,SELECTOR_ATONLY_START,SELECTOR_END,List.of(include(selectorArgs)));
		addToRepo(targetSelectors,patterns(List.of(selectorNewAt,namedMatch(selector,SELECTOR_ATONLY_NOARG))));
		//TODO this is not entering the repo
		
		
		//mcfunction calls
		//addToRepo("mcfs_simple",namedMatch(escapeChar,CMD_SAFE));
		Map mcf_escapes = enclosed(variable,CMD_FORMATTED_START,CMD_FORMATTED_END,List.of(include(INCLUDABLE_MAIN)));
		Map mcfs =enclosed( escapeChar, CMD_START, CMD_END,
				List.of(include(stringLits),
						include(resourcelocations),
						include(targetSelectors),//may be deprecated
						//namedMatch(selector,SELECTOR_BASIC,italic),
						namedMatch(italic,CMD_NL_ESCAPED),
						namedMatch(keyword,regexMCFkeyword()),
						namedMatch(basictype,regexMCFsubkeyword()),
						mcf_escapes
						) //good enough
				);
		addToRepo("mcfs",mcfs);
		//resourcelocations
		addToRepo(resourcelocations,namedMatch(function,RESOURCELOCATION,underlined));
		//keywords
		
		addToRepo("keywords",namedMatch(keyword,regexBasicKeywords()));
		addToRepo("subexecutes",namedMatch(keyword,regexSubexecuteKeywords(),italic));
		//basicTypes
		addToRepo("basictypes",namedMatch(basictype,regexBasicType()));
		//const types:
		addToRepo("const_types",namedMatch(consttype,regexConstType(),bold_blue));
		//structs
		addToRepo("structs",namedMatch(struct,regexStructType()));
		//builtinFunctions
		//
		addToRepo("builtinfunctions",namedMatch(keyword,regexBuiltinFuncType(),italic));
		
		
		addToRepo("functions",namedMatch(function,BASIC_FUNC));
		addToRepo("names",namedMatch(variable,NAME));
		
		
		//finish
		json.put("scopeName", scopeName);
		json.put("name", name);
		if(INCLUDABLE_MAIN !=null) {
			addSubRepo(INCLUDABLE_MAIN,patterns(patterns));
			json.put("patterns", List.of(include(INCLUDABLE_MAIN)));
		}else {
			//the old way
			json.put("patterns", patterns);
		}
		json.put("repository", repository);
		
		return json;
	}
	/**
	 * adds a pattern to the repo and refs it in patterns
	 * @param key
	 * @param value
	 */
	private static void addToRepo(String key,Object value) {
		repository.put(key, value);
		patterns.add(include(key));
	}
	/**
	 * adds a pattern to the repo but doesn't register it as an outer scope pattern
	 * @param key
	 * @param value
	 */
	private static void addSubRepo(String key,Object value) {
		repository.put(key, value);
	}
	static Map unnamedMatch(String regex) {
		return Map.of("match",regex);
	}
	private static Map namedMatch(String name,String regex) {
		return Map.of("name",name,"match",regex);
	}
	private static Map namedMatch(String name,String regex, String innerName) {
		return namedMatch(name,regex,0,innerName);
	}
	private static Map namedMatch(String name,String regex,int group, String innerName) {
		return Map.of("name",name,"match",regex,
				"captures", Map.of(
						Integer.toString(group), Map.of("name",innerName)
						));
	}
	private static Map include(String key) {
		return Map.of("include","#" + key);
	}
	static Map uninclude(Map pattern,Map repository) {
		if(pattern.containsKey("include")) {

			String key = ((String) pattern.get("include")).substring(1);
			//System.err.println(repository.get(key).toString());
			return (Map) repository.get(key);
		}else return pattern;
	}
	private static Map patterns(List patterns) {
		return Map.of("patterns",patterns);
	}
	private static Map namedMatch(String name,String regex, Map<Integer,String> groupnames) {
		return Map.of("name",name,"match",regex,
				"captures",
						groupnames.entrySet().stream().collect(Collectors.toMap(
								e -> e.getKey().toString(), 
								e -> Map.of("name",e.getValue())
							)
						)
				);
	}
	private static Map enclosed(String name, Object begin, Object end, List patterns) {
		return Map.of("name",name,
				"begin",begin,
				"end",end,
				"patterns",patterns);
	}
	private static Map enclosed(String name, Object begin, Object end, List patterns, String bracketname) {
		//TODO does not work yet
		return Map.of("name",name,
				"begin",begin,
				"end",end,
				"beginCaptures",(Object)Map.of( Integer.toString(0), Map.of("name",bracketname) ),
				"endCaptures",(Object)Map.of( Integer.toString(0), Map.of("name",bracketname) ),
				"patterns",patterns);
	}
	private static String regexBasicKeywords() {
		String[] kws = new String[Keyword.values().length];
		kws=Arrays.asList(Keyword.values()).stream().map(kw -> kw.name).toList().toArray(kws);
		return "\\b(%s)\\b".formatted(String.join("|",	 kws));
	}
	private static String regexSubexecuteKeywords() {
		String[] kws = new String[Execute.SUBS.size()];
		kws=Execute.SUBS.keySet().toArray(kws);
		return "\\b(%s)\\b".formatted(String.join("|",	 kws));
	}
	private static String regexBasicType() {
		String[] kws = new String[VarType.Builtin.valuesNonStruct().length];
		kws=Arrays.asList(VarType.Builtin.valuesNonStruct()).stream().map(kw -> kw.typename).toList().toArray(kws);
		return "\\b(%s)\\b".formatted(String.join("|",	 kws));
	}
	private static String regexConstType() {
		String[] kws = new String[ConstType.values().length];
		kws=Arrays.asList(ConstType.values()).stream().map(kw -> kw.name).toList().toArray(kws);
		return "\\b(%s)\\b".formatted(String.join("|",	 kws));
	}
	private static String regexStructType() {
		return "\\b(%s)\\b".formatted(String.join("|",	 Struct.getStructNames()));
	}private static String regexBuiltinFuncType() {
		return "\\b(%s)(?=\\s*\\()".formatted(String.join("|",	 BuiltinFunction.getFuncNames()));
	}
	private static String regexMCFkeyword() {
		return "\\b(%s)\\b".formatted(String.join("|",	 MCF_KEYWORDS));
	}private static String regexMCFsubkeyword() {
		return "\\b(%s)\\b".formatted(String.join("|",	 MCF_KEYWORDS_SUB));
	}
	private static PrintStream getOutput(String path) throws IOException {
		Path p = path ==null 
				?CompileJob.getGeneratedResources().resolve("vscode").resolve("syntaxes").resolve("mcpp.tmLanguage.json")
				: Path.of(path);
				;
		File f=p.toFile(); f.getParentFile().mkdirs();
		f.createNewFile();
		return new PrintStream(p.toFile());
		
	}

	private static final String[] MCF_KEYWORDS = {
			//ignore sub commands like if / run
			"advancement",
			"attribute",
			"ban",
			"ban-ip",
			"banlist",
			"bossbar",
			"clear",
			"clone",
			"data",
			"datapack",
			"debug",
			"defaultgamemode",
			"deop",
			"difficulty",
			"effect",
			"enchant",
			"execute", 
			"experience",
			"fill",
			"forceload",
			"function",
			"gamemode",
			"gamerule",
			"give",
			"help",
			"item",
			"jfr",
			"kick",
			"kill",
			"list",
			"locate",
			"locatebiome",
			"loot",
			"me",
			"msg",
			"op",
			"pardon",
			"pardon-ip",
			"particle",
			"perf",
			"playsound",
			"publish",
			"recipe",
			"reload",
			"save-all",
			"save-off",
			"say",
			"schedule",
			"scoreboard",
			"seed",
			"setblock",
			"setidletimeout",
			"setworldspawn",
			"spawnpoint",
			"spectate",
			"spreadplayers",
			"stop",
			"stopsound",
			"summon",
			"tag",
			"team",
			"teammsg",
			"tm",
			"teleport",
			"tp",
			"tell",
			"tellraw",
			"time",
			"title",
			"trigger",
			"w",
			"weather",
			"whitelist",
			"worldborder",
			"xp"
	};
	//common sub args in commands
	private static final String[] MCF_KEYWORDS_SUB = {
			//ignore sub commands like if / run
			"if",
			"unless",
			"store",
			"score",
			"entity",
			"storage",
			"block",
			"blocks",
			"positioned",
			"in",
			"rotated",
			"as",
			"at",
			"facing",
			"anchored",
			"run",
			"get",
			"modify",
			"merge",
			"remove",
			"add",
			"set",
			"players",
			"objective",
			"setdisplay",
			"list",
			"force",
			"normal",
			"revoke",
	};
	
}
