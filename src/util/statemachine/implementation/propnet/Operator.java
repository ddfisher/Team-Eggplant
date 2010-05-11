package util.statemachine.implementation.propnet;

public interface Operator {
	public abstract boolean[] propagateInternalOnly(boolean[] props);
}
