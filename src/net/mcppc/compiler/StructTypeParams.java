package net.mcppc.compiler;

public interface StructTypeParams{

	@Override public boolean equals(Object other) ;
	public static final class Blank implements StructTypeParams{
		public Blank(){}
		@Override public boolean equals(Object other) {
			return other instanceof Blank;
		}
	}
	public static class MembType implements StructTypeParams{
		public final VarType myType;
		public MembType(VarType vt) {
			myType=vt;
		}
		@Override public boolean equals(Object other) {
			return other instanceof MembType && this.myType.equals(((MembType)other).myType);
		}
	}
}