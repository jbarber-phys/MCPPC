package net.mcppc.compiler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.*;
import net.mcppc.compiler.errors.*;
import net.mcppc.compiler.tokens.*;
import net.mcppc.compiler.tokens.Statement.CodeBlockOpener;
import net.mcppc.compiler.tokens.Statement.Domment;
import net.mcppc.compiler.tokens.Statement.Headerable;
import net.mcppc.compiler.tokens.Statement.MultiFlow;

public class Compiler{
	private File src;
	private File hdr;
	private File mcf;
	public int cursor=0;
	int lineStart=0;
	int line=1;
	public int line() {return line;}
	public int column() {return cursor-lineStart;}
	/**
	 * the text of this source file
	 * make sure this stays private and never pass a reference to this to anything outside Compiler;
	 * Note: this.matcher may possess a reference; Matcher hides its reference to content so no need to worry;
	 */
	private StringBuffer content;
	
	
	public FileInterface myInterface;
	Matcher matcher;
	public ResourceLocation resourcelocation;
	public CompileJob.Namespace namespace;
	public final CompileJob job;//reference to parent
	public Scope baseScope=null; 
	public Scope currentScope=null; 
	/**
	 * true if the content contains the source currently
	 */
	boolean isSrcLoaded() {
		return this.content!=null;
	}
	boolean isHeaderOnly=false;
	boolean hasMadeHeader=false;
	boolean hasCompiled=false;
	boolean isStrict=false;
	/**
	 * true if the file has loaded its private and public members
	 */
	boolean areLocalsLoaded=false;

	public DommentCollector dommentCollector=null;
	
	//IO streams
	private Scanner scan;//other mechanisms exist 
	
	private PrintStream hout=null;
	private PrintStream mcfout=null;
	
	public void close() {
		//used in a finally block to close all open files
		if(scan!=null) scan.close();
		if(this.hout!=null)hout.close();
		if(this.mcfout!=null)mcfout.close();
		if(this.baseScope!=null 
				) {
			this.baseScope.closeFiles();
		}
		
	}
	public void newLine(int index) {
		this.lineStart=index;
		this.line++;
	}
	public Compiler (CompileJob job,ResourceLocation res,CompileJob.Namespace n) {
		this(job,res,n,false);
	}
	public Compiler (CompileJob job,ResourceLocation res,CompileJob.Namespace n,boolean hdrOnly) {
		this.resourcelocation=res;
		this.job=job;
		this.isHeaderOnly=hdrOnly;
		this.namespace=n;
		this.makeFiles();
		CompileJob.compileMcfLog.printf("new compiler at %s;\n", res);
	}
	private void makeFiles() {
		this.src=job.pathForSrc(this.resourcelocation).toFile();
		if(this.isHeaderOnly)this.hdr=job.pathForInclude(this.resourcelocation).toFile();
		else this.hdr=job.pathForHeaderOut(this.resourcelocation).toFile();
		
		this.mcf=job.pathForMcf(this.resourcelocation).toFile();
	}
	public void loadSrc() throws FileNotFoundException, CompileError {
		if(this.isHeaderOnly)loadH();
		else loadF(this.src);
	}
	public void loadH() throws FileNotFoundException {
		loadF(this.hdr);
	}
	private void loadF(File f) throws FileNotFoundException {
		scan=new Scanner(f);
		content=new StringBuffer();
		while (scan.hasNext()) {
			scanLine();
		}
		scan.close();
	}
	public void unload() {
		//remove reference to this.content from matcher
		this.matcher.reset();
		content=null;
		//entice garbage collectpr. Note: JVM may do this on its own
		System.gc();
	}
	public void readHeader() throws CompileError {
		
	}
	public PrintStream matchOut=OutputDump.out;
	@Deprecated private Token nextMatch(Token.Factory[] mode) throws CompileError {
		if(cursor>= matcher.regionEnd()) throw new CompileError("unexpected end of source");
		matcher.region(cursor, matcher.regionEnd());
		for (Token.Factory f: mode) {
			matcher.usePattern(f.pattern);
			if(matcher.lookingAt()) {
				return f.createToken(this, matcher, line, cursor);
			}
		}
		throw new CompileError("compile1: line %d start character %s not recognized"
				.formatted(line,this.content.charAt(cursor)));
	}
	public Token nextNonNullMatch(Token.Factory[] mode) throws CompileError {
		return nextNonNullMatch(mode,false);
	}
	public Token nextNonNullMatch(Token.Factory[] mode,boolean canEnd) throws CompileError {
		//TODO add arg to push domments to
		if(cursor>= matcher.regionEnd()) throw new CompileError("unexpected end of source");
		Token t=null;
		matchOut.println("nextNonNullMatch line %d col %d".formatted(this.line,this.column()));
		int prevcursor=-1;
		while ((t==null ) && this.cursor<matcher.regionEnd()) {
			matchOut.println("next match from %d : %d.".formatted(this.line,this.column()));
			boolean match=false;
			if(prevcursor==this.cursor)throw new CompileError("unexpected infinite loop in nextNonNullMatch");
			prevcursor=this.cursor;
			thefor:for (Token.Factory f: mode) {
				matcher.region(cursor, matcher.regionEnd());
				matcher.usePattern(f.pattern);
				if(matcher.lookingAt()) {
					matchOut.println("found '%s'".formatted(matcher.group()));
					
					t= f.createToken(this, matcher, line, cursor-this.lineStart);
					if (t!=null) {
						if (this.dommentCollector!=null && t instanceof Statement.Domment) {
							this.dommentCollector.addDomment((Domment)t);
							match=true;
							t=null;
							break thefor;
						}
						else return t;
					}
					else {match=true;break thefor;}
				}
			}
			if(!match) {
				String s=(this.content.length()-10>this.cursor)?this.content.substring(this.cursor, this.cursor+10)
						: this.content.substring(this.cursor)+"<EOF>";
				throw new CompileError("no recognized token found with pattern to match '%s'... line %d col %d.".formatted(s,this.line,this.cursor-this.lineStart));
			}

		}
		if(canEnd)return null;
		throw new CompileError("Unexpected end of file");
		
	}
	/**
	 * fast-tokenizes (does not full tokenizes) and generates header for file
	 * also generates variable information (FileInterface)
	 * @throws FileNotFoundException 
	 */
	public void compile1(boolean genHeader) throws CompileError, FileNotFoundException {
		if (this.hasMadeHeader)return;
		if (!isSrcLoaded()) {
			if(this.isHeaderOnly) this.loadH();
			else  this.loadSrc();
		}
		CompileJob.compileHdrLog.println("making header for '%s'".formatted(this.resourcelocation));
		baseScope=currentScope=new Scope(this);
		cursor=0;
		line=1;
		lineStart=0;
		this.myInterface=new FileInterface(this.resourcelocation);
		this.matcher=Factories.headerLnStart[0].pattern.matcher(this.content);
		ArrayList<Statement> headerlines=new ArrayList<Statement>();
		ArrayList<Statement.Domment> domments=new ArrayList<Statement.Domment>();
		DommentCollector dc=new DommentCollector() {
			@Override
			public void addDomment(Domment dom) {
				domments.add(dom);
			}
		};
		this.dommentCollector=dc;
		boolean prevHeaderable = false;
		comploop: while(cursor<this.content.length()) {
			int startline=this.line;
			//domments go before their statement
			Token sm=this.nextNonNullMatch(Factories.headerLnStart,true);
			if (sm==null)break comploop;
			if(sm instanceof Token.CodeBlockBrace) {
				if(((Token.CodeBlockBrace)sm).forward) {
					//shoudl have been handled by previous statement
					throw new CompileError.UnexpectedToken(sm,"'{' or line;","previous statement should have handled any forward braces");
				}else {
					//end block
					if(this.currentScope.parent==null) {
						//may leave early
						domments.clear();
						this.dommentCollector=dc;
						//continue;
						throw new CompileError.UnexpectedToken(sm,"'}' or line;","found outside of scope");
						
					}
					CompileJob.compileHdrLog.println("header: block end: '%s', line %d col %d.".formatted(sm.asString(),this.line,this.cursor-this.lineStart));
						
					this.currentScope=this.currentScope.superscope();
					continue comploop;
				}
			}
			CompileJob.compileHdrLog.println("statement: '%s', line %d col %d.".formatted(sm.asString(),this.line,this.cursor-this.lineStart));
			for(Statement.Domment d:domments) {
				if (d.line<=startline+1 && prevHeaderable)headerlines.add(d);//add domment only if it is before / after headerable line
				prevHeaderable=sm instanceof Statement.Headerable? ((Statement.Headerable)sm).doHeader():false;
				if ( d.line >=sm.line-2 && prevHeaderable)headerlines.add(d);//add domment only if it is before / after headerable line
			}
			domments.clear();
			this.dommentCollector=dc;
			if(sm instanceof Statement)headerlines.add((Statement) sm);
			if(sm instanceof Statement.CodeBlockOpener && ((Statement.CodeBlockOpener) sm).didOpenCodeBlock()) {
				this.currentScope=((Statement.CodeBlockOpener) sm).getNewScope();
				//thats it
			}
		}
		this.areLocalsLoaded=true;
		//FileOutputStream fos=new FileOutputStream(hdr, false);//second arg is if append
		
		if(genHeader && !(this.isHeaderOnly)) {
			try {
				hdr.getParentFile().mkdirs();
				hdr.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			this.hout=new PrintStream(hdr);
			CompileJob.compileHdrLog.printf("writing %d statements to header.\n",headerlines.size());
			for(Statement s:headerlines) {
				CompileJob.compileHdrLog.printf("header writing: %s\n",s.asString());
				if(s instanceof Headerable && ((Headerable)s).doHeader()) ((Headerable)s).headerMe(hout);
			}
			hout.println("// i was here");
			//hout.flush();//i dont know if this shoudl be done
			hout.close();//all done
			this.hasMadeHeader=true;
		}
		
		//keep source loaded
	}
	private void scanLine() {
		content.append(scan.nextLine()+"\n");
	}
	private CodeBlock BuildCodeBlock(CodeBlock block) throws CompileError {
		return BuildCodeBlockOrCode(block,null,true);
	}
	private List<Statement> CompileCodeNoScope() throws CompileError {
		List<Statement> compiledLines=new ArrayList<Statement>();
		BuildCodeBlockOrCode(null,compiledLines,false);
		return compiledLines;
	}
	/**
	 * tokenization routine for global and more local scopes
	 * @param block
	 * @param compiledLines
	 * @param isInCodeBlock
	 * @return
	 * @throws CompileError
	 */
	private CodeBlock BuildCodeBlockOrCode(CodeBlock block, List<Statement> compiledLines, boolean isInCodeBlock) throws CompileError {
		List<Statement.Domment> domments=new ArrayList<Statement.Domment>();
		DommentCollector dc=new DommentCollector() {
			@Override
			public void addDomment(Domment dom) {
				domments.add(dom);
			}
		};
		this.dommentCollector=dc;
		if(isInCodeBlock)CompileJob.compileMcfLog.printf("new block for %s\n", block.scope.getSubRes());
		MultiFlow flowPred=null;
		comploop: while(cursor<this.content.length()) {
			//domments go before statements
			Token sm=this.nextNonNullMatch(Factories.compileLnStart,!isInCodeBlock);
			if(isInCodeBlock) {
				if (sm==null)throw new CompileError("unexpected end of file inside code block; missing a '}'");
			}else {
				if (sm==null)break comploop;
			}
			if(isInCodeBlock)CompileJob.compileMcfLog.println("%s::statement: '%s', line %d col %d.".formatted(block.scope.getSubRes().end,sm.asString(),this.line,this.cursor-this.lineStart));
			else CompileJob.compileMcfLog.println("statement: '%s', line %d col %d.".formatted(sm.asString(),this.line,this.cursor-this.lineStart));;
			for(Statement.Domment d:domments) {
				if(isInCodeBlock)block.addStatement(d);//add domment always
				else compiledLines.add(d);
			}
			domments.clear();
			this.dommentCollector=dc;
			if(sm instanceof Token.CodeBlockBrace) {
				if(isInCodeBlock) {
					if(((Token.CodeBlockBrace)sm).forward) {
						//shoudl have been handled by previous statement
						throw new CompileError.UnexpectedToken(sm,"'}' or line;","previous statement should have handled any open braces");
					}else {
						//end block
						this.currentScope=this.currentScope.superscope();
						CompileJob.compileMcfLog.printf("ended block for %s\n", block.scope.getSubRes());
						break comploop;
					}
				}else {
					throw new CompileError.UnexpectedToken(sm, "statement", "extra '}' found;");
				}
				
			}
			if(isInCodeBlock) block.addStatement((Statement) sm);
			else compiledLines.add((Statement) sm);
			if(sm instanceof Statement.CodeBlockOpener && ((Statement.CodeBlockOpener) sm).didOpenCodeBlock()) {
				this.currentScope=((Statement.CodeBlockOpener) sm).getNewScope();
				CodeBlock subblock=new CodeBlock(this.line,this.column(),this.currentScope,(CodeBlockOpener) sm);
				this.BuildCodeBlock(subblock);
				this.dommentCollector=dc;
				if(isInCodeBlock)block.addStatement(subblock);
				else compiledLines.add(subblock);
				if(sm instanceof Statement.MultiFlow) {
					if(flowPred!=null && ((Statement.MultiFlow)sm).recive())
						if(!((MultiFlow) sm).setPredicessor(flowPred)) 
							throw new CompileError("%s cannot be sent as predicessor to a %s statement;"
									.formatted(flowPred.getFlowType(),((MultiFlow) sm).getFlowType()));
					
					if(flowPred==null && ((Statement.MultiFlow)sm).sendForward())flowPred=(MultiFlow) sm;
					else if(flowPred!=null && ((Statement.MultiFlow)sm).sendForward()&& ((Statement.MultiFlow)sm).claim())flowPred=(MultiFlow) sm;
					if(!((Statement.MultiFlow)sm).sendForward())flowPred=null;
				}
			}
		}
		return block;
	}
	public void compileLine(PrintStream p,Scope s,Statement stm) throws CompileError {

		if(this.job.isDebug()) {
			int line = stm.line;
			p.printf("### %s :: %d \n", this.resourcelocation,line);
		}
		stm.compileMe(p, this, s);
	}
	/**
	 * fully tokenizes
	 * takes the token tree and generates the mcfunction code
	 * @throws FileNotFoundException 
	 * 
	 */
	public void compile2() throws CompileError, FileNotFoundException  {
		if (this.hasCompiled)return;
		if (this.isHeaderOnly) {
			Warnings.warning("attempted to true-compile header-only %s; skipped compilation;".formatted(this.resourcelocation));
			return;
		}
		if(!this.areLocalsLoaded)throw new CompileError("attempted to comp2 file %s before locals were loaded.".formatted(this.resourcelocation));
		if (!isSrcLoaded())this.loadSrc();
		if(this.myInterface==null) {
			CompileJob.compileHdrLog.println("not yet implimented re-reading self-interface");
		}
		this.myInterface.forceLoadLibs(this.job);
		this.myInterface.printmefordebug(CompileJob.compileMcfLog);
		
		CompileJob.compileMcfLog.println("making MCF for '%s'".formatted(this.resourcelocation));
		baseScope=currentScope=new Scope(this);
		cursor=0;
		line=1;
		lineStart=0;
		this.matcher=Factories.compileLnStart[0].pattern.matcher(this.content);
		
		List<Statement> compiledLines=this.CompileCodeNoScope();
		
		for(Statement s:compiledLines) {
			//s.printStatementTree(CompileJob.compileMcfLog, 0);//
		}

		CompileJob.compileMcfLog.println("namespace '%s'".formatted(this.baseScope.resBase.namespace));
		this.baseScope.open(job);
		for(Statement s:compiledLines) {
			//s.printStatementTree(CompileJob.compileMcfLog, 0);
			//s.compileMe(this.baseScope.out, this, currentScope);
			this.compileLine(this.baseScope.out, currentScope, s);
		}
		this.baseScope.closeJustMyFiles();
		if (this.job.CLEAN_MCF_SUBDIR) {
			File f=this.job.pathForMcfSubfunctionsDir(this.resourcelocation).toFile();
			//clean old unused functions
			if(f.exists() && f.isDirectory()) {
				for(File sf:f.listFiles()) {
					CompileJob.compileMcfLog.println("cleaning %s;".formatted(sf.getAbsolutePath()));
					sf.delete();
				}
			}
		}
		for(Statement block:compiledLines) if (block instanceof CodeBlock){
			//make sure this is after compiled code; this forces requests to work
			((CodeBlock) block).compileMyBlock(this);
		}
		this.currentScope=this.baseScope;
		this.hasCompiled=true;
	}
	public boolean isHeaderOnly() {
		return isHeaderOnly;
	}
}
