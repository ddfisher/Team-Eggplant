package util.statemachine;

import java.util.HashSet;
import java.util.Set;

import player.gamer.statemachine.eggplant.misc.Log;
import util.gdl.grammar.GdlSentence;
import util.propnet.architecture.components.Proposition;

public class BooleanMachineState extends MachineState {
	private boolean[] baseprops;
	private Proposition[] booleanOrdering;
	private Set<GdlSentence> contents;

	public BooleanMachineState(boolean[] baseProps, Proposition[] booleanOrdering) {
		this.baseprops = baseProps;
		this.booleanOrdering = booleanOrdering;
		this.contents = null;
	}
	
	@Override
	public Set<GdlSentence> getContents() {
		// TODO Auto-generated method stub
		if (this.contents == null) {
			this.contents = new HashSet<GdlSentence>();
			for (int i = 0; i < baseprops.length; i++) {
				if (baseprops[i]) {
					this.contents.add(booleanOrdering[i+1].getName().toSentence());
				}
			}
		}
		return this.contents;
	}
	
	public boolean[] getBooleanContents() {
		return baseprops;
	}
	
	@Override
	public int hashCode() {
		return booleanOrdering.hashCode();
	}

}
