package net.mcpp.vscode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.*;

public class HighlightCode {
	public static class ThemeColors {
		public static final String BACKGROUND = "#1E1E1E";
		public static final String PURPLE = "#C586C0";
		public static final String BLUE = "	#569CD6";
		public static final String AQUA = "#4EC9B0";
		public static final String CYAN = "#9CDCFE";
		public static final String CREAM = "#DCDCAA";
		public static final String NUMBER = "#B5CEA8";
		public static final String ORANGE = "#CE9178";
		public static final String GOLD = "#D7BA7D";
		public static final String GREEN = "#6A9955";
		public static final String RED = "#F44747";
	}
	
	private static Map<String,Object> lang = null;

	private static List<Map> patterns = null;
	private static Map<String,Object> repository = null;
	private static int cursor;
	
	public static synchronized Object highlight(String line) {
		 if(lang==null ) {
			 lang= MakeTmLanguage.makeTmLang();
			 repository = (Map<String, Object>) lang.get("repository");
			 patterns = (List<Map>) lang.get("patterns");
		 }
		 Matcher m = Pattern.compile(".").matcher(line);
		 highlight(line,m,null,patterns);
		 return line;
		
	}
	private static synchronized Map<String, Object> highlight(String line,Matcher m,String end, List<Map> patterns) {
		 cursor = 0;
		 StringBuffer fstring = new StringBuffer();
		 boolean hasSubs=false;
		 while(m.regionStart() <line.length()) {
			 Object o = null;
			 for(Map pattern : patterns) {
				 o = find(line,m,pattern);
				 if(o==null)continue;
				 else break;
				 
			 }if(o==null && end!=null) {
				 o = find(line,m,MakeTmLanguage.unnamedMatch(end));
			 }
			 if(o==null) {
				 //no match
				 o = find(line,m,MakeTmLanguage.unnamedMatch("."));
			 }
			 if(o instanceof String) {
				 if(o.equals("%")) {
					 hasSubs=true;
					 fstring.append("%%");
				 }else fstring.append((String)o);
			 }
			 else if (o instanceof Map) {
				 hasSubs=true;
			 }else {
				 System.err.println("invalid tmlang object");
			 }
		 }
		 return null;
		
	}
	private static Object find(String s, Matcher m, Map pattern) {
		pattern = MakeTmLanguage.uninclude(pattern, repository);
		if(pattern.containsKey("begin")) {
			m.usePattern(Pattern.compile((String) pattern.get("begin")));
			if(m.lookingAt()) {
				String end = (String) pattern.get("end");
				List<Map> patterns = null;
				if(pattern.containsKey("patterns"))
					patterns = (List<Map>) pattern.get("patterns");
				m.region(m.end(), m.regionEnd());
				Map<String, Object> jsonText = highlight(s,m, end, patterns);
				
				if(pattern.containsKey("name")) {
					String name = (String) pattern.get("name");
					putFormat( jsonText,name);
					
				}
				m.region(m.end(), m.regionEnd());
				return jsonText;
			}else {
				return null;
			}
		}else if(pattern.containsKey("match")){
			m.usePattern(Pattern.compile((String) pattern.get("match")));
			if(m.lookingAt()) {
				//TODO
				if(pattern.containsKey("name")) {
					String name = (String) pattern.get("name");
					String text = m.group();
					Map<String, Object> jsonText;
					if(pattern.containsKey("patterns")) {
						jsonText = highlight(text,m,null,patterns);
					}else {
						jsonText = new HashMap<String, Object>();
						((Map<String, Object>) jsonText).put("translate", m.group());
					}
					putFormat( jsonText,name);
					m.region(m.end(), m.regionEnd());
					return jsonText;
				}
				else {
					String ret = m.group();
					m.region(m.end(), m.regionEnd());
					return ret;
				}
			}else {
				return null;
			}
		}
		return null;
	}
	private static Map<String,String> colors = Map.of(
			"keyword.control",ThemeColors.PURPLE,
			"keyword",ThemeColors.BLUE,
			"string",ThemeColors.ORANGE,
			"character.escape",ThemeColors.GOLD,
			"entity.name.type",ThemeColors.AQUA,
			"entity.name.function",ThemeColors.CREAM,
			"comment",ThemeColors.GREEN,
			"variable",ThemeColors.CYAN,
			"invalid.illegal",ThemeColors.RED,
			
			"markup.bold",ThemeColors.BLUE
			);
	private static void putFormat(Map<String,Object> jsontext,String name) {
		for(Entry<String,String> e: colors.entrySet()) {
			if(name.contains(e.getKey())) {
				jsontext.put("color", e.getValue());
				break;
			}
		}if(name.contains("markup.underline")) {
			jsontext.put("underlined", "true");
		}
		if(name.contains("markup.bold")) {
			jsontext.put("bold", "true");
		}
		if(name.contains("markup.italic")) {
			jsontext.put("italic", "true");
		}
		
		
	}
}
