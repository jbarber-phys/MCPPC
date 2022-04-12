package net.mcppc.compiler;

import java.util.regex.Matcher;

import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;

public class NbtPath {

	public static class NbtPathToken extends Token{
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
			super(line, col);
			this.nbt=new NbtPath(m);
		}
		public NbtPathToken(int line, int col,String s) {
			super(line, col);
			this.nbt=new NbtPath(s);
		}
		@Override public String asString() {
			return this.nbt.path;
		}
	}
	String path;
	public NbtPath(String s) {
		path=s.trim();
	}
	public NbtPath(Matcher m) {
		path=m.group().trim();
	}
	@Override public String toString() {return this.path;}
	
	public static NbtPathToken nextNbtCarefull (Compiler c, Matcher matcher) throws CompileError {
		StringBuffer buff=new StringBuffer();
		
		@SuppressWarnings("unused") Token wc=c.nextNonNullMatch(Factories.skipSpace);
		int braces=0;
		int line=c.line;
		int col=c.column();
		while (true) {
			matcher.region(c.cursor, matcher.regionEnd());
			if(c.cursor>=matcher.regionEnd())throw new CompileError.UnexpectedFileEnd(c.line);
			if (matcher.usePattern(Regexes.NBT_OPEN)
			.lookingAt()) {
				buff.append(matcher.group());
				braces++;
				c.cursor=matcher.end();
				continue;
			}else if (matcher.usePattern(Regexes.NBT_CLOSE)
			.lookingAt()) {
				if(braces<=0)throw new CompileError("unexpected close brace '}' in nbttag on line %d;".formatted(c.line));
				buff.append(matcher.group());
				braces--;
				c.cursor=matcher.end();
				continue;
			}
			else if (matcher.usePattern(Regexes.NBT_THROUGH)
			.lookingAt()) {
				buff.append(matcher.group());
				c.cursor=matcher.end();
				continue;
			}else {
				//end
				if(braces!=0) new CompileError("unexpected brace count (extra opens %d) in nbttag on line %d;".formatted(braces,c.line));
				return new NbtPathToken(line,col,buff.toString());
			}
		}
	}

}
