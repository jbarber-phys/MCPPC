package net.mcppc.compiler;
/**
 * represents an imported file;
 * @author RadiumE13
 *
 */
public class Library {
	public final String nickname;
	public final ResourceLocation res;
	
	FileInterface f;
	public Library(ResourceLocation res) {
		this.res = res;
		this.nickname=res.end;
	}
	public Library(String name,ResourceLocation res) {
		this.res = res;
		this.nickname=name;
	}
	
}
