package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Function.FuncCallToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.struct.NbtCompound;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.MemberName;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.TemplateArgsToken;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Token.BasicName;
import net.mcppc.compiler.tokens.Token.StringToken;
import net.mcppc.compiler.tokens.TreePrintable;
/**
 * prints a string with formatting to the console
 * this is an abstraction of /tellraw...
 * default target is :@s but lead arg with a selector to change:
 * args:
 * ([selector target], <string format>, [selector or equation param]...)
 * 
 * json text format: https://minecraft.fandom.com/wiki/Raw_JSON_text_format?so=search#Plain_Text
 * 
 * 
 * TODO consider preventing translation confilcts using {"translate": "", "fallback": format,...}
 * 
 * @author RadiumE13
 *
 */

public class PrintF extends BuiltinFunction{
	public static void registerAll() {
		BuiltinFunction.register(stdout);
		BuiltinFunction.register(stderr);
		BuiltinFunction.register(stdwarn);
		
		BuiltinFunction.register(TitleF.titlef);
		BuiltinFunction.register(TitleF.subtitlef);
		BuiltinFunction.register(TitleF.actionbarf);

		BuiltinFunction.register(FormatF.format);
		BuiltinFunction.register(FormatF.formatLit);
	}
	/**
	 * anything that can be turned into a json text element; common examples would be variables and const values;
	 * @author RadiumE13
	 *
	 */
	public static interface IPrintable {
		public String getJsonTextSafe();
		public static PString string(String s) {return new PString(s);}
		public static class PString implements IPrintable{
			private final String lit;
			public PString(String s) {
				this.lit=Regexes.escape(s);
			}
			@Override
			public String getJsonTextSafe() {
				return "{\"text\": \"%s\"}".formatted(lit);
			}
			
		}
	}
	/**
	 * contains information about the current json text element being created;
	 * this is used to prevent name collisions between buffer variables, and about when to use them;
	 * @author RadiumE13
	 *
	 */
	public static class PrintContext{
		final String base;
		final List<Integer> withs = new ArrayList<Integer>();
		final boolean varCopy;
		public PrintContext(String base,boolean varCopy) {
			this.base=base;
			this.varCopy= varCopy;
		}
		public PrintContext(PrintContext parent,int withIndex) {
			this.base=parent.base;
			this.withs.addAll(parent.withs);
			this.withs.add(withIndex);
			this.varCopy = parent.varCopy;
		}
		/**
		 * returns a variable to be used by a printing statement as a buffer for values;
		 * this is done to maintain the values at their position in the execution order even if later
		 * functions in the format elements alter them;
		 * note that {@link FormatF.formatLit} will not use this (it will issue a warning if required to) because the buffer values are temporary, but
		 * a literal json text might not be resolved until later;
		 * @param index
		 * @param c
		 * @param s
		 * @param type
		 * @return
		 * @throws CompileError
		 */
		public Variable getPrintVar(int index,Compiler c,Scope s,VarType type) throws CompileError{
			if(!this.varCopy) Warnings.warning("attempted to copy data in a non-copying format function; possible loss of buffered variable values;", c);
			//ResourceLocation res = c.resourcelocation;// this could lead to conflict
			ResourceLocation res =s.getSubResNoTemplate();
			StringBuffer path = new StringBuffer();
			path.append("\"$%s\"".formatted(this.base));
			for(int withi : this.withs) path.append(".\"$with_%d\"".formatted(withi));
			path.append(".\"$%d\"".formatted(index));
			NbtPath anonvn=new NbtPath(path.toString());
			Variable anon=new Variable("anon",type,null,c).maskStorageAllocatable(res, anonvn);
			return anon;
		}
		public boolean isTopLevel() {
			return this.withs.isEmpty();
		}
		private boolean allocated = false;
		public void ensureAllocate(PrintStream p,Compiler c,Scope s) throws CompileError {
			//only allocate the top level var as an empty compound; all subvars can auto generate from tag {}
			//still must make sure it is a compound;
			if(allocated) return;//never allocate twice;
			allocated=true;
			if(!this.isTopLevel()) return;//only allocate at top level
			ResourceLocation res =s.getSubResNoTemplate();//TODO test this
			NbtPath anonvn=new NbtPath("\"$%s\"".formatted(this.base));//the outermost thing
			Variable anon=new Variable("anon",NbtCompound.TAG_COMPOUND,null,c).maskStorageAllocatable(res, anonvn);
			anon.allocateLoad(p, true);
		}
	}
	public static abstract class TextColors{
		public static final String RESET="reset";
		

		public static final String BLACK="black";
		public static final String DARK_BLUE="dark_blue";
		public static final String DARK_GREEN="dark_green";
		public static final String DARK_AQUA="dark_aqua";
		public static final String DARK_RED="dark_red";
		public static final String DARK_PURPLE="dark_purple";
		public static final String GOLD="gold";
		public static final String GRAY="gray";
		public static final String DARK_GRAY="dark_gray";
		public static final String BLUE="blue";
		public static final String GREEN="green";
		public static final String AQUA="aqua";
		public static final String RED="red";
		public static final String LIGHT_PURPLE="light_purple";
		public static final String YELLOW="yellow";
		public static final String WHITE="white";
		
		public static final String CUSTOM_BROWN=rgb(127,63,0);
		

		/*
		 * note: mc accepts #xxxxxx hex strings as colors for text
		 */
		public static String rgb(int r,int g,int b) {
			return String.format("#%02X%02X%02X", r,g,b);
		}
		
	}
	public static final PrintF stdout = new PrintF("printf",TextColors.WHITE);
	public static final PrintF stdwarn = new PrintF("warnf",TextColors.GOLD);
	public static final PrintF stderr = new PrintF("errorf",TextColors.DARK_RED);//TODO consider adding a default click statement to open file that errors
	
	
	
	public final String color;
	public PrintF(String name,String color) {
		super(name);
		this.color=color;
		
		//ignore fields for bold, underline, etc for now
		//consider adding clickEvents for ease of debugging
	}
	
	@Override
	public VarType getRetType(BFCallToken token) {
		return VarType.VOID;
	}
	@Override
	public void getRet(PrintStream p, Compiler c, Scope s,  BFCallToken token, RStack stack, int stackstart, VarType typeWanted) throws CompileError{
		throw new CompileError.UnsupportedCast( this.getRetType(token),stack.getVarType(stackstart));
	}
	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack) throws CompileError{
		throw new CompileError.UnsupportedCast( this.getRetType(token),v.type);
	}
	@Override
	public Number getEstimate(BFCallToken token) {
		return null;
	}
	private static final Token.Factory[] testForSelector = Factories.genericCheck(Selector.SelectorToken.factory);
	private static final Token.Factory[] nextFormatString = Factories.genericCheck(Token.StringToken.factory);
	public static class PrintfArgs implements Args{
		final Selector s ;
		final String lit;
		public final List<Token> targs=new ArrayList<Token>();

		public final Map<String,Token> fkwargs=new HashMap<String,Token>();
		
		public FuncCallToken runFunc = null;
		public PrintfArgs(Selector s,String lit) {
			this.s=s;
			this.lit=lit;
		}
		
	}
	public static final String ONCLICK_FUNCTION = "run"; 
	static BasicName checkForFormatArg(Compiler c, Scope s, Matcher matcher, int line,int col, RStack stack) throws CompileError {
		Token.Factory[] look = Factories.genericCheck(Token.BasicName.factory);
		Token.Factory[] look2 = Factories.genericCheck(Token.Assignlike.factoryAssign);
		int begin = c.cursor;
		Token t=c.nextNonNullMatch(look);
		if(!(t instanceof BasicName)) return null;
		Token op = c.nextNonNullMatch(look2);
		if(op instanceof Token.Assignlike) {
			//keep going
		}else {
			//reset
			c.cursor = begin;
			return null;
		}
		//System.err.printf("found a format arg: %s\n", t.asString());
		return (BasicName) t;
	}
	public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line,int col, RStack stack)throws CompileError {
		return PrintF.tokenizeFormatArgs(c, s, matcher, line, col, stack,true);
	}
	/**
	 * tokenizes args of a format similar to printf, but with the argument for a reciever of the text being optional
	 * @param c
	 * @param s
	 * @param matcher
	 * @param line
	 * @param col
	 * @param stack
	 * @param hasReciver
	 * @return
	 * @throws CompileError
	 */
	public static Args tokenizeFormatArgs(Compiler c, Scope s, Matcher matcher, int line,int col, RStack stack,boolean hasReciver)throws CompileError {
		Selector reciver=null;
		ConstExprToken t;
		if(hasReciver) {
			reciver=Selector.AT_S;
			t=Const.checkForExpressionSafe(c,s, matcher, line, col, ConstType.SELECTOR,ConstType.STRLIT);
					//c.nextNonNullMatch(testForSelector);
			
			if(t!=null &&t instanceof Selector.SelectorToken) {
				reciver=((Selector.SelectorToken) t).selector();
				if(!BuiltinFunction.findArgsep(c))new CompileError("not enough args in printf(...)");
				t=Const.checkForExpressionSafe(c,s, matcher, line, col,ConstType.STRLIT);
			}
		}else {
			t=Const.checkForExpressionSafe(c,s, matcher, line, col,ConstType.STRLIT);
		}
		
		//t=c.nextNonNullMatch(nextFormatString);
		
		if(t==null ||!(t instanceof Token.StringToken)) {
			Selector s2=Entity.checkForEntityVar(c, s, matcher, line, col);
			if(s2==null) {
				String next = c.getNextChars();
				throw new CompileError("printf: unexpected first argument: %s...".formatted(next));
			}
			reciver=s2;
			if(!BuiltinFunction.findArgsep(c))new CompileError("not enough args in printf(...)");
			t=Const.checkForExpression(c,s, matcher, line, col,ConstType.STRLIT);
		}
		if(reciver!=null)reciver=reciver.playerify();
		String litFstring=((StringToken) t).literal();
		//CompileJob.compileMcfLog.printf("printf: %s, %s;\n",s,litFstring);
		PrintfArgs args=new PrintfArgs(reciver,litFstring);
		boolean moreArgs=BuiltinFunction.findArgsep(c);
		BasicName kwarg = null;
		while(moreArgs) {
			//t=c.nextNonNullMatch(testForSelector);
			kwarg = PrintF.checkForFormatArg(c, s, matcher, line, col, stack);
			if(kwarg!=null) {
				break;
			}
			t=Const.checkForExpressionSafe(c,s, matcher, line, col, ConstType.SELECTOR);
			if(t!=null && t instanceof Selector.SelectorToken) {
				args.targs.add(t);
				moreArgs =BuiltinFunction.findArgsep(c);
			}else {
				Equation eq=Equation.toArgue(line, col, c, matcher, s);
				args.targs.add(eq);
				moreArgs=!eq.wasLastArg();
			}
		}
		boolean first=true;
		if(kwarg!=null) while (moreArgs) {
			if(first) first=false;
			else kwarg = PrintF.checkForFormatArg(c, s, matcher, line, col, stack);
			if(kwarg==null) {
				throw new CompileError("failed to find a <kwarg>=... term in json format function before close paren");
			}
			//System.err.printf("found arg %s\n", kwarg.name);
			if(kwarg.name.equals(ONCLICK_FUNCTION)) {
				Token memb = c.nextNonNullMatch(Factories.checkForMembName);
				if(!(memb instanceof MemberName)) throw new CompileError.UnexpectedToken(t, "function name (with no paren or args)");
				int pc=c.cursor;
				//check for function template 
				TemplateArgsToken tempargs=null;
				Function f=c.myInterface.checkForFunctionWithTemplate(((MemberName) memb).names, s);
				if(f!=null) {
					tempargs=TemplateArgsToken.checkForArgs(c, s, matcher);
					if(tempargs==null)c.cursor=pc;
				}
				args.runFunc =new Function.FuncCallToken( memb.line, memb.col,(MemberName) memb) ;
				args.runFunc.identify(c,s);
				if(tempargs!=null)args.runFunc.withTemplate(tempargs);
				args.runFunc.linkMe(c, s);
				moreArgs = BuiltinFunction.findArgsep(c);
				continue;
			}
			Equation eq=Equation.toArgue(line, col, c, matcher, s);
			args.fkwargs.put(kwarg.name, eq);
			moreArgs=!eq.wasLastArg();
		}
		return args;
		
	}
	public static boolean ESCAPE_TAG_IN_JSON = true;//TODO test in mc to see what should be done

	@Override
	public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		this.call(p, c, s, token.getArgs(), stack, "");
	}
	public void call(PrintStream p, Compiler c, Scope s, Args args, RStack stack,String prefix) throws CompileError {
		PrintfArgs pargs=(PrintfArgs) args;
		PrintContext context = new PrintContext("printf",true);
		String json = compileJsonTextElement(p, c, s, pargs, stack, this.color,context);
		p.printf("%stellraw %s %s\n"
				,prefix
				,pargs.s.toCMD()
				,json
				);
	}
	/**
	 * compiles all the commands needed to get a json text and then returns said component;
	 * @param p
	 * @param c
	 * @param s
	 * @param pargs
	 * @param stack
	 * @param color
	 * @param context
	 * @return
	 * @throws CompileError
	 */
	public static String compileJsonTextElement(PrintStream p, Compiler c, Scope s, PrintfArgs pargs, RStack stack,String color,PrintContext context)  throws CompileError{
		//PrintfArgs pargs=(PrintfArgs) args;
		List<String> jsonargs=new ArrayList<String>();
		int index=1;
		//if set to true, vars will be passed by reference and printf will display what they show after all ops have been performed
		boolean REFARGS=false;
		final String COLOR = "color";
		//CompileJob.compileMcfLog.printf("printf compile begun;\n");
		//boolean toallocate = true;this is inside the context
		for(Token t:pargs.targs) {
			if(t instanceof Selector.SelectorToken) {
				jsonargs.add(((Selector.SelectorToken) t).selector().getJsonText());
			}else {
				Equation eq=(Equation) t;
				eq.constify(c, s);
				//use ref if it is given
				if(eq.isConstable()) {
					ConstExprToken cs = eq.getConst();
					jsonargs.add(cs.getJsonText());
				}
				else if((!context.varCopy) && eq.isRefable() && !eq.retype.isLogical()) {
					//DO NOT take a const-only ref as later args may edit it;
					//UNLESS if this is a formatLit, then copied data could be edited later so instead use direct refs
					Variable ref=eq.getVarRef();
					jsonargs.add(ref.getJsonText());
				}else {
					context.ensureAllocate(p, c, s);
					PrintContext subcontext = new PrintContext(context,index);
					eq.bindPrintContext(subcontext);
					eq.compileOps(p, c, s, null);
					if(eq.didBFMakeJsonText()) {
						//for format(...) statements
						//TODO test this;
						String json = eq.getGeneratedJsonText(p, c, s);
						jsonargs.add(json);
					} else {
						//NbtPath anonvn=new NbtPath("\"$printf\".\"$%d\"".formatted(index));
						//Variable anon=new Variable("anon",eq.retype,null,c).maskStorageAllocatable(c.resourcelocation, anonvn);
						Variable anon=context.getPrintVar(index, c, s, eq.retype);
						//anon vars must think they are being loaded
						//if(anon.willAllocateOnLoad(false))anon.allocateLoad(p, false);//this is redundant and inefficient
						eq.setVar(p, c, s, anon);
						if(eq.retype.isLogical() && !eq.retype.isNumeric()) {
							convertBoolToStr(p,anon,s,stack);
						}
						jsonargs.add(anon.getJsonText());
					}
					
				}
				
			}
			index++;
		}
		String thecolor = color==null?null:"\"%s\"".formatted(color);
		String clickEvent="";
		if(pargs.runFunc!=null) {
			//call a function as a click event
			FuncCallToken ft = pargs.runFunc;
			ResourceLocation mcf = ft.getMyMCF();
			clickEvent = ", \"clickEvent\": {\"action\": \"run_command\", \"value\": \"/function %s\"}".formatted(mcf.toString());
			//skip the args
		}
		String formats="";
		if(!pargs.fkwargs.isEmpty()) {
			StringBuffer buff = new StringBuffer();
			for(Entry<String, Token> e:pargs.fkwargs.entrySet()) {
				String key = e.getKey();
				Token t = e.getValue();
				Equation eq=(Equation) t;
				eq.constify(c, s);
				//use ref if it is given
				if(!eq.isConstable()) throw new CompileError("failed to get a const value for format argument %s=...".formatted(key));
				ConstExprToken ce = eq.getConst();
				if(key.equals(COLOR)) thecolor = ce.getJsonArg();
				else buff.append(" , \"%s\": %s".formatted(key,ce.getJsonArg()));
				//System.err.printf("kwarg: %s\n", key);
			}
			formats = buff.toString();
			//System.err.printf("formats: %s\n", formats);
		}
		
		
		CharSequence[] subtags = new CharSequence[jsonargs.size()];jsonargs.toArray(subtags);
		String argstr=String.join(" , ", jsonargs);//
		String setcolor = thecolor==null?"" :", \"color\": %s".formatted(thecolor);
		return String.format( "{\"translate\": %s, \"with\": [%s]%s%s%s}"
				, pargs.lit
				, argstr
				,setcolor,
				clickEvent,
				formats);
	}
	public static void convertBoolToStr(PrintStream p,Variable var,Scope s,RStack stack) throws CompileError {
		//p.printf("execute unless %s run data modify %s set value \"true\"\n", var.matchesPhrase("0"),var.dataPhrase());
		//p.printf("execute unless %s run data modify %s set value \"false\"\n", var.matchesPhrase("\"true\""),var.dataPhrase());
		//return;
		int h=stack.reserve(1);
		var.getMe(p, s, stack, h, VarType.BOOL);
		Register r=stack.getRegister(h);
		p.printf("execute if score %s matches 1.. run data modify %s set value \"true\"\n", r.inCMD(),var.dataPhrase());
		p.printf("execute unless score %s matches 1.. run data modify %s set value \"false\"\n", r.inCMD(),var.dataPhrase());
		
	}
	//String format comes as a literal
	public void printf(PrintStream p, String format,IPrintable... args) throws CompileError {
		this.printf(p,Selector.AT_S, format, args);
	}
	public void printf(PrintStream p, Selector subject, String format,IPrintable... args) throws CompileError {
		this.printf(p,"", subject, format, args);
	}
	public void printf(PrintStream p, String prefix,Selector subject, String format,IPrintable... args) throws CompileError {
		
		String argstr=String.join(" , ",   List.of(args).stream().map(var ->var.getJsonTextSafe()).toList());//
		p.printf("%stellraw %s {\"translate\": %s, \"with\": [%s], \"color\": \"%s\"}\n"
				,prefix
				,subject.playerify()
				,format
				, argstr
				,this.color
				);
	}

	public void printStatementTree(PrintStream p,BFCallToken token,int tabs) {
		//for debuging
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		PrintF.PrintfArgs args = (PrintfArgs) token.getArgs();
		p.printf("%s%s( %s\n",s.toString(), this.name,args.lit);
		for(Token t:args.targs) {
			if(t instanceof TreePrintable) ((TreePrintable) t).printStatementTree(p, tabs+1);
			else  p.printf("%s\t%s\n",s.toString(), t.asString());
		}
		p.printf("%s\t)\n",s.toString(), this.name);
	}
	
	/**
	 * similar to printf but makes a /title command instead (with a fade in/out);
	 * can add durations as if they were format args;
	 * note that action bar is affected differently by the fade parameters
	 * @author RadiumE13
	 *
	 */
	public static class TitleF extends PrintF {
		public static final PrintF titlef = new TitleF("titlef",TextColors.WHITE,"title",5,30,20);
		public static final PrintF subtitlef = new TitleF("subtitlef",TextColors.WHITE,"subtitle",5,30,20);
		public static final PrintF actionbarf = new TitleF("actionbarf",TextColors.WHITE,"actionbar",5,30,20);
		public final String location;
		public final int fadeInDefault;
		public final int stayDefault;
		public final int fadeOutDefault;
		public static final String FADE_IN = "fadeIn";
		public static final String STAY = "stay";
		public static final String FADE_OUT = "fadeOut";
		public TitleF(String name, String color,String location,int fadeIn,int stay,int fadeOut) {
			super(name, color);
			this.location=location;
			this.fadeInDefault = fadeIn;
			this.stayDefault = stay;
			this.fadeOutDefault = fadeOut;
		}
		@Override
		public void call(PrintStream p, Compiler c, Scope s, Args args, RStack stack,String prefix) throws CompileError {
			PrintfArgs pargs=(PrintfArgs) args;
			Num fin =  (Num) Equation.constifyAndGet(p, (Equation) pargs.fkwargs.remove(FADE_IN), c, s, stack, ConstType.NUM);
			Num sty =  (Num) Equation.constifyAndGet(p, (Equation) pargs.fkwargs.remove(STAY), c, s, stack, ConstType.NUM);
			Num fot =  (Num) Equation.constifyAndGet(p, (Equation) pargs.fkwargs.remove(FADE_OUT), c, s, stack, ConstType.NUM);
			int fadeIn = fin!=null?fin.value.intValue():this.fadeInDefault;
			int stay = sty!=null?sty.value.intValue():this.stayDefault;
			int fadeOut = fot!=null?fot.value.intValue():this.fadeOutDefault;
			PrintContext context = new PrintContext("titlef",true);
			String json = compileJsonTextElement(p, c, s, pargs, stack, this.color,context);
			p.printf("%stitle %s %s %s\n"
					,prefix
					,pargs.s.toCMD()
					,this.location
					,json
					);
			p.printf("%stitle %s times %s %s %s\n"
					,prefix
					,pargs.s.toCMD()
					,fadeIn,stay,fadeOut
					);
		}
		@Override
		public void printf(PrintStream p, String prefix,Selector subject, String format,IPrintable... args) throws CompileError {
			//this is for internal use by the compiler; this one should probably not be used
			String argstr=String.join(" , ",   List.of(args).stream().map(var ->var.getJsonTextSafe()).toList());//
			p.printf("%stellraw %s {\"translate\": %s, \"with\": [%s], \"color\": \"%s\"}\n"
					,prefix
					,subject.playerify()
					,format
					, argstr
					,this.color
					);
		}
		
	}
}
