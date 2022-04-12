package net.mcppc.compiler.tokens;

import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Struct;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.CompileError.NoSuchType;
import net.mcppc.compiler.errors.CompileError.UnexpectedToken;
import net.mcppc.compiler.tokens.Token.BasicName;
import net.mcppc.compiler.tokens.Token.Factory;

public class Type extends Token {
	//Cast is depricated
	private class Cast extends Type{
		public Cast(int line, int col, VarType type) {
			super(line, col, type);
		}
	}
	public final VarType type;
	public Type(int line, int col,VarType type) {
		super(line, col);
		this.type=type;
	}
	private Cast asCast(int line,int col) {
		return new Cast(line,col,this.type);
	}

	@Override
	public String asString(){
		return type.asString();
	}
	public static Type tokenizeType(Compiler c, Matcher matcher, int line, int col,Token.BasicName name) throws CompileError{
		VarType.Builtin tt=VarType.getBuiltinType(name.name);
		if (tt==null) throw new CompileError.NoSuchType(name);
		VarType vt;
		if (tt.isStruct) {
			Struct s=Struct.STRUCTS.get(name.name);
			VarType.StructTypeParams args=s.tokenizeTypeArgs(c, matcher, line, col);
			vt = new VarType(s,args);
			
		}
		else if(tt.isFloatP) {
			Token p = c.nextNonNullMatch(Factories.nextNum);
			if ((!(p instanceof Token.Num)) || ((Token.Num)p).type.isFloatP()) throw new CompileError.UnexpectedToken(p,"integer");
			int precision = ((Token.Num)p).value.intValue();
			vt=new VarType(tt,precision);
			Token close=c.nextNonNullMatch(Factories.closeParen);
			if ((!(close instanceof Token.Paren)) || ((Token.Paren)close).forward) throw new CompileError.UnexpectedToken(close,"')'");
		}else {
			vt=new VarType(tt);
			Token close=c.nextNonNullMatch(Factories.closeParen);
			if ((!(close instanceof Token.Paren)) || ((Token.Paren)close).forward) throw new CompileError.UnexpectedToken(close,"')'");
		}
		return new Type(line,col,vt);
		
	}
	public static Type tokenizeTypeNoArgs(Compiler c, Matcher matcher, int line, int col,Token.BasicName name) throws CompileError{
		VarType.Builtin tt=VarType.getBuiltinType(name.name);
		VarType v;
		if (tt.isStruct) {
			Struct s=Struct.STRUCTS.get(name.name);
			VarType.StructTypeParams params=s.paramsWNoArgs();
			v=new VarType(s,params);
		}else {
			v=new VarType(tt);
		}
		return new Type(line,col,v);
	}
	public static Type tokenizeNextVarType(Compiler c, Matcher matcher, int line, int col) throws CompileError  {
		final Token.Factory[] look = {Token.BasicName.factory,Factories.space,Factories.newline,Factories.comment,Token.Paren.factory};
		Token t=c.nextNonNullMatch(look);
		if (!(t instanceof BasicName)) throw new UnexpectedToken(t,"name");
		//TODO let Type tokenize itself if true
		int aftertypename=c.cursor;
		Token t2 = c.nextNonNullMatch(look);
		Type type;
		if (t2 instanceof Token.Paren) {
			if (!((Token.Paren) t2).forward)throw new UnexpectedToken(t2);
			type=Type.tokenizeType(c, matcher, line, col, (BasicName) t);
			//leave cursor alone
		}
		else {
			type=Type.tokenizeTypeNoArgs(c, matcher, line, col, (BasicName) t);
			c.cursor=aftertypename;//rewind
		}
		return type;
	}
	//Depricated
	static final Token.Factory depfactory = new Factory(Regexes.NAME) {
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) {
			c.cursor=matcher.end();
			return null;
		}
		
	};
	
}
