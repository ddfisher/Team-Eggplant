package util.statemachine.implementation.propnet;

public interface Operator {
	public abstract boolean[] propagate(boolean[] props);
	public abstract boolean[] propagateInternalOnly(boolean[] props);
}
