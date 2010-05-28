package util.statemachine.implementation.propnet;


public class NativeOperator extends Operator {
	
	public NativeOperator(String libPath) {
		System.load(libPath);
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
	public native void propagateLegalOnly(boolean[] props, int role, int legalIndex);
	
	@Override
	public native void propagateGoalOnly(boolean[] props, int role);
	
	@Override
	public native boolean monteCarlo(boolean[] props, int maxDepth);
	
	@Override
	public native void initMonteCarlo(int[][] legalPropMap, int[] legalInputMap);
}
