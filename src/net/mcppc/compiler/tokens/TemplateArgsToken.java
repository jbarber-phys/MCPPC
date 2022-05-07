package net.mcppc.compiler.tokens;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.*;
import net.mcppc.compiler.errors.CompileError;

public class TemplateArgsToken extends Token {
	public static TemplateArgsToken checkForArgs(Compiler c,Scope s, Matcher matcher) throws CompileError {
		Token open=c.nextNonNullMatch(AbstractBracket.checkForTypeargBracket);
		if(!(open instanceof Token.TypeArgBracket) || !((TypeArgBracket) open).forward)return null;
		TemplateArgsToken template= new TemplateArgsToken(open.line, open.col);
		boolean first=true;
		while(true) {
			Token close=c.nextNonNullMatch(AbstractBracket.checkForTypeargSep);
			if((close instanceof Token.TypeArgBracket) && !((TypeArgBracket) close).forward)break;
			if(close instanceof Token.ArgEnd && first)throw new CompileError.UnexpectedToken(close, "'>' or const-expr");
			first=false;
			Const.ConstExprToken t=Const.checkForExpressionAny(c,s, matcher, c.line(),c.column());
			if(t==null)throw new CompileError("failed to parse template arg on line %s, col %s".formatted(c.line(),c.column()));
			template.values.add(t);
		}
		return template;
	}
	public final List<ConstExprToken> values = new ArrayList<ConstExprToken>();
	public TemplateArgsToken(int line, int col) throws CompileError {
		super(line, col);
		if (Token.AbstractBracket.ARE_TYPEARGS_PARENS)throw new CompileError("cannot use templates if '<' = '('");
	}

	@Override
	public String asString() {
		return "<...>";
	}

	public String inresPath() {
		//String[] args = (String[]) this.values.stream().map(
		//		ce->ce.resSuffix()).toArray();
		String[] args = new String[this.values.size()];
		for(int i=0;i<args.length;i++) args[i]=this.values.get(i).resSuffix();
		//System.err.printf("template size: %s\n",this.values.size());
		//System.err.printf("templateBound: %s\n",String.join("_", args));
		
		return "template_%s".formatted(String.join("_", args));
	}
	
}
