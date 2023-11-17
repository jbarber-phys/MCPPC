package net.mcppc.compiler.tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.*;
import net.mcppc.compiler.errors.CompileError;

/**
 * represents the args given as a template to a function call;
 * @author RadiumE13
 *
 */
/*
 * TODO templates are broken; TODO allow this to stay unidentified for longer
 */
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
			int vstart = c.cursor;
			Const.ConstExprToken t=Const.checkForExpressionAny(c,s, matcher, c.line(),c.column());
			if(t==null) {
				c.cursor=vstart;
				//System.err.printf("othertemplate %s\n", c.getNextChars());
				Token bn = c.nextNonNullMatch(Factories.checkForBasicName);
				if(!(bn instanceof BasicName)) throw new CompileError("failed to parse template arg on line %s, col %s".formatted(c.line(),c.column()));
				template.otherTemplateVars.put(template.values.size(), ((BasicName) bn).name);
				//then let a null be added
			}
			template.values.add(t);
		}
		return template;
	}
	private final List<ConstExprToken> values = new ArrayList<ConstExprToken>();
	
	private Map<Integer,String> otherTemplateVars = new HashMap<Integer,String> ();//new
	public TemplateArgsToken(int line, int col) throws CompileError {
		super(line, col);
		if (Token.AbstractBracket.ARE_TYPEARGS_PARENS)throw new CompileError("cannot use templates if '<' = '('");
	}
	public TemplateArgsToken(int line, int col,List<ConstExprToken> args) throws CompileError {
		this(line, col);
		this.values.addAll(args);
	}

	@Override
	public String asString() {
		return "<...>";
	}
	public boolean dependsOnOtherTemplate() {
		return !this.otherTemplateVars.isEmpty();
	}
	public String inresPath() {
		if (!this.dependsOnOtherTemplate());//template def instances cannot depend on other templates
		//this is not stoping it
		//String[] args = (String[]) this.values.stream().map(
		//		ce->ce.resSuffix()).toArray();
		String[] args = new String[this.values.size()];
		//System.err.printf("inres: %s map: %s; %s", this.values,this.otherTemplateVars,!this.dependsOnOtherTemplate());
		for(int i=0;i<args.length;i++) args[i]=this.values.get(i).resSuffix();
		//System.err.printf("template size: %s\n",this.values.size());
		//System.err.printf("templateBound: %s\n",String.join("_", args));
		
		return "template_%s".formatted(String.join("_", args));
	}
	public boolean add(ConstExprToken add) {
		return this.values.add(add);
	}
	public boolean addOtherTemplate(String add) {
		this.otherTemplateVars.put(this.values.size(), add);
		return this.values.add(null);
	}
	public int size() {
		return this.values.size();
	}
	public void prependOther(TemplateArgsToken tgs) {
		int sz = tgs.size();
		this.values.addAll(0, tgs.values);
		Map<Integer,String> otvs = new HashMap<Integer,String> ();
		for(Entry<Integer,String> e:this.otherTemplateVars.entrySet()) {
			otvs.put(e.getKey()+sz, e.getValue());
		}
		otvs.putAll(tgs.otherTemplateVars);
		this.otherTemplateVars= otvs;
		//TODO 
	}
	public VarType typeOrPrecision(VarType base,int index) {
		Token t = this.values.get(index);
		if(t instanceof Type) {
			return ((Type) t).type;
		}else if (t instanceof Num) {
			int p = ((Num) t).value.intValue();
			try {
				return base.withPrecision(p);
			} catch (CompileError e) {
				e.printStackTrace();
				return null;
			}
		}else if (this.otherTemplateVars.containsKey(index)) {
			try {
				return base.withTemplatePrecision(this.otherTemplateVars.get(index));
			} catch (CompileError e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	/**
	 * gets an element without checking the scope for other templates to fill it in;
	 * this should only be used by {@link TemplateDefToken}
	 * @return
	 */
	public ConstExprToken getAssumingComplete(int index) {
		return this.values.get(index);
	}
	/**
	 * sets an argment and overrides 
	 * @param index
	 * @param e
	 * @return
	 */
	public ConstExprToken setArg(int index,ConstExprToken e) {
		return this.values.set(index, e);
	}
	public boolean rebind(Compiler c,Scope s) throws CompileError{
		if(this.otherTemplateVars.isEmpty()) return false;
		for(Entry<Integer,String> e: this.otherTemplateVars.entrySet()) {
			Const cv = s.checkForTemplateOrLocalConst(e.getValue());
			if(cv==null)  throw new CompileError("template arg %s faild to bind while compiling".formatted(e.getValue()));
			ConstExprToken ce = cv.getValue();
			this.values.set(e.getKey(), ce);
		}
		return true;
	}
}
