package net.mcppc.compiler;

import java.util.ArrayList;

import net.mcppc.compiler.tokens.Statement;
import net.mcppc.compiler.tokens.Statement.Domment;

/**
 * domments are collected by compiler and line start statements
 * @author RadiumE13
 *
 */
public interface DommentCollector{
	public void addDomment(Statement.Domment dom);
	
	public static class DList implements DommentCollector{
		public DList() {}
		public final ArrayList<Domment> list = new ArrayList<Domment>();
		@Override
		public void addDomment(Domment dom) {
			this.list.add(dom);
			
		}
		
	}public static class Dump implements DommentCollector{
		public static final Dump INSTANCE=new Dump();
		
		public Dump() {}
		@Override
		public void addDomment(Domment dom) {
			//do nothing
			
		}
		
	}
}