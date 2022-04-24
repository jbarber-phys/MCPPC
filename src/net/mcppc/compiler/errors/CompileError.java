package net.mcppc.compiler.errors;

import net.mcppc.compiler.tokens.*;

import java.util.List;

import net.mcppc.compiler.*;

public class CompileError extends Exception {
	public final String error;
	@Override public String getMessage() {return this.error;}
	//string format codes:
	//https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Formatter.html#syntax
	public CompileError(String string) {
		this.error=string;
	}
	//can overide to auto-format output
	
	public static class UnexpectedToken extends CompileError{
		public UnexpectedToken(Token t) {
			super("Unexpected token \'%s\' at line %d col %d.".formatted(t.asString(),t.line,t.col));
		}public UnexpectedToken(Token t,String expectedA) {
			super("Unexpected token \'%s\' at line %d col %d; Expected a %s.".formatted(t.asString(),t.line,t.col,expectedA));
		}public UnexpectedToken(Token t,String expectedA,String reason) {
			super("Unexpected token \'%s\' at line %d col %d; Expected a %s; %s.".formatted(t.asString(),t.line,t.col,expectedA,reason));
		}
		
	}
	public static class UnexpectedTokens extends CompileError{
		public UnexpectedTokens(List<Token> ts) {
			super("unexpected tokens [%s];".formatted(String.join(" , ", Token.asStrings(ts))));
		}public UnexpectedTokens(List<Token> ts,String expected) {
			super("unexpected tokens [%s]; expected %s".formatted(String.join(" , ", Token.asStrings(ts)),expected));
		}
		
	}
	public static class NoSuchType extends CompileError{
		public NoSuchType(Token t) {
			super("No such type \'%s\' at line %d col %d.".formatted(t.asString(),t.line,t.col));
		}
	}
	public static class UnexpectedFileEnd extends CompileError{
		public UnexpectedFileEnd(int line) {
			super("Unexpected end of file at line %d.".formatted(line));
		}
		
	}
	public static class DoubleDeclaration extends CompileError{
		public DoubleDeclaration(Declaration d) {
			super(d.isFunction()?
					"Function '%s(...)' was declared a second time at line %d col %d.".formatted(d.getFunction().name,d.line,d.col)
					:
					"Variable '%s' was declared a second time at line %d col %d.".formatted(d.getVariable().name,d.line,d.col)
					);
		}
		public DoubleDeclaration(Import i) {
			super("Import alias '%s' was used twice %d col %d.".formatted(i.getAlias(),i.line,i.col)
					);
		}
		
	}
	public static class UnsupportedOperation extends CompileError{
		public UnsupportedOperation(VarType v1,Token op, VarType v2) {
			super("Unsupported operation: %s %s %s (line %d col %d);".formatted(v1.asString(),op.asString(),v2.asString(),op.line,op.col));
		}
		public UnsupportedOperation(VarType v1,BiOperator.OpType op, VarType v2) {
			super("Unsupported operation: %s %s %s ;".formatted(v1.asString(),op.s,v2.asString()));
		}
		public UnsupportedOperation(UnaryOp.UOType op, VarType v) {
			super("Unsupported operation: %s %s ;".formatted(op.s,v.asString()));
		}
		public UnsupportedOperation(Token op, VarType v2) {
			super("Unsupported unary operation:  %s %s (line %d col %d);".formatted(op.asString(),v2.asString(),op.line,op.col));
		}
		
	}
	public static class UnsupportedCast extends CompileError{
		public UnsupportedCast(VarType from, VarType to) {
			super("Unsupported cast from %s to %s;".formatted(from,to));
		}
		public UnsupportedCast(VarType from, VarType to, String in) {
			super("Unsupported implied cast from %s to %s in %s;".formatted(from,to,in));
		}
		
	}
	public static class CannotSet extends CompileError{
		public CannotSet(VarType set, String in) {
			super("Cannot set a %s to a literal %s;".formatted(set.asString(),in));
		}
		
	}
	public static class VarNotFound extends CompileError{
		public VarNotFound(String var) {
			super("Could not find Variable %s;".formatted(var));
		}
		public VarNotFound(Struct s,String field) {
			super("Could not find Field  %s.%s;".formatted(s.name,field));
		}
		public VarNotFound(Struct s,int index) {
			super("Could not find index member  %s[%s];".formatted(s.name,index));
		}
		
	}
	public static class BFuncNotFound extends CompileError{
		public BFuncNotFound(String var) {
			super("Could not find BuiltinFunction %s;".formatted(var));
		}
		public BFuncNotFound(Struct s,String f) {
			super("Could not find Method  %s.%s;".formatted(s.name,f));
		}
		public BFuncNotFound(Struct s,String f,boolean statc) {
			super("Could not find Method  %s%s%s;".formatted(s.name,statc?"::":".",f));
		}
		
	}
	
}
