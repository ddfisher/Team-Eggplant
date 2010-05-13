package util.statemachine.implementation.propnet;

public abstract class Operator {
	public void propagate(boolean[] props) {
		propagateInternalOnly(props);
		transition(props);
	}
	public abstract void transition(boolean[] props);
	public abstract void propagateInternalOnly(boolean[] props);
	public abstract void propagateInternalOnlyTerminal(boolean[] props);
	public abstract void propagateInternalOnlyLegal(int index, boolean[] props);
	public abstract void propagateInternalOnlyGoal(int index, boolean[] props);
}
