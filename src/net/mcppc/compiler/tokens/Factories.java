package net.mcppc.compiler.tokens;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Function;
import net.mcppc.compiler.NbtPath;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Function.FuncCallToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Equation.End;
import net.mcppc.compiler.tokens.Statement.CallStatement;
import net.mcppc.compiler.tokens.Token.Assignlike;
import net.mcppc.compiler.tokens.Token.BasicName;
import net.mcppc.compiler.tokens.Token.Factory;
import net.mcppc.compiler.tokens.Token.Assignlike.Kind;
/**
 * lots of token factories, including for statement start;
 * @author RadiumE13
 *
 */
public final class Factories {
	
	public static final Token.Factory headerName = new Factory(Regexes.NAME) {
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			Keyword w=Keyword.fromString(matcher.group());
			if (w!=null )switch(w) {
			case VOLATILE: case SYNCHRONIZED: case RECURSIVE:
				throw new CompileError("Keyword %s must be later in the statement (after the access or thread keywords)".formatted(w.name()));
				//TODO in future could intercept these and add them to thread / declare statements
			case PUBLIC:return Declaration.header(c, matcher, line, col, w,c.isHeaderOnly());
			case PRIVATE:return Declaration.fromSrc1(c, matcher, line, col, w);
			case EXTERN:return Declaration.fromSrc1(c, matcher, line, col, w);
			case IMPORT:return Import.header(c, matcher, line, col,c.isHeaderOnly());
			//flow must change scope
			case IF:
				return IfElse.skipMe(c, matcher, line, col, w);
			case ELIF:
				return IfElse.skipMe(c, matcher, line, col, w);
			case ELSE:
				return IfElse.skipMe(c, matcher, line, col, w);
			case TARGET:
				return IfTarget.skipMe(c, matcher, line, col, false);
			case SWITCH,CASE,DEFAULT:
				return Switch.skipMe(c, matcher, line, col, w);
			case FOR:
				return ForStm.skipMe(c, matcher, line, col, w);
			case WHILE:
				return While.skipMe(c, matcher, line, col, w);
			case EXECUTE:
				return Execute.skipMe(c, matcher, line, col, w);
			case THREAD:
			case NEXT:
				return ThreadStm.skipMe(c, matcher, line, col, w);
			case TAG,TICK,LOAD:
				return TagStatement.skipMe(c, matcher, line, col, w);
			default:{
				//don't include in header
			}
			}
			//builtin function, ignore
			c.cursor=matcher.end();
			Token t=c.nextNonNullMatch(Factories.headerSkipline);
			c.cursor=matcher.end();
			return null;
		}
		
	};
	public static final Token.Factory srcStartName = new Factory(Regexes.NAME) {
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			Keyword w=Keyword.fromString(matcher.group());
			boolean isGoto = false;
			boolean isThreadvar = false;
			boolean isThis = false;
			//CompileJob.compileMcfLog.printf("keyword: %s;\n", w);
			if (w!=null )switch(w) {
			//from declarations, only extract estimantes / assignments / code blocks
			case PRIVATE:
				return Declaration.compileToken(c, matcher, line, col, w);
			case PUBLIC:
				return Declaration.compileToken(c, matcher, line, col, w);
			case EXTERN:
				return Declaration.compileToken(c, matcher, line, col, w);
				
			case IMPORT:
				return Import.fromSrc(c, matcher, line, col);
				
			case IF:
				return IfElse.makeMe(c, matcher, line, col, w);
			case ELIF:
				return IfElse.makeMe(c, matcher, line, col, w);
			case ELSE:
				return IfElse.makeMe(c, matcher, line, col, w);
			case TARGET: 
				return IfTarget.makeMe(c, matcher, line, col, false);
			case SWITCH,CASE,DEFAULT:
				return Switch.makeMe(c, matcher, line, col, w);
			case FOR:
				return ForStm.makeMe(c, matcher, line, col, w);
			case WHILE:
				return While.makeMe(c, matcher, line, col, w);
			case EXECUTE:
				return Execute.makeMe(c, matcher, line, col, w);
			case BREAK:
				return Statement.Assignment.makeBreak(c, matcher, line, col);
			case RETURN:
				return Statement.Assignment.makeReturn(c, matcher, line, col);
			case THIS:
				isThis=true;
				break;//treat it as a normal var name and it will work
				//return Statement.Assignment.makeThis(c, matcher, line, col);

			case CONST:
				throw new CompileError("keyword %s not expected at start of statement".formatted(w.name));
			case THREAD:
			case NEXT:
				return ThreadStm.makeMe(c, matcher, line, col, w);
			case START: case STOP: case RESTART:{
				return ThreadCall.make(c, matcher, line, col, w,false, true);
			}
			case KILL: {
				//builtin func: kill(target)
				//thread func: kill thread(target)
				break;
			}
			case GOTO:{
				isThreadvar = true;
				isGoto=true;//normal var under the hood, but only accept block names
			}break;
			case WAIT: case EXIT: {
				isThreadvar=true;
			}break;
			case TAG,TICK,LOAD:
				return TagStatement.makeMe(c, matcher, line, col, w);
			default:
				break;

			}else {
				//its a normal name
			}
			//its a normal name
			MemberName nm=(MemberName) MemberName.factory.createToken(c, matcher, line, col);
			
			TemplateArgsToken tempargs = TemplateArgsToken.checkForTemplateAfterName(c, c.currentScope, matcher, nm, false);
			Token par=c.nextNonNullMatch(checkForParenOrIndexBracket);
			if (par instanceof Token.Paren && ((Token.Paren) par).forward) {
				//function call
				//constructor has its own hook
				//CompileJob.compileMcfLog.printf("call to: %s; builtins are %s;\n", nm.names,BuiltinFunction.BUILTIN_FUNCTIONS.keySet());
				BuiltinFunction bf = BuiltinFunction.getBuiltinFunc(nm.names, c, c.currentScope);
				if(bf!=null) {
					//a builtin function
					//CompileJob.compileMcfLog.printf("builtin funct: %s\n", nm.names);
					//static and nonstatic members
					RStack stack=c.currentScope.getStackFor();
					BuiltinFunction.BFCallToken sub=BuiltinFunction.BFCallToken.make(c, c.currentScope, matcher, nm.line,nm.col, stack, bf);
					if(tempargs!=null)sub.withTemplate(tempargs);
					if(bf.isNonstaticMember()) {
						List<String> nms=((MemberName) nm).names;nms=nms.subList(0, nms.size()-1);
						Variable self = c.myInterface.identifyVariable(nms, c.currentScope);
						sub.withThis(self);
					}if(sub.canConvert()) {
						Token ft=sub.convert(c, c.currentScope, stack);
						if(!(ft instanceof Function.FuncCallToken)) {
							throw new CompileError("function converted to non-call token in statement start context; not yet supported;");
							//TODO if this is allowed: (... + ...).doSomething(); is done
						}else {
							((FuncCallToken) ft).linkMeByForce(c, c.currentScope);
							Token end=c.nextNonNullMatch(Factories.nextIsLineEnd);
							if(!(end instanceof Token.LineEnd))new CompileError.UnexpectedToken(end, ";","code block after builtin func not yet supported");
							return new Statement.CallStatement(line,col,c.cursor,((FuncCallToken) ft),c);
							
						}
					}
					Token end=c.nextNonNullMatch(Factories.nextIsLineEnd);
					if(!(end instanceof Token.LineEnd))new CompileError.UnexpectedToken(end, ";","code block after builtin func not yet supported");
					return new Statement.CallStatement(line, col,c.cursor, sub,c);

				}else {
					//a normal function
					Function.FuncCallToken ft=Function.FuncCallToken.make(c, c.currentScope, nm.line, nm.col, matcher, nm, new RStack(c.resourcelocation,c.currentScope));
					ft.identify(c,c.currentScope);
					if(tempargs!=null)ft.withTemplate(tempargs);
					ft.linkMe(c, c.currentScope);
					Token end=c.nextNonNullMatch(Factories.nextIsLineEnd);
					if(!(end instanceof Token.LineEnd))new CompileError.UnexpectedToken(end, ";","code block after builtin func not yet supported");
					return new Statement.CallStatement(line,col,c.cursor,ft,c);
				}
				
			}
			else {
				nm.identify(c,c.currentScope);
				//Token sbk = c.nextNonNullMatch(checkForIndexBrace);
				Token sbk = par;
				//System.err.printf("sbk = %s\n",sbk.asString());
				Token v=nm;
				boolean indexed=false;
				while (sbk instanceof Token.IndexBrace && ((Token.IndexBrace) sbk).forward) {
					//if(!(v instanceof Token.MemberName) )throw new CompileError("%s is not a variable and so cannot be indexed []".formatted(v.asString()));
					Token vet=VariableElementToken.make(c, c.currentScope,  matcher, v, new RStack(c.resourcelocation,c.currentScope), v.line,v.col, true);
					v=vet;
					sbk=c.nextNonNullMatch(checkForIndexBrace);
					//System.err.printf("sbk = %s\n",sbk.asString());
					if(indexed && vet instanceof VariableElementToken) throw new CompileError("double var-index on assignment not supported");
					indexed=true;
				}
				Token asn = c.nextNonNullMatch(Factories.checkForAssignlike);
				//? ->
				if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.MASK) {
					throw new CompileError.UnexpectedToken(asn, "non-mask assignlike", "cannot mask var after declaration");
				}
				
				//? ~~
				if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ESTIMATE) {
					//ignore index
					
					/*
					Token est=c.nextNonNullMatch(Factories.checkForNullableNumber);//TODO deprecate this
					if(!(est instanceof Num)) {
						est=Num.tokenizeNextNumNonNull(c,c.currentScope, matcher, line, col);
					}
					if(!(est instanceof Num)) {
						throw new CompileError.UnexpectedToken(est, "number");
					}
					Number estval = ((Num) est).value;
					*/
					ConstExprToken est = Const.checkForExpression(c, c.currentScope, matcher, line, col, ConstType.NUM,ConstType.NULL);
					Number estval;
					if(est instanceof NullToken) {
						estval=null;
					}else if (est instanceof Num) {
						estval = ((Num) est).value;
					}else {
						throw new CompileError.UnexpectedToken(est, "number or null");
					}
					asn = c.nextNonNullMatch(Factories.nextIsLineEnd);
					if(!(asn instanceof Token.LineEnd))new CompileError.UnexpectedToken(asn, ";");
					Statement.Estimate stm= new Statement.Estimate(line, col,c.cursor, nm.var,estval);
					c.currentScope.addEstimate(stm.var, stm.estimate);
					return stm;
				}
				//? =
				if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ASSIGNMENT) {
					Equation eq=isGoto? Equation.toAssignGoto(line, col, c, matcher)
							:Equation.toAssign(line, col, c, c.currentScope, matcher);
					//equation finds the semicolon;
					if(eq.end !=End.STMTEND) throw new CompileError("assignment ended with a non-';'");
					if(indexed) {
						if (v instanceof VariableElementToken)return ((VariableElementToken) v).convertSet(c, c.currentScope, eq);
						else nm = (MemberName) v;
					}
					//if(isThreadvar)nm.var = nm.var.attemptSelfify(c.currentScope);//fileInterface did this already
					return new Statement.Assignment(line, col,c.cursor, nm.var, eq);
				}else
					throw new CompileError.UnexpectedToken(asn, "'=' or '~~' or '('");
			}
		}
		
	};

	public static final Token.Factory basicNameKeyword = new Factory(Regexes.NAME) {
		
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			Keyword w=Keyword.fromString(matcher.group());
			if(w==null) {
				c.cursor=matcher.start();//dont pass
				return new Token.WildChar(line, col, matcher.group());
			}else {
				c.cursor=matcher.end();//pass
				return new Token.BasicName(line, col, matcher.group());
			}
		}
		
	};
	public static class LazySkip extends Token.Factory{
		boolean endLn=false;
		String s;
		public LazySkip(Pattern pattern,String look,boolean endStatement) {
			this(pattern,look);
			this.endLn=endStatement;
		}
		public LazySkip(Pattern pattern,String look) {
			super(pattern);
			s=look;
		}
		@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) {
			c.cursor= matcher.end();
			return endLn?new Token.LineEnd(line, col):null;
		}
	}
	static class LazyNewLine extends Token.Factory{
		public LazyNewLine(Pattern pattern) {
			super(pattern);
		}
		@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) {
			c.newLine(c.cursor= matcher.end());
			return null;
		}
	}
	static class MultiNewLine extends Token.Factory{
		public MultiNewLine(Pattern pattern) {
			super(pattern);
		}
		@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) {
			String match = matcher.group();
			int nnl = 0;
			int index=0;
			int nindex=0;
			int pindex = 0;
			while((nindex = match.indexOf("\n", index)) > pindex) {
				//System.err.printf("newlines: %d -> %d\n", index , nindex);
				index=nindex+1;
				pindex = nindex;
				nnl++;
			}
			if(nnl>0) {
				c.newLines(matcher.start()+index, nnl);
			}
			c.cursor= matcher.end();
			return null;
		}
	}
	public static final Token.Factory space = new LazySkip(Regexes.SPACE," ");
	private static final Token.Factory comment = new LazySkip(Regexes.COMMENT,"//...");
	private static final Token.Factory skipDomment = new LazySkip(Regexes.DOMMENT,"///...");
	private static final Token.Factory domment = Statement.Domment.factory;
	public static final Token.Factory skiplineEnd = new LazySkip(Regexes.SKIPLINE_END,"<...skipped line>",true);
	public static final Token.Factory skiplineMid = new LazySkip(Regexes.SKIPLINE_MID,"<..skip line>");
	public static final Token.Factory newline = new LazyNewLine(Regexes.SPACE_NEWLINE);
	
	public static final Token.Factory commentBlock = new MultiNewLine(Regexes.COMMENT_BLOCK);
	public static final Token.Factory dommentBlock = Statement.Domment.factory_block;
	public static final Token.Factory dommentSkipBlock = new MultiNewLine(Regexes.DOMMENT_BLOCK);
	
	//these are compile-1 only and used to be newline for some reason
	private static final Token.Factory skipDoubleBlockBrace = new LazySkip(Regexes.CODEBLOCKBRACEDOUBLED,"{{|}}");
	private static final Token.Factory skipMscChar = new LazySkip(Regexes.STM_SKIP_MSCCHAR,"[.]");
	
	
	public static final Token.Factory skipWildchar = new LazySkip(Regexes.ANY_CHAR,"[.]");
	public static final Token.Factory skipFslash = new LazySkip(Regexes.FSLASH,"<..skip line>");
	//public static final Token.Factory skipFslash = new LazyNewLine(Regexes.FSLASH);
	
	public static final Token.Factory[] headerLnStart = Factories.genericLookPriority(new Token.Factory[] {headerName},
			//Statement.Domment.factory,//redundant
			Token.CodeBlockBrace.unscopeFactory,
			skiplineEnd,skiplineMid);
	
	public static final Token.Factory[] headerSkipline = {comment,commentBlock,skipDomment,dommentSkipBlock,newline,skiplineEnd,skiplineMid
			//,domment//covered
			,space};
	
	public static final Token.Factory[] closeParen = Factories.genericLook(Statement.Domment.factory,Token.Paren.factory);
	public static final Token.Factory[] checkForParen = Factories.genericCheck(Token.Paren.factory);
	public static final Token.Factory[] checkForParenOrIndexBracket = Factories.genericCheck(Token.Paren.factory,Token.IndexBrace.factory);
	public static final Token.Factory[] checkForMinus =Factories.genericLook(UnaryOp.uminusfactory,Token.WildChar.dontPassFactory);
	public static final Token.Factory[] checkForKeyword = Factories.genericCheck(Factories.basicNameKeyword);
		//{newline,comment,domment,space,Statement.Domment.factory,Factories.basicNameKeyword,Token.WildChar.dontPassFactory};
	public static final Token.Factory[] checkForMembName = Factories.genericCheck(MemberName.factory);
	public static final Token.Factory[] checkForBasicName = Factories.genericCheck(Token.BasicName.factory);
	public static final Token.Factory[] checkForRangesep = Factories.genericCheck(Token.RangeSep.factory);
	public static final Token.Factory[] checkForAssignlike = Factories.genericCheck(Token.Assignlike.factoryAssign,Token.Assignlike.factoryEstimate);
	public static final Token.Factory[] checkForIndexBrace = Factories.genericCheck(Token.IndexBrace.factory);
	public static final Token.Factory[] checkForAssignlikeOrMemb = Factories.genericCheck(Token.Assignlike.factoryAssign,Token.Assignlike.factoryEstimate,Token.Member.factory);
	public static final Token.Factory[] checkForAssignlikeOrOparen = Factories.genericCheck(Token.Assignlike.factoryAssign,Token.Assignlike.factoryEstimate,Token.Paren.factory);

	//public static final Token.Factory[] checkForAssignlike = {newline,comment,space,Statement.Domment.factory,Token.Assignlike.factoryAssign,Token.Assignlike.factoryEstimate,Token.WildChar.dontPassFactory};
	public static final Token.Factory[] nextNum = Factories.genericLook(Num.factory);//{newline,comment,domment,space,Num.factory};
	//public static final Token.Factory[] checkForNullableNumber = Factories.genericCheck(Num.factory,Num.nullfactory);//{newline,comment,domment,space,Num.factory,Num.nullfactory,Token.WildChar.dontPassFactory};	
	//returns a nonnull WildChar after space; does this without moving past the wildchar
	public static final Token.Factory[] skipSpace =Factories.genericLook(Token.WildChar.dontPassFactory); 
	
	static final Token.Factory[] nextIsLineEnd =Factories.genericLook(
			//Token.BasicName.factory,
			Token.LineEnd.factory,Token.CodeBlockBrace.factory
			);

	public static final Token.Factory[] compileLnStart = Factories.genericLookPriority(new Token.Factory[] {srcStartName},
			Token.LineEnd.factory,Token.CodeBlockBrace.unscopeFactory,Statement.CommandStatement.factory
			);
	//should be complete for now
	
	public static final Token.Factory[] argsepOrParen = Factories.genericLook(Token.ArgEnd.factory,Token.Paren.factory); 
	public static final Token.Factory[] argsepOrTypeBracket = Factories.genericLook(Token.ArgEnd.factory,Token.TypeArgBracket.factory);
	
	
	private static final int GNLSZ = 6;
	public static Token.Factory[] genericLook(Token.Factory f){
		return new Token.Factory[]{newline,comment,commentBlock,domment,dommentBlock,space,f};
		
	}
	public static Token.Factory[] genericLook(Token.Factory... f){
		Token.Factory[] looks= new Token.Factory[GNLSZ+f.length];
		looks[0]=newline;
		looks[1]=space;
		looks[2]=domment;
		looks[3]=dommentBlock;
		looks[4]=comment;
		looks[5]=commentBlock;
		for(int i=0;i<f.length;i++)looks[GNLSZ+i]=f[i];
		return looks;
		
	}
	public static Token.Factory[] genericLookPriority(Token.Factory[] overrides,Token.Factory... f){
		int ol = overrides.length;
		Token.Factory[] looks= new Token.Factory[GNLSZ+f.length + overrides.length];
		for(int i=0;i<ol;i++)looks[i]=overrides[i];
		looks[0+ol]=newline;
		looks[1+ol]=space;
		looks[2+ol]=domment;
		looks[3+ol]=dommentBlock;
		looks[4+ol]=comment;
		looks[5+ol]=commentBlock;
		for(int i=0;i<f.length;i++)looks[GNLSZ+ol+i]=f[i];
		return looks;
	}
	
	
	public static Token.Factory[] genericCheck(Token.Factory f){
		return new Token.Factory[]{newline,comment,commentBlock,domment,dommentBlock,space,f,Token.WildChar.dontPassFactory};
	}

	public static Token.Factory[] genericCheck(Token.Factory... f){
		Token.Factory[] looks= new Token.Factory[GNLSZ+1+f.length];
		looks[0]=newline;
		looks[1]=space;
		looks[2]=domment;
		looks[3]=comment;
		looks[4]=dommentBlock;
		looks[5]=commentBlock;
		for(int i=0;i<f.length;i++)looks[GNLSZ+i]=f[i];
		looks[GNLSZ+f.length]=Token.WildChar.dontPassFactory;
		return looks;
	}
	private static final Token.Factory[] carefullSkip = {newline,comment,commentBlock,domment,dommentBlock,space
		,CommandToken.factorySafeSkip
		,Token.StringToken.factory
		,Selector.SelectorToken.factory
		,Token.LineEnd.factory
		,Factories.skipDoubleBlockBrace
		,Token.CodeBlockBrace.factory//start tag canidate
		,Token.BasicName.factory
		,Num.factory
		,Factories.skipMscChar
		,Factories.skipFslash
	};
	/**
	 * carefully skips over a statement returning the end term (';' or '{').
	 * 
	 * any nbt tags in a statement that uses this (not a declaration or tag declaration) must double up braces to escape them.
	 * @param c
	 * @param matcher
	 * @param line
	 * @param col
	 * @return
	 * @throws CompileError
	 */
	public static Token carefullSkipStm(Compiler c, Matcher matcher, int line, int col)throws CompileError{
		//System.err.println("carefullSkip");
		
		while(true) {
			Token t=c.nextNonNullMatch(carefullSkip);
			if(t instanceof Token.LineEnd)return t;
			else if (t instanceof Token.CodeBlockBrace) {
				if(!((Token.CodeBlockBrace)t).forward)throw new CompileError.UnexpectedToken(t, "';', or '{'");
				return t;
			}
			//System.err.println("skipped %s".formatted(t.asString()));
		}
		
	}
}
