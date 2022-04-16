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
}
