package net.mcppc.compiler;

import java.io.PrintStream;

import net.mcppc.compiler.PrintF;
import net.mcppc.compiler.errors.CompileError;

public class Register implements Comparable<Register>,PrintF.IPrintable{
	/**
	 * constant equal to the number of bits in an mc score; currently is 32 (sad);
	 */
	public static final int SCORE_BITS = 32;
	public static final double score_max(int guardbits) {
		return Math.pow(2, SCORE_BITS-1-guardbits);
	}
	
	final String holder;
	final int index;
	final Number estimate;//read only
	public Register(ResourceLocation res, int index,Number est) {
		holder=res.toString();
		this.index=index;
		this.estimate=est;
	}
	public Register(ResourceLocation res, Function f, int index,Number estimate) {
		if(f==null)holder=res.toString();
		else holder="%s.%s".formatted(res.toString(),f.name);
		this.index=index;
		this.estimate=estimate;
	}
	public String inCMD() {
		return "%s %s".formatted(holder,getScoreAt(index));
	}
	public String testMeInCMD() {
		return "score %s matches 1..".formatted(this.inCMD());
	}
	public static String getScoreAt(int index) {
		return "MCPP_REGISTER_%d".formatted(index);
	}
	public static void createAll(PrintStream p,int max) {
		for(int i=0;i<=max;i++) {
			p.printf("scoreboard objectives add %s dummy\n", getScoreAt(i));
		}
	}
	public void setValue(PrintStream p,long value) {
		p.printf("scoreboard players set %s %d\n", this.inCMD(),value);
	}
	public void setValue(PrintStream p,boolean value) {
		p.printf("scoreboard players set %s %d\n", this.inCMD(),value?1:0);
	}
	public String setValueStr(long value) {
		return "scoreboard players set %s %d".formatted(this.inCMD(),value);
	}
	public String setValueStr(boolean value) {
		return "scoreboard players set %s %d".formatted(this.inCMD(),value?1:0);
	}
	public void setValue(PrintStream p,Scope s,Number value,VarType type) throws CompileError {
		long val=(long) (value.doubleValue()*Math.pow(10, type.getPrecision(s)));
		p.printf("scoreboard players set %s %d\n", this.inCMD(),val);
	}
	public void increment(PrintStream p,long ammount) {
		p.printf("scoreboard players add %s %d\n", this.inCMD(),ammount);
	}
	public void decrement(PrintStream p, long ammount) {
		p.printf("scoreboard players remove %s %d\n", this.inCMD(),ammount);
	}
	public void operation(PrintStream p,String op,Register other) {
		p.printf("scoreboard players operation %s %s %s\n", this.inCMD(),op,other.inCMD());
	}
	public String compare(String op,Register other) {
		return "score %s %s %s".formatted(this.inCMD(),op,other.inCMD());
	}
	public void multByFloatUsingRam(PrintStream p,RStack stack,double mult) {
		p.printf("execute store result storage %s double %s run scoreboard players get %s\n", stack.getTempRamInCmd(),CMath.getMultiplierFor(mult),this.inCMD());
		p.printf("execute store result score %s run data get storage %s 1\n", this.inCMD(),stack.getTempRamInCmd());
	}
	/**
	 * does not account for types - that must be done manual
	 */
	public void multByFloatUsingRamToRaw(PrintStream p,RStack stack,double mult,Register dest) {
		p.printf("execute store result storage %s double %s run scoreboard players get %s\n", stack.getTempRamInCmd(),CMath.getMultiplierFor(mult),this.inCMD());
		p.printf("execute store result score %s run data get storage %s 1\n", dest.inCMD(),stack.getTempRamInCmd());
	}
	@Override
	public int compareTo(Register o){
		if(this.holder.equals(o.holder)) {
			return this.index-o.index;
		}else throw new ClassCastException("cannot compare registers with different holders: '%s' and '%s'; (not actually a Class caste problem)".formatted(this.holder,o.holder));
	}
	public void setToSuccess(PrintStream p,String cmd) {
		p.printf("execute store success score %s run %s\n", this.inCMD(),cmd);
	}
	public void setToResult(PrintStream p,String cmd) {
		p.printf("execute store result score %s run %s\n", this.inCMD(),cmd);
		
	}
	@Override
	public String getJsonTextSafe() {
		String score="{\"score\": {\"name\": \"%s\", \"objective\": \"%s\"}}".formatted(this.holder,getScoreAt(index));
		return score;
	}
}
