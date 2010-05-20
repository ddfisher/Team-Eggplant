package util.statemachine.implementation.propnet;

public class NativeOperator extends Operator {
	
	public NativeOperator() {
		System.loadLibrary("NativeOperator");
	}

	@Override
	public native void transition(boolean[] props);
	
	@Override
	public native void propagateInternal(boolean[] props);

	@Override
	public native void propagate(boolean[] props);
	
	@Override
	public native void propagateTerminalOnly(boolean[] props);
	
	@Override
	public native void propagateLegalOnly(boolean[] props, int role);
	
	@Override
	public native void propagateGoalOnly(boolean[] props, int role);
	
	public native void monteCarlo(boolean[] props);
	
	public native void initMonteCarlo(int[][] legalPropMap, int[] legalInputMap);
}
