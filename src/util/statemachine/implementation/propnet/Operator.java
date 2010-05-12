package util.statemachine.implementation.propnet;

public interface Operator {
	public abstract void propagate(boolean[] props);
	public abstract void propagateInternalOnly(boolean[] props);
	public abstract void propagateInternalOnlyTerminal(boolean[] props);
	public abstract void propagateInternalOnlyLegal(int index, boolean[] props);
	public abstract void propagateInternalOnlyGoal(int index, boolean[] props);
}
