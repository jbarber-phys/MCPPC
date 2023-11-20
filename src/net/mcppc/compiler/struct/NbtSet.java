package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.CodeGenerator;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.StructTypeParams;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.Size;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.Equation;

public class NbtSet  extends NbtCollection{
	public static final NbtSet set = new NbtSet("Set");
	public NbtSet(String name) {
		super(name);
	}
	public static void registerAll() {
		final Size size = new Size("size");
		final Size isFull = new Size.IsFull("isFull");
		set.METHODS = Map.of(
				size.name,size,
				isFull.name,isFull,
				Ell.add.name,Ell.add,
				Ell.remove.name,Ell.remove,
				Ell.has.name,Ell.has,
				Clear.clear.name,Clear.clear
				);
		Struct.register(set);
	}
	public VarType setOf(VarType memb) {
		return new VarType(this,new StructTypeParams.MembType(memb));
	}
	public static class Ell extends BuiltinFunction {
		public static final Ell add = new Ell("add",+1);
		public static final Ell remove = new Ell("remove",-1);
		public static final Ell has = new Ell("has",0);
		private final int addition;
		public static BuiltinFunction.BFCallToken getAdder(Variable self,Equation edd) {
			BuiltinFunction.BFCallToken bft=new BuiltinFunction.BFCallToken(edd.line, edd.col, Ell.add);
			bft.withThis(self);
			bft.withArgs(fromEquations(edd));
			return bft;
		}
		public Ell(String name,int add) {
			super(name);
			this.addition=add;
		}
		@Override public boolean isNonstaticMember() {return true;}

		@Override
		public VarType getRetType(BFCallToken token, Scope s) {
			return this.addition ==0 ? VarType.BOOL : VarType.VOID;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, s, matcher, line, col, stack);
			if(args.nargs()!=1) throw new CompileError("wrong number of args in Set.%s(value); expected 1 but got %d;".formatted(this.name,args.nargs()));
			return args;
		}
		Variable setbuff1 = null;
		Variable setbuff2 = null;
		Variable valuebuff = null;
		@Override
		@Targeted
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {

			Equation value = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Variable self = token.getThisBound();
			VarType memb = NbtList.myMembType(self.type);
			Loop loop = Loop.loop(addition);
			this.valuebuff = loop.getValueBuff(memb);
			this.setbuff1 = loop.getSetBuff1(self.type);
			this.setbuff2 = loop.getSetBuff2(self.type);
			Variable.directSet(p, s,setbuff1, self, stack);
			this.setbuff2.allocateLoad(p,s.getTarget(), true);
			value.compileOps(p, c, s, memb);
			value.setVar(p, c, s, this.valuebuff);
			loop.call(p);
			switch(this.addition) {
			case +1: p.printf("data modify %s prepend from %s\n",setbuff2.dataPhrase(), this.valuebuff.dataPhrase()); break;
			case 0: break; //let loop handle it
			}
			Variable.directSet(p, s, self, this.setbuff2, stack);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {
			if(this.addition !=0) return;
			Loop loop = Loop.loop(addition);
			Register ret = loop.getRet();
			stack.getRegister(stackstart).operation(p, "=", ret);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			if(this.addition !=0) return;
			Loop loop = Loop.loop(addition);
			Register ret = loop.getRet();
			v.setMe(p, s, ret, VarType.BOOL);
		}

		@Override
		public Number getEstimate(BFCallToken token, Scope s) {
			return null;
		}
		private static final class Loop extends CodeGenerator {
			private static Loop loop = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"set/addremloop"),false);
			private static Loop loopret = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"set/testloop"),true);
			public static Loop loop(int add) {
				return add==0?loopret:loop;
			}
			private final boolean ret;
			public Loop(ResourceLocation res,boolean ret) {
				super(res);
				this.ret=ret;
			}
			public Variable getSetBuff1(VarType type) {
				return new Variable("\"$setbuff\"",type,null,this.res);
			}
			public Variable getSetBuff2(VarType type) {
				return new Variable("\"$setbuff2\"",type,null,this.res);
			}

			public Variable getValueBuff(VarType type) {
				return new Variable("\"$value\"",type,null,this.res);
			}
			private Variable getTemp(VarType type) {
				return new Variable("\"$temp\"",type,null,this.res);
			}
			public Register getRet() throws CompileError {
				RStack stack = new RStack(this.res);
				int home = stack.setNext(VarType.INT);
				Register testreg = stack.getRegister(home);
				int home2 = stack.setNext(VarType.INT);
				return stack.getRegister(home2); 
			}
			@Override
			@Targeted
			public void build(PrintStream p, CompileJob job, Namespace ns) throws CompileError {
				RStack stack = new RStack(this.res);
				VarType elltype = VarType.BOOL;//internal only
				VarType settype = set.setOf(elltype);//internal only
				int home = stack.setNext(VarType.INT);
				Register testreg = stack.getRegister(home);
				int home2 = stack.setNext(VarType.INT);
				Register donereg = stack.getRegister(home2);
				Variable listbuff1 = this.getSetBuff1(settype);//internal only
				Variable listbuff2 = this.getSetBuff2(settype);
				Variable value = this.getValueBuff(elltype);
				Variable temp = this.getTemp(elltype);
				Variable first = listbuff1.indexMyNBTPath(0);
				p.printf("data modify %s set from %s\n",temp.dataPhrase(), first.dataPhrase());
				p.printf("execute store success score %s run data modify %s set from %s\n",testreg.inCMD(),temp.dataPhrase(), value.dataPhrase());
				
				if(this.ret)p.printf("execute if score %s matches 0 run scoreboard players set %s 1\n",testreg.inCMD(),donereg.inCMD());
				
				p.printf("execute unless score %s matches 0 run data modify %s append from %s\n",testreg.inCMD(),listbuff2.dataPhrase(), first.dataPhrase());//remove first index tag
				p.printf("data remove %s\n", first.dataPhrase());//remove first index tag
				p.printf("execute if data %s run function %s\n",first.dataPhrase(), this.res);//remove first index tag
				stack.finish(job);
				
			}
			private boolean registered=false;
			@Targeted
			public void call(PrintStream p) throws CompileError {
				RStack stack = new RStack(this.res);
				int home = stack.setNext(VarType.INT);
				Register testreg = stack.getRegister(home);//testreg.setValue(p, 0);
				int home2 = stack.setNext(VarType.INT);
				Register donereg = stack.getRegister(home2);donereg.setValue(p, 0);
				VarType listtype = set.setOf(VarType.BOOL);//internal only
				Variable setbuff1 = this.getSetBuff1(listtype);//internal only
				Variable setbuff2 = this.getSetBuff2(listtype);
				Variable first = setbuff1.indexMyNBTPath(0);
				Variable value = this.getValueBuff(VarType.BOOL);
				p.printf("execute if data %s run function %s\n",first.dataPhrase(), this.res);//remove first index tag
				
				if(this.ret) p.printf("execute if score %s matches 1.. run data modify %s prepend from %s\n",donereg.inCMD(),setbuff2.dataPhrase(), value.dataPhrase());
				//bring queried elements to the top
				//self register
				if(!this.registered) {
					CodeGenerator.register(this);this.registered=true;
				}
			}
		}
	}
	private final SetConstructor setInit = new SetConstructor(this);
	@Override
	public BuiltinConstructor getConstructor(VarType myType) throws CompileError {
		return this.setInit;
	}
	public static class SetConstructor extends BuiltinConstructor{
		public SetConstructor(String name) {
			super(name);
		}
		public SetConstructor(NbtCollection clazz) {
			this(clazz.name);
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line,int col, RStack stack) throws CompileError {
			BasicArgs a=new BuiltinFunction.BasicArgs().equations(c, s, line, col, matcher, stack);
			//any number of args is OK
			return a;
		}

		private static final String NEW= "\"$Collection\".\"$new\"";
		private Variable newobj(Compiler c,BFCallToken tk) {
			Variable v=new Variable(NEW, tk.getStaticType(), null,c.resourcelocation);
			return v;
		}
		@Override
		public void call(PrintStream p, Compiler c, Scope s,  BFCallToken token, RStack stack) throws CompileError {
			//default to storage
			BasicArgs args = (BasicArgs)token.getArgs();
			Variable obj=this.newobj(c,token);
			obj.allocateLoad(p,s.getTarget(), false);//anon must think its loaded
			int size = args.nargs();
			for(int i=0;i<size;i++) {
				//Variable arg=NbtCollection.componentOf(obj, i);
				Equation eq=(Equation) ((BasicArgs)args).arg(i);
				Ell.getAdder(obj, eq).call(p, c, s, stack);
			}
			
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {
			//will throw
			Variable obj=this.newobj(c,token);
			obj.getMe(p,s, stack, stackstart, typeWanted);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			Variable obj=this.newobj(c,token);
			Variable.directSet(p,s, v, obj, stack);
		}

		@Override
		public Number getEstimate(BFCallToken token, Scope s) {
			return null;
		}
		
	}
}
