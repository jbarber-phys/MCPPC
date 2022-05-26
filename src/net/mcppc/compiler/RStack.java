package net.mcppc.compiler;

import java.io.PrintStream;
import java.util.NavigableMap;
import java.util.TreeMap;

import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.PrintF;

public class RStack {
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
		if(this.vartypes.isEmpty()) {
			if(this.maxSizeEver<numReg)this.maxSizeEver=numReg;
			return 0;
		}
		if(this.maxSizeEver<this.getTop()+this.getVarType(this.getTop()).sizeOf()+numReg)
			this.maxSizeEver=this.getTop()+this.getVarType(this.getTop()).sizeOf()+numReg;
		return this.getExtra();
	}
	public void estmiate(int reg,Number est) {
		this.regvarEstimates.put(reg, est);
	}
	public int setNext(VarType v) throws CompileError {
		if(v.sizeOf()<=0)throw new CompileError.CannotStack(v);
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
		this.finish(ns);
	}
	public void finish(Namespace ns) {
		ns.fillMaxRegisters(this.maxSizeEver);
	}
	public void castRegister(PrintStream p,Scope s,int index,VarType newType) throws CompileError {
		VarType oldType=this.getVarType(index);
		this.castRegisterValue(p,s, index, oldType, newType);
		this.setVarType(index, newType);
		//this.vartypes.put(index, newType);
	}
	private void castRegisterValue(PrintStream p,Scope s,int index,VarType oldType,VarType newType) throws CompileError {
		if(oldType.equals(newType))return;//skip cast
		if(oldType.isStruct() || newType.isStruct()) {
			int oldSize = oldType.sizeOf();
			int newSize = newType.sizeOf();
			if(newSize>oldSize) {
				//grow
				this.pad(p, index, oldSize, newSize);
			}
			if(newType.isStruct() && newType.struct.canCasteFrom(oldType,newType))
				newType.struct.castRegistersFrom(p, s, this, index, oldType, newType);
			else if(oldType.isStruct()  && oldType.struct.canCasteTo(newType,oldType))
				newType.struct.castRegistersTo(p,  s, this, index, newType, oldType);
			else throw new CompileError.UnsupportedCast(newType, oldType);
			if(newSize<oldSize) {
				//shrink
				this.pad(p, index, oldSize, newSize);
			}
			
		}else {
			if (oldType.isNumeric() ^ newType.isNumeric())throw new CompileError.UnsupportedCast(newType, oldType);
			else if (oldType.isFloatP() || newType.isFloatP()) {
				int dp=newType.getPrecision(s)-oldType.getPrecision(s);
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
		//this.vartypes.put(index, newType);
		this.setVarType(index, newType);
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
	public void pad(PrintStream p,int start, int oldSize, int newSize) throws CompileError {
		if(newSize==oldSize)return;
		if(this.vartypes.isEmpty() || this.vartypes.lastKey()<=start)return;
		if(newSize>oldSize) {
			int i=this.vartypes.lastKey();
			int ds=newSize-oldSize;
			while(i>=start+oldSize) {
				int h1=i+ds;
				int h2=i;
				this.move(p, h1, h2);
				if(this.vartypes.subMap(0, i).isEmpty())break;
				i=this.vartypes.subMap(0, i).lastKey();
			}
		}else if(newSize<oldSize) {
			int i=this.vartypes.subMap(start+oldSize, this.vartypes.lastKey()).firstKey();
			int ds=newSize-oldSize;
			while(i>=start+oldSize) {
				int h1=i+ds;
				int h2=i;
				this.move(p, h1, h2);
				if(this.vartypes.subMap(i+1, this.vartypes.lastKey()).isEmpty())break;
				i=this.vartypes.subMap(i+1, this.vartypes.lastKey()).firstKey();
			}
		}
	}
	public String getTempRamInCmd() {
		return getTempRamInCmd(0);
	}
	public String getTempRamInCmd(int index) {
		return "%s %s%d".formatted(this.res,"\"$tempram\".ram",index);
	}
	public void setVarType(Integer dest, VarType otype) {
		if(otype!=null) {
			//boolean strace=this.vartypes.get(dest)==null?false:this.vartypes.get(dest).isStruct();
			this.vartypes.put(dest, otype);
			//System.err.printf("setVarType ( %s , %s ) ;\n", dest,otype.headerString());
			//if(strace)Thread.dumpStack();
			//auto clear members
			if(this.vartypes.isEmpty())return;
			int size=otype.sizeOf();
			for(int i=dest+1;i<size+dest;i++)this.vartypes.remove(i);
		}
		else this.vartypes.remove(dest);
	}
	public Number getEstimate(Integer reg) {
		return this.regvarEstimates.get(reg);
	}
	public int size() {
		if(this.vartypes.isEmpty()) return 0;
		return this.getTop()+this.getVarType(this.getTop()).sizeOf();
	}
	public void debugOut(PrintStream p) {
		p.printf("%s, \n %s ;\n", this.vartypes,this.regvarEstimates);
	}
	public void printTypes(PrintStream p,int start,int stop) {
		int i=start;
		p.printf("stack types: ");
		while(i<=stop) {
			VarType tp=this.getVarType(i);
			p.printf("%d::%s, ", i,tp==null?"null":tp.asString());
			i+=tp==null?1:tp.sizeOf();
		}
		p.printf("\n");
	}

	public void runtimeOutShow(PrintStream p,int start,int stop) throws CompileError {
		runtimeOutShow(p,start,stop,PrintF.stdwarn);
	}
	public void runtimeOutShow(PrintStream p,int start,int stop, PrintF out) throws CompileError {
		Register[] rg=new Register[stop-start+1];
		String[] sfs=new String[stop-start+1];
		for(int i=0;i<stop-start+1;i++) {
			rg[i]=this.getRegister(start+i);
			sfs[i]="%d::%%s".formatted(start+i);
		}
		String fmat='"'+String.join(" , ",sfs)+'"';
		out.printf(p, Selector.AT_A, fmat, rg);
	}
}