package net.mcppc.compiler;

public abstract class CMath {
	public static Number uminus(Number n) {
		if(n instanceof Byte)return -n.byteValue();
		else if(n instanceof Short)return -n.shortValue();
		else if(n instanceof Integer)return -n.intValue();
		else if(n instanceof Long)return -n.longValue();
		else if(n instanceof Float)return -n.floatValue();
		else if(n instanceof Double)return -n.doubleValue();
		else return null;
		
	}
	public static Number pow(Number b,Number e) {
		if(e instanceof Float)return (double)Math.pow(b.doubleValue(), e.doubleValue());
		else if(e instanceof Double)return (double)Math.pow(b.doubleValue(), e.doubleValue());
		else if(b instanceof Byte)return (int)Math.pow(b.doubleValue(), e.doubleValue());
		else if(b instanceof Short)return (int)Math.pow(b.doubleValue(), e.doubleValue());
		else if(b instanceof Integer)return (long)Math.pow(b.doubleValue(), e.doubleValue());
		else if(b instanceof Long)return (long)Math.pow(b.doubleValue(), e.doubleValue());
		else if(b instanceof Float)return (double)Math.pow(b.doubleValue(), e.doubleValue());
		else if(b instanceof Double)return (double)Math.pow(b.doubleValue(), e.doubleValue());
		else return null;
		
	}
	public static boolean isNumberInt(Number n) {
		if(n instanceof Integer)return true;
		if(n instanceof Long)return true;
		if(n instanceof Byte)return true;
		if(n instanceof Short)return true;
		return false;
	}
	public static String getMultiplierFor(double num) {
		//TODO there is a bug in MC: SCI NOT works in tag values but not in multipliers for /data get ... # ;  and /execute store ... # ;
		return removeSciNot(num);
		//String s="%s".formatted(num);
		//return s;
	}
	public static String removeSciNot(double num) {
		int MAX_DIGITS=(int) Math.ceil(Register.SCORE_BITS*Math.log10(2));
		String f="%%-%d.%df".formatted(MAX_DIGITS,MAX_DIGITS);
		StringBuffer s=new StringBuffer(f.formatted(num).trim());
		int point=s.indexOf(".");if(point<0)return s.toString();
		final int SUBS=1;
		int size=s.length();
		while(size>point+SUBS+1 && s.charAt(size-1)=='0') {
			size--;
		}
		//TODO there is a bug in MC: SCI NOT works in tag values but not in multipliers for /data get ... # ;  and /execute store ... # ;
		return s.substring(0, size);
	}
	
	private static int[] cycleElements;
	private static int cycleElementIndex = 0;
	private static boolean cycleFound = false;
	private static final int NEW = 0;
	private static final int PUSHED = 1;
	private static final int POPPED = 2;
	private static final int OLD = 3;
	/**
	 * finds circular loops in a graph
	 * thx nits.kk https://stackoverflow.com/questions/37907339/how-to-detect-circular-reference-in-a-tree
	 * @param graph
	 * @param N
	 * @param u
	 * @param states
	 * @return
	 */
	private static int findCycle(int[][] graph,int N, int u, int[] states){
	    for(int v = 0; v < N; v++){
	        if(graph[u][v] == 1){
	            if(states[v] == PUSHED){
	                // cycle found
	                cycleFound = true;
	                return v;
	            }else if(states[v] == NEW){
	                states[v] = PUSHED;
	                int poppedVertex = findCycle(graph, N, v, states);
	                states[v] = POPPED;
	                if(cycleFound){
	                    if(poppedVertex == u){
	                        cycleElements[cycleElementIndex++] = v;
	                        cycleElements[cycleElementIndex++] = u;
	                        //cycleFound = false;
	                        return -1;
	                    }else{
	                        cycleElements[cycleElementIndex++] = v;
	                        return poppedVertex;
	                    }
	                }
	            }
	        }
	    }
	    return -1;
	}
	public static int[] findCycle(int[][] graph,int N){
	    int[] states = new int[N];
	    cycleFound=false;
	    for(int u=0;u<N;u++) if(states[u]==NEW){
			cycleElements = new int[N];
			cycleElementIndex=0;
		    states[u] = PUSHED;
		    findCycle(graph,N,u,states);
		    if(cycleFound)break;
	    }
	    	
	    
	    if(cycleFound) {
			System.err.println("loop found");
	    	int[] ret=new int[cycleElementIndex];
	    	for(int i = 0; i < cycleElementIndex; i++)ret[i]=cycleElements[i];
	    	return ret;
	    }
	    return null;
	    
	}
}
