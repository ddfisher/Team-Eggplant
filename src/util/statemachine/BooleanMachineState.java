package util.statemachine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import util.gdl.grammar.GdlSentence;
import util.propnet.architecture.components.Proposition;

public class BooleanMachineState extends MachineState {
	private boolean[] baseProps;
	private Proposition[] booleanOrdering;
	private Set<GdlSentence> contents;

	public BooleanMachineState(boolean[] baseProps, Proposition[] booleanOrdering) {
		this.baseProps = baseProps;
		this.booleanOrdering = booleanOrdering;
		this.contents = null;
	}
	
	@Override
	public Set<GdlSentence> getContents() {
		// TODO Auto-generated method stub
		if (this.contents == null) {
			this.contents = new HashSet<GdlSentence>();
			for (int i = 0; i < baseProps.length; i++) {
				if (baseProps[i]) {
					this.contents.add(booleanOrdering[i+1].getName().toSentence());
				}
			}
		}
		return this.contents;
	}
	
	public String toString() {
		return getContents().toString();
		//return Arrays.toString(this.baseprops);
	}
	
	public boolean[] getBooleanContents() {
		return baseProps;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(baseProps);
	}
	
	@Override
	public boolean equals(Object o) {
        if (o != null) {
        	if (o instanceof BooleanMachineState) {
        		BooleanMachineState state = (BooleanMachineState) o;
                return Arrays.equals(baseProps, state.baseProps);
        	}
        	else if (o instanceof MachineState) {
        		MachineState state = (MachineState) o;
                return state.getContents().equals(getContents());        		
        	}
        }

        return false;
	}
}
