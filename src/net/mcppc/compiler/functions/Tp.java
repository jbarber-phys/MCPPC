package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.BuiltinFunction.BasicArgs;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.Coordinates.CoordToken;
import net.mcppc.compiler.*;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.Execute;
import net.mcppc.compiler.tokens.Token;

public class Tp extends BuiltinFunction{
	public static final Tp tp = new Tp("tp");
	public static final Tp teleport = tp;
	public static void registerAll() {
		BuiltinFunction.register(tp);
		BuiltinFunction.alias(tp,"teleport");
	}
	public Tp(String name) {
		super(name);
	}

	@Override
	public VarType getRetType(BFCallToken token) {
		return VarType.VOID;
	}

	@Override
	public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
		BasicArgs args=new BasicArgs();
		Token e1=null,e2=null;
		e1=Entity.checkForSelectorOrEntityToken(c, c.currentScope, matcher, line, col);
		if(e1!=null) {
			if(!BuiltinFunction.findArgsep(c)) {
				args.add("deste", e1);
				return args;
			}
			args.add("victims", e1);
			//System.err.println("e1: %s".formatted(e1.asString()));
			e2=Entity.checkForSelectorOrEntityToken(c, c.currentScope, matcher, line, col); 
			if(e2!=null) {
				args.add("deste", e2);
				//System.err.println("e2");
				if(!BuiltinFunction.findArgsep(c)) return args;
				throw new CompileError("too many args in tp (entity,entity,***BAD...;");
			} 
		}
		Coordinates.CoordToken coord = (CoordToken) Const.checkForExpressionSafe(c, c.currentScope, matcher, line, col, ConstType.COORDS);
		if(coord!=null) {
			args.add("pos", coord);
			if(!BuiltinFunction.findArgsep(c)) {
				return args;
			}
		}else  
			throw new CompileError("too many args in tp (...,anchor,***BAD...;");
		ConstExprToken facing = Const.checkForExpressionSafe(c, c.currentScope, matcher, line, col, ConstType.COORDS,ConstType.ROT);
		if(facing instanceof Rotation.RotToken) {
			args.add("rot", facing);
			if(!BuiltinFunction.findArgsep(c)) {
				return args;
			}
			throw new CompileError("too many args in tp (...,rotation,***BAD...;");
		}else if (facing instanceof Coordinates.CoordToken) {
			args.add("facingpos", facing);
			if(!BuiltinFunction.findArgsep(c)) {
				return args;
			}
			throw new CompileError("too many args in tp (...,facing pos,***BAD...;");
		}
		Token e3=Entity.checkForSelectorOrEntityToken(c, c.currentScope, matcher, line, col);
		
		if(e3!=null) {
			args.add("facingentity", e3);
			if(!BuiltinFunction.findArgsep(c)) {
				return args;
			}
		}
		Token a=Execute.Anchor.getNextToken(c, matcher, line, col);
		args.add("anchor", a);
		if(!BuiltinFunction.findArgsep(c)) {
			return args;
		}
		throw new CompileError("too many args in tp (...,anchor,***BAD...;");
	}
	@Targeted
	@Override
	public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
		BasicArgs bargs=(BasicArgs) token.getArgs();
		if(bargs.has("deste") && !bargs.has("victims")) {
			p.printf("tp %s\n", Entity.getSelectorFor(bargs.arg("deste"),true).toCMD());
			return;
		}
		if(bargs.has("deste") && bargs.has("victims")) {
			p.printf("tp %s %s\n"
					, Entity.getSelectorFor(bargs.arg("victims"),false).toCMD()
					,Entity.getSelectorFor(bargs.arg("deste"),true).toCMD());
			return;
		}
		if(!bargs.has("victims")) {
			p.printf("tp %s\n", ((Coordinates.CoordToken) bargs.arg("pos")).pos.inCMD());
			return;
		}
		Selector victims = bargs.has("victims")?
				Entity.getSelectorFor(bargs.arg("victims"),false):
				Selector.AT_S;
		if(bargs.nargs()==2) {
			p.printf("tp %s %s\n"
					, victims.toCMD()
					,((Coordinates.CoordToken) bargs.arg("pos")).pos.inCMD());
			return;
		}
		if(bargs.has("rot")) {
			p.printf("tp %s %s %s\n"
					, victims.toCMD()
					,((Coordinates.CoordToken) bargs.arg("pos")).pos.inCMD()
					,((Rotation.RotToken) bargs.arg("rot")).rot.inCMD());
			return;
		}
		if(bargs.has("facingpos")) {
			p.printf("tp %s %s facing %s\n"
					, victims.toCMD()
					,((Coordinates.CoordToken) bargs.arg("pos")).pos.inCMD()
					,((Coordinates.CoordToken) bargs.arg("facingpos")).pos.inCMD());
			return;
		}
		if(bargs.has("facingentity")) {
			p.printf("tp %s %s facing entity %s%s\n"
					, victims.toCMD()
					,((Coordinates.CoordToken) bargs.arg("pos")).pos.inCMD()
					,Entity.getSelectorFor(bargs.arg("facingentity"),true).toCMD()
					,bargs.has("anchor")? ' '+bargs.arg("anchor").asString() : ""
					);
			return;
		}
		
		
		
	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
			throws CompileError {
		
	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack) throws CompileError {
	}

	@Override
	public Number getEstimate(BFCallToken token) {
		return null;
	}

}
