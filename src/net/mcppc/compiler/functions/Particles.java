package net.mcppc.compiler.functions;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import net.mcppc.compiler.BuiltinFunction;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const;
import net.mcppc.compiler.Coordinates;
import net.mcppc.compiler.Coordinates.CoordSystem;
import net.mcppc.compiler.Coordinates.CoordToken;
import net.mcppc.compiler.RStack;
import net.mcppc.compiler.Scope;
import net.mcppc.compiler.Selector;
import net.mcppc.compiler.VarType;
import net.mcppc.compiler.Variable;
import net.mcppc.compiler.Const.ConstExprToken;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.struct.Singleton;
import net.mcppc.compiler.struct.Struct;
import net.mcppc.compiler.target.Targeted;
import net.mcppc.compiler.tokens.Equation;
import net.mcppc.compiler.tokens.Factories;
import net.mcppc.compiler.tokens.Num;
import net.mcppc.compiler.tokens.ParticleTypeToken;
import net.mcppc.compiler.tokens.Token;
import net.mcppc.compiler.tokens.Token.BasicName;
/**
 * super class of various mass particle functions;<p>
 * args are:
 * (particle_id and its args)
 * optional: force|normal
 * optional: showTo
 * ...
 * @author RadiumE13
 *
 */
public abstract class Particles extends BuiltinFunction {
	public static final Singleton particles = new Singleton("particles");
	public static final Line line = new Line("line");
	public static final Ring ring = new Ring("ring");
	public static final Sphere sphere = new Sphere("sphere");
	public static void registerAll() {
		//TODO test line
		particles.put(line);
		particles.put(ring);
		particles.put(sphere);
		Struct.register(particles);
	}
	
	public static final Set<String> FORCEMODES = Set.of("force","normal");
	public Particles(String name) {
		super(name);
	}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack, int stackstart, VarType typeWanted)
			throws CompileError {}

	@Override
	public void getRet(PrintStream p, Compiler c, Scope s, BFCallToken token, Variable v, RStack stack)
			throws CompileError {}

	@Override
	public Number getEstimate(BFCallToken token) {
		return null;
	}


	@Override
	public VarType getRetType(BFCallToken token) {
		return VarType.VOID;
	}
	
	@Override
	public Args tokenizeArgs(Compiler c, Scope s, Matcher matcher, int line, int col, RStack stack)
			throws CompileError {
		Token.Factory[] look = Factories.genericLook(ParticleTypeToken.factory,ParticleTypeToken.factoryImpliedMc);
		ParticleTypeToken particle = (ParticleTypeToken) c.nextNonNullMatch(look);
		if(!BuiltinFunction.findArgsep(c)) throw new CompileError("not enough args in %s(...) call".formatted(this.name));
		int scursor = c.cursor;
		Token doforce = c.nextNonNullMatch(Factories.checkForBasicName);
		if(doforce instanceof BasicName) {
			if(FORCEMODES.contains(((BasicName) doforce).name)) {
				if(!BuiltinFunction.findArgsep(c)) throw new CompileError("not enough args in %s(...) call".formatted(this.name));
			}else {
				c.cursor=scursor;//rewind
				doforce=null;
			}
		}
		//System.err.printf("next chars: %s\n", c.getNextChars());
		BasicArgs basics = BuiltinFunction.tokenizeArgsEquations(c, s, matcher, line, col, stack);
		//args out of order but with names;
		basics.add("id", particle);
		basics.add("doforce", doforce);
		return basics;
	}
	protected ParticleTypeToken getId(BFCallToken token) {
		return (ParticleTypeToken) ((BasicArgs)token.getArgs()).arg("id");
	}
	protected String getForce(BFCallToken token) {
		BasicName bn = (BasicName) ((BasicArgs)token.getArgs()).arg("doforce");
		if(bn!=null)return bn.name ;
		else return "normal";
	}
	protected Selector getShowTo(Compiler c,Scope s,BFCallToken token) throws CompileError {
		Equation eq = (Equation) ((BasicArgs)token.getArgs()).arg(0);
		eq.constify(c, s);
		if(!eq.isConstable()) throw new CompileError("could not constify equation in %s(...)".formatted(this.name));
		ConstExprToken t = eq.getConst();
		if(t.constType()!=ConstType.SELECTOR) return Selector.AT_A;
		else return ((Selector.SelectorToken)t).selector();
	}
	protected int getStartindex(Compiler c,Scope s,BFCallToken token) throws CompileError {
		Equation eq = (Equation) ((BasicArgs)token.getArgs()).arg(0);
		eq.constify(c, s);
		if(!eq.isConstable()) throw new CompileError("could not constify equation in %s(...)".formatted(this.name));
		ConstExprToken t = eq.getConst();
		if(t.constType()!=ConstType.SELECTOR) return 0;
		else return 1;
	}
	/**
	 * makes a line of particles;<p>
	 * args: pos1, pos2, num, [speed]
	 * @author RadiumE13
	 *
	 */
	public static class Line extends Particles {

		public Line(String name) {
			super(name);
		}
		@Targeted
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			ParticleTypeToken id = this.getId(token);
			String doforce = this.getForce(token);
			Selector showTo = this.getShowTo(c, s, token);
			BasicArgs args = (BasicArgs) token.getArgs();
			int start = this.getStartindex(c, s, token);
			if(args.nargs() < start + 3 + 2) throw new CompileError("not enough args in %s(...) call".formatted(this.name));
			Coordinates.CoordToken v1 = (CoordToken) Equation.constifyAndGet(p, (Equation)args.arg(start), c, s, stack, ConstType.COORDS);
			Coordinates.CoordToken v2 = (CoordToken) Equation.constifyAndGet(p, (Equation)args.arg(start+1), c, s, stack, ConstType.COORDS);
			Num num = (Num) Equation.constifyAndGet(p, (Equation)args.arg(start+2), c, s, stack, ConstType.NUM);
			Number speed;
			if(args.nargs() > start + 3 + 2) {
				Num spd = (Num) Equation.constifyAndGet(p, (Equation)args.arg(start+3), c, s, stack, ConstType.NUM);
				speed = spd.value;
			}else speed = 0;
			int n = num.value.intValue();
			for(int i=0;i<n;i++) {
				double a = (double)i / (n-1);
				Coordinates v3 = Coordinates.interpolate(v1.pos, v2.pos, a);
				p.printf("particle %s %s 0 0 0 %s 1 %s %s\n", id.textInMcf(),v3.inCMD(),speed,doforce,showTo.toCMD());
			}
			
		}
		
	}
	/**
	 * makes a ring of particles;<p>
	 * args: radius, velocity, num
	 * 
	 * if velocity is in caret notation, then v_x = v_r, v_y = v_y, and v_z = v_theta;
	 * polar cordinates match first element of Rotation[]
	 * @author RadiumE13
	 *
	 */
	public static class Ring extends Particles {

		public Ring(String name) {
			super(name);
		}
		@Targeted
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			ParticleTypeToken id = this.getId(token);
			String doforce = this.getForce(token);
			Selector showTo = this.getShowTo(c, s, token);
			BasicArgs args = (BasicArgs) token.getArgs();
			int start = this.getStartindex(c, s, token);
			if(args.nargs() < start + 3 + 2) throw new CompileError("not enough args in %s(...) call".formatted(this.name));
			Num rad = (Num) Equation.constifyAndGet(p, (Equation)args.arg(start+0), c, s, stack, ConstType.NUM);
			Coordinates.CoordToken v = (CoordToken) Equation.constifyAndGet(p, (Equation)args.arg(start+1), c, s, stack, ConstType.COORDS);
			Num num = (Num) Equation.constifyAndGet(p, (Equation)args.arg(start+2), c, s, stack, ConstType.NUM);

			int n = num.value.intValue();
			for(int i=0;i<n;i++) {
				double theta = Math.PI*2. *(double)i/n;
				double dx1 = rad.value.doubleValue()*Math.cos(theta);
				double dx2 = rad.value.doubleValue()*Math.sin(theta);
				Coordinates ri = Coordinates.asTildes(-dx2, 0, dx1);
				Coordinates vi = v.pos;
				if(vi.getSystem(0) == CoordSystem.CARET) {
					double vr = vi.numCoord(0);
					double vtht = vi.numCoord(2);
					double v1 = vr * Math.cos(theta) - vtht * Math.sin(theta);
					double v2 = vr * Math.sin(theta) + vtht * Math.cos(theta);
					double vy = vi.numCoord(1);
					vi = Coordinates.asAbs(-v2, vy, v1);
					
				}else if (vi.getSystem(0) == CoordSystem.TILDE) {
					throw new CompileError("dont use tiled notation for particle ring velocity; use absolute or caret only");
				}
				p.printf("particle %s %s %s 1 0 %s %s\n", id.textInMcf(),ri.inCMD(),vi.inCMD(),doforce,showTo.toCMD());
			}
			
		}
		
	}
	/**
	 * makes a sphere of particles;<p>
	 * args: radius, velocity, num
	 * 
	 * if velocity is in caret notation, then v_x = v_r, v_y = v_theta, and v_z = v_phi;
	 * polar cordinates match first element of Rotation[]
	 * theta is zero at equator and increases upward
	 * number is only approximate, actual number may vary
	 * @author RadiumE13
	 *
	 */
	public static class Sphere extends Particles {
		public static record LatLong(double p,double t) {}
		
		public List<LatLong> points(int number){
			return pointsBasic(number);
			
		}
		public Sphere(String name) {
			super(name);
		}
		@Targeted
		@Override
		public void call(PrintStream p, Compiler c, Scope s, BFCallToken token, RStack stack) throws CompileError {
			ParticleTypeToken id = this.getId(token);
			String doforce = this.getForce(token);
			Selector showTo = this.getShowTo(c, s, token);
			BasicArgs args = (BasicArgs) token.getArgs();
			int start = this.getStartindex(c, s, token);
			if(args.nargs() < start + 3 + 2) throw new CompileError("not enough args in %s(...) call".formatted(this.name));
			Num rad = (Num) Equation.constifyAndGet(p, (Equation)args.arg(start+0), c, s, stack, ConstType.NUM);
			Coordinates.CoordToken v = (CoordToken) Equation.constifyAndGet(p, (Equation)args.arg(start+1), c, s, stack, ConstType.COORDS);
			Num num = (Num) Equation.constifyAndGet(p, (Equation)args.arg(start+2), c, s, stack, ConstType.NUM);
			double r = rad.value.doubleValue();
			int n = num.value.intValue();
			List<LatLong> points = this.points(n);
			for(LatLong pt : points) {
				double phi = pt.p();
				double theta = pt.t();
				//basis vectors
				double erx = -Math.sin(phi)*Math.cos(theta);
				double ery = Math.sin(theta);
				double erz = Math.cos(phi)*Math.cos(theta);
				
				double etx = -Math.sin(phi)*-Math.sin(theta);
				double ety = Math.cos(theta);
				double etz = Math.cos(phi)*-Math.sin(theta);
				
				double epx = -Math.cos(phi)*Math.cos(theta);
				double epy = 0;
				double epz = -Math.sin(phi)*Math.cos(theta);
				
				Coordinates ri = Coordinates.asTildes(erx*r,ery*r,erz*r);
				Coordinates vi = v.pos;
				if(vi.getSystem(0) == CoordSystem.CARET) {
					double vr = vi.numCoord(0);
					double vt = vi.numCoord(1);
					double vp = vi.numCoord(2);
					double vx = vr * erx + vt * etx + vp * epx;
					double vy = vr * ery + vt * ety + vp * epy;
					double vz = vr * erz + vt * etz + vp * epz;
					vi = Coordinates.asAbs(vx,vy,vz);
					
				}else if (vi.getSystem(0) == CoordSystem.TILDE) {
					throw new CompileError("dont use tiled notation for particle ring velocity; use absolute or caret only");
				}
				p.printf("particle %s %s %s 1 0 %s %s\n", id.textInMcf(),ri.inCMD(),vi.inCMD(),doforce,showTo.toCMD());
			}
			
		}
		public static List<LatLong> pointsBasic(int number){
			List<LatLong> list = new ArrayList<LatLong>();
			int n =(int) Math.sqrt(number*Math.PI/4.);
			list.add(new LatLong(0,-Math.PI/2.));
			list.add(new LatLong(0,Math.PI/2.));
			for(int i=1;i<n;i++) {
				double theta = Math.PI *(double)i/n - Math.PI/2.;
				int m = (int)(2*n*Math.cos(theta));
				for(int j=0;j<m;j++) {
					double phi = Math.PI*2. *(double)j/m;
					list.add(new LatLong(phi,theta));
				}
			}
			return list;
			
		}
		
	}
}
