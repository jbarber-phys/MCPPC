package net.mcppc.compiler.tokens;

import java.io.PrintStream;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.Future;
//break; return; return value;
//if no return then func will return zero
/**
 * a statement that executes a true return; will try to return a number for the depth;<p>
 * this statement currently cannot work because conditional returns are impossible;<br>
 *  example: return if ... run return 4<br>
 *  this will not return from the function running the whole line;<p>
 *  this class is left here as a placeholder if conditional returns are added in the future
 * @author RadiumE13
 *
 */
/*
 * changes needed for this to work:
 * mcfunction must allow conditional returns;
 * must alter calls to subscopes to get return value, test depth and conditionally return again
 * must allow this statement to recive the Flow's that add to end of this block and inline them before the return
 */
@Future
public class TrueReturnLike extends Statement {
	private Assignment asign;
	private int breakDepth = -1;
	public TrueReturnLike(Compiler c,Assignment asn) throws CompileError{
		super(c);
		this.asign=asn;
		throw new CompileError("true returns are not possible as any conditional return is not");
	}
	public TrueReturnLike(Compiler c,Assignment asn,int breakdepth) throws CompileError {
		this(c,asn);
		this.breakDepth=breakdepth;
	}

	@Override
	public void compileMe(PrintStream p, Compiler c, Scope s) throws CompileError {
		if(!this.asign.eq.isEmpty())this.asign.compileMe(p, c, s);

		// TODO add to end of block statements put here
		int layers = 0;
		//TODO calculate how deep to return
		
		
		p.printf("return %d\n", layers);
		throw new CompileError("true returns are not possible as any conditional return is not");
	}
	
	@Override
	public String asString() {
		return this.asign.var.name;
	}

}
