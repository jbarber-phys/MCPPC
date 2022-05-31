package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Token.BasicName;
import net.mcppc.compiler.tokens.Token.Factory;

public class MemberName extends Token implements Identifiable{
	//for a named thing that hasn't been identified yet
	public static final Factory factory = new Factory(Regexes.NAME) {
		//static final Factory[] look= {Factories.newline,Factories.comment,Factories.domment,Factories.space,
		//		Token.BasicName.factory,Token.Member.factory,Token.WildChar.dontPassFactory};
		
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			final Factory[] lookdot=Factories.genericCheck(Token.Member.factory);
			final Factory[] lookname=Factories.checkForBasicName;
			c.cursor=matcher.end();
			MemberName me= new MemberName(line,col,matcher.group());
			//int pc=c.cursor;//wildchar will be smart
			while(true) {
				Token t=c.nextNonNullMatch(lookdot);
				if(t instanceof Token.WildChar)break;
				else if (t instanceof Token.Member) {
					//move on
				}else throw new CompileError.UnexpectedToken(t,"'.' or non-name");
				Token t2=c.nextNonNullMatch(lookname);
				if (!(t2 instanceof BasicName))throw new CompileError.UnexpectedToken(t,"name");
				me.names.add(((BasicName)t2).name);
			}
			return me;
		}
	};
	public final List<String> names=new ArrayList<String>();
	public MemberName(int line, int col,String name) {
		super(line, col);
		this.names.add(name);
	}
	public MemberName(BasicName b) {
		this(b.line, b.col,b.name);
	}
	public MemberName addName(String name) {
		this.names.add(name);return this;
	}
	@Override public String asString() {
		return String.join(".", names);
	}
	Variable var=null;
	Number estimate=null;
	@Override
	public int identify(Compiler c,Scope s) throws CompileError {
		this.var=c.myInterface.identifyVariable(this,s);
		if(this.var!=null)this.estimate=s.getEstimate(var);
		return 0;
	}
	public boolean identifySafe(Compiler c,Scope s) {
		try {
			this.var=c.myInterface.identifyVariable(this,s);
			if(this.var!=null)this.estimate=s.getEstimate(var);
			return true;
		} catch (CompileError e) {
			return false;
		}
	}
	public int identifyWith(Variable v) {
		this.var=v;
		return 0;
	}
	public Variable getVar() {
		return this.var;
	}
}