package net.mcppc.compiler;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.OutputDump;
import net.mcppc.compiler.tokens.Import;

public class CompileJob {
	public static final String DATA="data";
	public static final String RESOURCES="resources";
	public static final String GENERATED="generated";
	public static final String EXT_SRC="mcpp";
	public static final String EXT_H="mch";
	public static final String EXT_MCF="mcfunction";
	public static final String EXT_JSON="json";
	

	public static final String STDLIB_NAMESPACE="mcppc";
	public static final String MINECRAFT="minecraft";
	public static final boolean INCLUDE_STDLIB=true;
	public static final boolean PRINT_TREE=false;
	

	public static final String SUBDIR_SRC="src";
	public static final String SUBDIR_HDR="include";
	public static final String SUBDIR_MCF="functions";
	
	public static final String FILE_TO_SUBDIR_SUFFIX="__/";

	public static final String PACK_MCMETA="pack.mcmeta";
	
	public static final PrintStream fileLog=OutputDump.out;
	public static final PrintStream compileHdrLog=System.out;
	public static final PrintStream compileMcfLog=System.out;

	public static final PrintStream dommentLog=OutputDump.out;
	
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
		String name;
		public final boolean isExternal;
		//File dataDir;//depricated; is asociated with multiple locations; CompileJob has methods for this instead;
		int maxNumRegisters;
		ArrayList<Path> srcFilesRel=new ArrayList<Path>();
		final Map<String, Variable> objectives = new HashMap<String,Variable>();
		public Namespace(File data) {
			this(data.getName(),false);
			//this.dataDir=data;
		}
		public Namespace(String name,boolean isExternal) {
			this.name=name;
			//this.dataDir=data;
			this.isExternal=isExternal;
		}
		public ResourceLocation getLoadFunction() {
			return new ResourceLocation(this,"mcpp__load");
		}
		public void fillMaxRegisters(int i) {
			this.maxNumRegisters=Math.max(this.maxNumRegisters, i);
		}
		public boolean isMC() {
			return this.name.equals(MINECRAFT);
		}
		public void addObjective(Variable v) {
			if(v.pointsTo==Mask.SCORE && !v.isStruct()) this.objectives.putIfAbsent(v.getAddress(), v);
		}
	}
	
	//nonstatic below
	public boolean CLEAN_MCF_SUBDIR=true;
	public boolean CHECK_INCLUDES_FOR_CIRCULAR_RUNS=false;
	public final int MAX_NUM_CMDS=(int) Math.round(Math.pow(2, 15)-1);
	private boolean isBuildingStdLib=false; public void stdLib() {this.isBuildingStdLib=true;}
	private boolean debugDomment=false; public void debugMode() {this.debugDomment=true;} public boolean isDebug() {return this.debugDomment;}
	
	final Map<String,Namespace> namespaces=new HashMap<String,Namespace>();
	
	//there shound never be more than 1 compiler per resourcelocation
	//(src compilers may be handling a header as well)
	final Map<ResourceLocation,Compiler> compilers=new HashMap<ResourceLocation,Compiler>();
	final Map<ResourceLocation,Compiler> headerOnlyCompilers=new HashMap<ResourceLocation,Compiler>();
	
	//list any imports that could require source files with their own dependancies on linked dirs; only 
	//final List<ResourceLocation> externalImports=new ArrayList<ResourceLocation>();
	final Stack<ResourceLocation> externalImports=new Stack<ResourceLocation>();
	final Stack<ResourceLocation> externalImportsStrict=new Stack<ResourceLocation>();
	/**
	 * primary root directory; used to locate namespaces to compile
	 * all the other roots are secondary
	 */
	Path rootSrc;public void setRootSrc(Path o) {
		this.rootSrc=o;
	}

	/**
	 * specifies the root where all mch generated will go; may be same as src root
	 */
	Path rootHeaderOut;public void setRootHeaderOut(Path o) {
		this.rootHeaderOut=o;
	}
	/**
	 * specifies the root to find other hdrs if not in rootHeaderOut; similar to g++ -I'...'
	 */
	final List<Path> rootIncludes=new ArrayList<Path>();
	
	/**
	 * specifies where to find any mcf functions that need to be copied into the resulting datapack;
	 *  may be same as rootSrc root but not as rootDatapack
	 *  not every mcf must be here as long as the other mcf files are already in t;
	 *  TODO figure out if to do this or not
	 */
	final List<Path> rootLinks=new ArrayList<Path>();public void addLink(Path o) {
		rootLinks.add(o);
	}

	/**
	 * specifies where all mcf functions will go; may be same as src root
	 */
	Path rootDatapack;public void setRootDatapack(Path o) {
		this.rootDatapack=o;
	}
	public static Path getResources() { return Path.of(getCWD()).resolve(RESOURCES); }
	public static Path getGeneratedResources() { return Path.of(getCWD()).resolve(GENERATED); }
	
	final boolean GEN_H_FROM_SRC=true;
	/**
	 * sets to compile with the java project as the datapack directory (with the source files)
	 */
	public CompileJob() {
		this.rootSrc=this.rootDatapack=this.rootHeaderOut=Path.of(getCWD());
		if(CompileJob.INCLUDE_STDLIB) {
			Path resources=getResources();
			Path generated=getGeneratedResources();
			
			this.includePath(generated);
			this.addLink(generated);
			
			this.includePath(resources);
			this.addLink(resources);
		}
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
	public Path pathForMcfSubfunctionsDir(ResourceLocation res) {
		Path p=this.rootDatapack.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_MCF).resolve(res.path+CompileJob.FILE_TO_SUBDIR_SUFFIX);
		return p;
	}
	public Path findPathForLink(ResourceLocation res) {
		Path p;
		//p=this.rootDatapack.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_MCF).resolve(res.path+"."+CompileJob.EXT_MCF);
		//if(p.toFile().exists())return null;//dont circular link
		for(Path r:this.rootLinks) {
			p=r.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_MCF).resolve(res.path+"."+CompileJob.EXT_MCF);
			if(p.toFile().exists())return p;
		}
		return null;
	}
	public Path findPathForLinkSubfunctionsDir(ResourceLocation res) {
		Path p;
		//p=this.rootDatapack.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_MCF).resolve(res.path+CompileJob.FILE_TO_SUBDIR_SUFFIX);
		//if(p.toFile().exists())return null;//dont circular link
		for(Path r:this.rootLinks) {
			p=r.resolve(CompileJob.DATA).resolve(res.namespace).resolve(CompileJob.SUBDIR_MCF).resolve(res.path+CompileJob.FILE_TO_SUBDIR_SUFFIX);
			if(p.toFile().exists())return p;
		}
		return null;
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
	public FileInterface getFileInterfaceFor(ResourceLocation res,boolean isStrict) throws FileNotFoundException, CompileError {
		return this.enshureCompiler(res,isStrict).myInterface;
	}

	public Namespace enshureNamespace(ResourceLocation res)  {
		Namespace ns;
		if (!this.namespaces.containsKey(res.namespace)) {
			System.out.printf("linked namespace %s;\n",res.namespace);
			this.namespaces.put(res.namespace, ns=new Namespace(res.namespace,true));
		}else ns=this.namespaces.get(res.namespace);
		return ns;
	}
	private Compiler enshureCompiler(ResourceLocation res,boolean isStrict) throws FileNotFoundException, CompileError {
		Compiler c=null;
		//CompileJob.compileMcfLog.printf("%s, %s;\n", this.compilers.keySet(),this.headerOnlyCompilers.keySet());
		//CompileJob.compileMcfLog.printf("%s;\n", res);
		if(this.compilers.containsKey(res))c= this.compilers.get(res);
		else if(this.headerOnlyCompilers.containsKey(res))c= this.compilers.get(res);
		if(c==null) {
			//else compile a new header
			Path p = this.pathForInclude(res);
			if(p==null)throw new CompileError("could not find include for %s;".formatted(res));
			Namespace ns=this.enshureNamespace(res);
			c=new Compiler(this, res,ns,true);
			c.isStrict=isStrict;
			this.headerOnlyCompilers.put(res, c);
		}
		if(c.areLocalsLoaded) {
			return c;
		}else {
			c.compile1(false);
			c.unload();
			return c;
			
		}
	}
	public boolean addPossibleExternalDependancy(Import i) {
		if(this.compilers.containsKey(i.getLib())) return false;
		if(this.headerOnlyCompilers.containsKey(i.getLib())) return false;
		if(this.externalImports.contains(i.getLib())) return false;
		
		//this is only runned if not strict
		this.externalImports.add(i.getLib());
		//this.externalImportsAreStrict.add(i.isStrict);
		
		return true;
	}
	public boolean compileAll() {
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
			else return false;
			this.CLEAN_MCF_SUBDIR=false;//avoid cleaning just in case
		}else{
			//consider loading in version number
		}

		Collection<Namespace> srcNamespaces=new ArrayList<Namespace>();
		for(Namespace ns: this.namespaces.values()) {
			srcNamespaces.add(ns);
		}//avoid java.util.ConcurrentModificationException
		for(Namespace ns: srcNamespaces) {
			this.genMcf(ns);
		}
		if(this.isBuildingStdLib) {
			CodeGenerator.generateAll(this);
		}
		this.traceExternalImports();
		if(!this.checkForCircularRuns())return false;
		this.linkAll();
		this.genNamespaceFunctionsAndTags();
		boolean success=true;
		for(Compiler c:this.compilers.values()) if(!c.hasCompiled){
			success=false;
			System.err.printf("failed to compile %s;\n", c.resourcelocation);
		}
		if(success) {
			System.out.println("========================");
			System.out.println("Compilation Successful;");
		}
		else {
			System.err.println("========================");
			System.err.println("Compilation Failed;");
		}
		return success;
	}
	private boolean haspackmcmeta() {
		if(this.isBuildingStdLib)return true;
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
        	this.compilers.put(script, new Compiler(this,script,ns));
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
            	c.compile1(this.GEN_H_FROM_SRC);
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
	private void genNamespaceLoadFunction(PrintStream p,Namespace ns) throws CompileError {
		Register.createAll(p, ns.maxNumRegisters);
		for(Compiler c:this.compilers.values()) {
			if (c.namespace==ns) {//do not include header only compilers
				c.myInterface.allocateAll(p, ns);
			}
		}
		for(Variable v:ns.objectives.values()) {
			v.makeObjective(p);
		}
		
	}
	public void genNamespaceFunctionsAndTags() {
		List<ResourceLocation> loads=new ArrayList<ResourceLocation>();
		for(Namespace ns: this.namespaces.values()) {
			if((!ns.isExternal) && ns.srcFilesRel.size()==0)continue;//skip if no src found
			ResourceLocation mcf=ns.getLoadFunction();
			Path load=this.pathForMcf(mcf);
			PrintStreamLineCounting p=null;
			boolean success=true;
			if(ns.isExternal) {
				this.copyLinkedNamespaceToDatapack(ns);
				System.out.printf("copied load function %s;\n", mcf);
			}
			else try {
				File f=load.toFile();
				f.getParentFile().mkdirs();
				f.createNewFile();
				p=new PrintStreamLineCounting(f);
				this.genNamespaceLoadFunction(p, ns);
				
			} 
			catch (IOException e) {
				e.printStackTrace();
				CompileJob.compileMcfLog.printf("failed to make namespace %s;\n", ns.name);
				success=false;
			} catch (CompileError e) {
				e.printStackTrace();
				success=false;
			}finally {
				if(p!=null) {
					p.close();
					p.announceLines(mcf.toString());
				}
			}
			if(success) {
				CompileJob.compileMcfLog.printf("made namespace %s;\n", ns.name);
				loads.add(mcf);
				
			}
			
		}
		if(loads.size()==0) {
			CompileJob.compileMcfLog.printf("didn't make any namespaces;\n");return;
		}
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
	public boolean checkForCircularRuns() {
		List<ResourceLocation> cs=new ArrayList<ResourceLocation>();
		for(Compiler key: this.compilers.values()) {
			cs.add(key.resourcelocation);
		}
		if(this.CHECK_INCLUDES_FOR_CIRCULAR_RUNS)for(Compiler key: this.headerOnlyCompilers.values()) {
			cs.add(key.resourcelocation);
		}

		int N=cs.size();
		int[][] graph = new int[N][N];
		for(int i=0;i<N;i++) {
			Compiler c=this.compilers.get(cs.get(i));
			if (c==null)c=this.headerOnlyCompilers.get(cs.get(i));
			if(c!= null && c.myInterface!=null)for(ResourceLocation r: c.myInterface.runs.values()) {
				int b;
				if((b=cs.indexOf(r)) >=0) graph[i][b]=1;
			}
		}
		//for(int i=0;i<N;i++) {for(int j=0;j<N;j++) {System.out.print(graph[i][j]+"   ");}System.out.print("\n");}
    	//System.out.printf("%s\n",this.compilers.keySet());
    	//System.out.printf("%s\n",this.headerOnlyCompilers.keySet());
    	//System.out.printf("%s\n",N);
		//int[] loop=null;
		int[] loop=CMath.findCycle(graph, N);
		if(loop!=null) {
			System.err.println("Error: found circular runs between:");
			for(int i=0;i<loop.length;i++)
				System.err.printf("\t %s\n",cs.get(loop[i]));
			return false;
				
		}return true;
	}

	public boolean traceExternalImports() {
		boolean success=true;
		while(!this.externalImports.isEmpty()) {
			ResourceLocation res = this.externalImports.pop();
			boolean isStrict= false;//this.externalImportsAreStrict.pop();
			if(!isStrict) try {
				Compiler c=this.enshureCompiler(res,false);
				//do nothing with it
			} catch (FileNotFoundException e) {
				//ignore, this is OK
			} catch (CompileError e) {
				e.printStackTrace();
				success=false;
			}
			
		}
		return success;
		
	}
	public static void copyDirectory(Path dirFrom, Path dirTo) 
			  throws IOException {
		String sourceDirectoryLocation = dirFrom.toString();
		String destinationDirectoryLocation = dirTo.toString();
		/*
		if(dirTo.toFile().exists()) {Files.walk(dirTo)
	      .forEach(old -> {
	    		//System.err.println(old.toAbsolutePath().toString());
	          old.toFile().delete();
	      });
		*/
		if(dirTo.toFile().exists())Files.walk(dirTo)
	      .sorted(Comparator.reverseOrder())
	      .map(Path::toFile)
	      .forEach(File::delete);
	    Files.walk(dirFrom)
	      .forEach(source -> {
	          Path destination = Paths.get(destinationDirectoryLocation, source.toString()
	            .substring(sourceDirectoryLocation.length()));
	          try {
	              Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
	          } catch (IOException e) {
	              e.printStackTrace();
	          }
	      });
	}
	public void copyLinkToDatapack(Compiler c) {
		ResourceLocation res=c.resourcelocation;
		Path from = this.findPathForLink(res);
		Path fromsub = this.findPathForLinkSubfunctionsDir(res);
		Path to = this.pathForMcf(res);
		Path tosub = this.pathForMcfSubfunctionsDir(res);
	    if(from!=null)try {
			to.toFile().getParentFile().mkdirs();
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
			if(fromsub!=null) {
				//tosub.toFile().delete();
				copyDirectory(fromsub,tosub);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void copyStrictLinkToDatapack(ResourceLocation res) {
		Path from = this.findPathForLink(res);
		Path to = this.pathForMcf(res);
		//from.toFile().list
    	//System.err.printf("strict res %s\n", res);
		if(from==null) return;
		
    	Path dirFrom = from.getParent();
    	Path dirTo = to.getParent();
    	String suffix = "." + CompileJob.EXT_MCF;
    	String prefixname = from.getFileName().toString().replace(suffix, "");
    	//System.err.printf("%s\n%s\n%s\n", dirFrom,dirTo,prefixname);
    	List<String> files=new ArrayList<String>();
    	for(String s:dirFrom.toFile().list()) {
    		if (s.startsWith(prefixname)&& s.endsWith(suffix)) files.add(s);
    	}
    	//System.err.println(files);
    	for(String s:files) {
    		Path fileFrom=dirFrom.resolve(s);
    		Path fileTo=dirTo.resolve(s);
			fileTo.toFile().getParentFile().mkdirs();
			try {
				Files.copy(fileFrom, fileTo, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
	}
	public void copyLinkedNamespaceToDatapack(Namespace ns) {
		ResourceLocation res=ns.getLoadFunction();
		Path from = this.findPathForLink(res);
		Path to = this.pathForMcf(res);
		if(from!=null)try {
			to.toFile().getParentFile().mkdirs();
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public boolean linkAll() {
		
		for(Compiler c:this.headerOnlyCompilers.values()) {
			if(!c.isStrict)this.copyLinkToDatapack(c);
		} 
    	//System.err.printf("strict number %s\n", this.externalImportsStrict.size());
		while(!this.externalImportsStrict.empty()) {
			ResourceLocation res=this.externalImportsStrict.pop();
			copyStrictLinkToDatapack(res);
		}
		return true;
		
	}
}
