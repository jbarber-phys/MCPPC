package net.mcppc.compiler;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.OutputDump;

public class CompileJob {
	public static final String DATA="data";
	public static final String EXT_SRC="mcpp";
	public static final String EXT_H="mch";
	public static final String EXT_MCF="mcfunction";
	public static final String EXT_JSON="json";
	

	public static final String SUBDIR_SRC="src";
	public static final String SUBDIR_HDR="include";
	public static final String SUBDIR_MCF="functions";
	
	public static final String FILE_TO_SUBDIR_SUFFIX="__/";

	public static final String PACK_MCMETA="pack.mcmeta";
	
	public static final PrintStream fileLog=OutputDump.out;
	public static final PrintStream compileHdrLog=OutputDump.out;
	public static final PrintStream compileMcfLog=System.out;
	
	public static final Scanner stdin = new Scanner(System.in);
	public static class FFs{
		public static final FileFilter data = new FileFilter() {
		      @Override public boolean accept(File file) {
		         return (file.isDirectory() && file.getName().equals(DATA));
		      }};
	      public static final FileFilter src = new FileFilter() {
		      @Override public boolean accept(File file) {
		         return (file.isDirectory() && file.getName().equals(SUBDIR_SRC));
		      }};
	      public static final FileFilter headers = new FileFilter() {
		      @Override public boolean accept(File file) {
		         return (file.isDirectory() && file.getName().equals(SUBDIR_HDR));
		      }};
	      public static final FileFilter mcfunctions = new FileFilter() {
		      @Override public boolean accept(File file) {
		         return (file.isDirectory() && file.getName().equals(SUBDIR_MCF));
		      }};
	      public static final FileFilter dirs = new FileFilter() {
	      @Override public boolean accept(File file) {
	         return (file.isDirectory());
	      	}};
	}
	
	public static String getCWD() {
		return System.getProperty("user.dir");
	}
	//compiles many files at once:
	public static class Namespace {
		//TODO remember to always load the setup file for each namespace
		String name;
		//File dataDir;//depricated; is asociated with multiple locations; CompileJob has methods for this instead;
		int maxNumRegisters;
		ArrayList<Path> srcFilesRel=new ArrayList<Path>();
		public Namespace(File data) {
			this.name=data.getName();
			//this.dataDir=data;
		}
		public ResourceLocation getLoadFunction() {
			return new ResourceLocation(this,"mcpp__load");
		}
		public void fillMaxRegisters(int i) {
			this.maxNumRegisters=Math.max(this.maxNumRegisters, i);
		}
		public boolean isMC() {
			return this.name.equals("minecraft");
		}
	}
	
	//nonstatic below
	final Map<String,Namespace> namespaces=new HashMap<String,Namespace>();
	
	//there shound never be more than 1 compiler per resourcelocation
	//(src compilers may be handling a header as well)
	final Map<ResourceLocation,Compiler> compilers=new HashMap<ResourceLocation,Compiler>();
	final Map<ResourceLocation,Compiler> headerOnlyCompilers=new HashMap<ResourceLocation,Compiler>();
	
	//list any imports that could require source files with their own dependancies on linked dirs; only 
	final List<ResourceLocation> externalImports=new ArrayList<ResourceLocation>();
	/**
	 * primary root directory; used to locate namespaces to compile
	 * all the other roots are secondary
	 */
	final Path rootSrc;

	/**
	 * specifies the root where all mch generated will go; may be same as src root
	 */
	final Path rootHeaderOut;
	/**
	 * specifies the root to find other hdrs if not in rootHeaderOut; similar to g++ -I'...'
	 */
	final List<Path> rootIncludes=new ArrayList<Path>();
	
	/**
	 * specifies where to find any mcf functions that need to be copied into the resulting datapack;
	 *  may be same as rootSrc root but not as rootDatapack
	 *  not every mcf must be here as long as the other mcf files are already in t;
	 */
	final List<Path> rootLinks=new ArrayList<Path>();

	/**
	 * specifies where all mcf functions will go; may be same as src root
	 */
	public final Path rootDatapack;
	
	
	final boolean GEN_H_FROM_SRC=true;
	/**
	 * sets to compile with the java project as the datapack directory (with the source files)
	 */
	public CompileJob() {
		this.rootSrc=this.rootDatapack=this.rootHeaderOut=Path.of(getCWD());
	}
	public CompileJob(Path root) {
		this.rootSrc=this.rootDatapack=this.rootHeaderOut=root;
	}
	public CompileJob(Path root,Path rootDatapack) {
		this.rootSrc=this.rootHeaderOut=root;
		this.rootDatapack=rootDatapack;
	}
	public CompileJob includePath(Path libroot) {
		this.rootIncludes.add(libroot);
		return this;
	}
	public Path pathForSrc(ResourceLocation res) {
		Path p=this.rootSrc.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_SRC).resolve(res.path+"."+CompileJob.EXT_SRC);
		return p;
	}
	public Path pathForSrcDir(Namespace ns) {
		Path p=this.rootSrc.resolve(CompileJob.DATA).resolve(ns.name).resolve(CompileJob.SUBDIR_SRC);
		return p;
	}
	/**
	 * gets a path for a header to be generated at
	 * @param res
	 * @return
	 */
	public Path pathForHeaderOut(ResourceLocation res) {
		Path p=this.rootHeaderOut.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_HDR).resolve(res.path+"."+CompileJob.EXT_H);
		return p;
	}
	/**
	 * gets a path for a header to be included from
	 * @param res
	 * @return
	 */
	public Path pathForInclude(ResourceLocation res) {
		//header in
		//search output first
		Path p=this.rootHeaderOut.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_HDR).resolve(res.path+"."+CompileJob.EXT_H);
		if(p.toFile().exists())return p;
		for(Path r:this.rootIncludes) {
			p=r.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_HDR).resolve(res.path+"."+CompileJob.EXT_H);
			if(p.toFile().exists())return p;
		}
		return null;
	}
	
	public Path pathForMcf(ResourceLocation res) {
		Path p=this.rootDatapack.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_MCF).resolve(res.path+"."+CompileJob.EXT_MCF);
		return p;
	}
	public Path pathForTagLoad() {
		Path p=this.rootDatapack.resolve(CompileJob.DATA).resolve("minecraft/tags/functions").resolve("load"+"."+CompileJob.EXT_JSON);
		return p;
	}
	public Path pathForPackMcmeta() {
		Path p=this.rootDatapack.resolve(CompileJob.PACK_MCMETA);
		return p;
	}
	public boolean hasFileInterfaceFor(ResourceLocation res) {
		if(this.compilers.containsKey(res)) {
			if(this.compilers.get(res).areLocalsLoaded) return true;
			else return false;
		}
		if(this.headerOnlyCompilers.containsKey(res)) {
			if(this.headerOnlyCompilers.get(res).areLocalsLoaded) return true;
			else return false;
		}
		return false;
	}
	public FileInterface getFileInterfaceFor(ResourceLocation res) throws FileNotFoundException, CompileError {
		Compiler c=null;
		//CompileJob.compileMcfLog.printf("%s, %s;\n", this.compilers.keySet(),this.headerOnlyCompilers.keySet());
		//CompileJob.compileMcfLog.printf("%s;\n", res);
		if(this.compilers.containsKey(res))c= this.compilers.get(res);
		else if(this.headerOnlyCompilers.containsKey(res))c= this.compilers.get(res);
		if(c==null) {
			//else compile a new header
			Path p = this.pathForInclude(res);
			if (!this.namespaces.containsKey(res.namespace)) {
				this.namespaces.put(res.namespace, new Namespace(p.toFile()));
			}
			c=new Compiler(this, res);c.isHeaderOnly=true;
			this.headerOnlyCompilers.put(res, c);
		}
		if(c.areLocalsLoaded) {
			return c.myInterface;
		}else {
			c.compile1(false);
			c.unload();
			return c.myInterface;
			
		}
		
		
	}
	public boolean addPossibleExternalDependancy(ResourceLocation res) {
		if(this.externalImports.contains(res)) return false;
		this.externalImports.add(res);
		return true;
	}
	public void compileAll() {
		addAllNamespaces();
		for(Namespace ns: this.namespaces.values()) {
			discoverSrc(ns);
		} for(Namespace ns: this.namespaces.values()) {
			this.genHeaders(ns);
		}
		if (!this.haspackmcmeta()) {
			//this is to prevent accidental deletion of files from choosing the wrong output dir
			System.out.printf("warning: target pack dir '%s' does not contain a pack.mcmeta;\n\t is this the correct output directory? (Y,N) ",
					this.rootDatapack.toAbsolutePath());
			String s=stdin.nextLine();
			if(s.equals("Y")) ;//proceed
			else return;
		}else{
			//consider loading in version number
		}
		
		for(Namespace ns: this.namespaces.values()) {
			this.genMcf(ns);
		}
		this.genNamespaceFunctionsAndTags();
	}
	private boolean haspackmcmeta() {
		Path p=this.pathForPackMcmeta();
		return p.toFile().exists();
	}
	public void addAllNamespaces() {
		//System.out.println("adding namespaces");
		//File cwd=new File(getCWD());
		
		Path data=this.rootSrc.resolve(DATA);
		System.out.println(data);
		//fileLog.println(namespacefolders);
		List<File> fs;
		try {
			fs=Files.list(data).map(Path::toFile).collect(Collectors.toList());
			//fs = Files.walk(data,2)
			//        .filter(Files::isDirectory)
			//        .map(Path::toFile)
			//        .collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		//File[] fs2 = data.toFile().listFiles(FFs.dirs);
		for(File ns:fs) {
			fileLog.println("found namespace %s".formatted(ns.getName()));
			this.namespaces.put(ns.getName(), new Namespace(ns));
		}
	}
	public void discoverSrc(Namespace ns) {
		List<Path> srcs = null;
		Path srcPath=this.pathForSrcDir(ns);
        try (Stream<Path> walk = Files.walk(srcPath)) {
            srcs = walk.filter(Files::isRegularFile)
            		.filter(p -> p.getFileName().toString().endsWith(EXT_SRC))
                    .collect(Collectors.toList());
        } catch (IOException e) {
        	if(ns.isMC()) srcs=new ArrayList<Path>();
        	else e.printStackTrace();
		}
        fileLog.println("looking for %d source files".formatted(srcs.size()));
        for(Path p: srcs) {
        	Path rp = srcPath.relativize(p);
        	fileLog.println("found source %s:%s".formatted(ns.name,rp));
        	ns.srcFilesRel.add(rp);
        	ResourceLocation script=new ResourceLocation(ns,rp);
        	this.compilers.put(script, new Compiler(this,script));
        }
	}
	//depricated; dont use
	@Deprecated public void discoverHdrs(Namespace ns) {
			String a=null;a.getBytes();//dont use me
		List<Path> srcs = null;
		//Path srcPath=ns.dataDir.listFiles(FFs.src)[0].toPath();
		Path srcPath=this.pathForSrcDir(ns);
        try (Stream<Path> walk = Files.walk(srcPath)) {
            srcs = walk.filter(Files::isRegularFile)
            		.filter(p -> p.getFileName().toString().endsWith(EXT_SRC))
                    .collect(Collectors.toList());
        } catch (IOException e) {
			e.printStackTrace();
		}
    	System.out.println(srcs.size());
        for(Path p: srcs) {
        	Path rp = srcPath.relativize(p);
        	fileLog.println("found header %s:%s".formatted(ns.name,rp));
        	ns.srcFilesRel.add(rp);
        }
	}
	public boolean genHeaders(Namespace ns) {
		fileLog.println("genHeaders for namespace %s".formatted(ns.name));
		fileLog.println("%d".formatted(ns.srcFilesRel.size()));
		boolean success=true;
        for(ResourceLocation res:this.compilers.keySet()) if (res.namespace.equals(ns.name)) {
        	fileLog.println("genHeaders %s".formatted(res));
        	Compiler c=this.compilers.get(res);
        	//fileLog.println(script);
        	//Path samePath=this.pathForSrc(script);fileLog.println(samePath);
        	try {
            	c.compile1(this.GEN_H_FROM_SRC);//ready to test
            	boolean canCompile=c.myInterface.attemptLoadLibs(this);
            	if(canCompile) {
            		//go ahead and compile early
            		//c.compile2(); //not ready yet
            	}else {
            		
            	}
            	//save some memory
        		c.unload();
        		System.gc();//induce garbage collection
        	}catch (CompileError  ce) {
        		ce.printStackTrace();
        		//System.out.println(ce.getMessage());
        		ce.printStackTrace();
        		success=false;
        	}catch(FileNotFoundException ce){
        		ce.printStackTrace();
        		success=false;
        		//break;//keep going for now
        	}	finally {
        	
        		c.close();
        	}
        	
        }
        return success;
		
	}
	public boolean genMcf(Namespace ns) {
		fileLog.println("compiling namespace %s".formatted(ns.name));
		fileLog.println("%d".formatted(ns.srcFilesRel.size()));
		boolean success=true;
        for(ResourceLocation res:this.compilers.keySet()) if (res.namespace.equals(ns.name)) {
        	fileLog.println("genHeaders %s".formatted(res));
        	Compiler c=this.compilers.get(res);
        	if(c.hasCompiled) {
        		compileMcfLog.printf("src file %s was already compiled\n",res);
        		continue;
        	}
        	if(!c.areLocalsLoaded) {
        		compileMcfLog.printf("skipping compilation for %s; failed to load self-interface;",res);
        		continue;
        	}
        	try {
            	c.compile2();
            	//save some memory
        		c.unload();
        		System.gc();//induce garbage collection
        	}catch (CompileError  ce) {
        		ce.printStackTrace();
        		//System.out.println(ce.getMessage());
        		ce.printStackTrace();
        		success=false;
        	}catch(FileNotFoundException ce){
        		ce.printStackTrace();
        		success=false;
        		//break;//keep going for now
        	}	finally {
        	
        		c.close();
        	}
        	
        }
        return success;
		
	}
	public void genNamespaceFunctionsAndTags() {
		List<ResourceLocation> loads=new ArrayList<ResourceLocation>();
		for(Namespace ns: this.namespaces.values()) {
			if(ns.srcFilesRel.size()==0)continue;//skip if no src found
			ResourceLocation mcf=ns.getLoadFunction();
			Path load=this.pathForMcf(mcf);
			PrintStream p=null;
			boolean success=true;
			try {
				File f=load.toFile();
				f.getParentFile().mkdirs();
				f.createNewFile();
				p=new PrintStream(f);
				Register.createAll(p, ns.maxNumRegisters);
			} catch (IOException e) {
				e.printStackTrace();
				CompileJob.compileMcfLog.printf("failed to make namespace %s;\n", ns.name);
				success=false;
			}finally {
				if(p!=null)p.close();
			}
			if(success) {
				CompileJob.compileMcfLog.printf("made namespace %s;\n", ns.name);
				loads.add(mcf);
				
			}
			
		}
		if(loads.size()==0) {
			CompileJob.compileMcfLog.printf("didn't make any namespaces;\n");return;
		}
		//TODO make tag
		PrintStream tagtick=null;
		try {
			File f=this.pathForTagLoad().toFile();
			f.getParentFile().mkdirs();
			f.createNewFile();
			tagtick=new PrintStream(f);
			List<String> values=ResourceLocation.literals(loads);
			String fill=String.join(",\n\t\t", values);
			tagtick.printf("{\n\t\"values\": [\n\t\t%s\n\t]\n}", fill);
			CompileJob.compileMcfLog.printf("successfully made load tag;\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if(tagtick!=null)tagtick.close();
		}
		
		
	}
	//TODO figure out (and when to) which external mcfs need to be copied (linking);
		//may need to re-read headers to recursively find dependancies
}
