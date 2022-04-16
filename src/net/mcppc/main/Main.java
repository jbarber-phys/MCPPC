package net.mcppc.main;

import java.util.regex.Matcher;

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
		
	}
	public static void main(String[] args) {
		//the project directory is the datapack level (same as pack.mcmeta)
		CompileJob job=new CompileJob();
		job.compileAll();
		//floatFormatTest();
	}
}
