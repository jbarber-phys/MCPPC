package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.regex.Matcher;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.*;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.AbstractCallToken;
import net.mcppc.compiler.tokens.Equation.End;
import net.mcppc.compiler.tokens.Statement.Assignment;
/**
 * token representing an indexed member of a variable: var[index];
 * @author RadiumE13
 *
 */
public class VariableElementToken extends Token{
	public static final Token make(Compiler c,Scope s, Matcher m,Token name, RStack stack,int line,int col, boolean isTopLevel) throws CompileError {
		Equation index=new Equation(line, col, stack);
		index.isAnArg=true;
		index.isTopLevel=false;
		index=index.populate(c, s, m);
		if(index.end!=End.INDEXBRACE)throw new CompileError("unexpected arg list ended with a %s.".formatted(index.end.name()));
		return new VariableElementToken(name.line,name.col,name,index,stack).convertVar(c,c.currentScope);
		
	}
	//Membername | VariableElementToken | callToken with a return value
	final Token var;
	final Equation indexeq;
	final RStack stack;
	public VariableElementToken(int line, int col,Token var,Equation index,RStack stack) {
		super(line, col);
		this.var=var;
		this.indexeq=index;
		this.stack=stack;
	}

	@Override
	public String asString() {
		return "%s[%s]".formatted(var.asString(),indexeq.asString());
	}
	public Token convertVar(Compiler c,Scope s) throws CompileError {
		//convert to a subvar
		if(this.var instanceof MemberName && this.indexeq.isNumber()) {
			Variable v = ((MemberName) this.var).getVar();
			Number n=Num.getNumber(indexeq, VarType.INT);
			if(n!=null) {
				int i=n.intValue();
				if(v.isStruct() && v.type.struct.canIndexMe(v, i)) {
					Variable ell = v.type.struct.getIndexRef(v, i);
					MemberName memn = ell.memberName(s, var, var.asString()+ "[%d]".formatted(i));
					return memn;
				}
			}
		}
		return this;
		
	}
	public Token convertGet(Compiler c,Scope s) throws CompileError {
		//convert to a func call
		if(this.indexeq.isNumber()) {
			Number n=Num.getNumber(indexeq, VarType.INT);
			if(n!=null) {
				//this case should be unreachable
				throw new CompileError ("reached unrachable");
			}
		}else if(this.var instanceof MemberName) {
			Variable v = ((MemberName) this.var).getVar();
			if(v.isStruct()) {
				Token get = v.type.struct.convertIndexGet(v, this.indexeq);
				if(get instanceof BFCallToken && ((BFCallToken) get).canConvert()) get = ((BFCallToken) get).convert(c, s, this.stack);
				return get;
			}
		} throw new CompileError ("cannot get indexed element of %s".formatted(this.var));
		
	}
	public Statement convertSet(Compiler c,Scope s,Equation to) throws CompileError {
		//convert to a func call
		if(this.indexeq.isNumber()) {
			Number n=Num.getNumber(indexeq, VarType.INT);
			if(n!=null) {
				//this case should be unreachable
				throw new CompileError ("reached unrachable");
			}
		}else if(this.var instanceof MemberName) {
			Variable v = ((MemberName) this.var).getVar();
			if(v.isStruct()) {
				Token set = v.type.struct.convertIndexSet(v, this.indexeq, to);
				if(set instanceof BFCallToken && ((BFCallToken) set).canConvert()) set = ((BFCallToken) set).convert(c, s, this.stack);
				
				return new Statement.CallStatement(line, col,c.cursor, (AbstractCallToken) set, c);
			}
		} throw new CompileError ("cannot assign index of %s".formatted(this.var));
		
	}

}
