package net.mcppc.compiler.struct;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.BuiltinFunction.BFCallToken;
import net.mcppc.compiler.StructTypeParams.MembType;
import net.mcppc.compiler.VarType.Builtin;
import net.mcppc.compiler.Variable.Mask;
import net.mcppc.compiler.errors.COption;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.RuntimeError;
import net.mcppc.compiler.functions.EquationMask;
import net.mcppc.compiler.functions.FunctionMask;
import net.mcppc.compiler.functions.PrintF;
import net.mcppc.compiler.target.VTarget;
import net.mcppc.compiler.tokens.BiOperator;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.BiOperator.OpType;
import net.mcppc.compiler.tokens.Regexes;
import net.mcppc.compiler.tokens.TemplateArgsToken;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Type;
import net.mcppc.compiler.tokens.UnaryOp;
import net.mcppc.compiler.tokens.UnaryOp.UOType;
/**
 * struct for dealing with directions of space; should be mappable onto an NBT array of length 3;
 * @author RadiumE13
 *
 */
public class Vector extends Struct {
	public static final Vector vector;
	public static final Vector vec3d;
	public static final Vector vec3i;
	public static final ResourceLocation STDLIB = new ResourceLocation(CompileJob.STDLIB_NAMESPACE ,"vecmath");
	static {
		vector=new Vector("Vector");
		vec3d=new Vector("Vec3d",VarType.DOUBLE);
		vec3i=new Vector("Vec3i",VarType.LONG);
		
	}
	public static void registerAll() {
		vec3d.STATICMETHODS= Map.of(
				LOOKAT.name, LOOKAT,
				LOOKING.name,LOOKING);
		vec3i.METHODS = Map.of(
				SqrMag.instance.name,SqrMag.instance
				);
		vec3d.METHODS = Map.of(
				NORM.name,NORM
				,MAG.name,MAG
				,SqrMag.instance.name,SqrMag.instance
				);
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
	
	public static Variable positionOf(Selector s,int precision) throws CompileError {
		VarType vt = vec3d.withPrecision(precision);
		Variable v = new Variable("$anonposov",vt,null,new ResourceLocation("mcppc","vector"));
		return v.maskEntity(s, NbtPath.POS);
	}
	public static Variable velocityOf(Selector s,int precision) throws CompileError {
		VarType vt = vec3d.withPrecision(precision);
		Variable v = new Variable("$anonvelof",vt,null,new ResourceLocation("mcppc","vector"));
		return v.maskEntity(s, NbtPath.MOTION);
	}
	//vector type can be set by type arg or by naming vector subtype
	//if null, must supply args
	//else must not supply args
	public static final int DIM=3;
	public static final String[] DIMNAMES= {"x","y","z"};
	public static final int X=0;
	public static final int Y=1;
	public static final int Z=2;
	

	public static final int HELICITY_LEFT=-1;
	public static final int HELICITY_RIGHT=+1;
	public static final int MINECRAFT_COORD_HELICITY=HELICITY_RIGHT;
	
	
	
	public final VarType defaulttype;
	/**
	 * states how the cross product should appear to act in game (which handedness)
	 * defaults behavior: same as minecraft coord system (right handed, which contrasts most other games)
	 */
	public final int helicity=MINECRAFT_COORD_HELICITY;
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
		return VarType.Builtin.NBT_LIST;//?
	}
	
	public boolean canMask(VarType mytype, Mask mask) {
		//return mask !=Mask.SCORE;
		return true;
	}
	@Override
	public StructTypeParams tokenizeTypeArgs(Compiler c,Scope s, Matcher matcher, int line, int col, List<Const> forbidden) throws CompileError {
		if(this.defaulttype==null)
			return StructTypeParams.MembType.tokenizeTypeArgs(c,s, matcher, line, col, forbidden);
		else  if (this.defaulttype.isFloatP()){
			//precision
			StructTypeParams.PrecisionType pc=StructTypeParams.PrecisionType.tokenizeTypeArgs(c,s, matcher, line, col,forbidden);
			VarType tp=pc.impose(this.defaulttype);
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
	public VarType withPrecision(int newPrecision) throws CompileError {
		if(this.defaulttype==null) throw new CompileError("cannot call withPrecision(int newPrecision) for type %s".formatted(this.name));
		StructTypeParams pms=new MembType(this.defaulttype).withPrecision(newPrecision);
		return new VarType(this, pms);
	}
	@Override
	public VarType withTemplatePrecision(VarType vt,String pc) throws CompileError {
		StructTypeParams pms=((MembType) vt.structArgs).withTemplatePrecision(pc);
		return new VarType(this, pms);
	}
	@Override
	public String asString(VarType varType) {
		return this.headerTypeString(varType);
	}
	@Override
	public String headerTypeString(VarType varType) {
		if(this.defaulttype==null) {
			return VarType.HDRFORMATNOTREADY.formatted(this.name,((MembType)varType.structArgs).myType.headerString());
		}else  if (this.defaulttype.isFloatP()){
			//precision
			//System.err.printf("%s\n",((MembType)varType.structArgs).myType.headerString());
			//System.err.printf("%s\n",((MembType)varType.structArgs).myType.precisionTemplateName);
			//System.err.printf("%s,%s\n",((MembType)varType.structArgs).myType.isReady(),((MembType)varType.structArgs).myType.getPrecisionStr());
			return VarType.HDRFORMATNOTREADY.formatted(this.name,((MembType)varType.structArgs).myType.getPrecisionStr());
		}else {
			return this.name;
		}
	}
	private static VarType myMembType(VarType mytype) {
		return ((MembType) mytype.structArgs).myType;
	}
	@Override
	public int getPrecision(VarType mytype, Scope s) throws CompileError {
		return Vector.myMembType(mytype).getPrecision(s);
	}

	@Override
	public String getPrecisionStr(VarType mytype)  {
		return Vector.myMembType(mytype).getPrecisionStr();
	}
	@Override
	public String getJsonTextFor(Variable self) throws CompileError {
		String[] cpnts=new String[DIM];
		for(int i=0;i<DIM;i++)cpnts[i]=this.getComponent(self, i).getJsonText();
		return Regexes.formatJsonWith("Vector(%s,%s,%s)", cpnts);
	}
	@Override
	public int sizeOf(VarType mytype) {
		return 3*Vector.myMembType(mytype).sizeOf();
	}
	@Override public boolean canBeRecursive(VarType type) {
		return true;
	}
	@Override
	public void allocateLoad(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		if(var.getMaskType() == Mask.SCORE) {
			for(int i=0;i<DIM;i++) {
				Variable vi = this.getComponent(var, i);
				vi.allocateLoad(p,tg, fillWithDefaultvalue);
			}
		} else {
			this.allocateArrayLoad(p, tg, var, fillWithDefaultvalue, DIM, Vector.myMembType(var.type));
		}
	}
	@Override
	public void allocateCall(PrintStream p, VTarget tg, Variable var, boolean fillWithDefaultvalue) throws CompileError {
		//do not need to support scores; used in recursion only
		this.allocateArrayCall(p, tg, var, fillWithDefaultvalue, DIM, Vector.myMembType(var.type));
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
	public void castRegistersTo(PrintStream p, Scope s, RStack stack, int start, VarType newType, VarType mytype)
			throws CompileError {
		this.castElementwize(p,s, stack, start, mytype, newType);
	}

	@Override
	public void castRegistersFrom(PrintStream p, Scope s, RStack stack, int start, VarType old, VarType mytype)
			throws CompileError {
		this.castElementwize(p,s, stack, start, old, mytype);
	}
	protected void castElementwize(PrintStream p,Scope s, RStack stack, int start, VarType old, VarType newtype)
			throws CompileError {
		VarType from = Vector.myMembType(old);
		VarType to = Vector.myMembType(newtype);
		//System.err.printf("vector cast start %s -> %s \n ",from.headerString(),to.headerString());
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
			stack.setVarType(id, from);
		}
		for(int i=0;i<DIM;i++) {
			int id=start+i*sz;
			//System.err.printf("%d : %s \n ",id,stack.getVarType(id));
			
			stack.castRegister(p,s, id, to);
			
		}
		if(sz>to.sizeOf()) {
			//contract
			for(int i=0;i<DIM;i++) {
				int id2=start+i*sz;
				int id1=start+i*to.sizeOf();
				stack.move(p, id1, id2,from.sizeOf());
			}
		}
		//System.err.printf("vector cast %s -> %s;\n",old.headerString(),newtype.headerString());
		//stack.setVarType(start, newtype);
		//stack.debugOut(System.err);
	}
	@Override
	public void getMe(PrintStream p, Scope s, RStack stack, int home, Variable me, VarType typeWanted) throws CompileError {
		for(int j=0;j<DIM;j++) {
			Variable cpn=this.getComponent(me, j);
			int hj=home+j*cpn.type.sizeOf();
			cpn.getMe(p,s, stack, hj, typeWanted);
		}
		//stack.runtimeOutShow(p, home, home+2,PrintF.stderr);
		stack.setVarType(home, me.type);
	}
	@Override
	public void setMe(PrintStream p, Scope s, RStack stack, int home, Variable me) throws CompileError {
		//if(s.isDebugMode())stack.printTypes(System.err,home,home+me.type.sizeOf());
		for(int j=0;j<DIM;j++) {
			Variable cpn=this.getComponent(me, j);
			int hj=home+j*cpn.type.sizeOf();
			cpn.setMe(p,s, stack, hj,cpn.type);
		}
	}
	@Override
	public void getMeDirect(PrintStream p,Scope s,RStack stack,Variable to, Variable me)throws CompileError{
		this.directSet(p, s, stack, to, me);
	}
	@Override
	public void setMeDirect(PrintStream p,Scope s,RStack stack,Variable me, Variable from)throws CompileError{
		this.directSet(p, s, stack, me, from);
	}
	private static void directSet(PrintStream p,Scope s,RStack stack,Variable to, Variable from) throws CompileError{
		if(    to.getMaskType() == Mask.SCORE
			|| from.getMaskType() == Mask.SCORE
			|| !Vector.myMembType(to.type).equals(Vector.myMembType(from.type))) {
			//element-wise direct set
			for(int i=0;i<DIM;i++) {
				Variable fi=((Vector) from.type.struct).getComponent(from, i);
				Variable ti=((Vector) to.type.struct).getComponent(to, i);
				
				Variable.directSet(p, s, ti, fi, stack);
			}
		} else {
			Struct.basicSetDirect(p, to, from);//super
		}
		
	}
	public static final OpType CROSS=OpType.MOD;//use mod for cross product
	@Override
	public boolean canDoBiOp(OpType op, VarType mytype, VarType other, boolean isFirst) throws CompileError {
		//uses the % operator as a cross product
		boolean oIsVec=other.isStruct()?other.struct instanceof Vector:false;
		boolean oIsNum=other.isNumeric();//other.isStruct()?false:other.isNumeric();
		if(oIsVec && ((Vector)other.struct).helicity!=this.helicity) return false;//cannot mix right and left handed vectors
		if(op==CROSS) {
			//vecs must have equal helicity
			return oIsVec;
		}
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
		boolean oIsNum=other.isNumeric();//other.isStruct()?false:other.isNumeric();
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
				for(int i=0;i<DIM;i++)stack.castRegister(p,s, home1+i, typef);
				Number estf2=(est1==null||est2==null)? null : Math.hypot(est2.doubleValue(),est2.doubleValue());
				stack.estmiate(home1, estf2);
				stack.setVarType(home1, Vector.vectorOf(typef));
				break;//dont collect
			
			}
			//next script should cap this properly
		}else {
			if(s.<Boolean>getOption(COption.DEBUG_MODE, c.job, c.getFlagCursor())) { //.isDebugMode()
				RuntimeError.printf(p,"", "scores, v = (%s,%s,%s) %s num %s"
						, stack.getRegister(home1), stack.getRegister(home1+1), stack.getRegister(home1+2),
						  PrintF.IPrintable.string(op.s),
						 stack.getRegister(home2));
			}
			this.elementwizeHalf(op, p, c, s, stack, home1, home2);
			if(s.<Boolean>getOption(COption.DEBUG_MODE, c.job, c.getFlagCursor())) { //.isDebugMode()
				RuntimeError.printf(p,"", "  = (%s,%s,%s)"
						, stack.getRegister(home1), stack.getRegister(home1+1), stack.getRegister(home1+2));
			}
			VarType typef = stack.getVarType(home1+X);//x-coord
			for(int i=0;i<DIM;i++)stack.castRegister(p,s, home1+i, typef);
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
			//FIXED scalar vec order issue
			
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
		}
		for(int j=0;j<DIM;j++) {
			int h1=home1+j*te1.sizeOf();
			int h2=home2+j*te2.sizeOf();
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
		//VarType tes=Vector.myMembType(stack.getVarType(homescalar));
		BiOperator opt=new BiOperator(c.line(), -1, op);
		for(int j=0;j<DIM;j++) {
			int hv=homevec+j*tev.sizeOf();
			stack.setVarType(hv, tev);
		}
		for(int j=0;j<DIM;j++) {
			int hv=homevec+j*tev.sizeOf();
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
	
	private int invertCrossProd() {
		return this.helicity*Vector.MINECRAFT_COORD_HELICITY;
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

		int home3=stack.reserve(te1.sizeOf()*(DIM+2));
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
			// -1 % 3 = -1 in java
			//use Math.floormod(-1,3) = 2
			int flip=this.invertCrossProd();
			int i=Math.floorMod((k-2*flip),DIM);
			int j=Math.floorMod((k-1*flip),DIM);
			int h1i=home1+i*s1;
			int h2j=home2+j*s2;
			int h1j=home1+j*s1;
			int h2i=home2+i*s2;
			int h3k=home3+k*s1;
			int h3kp1=h3k+s1;
			int h3kp2=h3k+s1*2;
			stack.move(p, h3k, h1i);
			stack.move(p, h3kp1, h2j);
			TIMES.perform(p, c, s, stack, h3k, h3kp1);
			stack.move(p, h3kp1, h1j);
			stack.move(p, h3kp2, h2i);
			TIMES.perform(p, c, s, stack, h3kp1, h3kp2);
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
		for(int i=0;i<DIM;i++)stack.castRegister(p,s, home1+i, typef);
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
	@Override public boolean canDoLiteralMultDiv(BiOperator op,VarType mytype,Num other)throws CompileError {
		VarType memb=Vector.myMembType(mytype);
		return memb.isStruct()?memb.struct.canDoLiteralMultDiv(op, memb, other):memb.isNumeric();
	}
	@Override public void doLiteralMultDiv(BiOperator op,VarType mytype,PrintStream p,Compiler c,Scope s, RStack stack,Integer in,Integer dest,Num other) throws CompileError{
		VarType vt=mytype;
		VarType memb=Vector.myMembType(mytype);
		for(int i=0;i<DIM;i++) {
			int h=in + memb.sizeOf()*i;
			stack.setVarType(h, memb);
			op.literalMultOrDiv(p, c, s, stack, h, h, other);
		}
		stack.setVarType(in, mytype);
		stack.move(p, dest, in);
	}

	@Override public TemplateArgsToken getTemplateArgs(VarType varType, Scope s) throws CompileError {
		VarType type=Vector.myMembType(varType);
		if(this.defaulttype==null) {
			TemplateArgsToken tp=new TemplateArgsToken(-1, -1);
			tp.add(new Type(-1,-1,type));
			return tp;
		}else {
			return type.getTemplateArgs(s);
		}
	}
	public static int dir(String d) {
		for(int j=0;j<DIM;j++)if(DIMNAMES[j].equals(d))return j;
		return -1;
	}
	@Override
	public boolean hasField(Variable self, String name) {
		return dir(name)>=0;
	}
	@Override
	public Variable getField(Variable self, String name) throws CompileError {
		int i=dir(name);
		return this.getComponent(self, i);
	}
	protected Variable getComponent(Variable self, int i) throws CompileError {
		if(i<0)throw new CompileError.VarNotFound(this, name);
		
		if(self.getMaskType() == Mask.SCORE) {
			return self.indexMyScoreBasic(i, Vector.myMembType(self.type));//basic
		}
		return self.indexMyNBTPathBasic(i, Vector.myMembType(self.type));
	}
	protected static Variable componentOf(Variable self,int i)throws CompileError {
		return ((Vector)self.type.struct).getComponent(self, i);
	}
	private Map<String,BuiltinFunction> METHODS;
	public static final FunctionMask NORM=new FunctionMask("norm", STDLIB, "norm" , true);
	public static final FunctionMask MAG=new FunctionMask("mag", STDLIB, "mag" , true);
	@Override
	public boolean hasBuiltinMethod(Variable self, String name) {
		return super.hasBuiltinMethodBasic(name, this.METHODS);
	}
	@Override
	public BuiltinFunction getBuiltinMethod(Variable self, String name) throws CompileError {
		return super.getBuiltinMethodBasic(self, name, this.METHODS);
	}
	public static class SqrMag extends EquationMask{
		public static final SqrMag instance = new SqrMag("sqrmag");
		public SqrMag(String name) {
			super(name);
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack) throws CompileError {
			return super.tokenizeArgsNone(c, matcher, line, col);
		}

		@Override
		public Equation convert(BFCallToken token, Compiler c, Scope s, RStack stack) throws CompileError {
			if(!token.hasThisBound()) throw new CompileError("function %s must be called as a member of a Vector object".formatted(this.name));
			Variable self=token.getThisBound();
			Token vt = self.basicMemberName(s);
			BiOperator op = new BiOperator(-1,-1,OpType.MULT);
			Equation ret = Equation.subEqHusk(stack, vt,op,vt);
			return ret;
		}
		@Override
		public boolean isNonstaticMember() {
			return true;
		}
		
	}
	private final Constructor init = new Constructor(this);
	@Override public BuiltinConstructor getConstructor(VarType myType) throws CompileError {
		return this.init;
	}
	public static class Constructor extends BuiltinConstructor{
		public Constructor(String name) {
			super(name);
		}
		public Constructor(Vector clazz) {
			this(clazz.name);
		}

		@Override
		public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line,int col, RStack stack) throws CompileError {
			BasicArgs a=new BuiltinFunction.BasicArgs().equations(c, s, line, col, matcher, stack);
			if(a.nargs()!=DIM) throw new CompileError("wrong number of args in Vector()...;");
			return a;
		}

		private static final String NEW= "\"$Vector\".\"$new\"";
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
			for(int i=0;i<DIM;i++) {
				Variable arg=Vector.componentOf(obj, i);
				Equation eq=(Equation) ((BasicArgs)args).arg(i);
				eq.compileOps(p, c, s,arg.type);
				eq.setVar(p, c, s, arg);
			}
			
		}

		@Override
		public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
				throws CompileError {
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
	public static final FunctionMask LOOKAT=new FunctionMask("lookAt", STDLIB, "lookAt"
			, List.of(Token.NullArgDefault.instance,Token.NullArgDefault.instance,new Num(-1,-1,1,VarType.INT)));//List.of does not allow null members;
	public static final FunctionMask LOOKING=new FunctionMask("looking", STDLIB, "looking"
			, List.of(new Selector.SelectorToken(-1,-1,Selector.AT_S)));//List.of does not allow null members;


	private Map<String,BuiltinFunction> STATICMETHODS;
	@Override
	public boolean hasStaticBuiltinMethod(String name) {
		return super.hasStaticBuiltinMethodBasic(name, this.STATICMETHODS);
	}
	@Override
	public BuiltinFunction getStaticBuiltinMethod(String name, VarType type) throws CompileError {
		return super.getStaticBuiltinMethodBasic(name,type, this.STATICMETHODS);
	}
	@Override
	public void deallocateLoad(PrintStream p, VTarget tg, Variable var) throws CompileError {
		if(var.getMaskType().isNbt)
			super.deallocateLoad(p, tg, var);
		//else done
	}

}
