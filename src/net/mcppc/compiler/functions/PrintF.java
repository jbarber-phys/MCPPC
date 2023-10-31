package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
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
	
	
	public static void registerAll() {
		BuiltinFunction.register(stdout);
		BuiltinFunction.register(stderr);
		BuiltinFunction.register(stdwarn);
	}
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
	public void getRet(PrintStream p, Compiler c, Scope s,  BFCallToken token, RStack stack, int stackstart) throws CompileError{
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
		public PrintfArgs(Selector s,String lit) {
			this.s=s;
			this.lit=lit;
		}
		
	}
	public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line,int col, RStack stack)throws CompileError {

		Selector reciver=Selector.AT_S;
		ConstExprToken t=Const.checkForExpressionSafe(c,s, matcher, line, col, ConstType.SELECTOR,ConstType.STRLIT);
				//c.nextNonNullMatch(testForSelector);
		
		if(t!=null &&t instanceof Selector.SelectorToken) {
			reciver=((Selector.SelectorToken) t).selector();
			if(!BuiltinFunction.findArgsep(c))new CompileError("not enough args in printf(...)");
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
		reciver=reciver.playerify();
		String litFstring=((StringToken) t).literal();
		//CompileJob.compileMcfLog.printf("printf: %s, %s;\n",s,litFstring);
		PrintfArgs args=new PrintfArgs(reciver,litFstring);
		boolean moreArgs=BuiltinFunction.findArgsep(c);
		while(moreArgs) {
			//t=c.nextNonNullMatch(testForSelector);
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
		return args;
		
	}
	public static boolean ESCAPE_TAG_IN_JSON = true;//TODO test in mc to see what should be done

	@Override
	public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		this.call(p, c, s, token.getArgs(), stack, "");
	}
	public void call(PrintStream p, Compiler c, Scope s, Args args, RStack stack,String prefix) throws CompileError {
		PrintfArgs pargs=(PrintfArgs) args;
		List<String> jsonargs=new ArrayList<String>();
		int index=1;
		//if set to true, vars will be passed by reference and printf will display what they show after all ops have been performed
		boolean REFARGS=false;
		//CompileJob.compileMcfLog.printf("printf compile begun;\n");
		for(Token t:pargs.targs) {
			if(t instanceof Selector.SelectorToken) {
				jsonargs.add(((Selector.SelectorToken) t).selector().getJsonText());
			}else {
				Equation eq=(Equation) t;
				//use ref if it is given
				if(REFARGS && eq.isRefable() && !eq.retype.isLogical()) {
					//DO NOT take a const-only ref as later args may edit it;
					Variable ref=eq.getVarRef();
					jsonargs.add(ref.getJsonText());
				}else {
					eq.compileOps(p, c, s, null);
					NbtPath anonvn=new NbtPath("\"$printf\".\"$%d\"".formatted(index));
					Variable anon=new Variable("anon",eq.retype,null,c).maskStorageAllocatable(c.resourcelocation, anonvn);
					//anon vars must think they are being loaded
					if(anon.willAllocateOnLoad(false))anon.allocateLoad(p, false);
					eq.setVar(p, c, s, anon);
					if(eq.retype.isLogical()) {
						convertBoolToStr(p,anon,s,stack);
					}
					jsonargs.add(anon.getJsonText());
				}
				
			}
			index++;
		}
		
		
		CharSequence[] subtags = new CharSequence[jsonargs.size()];jsonargs.toArray(subtags);
		String argstr=String.join(" , ", jsonargs);//
		p.printf("%stellraw %s {\"translate\": %s, \"with\": [%s], \"color\": \"%s\"}\n"
				,prefix
				,pargs.s.toCMD()
				,pargs.lit
				, argstr
				,this.color
				);
	}
	public void convertBoolToStr(PrintStream p,Variable var,Scope s,RStack stack) throws CompileError {
		//p.printf("execute unless %s run data modify %s set value \"true\"\n", var.matchesPhrase("0"),var.dataPhrase());
		//p.printf("execute unless %s run data modify %s set value \"false\"\n", var.matchesPhrase("\"true\""),var.dataPhrase());
		//return;
		int h=stack.reserve(1);
		var.getMe(p, s, stack, h);
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
		//TODO java.util.concurrent.CompletionException: java.lang.IllegalArgumentException: 
				//Whilst parsing command on line 23: Only players may be affected by this command,
				// but the provided selector includes entities at position 0: <--[HERE]
		//playerify subject
		String argstr=String.join(" , ",   List.of(args).stream().map(var ->var.getJsonTextSafe()).toList());//
		p.printf("%stellraw %s {\"translate\": %s, \"with\": [%s], \"color\": \"%s\"}\n"
				,prefix
				,subject
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
}
