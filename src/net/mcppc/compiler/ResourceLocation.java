package net.mcppc.compiler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
/**
 * location of a resource datapack:path/to/thing
 * resourcelocation is synonym for Namespaced ID
 * @author jbarb_t8a3esk
 *
 */
public class ResourceLocation {
	public static class ResourceToken extends Token {
		public static final Factory factory = new Factory(Regexes.RESOURCELOCATION) {
			@Override public Token createToken(Compiler c, Matcher matcher, int line, int col) throws CompileError {
				c.cursor=matcher.end();
				return new ResourceToken(line,col,matcher);
			}
		};
		public final ResourceLocation res;
		public ResourceToken(int line, int col,Matcher m) {
			super(line, col);
			this.res=new ResourceLocation(m);
		}
		@Override
		public String asString() {
			return null;
		}
		
	}
	public final String namespace;
	//path includes the end
	public final String path;
	public final String end;
	public ResourceLocation(Matcher m) {
		this.namespace=m.group("namespace");
		this.path=m.group("path");
		this.end=m.group("end");
	}
	public ResourceLocation(Namespace ns,Path p) {
		this.namespace=ns.name;
		//path relative to data/<namespace>/<catagory>/
		String[] s=p.toString().replace("\\", "/").split("\\.");
		this.path=s[0];
		s=p.getFileName().toString().replace("\\", "/").split("\\.");
		this.end=s[0];
		//works
	}
	public ResourceLocation(Namespace ns,String p) {
		this.namespace=ns.name;
		//path relative to data/<namespace>/<catagory>/
		String[] s=p.split("/");
		this.path=p;
		this.end=s[s.length-1];
		//works
	}
	public ResourceLocation(String ns,String p) {
		this.namespace=ns;
		//path relative to data/<namespace>/<catagory>/
		String[] s=p.split("/");
		this.path=p;
		this.end=s[s.length-1];
		//works
	}
	@Override public String toString() {
		return "%s:%s".formatted(namespace,path);
	}
	public String stringLiteral() {
		return "\"%s:%s\"".formatted(namespace,path);
	}
	public static List<String> literals(List<ResourceLocation> in){
		List<String>  out=new ArrayList<String>();
		for(ResourceLocation r:in) {
			out.add(r.stringLiteral());
		}return out;
	}
	@Override public boolean equals(Object other) {
		if (other instanceof ResourceLocation)return this.namespace.equals(((ResourceLocation) other).namespace)
				&& this.path.equals(((ResourceLocation) other).path);
		return false;
	}
	@Override public int hashCode() {
		//this is needed to be a key in a hashmap
		return this.toString().hashCode();
	}
	
}
