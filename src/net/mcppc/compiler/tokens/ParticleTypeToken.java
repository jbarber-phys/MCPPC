package net.mcppc.compiler.tokens;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.ResourceLocation.ResourceToken;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Singleton;
import net.mcppc.compiler.struct.Struct;
/**
 * a token for a particle type along with all particle-specific args seperated by spaces;
 * @author RadiumE13
 *
 */
public class ParticleTypeToken extends Token{
	public static ParticleTypeToken make(Compiler c, Matcher matcher, int line, int col,String id) throws CompileError  {
		
		ParticleTypeToken particle =  new ParticleTypeToken(line,col,id);
		Token.Factory[] look = Factories.genericCheck(ResourceLocation.ResourceToken.factory);
		Token t = c.nextNonNullMatch(look);
		if(t instanceof ResourceToken) {
			ResourceToken block = (ResourceToken) t;
			t =  c.nextNonNullMatch(Factories.genericCheck(BlockstateToken.argFactory));
			if(t instanceof BlockstateToken) {
				((BlockstateToken) t).block= block.asString();
			}else t = new BlockstateToken((ResourceToken) block);
			particle.args.add(t);
			return particle;
			//ignore item nbt; dont use it; it has no effect;
		}
		while(true) {
			ConstExprToken ct = Const.checkForExpressionSafe(c, c.currentScope, matcher, line, col, ConstType.NUM);
			if(ct==null)
				break;
			else {
				particle.args.add(ct);
				//System.err.printf("particle type arg: %s\n", ct.textInMcf());
			}
		}
		//TODO some nums are null and end in a d, but arg list seems fine
		//bug is in textInMcf();
		//System.err.printf("particle nargs: %s\n", particle.args.size());
		//System.err.printf("\t particle inmcf: %s\n", particle.textInMcf());
		return particle;
		
	}
	public static final Token.Factory factory = new Token.Factory(Regexes.RESOURCELOCATION) { 
		@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
			c.cursor = matcher.end();
			return make(c,matcher,line,col,matcher.group());
		} };
		public static final Token.Factory factoryImpliedMc = new Token.Factory(Regexes.NAME) { 
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor = matcher.end();
				return make(c,matcher,line,col,new ResourceLocation(matcher.group()).toString());
			} };
	String id;
	List<Token> args = new ArrayList<Token>();
	public ParticleTypeToken(int line, int col,String id) {
		super(line, col);
		this.id=id;
	}

	@Override
	public String asString() {
		try {
			return this.textInMcf();
		} catch (CompileError e) {
			return this.id + " ...error";
		}
	}

	public String textInMcf() throws CompileError{
		String[] ss = new String[this.args.size()+1];
		ss[0] = this.id;
		for(int i=0;i<args.size();i++) {
			Token t = args.get(i);
			if(t instanceof Num) ss[i+1] = ((Num) t).textAsMultiplier();
			else if(t instanceof BlockstateToken) ss[i+1] = ((BlockstateToken) t).textInMcf();
			else throw new CompileError("token %s not allowed in particle type".formatted(t.asString()));
		}
		return  String.join(" ", ss);
	}
	public static  class BlockstateToken extends Token{
		public static final Token.Factory argFactory = new Token.Factory(Regexes.BLOCKSTATEARGS) { 
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor = matcher.end();
				return new BlockstateToken(line,col,matcher.group());
			} };
		public BlockstateToken(int line, int col,String args) {
			super(line, col);
			this.args=args;
		}public BlockstateToken(ResourceToken t) {
			super(t.line, t.col);
			this.args="";
			this.block = t.asString();
		}
		public final String args;
		String block = "";
		@Override
		public String asString() {
			return this.textInMcf();
		}

		public String textInMcf(){
			return  block+args;
		}
		
	}
}
