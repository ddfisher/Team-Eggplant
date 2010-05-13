package util.statemachine.implementation.propnet;

public abstract class Operator {
	public void propagate(boolean[] props) {
		propagateInternal(props);
		transition(props);
	}
	public abstract void transition(boolean[] props);
	public abstract void propagateInternal(boolean[] props);
	public abstract void propagateTerminalOnly(boolean[] props);
	public abstract void propagateLegalOnly(boolean[] props, int role);
	public abstract void propagateGoalOnly(boolean[] props, int role);
}
