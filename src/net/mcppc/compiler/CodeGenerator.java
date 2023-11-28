package net.mcppc.compiler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.mcppc.compiler.*;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.target.Targeted;
/**
 * manages code that generates mcfunctions used by mcpp;
 * @author RadiumE13
 *
 */
public abstract class CodeGenerator {
	public final ResourceLocation res;
	public final boolean takesMacros;
	public CodeGenerator(String path) {
		this(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,path));
	}
	public CodeGenerator(ResourceLocation res) {
		this.res = res;
		this.takesMacros=false;
	}
	public CodeGenerator(ResourceLocation res,boolean macro) {
		this.res = res;
		this.takesMacros=macro;
	}
	public abstract void build(PrintStream p,CompileJob job,Namespace ns) throws CompileError ;

	@Targeted
	public String getCall() {
		if(this.takesMacros) {
			Variable macros = this.macroTag();
			return "function %s with %s".formatted(this.res.toString(),macros.dataPhrase());
		}
		else return "function %s".formatted(this.res.toString());
	}
	public Variable macroTag () {
		return Variable.macrosTag(this.res);
	}
	public CodeGenerator subscribe() {
		CodeGenerator.CODE_GENERATORS.add(this);
		return this;
	}
	//static stuff:
	

	private static final List<CodeGenerator> CODE_GENERATORS = new ArrayList<CodeGenerator>();
	public static void register(CodeGenerator g) {
		CODE_GENERATORS.add(g);
	}
	public static boolean generateAll(CompileJob job){
		PrintStreamLineCounting p=null;
		boolean success=true;
		for(CodeGenerator gen:CODE_GENERATORS) try {
			Path f=job.pathForMcf(gen.res);
			f.toFile().getParentFile().mkdirs();
			try {
				f.toFile().createNewFile();
			} catch (IOException e) {
				job.compileMcfError.printf("exception while generating mcf file %s\n",gen.res.toString());
				e.printStackTrace();
				continue;
			}
			p = new PrintStreamLineCounting(f.toFile());
			Namespace ns= job.enshureNamespace(gen.res);
			gen.build(p, job, ns);
			p.close();
			p.announceLines(gen.res.toString());
			
		}catch (FileNotFoundException e) {
			e.printStackTrace();
			p.close();
			success=false;
		} catch (CompileError e) {
			e.printStackTrace();
			p.close();
			success=false;
		}
		return success;
	}
}
