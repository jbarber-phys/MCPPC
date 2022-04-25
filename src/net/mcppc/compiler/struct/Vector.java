package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Register.RStack;
import net.mcppc.compiler.StructTypeParams.MembType;
import net.mcppc.compiler.VarType.Builtin;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.Type;
import net.mcppc.compiler.tokens.UnaryOp;
import net.mcppc.compiler.tokens.UnaryOp.UOType;
/**
 * struct for dealing with directions of space; should be mappable onto an NBT array;
 * DIM : cross product will break if it is not 3
 * 
 * 
 * TODO: test operations, set tags, 
 * 
 * @author jbarb_t8a3esk
 *
 */
public class Vector extends Struct {
	public static final Vector vector;
	public static final Vector vec3d;
	public static final Vector vec3i;
	static {
		vector=new Vector("Vector");
		vec3d=new Vector("Vec3d",VarType.DOUBLE);
		vec3i=new Vector("Vec3i",VarType.LONG);
		
	}
	public static void registerAll() {
		Struct.register(vector);
		Struct.register(vec3d);
		Struct.register(vec3i);
	}
	public static VarType vectorOf(VarType memb) {
		if(memb.type==Builtin.DOUBLE)return new VarType(vec3d,new StructTypeParams.MembType(memb));
		if(memb.type==Builtin.LONG)return new VarType(vec3i,new StructTypeParams.MembType(memb));
		if(memb.type==Builtin.INT)return new VarType(vec3i,new StructTypeParams.MembType(memb));
		return new VarType(vector,new StructTypeParams.MembType(memb));
		
	}
	//vector type can be set by type arg or by naming vector subtype
	//if null, must supply args
	//else must not supply args
	public static final int DIM=3;
	public static final String[] DIMNAMES= {"x","y","z"};
	public static final int X=0;
	public static final int Y=1;
	public static final int Z=2;
	public final VarType defaulttype;
	public Vector(String name)  {
		super(name);
		this.defaulttype=null;
	}
	public Vector(String name,VarType dt)  {
		super(name);
		this.defaulttype=dt;
	}
	@Override
	public String getNBTTagType(VarType varType) {
		//should be unused
		return "tag_compound";//?
	}
	@Override
	public StructTypeParams tokenizeTypeArgs(Compiler c, Matcher matcher, int line, int col) throws CompileError {
		if(this.defaulttype==null)
			return StructTypeParams.MembType.tokenizeTypeArgs(c, matcher, line, col);
		else  if (this.defaulttype.isFloatP()){
			//precision
			StructTypeParams.PrecisionType pc=StructTypeParams.PrecisionType.tokenizeTypeArgs(c, matcher, line, col);
			VarType tp=this.defaulttype.withPrecision(pc.precision);
			return new StructTypeParams.MembType(tp);
		}else {
			Type.closeTypeArgs(c, matcher, line, col);
			return new StructTypeParams.MembType(this.defaulttype);
		}
	}
	@Override
	public StructTypeParams paramsWNoArgs() throws CompileError {
		if(this.defaulttype==null) throw new CompileError("struct of type %s needs a member type param".formatted(this.name));
		return new StructTypeParams.MembType(this.defaulttype);
	}
	@Override
	public VarType withPrecision(VarType vt,int newPrecision) throws CompileError {
		StructTypeParams pms=((MembType) vt.structArgs).withPrecision(newPrecision);
		return new VarType(this, pms);
	}
	@Override
	public String asString(VarType varType) {
		return this.headerTypeString(varType);
	}
	@Override
	public String headerTypeString(VarType varType) {
		if(this.defaulttype==null) {
			return VarType.HDRFORMAT.formatted(this.name,((MembType)varType.structArgs).myType.headerString());
		}else  if (this.defaulttype.isFloatP()){
			//precision
			return VarType.HDRFORMAT.formatted(this.name,((MembType)varType.structArgs).myType.getPrecision());
		}else {
			return this.name;
		}
	}
	private static VarType myMembType(VarType mytype) {
		return ((MembType) mytype.structArgs).myType;
	}
	@Override
	public int getPrecision(VarType mytype) {
		return Vector.myMembType(mytype).getPrecision();
	}
	@Override
	protected String getJsonTextFor(Variable self) throws CompileError {
		String[] cpnts=new String[DIM];
		for(int i=0;i<DIM;i++)cpnts[i]=this.getComponent(self, i).getJsonText();
		return Regexes.formatJsonWith("Vector(%s,%s,%s)", cpnts);
	}
	@Override
	public int sizeOf(VarType mytype) {
		return 3*Vector.myMembType(mytype).sizeOf();
	}

	@Override
	public void allocate(PrintStream p, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		this.allocateArray(p, var, fillWithDefaultvalue, DIM, Vector.myMembType(var.type));
	}
	@Override
	public String getDefaultValue(VarType var) {
		return Struct.DEFAULT_LIST;
	}
	@Override
	public boolean canCasteFrom(VarType from, VarType mytype) {
		if(from.isStruct()&&from.struct instanceof Vector){
			return VarType.canCast(Vector.myMembType(from), Vector.myMembType(mytype));
		}else return false;
		
	}
	@Override
	public boolean canCasteTo(VarType to, VarType mytype) {
		if(to.isStruct()&&to.struct instanceof Vector){
			return VarType.canCast(Vector.myMembType(mytype), Vector.myMembType(to));
		}else return false;
	}
	@Override
	public void castRegistersTo(PrintStream p, RStack stack, int start, VarType newType, VarType mytype)
			throws CompileError {
		this.castElementwize(p, stack, start, mytype, newType);
	}

	@Override
	public void castRegistersFrom(PrintStream p, RStack stack, int start, VarType old, VarType mytype)
			throws CompileError {
		this.castElementwize(p, stack, start, old, mytype);
	}
	protected void castElementwize(PrintStream p, RStack stack, int start, VarType old, VarType newtype)
			throws CompileError {
		VarType from = Vector.myMembType(old);
		VarType to = Vector.myMembType(newtype);
		int sz=from.sizeOf();
		if(from.sizeOf()<to.sizeOf()) {
			//grow in advance
			for(int i=DIM-1;i>=0;i--) {
				int id2=start+i*from.sizeOf();
				int id1=start+i*to.sizeOf();
				stack.move(p, id1, id2,from.sizeOf());
			}
			sz=to.sizeOf();
		}
		for(int i=0;i<DIM;i++) {
			int id=start+i*sz;
			stack.castRegisterValue(p, i, old, newtype);
			
		}
		if(sz>to.sizeOf()) {
			//contract
			for(int i=0;i<DIM;i++) {
				int id2=start+i*sz;
				int id1=start+i*to.sizeOf();
				stack.move(p, id1, id2,from.sizeOf());
			}
		}
		
	}
	@Override
	public void getMe(PrintStream p, RStack stack, int home, Variable me) throws CompileError {
		for(int j=0;j<DIM;j++) {
			Variable cpn=this.getComponent(me, j);
			int hj=home+j*cpn.type.sizeOf();
			cpn.getMe(p, stack, hj);
		}
	}
	@Override
	public void setMe(PrintStream p, RStack stack, int home, Variable me) throws CompileError {
		for(int j=0;j<DIM;j++) {
			Variable cpn=this.getComponent(me, j);
			int hj=home+j*cpn.type.sizeOf();
			cpn.setMe(p, stack, hj,cpn.type);
		}
	}
	public static final OpType CROSS=OpType.MOD;//use mod for cross product
	@Override
	public boolean canDoBiOp(OpType op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		//uses the % operator as a cross product
		boolean oIsVec=other.isStruct()?other.struct instanceof Vector:false;
		boolean oIsNum=other.isStruct()?false:other.isNumeric();
		if(op==CROSS)return oIsVec;
		switch(op) {
		case ADD:
		case SUB:
		case EQ:
		case NEQ:
			return oIsVec;
		case MULT:
			return oIsVec||oIsNum;
		case DIV: return oIsNum && isFirst;
		default: return false;
		}
	}
	@Override
	public void doBiOpFirst(OpType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		Number est1=stack.getEstimate(home1);
		Number est2=stack.getEstimate(home2);
		if(op==CROSS) {
			this.crossProd(mytype, p, c, s, stack, home1, home2);
			Number estf=(est1==null||est2==null)? null : est1.doubleValue()*est2.doubleValue();
			stack.estmiate(home1, estf);
			return;
		}
		VarType other=stack.getVarType(home2);
		boolean oIsVec=other.isStruct()?other.struct instanceof Vector:false;
		boolean oIsNum=other.isStruct()?false:other.isNumeric();
		if(oIsVec) {
			this.elementwize(op, p, c, s, stack, home1, home2);
			switch(op) {
			case EQ:
				this.collect(OpType.AND, p, c, s, stack, home1, home1, stack.getVarType(home1).sizeOf());
				stack.estmiate(home1, null);
				break;
			case MULT:
				this.collect(OpType.ADD, p, c, s, stack, home1, home1, stack.getVarType(home1).sizeOf());
				Number estf=(est1==null||est2==null)? null : est1.doubleValue()*est2.doubleValue();
				stack.estmiate(home1, estf);
				break;
			case NEQ:
				this.collect(OpType.OR, p, c, s, stack, home1, home1, stack.getVarType(home1).sizeOf());
				stack.estmiate(home1, null);
				//next script should cap this properly
				break;
			default:
				//+ -
				//cast vector elements
				VarType typef = stack.getVarType(home1+X);//x-coord
				for(int i=0;i<DIM;i++)stack.castRegister(p, home1+i, typef);
				Number estf2=(est1==null||est2==null)? null : Math.hypot(est2.doubleValue(),est2.doubleValue());
				stack.estmiate(home1, estf2);
				stack.setVarType(home1, Vector.vectorOf(typef));
				break;//dont collect
			
			}
			//next script should cap this properly
		}else {
			this.elementwizeHalf(op, p, c, s, stack, home1, home2);
			VarType typef = stack.getVarType(home1+X);//x-coord
			for(int i=0;i<DIM;i++)stack.castRegister(p, home1+i, typef);
			switch(op) {
			case MULT:
				Number estf=(est1==null||est2==null)? null : est1.doubleValue()*est2.doubleValue();
				stack.estmiate(home1, estf);
				break;
			case DIV:
				stack.estmiate(home1, null);
				//next script should cap this properly
				break;
			default:
				//* /
				
				break;//dont collect
			
			}
			Number estf2=(est1==null||est2==null)? null : Math.hypot(est2.doubleValue(),est2.doubleValue());
			stack.estmiate(home1, estf2);
			stack.setVarType(home1, Vector.vectorOf(typef));
			//then done
		}
	}
	@Override
	public void doBiOpSecond(OpType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		VarType other=stack.getVarType(home1);
		boolean oIsVec=other.isStruct()?other.struct instanceof Vector:false;
		if(oIsVec) {
			other.struct.doBiOpFirst(op, mytype, p, c, s, stack, home1, home2);
			return;
		}else {
			if(op==OpType.DIV)throw new CompileError.UnsupportedOperation(other, op, mytype);
			//must be scalar*vec
			//reverse the roles
			this.doBiOpFirst(op, mytype, p, c, s, stack, home2, home1);
			//copy
			stack.move(p, home1, home2);
			
		}
	}
	
	private void elementwize(OpType op, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError{
		VarType te1=Vector.myMembType(stack.getVarType(home1));
		VarType te2=Vector.myMembType(stack.getVarType(home2));
		BiOperator opt=new BiOperator(c.line(), -1, op);
		Number est1=stack.getEstimate(home1);
		Number est2=stack.getEstimate(home2);
		for(int j=0;j<DIM;j++) {
			int h1=home1+j*te1.sizeOf();
			int h2=home2+j*te2.sizeOf();
			stack.setVarType(h1, te1);
			stack.setVarType(h2, te2);
			stack.estmiate(h1, est1);
			stack.estmiate(h2, est2);
			opt.perform(p, c, s, stack, h1, h2);
			stack.setVarType(h2, null);
		}
	}
	private void elementwizeUnary(UOType op, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1) throws CompileError{
		VarType te1=Vector.myMembType(stack.getVarType(home1));
		UnaryOp opt=new UnaryOp(c.line(), -1, op);
		Number est1=stack.getEstimate(home1);
		for(int j=0;j<DIM;j++) {
			int h1=home1+j*te1.sizeOf();
			stack.setVarType(h1, te1);
			stack.estmiate(h1, est1);
			opt.perform(p, c, s, stack, h1);
		}
	}
	private void elementwizeHalf(OpType op, PrintStream p, Compiler c, Scope s, RStack stack, Integer homevec,
			Integer homescalar) throws CompileError{
		VarType tev=Vector.myMembType(stack.getVarType(homevec));
		VarType tes=Vector.myMembType(stack.getVarType(homescalar));
		BiOperator opt=new BiOperator(c.line(), -1, op);
		for(int j=0;j<DIM;j++) {
			int hv=homevec+j*tev.sizeOf();
			stack.setVarType(hv, tev);
			opt.perform(p, c, s, stack, hv, homescalar);
		}
	}
	private void collect(OpType op, PrintStream p, Compiler c, Scope s, RStack stack, int out,
			int instart,int size) throws CompileError{
		BiOperator opt=new BiOperator(c.line(), -1, op);
		if(instart!=out) {
			//copy
			stack.move(p, out, instart);
		}//else do nothing
		for(int j=1;j<DIM;j++) {
			int hin=instart+j*size;
			opt.perform(p, c, s, stack, out, hin);
		}
		
	}
	

	public void crossProd(VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home1,
			Integer home2) throws CompileError {
		if(DIM!=3)throw new CompileError("Vector cross product cannot be defined for dimension %s != 3;");
		VarType te1=Vector.myMembType(stack.getVarType(home1));
		VarType te2=Vector.myMembType(stack.getVarType(home2));
		final BiOperator TIMES=new BiOperator(c.line(), -1, BiOperator.OpType.MULT);
		final BiOperator MINUS=new BiOperator(c.line(), -2, BiOperator.OpType.SUB);
		Number est1=stack.getEstimate(home1);
		Number est2=stack.getEstimate(home2);
		int home3=stack.reserve(te1.sizeOf()*(DIM+1));
		int s1=te1.sizeOf();
		int s2=te2.sizeOf();

		for(int k=0;k<DIM;k++) {
			int h1=home1+k*s1;
			int h2=home2+k*s2;
			stack.setVarType(h1, te1);
			stack.setVarType(h2, te2);
			stack.estmiate(h1, est1);
			stack.estmiate(h2, est2);
		}
		for(int k=0;k<DIM;k++) {
			int i=(k-2)%DIM;
			int j=(k-1)%DIM;
			int h1i=home1+i*s1;
			int h2j=home2+j*s2;
			int h1j=home1+j*s1;
			int h2i=home2+i*s2;
			int h3k=home3+k*s1;
			int h3kp1=h3k+s1;
			stack.move(p, h3k, h1i);
			TIMES.perform(p, c, s, stack, h3k, h2j);
			stack.move(p, h3kp1, h1j);
			TIMES.perform(p, c, s, stack, h3kp1, h2i);
			MINUS.perform(p, c, s, stack, h3k, h3kp1);
			//opt.perform(p, c, s, stack, h1, h2);
			//stack.setVarType(h2, null);
		}
		for(int k=0;k<DIM;k++) {
			int h1=home1+k*s1;
			int h3=home3+k*s1;
			stack.move(p, h1, h3);
		}
		//cast vector elements
		VarType typef = stack.getVarType(home1+X);//x-coord
		for(int i=0;i<DIM;i++)stack.castRegister(p, home1+i, typef);
		Number estf2=(est1==null||est2==null)? null : est2.doubleValue()*est2.doubleValue();
		stack.estmiate(home1, estf2);
		stack.setVarType(home1, Vector.vectorOf(typef));
		
	}
	
	@Override
	public boolean canDoUnaryOp(UOType op, VarType mytype, VarType other) throws CompileError {
		switch(op) {
		case UMINUS:return true;
		default: return false;
		}
	}
	@Override
	public void doUnaryOp(UOType op, VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home)
			throws CompileError {
		if(op!=UOType.UMINUS)throw new CompileError.UnsupportedOperation( op, mytype);
		this.doUnaryMinus(mytype, p, c, s, stack, home);
	}
	public void doUnaryMinus( VarType mytype, PrintStream p, Compiler c, Scope s, RStack stack, Integer home)
			throws CompileError {
		Number est=stack.getEstimate(home);
		this.elementwizeUnary(UOType.UMINUS, p, c, s, stack, home);
		stack.setVarType(home, mytype);
		stack.estmiate(home, est);
		
	}
	public static int dir(String d) {
		for(int j=0;j<DIM;j++)if(DIMNAMES[j].equals(d))return j;
		return -1;
	}
	@Override
	public boolean hasField(String name, VarType mytype) {
		return dir(name)>=0;
	}
	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		int i=dir(name);
		return this.getComponent(self, i);
	}
	public Variable getComponent(Variable self, int i) throws CompileError {
		if(i<0)throw new CompileError.VarNotFound(this, name);
		return self.indexMyNBTPath(i, Vector.myMembType(self.type));
	}
	public static Variable componentOf(Variable self,int i)throws CompileError {
		return ((Vector)self.type.struct).getComponent(self, i);
	}
	@Override
	public boolean hasBuiltinMethod(String name, VarType mytype) {
		return false;
	}
	@Override
	public BuiltinStructMethod getBuiltinMethod(Variable self, String name) throws CompileError {
		return null;
	}
	
	@Override public BuiltinConstructor getConstructor(VarType myType) throws CompileError {
		return new Constructor(this.name,myType);
	}
	public static class Constructor extends BuiltinConstructor{
		public Constructor(String name, VarType mytype) {
			super(name, mytype);
		}

		@Override
		public VarType getRetType(Args a) {
			return mytype;
		}

		@Override
		public Args tokenizeArgs(Compiler c, Matcher matcher, int line, int col,RStack stack) throws CompileError {
			BasicArgs a=new BuiltinFunction.BasicArgs().equations(c, line, col, matcher, stack);
			if(a.targs.size()!=DIM) throw new CompileError("wrong number of args in Vector()...;");
			return a;
		}

		private static final String NEW= "\"$Vector.$new\"";
		private Variable newobj(Compiler c) {
			Variable v=new Variable(NEW, mytype, null,c.resourcelocation);
			return v;
		}
		@Override
		public void call(PrintStream p, Compiler c, Scope s, Args args, RStack stack) throws CompileError {
			//default to storage
			Variable obj=this.newobj(c);
			obj.allocate(p, false);
			for(int i=0;i<DIM;i++) {
				Variable arg=Vector.componentOf(obj, i);
				Equation eq=(Equation) ((BasicArgs)args).targs.get(i);
				eq.compileOps(p, c, s,arg.type);
				eq.setVar(p, c, s, arg);
			}
			
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, Args args, RStack stack, int stackstart)
				throws CompileError {
			Variable obj=this.newobj(c);
			obj.getMe(p, stack, stackstart);
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, Args args, Variable v, RStack stack)
				throws CompileError {
			Variable obj=this.newobj(c);
			Variable.directSet(p, v, obj, stack);
		}

		@Override
		public Number getEstimate(Args args) {
			return null;
		}
		
	}

}
