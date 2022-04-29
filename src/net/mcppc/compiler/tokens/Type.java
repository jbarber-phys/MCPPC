package net.mcppc.compiler.tokens;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Struct;
import net.mcppc.compiler.StructTypeParams;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.CompileError.NoSuchType;
import net.mcppc.compiler.errors.CompileError.UnexpectedToken;
import net.mcppc.compiler.tokens.Token.BasicName;
import net.mcppc.compiler.tokens.Token.Factory;
import net.mcppc.compiler.tokens.*;

public class Type extends Const.ConstLiteralToken {
	public static final boolean ALLOW_NEGATIVE_PRECISION=true;
	//has no factory
	public final VarType type;
	public Type(int line, int col,VarType type) {
		super(line, col,ConstType.TYPE);
		this.type=type;
	}

	@Override
	public String asString(){
		return type.asString();
	}
	public static Type tokenizeType(Compiler c, Matcher matcher, int line, int col,String name, List<Const> forbidden) throws CompileError{
		VarType.Builtin tt=VarType.getBuiltinType(name);
		//if (tt==null) throw new CompileError.NoSuchType(name);
		VarType vt;
		if (tt.isStruct) {
			Struct s=Struct.getStruct(name);
			StructTypeParams args=s.tokenizeTypeArgs(c, matcher, line, col,forbidden);
			vt = new VarType(s,args);
			
		}
		else if(tt.isFloatP) {
			//Token menos = c.nextNonNullMatch(Factories.checkForMinus);
			//Token p = c.nextNonNullMatch(Factories.nextNum);
			Const.ConstExprToken p = Num.tokenizeNextNumNonNull(c, matcher, line, col);
			if ((!(p instanceof Num)) || ((Num)p).type.isFloatP()) throw new CompileError.UnexpectedToken(p,"integer");
			int precision = ((Num)p).value.intValue();
			//if(menos instanceof UnaryOp) precision*=-1;
			if(precision<0 && !Type.ALLOW_NEGATIVE_PRECISION)throw new CompileError("negative precision used; to permit this, set Type.ALLOW_NEGATIVE_PRECISION to true;");
			vt=new VarType(tt,precision);
			Token close=c.nextNonNullMatch(AbstractBracket.checkForTypeargBracket);
			if ((!(AbstractBracket.isArgTypeArg(close))) || ((Token.AbstractBracket)close).forward) throw new CompileError.UnexpectedToken(close,"')'");
		}else {
			vt=new VarType(tt);
			Token close=c.nextNonNullMatch(AbstractBracket.checkForTypeargBracket);
			if ((!(AbstractBracket.isArgTypeArg(close))) || ((Token.AbstractBracket)close).forward) throw new CompileError.UnexpectedToken(close,"')'");
		}
		return new Type(line,col,vt);
		
	}
	public static void closeTypeArgs(Compiler c, Matcher matcher, int line, int col) throws CompileError{
		Token close=c.nextNonNullMatch(AbstractBracket.checkForTypeargBracket);
		if ((!(AbstractBracket.isArgTypeArg(close))) || ((Token.AbstractBracket)close).forward) throw new CompileError.UnexpectedToken(close,"')'");

	}
	public static Type tokenizeTypeNoArgs(Compiler c, Matcher matcher, int line, int col,String name) throws CompileError{
		VarType.Builtin tt=VarType.getBuiltinType(name);
		VarType v;
		if (tt.isStruct) {
			Struct s=Struct.getStruct(name);
			StructTypeParams params=s.paramsWNoArgs();
			v=new VarType(s,params);
		}else {
			v=new VarType(tt);
		}
		return new Type(line,col,v);
	}
	public static Type tokenizeNextVarType(Compiler c, Matcher matcher, int line, int col) throws CompileError  {
		//return (Type)Const.checkForExpression(c, matcher, line, col, Const.ConstType.TYPE);
		List<Const> forbidden = new ArrayList<Const>();
		return Type.tokenizeNextVarType(c, matcher, line, col,forbidden);
	}
	public static Type tokenizeNextVarType(Compiler c, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError  {
		//final Token.Factory[] look = {Token.BasicName.factory,Factories.space,Factories.newline,Factories.comment};
		final Token.Factory[] look = Factories.checkForBasicName;
		int start=c.cursor;
		Token t=c.nextNonNullMatch(look);
		if (!(t instanceof BasicName)) throw new UnexpectedToken(t,"name");
		if(!VarType.isType(((BasicName)t).name)) {
			//not a var name but check to see if it is a constant
			//TODO check libs for class members
			c.cursor=start;
			Const cv=Const.identifyConst(c, matcher, line, col, forbidden, ConstType.TYPE);
			return (Type) cv.getValue();
		}
		//int aftertypename=c.cursor;
		Token t2 = c.nextNonNullMatch(Token.AbstractBracket.checkForTypeargBracket);
		Type type;
		//CompileJob.compileMcfLog.printf("t2=%s;\n", t2.asString());
		if (t2 instanceof Token.AbstractBracket && Token.AbstractBracket.isArgTypeArg((AbstractBracket) t2)) {
			//CompileJob.compileMcfLog.printf("tokenizeType\n");
			if (!((Token.AbstractBracket) t2).forward) {
				//error
				throw new CompileError.UnexpectedToken(t2, AbstractBracket.TYPEOPEN);
				//c.cursor=aftertypename;//rewind
				//type=Type.tokenizeTypeNoArgs(c, matcher, line, col, (BasicName) t);
			}else type=Type.tokenizeType(c, matcher, line, col, ((BasicName) t).name,forbidden);
			//leave cursor alone
		}
		else {
			type=Type.tokenizeTypeNoArgs(c, matcher, line, col, ((BasicName) t).name);
			//DONT rewind
		}
		return type;
	}
	public static Type tokenizeNextVarTypeNullable(Compiler c, Matcher matcher, int line, int col, List<Const> forbidden)  {
		try {
			Type t=Type.tokenizeNextVarType(c, matcher, line, col,forbidden);
			return t;
		} catch (CompileError e) {
			return null;
		}
		
	}
	//Depricated
	@Deprecated static final Token.Factory depfactory = new Factory(Regexes.NAME) {
		@Override
		public Token createToken(Compiler c, Matcher matcher, int line, int col) {
			c.cursor=matcher.end();
			return null;
		}
		
	};
	@Override
	public String textInHdr() {
		return this.asString();
	}
	
}
