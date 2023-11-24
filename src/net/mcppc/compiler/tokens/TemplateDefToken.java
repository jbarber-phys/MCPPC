package net.mcppc.compiler.tokens;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Const.*;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Num.Numrange;
import net.mcppc.compiler.tokens.Token.Assignlike.Kind;

/**
 * represents a token that defines the template of a function, similar to funnction args but constant types;
 * each term gives a default value or range for which templates to generate; the export suffix in a function definition can then add more if needed;
 * @author RadiumE13
 *
 */
public class TemplateDefToken extends Token{
	public static TemplateDefToken checkForDef(Compiler c,Scope s, Matcher matcher) throws CompileError {
		Token open=c.nextNonNullMatch(AbstractBracket.checkForTypeargBracket);
		if(!(open instanceof Token.TypeArgBracket) || !((TypeArgBracket) open).forward)return null;
		TemplateDefToken template= new TemplateDefToken(open.line, open.col);
		boolean first=true;
		while(true) {
			Token close=c.nextNonNullMatch(AbstractBracket.checkForTypeargSep);
			if((close instanceof Token.TypeArgBracket) && !((TypeArgBracket) close).forward)break;
			if(close instanceof Token.ArgEnd && first)throw new CompileError.UnexpectedToken(close, "'>' or const-expr");
			first=false;
			Token typename=c.nextNonNullMatch(Factories.checkForBasicName);if(!(typename instanceof Token.BasicName))throw new CompileError.UnexpectedToken(typename, "basic name");
			ConstType type=ConstType.get(((BasicName) typename).name);
			if(type==null)throw new CompileError.UnexpectedToken(typename, "const-type name");
			Token name=c.nextNonNullMatch(Factories.checkForBasicName);
			if(!(name instanceof Token.BasicName))throw new CompileError.UnexpectedToken(name, "basic name");
			
			Const.ConstExprToken t2;
			Token asn = c.nextNonNullMatch(Factories.checkForAssignlike);
			int rangeat=-1;
			if(asn instanceof Token.Assignlike) {
				if(((Assignlike) asn).k!=Kind.ASSIGNMENT) throw new CompileError.UnexpectedToken(asn, "=");
				t2=Const.checkForExpressionAny(c,s, matcher, c.line(),c.column());
				if(t2==null)throw new CompileError("failed to parse template arg on line %s, col %s".formatted(c.line(),c.column()));
				
				if(t2 instanceof Num) {
					Token t3 = c.nextNonNullMatch(Factories.checkForRangesep);
					if(t3 instanceof Token.RangeSep) {
						template.rangeat=template.params.size();
						t3=Const.checkForExpression(c, s, matcher, c.line(), c.column(), ConstType.NUM);
						if(t3 instanceof Num)
							t2 = new Num.Numrange((Num)t2, (Num)t3);
						else throw new CompileError("non number after .. range sep"); 
					}
				}
				
			}else t2=null;
			if(t2==null)template.full=false;
			Const cv=new Const(((BasicName) name).name, c.resourcelocation,type, Keyword.PRIVATE, null);
			cv.isTemplate=true;
			template.params.add(cv);
			template.defaultvalues.add(t2);
		}
		return template;
	}
	public final List<Const> params = new ArrayList<Const>();
	public final List<ConstExprToken> defaultvalues = new ArrayList<ConstExprToken>();
	private int rangeat=-1;
	private boolean full=true;
	public TemplateDefToken(int line, int col) throws CompileError {
		super(line, col);
		if (Token.AbstractBracket.ARE_TYPEARGS_PARENS)throw new CompileError("cannot use templates if '<' = '('");
	}

	public TemplateDefToken bind(TemplateArgsToken args) throws CompileError {
		assert !args.dependsOnOtherTemplate();
		if(args==null)throw new CompileError("function requires a template of form %s".formatted(this.inHDR()));
		TemplateDefToken tp=new TemplateDefToken(args.line,args.col);
		if(args.size()>this.params.size()) throw new CompileError.UnexpectedToken(args, "smaller template", "too many template args");
		for(int i=0;i<this.params.size();i++) {
			Const old = this.params.get(i);
			ConstExprToken value =args.size()>i? args.getAssumingComplete(i):old.getValue();
			if(value==null) throw new CompileError("Template param %s left unbound;".formatted(old.name));
			Const c = new Const(old.name,old.path,old.ctype,old.access,value);
			tp.params.add(c);
			tp.defaultvalues.add(value);
			//System.err.printf("bind size %s\n",tp.params.size());
		}
		return tp;
	}
	public List<TemplateArgsToken> getAllDefaultArgs() throws CompileError {
		List<TemplateArgsToken> binds=new ArrayList<TemplateArgsToken>();
		if(!this.full)return binds;
		if(this.rangeat<0) {
			binds.add(this.defaultArgs());
			return binds;
		}else {
			Num.Numrange range = (Numrange) this.defaultvalues.get(rangeat);
			//System.err.printf("loop start\n");
			for(Num n:range.getAll()) {
				//System.err.printf("int loop\n");
				TemplateArgsToken a= this.defaultArgs();
				a.setArg(rangeat, n);
				binds.add(a);
			}
		}
		return binds;
	}

	@Override
	public String asString() {
		return "<...=...>";
	}
	public String inHDR() {
		String[] argss=new String[this.params.size()];
		for(int i=0;i<argss.length;i++) {
			Const c=this.params.get(i);
			argss[i]=c.getValue()==null?
					"%s %s".formatted(c.ctype.name,c.name):
					"%s %s = %s".formatted(c.ctype.name,c.name,c.getValue().textInHdr());
		}
		return "<%s>".formatted(String.join(" , ", argss));
	}
	public TemplateArgsToken defaultArgs() throws CompileError {
		TemplateArgsToken args = new TemplateArgsToken(this.line, this.col,this.defaultvalues);
		//this.params.forEach(cv -> args.values.add(cv.getValue()));
		//args.values.addAll(this.defaultvalues);
		//for(ConstExprToken t:args.values)if(t==null)throw new CompileError("template args on line %s col %s not binded in time".formatted(this.line,this.col));
		for(ConstExprToken t:this.defaultvalues)if(t==null)throw new CompileError("template args on line %s col %s not binded in time".formatted(this.line,this.col));
		return args;
	}
}
