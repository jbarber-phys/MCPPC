package net.mcpp.vscode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Struct;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.tokens.Execute;
import net.mcppc.compiler.tokens.Keyword;
import net.mcppc.compiler.tokens.Regexes;

public class MakeTmLanguage extends Regexes.Strs{
	/*
	 * theme: Dark+ (vscode default)
	 * font colors allowed:
	 * keyword.control : light purple
	 * keyword: blue
	 * string: orange
	 * entity.name.type: aqua
	 * entity.name.function: light yellow
	 * comment : green
	 * variable : light cyan
	 * invalid.illegal : red
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
	
	private static final String keyword = "keyword.control.mcpp";
	private static final String basictype = "keyword.mcpp";
	private static final String consttype = basictype;//apply the bold after
	private static final String struct = "entity.name.type.mcpp";
	private static final String string = "string.mcpp";
	private static final String escapeChar = "constant.character.escape.mcpp";
	private static final String function = "entity.name.function.mcpp";
	private static final String comment = "comment.line.mcpp";
	private static final String domment = "comment.block.documentation.mcpp";
	private static final String variable = "variable.mcpp";
	private static final String invalid = "invalid.illegal.mcpp";
	private static final String number = "constant.numeric.mcpp"; // color is different than text just barely
	
	//no color:
	
	//shoehorned:
	private static final String bool = keyword; 
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
	

	private static final String italic = "markup.italic.mcpp";
	private static final String underlined = "markup.underline.mcpp";
	private static final String bold_blue = "markup.bold.mcpp";
	
	public static void make (String path) {
		System.out.println("making vscode extension");
		Map<String,Object> json = new HashMap<String,Object>();
		String scopeName = "source.mcpp";
		String name = "Mcpp";
		patterns = new ArrayList<Map>();
		repository = new HashMap<String,Object>();
		//add to repo;
		//comments + domments
		addToRepo("domments",namedMatch(domment,DOMMENT_LINE,italic));
		addToRepo("comments",namedMatch(comment,COMMENT_LINE));
		
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
		Map strings_double = Map.of("name", "string.quoted.double.mcpp",
				"begin", "\"",
				"end", "\"",
				"patterns", List.of(namedMatch("constant.character.escape.mcpp","\\\\."))
				);
		Map strings_single = Map.of("name", "string.quoted.single.mcpp",
				"begin", "'",
				"end", "'",
				"patterns", List.of(namedMatch("constant.character.escape.mcpp","\\\\."))
				);
		addToRepo("strings",strings_double);
		addToRepo("strings2",strings_single);
		//bools
		addToRepo("bools",namedMatch(bool,BOOL));
		//selector
		addToRepo("targetselectors",namedMatch(selector,SELECTOR,italic));
		//mcfunction calls
		//addToRepo("mcfs_simple",namedMatch(escapeChar,CMD_SAFE));
		Map mcfs = Map.of("name", escapeChar,
				"begin", CMD_START,
				"end", CMD_END,
				"patterns", 
				List.of(strings_double,strings_single,
						include("resourcelocations"),
						include("targetselectors"),
						namedMatch(selector,SELECTOR_BASIC,italic),
						namedMatch(keyword,regexMCFkeyword()),
						namedMatch(basictype,regexMCFsubkeyword())
						
						) //good enough
				);
		addToRepo("mcfs",mcfs);
		//resourcelocations
		addToRepo("resourcelocations",namedMatch(function,RESOURCELOCATION,underlined));
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
		json.put("patterns", patterns);
		json.put("repository", repository);
		PrintStream file=null;
		try{
			file = getOutput(path);
			JsonMaker.printAsJson(file, json, true, 0);
			System.out.println("successfully made vscode extension");
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(file!=null)file.close();
		}
	}
	private static void addToRepo(String key,Object value) {
		repository.put(key, value);
		patterns.add(include(key));
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
