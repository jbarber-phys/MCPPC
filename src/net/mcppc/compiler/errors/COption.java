package net.mcppc.compiler.errors;

import net.mcppc.compiler.CompileJob;

//TODO CompileJob give it a hashmap<COption,Object>
//but let compile job have a hashmap<COption,Treemap<int,Object>> for cursor position info
//give code block a param for cursor start;
public class COption<V> {
	//list all options here:
	public static final COption<Boolean> ALLOW_THREAD_UUID_LOOKUP = new SafeEfficient("allow_uuid_lookup", 3, null, null, -3, false);
	public static final COption<Boolean> ELEVATE_WARNINGS = new SafeEfficient("elevate_warnings", 9, null, null, null, false);
	
	//TODO longmult, ...
	//TODO debug mode
	
	//
	public final String name;
	
	public final boolean canBeCompileLevel;
	public final boolean canBeScopeLevel;
	
	//TODO priority
	private final boolean compilerPriority;
	
	public COption(String name) {
		this.name=name;
		canBeCompileLevel=true;
		canBeScopeLevel=true;
		compilerPriority = false;
	}

	
	public V defaultValue(CompileJob job) {
		return null;
	}
	public boolean doesCompilerGetPriority(CompileJob job) {
		 return job.takePriority || this.compilerPriority;
	}
	
	
	//all comparisons are via name
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if (this==obj) return true;
		if(obj instanceof COption) {
			return this.name.equals(((COption) obj).name);
		}
		return this.name.equals(obj);
	}
	@Override
	public String toString() {
		return this.name;
	}
	
	public static class Basic<V> extends COption<V>{
		private V basicDefault = null;
		public Basic(String name,V defaultv) {
			super(name);
			this.basicDefault = defaultv;
		}
		
		@Override public V defaultValue(CompileJob job) {
			return this.basicDefault;
		}
		
	}
	public static class SafeEfficient extends COption<Boolean>{
		public SafeEfficient(String name) {
			super(name);
		}
		public SafeEfficient(String name, Integer minEfficiency,Integer maxEfficiency, Integer minSafety, 
				Integer maxSafety, boolean invert) {
			super(name);
			this.minEfficiency = minEfficiency;
			this.minSafety = minSafety;
			this.maxEfficiency = maxEfficiency;
			this.maxSafety = maxSafety;
			this.invert = invert;
		}
		private Integer minEfficiency = null;
		private Integer minSafety = null;
		private Integer maxEfficiency = null;
		private Integer maxSafety = null;
		private boolean invert = false;
		@Override public Boolean defaultValue(CompileJob job) {
			boolean v = true;
			if(minEfficiency!=null && job.efficiency < minEfficiency) v=false;
			if(minSafety!=null && job.safety < minSafety) v=false;
			if(maxEfficiency!=null && job.efficiency > maxEfficiency) v=false;
			if(maxSafety!=null && job.safety > maxSafety) v=false;
			return v ^ invert;
		}
		
	}
	//reads command line for option
	public static interface OptionModifier{
		public boolean matches(CompileJob job,String[] args, int start);
		public int add(CompileJob job,String[] args, int start) ;
		public String helpLn() ;
	}
	public static class SafetyFlag implements OptionModifier{
		public SafetyFlag(String name, int safety,String msg) {
			super();
			this.name = name;
			this.safety = safety;
			this.msg=msg;
		} private final String name;private final int safety;private final String msg;
		@Override public boolean matches(CompileJob job, String[] args, int start) {
			return args[start].equals(name);
		} @Override public int add(CompileJob job, String[] args, int start) {
			job.safety=this.safety;
			return start+1;
		}
		@Override
		public String helpLn() {
			return "%s : %s\n".formatted(this.name,this.msg);
		}
		
	}
	public static class EffFlag implements OptionModifier{
		public EffFlag(String name, int eff,String msg) {
			super();
			this.name = name;
			this.eff = eff;
			this.msg=msg;
		} private final String name;private final int eff;private final String msg;
		@Override public boolean matches(CompileJob job, String[] args, int start) {
			return args[start].equals(name);
		} @Override public int add(CompileJob job, String[] args, int start) {
			job.efficiency=this.eff;
			return start+1;
		}
		@Override
		public String helpLn() {
			return "%s : %s\n".formatted(this.name,this.msg);
		}
	}
	public static class PriorityFlag implements OptionModifier{
		public PriorityFlag(String name,String msg) {
			super();
			this.name = name;
			this.msg=msg;
		} private final String name; private final String msg;
		@Override public boolean matches(CompileJob job, String[] args, int start) {
			return args[start].equals(name);
		} @Override public int add(CompileJob job, String[] args, int start) {
			job.takePriority=true;
			return start+1;
		}
		@Override
		public String helpLn() {
			return "%s : %s\n".formatted(this.name,this.msg);
		}
	}
	public static class OptionFlag<V> implements OptionModifier{
		private final String flag;
		private final COption<V> option;
		private final V value;
		private final String msg;

		public OptionFlag(String flag, COption<V> option, V value,String msg) {
			super();
			this.flag = flag;
			this.option = option;
			this.value = value;
			this.msg=msg;
		}

		@Override
		public boolean matches(CompileJob job, String[] args, int start) {
			return args[start].equals(this.flag);
		}
		@Override
		public int add(CompileJob job, String[] args, int start) {
			job.<V>setOption(this.option, this.value);
			return start+1;
		}

		@Override
		public String helpLn() {
			return "%s : %s\n".formatted(this.flag,this.msg);
		}
		
	}
}
