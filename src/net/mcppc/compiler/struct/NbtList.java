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
import net.mcppc.compiler.INbtValueProvider.Macro;
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
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.target.Version;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Token;
/**
 * an ordered, size-changable collection;
 * note that for non-const index lookup, mcpp must loop through the list at linear in size time;
 * macros could speed this up in the future (TODO)
 * @author RadiumE13
 *
 */
public class NbtList extends NbtCollection{
	public static final boolean ALLOW_MACROS = true;//TODO set to true and test
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
				ChangeAt.insert.name,ChangeAt.insert,
				RemoveAt.remove.name,RemoveAt.remove
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
		public VarType getRetType(BFCallToken token, Scope s) {
			return NbtList.myMembType(token.getThisBound().type);
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, s, matcher, line, col, stack);
			if(args.nargs()!=1) throw new CompileError("wrong number of args in List.get(index); expected 1 but got %d;".formatted(args.nargs()));
			return args;
		}
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			Equation index = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Variable self = token.getThisBound();
			index.constify(c, s);
			if(index.isNumber()) {
				int i = index.getNumber().intValue();

				//ell = self.indexMyNBTPath(i); //do nothing for now 
				return;
			}
			if(s.getTarget().minVersion.isGreaterOrEqualTo(Version.JAVA_1_20_2) && ALLOW_MACROS) {
				//new call
				Variable listbuff = NbtList.MacroGetSet.macroGet.getListBuff(self.type);
				//Variable get = Macro.macro.getEllBuff(NbtList.myMembType(self.type));
				Variable indexmac = NbtList.MacroGetSet.macroGet.macroTag().fieldMyNBTPath(NbtList.MacroGetSet.INDEX, VarType.INT);
				Variable.directSet(p, s, listbuff, self, stack);
				index.compileOps(p, c, s, VarType.INT);
				index.setVar(p, c, s, indexmac);
				NbtList.MacroGetSet.macroGet.call(p,c,s);
			}else {
				//old call
				Variable listbuff = Loop.loop.getListBuff(token.getThisBound().type);//internal only
				//this.listbuff = Loop.loop.getListBuff(token.getThisBound().type);//internal only
				Variable.directSet(p, s, listbuff, self, stack);
				Loop.loop.setIndex(p,c, s, index);
				Loop.loop.call(p);
			}
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {
			Equation index = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Variable self = token.getThisBound();
			if(index.isNumber()) {

				int i = index.getNumber().intValue();

				Variable ell = self.indexMyNBTPath(i); 
				ell.getMe(p, s, stack, stackstart, typeWanted);
				return;
			}
			NbtList list = (NbtList) token.getThisBound().type.struct;
			Variable listbuff;
			Variable get;
			if(s.getTarget().minVersion.isGreaterOrEqualTo(Version.JAVA_1_20_2) && ALLOW_MACROS) {
				listbuff = NbtList.MacroGetSet.macroGet.getListBuff(self.type);
				get = NbtList.MacroGetSet.macroGet.getEllBuff(NbtList.myMembType(self.type));
			}else {
				//old vars
				listbuff = Loop.loop.getListBuff(token.getThisBound().type);//internal only
				get = list.getIndexRef(listbuff, 0);
			}
			get.getMe(p, s, stack,stackstart, typeWanted);
			listbuff.deallocateLoad(p, s.getTarget());
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			Equation index = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Variable self = token.getThisBound();
			if(index.isNumber()) {

				int i = index.getNumber().intValue();

				Variable ell = self.indexMyNBTPath(i); 
				Variable.directSet(p, s, v, ell, stack);
				return;
			}
			NbtList list = (NbtList) token.getThisBound().type.struct;

			Variable listbuff;//internal only
			Variable get;
			if(s.getTarget().minVersion.isGreaterOrEqualTo(Version.JAVA_1_20_2) && ALLOW_MACROS) {
				listbuff = NbtList.MacroGetSet.macroGet.getListBuff(self.type);
				get = NbtList.MacroGetSet.macroGet.getEllBuff(NbtList.myMembType(self.type));
			}else {
				//old vars
				listbuff = Loop.loop.getListBuff(token.getThisBound().type);
				get = list.getIndexRef(listbuff, 0);
			}
			Variable.directSet(p, s, v, get, stack);
			listbuff.deallocateLoad(p, s.getTarget());
			
		}

		@Override
		public Number getEstimate(BFCallToken token, Scope s) {
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
				index.getMe(p, s, stack, indexhome, VarType.INT);
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
			@Targeted
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
			@Targeted
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
		public VarType getRetType(BFCallToken token, Scope s) {
			return VarType.VOID;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, s, matcher, line, col, stack);
			if(args.nargs()!=2) throw new CompileError("wrong number of args in List.%s(index); expected 1 but got %d;".formatted(this.name,args.nargs()));
			return args;
		}
		@Override
		@Targeted
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {

			Equation index = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Equation value = (Equation) ((BasicArgs)token.getArgs()).arg(1);
			Variable self = token.getThisBound();
			VarType memb = NbtList.myMembType(self.type);
			index.constify(c, s);
			//System.err.printf("index const %s[%s]\n",self.name,index.asString());
			//index.printStatementTree(System.err, 0);
			if(index.isNumber()) {
				Variable valuebuff = Loop.loop(this.isInsert).getValueBuff(memb);
				int i = index.getNumber().intValue();
				//System.err.printf("index const %s[%d]\n",self.name,i);
				if(this.isInsert) {
					boolean macros = valuebuff.hasMacro();
					value.compileOps(p, c, s, memb);
					value.setVar(p, c, s, valuebuff);
					if(macros) {
						VTarget.requireTarget(VTarget.after(Version.JAVA_1_20_2), s.getTarget(), this.name, c);
						p.print("$");
					}
					p.printf("data modify %s insert %d %s\n", self.dataPhrase(),i,valuebuff.fromCMDStatement(s.getTarget()));
				}else {
					value.compileOps(p, c, s, memb);
					Variable ell = self.indexMyNBTPath(i);
					value.setVar(p, c, s, ell);
				}
				return;
			}
			if(s.getTarget().minVersion.isGreaterOrEqualTo(Version.JAVA_1_20_2) && ALLOW_MACROS) {
				//new call
				MacroGetSet gen = this.isInsert ? MacroGetSet.macroInsert : MacroGetSet.macroSet;
				Variable valuebuff = gen.getEllBuff(memb);
				Variable listbuff = gen.getListBuff(self.type);
				//Variable get = Macro.macro.getEllBuff(NbtList.myMembType(self.type));
				Variable indexmac = gen.macroTag().fieldMyNBTPath(NbtList.MacroGetSet.INDEX, VarType.INT);
				Variable.directSet(p, s, listbuff, self, stack);
				index.compileOps(p, c, s, VarType.INT);
				index.setVar(p, c, s, indexmac);
				value.compileOps(p, c, s, memb);
				value.setVar(p, c, s, valuebuff);
				gen.call(p,c,s);
				Variable.directSet(p, s, self, listbuff, stack);
			}else {
				Loop loop = Loop.loop(this.isInsert);
				Variable valuebuff = loop.getValueBuff(memb);
				Variable listbuff1= Loop.loop(this.isInsert).getListBuff1(self.type);
				Variable listbuff2 = Loop.loop(this.isInsert).getListBuff2(self.type);
				Variable.directSet(p, s, listbuff1, self, stack);
				listbuff2.allocateLoad(p,s.getTarget(), true);
				loop.setIndex(p,c, s, index);
				value.compileOps(p, c, s, memb);
				value.setVar(p, c, s, valuebuff);
				loop.call(p, s);
				Variable.directSet(p, s, self, listbuff2, stack);
			}
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {
			//nothing
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			//nothing
			
		}

		@Override
		public Number getEstimate(BFCallToken token, Scope s) {
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
				index.getMe(p, s, stack, indexhome, VarType.INT);
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
			@Targeted
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
							listbuff2.dataPhrase(),value.fromCMDStatement(ns.target)
							);//append inserted value

				}else {
					p.printf("execute if score %s matches 0 run "+
							"data modify %s set %s\n"
							,idxr.inCMD(),
							first.dataPhrase(),value.fromCMDStatement(ns.target)
							);//set first slot value
				}
				p.printf("data modify %s append from %s\n",listbuff2.dataPhrase(), first.dataPhrase());//remove first index tag
				p.printf("data remove %s\n", first.dataPhrase());//remove first index tag
				idxr.decrement(p, 1);
				p.printf("execute if data %s run function %s\n",first.dataPhrase(), this.res);//remove first index tag
				stack.finish(job);
				
			}
			private boolean registered=false;
			@Targeted
			public void call(PrintStream p, Scope s) throws CompileError {
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
		}
	}
	public static class RemoveAt extends BuiltinFunction {
		public static final RemoveAt remove = new RemoveAt("remove");
		public RemoveAt(String name) {
			super(name);
		}
		@Override public boolean isNonstaticMember() {return true;}

		@Override
		public VarType getRetType(BFCallToken token, Scope s) {
			return VarType.VOID;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			
			BasicArgs args = super.tokenizeArgsEquations(c, s, matcher, line, col, stack);
			if(args.nargs()!=1) throw new CompileError("wrong number of args in List.%s(index); expected 1 but got %d;".formatted(this.name,args.nargs()));
			return args;
		}
		@Override
		@Targeted
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {

			Equation index = (Equation) ((BasicArgs)token.getArgs()).arg(0);
			Variable self = token.getThisBound();
			VarType memb = NbtList.myMembType(self.type);
			index.constify(c, s);
			//System.err.printf("index const %s[%s]\n",self.name,index.asString());
			//index.printStatementTree(System.err, 0);
			if(index.isNumber()) {
				int i = index.getNumber().intValue();
				//System.err.printf("index const %s[%d]\n",self.name,i);
				Variable ell = self.indexMyNBTPath(i);
				ell.basicdeallocateBoth(p, s.getTarget(), false);
				return;
			}
			if(s.getTarget().minVersion.isGreaterOrEqualTo(Version.JAVA_1_20_2) && ALLOW_MACROS) {
				//new call
				MacroGetSet gen = MacroGetSet.macroRemove;
				Variable listbuff = gen.getListBuff(self.type);
				//Variable get = Macro.macro.getEllBuff(NbtList.myMembType(self.type));
				Variable indexmac = gen.macroTag().fieldMyNBTPath(NbtList.MacroGetSet.INDEX, VarType.INT);
				Variable.directSet(p, s, listbuff, self, stack);
				index.compileOps(p, c, s, VarType.INT);
				index.setVar(p, c, s, indexmac);
				gen.call(p,c,s);
				Variable.directSet(p, s, self, listbuff, stack);
			}else {
				Loop loop = Loop.removeloop;
				Variable listbuff1= loop.getListBuff1(self.type);
				Variable listbuff2 = loop.getListBuff2(self.type);
				Variable.directSet(p, s, listbuff1, self, stack);
				listbuff2.allocateLoad(p,s.getTarget(), true);
				loop.setIndex(p,c, s, index);
				loop.call(p, s);
				Variable.directSet(p, s, self, listbuff2, stack);
			}
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {
			//nothing
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
				throws CompileError {
			//nothing
			
		}

		@Override
		public Number getEstimate(BFCallToken token, Scope s) {
			return null;
		}
		private static final class Loop extends CodeGenerator {
			static Loop removeloop = new Loop(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"list/removeloop"));
			public Loop(ResourceLocation res) {
				super(res);
			}
			public Variable getListBuff1(VarType type) {
				return new Variable("\"$listbuff1\"",type,null,this.res);
			}
			public Variable getListBuff2(VarType type) {
				return new Variable("\"$listbuff2\"",type,null,this.res);
			}

			public void setIndex(PrintStream p,Scope s, Variable index) throws CompileError {
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				index.getMe(p, s, stack, indexhome, VarType.INT);
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
			@Targeted
			public void build(PrintStream p, CompileJob job, Namespace ns) throws CompileError {
				RStack stack = new RStack(this.res);
				int indexhome = stack.setNext(VarType.INT);
				VarType elltype = VarType.BOOL;//internal only
				VarType listtype = list.listOf(elltype);//internal only
				Register idxr = stack.getRegister(indexhome);
				Variable listbuff1 = this.getListBuff1(listtype);//internal only
				Variable listbuff2 = this.getListBuff2(listtype);
				Variable first = listbuff1.indexMyNBTPath(0);
				p.printf("execute unless score %s matches 0 run "+
						"data modify %s append from %s\n"
						,idxr.inCMD(),
						listbuff2.dataPhrase(), first.dataPhrase()
						);//append unless match

				p.printf("data remove %s\n", first.dataPhrase());//remove first index tag
				idxr.decrement(p, 1);
				p.printf("execute if data %s run function %s\n",first.dataPhrase(), this.res);//remove first index tag
				stack.finish(job);
				
			}
			private boolean registered=false;
			@Targeted
			public void call(PrintStream p, Scope s) throws CompileError {
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
		}
	}
	static final class MacroGetSet extends CodeGenerator {
		private static final int GET = 0;
		private static final int SET = 1;
		private static final int INSERT = 2;
		private static final int REMOVE = 3;
		public static final MacroGetSet macroGet = new MacroGetSet(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"list/getatmacro"),GET);
		public static final MacroGetSet macroSet = new MacroGetSet(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"list/setatmacro"),SET);
		public static final MacroGetSet macroInsert = new MacroGetSet(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"list/insertatmacro"),INSERT);
		public static final MacroGetSet macroRemove = new MacroGetSet(new ResourceLocation(CompileJob.STDLIB_NAMESPACE,"list/removeatmacro"),REMOVE);
		public static final String INDEX = "index";
		private final int action;
		public MacroGetSet(ResourceLocation res,int action) {
			super(res, true);
			this.action=action;
		}
	
		public Variable getListBuff(VarType type) {
			return new Variable("\"$listbuff\"",type,null,this.res);
		}
		public Variable getEllBuff(VarType type) {
			return new Variable("\"$ellbuff\"",type,null,this.res);
		}
		@Override
		public void build(PrintStream p, CompileJob job, Namespace ns) throws CompileError {
			Variable ellbuff = this.getEllBuff(VarType.BOOL);//internal list.listOf(VarType.BOOL)
			Variable listbuff = this.getListBuff(list.listOf(VarType.BOOL));//internal
			Macro idx = new Macro(INDEX,VarType.INT);
			Variable at = listbuff.indexMyNBTPathBasic(idx, VarType.BOOL);
			switch(this.action) {
			case GET:Variable.trueDirectSetBasicNbt(p, job.getTarget(), ellbuff, at);break;
			case SET:Variable.trueDirectSetBasicNbt(p, job.getTarget(), at, ellbuff);break;
			case INSERT: p.printf("$data modify %s insert %s %s\n", listbuff.dataPhrase(),idx.getMacroString(),ellbuff.fromCMDStatement(job.getTarget())); break;
			case REMOVE: at.basicdeallocateBoth(p, job.getTarget(),false); break;
			}
		}
		private boolean registered = false;
		public void call(PrintStream p,Compiler c,Scope s) {
			p.println(this.getCall());
			//self register
			if(!this.registered) {
				CodeGenerator.register(this);this.registered=true;
			}
		}
		
	}
}
