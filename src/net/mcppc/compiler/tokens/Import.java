package net.mcppc.compiler.tokens;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

import net.mcppc.compiler.*;
import net.mcppc.compiler.Compiler;
import net.mcppc.compiler.errors.CompileError;
import net.mcppc.compiler.tokens.Token.Assignlike.Kind;
/**
 * imports stuff from another file; optionally specify an alias for it; will not run function unless specified
 * syntax:
 * import [if] [run] [<alias> ->] <resourcelocation>;
 * 
 * default alias is the file-name part of path:  name -> ns:folder/dir/name
 * @author RadiumE13
 *
 */
public class Import extends Statement implements Statement.Headerable,DommentCollector {
	static final Token.Factory[] look = Factories.genericLookPriority(new Token.Factory[] 
			{ResourceLocation.ResourceToken.factory,Token.BasicName.factory}, 
			Token.Assignlike.factoryMask,
			Token.LineEnd.factory);

	public static Import header(Compiler c, Matcher matcher, int line, int col, boolean isHeaderOnly) throws CompileError {
		return interperet(c, matcher, line, col, isHeaderOnly,false);
	}

	public static Import fromSrc(Compiler c, Matcher matcher, int line, int col) throws CompileError {
		return interperet(c, matcher, line, col, false,true);
		
	}
	public static Import interperet(Compiler c, Matcher matcher, int line, int col, boolean isHeaderOnly,boolean isMcfCompiling) throws CompileError {
		if (isHeaderOnly) {
			//c.nextNonNullMatch(Factories.headerSkipline);
			//return null;//ignore if in header file
		}
		c.cursor=matcher.end();
		boolean isStrict=false;
		Token ift=c.nextNonNullMatch(Factories.checkForKeyword);
		if(ift instanceof Token.BasicName) {
			if(Keyword.fromString(ift.asString())==Keyword.IF) {
				isStrict=true;
			}else throw new CompileError.UnexpectedToken(ift, "keyword 'if' or lib name/path");
		}
		DommentCollector.DList dms=new DommentCollector.DList();
		if(!isHeaderOnly)c.dommentCollector=dms;
		Token a=c.nextNonNullMatch(look);
		String alias=null;
		boolean run=false;
		if (a instanceof BasicName && ((BasicName) a).name.equals("run")) {
			//custom alias
			CompileJob.compileHdrLog.printf("import run\n");
			run=true;
			a=c.nextNonNullMatch(look);
		}
		if (a instanceof BasicName) {
			//custom alias
			alias=((BasicName) a).name;
			CompileJob.compileHdrLog.printf("import alias '%s'\n",((BasicName) a).name);
			a=c.nextNonNullMatch(look);
			if(!(a instanceof Token.Assignlike) || ((Token.Assignlike)a).k!=Kind.MASK)throw new CompileError.UnexpectedToken(a, "->");
			a=c.nextNonNullMatch(look);
		}
		Token r=a;
		if (!(r instanceof ResourceLocation.ResourceToken))throw new CompileError.UnexpectedToken(a, "resourcelocation");
		ResourceLocation res=((ResourceLocation.ResourceToken) r).res;
		if(alias==null)alias=res.end;
		Import i=new Import(line,col,c.cursor,res,alias,dms.list,run, isStrict);
		Token end=c.nextNonNullMatch(look);
		if(!(end instanceof Token.LineEnd))new CompileError.UnexpectedToken(a, "';'");
		if(isMcfCompiling)return i;
		else if(isHeaderOnly) {
			//only if not strict
			if(!isStrict)c.job.addPossibleExternalDependancy(i);
			return null;
		}
		else if(c.myInterface.add(i))
			return i;
		else throw new CompileError.DoubleDeclaration(i);
		
	}
	final ResourceLocation lib;public ResourceLocation getLib() {return this.lib;}
	final String alias;public String getAlias() {return this.alias;}
	final boolean run;
	public final boolean isStrict;
	public Import(int line, int col,int cursor, ResourceLocation lib,String alias, List<Domment> d,boolean run, boolean isStrict) {
		super(line, col, cursor);
		this.lib=lib;
		this.alias=alias;
		this.dms=d;
		this.run=run;
		this.isStrict=isStrict;
	}
	public Import(int line, int col,ResourceLocation lib, String alias, int cursor) {
		this(line, col,cursor,lib,alias,new ArrayList<Domment>(),false, false);
	}
	public Import(int line, int col,ResourceLocation lib, int cursor) {
		this(line, col,lib,lib.end, cursor);
	}

	@Override
	public void compileMe(PrintStream f,Compiler c,Scope s) {
		if(this.run) {
			for(Domment d:this.dms)f.println(d.inCMD(s.getTarget()));
			this.lib.run(f,s.getTarget());
		}

	}
	@Override
	public String asString() {
		return "import ...";
	}
	@Override
	public boolean doHeader() {
		//prints to header; is not read from header
		return true;
	}
	@Override
	public void headerMe(PrintStream f) throws CompileError {
		for(Domment d:this.dms)f.println(d.inHeader());
		String st=this.isStrict?"if ":"";
		String r=this.run?"run ":"";
		if(this.alias.equals(this.lib.end))
			f.printf("import %s%s%s;\n",st,r, this.lib.toString());
		else 
			f.printf("import %s%s%s -> %s;\n",st,r,this.alias, this.lib.toString());
		
	}
	final List<Domment> dms;
	@Override
	public void addDomment(Domment dom) {
		this.dms.add(dom);
	}

	public boolean willRun() {
		return run;
	}

}
