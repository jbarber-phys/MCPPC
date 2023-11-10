package net.mcppc.compiler.tokens;
import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.Const.ConstType;
import net.mcppc.compiler.DommentCollector.Dump;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.errors.OutputDump;
import net.mcppc.compiler.errors.CompileError.UnexpectedToken;
import net.mcppc.compiler.errors.Warnings;
import net.mcppc.compiler.struct.Entity;
import net.mcppc.compiler.tokens.Statement.Domment;
import net.mcppc.compiler.tokens.Token.Assignlike.Kind;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;

//TODO allow estimate of function return value
//example public int a(int b,int c) ~~ a*b*2 {...}
//example public float sin(float x) ~~ min(x,1) {...}


/**
 * declares a function or variable or const:
 * 
 * for var:
 * <public / private> <type> <name> [ -> mask] [~~ estimate] [= assignment];
 * <extern> <type> <name>  -> < mask> [~~ estimate] [= assignment];
 * for const:
 * <public / private> const <const-type> <name>  = <value / other const>;
 * for function:
 * <public / private> [recursive] [template] <return type> [<thistype>.]<name>  (<args...>) [final] [ export <...>] {...};
 * <extern> [recursive] [template] <return type> [<thistype>.]<name>  (<args...>) [final] [ export <...>] -> <mcfunction location>;
 * for an arg:
 * 	[ref] <type> name
 * 	Separator: ','
 * @author RadiumE13
 *
 */
public class Declaration extends Statement implements Statement.Headerable,DommentCollector,Statement.CodeBlockOpener{
	
	static final Token.Factory[] look = Factories.genericLookPriority(new Token.Factory[]{Token.BasicName.factory}, 
			Token.Paren.factory,
			Token.Assignlike.factoryMask,Token.Assignlike.factoryAssign,Token.Assignlike.factoryEstimate,
			Token.ArgEnd.factory,Token.LineEnd.factory,Token.CodeBlockBrace.factory
			);

	static final Token.Factory[] lookMaskHolder = Factories.genericLook(
			Coordinates.CoordToken.factory,Selector.SelectorToken.factory,ResourceLocation.ResourceToken.factory,Token.WildChar.dontPassFactory);
	static final Token.Factory[] lookMaskOp =  Factories.genericLook(
			Token.TagOf.factory,Token.ScoreOf.factory,Token.BossbarMaskSep.factory,Token.LineEnd.factory,Token.CodeBlockBrace.factory);

	static final Token.Factory[] lookMaskScore = Factories.genericLook(Token.BasicName.factory);
	
	static final Token.Factory[] lookMaskNbt = Factories.genericLook(NbtPath.NbtPathToken.factory);
	
	
	static final Token.Factory[] lookCompiletime = Factories.genericLookPriority(new Token.Factory[]{Token.BasicName.factory}, 
			Token.Paren.factory,
			Token.Assignlike.factoryMask,Token.Assignlike.factoryAssign,Token.Assignlike.factoryEstimate,
			Token.ArgEnd.factory,Token.LineEnd.factory,Token.CodeBlockBrace.factory,Num.factory
			);
	
	static final Token.Factory[] checkForRef = Factories.genericLook(
			Token.RefArg.factory,Token.WildChar.dontPassFactory 
			);
	
	private boolean isArgRef(Compiler c, Matcher matcher, int line, int col) throws CompileError {
		Token t=c.nextNonNullMatch(checkForRef);
		if (t instanceof Token.RefArg) {
			//CompileJob.compileMcfLog.println("found a ref arg;");
			return true;
		}
		//else it is wildchar
		return false;
	}
	private boolean isNextCloseParen(Compiler c, Matcher matcher, int line, int col) throws CompileError {
		Token t=c.nextNonNullMatch(Factories.checkForParen);
		if (t instanceof Token.Paren && !((Token.Paren) t).forward) {
			//CompileJob.compileMcfLog.println("found a ref arg;");
			return true;
		}
		//else it is wildchar
		return false;
	}
	public void applyMask(Compiler c, Matcher matcher, int line, int col,Keyword access, boolean skip) throws CompileError {
		
		Token holder=c.nextNonNullMatch(lookMaskHolder);
		boolean isThreadThis = false;
		if(holder instanceof Token.WildChar) {
			//holder is a member-name
			if (c.currentScope.hasThread() && c.currentScope.getThread().hasSelf()) 
				isThreadThis = Keyword.checkFor(c, matcher, Keyword.THIS);
			if(isThreadThis) ;
			else holder = Const.checkForExpressionSafe(c,c.currentScope, matcher, line, col, ConstType.SELECTOR);//check for consts
		}
		if(holder ==null && !isThreadThis) {
			holder = c.nextNonNullMatch(Factories.checkForBasicName);
			if(holder instanceof Token.BasicName) holder = ((Token.BasicName) holder).toMembName();
			if(!(holder instanceof MemberName)) throw new CompileError.UnexpectedToken(holder,"var name or mask target");
			if(c.currentScope.isInFunctionDefine()) {
				//System.err.printf("func: %s\n", c.currentScope.getFunction().name);
				//System.err.printf("locals: %s\n", c.currentScope.getFunction().locals.keySet());
			}
			((MemberName)holder).identify(c, c.currentScope);
		}
		if (holder instanceof Coordinates.CoordToken) {
			//space delimited
			Token address=Const.checkForExpression(c,c.currentScope, matcher, line, col, ConstType.NBT);
					//c.nextNonNullMatch(lookMaskNbt);
			if(!(address instanceof NbtPath.NbtPathToken))throw new CompileError.UnexpectedToken(address,"nbt path");
			if(!skip)this.variable.maskBlock(((Coordinates.CoordToken)holder).pos, ((NbtPath.NbtPathToken)address).path());
			return;
		}
		int beforeop=c.cursor;
		Token op=c.nextNonNullMatch(lookMaskOp);
		boolean isEntity=false;
		boolean isSelector=false;
		isEntity = isSelector = holder instanceof Selector.SelectorToken;
		isEntity = isEntity	|| (holder instanceof MemberName && ((MemberName) holder).var.type.isConstReducable(ConstType.SELECTOR));
		isEntity = isEntity || isThreadThis;
		if (isEntity && op instanceof Token.TagOf) {
			//entity
			Token address=Const.checkForExpression(c,c.currentScope, matcher, line, col, ConstType.NBT);
			if(!(address instanceof NbtPath.NbtPathToken))throw new CompileError.UnexpectedToken(address,"nbt path");
			NbtPath path = ((NbtPath.NbtPathToken)address).path();
			if(!skip) {
				if(isThreadThis) c.currentScope.getThread().thisNbt(variable, path);
				else {
					Selector selector = Entity.getSelectorFor(holder,true);
					this.variable.maskEntity(selector, path);
				}
			}
			return;
		}else if (isEntity && op instanceof Token.ScoreOf) {
			//score
			//=isSelector? ((Selector.SelectorToken)holder).selector():Entity.entities.getSelectorFor(((MemberName) holder).var);
			Token address=c.nextNonNullMatch(lookMaskScore);
			if(!(address instanceof Token.BasicName))throw new CompileError.UnexpectedToken(address,"score");
			String objective = ((Token.BasicName)address).name;
			if(!skip) {
				if(isThreadThis) c.currentScope.getThread().thisScore(this.variable, objective);
				else {
					Selector selector = Entity.getSelectorFor(holder,true);
					this.variable.maskScore(selector, objective);
				}
			}
			return;
			
		}else if (holder instanceof ResourceLocation.ResourceToken && op instanceof Token.TagOf) {
			//storage
			Token address=Const.checkForExpression(c,c.currentScope, matcher, line, col, ConstType.NBT);
					//c.nextNonNullMatch(lookMaskNbt);
			if(!(address instanceof NbtPath.NbtPathToken))throw new CompileError.UnexpectedToken(address,"nbt path");
			if(!skip)this.variable.maskStorage(((ResourceLocation.ResourceToken)holder).res, ((NbtPath.NbtPathToken)address).path());
			return;	
		}else if (holder instanceof ResourceLocation.ResourceToken && op instanceof Token.BossbarMaskSep) {
			//bossbar
			Token address=Const.checkForExpression(c,c.currentScope, matcher, line, col, ConstType.STRLIT);
			if(!(address instanceof Token.StringToken))throw new CompileError.UnexpectedToken(address,"string");
			boolean isExtern = Keyword.checkFor(c, matcher, Keyword.EXTERN);
			if(!skip)this.variable.maskBossbar(((ResourceLocation.ResourceToken)holder).res, ((Token.StringToken) address).literal(),!isExtern,false);
			return;	
		}
		else if (holder instanceof MemberName && (op instanceof Token.LineEnd || op instanceof Token.CodeBlockBrace)) {
			//another var
			c.cursor=beforeop;
			if(!skip)this.variable.maskOtherVar(((MemberName) holder).var);
			return;	
		}
		else {
			//incorrect format
			throw new CompileError.UnexpectedToken(op,"'.'");
		}
	}
	public void addConst(Compiler c,Scope s, Matcher matcher, int line, int col,Keyword access,boolean isReadingHeader, boolean isCompiling) throws CompileError {
		if (isCompiling) {
			c.nextNonNullMatch(Factories.headerSkipline);
			return;
		}
		Token ctypet = c.nextNonNullMatch(Factories.checkForBasicName);
		if(!(ctypet instanceof Token.BasicName))throw new CompileError.UnexpectedToken(ctypet, "const type name");
		Const.ConstType ctype = Const.ConstType.get(((Token.BasicName)ctypet).name);
		if(ctype==null)throw new CompileError.UnexpectedToken(ctypet, "const type name");
		
		//thing name
		Token nm = c.nextNonNullMatch(look);
		if (!(nm instanceof Token.BasicName))throw new UnexpectedToken(nm,"name");
		String cname=((BasicName) nm).name;
		//=
		Token asn = c.nextNonNullMatch(Factories.checkForAssignlike);
		if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ASSIGNMENT) {
			//continue
		}else throw new CompileError.UnexpectedToken(asn," = <const-literal>","consts must be assigned");
		// ...
		Const.ConstExprToken value=Const.checkForExpression(c,s, matcher, line, col, ctype);
		if(value.refsTemplate()) {
			// (currently unreachable)
		}
		this.constv=new Const(cname, c.resourcelocation, ctype,this.access, value);
		this.objType=DeclareObjType.CONST;

		if(s.isInFunctionDefine()) {
			if(this.access==Keyword.PUBLIC)
				Warnings.warning("consts declared in functions cannot be public; converted var %s to local;".formatted(constv), c);
			s.getFunction().addConst(this.constv);
		}else if (this.isInThread ){
			if(!s.getThread().add(this, c, s, s.getThreadBlock())) throw new CompileError.DoubleDeclaration(this);
			//if(!c.myInterface.add(this)) throw new CompileError.DoubleDeclaration(this);
			
		}else {
			if(!c.myInterface.add(this)) throw new CompileError.DoubleDeclaration(this);
		}
		c.nextNonNullMatch(Factories.nextIsLineEnd);
		
	}
	private void addVar(Compiler c,Scope s, boolean isReadingHeader, boolean isCompiling) throws CompileError {
		if(isCompiling) return ;//skip
		
		if(s.isInFunctionDefine()) {
			//System.err.printf("local %s . %s created\n", s.getFunction().name,this.variable.name);
			s.getFunction().withLocalVar(this.variable, c);
		}else if (this.isInThread ){
			if(!s.getThread().add(this, c, s, s.getThreadBlock())) throw new CompileError.DoubleDeclaration(this);
			//if(!c.myInterface.add(this)) throw new CompileError.DoubleDeclaration(this);
			
		}else {
			if(!c.myInterface.add(this)) throw new CompileError.DoubleDeclaration(this);
		}
	}

	public static Declaration fromSrc1(Compiler c, Matcher matcher, int line, int col,Keyword access) throws CompileError {
		return header(c, matcher, line, col, access, false);
	}
	public static Declaration fromheader(Compiler c, Matcher matcher, int line, int col,Keyword access) throws CompileError {
		return header(c, matcher, line, col, access, true);
	}
	public static Declaration header(Compiler c, Matcher matcher, int line, int col,Keyword access,boolean isReadingHeader) throws CompileError {
		Declaration d = new Declaration(line,col,c.cursor,access);
		c.dommentCollector=d;
		//typename
		c.cursor=matcher.end();
		boolean isRecursive=false;
		d.isInThread=c.currentScope.hasThread();
		Token keyword2 = c.nextNonNullMatch(Factories.checkForKeyword);
		if(keyword2 instanceof Token.BasicName) {
			Token.BasicName kw2 = (BasicName) keyword2;
			if( Keyword.fromString(kw2.name)==Keyword.CONST) {
				d.addConst(c,c.currentScope, matcher, line, col, access, isReadingHeader,false);
				return d;
			}else if ( Keyword.fromString(kw2.name)==Keyword.RECURSIVE) {
				isRecursive=true;
			}else if ( Keyword.fromString(kw2.name)==Keyword.VOLATILE) {
				d.isVolatile=true;
			}
		}
		

		TemplateDefToken template = TemplateDefToken.checkForDef(c,c.currentScope, matcher);
		Scope typescope=template==null?c.currentScope:c.currentScope.defTemplateScope(template);
		
		Type type=Type.tokenizeNextVarType(c,typescope, matcher, line, col);
		
		
		//test for nonstatic namespace: public retype membtype.fname(...)
		Type thistypetok = Type.checkForVarType(c, typescope, matcher, line, col);VarType thisType=null;
		if(thistypetok!=null) {
			thisType=thistypetok.type;
			Token dot=c.nextNonNullMatch(Factories.genericCheck(Token.Member.factory));
			if(dot instanceof Token.WildChar) throw new CompileError("function name %s alligns with a type name;".formatted(thistypetok.asString()));
		}
		//thing name
		Token t2 = c.nextNonNullMatch(look);
		if (!(t2 instanceof Token.BasicName))throw new UnexpectedToken(t2,"name");
		Token.BasicName varname=(BasicName) t2;
		//CompileJob.compileHdrLog.println("Declaration var/func: %s %s".formatted(type.asString(),varname.asString()));
		
		Token t3 = c.nextNonNullMatch(look);
		
		//? arglist?
		if (t3 instanceof Token.Paren) {
			if (c.currentScope.isInFunctionDefine())throw new CompileError("nested functions not supported");
			if (!((Token.Paren) t3).forward)throw new UnexpectedToken(t3);
			//CompileJob.compileHdrLog.println("Declaration its a function");
			if (BuiltinFunction.BUILTIN_FUNCTIONS.containsKey(varname.name))throw new CompileError("function name %s conflicts with a builtin function on line %d column %d.".formatted(varname.line,varname.col));
			d.function=new Function(varname.asString(),type.type,thisType,access,c,isRecursive);
			d.function.withTemplate(template);
			//Scope subscope=c.currentScope.subscope(d.function);
			
			//arglist!
			if(!d.isNextCloseParen(c, matcher, line, col))while(true) {
				boolean isRef=d.isArgRef(c, matcher, line, col);
				Type pt=Type.tokenizeNextVarType(c,typescope, matcher, line, col);
				Token pname=c.nextNonNullMatch(look);
				if (!(pname instanceof Token.BasicName))throw new UnexpectedToken(pname,"name");
				d.function.withArg(new Variable(((Token.BasicName)pname).name, pt.type, Keyword.PRIVATE, c), c,isRef);
				Token term=c.nextNonNullMatch(look);
				if(term instanceof ArgEnd)continue;
				else if ((!(term instanceof Token.Paren)) ||((Token.Paren)term).forward) throw new CompileError.UnexpectedToken(term,")");
				else if (c.cursor>=matcher.regionEnd())throw new CompileError.UnexpectedFileEnd(term.line);
				else break;
			}
			d.objType=DeclareObjType.FUNC;
			for(Variable p:d.function.args) {
				//CompileJob.compileHdrLog.println("\t parameter: %s %s".formatted(p.type.asString(),p.name));
				
			}
			//final before -> or export

			if(d.function.hasThis()) {
				//check for final suffix
				int start=c.cursor;
				Token t=c.nextNonNullMatch(Factories.checkForKeyword);
				if(t instanceof Token.BasicName) {
					String kw=((Token.BasicName) t).name;
					if(Keyword.fromString(kw)==Keyword.FINAL) {
						d.function.self.makeFinalThis();
					}else c.cursor=start;
				}
			}
			if(access==Keyword.EXTERN) {
				//-> statement for resourcelocation
				Token asn = c.nextNonNullMatch(look);
				if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.MASK) {
					asn=c.nextNonNullMatch(lookMaskHolder);//not looking for consts; and shouldnt ;
					if(!(asn instanceof ResourceLocation.ResourceToken))throw new CompileError.UnexpectedToken(asn, "resource location");
					d.function.setResourceLocation(((ResourceLocation.ResourceToken)asn).res);
					int precursor=c.cursor;
					asn=c.nextNonNullMatch(lookMaskOp);
					if(asn instanceof Token.TagOf) {
						//optional
						//CompileJob.compileHdrLog.println("custom func name");
						asn=c.nextNonNullMatch(look);
						if (!(asn instanceof Token.BasicName)) throw new CompileError.UnexpectedToken(asn,"normal function name");
						d.function.withMCFName(((Token.BasicName)asn).name);
					}else {
						c.cursor=precursor;
					}

				}else throw new CompileError.UnexpectedToken(asn, "->");
			}else {
				//template auto-requests
				Token bind = c.nextNonNullMatch(Factories.checkForKeyword);
				while (bind instanceof Token.BasicName) {
					if(Keyword.fromString(bind.asString())==Keyword.EXPORT) {
						TemplateArgsToken targs = TemplateArgsToken.checkForArgs(c, typescope, matcher);
						if(targs==null) new CompileError("expected template args to export");
						//d.function.requestTemplate(targs, typescope); //this is later
						bind = c.nextNonNullMatch(Factories.checkForKeyword);
					}else throw new CompileError.UnexpectedToken(bind, "export, ';', or '{'");
				}
			}
			if(!c.myInterface.add(d)) throw new CompileError.DoubleDeclaration(d);
			// {...}
			//in header, just skip;
			Token term=c.nextNonNullMatch(look);
			if(d.access==Keyword.EXTERN || isReadingHeader) {
				//no code blocks for extners / funcs in header
				//;

				if(!(term instanceof Token.LineEnd))throw new CompileError.UnexpectedToken(term,";");
				//else good
			}else {
				if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
				//{...}
				//let Compiler.... handle it
				
				//c.currentScope=c.currentScope.subscope(d.function);
				d.defineScope=c.currentScope.subscope(d.function);
				//this is a recent change; it will add scopes to childeren on both pass 1 and 2, but the scope
				// from pass 1 is tossed (and flow-blocks do this) so it should have no affect
			}
			return d;
		}else {
			if (type.type.isVoid())throw new CompileError("unexpected type void for variable %s on line %d col %d.".formatted(varname.name,varname.line,varname.col));
			if(template!=null)throw new CompileError ("varaible definition cannot contain template;");
			if(thisType!=null) throw new CompileError("variable definition cannot be a type-member;");
			//CompileJob.compileHdrLog.println("Declaration its a variable");
			boolean isFuncLocal=c.currentScope.isInFunctionDefine();
			;
			if (isFuncLocal && d.access==Keyword.PUBLIC) {
				Warnings.warning("vars declared in functions cannot be public; converted var %s to local;".formatted(varname.asString()), c);
			}
			d.variable=new Variable(varname.asString(),type.type, access, c);
			if(type.type.isStruct()) d.variable = type.type.struct.varInit(d.variable, c, c.currentScope);
			Function localOf=null;
			if(isFuncLocal) {
				localOf = c.currentScope.getFunction();
				d.variable.localOf(localOf);
			}else if (d.isInThread) {
				//later
			}
			d.objType=DeclareObjType.VAR;
			if (t3 instanceof Token.LineEnd) {
				//if(!c.myInterface.add(d)) throw new CompileError.DoubleDeclaration(d);
				d.addVar(c, c.currentScope, isReadingHeader, false);
				return d;//end early
			}
			Token asn = t3;//c.nextNonNullMatch(look);
			//? ->
			if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.MASK) {
				d.applyMask(c, matcher, line, col, access, false);
				asn = c.nextNonNullMatch(look);
				d.hdrHasMask=true;
			}
			if (asn instanceof Token.LineEnd) {
				//if(!c.myInterface.add(d)) throw new CompileError.DoubleDeclaration(d);
				d.addVar(c, c.currentScope, isReadingHeader, false);
				return d;//end early
			}
			if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ASSIGNMENT) {
				d.hdrHasAssign = true;
			}
			
			//skip? ~~
			//then:
			c.nextNonNullMatch(Factories.headerSkipline);
			//if(!c.myInterface.add(d)) throw new CompileError.DoubleDeclaration(d);
			d.addVar(c, c.currentScope, isReadingHeader, false);
			
			return d;
		}
	}
	static Token compileToken(Compiler c, Matcher matcher, int line, int col,Keyword access) throws CompileError {
		if (access==Keyword.EXTERN) {
			c.nextNonNullMatch(Factories.headerSkipline);
			return null;//ignore if in header file
		}
		Declaration d = new Declaration(line,col,c.cursor,access);
		c.dommentCollector=DommentCollector.Dump.INSTANCE;
		//typename
		c.cursor=matcher.end();
		Token keyword2 = c.nextNonNullMatch(Factories.checkForKeyword);
		boolean isRecursive=false;
		if(keyword2 instanceof Token.BasicName) {
			Token.BasicName kw2 = (BasicName) keyword2;
			if( Keyword.fromString(kw2.name)==Keyword.CONST) {
				d.addConst(c,c.currentScope, matcher, line, col, access, false,true);
				return null;//ignore if in header file
				//return d;
			}else if ( Keyword.fromString(kw2.name)==Keyword.RECURSIVE) {
				isRecursive=true;
			}else if ( Keyword.fromString(kw2.name)==Keyword.VOLATILE) {
				d.isVolatile=true;
			}
		}
		

		TemplateDefToken template = TemplateDefToken.checkForDef(c,c.currentScope, matcher);
		Scope typescope=template==null?c.currentScope:c.currentScope.defTemplateScope(template);
		
		Type type=Type.tokenizeNextVarType(c,typescope, matcher, line, col);
		
		//test for nonstatic namespace: public retype membtype.fname(...)
		Type thistypetok = Type.checkForVarType(c, typescope, matcher, line, col);VarType thisType=null;
		if(thistypetok!=null) {
			thisType=thistypetok.type;
			Token dot=c.nextNonNullMatch(Factories.genericCheck(Token.Member.factory));
			if(dot instanceof Token.WildChar) throw new CompileError("function name %s alligns with a type name;".formatted(thistypetok.asString()));
		}
		//thing name
		Token t2 = c.nextNonNullMatch(look);
		if (!(t2 instanceof Token.BasicName))throw new UnexpectedToken(t2,"name");
		Token.BasicName varname=(BasicName) t2;
		//CompileJob.compileMcfLog.println("compile: Declaration var/func: %s %s".formatted(type.asString(),varname.asString()));
		
		Token t3 = c.nextNonNullMatch(look);
		
		//? arglist?
		if (t3 instanceof Token.Paren) {
			if (c.currentScope.isInFunctionDefine())throw new CompileError("nested functions not supported");
			if (!((Token.Paren) t3).forward)throw new UnexpectedToken(t3);
			//CompileJob.compileMcfLog.println("Declaration its a function");
			if (BuiltinFunction.BUILTIN_FUNCTIONS.containsKey(varname.name))throw new CompileError("function name %s conflicts with a builtin function on line %d column %d.".formatted(varname.line,varname.col));
			//d.function=new Function(varname.asString(),type.type,access,c);
			///this remains the same
			d.function=c.myInterface.identifyFunction(varname.name,c.currentScope);
			//arglist!
			if(!d.isNextCloseParen(c, matcher, line, col))while(true) {
				boolean isRef=d.isArgRef(c, matcher, line, col);//skip the keyword
				Type pt=Type.tokenizeNextVarType(c,typescope, matcher, line, col);//check subscope for type consts
				Token pname=c.nextNonNullMatch(look);
				if (!(pname instanceof Token.BasicName))throw new UnexpectedToken(pname,"name");
				//d.function.withArg(new Variable(((Token.BasicName)pname).name, pt.type, Keyword.PRIVATE, c), c);
				Token term=c.nextNonNullMatch(look);
				if(term instanceof ArgEnd)continue;
				else if ((!(term instanceof Token.Paren)) ||((Token.Paren)term).forward) throw new CompileError.UnexpectedToken(term,")");
				else if (c.cursor>=matcher.regionEnd())throw new CompileError.UnexpectedFileEnd(term.line);
				else break;
			}
			d.objType=DeclareObjType.FUNC;
			for(Variable p:d.function.args) {
				//CompileJob.compileMcfLog.println("\t parameter: %s%s %s".formatted(p.isReference()?"ref ":"" ,p.type.asString(),p.name));
				
			}
			//final before -> and export
			if(d.function.hasThis()) {
				//check for final suffix
				int start=c.cursor;
				Token t=c.nextNonNullMatch(Factories.checkForKeyword);
				if(t instanceof Token.BasicName) {
					String kw=((Token.BasicName) t).name;
					if(Keyword.fromString(kw)==Keyword.FINAL) {
						//d.function.self.makeFinalThis();
					}else c.cursor=start;
				}
			}
			if(access==Keyword.EXTERN) {
				//-> statement for resourcelocation
				Token asn = c.nextNonNullMatch(look);
				if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.MASK) {
					asn=c.nextNonNullMatch(lookMaskHolder);
					if(!(asn instanceof ResourceLocation.ResourceToken))throw new CompileError.UnexpectedToken(asn, "resource location");
					//d.function.setResourceLocation(((ResourceLocation.ResourceToken)asn).res);
					int precursor=c.cursor;
					asn=c.nextNonNullMatch(lookMaskOp);
					if(asn instanceof Token.TagOf) {
						//optional
						//CompileJob.compileMcfLog.println("custom func name");
						asn=c.nextNonNullMatch(look);
						if (!(asn instanceof Token.BasicName)) throw new CompileError.UnexpectedToken(asn,"normal function name");
						//d.function.withMCFName(((Token.BasicName)asn).name);
					}else {
						c.cursor=precursor;
					}

				}else throw new CompileError.UnexpectedToken(asn, "->");
			}else {
				//template auto-requests
				Token bind = c.nextNonNullMatch(Factories.checkForKeyword);
				while (bind instanceof Token.BasicName) {
					if(Keyword.fromString(bind.asString())==Keyword.EXPORT) {
						TemplateArgsToken targs = TemplateArgsToken.checkForArgs(c, typescope, matcher);
						if(targs==null) new CompileError("expected template args to export");
						d.function.requestTemplate(targs, typescope);
						bind = c.nextNonNullMatch(Factories.checkForKeyword);
					}else throw new CompileError.UnexpectedToken(bind, "export, ';', or '{'");
				}
			}
			// {...}
			Token term=c.nextNonNullMatch(look);
			if(d.access==Keyword.EXTERN) {
				//;

				if(!(term instanceof Token.LineEnd))throw new CompileError.UnexpectedToken(term,";");
				//else good
			}else {
				if((!(term instanceof Token.CodeBlockBrace)) || (!((Token.CodeBlockBrace)term).forward))throw new CompileError.UnexpectedToken(term,"{");
				//{...}
				d.defineScope=c.currentScope.subscope(d.function);
				//c.currentScope=d.defineScope;
				//should be good from here
			}
		}else {
			if (type.type.isVoid())throw new CompileError("unexpected type void for variable %s on line %d col %d.".formatted(varname.name,varname.line,varname.col));

			if(thisType!=null) throw new CompileError("variable definition cannot be a type-member;");
			//if (c.currentScope.isInFunctionDefine())Warnings.warning("vars declared in functions are not supported, will not act like local vars, and could misbehave.");
			//d.variable=new Variable(varname.asString(),type.type, access, c);
			d.variable=c.myInterface.identifyVariable(varname.name,c.currentScope);
			d.objType=DeclareObjType.VAR;
			if (t3 instanceof Token.LineEnd) {
				return d;//end early
			}
			Token asn = t3;//c.nextNonNullMatch(look);
			//? ->
			if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.MASK) {
				d.applyMask(c, matcher, line, col, access, false);
				asn = c.nextNonNullMatch(look);
			}
			if (asn instanceof Token.LineEnd) {
				return d;//end early
			}
			
			//? ~~
			if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ESTIMATE) {
				Token est;
				/*est=c.nextNonNullMatch(lookCompiletime);
				if(!(est instanceof Num)) {
					est=Num.tokenizeNextNumNonNull(c,typescope, matcher, line, col);
				}*/
				est=Num.tokenizeNextNumNonNull(c,typescope, matcher, line, col);
				if(!(est instanceof Num)) throw new CompileError.UnexpectedToken(est, "number");
				d.estimate=((Num)est).value;
				asn = c.nextNonNullMatch(look);
				c.currentScope.addEstimate(d.variable, d.estimate);
			}
			if (asn instanceof Token.LineEnd) {
				return d;//end early
			}
			//? =
			if(asn instanceof Token.Assignlike && ((Assignlike)asn).k==Kind.ASSIGNMENT) {
				Equation eq=Equation.toAssign(line, col, c, c.currentScope, matcher);
				//asn = c.nextNonNullMatch(look);
				//equation finds the semicolon;
				d.assignment=eq;
			}else
				c.nextNonNullMatch(Factories.headerSkipline);
		}
		
		return d;
	}
	public final Keyword access;
	public static enum DeclareObjType{
		VAR,FUNC,CONST;
	}
	//bool + 1 obj + 1 null
	DeclareObjType objType;
	Function function;
	Scope defineScope=null;
		@Override public boolean didOpenCodeBlock() {
			return this.defineScope!=null;
		} @Override public Scope getNewScope() {
			return this.defineScope;
		}
	Variable variable;
		//mask is part of variable
	//seperate:
		//estimate
		//assignment
	Equation assignment=null;
	Number estimate=null;
	
	boolean isConst=false; 
	Const constv=null;
	Const.ConstLiteralToken constValue=null;
	
	boolean isInThread = false;
	public boolean hdrHasAssign = false;
	public boolean hdrHasMask = false;
	public boolean isVolatile = false;
	
	public boolean isFunction() {return this.objType==DeclareObjType.FUNC;}
	public boolean isVariable() {return this.objType==DeclareObjType.VAR;}
	public boolean isConst() {return this.objType==DeclareObjType.CONST;}
	public Function getFunction() {return this.function;}
	public Variable getVariable() {return this.variable;}
	public Const getConst() {return this.constv;}
	public boolean isPublic() {return this.access==Keyword.PUBLIC;}
	boolean willTakeDomments=true;
	final ArrayList<Domment> domments=new ArrayList<Domment>(); @Override public void addDomment(Domment dom) {
		if(this.willTakeDomments)domments.add(dom);
	}
	public Declaration(int line, int col,int cursor,Keyword type) {
		super(line, col, cursor);
		this.access=type;
	}

	@Override
	public void compileMe(PrintStream f,Compiler c,Scope s) throws CompileError {
		//do nothing
		if(this.objType==DeclareObjType.VAR && this.assignment!=null) {
			for(Domment d:this.domments) f.println(d.inCMD());//only if it is assigned on define
			Assignment t=new Assignment(this.line,this.col,this.myCursor,this.variable,this.assignment);
			t.compileMe(f, c, s);
		}
		//estimate does nothing
		
	}
	@Override
	public boolean doHeader() {
		return this.access==Keyword.PUBLIC && (!this.isInThread);
	}
	@Override
	public void headerMe(PrintStream f) throws CompileError {
		for(Domment d:this.domments) f.println(d.inHeader());
		switch(this.objType) {
		case CONST:
			this.constv.headerDeclaration(f);
			break;
		case FUNC:
			f.printf("public %s;\n",this.function.toHeader());
			break;
		case VAR:
			f.printf("public %s;\n".formatted(this.variable.toHeader()));
			break;
		default:
			throw new CompileError("null objType in declaration");
		
		}
		
	}
	@Override public void printStatementTree(PrintStream p,int tabs) {
		super.printStatementTree(p, tabs);
		StringBuffer s=new StringBuffer();while(s.length()<tabs)s.append('\t');
		if(this.estimate!=null) {
			p.printf("%s\t~~%s,\n", s,this.estimate);
		}
		if(this.assignment!=null) {
			p.printf("%s\t=:\n", s,this.estimate);
			this.assignment.printTree(p, tabs+1);
			
		}
	}
	@Override
	public String asString() {
		return "<statement-declaration %s %s>"
				.formatted(this.estimate!=null?"~~...":"",
						this.assignment!=null?"=...":"");
	}

	@Override
	public void addToStartOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
		if(this.objType==DeclareObjType.FUNC) {
			this.function.allocateMyLocalsCallInside(p);
		}
	}
	@Override
	public void addToEndOfMyBlock(PrintStream p, Compiler c, Scope s) throws CompileError {
		//do nothing
		if(this.objType==DeclareObjType.FUNC) {
			this.function.deallocateLocalAfterCallInside(p);
		}
	}
	public DeclareObjType getObjType() {
		return objType;
	}
	

}
