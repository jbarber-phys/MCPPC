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
	public static void main(String[] args) {
		//the project directory is the datapack level (same as pack.mcmeta)
		CompileJob job=new CompileJob();
		job.compileAll();
	}
}
