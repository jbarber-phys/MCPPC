package net.mcppc.main;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;

import net.mcpp.vscode.MakeTmLanguage;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.tokens.Regexes;

public class Main {
	public static void regexTest() {
		//success
		String s="---+***%/-++";
		
		Matcher m=Regexes.AFLOP.matcher(s);
		int index=0;
		while (index<s.length()) {
			m.region(index, m.regionEnd());
			if (!m.lookingAt()) {
				System.out.println("unrecog token");
				return;
			}
			System.out.println(m.group());
			if (m.pattern() == Regexes.AFLOP && m.group(1)!=null) {
				m.usePattern(Regexes.MFLOP);
			}if (m.pattern() == Regexes.MFLOP && m.group(2)!=null) {
				m.usePattern(Regexes.AFLOP);
			}
			index=m.end();
		}
		
	}
	public static void floatFormatTest() {
		double big=Math.pow(7, 10);
		double small=Math.pow(7, -5);
		System.out.printf("%%s : %s , %s ;\n", big,small); //uses sci. not. if it is big
		System.out.printf("%%f : %f , %f ;\n", big,small); // will round to 6 places
		/*
		 * USE %s; SCI NOTATION IS NOT A PROBLEM; I TESTED THIS IN MC; THE VSCODE PLUGIN GETS IT WRONG
		 */
		int i=3;
		Integer itg=3;
		Double dbl=(double) 3;
		Number ni=true?(long) 3.0:3.0;
		Number nd=(double) 3;
		System.out.printf("%%s : %s , %s, %s, %s, %s ;\n", i,itg,dbl,ni,nd); //uses sci. not. if it is big
		
		Byte b=1;
		Short s=1;
		Integer in=1;
		Long l=1l;
		Float f=1f;
		Double d=1d;
		System.out.printf("%%s : %s , %s, %s, %s; %s %s ;\n", b,s,in,l,f,d); //uses sci. not. if it is big
		//%s does not show the type letter
		
		
		
	}
	public static boolean compileStdLib() {
		CompileJob stdlib=new CompileJob(CompileJob.getResources(),CompileJob.getGeneratedResources());
		stdlib.setRootHeaderOut(CompileJob.getGeneratedResources());
		stdlib.stdLib();
		//stdlib.debugMode();
		boolean success=stdlib.compileAll();
		System.out.println("successfully compiled std library;");
		return success;
	}
	public static void main(String[] args) {
		//the project directory is the datapack level (same as pack.mcmeta)
		//args
		CompileJob job=new CompileJob();
		boolean compStd=false;
		boolean makevscode=false;
		boolean skipMainCompile=false;
		String vscode_grammar_path = null;
		for(int i=0;i<args.length;i++) {
			String arg=args[i];
			if(arg.equals("-o") && i+1<args.length) {
				i++;
				arg=args[i];
				File f=new File(arg);
				if(f.exists()) {
					System.out.printf("set output dir to: %s\n", arg);
					job.setRootDatapack(f.toPath());
				}else {
					System.err.printf("could not find output dir: %s\n", arg);
					return;
				}
			}
			if(arg.equals("-src") && i+1<args.length) {
				i++;
				arg=args[i];
				File f=new File(arg);
				if(f.exists()) {
					System.out.printf("set src dir to: %s\n", arg);
					job.setRootSrc(f.toPath());
				}else {
					System.err.printf("could not find src dir: %s\n", arg);
					return;
				}
			}
			if(arg.equals("-h") && i+1<args.length) {
				i++;
				arg=args[i];
				File f=new File(arg);
				if(f.exists()) {
					System.out.printf("set header out dir to: %s\n", arg);
					job.setRootHeaderOut(f.toPath());
				}else {
					System.err.printf("could not find header out dir: %s\n", arg);
					return;
				}
			}
			if(arg.equals("-I") && i+1<args.length) {
				i++;
				arg=args[i];
				File f=new File(arg);
				if(f.exists()) {
					System.out.printf("added include dir: %s\n", arg);
					job.includePath(f.toPath());
				}else {
					System.err.printf("could not find include dir: %s\n", arg);
					return;
				}
			}
			if(arg.equals("-L") && i+1<args.length) {
				i++;
				arg=args[i];
				File f=new File(arg);
				if(f.exists()) {
					System.out.printf("added linking dir: %s\n", arg);
					job.addLink(f.toPath());
				}else {
					System.err.printf("could not find include dir: %s\n", arg);
					return;
				}
			}
			if(arg.equals("-std")) {
				compStd=true;
			}

			if(arg.equals("--std")) {
				compStd=true;
				skipMainCompile=true;
				break;
			}

			if(arg.equals("-g")) {
				job.debugMode();
			}
			if(arg.equals("--vscode")) {
				makevscode=true;
				skipMainCompile=true;
				if(i+1<args.length) {
					i++;
					vscode_grammar_path=args[i];
					
				}
				break;
			}
		}
		boolean stdSuccess=true;
		if(compStd) stdSuccess=compileStdLib();
		if(makevscode) MakeTmLanguage.make(vscode_grammar_path);
		if(skipMainCompile)return;
		job.compileAll();
		if(!stdSuccess)System.err.println("compilation of stdlib failed;");
		//floatFormatTest();
		
		//C:\Users\jbarb_t8a3esk\AppData\Roaming\.minecraft\saves\Mcpp_test_world\datapacks\
	}
}
