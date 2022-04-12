package net.mcppc.compiler.tokens;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Function;
import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Equation.End;
import net.mcppc.compiler.tokens.Statement.FuncCallStatement;
import net.mcppc.compiler.tokens.Token.Assignlike;
import net.mcppc.compiler.tokens.Token.BasicName;
import net.mcppc.compiler.tokens.Token.Factory;
import net.mcppc.compiler.tokens.Token.MemberName;
import net.mcppc.compiler.tokens.Token.Assignlike.Kind;

public final class Factories {
	
	public static final Token.Factory headerName = new Factory(Regexes.NAME) {
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			Keyword w=Keyword.fromString(matcher.group());
			if (w!=null )switch(w) {
			case PUBLIC:return Declaration.fromSrc1(c, matcher, line, col, w);
			case PRIVATE:return Declaration.fromSrc1(c, matcher, line, col, w);
			case EXTERN:return Declaration.fromSrc1(c, matcher, line, col, w);
			case IMPORT:return Import.header(c, matcher, line, col,false);
			default:{
				//dont include in header
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
				//TODO
				break;
			case ELIF:
				break;
			case ELSE:
				break;
			case EXECUTE:
				break;
			case FOR:
				break;
			case WHILE:
				break;
			default:
				break;

			}else {
				//its a normal name
			}
			//its a normal name
			Token.MemberName nm=(MemberName) Token.MemberName.factory.createToken(c, matcher, line, col);
			Token par=c.nextNonNullMatch(checkForParen);
			if (par instanceof Token.Paren && ((Token.Paren) par).forward) {
				//function call
				//constructor has its own hook
				new Statement.FuncCallStatement(line, col, null,c);
				if((nm.names.size()==1) &&
						BuiltinFunction.isBuiltinFunc((nm.names.get(0)))) {
					//a builtin function
					BuiltinFunction.BFCallToken sub=BuiltinFunction.BFCallToken.make(c, matcher, nm.line, nm.col, nm.names.get(0));
					Token end=c.nextNonNullMatch(Factories.nextIsLineEnd);
					if(!(end instanceof Token.LineEnd))new CompileError.UnexpectedToken(end, ";","code block after builtin func not yet supported");
					return new Statement.BuiltinFuncCallStatement(line, col, sub);

				}else {
					//a normal function
					Function.FuncCallToken ft=Function.FuncCallToken.make(c, nm.line, nm.col, matcher, nm, new RStack(c.resourcelocation,c.currentScope));
					ft.identify(c,c.currentScope);
					Token end=c.nextNonNullMatch(Factories.nextIsLineEnd);
					if(!(end instanceof Token.LineEnd))new CompileError.UnexpectedToken(end, ";","code block after builtin func not yet supported");
					return new Statement.FuncCallStatement(line,col,ft,c);
				}
				
			}else {
				nm.identify(c,c.currentScope);
				Token asn = c.nextNonNullMatch(Factories.checkForAssignlike);
				//? ->
				if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.MASK) {
					throw new CompileError.UnexpectedToken(asn, "non-mask assignlike", "cannot mask var after declaration");
				}
				
				//? ~~
				if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ESTIMATE) {
					Token est=c.nextNonNullMatch(Factories.nextNumNullable);
					if(!(est instanceof Token.Num)) throw new CompileError.UnexpectedToken(est, "number");
					asn = c.nextNonNullMatch(Factories.nextIsLineEnd);
					if(!(asn instanceof Token.LineEnd))new CompileError.UnexpectedToken(asn, ";");
					Statement.Estimate stm= new Statement.Estimate(line, col, nm.var,((Token.Num)est).value);
					c.currentScope.addEstimate(stm.var, stm.estimate);
					return stm;
				}
				//? =
				if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ASSIGNMENT) {
					Equation eq=Equation.toAssign(line, col, c, matcher);
					//equation finds the semicolon;
					if(eq.end !=End.STMTEND) throw new CompileError("assignment ended with a non-';'");
					return new Statement.Assignment(line, col, nm.var, eq);
				}else
					throw new CompileError.UnexpectedToken(asn, "'=' or '~~' or '('");
			}
		}
		
	};
	static class LazySkip extends Token.Factory{
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
	public static final Token.Factory space = new LazySkip(Regexes.SPACE," ");
	public static final Token.Factory comment = new LazySkip(Regexes.COMMENT,"//...");
	public static final Token.Factory skipDomment = new LazySkip(Regexes.DOMMENT,"///...");
	public static final Token.Factory domment = Statement.Domment.factory;
	public static final Token.Factory skiplineEnd = new LazySkip(Regexes.SKIPLINE_END,"<...skipped line>",true);
	public static final Token.Factory skiplineMid = new LazySkip(Regexes.SKIPLINE_MID,"<..skip line>");
	public static final Token.Factory newline = new LazyNewLine(Regexes.SPACE_NEWLINE);
	
	public static final Token.Factory[] headerLnStart = {headerName,space,newline,comment,domment,Statement.Domment.factory,Token.CodeBlockBrace.unscopeFactory,skiplineEnd,skiplineMid};
	public static final Token.Factory[] headerSkipline = {comment,skipDomment,newline,skiplineEnd,skiplineMid,domment,space};
	public static final Token.Factory[] closeParen = {newline,comment,domment,space,Statement.Domment.factory,Token.Paren.factory};
	public static final Token.Factory[] checkForParen = {newline,comment,domment,space,Statement.Domment.factory,Token.Paren.factory,Token.WildChar.dontPassFactory};
	public static final Token.Factory[] checkForAssignlike = {newline,comment,space,Statement.Domment.factory,Token.Assignlike.factoryAssign,Token.Assignlike.factoryEstimate,Token.WildChar.dontPassFactory};
	public static final Token.Factory[] nextNum = {newline,comment,domment,space,Token.Num.factory};
	public static final Token.Factory[] nextNumNullable = {newline,comment,domment,space,Token.Num.factory,Token.Num.nullfactory};	
	//returns a nonnull WildChar after space; does this without moving past the wildchar
	public static final Token.Factory[] skipSpace = {newline,comment,domment,space,Token.WildChar.dontPassFactory};
	
	static final Token.Factory[] nextIsLineEnd = {Token.BasicName.factory,Factories.space,Factories.newline,Factories.comment,Factories.domment,
			Token.LineEnd.factory,Token.CodeBlockBrace.factory
	};

	public static final Token.Factory[] compileLnStart = {srcStartName,space,newline,comment, Statement.Domment.factory,
			Token.LineEnd.factory,Token.CodeBlockBrace.unscopeFactory,Statement.CommandStatement.factory};
	//should be complete for now
	//cmd
}
