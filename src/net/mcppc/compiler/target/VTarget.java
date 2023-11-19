package net.mcppc.compiler.target;

import net.mcpp.util.Ordered;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.Warnings;

/**
 * represents a contiguous range of versions;
 * if either bound is null than that side is unbounded;
 * 
 * 
 * @author RadiumE13
 *
 */
public class VTarget {
	public static final VTarget ANY = new VTarget(null,null);
	public static final VTarget NONE = new VTarget(Version.JAVA_INFINITY,Version.JAVA_NEFINITY);//inverted oreder
	public static VTarget after(Version v) {
		return new VTarget(v,Version.JAVA_INFINITY);
	}
	public static VTarget before(Version v) {
		return new VTarget(Version.JAVA_NEFINITY,v);
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
		return "versions(%s..%s)".formatted(this.minVersion,this.maxVersion);
	}
	public static void requireTarget(VTarget supported,VTarget target,String use,Compiler c) throws CompileError {
		if(target.isSubsetOf(supported)) {
			return;//all good
		}else if (target.intersection(supported).isEmpty()) {
			throw new CompileError("%s only supports %s, which confilcts with compile target %s".formatted(use,supported,target));
		}else {
			Warnings.warning(use, c);
		}
	}
}