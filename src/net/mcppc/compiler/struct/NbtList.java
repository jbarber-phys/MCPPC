package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.BuiltinFunction.BasicArgs;
import net.mcppc.compiler.CodeGenerator;
import net.mcppc.compiler.CompileJob;
import net.mcppc.compiler.CompileJob.Namespace;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Register;
import net.mcppc.compiler.ResourceLocation;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.StructTypeParams;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.functions.Size;
import net.mcppc.compiler.struct.NbtCollection.Clear;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Token;
/**
 * an ordered, size-changable collection;
 * currently uses a simple list + loops; if you want a big list, make a different struct
 * is currently compadible with other basic Sets
 * TODO test
 * @author jbarb_t8a3esk
 *
 */
public class NbtList extends NbtCollection{

	public static final NbtList list = new NbtList("List");
	public static void registerAll() {
		final Size size = new Size("size");
		final Size isFull = new Size.IsFull("isFull");
		list.METHODS = Map.of(
				"append",EndPend.append, 
				"prepend",EndPend.prepend,
				size.name,size,
				Clear.clear.name,Clear.clear,
				GetAt.get.name,GetAt.get,
				ChangeAt.set.name,ChangeAt.set,
				ChangeAt.insert.name,ChangeAt.insert
				);
		Struct.register(list);
	}
	public NbtList(String name) {
		super(name);
	}
	public VarType listOf(VarType memb) {
		return new VarType(this,new StructTypeParams.MembType(memb));
	}
	@Override
	public boolean canIndexMe(Variable self, int index) throws CompileError {
		return true;
	}
	@Override
	public Variable getIndexRef(Variable self, int index) throws CompileError {
		return self.indexMyNBTPathBasic(index, NbtList.myMembType(self.type));
	}
	@Override
	public Token convertIndexGet(Variable self, Equation index) throws CompileError {
		BuiltinFunction bf = GetAt.get;
		BuiltinFunction.BFCallToken bft = new BuiltinFunction.BFCallToken(index.line, index.col, bf);
		bft.withThis(self);
		BasicArgs args = new BasicArgs();
		args.add(index);
		bft.withArgs(args);
		return bft;
	}
	@Override
	public Token convertIndexSet(Variable self, Equation index, Equation setTo) throws CompileError {
		BuiltinFunction bf = ChangeAt.set;
		BuiltinFunction.BFCallToken bft = new BuiltinFunction.BFCallToken(index.line, index.col, bf);
		bft.withThis(self);
		BasicArgs args = new BasicArgs();
		args.add(index);
		args.add(setTo);
		bft.withArgs(args);
		return bft;
	}
	public static class GetAt extends BuiltinFunction {
		public static final GetAt get = new GetAt("get");
		public GetAt(String name) {
			super(name);
		}

		@Override public boolean isNonstaticMember() {return true;}
		@Override
		public VarType getRetType(BFCallToken token) {
			return NbtList.myMembType(token.getThisBound().type);
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, matcher, line, col, stack);
			if(args.nargs()!=1) throw new CompileError("wrong number of args in List.get(index); expected 1 but got %d;".formatted(args.nargs()));
			return args;
		}
		Variable listbuff = null;
		Variable ell = null;
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			this.listbuff = Loop.loop.getListBuff(token.getThisBound().type);//internal only
			Equation index = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Variable self = token.getThisBound();
			if(index.isNumber()) {
				int i = index.getNumber().intValue();

				ell = self.indexMyNBTPath(i); 
				return;
			}
			Variable.directSet(p, s, listbuff, self, stack);
			Loop.loop.setIndex(p,c, s, index);
			Loop.loop.call(p);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			if(this.ell!=null) {
				ell.getMe(p, s, stack, stackstart);
				return;
			}
			NbtList list = (NbtList) token.getThisBound().type.struct;
			Variable get = list.getIndexRef(this.listbuff, 0);
			get.getMe(p, s, stack,stackstart);
			listbuff.deallocateLoad(p);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			if(this.ell!=null) {
				Variable.directSet(p, s, v, this.ell, stack);
				return;
			}
			NbtList list = (NbtList) token.getThisBound().type.struct;
			Variable get = list.getIndexRef(this.listbuff, 0);
			Variable.directSet(p, s, v, get, stack);
			listbuff.deallocateLoad(p);
			
		}

		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		private static final class Loop extends CodeGenerator {
			static Loop loop = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"list/getloop"));
			public Loop(ResourceLocation res) {
				super(res);
			}
			public Variable getListBuff(VarType type) {
				return new Variable("\"$listbuff\"",type,null,this.res);
			}
			public void setIndex(PrintStream p,Scope s, Variable index) throws CompileError {
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				index.getMe(p, s, stack, indexhome);
			}
			public void setIndex(PrintStream p,Compiler c,Scope s, Equation index) throws CompileError {
				if(index.isConstRefable()) {
					this.setIndex(p, s, index.getConstVarRef());
					return;
				}
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				Register idxr = stack.getRegister(indexhome);
				index.compileOps(p, c, s, VarType.INT);
				int home = index.setReg(p, c, s, VarType.INT);
				index.transferRegValue(p, home, idxr);
			}
			@Override
			public void build(PrintStream p, CompileJob job, Namespace ns) throws CompileError {
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				VarType listtype = list.listOf(VarType.BOOL);//internal only
				Register idxr = stack.getRegister(indexhome);
				Variable listbuff = this.getListBuff(listtype);
				String listDat = listbuff.dataPhrase();
				Variable first = listbuff.indexMyNBTPath(0);
				p.printf("data remove %s\n", first.dataPhrase());//remove first index tag
				idxr.decrement(p, 1);
				p.printf("execute if score %s matches 1.. if data %s run function %s\n",idxr.inCMD(),first.dataPhrase(), this.res);//remove first index tag
				stack.finish(job);
				
			}
			private boolean registered=false;
			public void call(PrintStream p) throws CompileError {
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				VarType listtype = list.listOf(VarType.BOOL);//internal only
				Register idxr = stack.getRegister(indexhome);
				Variable listbuff = this.getListBuff(listtype);
				Variable first = listbuff.indexMyNBTPath(0);
				int len = stack.reserve(1);
				Register lenr = stack.getRegister(len);
				Size.lengthOf(p, lenr, listbuff);
				p.printf("execute if score %s matches ..-1 run scoreboard players operation %s %%= %s\n", idxr.inCMD(),idxr.inCMD(),lenr.inCMD());//negative index
				p.printf("execute if score %s matches 1.. if data %s run function %s\n",idxr.inCMD(),first.dataPhrase(), this.res);//remove first index tag

				//self register
				if(!this.registered) {
					CodeGenerator.register(this);this.registered=true;
				}
			}
		};
	}
	public static class ChangeAt extends BuiltinFunction {
		public static final ChangeAt set = new ChangeAt("set",false);
		public static final ChangeAt insert = new ChangeAt("insert",true);
		private final boolean isInsert;
		public ChangeAt(String name,boolean insert) {
			super(name);
			this.isInsert=insert;
		}
		@Override public boolean isNonstaticMember() {return true;}

		@Override
		public VarType getRetType(BFCallToken token) {
			return VarType.VOID;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, matcher, line, col, stack);
			if(args.nargs()!=2) throw new CompileError("wrong number of args in List.%s(index); expected 1 but got %d;".formatted(this.name,args.nargs()));
			return args;
		}
		Variable listbuff1 = null;
		Variable listbuff2 = null;
		Variable valuebuff = null;
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {

			Equation index = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Equation value = (Equation) ((BasicArgs)token.getArgs()).arg(1);
			Variable self = token.getThisBound();
			VarType memb = NbtList.myMembType(self.type);
			this.valuebuff = Loop.loop(this.isInsert).getValueBuff(memb);
			//System.err.printf("index const %s[%s]\n",self.name,index.asString());
			index.printStatementTree(System.err, 0);
			if(index.isNumber()) {
				int i = index.getNumber().intValue();
				//System.err.printf("index const %s[%d]\n",self.name,i);
				if(this.isInsert) {
					value.compileOps(p, c, s, memb);
					value.setVar(p, c, s, this.valuebuff);
					p.printf("data modify %s insert %d %s\n", self.dataPhrase(),i,this.valuebuff.fromCMDStatement());
				}else {
					value.compileOps(p, c, s, memb);
					Variable ell = self.indexMyNBTPath(i);
					value.setVar(p, c, s, ell);
				}
				return;
			}
			Loop loop = Loop.loop(this.isInsert);
			this.listbuff1 = Loop.loop(this.isInsert).getListBuff1(self.type);
			this.listbuff2 = Loop.loop(this.isInsert).getListBuff2(self.type);
			Variable.directSet(p, s, listbuff1, self, stack);
			this.listbuff2.allocateLoad(p, true);
			loop.setIndex(p,c, s, index);
			value.compileOps(p, c, s, memb);
			value.setVar(p, c, s, this.valuebuff);
			loop.call(p);
			Variable.directSet(p, s, self, this.listbuff2, stack);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart)
				throws CompileError {
			//nothing
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			//nothing
			
		}

		@Override
		public Number getEstimate(BFCallToken token) {
			return null;
		}
		private static final class Loop extends CodeGenerator {
			static Loop setloop = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"list/setloop"),false);
			static Loop insertloop = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"list/insertloop"),true);
			public static Loop loop(boolean insert) {
				return insert? insertloop:setloop;
			}
			private final boolean insert;
			public Loop(ResourceLocation res,boolean insert) {
				super(res);
				this.insert=insert;
			}
			public Variable getListBuff1(VarType type) {
				return new Variable("\"$listbuff1\"",type,null,this.res);
			}
			public Variable getListBuff2(VarType type) {
				return new Variable("\"$listbuff2\"",type,null,this.res);
			}

			public Variable getValueBuff(VarType type) {
				return new Variable("\"$value\"",type,null,this.res);
			}
			public void setIndex(PrintStream p,Scope s, Variable index) throws CompileError {
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				index.getMe(p, s, stack, indexhome);
			}
			public void setIndex(PrintStream p,Compiler c,Scope s, Equation index) throws CompileError {
				if(index.isConstRefable()) {
					this.setIndex(p, s, index.getConstVarRef());
					return;
				}
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				index.compileOps(p, c, s, VarType.INT);
				int home = index.setReg(p, c, s, VarType.INT);
				
			}
			@Override
			public void build(PrintStream p, CompileJob job, Namespace ns) throws CompileError {
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				VarType elltype = VarType.BOOL;//internal only
				VarType listtype = list.listOf(elltype);//internal only
				Register idxr = stack.getRegister(indexhome);
				Variable listbuff1 = this.getListBuff1(listtype);//internal only
				Variable listbuff2 = this.getListBuff2(listtype);
				Variable value = this.getValueBuff(elltype);
				Variable first = listbuff1.indexMyNBTPath(0);
				if(this.insert) {
					p.printf("execute if score %s matches 0 run "+
							"data modify %s append %s\n"
							,idxr.inCMD(),
							listbuff2.dataPhrase(),value.fromCMDStatement()
							);//append inserted value

				}else {
					p.printf("execute if score %s matches 0 run "+
							"data modify %s set %s\n"
							,idxr.inCMD(),
							first.dataPhrase(),value.fromCMDStatement()
							);//set first slot value
				}
				p.printf("data modify %s append from %s\n",listbuff2.dataPhrase(), first.dataPhrase());//remove first index tag
				p.printf("data remove %s\n", first.dataPhrase());//remove first index tag
				idxr.decrement(p, 1);
				p.printf("execute if data %s run function %s\n",first.dataPhrase(), this.res);//remove first index tag
				stack.finish(job);
				
			}
			private boolean registered=false;
			public void call(PrintStream p) throws CompileError {
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				Register idxr = stack.getRegister(indexhome);
				VarType listtype = list.listOf(VarType.BOOL);//internal only
				Variable listbuff1 = this.getListBuff1(listtype);//internal only
				Variable listbuff2 = this.getListBuff2(listtype);
				Variable first = listbuff1.indexMyNBTPath(0);
				int len = stack.reserve(1);
				Register lenr = stack.getRegister(len);
				Size.lengthOf(p, lenr, listbuff1);
				p.printf("execute if score %s matches ..-1 run scoreboard players operation %s %%= %s\n", idxr.inCMD(),idxr.inCMD(),lenr.inCMD());//negative index
				p.printf("execute if data %s run function %s\n",first.dataPhrase(), this.res);//remove first index tag
				//self register
				if(!this.registered) {
					CodeGenerator.register(this);this.registered=true;
				}
			}
		};
	}
}
