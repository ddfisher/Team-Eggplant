package util.statemachine.implementation.propnet;

public interface Operator {
	public abstract void propagate(boolean[] props);
	public abstract void propagateInternalOnly(boolean[] props);
}
