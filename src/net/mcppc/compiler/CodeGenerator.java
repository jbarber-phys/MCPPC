package net.mcppc.compiler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.mcppc.compiler.*;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;
/**
 * manages code that generates mcfunctions for stdlib
 * @author jbarb_t8a3esk
 *
 */
public abstract class CodeGenerator {
	public final ResourceLocation res;
	public CodeGenerator(String path) {
		this(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,path));
	}
	public CodeGenerator(ResourceLocation res) {
		this.res = res;
	}
	public abstract void build(PrintStream p,CompileJob job,Namespace ns) throws CompileError ;
	
	public String getCall() {
		return "function %s".formatted(this.res.toString());
	}
	
	public CodeGenerator subscribe() {
		CodeGenerator.CODE_GENERATORS.add(this);
		return this;
	}
	//static stuff:
	

	private static final List<CodeGenerator> CODE_GENERATORS = new ArrayList<CodeGenerator>();
	
	public static boolean generateAll(CompileJob job){
		PrintStreamLineCounting p=null;
		boolean success=true;
		for(CodeGenerator gen:CODE_GENERATORS) try {
			Path f=job.pathForMcf(gen.res);
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
