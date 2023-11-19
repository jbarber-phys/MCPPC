package net.mcppc.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Token;
/**
 * location of a resource datapack:path/to/thing
 * resourcelocation is synonym for Namespaced ID
 * @author RadiumE13
 *
 */

@Targeted 
public class ResourceLocation {
	@FunctionalInterface
	public static interface IPathGetter{
		public Path get(ResourceLocation res);
	}
	public static ResourceToken getNext(Compiler c) throws CompileError {
		return (ResourceToken)c.nextNonNullMatch(Factories.genericLook(ResourceToken.factory));
	}
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
		public ResourceToken(int line, int col,ResourceLocation res) {
			super(line, col);
			this.res=res;
		}
		@Override
		public String asString() {
			return this.res.toString();
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
	public ResourceLocation(String p) {
		this(CompileJob.MINECRAFT,p);
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
	public static List<String> strings(List<ResourceLocation> in){
		List<String>  out=new ArrayList<String>();
		for(ResourceLocation r:in) {
			out.add(r.toString());
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

	@Targeted
	public void run(PrintStream p,VTarget t) {
		p.printf("function %s\n",this.toString());
	}
	@Targeted
	public void runIf(PrintStream p,Register r, VTarget tg) {
		p.printf("execute if %s run function %s\n", r.testMeInCMD(),this.toString());
	}
	@Targeted
	public void runUnless(PrintStream p,Register r, VTarget tg) {
		p.printf("execute unless %s run function %s\n", r.testMeInCMD(),this.toString());
	}
	
}
