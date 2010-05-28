package util.statemachine.implementation.propnet;

import java.util.Random;


public abstract class Operator {
	protected int[][] legalPropMap;
	protected int[] legalInputMap;
	protected Random rand;
	
	public void propagate(boolean[] props) {
		propagateInternal(props);
		transition(props);
	}
	public abstract void transition(boolean[] props);
	public abstract void propagateInternal(boolean[] props);
	public abstract void propagateTerminalOnly(boolean[] props);
	public abstract void propagateLegalOnly(boolean[] props, int role, int legalIndex);
	public abstract void propagateGoalOnly(boolean[] props, int role);
	public abstract boolean monteCarlo(boolean[] props, int maxDepth);
	public abstract void initMonteCarlo(int[][] legalPropMap, int[] legalInputMap);
}
