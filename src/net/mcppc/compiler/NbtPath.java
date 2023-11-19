package net.mcppc.compiler;

import java.util.regex.Matcher;

import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
@Targeted
public class NbtPath {
	public static NbtPath UUID = new NbtPath("UUID");
	public static NbtPath UUID_LAST = new NbtPath("UUID[3]");
	
	public static NbtPath POS = new NbtPath("Pos");
	public static NbtPath MOTION = new NbtPath("Motion");
	public static NbtPath ROTATION = new NbtPath("Rotation");
	

	public static NbtPath YAW = new NbtPath("Rotation[0]");
	public static NbtPath PITCH = new NbtPath("Rotation[1]");
	public static NbtPath ROT1 = YAW;
	public static NbtPath ROT2 = PITCH;
	public static class NbtPathToken extends Const.ConstLiteralToken{
		//for a named thing that hasn't been identified yet
		public static final Token.Factory factory = new Token.Factory(Regexes.NBTPATH) {
			@Override
			public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new NbtPathToken(line,col,matcher);
			}
		};
		
		NbtPath nbt;public NbtPath path() {return this.nbt;}
		public NbtPathToken(int line, int col,Matcher m) {
			super(line, col,ConstType.NBT);
			this.nbt=new NbtPath(m);
		}
		public NbtPathToken(int line, int col,String s) {
			super(line, col,ConstType.NBT);
			this.nbt=new NbtPath(s);
		}
		@Override public String asString() {
			return this.nbt.path;
		}
		@Override
		public String textInHdr() {
			// never need to worry about double braces here; const defs do not open blocks
			return this.path().path;
		}
		@Override
		public String textInMcf(VTarget tg) {
			//no need for escaping, escapes were already removed
			return this.path().path;
		}
		@Override
		public String resSuffix() {
			return "nbt_%x".formatted(this.valueHash());
		}
		@Override
		public int valueHash() {
			return this.nbt.hashCode();
		}
		@Override
		public boolean canCast(VarType type) {
			return false;
		}
		@Override
		public ConstExprToken constCast(VarType type) throws CompileError {
			throw new CompileError.UnsupportedCast( this.constType(),type);
		}
		@Override
		public String getJsonText() throws CompileError {
			throw new CompileError("cannot print an %s".formatted(this.constType().name));
		}
	}

	@Targeted String path;
	public NbtPath(String s) {
		path=s.trim();
	}
	public NbtPath(Matcher m) {
		path=m.group().trim();
	}
	@Override public String toString() {return this.path;}
	@Override public int hashCode() {return this.path.hashCode();}

	public static NbtPathToken nextNbtCarefull (Compiler c, Matcher matcher) throws CompileError {
		return nextNbtCarefull(c,matcher,false);
	}
	public static NbtPathToken nextNbtCarefull (Compiler c, Matcher matcher,boolean doubleBraces) throws CompileError {
		StringBuffer buff=new StringBuffer();
		
		@SuppressWarnings("unused") Token wc=c.nextNonNullMatch(Factories.skipSpace);
		int braces=0;
		int line=c.line();
		int col=c.column();
		while (true) {
			matcher.region(c.cursor, matcher.regionEnd());
			if(c.cursor>=matcher.regionEnd())throw new CompileError.UnexpectedFileEnd(c.line());
			if (matcher.usePattern(Regexes.NBT_OPEN)
			.lookingAt()) {
				buff.append(matcher.group());
				braces++;
				c.cursor=matcher.end();
				if(doubleBraces) {
					matcher.region(c.cursor, matcher.regionEnd());
					if(!matcher.usePattern(Regexes.CODEBLOCKBRACE).lookingAt()
							||
							matcher.group(1)==null)throw new CompileError("expected a double (escpaed) brace {{ but got a single one");
					c.cursor=matcher.end();
				}
				continue;
			}else if (matcher.usePattern(Regexes.NBT_CLOSE)
			.lookingAt()) {
				if(braces<=0)throw new CompileError("unexpected close brace '}' in nbttag on line %d;".formatted(c.line()));
				buff.append(matcher.group());
				braces--;
				c.cursor=matcher.end();
				if(doubleBraces) {
					matcher.region(c.cursor, matcher.regionEnd());
					if(!matcher.usePattern(Regexes.CODEBLOCKBRACE).lookingAt()
							||
							matcher.group(2)==null)throw new CompileError("expected a double (escpaed) brace {{ but got a single one");
					c.cursor=matcher.end();
				}
				continue;
			}
			else if (matcher.usePattern(Regexes.NBT_THROUGH)
			.lookingAt()) {
				buff.append(matcher.group());
				c.cursor=matcher.end();
				continue;
			}else {
				//end
				if(braces!=0) throw new CompileError("unexpected brace count (extra opens %d) in nbttag on line %d;".formatted(braces,c.line()));
				return new NbtPathToken(line,col,buff.toString());
			}
		}
	}

}
