package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.NavigableMap;
import java.util.TreeMap;

import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;

public class Register implements Comparable<Register>{
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
	public void setValue(PrintStream p,Number value,VarType type) {
		long val=(long) (value.doubleValue()*Math.pow(10, type.getPrecision()));
		p.printf("scoreboard players set %s %d\n", this.inCMD(),val);
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
		p.printf("execute store result score %s run datadata get storage %s 1\n", dest.inCMD(),stack.getTempRamInCmd());
	}
	@Override
	public int compareTo(Register o){
		if(this.holder.equals(o.holder)) {
			return this.index-o.index;
		}else throw new ClassCastException("cannot compare registers with different holders: '%s' and '%s'; (not actually a Class caste problem)".formatted(this.holder,o.holder));
	}
	public static class RStack {
		int maxSizeEver=0;
		final ResourceLocation res;
		final Function f;
		//must be a navagatable map; must be ordered
		NavigableMap<Integer,VarType> vartypes = new TreeMap<Integer,VarType>();
		
		//estimates of current value in registers when converted back to var form;
		NavigableMap<Integer,Number> regvarEstimates = new TreeMap<Integer,Number>();
		public RStack(ResourceLocation r){
			this(r,null);
		}
		public RStack(ResourceLocation r,Scope currentScope){
			this.res=r;
			this.f=currentScope.function;
			
		}
		public RStack(Scope currentScope){
			this.res=currentScope.resBase;
			this.f=currentScope.function;
			
		}
		public Register getRegister(int i) {
			Number est=this.regvarEstimates.get(i);
			Register r=new Register(res,f,i,est);return r;
		}
		public VarType getVarType(int i) {
			return this.vartypes.get(i);
		}
		public int getPrevious(int i) {
			return this.vartypes.floorKey(i-1);
		}
		public int getNext(int i) {
			return this.vartypes.ceilingKey(i+1);
		}
		public int getTop() {
			return this.vartypes.lastKey();
		}
		public int getExtra() {
			return this.vartypes.lastKey()+this.vartypes.lastEntry().getValue().sizeOf();
		}
		//reserves extra registers without type-ing them
		public int reserve(int numReg) {
			if(this.maxSizeEver<this.getTop()+this.getVarType(this.getTop()).sizeOf()+numReg)this.maxSizeEver=this.getTop()+this.getVarType(this.getTop()).sizeOf()+numReg;
			return this.getExtra();
		}
		public void estmiate(int reg,Number est) {
			this.regvarEstimates.put(reg, est);
		}
		public int setNext(VarType v) throws CompileError {
			if(v.sizeOf()<=0)throw new CompileError("cant put type %s on registers;".formatted(v.asString()));
			int max=this.vartypes.isEmpty()?0:this.vartypes.lastKey();
			int s2=this.vartypes.isEmpty()?0:this.vartypes.get(max).sizeOf();
			int s=v.sizeOf();
			this.maxSizeEver = Math.max(this.maxSizeEver, max+s2+s);
			this.vartypes.put(max+s2, v);
			return max+s2;
		}
		public int pop() throws CompileError {
			if(this.vartypes.isEmpty())throw new CompileError("tried to pop from empty stack");
			VarType v=this.vartypes.remove(this.vartypes.lastKey());
			if(this.vartypes.isEmpty()) {
				this.regvarEstimates.clear();
				return -1;//DNE
			}
			int max=this.vartypes.lastKey();
			while((!this.regvarEstimates.isEmpty())&&this.regvarEstimates.lastKey()>max)this.regvarEstimates.remove(this.regvarEstimates.lastKey());
			return max;
		}
		public void cap(int cap) throws CompileError {
			//a safer pop
			while((!this.vartypes.isEmpty())
					&&this.vartypes.lastKey()>cap)this.pop();
			//while(this.regvarEstimates.lastKey()>cap)this.pop();
		}
		public void clear() {
			this.vartypes.clear();
			this.regvarEstimates.clear();
		}
		public void finish(CompileJob job) {
			Namespace ns=job.namespaces.get(this.res.namespace);
			ns.fillMaxRegisters(this.maxSizeEver);
		}
		public void castRegister(PrintStream p,int index,VarType newType) throws CompileError {
			VarType oldType=this.getVarType(index);
			this.castRegisterValue(p, index, oldType, newType);
			this.vartypes.put(index, newType);
		}
		public void castRegisterValue(PrintStream p,int index,VarType oldType,VarType newType) throws CompileError {
			if(oldType.equals(newType))return;//skip cast
			if(oldType.isStruct() || newType.isStruct()) {
				if(newType.isStruct() && newType.struct.canCasteFrom(oldType,newType))
					newType.struct.castRegistersFrom(p, this, index, oldType, newType);
				else if(oldType.isStruct()  && oldType.struct.canCasteTo(newType,oldType))
					newType.struct.castRegistersTo(p,  this, index, newType, oldType);
				else throw new CompileError.UnsupportedCast(newType, oldType);
				
			}else {
				if (oldType.isNumeric() ^ newType.isNumeric())throw new CompileError.UnsupportedCast(newType, oldType);
				else if (oldType.isFloatP() || newType.isFloatP()) {
					int dp=newType.getPrecision()-oldType.getPrecision();
					String op = (dp>0)?"*=":"/=";
					long mult=(long) Math.pow(10, Math.abs(dp));
					if(dp!=0) {
						int extra=this.getExtra();
						Register rh=this.getRegister(index);
						Register re=this.getRegister(extra);
						this.reserve(1);
						re.setValue(p, mult);
						rh.operation(p, op, re);
						//estmate unaffected
					}
				}
			}
			this.vartypes.put(index, newType);
		}
		//caries type with it; does not cast; is copylike (does not modify original)
		public void move(PrintStream p,int dest,int from) throws CompileError {
			if(dest==from)return;
			VarType t2=this.getVarType(from);
			//type is carried with it
			int sz=t2.sizeOf();
			this.move(p, dest, from, sz);
			this.setVarType(dest, t2);
		}
		//does not affect types and also does not cast; is copylike (does not modify original)
		public void move(PrintStream p,int dest,int from,int sz) throws CompileError {
			if(dest==from)return;
			boolean forward=dest<=from;
			for(int i=0;i<sz;i++) {
				int h1=dest+(forward?i:sz-i-1);
				int h2=from+(forward?i:sz-i-1);
				Register r1=this.getRegister(h1);
				Register r2=this.getRegister(h2);
				r1.operation(p, "=", r2);
			}
			this.estmiate(dest, this.getEstimate(from));
		}
		
		public String getTempRamInCmd() {
			return getTempRamInCmd(0);
		}
		public String getTempRamInCmd(int index) {
			return "%s %s%d".formatted(this.res,"\"$tempram\".ram",index);
		}
		public void setVarType(Integer dest, VarType otype) {
			if(otype!=null) {
				this.vartypes.put(dest, otype);
				//auto clear members
				if(this.vartypes.isEmpty())return;
				int size=otype.sizeOf();
				for(int i=dest+1;i<size;i++)this.vartypes.remove(i);
			}
			else this.vartypes.remove(dest);
		}
		public Number getEstimate(Integer reg) {
			return this.regvarEstimates.get(reg);
		}
	}
}
