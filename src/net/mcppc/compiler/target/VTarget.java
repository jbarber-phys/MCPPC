package net.mcppc.compiler.target;

import java.util.regex.Matcher;

import net.mcpp.util.Ordered;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;

/**
 * represents a contiguous range of versions;
 * if either bound is null than that side is unbounded;
 * 
 * 
 * @author RadiumE13
 *
 */
public class VTarget {
	public static final VTarget ANY = new VTarget(Version.JAVA_NEFINITY,Version.JAVA_INFINITY);
	public static final VTarget NONE = new VTarget(Version.JAVA_INFINITY,Version.JAVA_NEFINITY);//inverted oreder
	public static VTarget after(Version v) {
		return new VTarget(v,Version.JAVA_INFINITY);
	}
	public static VTarget before(Version v) {
		return new VTarget(Version.JAVA_NEFINITY,v);
	}public static VTarget at(Version v) {
		return new VTarget(v,v);
	}
	public static VTarget fromCmdArgument(String arg) {
		Matcher m = Regexes.UINT.matcher(arg);
		if(m.matches()) {
			int i=Integer.parseInt(m.group());
			return new Version(i).justMe();
		} 
		m.usePattern(Regexes.UINT_RANGE);
		if(m.matches()) {
			String smin =  m.group("min");
			Version min = smin==null ? Version.JAVA_NEFINITY : new Version(Integer.parseInt(smin));
			String smax =  m.group("max");
			Version max = smax==null ? Version.JAVA_NEFINITY : new Version(Integer.parseInt(smax));
			return new VTarget(min,max);
		}
		else{
			System.err.printf("did not recognize version range '%s' found after -x ;\n", arg);
			return null;
		} 
		
	}
	public final Version minVersion;
	public final Version maxVersion;
	public VTarget(Version min,Version max) {
		this.minVersion = min;
		this.maxVersion = max;
	}
	public boolean isEmpty() {
		return this.maxVersion.compareTo(this.minVersion) <0;
	}
	public VTarget intersection(VTarget o) {
		return new VTarget(
				Ordered.<Version>max(this.minVersion,o.minVersion),
				Ordered.<Version>min(this.maxVersion,o.maxVersion)
				);
	}
	public VTarget convexUnion(VTarget o) {
		return new VTarget(
				Ordered.<Version>min(this.minVersion,o.minVersion),
				Ordered.<Version>max(this.maxVersion,o.maxVersion)
				);
	}
	public boolean isSubsetOf(VTarget o) {
		return this.minVersion.isGreaterOrEqualTo(o.minVersion)
				&&this.maxVersion.isLessOrEqualTo(o.maxVersion) ;
	}
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof VTarget)) return false;
		if(this.isEmpty() && ((VTarget) obj).isEmpty()) return true;
		return this.minVersion.equals(((VTarget) obj).minVersion) && this.maxVersion.equals(((VTarget) obj).maxVersion);
	}
	@Override
	public String toString() {
		if(this.isEmpty()) return "[no versions]";
		String v1=this.minVersion.toString();
		String v2=this.maxVersion.toString();
		if(this.minVersion.equals(Version.JAVA_NEFINITY))v1="";
		if(this.maxVersion.equals(Version.JAVA_INFINITY))v2="";
		return "[ versions %s..%s ]".formatted(v1,v2);
	}
	/**
	 * checks that the target matches a supported range;
	 * if there is no coverage of target, throw an error;
	 * if there is partial coverage of target, issue a warning instead;
	 * do not call this during tokenization as compilation of that token could be contingent on a narrower target;
	 * @param supported
	 * @param target
	 * @param use
	 * @param c
	 * @throws CompileError
	 */
	public static void requireTarget(VTarget supported,VTarget target,String use,Compiler c) throws CompileError {
		if(target.isSubsetOf(supported)) {
			return;//all good
		}else if (target.intersection(supported).isEmpty()) {
			throw new CompileError("%s only supports %s, which confilcts with compile target %s".formatted(use,supported,target));
		}else {
			c.warnAboutMisallignedTarget(supported, target, use);
		}
	}
	public static class TargetToken extends Token{
		public static final Token.Factory factory = new Token.Factory(Regexes.UINT_RANGE) {
			@Override
			public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new TargetToken(line,col,matcher);
			}
		};
		public TargetToken(int line, int col,Matcher m) {
			super(line, col);
			String s1=m.group("min");
			String s2=m.group("max");
			Version min = s1!=null? new Version(Integer.parseInt(s1)):Version.JAVA_NEFINITY;
			Version max = s2!=null? new Version(Integer.parseInt(s2)):Version.JAVA_INFINITY;
			this.target =new VTarget(min,max);
		}
		public final VTarget target;
		@Override
		public String asString() {
			return null;
		}
		
	}
}