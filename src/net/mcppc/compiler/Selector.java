package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcpp.util.MultiHashMap;
import net.mcpp.util.MultiMap;
import net.mcpp.util.Strings;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.NbtPath.NbtPathToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Token.Factory;

//TODO HTML ify docs
/**
 * object representing a target selector
 * 
 * Important notes on target selectors:
 *  a trailing comma is allowed but not a leading one
 *  limit=... is invalid in @@s
 *  there can be multiple tag, predicate, and nbt keys but only those
 *  inside advancements = {...} or scores = {...} , there may be duplicate keys
 *  
 *  
 *  note: raw names are not supported, use @@p[name=$username] instead
 *  
 * selectors can be intersected with the & operator, and the first one will take precedent for sorting
 * 
 * @author RadiumE13
 *
 */
public class Selector {

	//replace on group 3
	static final Pattern replace = Pattern.compile("\\\"(\\\\.|[^\\\"\\\\])*\\\"|(\\[\\[)|(\\]\\])");// \"(\\.|[^\"\\])*\"|(\[\[)|(\]\])
	
	//replace must go above the other static vars; consider adding to static block
	public static final Selector AT_S = new Selector("@s");
	public static final Selector AT_P = new Selector("@p");
	public static final Selector AT_A = new Selector("@a");
	public static final Selector AT_R = new Selector("@r");
	public static final Selector AT_E = new Selector("@e");
	public static final Selector NONE = new Selector();
	public static class SelectorToken extends Const.ConstLiteralToken{
		static class InvalidSelector extends CompileError{
			public InvalidSelector(SelectorToken token) {
				//this is mostly done on the token-factory level now
				super("invalid selector '%s'; non-@ player names may not contain args in the [] (empty [] are ok)"
						.formatted(token.selector().toCMD()));
			}
			
		}
		@Deprecated
		private static final Factory factory_old = new Factory(Regexes.SELECTOR) {
			@Override	public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				SelectorToken me=new SelectorToken(line,col,matcher);
				if(me.selector().hasUnusableArgs())throw new InvalidSelector(me);
				return me;
			}
		};
		@Deprecated
		private static final Factory carefullfactoryOld = new Factory(Regexes.SELECTOR) {
			@Override	public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				if(matcher.group(1).charAt(0) !='@' && matcher.group(2).length()!=0) {
					//invalid selector or a var with array access
					return new Token.WildChar(line, col, matcher.group(1).substring(0, 1));
				}
				c.cursor=matcher.end();
				SelectorToken me=new SelectorToken(line,col,matcher);
				if(me.selector().hasUnusableArgs())throw new InvalidSelector(me);
				return me;
			}
		};
		public static final Factory carefullfactory = new Factory(Regexes.SELECTOR_NAME) {
			@Override	public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				Selector val=new Selector(matcher.group());
				val.interperetArgs(c, matcher);
				SelectorToken me = new SelectorToken(line,col,val);
				if(me.selector().hasUnusableArgs())throw new InvalidSelector(me);
				return me;
			}
		};
		public static final Factory factory = carefullfactory;
		private final boolean garbage1232343___ = Struct.load();
		public final VarType type = new VarType(Entity.entities,new StructTypeParams.Blank());
		Selector val;public Selector selector() {return this.val;}
		public SelectorToken(int line, int col,Matcher m) {
			super(line, col,ConstType.SELECTOR);
		}
		public SelectorToken(int line, int col,Selector s) {
			super(line, col,ConstType.SELECTOR);
			val=s;
		}
		@Override public String asString() {
			return val.toString();
		}
		@Override
		public String textInHdr() {
			return this.selector().toHDR();
		}

		@Override
		public String resSuffix() {
			return "slctr_%s_%x".formatted(this.val.playerResCase(),this.valueHash());
		}
		@Override
		public int valueHash() {
			return this.val.hashCode();
		}
		@Override
		public String textInMcf() {
			return this.selector().toCMD();
		}
		@Override
		public boolean canCast(VarType type) {
			return false;
		}
		@Override
		public ConstExprToken constCast(VarType type) throws CompileError {
			throw new CompileError.UnsupportedCast( this.constType(),type);
		}
	}
	String player;
	//ready to be added to cmd
	//String argsCMD;
	//skip the replace [[ ]]
	//String argsHDR;
	
	private Map<String,String> argmap = new HashMap<String,String>();
	private List<String> tag = new ArrayList<String>();//may have empty elements
	private List<String> nbt = new ArrayList<String>();
	private List<String> predicate = new ArrayList<String>();
	//only the interior:
	private String scores = null;
	private String advancements = null;
	protected int nargs() {
		return argmap.size() + tag.size() + nbt.size() + predicate.size()
					+ (scores==null?0:1)
					+ (advancements==null?0:1)
					;
	}
	protected void interperetArgs(Compiler c,Matcher m) throws CompileError {
		//NbtPath.nextNbtCarefull(c, m);
		int end = m.end();
		int start = m.start();
		c.cursor=m.end();//name only
		m.region(c.cursor, m.regionEnd());
		//System.err.printf("%s\n", this.player);
		if (!m.usePattern(Regexes.SELECTOR_ARGOPEN) .lookingAt()) {
			//no args
			//System.err.printf("no args\n");
			//c.cursor=m.end();
			return;
		}
		c.cursor= m.end();
		//System.err.printf("args?\n");
		loop: while (true) {
			m.region(c.cursor, m.regionEnd());
			if (m.usePattern(Regexes.SELECTOR_KEY) .lookingAt()) {
				String key = m.group(Regexes.Strs.selector_key_group);
				//System.err.printf("%s = ...\n",key);
				c.cursor=m.end();
				m.region(c.cursor, m.regionEnd());
				switch (key) {
				case "nbt":{
					NbtPathToken ntag =NbtPath.nextNbtCarefull(c, m);
					String val = ntag.textInMcf();
					this.nbt.add(val);
					break;
				}
				case "scores": case "advancements" :{
					//find sub tag
					StringBuffer buff = new StringBuffer();
					if (!m.usePattern(Regexes.SELECTOR_SCORE_START) .lookingAt())
						throw new CompileError("failed to find { at start of %s argument".formatted(key));
					c.cursor=m.end();
					loop2: while (true) {
						m.region(c.cursor, m.regionEnd());
						//System.err.printf("scores...\n");
						if (m.usePattern(Regexes.SELECTOR_KEY) .lookingAt()) {
							buff.append(m.group());
							c.cursor=m.end();
							m.region(c.cursor, m.regionEnd());
							String subval;
							if (m.usePattern(Regexes.SELECTOR_VAL_BASIC).lookingAt()) {
								subval = m.group();
								c.cursor=m.end();
							}
							else if (m.usePattern(Regexes.STRLIT).lookingAt()) {
								subval = m.group();
								c.cursor=m.end();
							}else throw new CompileError("was unable to parse selector arg basic value");
							buff.append(subval);
							m.region(c.cursor, m.regionEnd());
							if(m.usePattern(Regexes.SELECTOR_SEP).lookingAt()) {
								buff.append(m.group());
								c.cursor=m.end();
								continue loop2;
							}
							else if(m.usePattern(Regexes.SELECTOR_SCORE_END).lookingAt()) {
								c.cursor=m.end();
								break loop2;
							}else {
								String s = m.usePattern(Regexes.NEXT_10_CHAR).lookingAt()? Strings.getLiteral(m.group(), "'"): "EOF";
								throw new CompileError("failed to find , or } in selector, found %s".formatted(s));
							}
						}else if (m.usePattern(Regexes.SELECTOR_SCORE_END) .lookingAt()) {
							c.cursor=m.end();
							break loop2;
						}
					}
					if(buff.length()==0) Warnings.warningf(c,"empty %s value {} found in selector", key);
					else switch (key) {
					case "scores": this.scores= buff.toString(); break;
					case "advancements":this.advancements= buff.toString(); break;
					}
					break;
				}
				default:
					String val;
					if (m.usePattern(Regexes.SELECTOR_VAL_BASIC).lookingAt()) {
						val = m.group();
						c.cursor=m.end();
					}
					else if (m.usePattern(Regexes.STRLIT).lookingAt()) {
						val = m.group();
						c.cursor=m.end();
					}else throw new CompileError("was unable to parse selector arg basic value");
					switch(key) {
					case "tag": this.tag.add(val); break;
					case "predicate": this.predicate.add(val); break;
					default: this.argmap.put(key, val); break;
					}
					c.cursor=m.end();
					break;
				}
				m.region(c.cursor, m.regionEnd());
				if(m.usePattern(Regexes.SELECTOR_SEP).lookingAt()) {
					c.cursor=m.end();
					continue loop;
				}
				else if(m.usePattern(Regexes.SELECTOR_END).lookingAt()) {
					c.cursor=m.end();
					//System.err.printf("]\n");
					break loop;
				}else throw new CompileError("failed to find , or ] in selector");
				
			}else if (m.usePattern(Regexes.SELECTOR_END) .lookingAt()) {
				c.cursor=m.end();
				//System.err.printf(",]\n");
				break loop;//good
			}else throw new CompileError("failed to find arg key or ] in selector");
			//pre-seperators are NOT allowed, only post seperators
		}
		if(this.player.equals(AT_S.player) && this.argmap.containsKey("limit")) {
			throw new CompileError("limit=... not valid inside @s selector; remove this arg");
		}
		//System.err.printf("%s :: %d : made selector %s\n",c.resourcelocation.toString(),c.line(),this.toCMD());
	}
	
	boolean hasArgs() {
		return this.nargs()>0;
	}
	boolean hasUnusableArgs() {
		//true if this is not an @ selector but still has non-empty []
		if(!this.hasArgs())return false;
		if(player.contains("@"))return false;
		return true;
	}
	//@Deprecated private Selector(Matcher m); //groups 1 and 2 were player and args
		//argsHDR used to have strip
	/**
	 * old function originally supposed to remove double brackets to avoid name collisions
	 * this is no longer used
	 * it used to be that this would be called on args to be put into MCF
	 * @param hdr
	 * @return
	 */
	@Deprecated private static String stripDoubles(String hdr) {
		StringBuffer args=new StringBuffer(hdr);
		Matcher m2 = replace.matcher(args);//this should be OK
		int idx=0;
		while(idx<args.length()&&m2.find(idx)) {
			idx=m2.end();
			if(m2.group(2)!=null) {
				args.replace(m2.start(2), m2.end(2), "[");idx--;
			}else if(m2.group(3)!=null) {
				args.replace(m2.start(3), m2.end(3), "]");idx--;
			}
		}
		return args.toString();
	}
	public Selector(String plr) {
		this.player=plr;
	}
	private Selector(String plr, Selector getArgs) {
		this(plr);
		//shallow copy
		this.argmap = new HashMap<String,String>(getArgs.argmap);
		this.tag = new ArrayList<String>(getArgs.tag);
		this.nbt = new ArrayList<String>(getArgs.nbt);
		this.predicate = new ArrayList<String>(getArgs.predicate);
		//strings are immutable
		this.scores = getArgs.scores;
		this.advancements = getArgs.advancements;
	}
	private Selector(Selector clone) {
		this(clone.player,clone);
	}
	public Selector(String plr, String tag, Integer limit) {
		this(plr,tag,limit,null);
	}
	public Selector(String plr, String tag, Integer limit,String type) {
		this(plr);
		this.tag.add(tag);
		if(limit!=null)this.argmap.put("limit", limit.toString());
		if(type!=null)this.argmap.put("type", type);
	}
	/**
	 * make the none selector
	 * @param plr
	 * @param getArgs
	 */
	private Selector() {
		this("@s");
		//two contradictign tag terms
		this.tag.add("");
		this.tag.add("!");
	}
	private static final String ARGSEP = ",";
	private static final String SUBARGS = "{%s}";
	protected String argsToString() {
		List<String> args = new ArrayList<String>();
		for(String tg : this.tag) {
			args.add("tag=%s".formatted(tg));
		}
		for(Entry<String, String> e : this.argmap.entrySet()) {
			args.add("%s=%s".formatted(e.getKey(),e.getValue()));
		}
		if(this.scores!=null) args.add("scores={%s}".formatted(this.scores));
		if(this.advancements!=null) args.add("advancements={%s}".formatted(this.advancements));
		for(String tg : this.predicate) {
			args.add("predicate=%s".formatted(tg));
		}
		for(String tg : this.nbt) {
			args.add("nbt=%s".formatted(tg));
		}
		String[] arr = new String[args.size()]; args.toArray(arr);
		return String.join(",", (String[])arr);
	}
	public String toCMD() {
		if(this.hasArgs())return "%s[%s]".formatted(player,this.argsToString());
		else return player;
	}
	public String toHDR() {
		if(this.hasArgs())return "%s[%s]".formatted(player,this.argsToString());
		else return player;
	}
	
	public String getJsonText() {
		return "{\"selector\": \"%s\"}".formatted(Regexes.escape(this.toCMD()));
	}
	@Override
	public String toString() {
		return this.toHDR();
	}
	private static boolean isTypePlayer(String type) {
		return type.equals("player") 
				|| type.equals("minecraft:player");
	}
	private static final Pattern NONWORD=Pattern.compile("[^\\w]");// [^\w]
	String playerResCase() {
		String s=this.player.toLowerCase();
		String p=s.charAt(0)=='@'?"at":"";
		return  p+NONWORD.matcher(s).replaceAll("_");
	}
	@Override public int hashCode() {
		return Objects.hash(this.player,this.argsToString());
	}
	public Selector playerify() {
		if(this.argmap.containsKey("type")) {
			if(isTypePlayer(this.argmap.get("type"))) ;//move on
			else return NONE;
		}
		switch (this.player) {
		case "@e": {
			Selector s = new Selector("@a",this);
			s.argmap.remove("type");
			return s;
		}
		case "@s":{
			Selector s = new Selector("@s",this);
			s.argmap.put("type", "player");
			return s;
		}default: return this;
		}
	}
	public Selector selfify() {
		if(this.player.equals("@s")) return this;
		Selector s = new Selector("@s",this);
		s.argmap.remove("limit");
		s.argmap.remove("sort");
		switch(this.player) {
		case "@p":case "@a":case "@r":
		s.argmap.put("type", "player");
		break;
		default:break;
		}
		return s;
	}
	public boolean isSelf() {
		return player.equals(AT_S.player);
	}
	public boolean isPlayer() {
		if(this.argmap.containsKey("type")) {
			if(isTypePlayer(this.argmap.get("type"))) return true;//move on
			else return false;
		}
		return this.isMyPlrPlayer();
	}
	public Selector limited(int i) {
		if(this.isSelf())return this;
		Selector l = new Selector(this);
		l.argmap.put("limit", Integer.toString(i));
		return l;
	}
	public Selector unlimited() {
		if(this.isSelf())return this;
		Selector l = new Selector(this);
		l.argmap.remove("limit");
		return l;
	}
	public void kill(PrintStream p) {
		p.printf("kill %s\n", this.toCMD());
	}
	public void addTag(PrintStream p, String tag) {
		p.printf("tag %s add %s\n", this.toCMD(),tag);
	}
	public void removeTag(PrintStream p, String tag) {
		p.printf("tag %s remove %s\n", this.toCMD(),tag);
	}
	public void run(PrintStream p,ResourceLocation fun) {
		p.printf("execute as %s run ", this.toCMD());
			fun.run(p);
	}
	public void runAt(PrintStream p,ResourceLocation fun) {
		p.printf("execute as %s at @s run ", this.toCMD());
			fun.run(p);
	}
	public static String mergeTypes(String type1,String type2) throws CompileError{
		//returns null if none
		String t1 = type1; String t2 = type2;
		boolean not1 = false; if (t1.startsWith("!")) {not1 = true;t1 = t1.substring(1);}
		boolean not2 = false; if (t2.startsWith("!")) {not2 = true;t2 = t2.substring(1);}

		t1 = t1.replaceFirst("minecraft:", "");
		t2 = t2.replaceFirst("minecraft:", "");
		boolean suffixesEqual = t1.equals(t2);
		if(not1 && not2 ) {
			if(suffixesEqual) return type1;
			else throw new CompileError("cannot merge types %s and %s".formatted(type1,type2));
		}else if(!(not1 || not2) ) {
			if(suffixesEqual) return type1;
			else return null;
		}else  {
			if (suffixesEqual) return null;
			else return not1? type2 : type1;
		}
	}
	private boolean isMyPlrPlayer() {
		return player.equals(AT_P.player) || player.equals(AT_R.player) || player.equals(AT_A.player);
	}private boolean isMyPlrUni() {
		return player.equals(AT_P.player) || player.equals(AT_R.player) ;//@s is limit invalid
	}
	//try to get rid of type,limit,sort statements if possible; make it readable; this should have no impact on function
	private void absorbeArgsIfPossible() {
		if(this.player.equals(AT_S.player)) return;
		//@e
		String type = this.argmap.get("type");
		if(type==null || !isTypePlayer(type)) return;
		this.player = AT_A.player; this.argmap.remove("type");
		boolean unify = false;
		String sort = this.argmap.get("sort");
		if(sort==null)return;
		if(sort.equals("nearest")) {
			this.player = AT_P.player;
		}else if(sort.equals("random")) {
			this.player = AT_R.player;
		}else {
			return;
		}
		this.argmap.remove("sort");
		if("1".equals(this.argmap.get("limit"))) this.argmap.remove("limit");
	}
	/**
	 * merges the two selector tags roughly by intersection
	 * 
	 * in the case of sort, the first sorter takes precedent
	 * @param s1
	 * @param s2
	 * @return
	 * @throws CompileError
	 */
	public static Selector softIntersection (Selector s1,Selector s2) throws CompileError{
		//System.err.printf("mergins selectors: %s & %s\n",s1.toCMD(),s2.toCMD());
		
		//type
		String type1 = s1.argmap.get("type"); if(s1.isMyPlrPlayer()) type1 = "player";
		String type2 = s2.argmap.get("type"); if(s2.isMyPlrPlayer()) type2 = "player";
		String typef = null;
		if(type1 != null && type2 != null) {
			typef = mergeTypes(type1, type2);
			if (typef == null) return NONE;
		}else if (type1 !=null) typef = type1;
		else typef = type2;
		
		//limit
		String limit1s = s1.argmap.get("limit");
		String limit2s = s2.argmap.get("limit");
		Integer limit1 = limit1s != null?Integer.parseInt(limit1s) :null ; if (s1.isMyPlrUni() && limit1==null) limit1 = 1;
		Integer limit2 = limit2s != null?Integer.parseInt(limit2s) :null ; if (s2.isMyPlrUni() && limit2==null) limit2 = 1;
		Integer limit = null;
		if(limit1 !=null && limit2 !=null) limit = Math.min(limit1,limit2);
		else if (limit1 !=null) limit = limit1; else limit = limit2;
		
		//sort
		String sort1 = s1.argmap.get("sort"); 
		if(s1.player.equals(AT_P.player)) sort1 = "nearest";
		else if (s1.player.equals(AT_R.player)) sort1 = "random";
		String sort2 = s2.argmap.get("sort"); 
		if(s2.player.equals(AT_P.player)) sort2 = "nearest";
		else if (s2.player.equals(AT_R.player)) sort2 = "random";
		String sort = sort1 !=null ? sort1 : sort2;
		
		boolean isSelf = s1.player.equals(AT_S.player) || s2.player.equals(AT_S.player) ;
		String plr = isSelf ? "@s": "@e";//plr absorbe later
		
		if(isSelf)limit=null;//not allowd in @s
		
		
		Selector sf = new Selector(plr);
		
		
		Map<String,String> kwargs1 = new HashMap<String,String>(s1.argmap);
		Map<String,String> kwargs2 = new HashMap<String,String>(s2.argmap);
		kwargs1.remove("type");kwargs1.remove("limit");kwargs1.remove("sort");
		kwargs2.remove("type");kwargs2.remove("limit");kwargs2.remove("sort");
		//check for unallowed double args
		for(String k: kwargs1.keySet()) {
			if(kwargs2.containsKey(k) && !kwargs1.get(k).equals(kwargs2.get(k))) {
				throw new CompileError("could not merge selectors:\n\t%s\n\t%s\n conflicting argument %s = ..."
						.formatted(s1.toString(),s2.toString(),k));
			}
		}
		sf.argmap = kwargs1; sf.argmap.putAll(kwargs2);
		//special args:
		if(typef!=null) sf.argmap.put("type", typef);
		if(limit!=null) sf.argmap.put("limit", limit.toString());
		if(sort!=null) sf.argmap.put("sort", sort);
		
		sf.tag = new ArrayList<String>(s1.tag);sf.tag.addAll(s2.tag);
		sf.nbt = new ArrayList<String>(s1.nbt);sf.nbt.addAll(s2.nbt);
		sf.predicate = new ArrayList<String>(s1.predicate);sf.predicate.addAll(s2.predicate);
		
		
		if(s1.scores!=null && s2.scores !=null) {
			sf.scores = String.join(ARGSEP, s1.scores,s2.scores);
		}else if (s1.scores !=null) sf.scores = s1.scores; else sf.scores = s2.scores;


		if(s1.advancements!=null && s2.advancements !=null) {
			sf.advancements = String.join(ARGSEP, s1.advancements,s2.advancements);
		}else if (s1.advancements !=null) sf.advancements = s1.advancements; else sf.advancements = s2.advancements;
		sf.absorbeArgsIfPossible();
		//System.err.printf("result: %s\n", sf.toCMD());
		return sf;
	}
	public static SelectorToken intersect(ConstExprToken a, ConstExprToken b) throws CompileError {
		return new SelectorToken(a.line,a.col,softIntersection(((SelectorToken)a).val,((SelectorToken)b).val));
	}
	public static void registerOps() {
		Const.ConstType sl = Const.ConstType.SELECTOR;
		Const.registerBiOp(sl, BiOperator.OpType.AND, sl, Selector::intersect);
		
	}
	public boolean hasTag(String tag) {
		return this.tag.contains(tag);
	}
}
