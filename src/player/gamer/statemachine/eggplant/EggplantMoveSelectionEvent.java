package player.gamer.statemachine.eggplant;

import util.observer.Event;
import util.statemachine.Move;

public final class EggplantMoveSelectionEvent extends Event
{

	private final int statesSearched;
	private final int leavesSearched;
	private final long time;
	private final int value;
	private final Move selection;
	
	public EggplantMoveSelectionEvent(int statesSearched, int leavesSearched, long time, int value, Move selection) {
		this.statesSearched = statesSearched;
		this.leavesSearched = leavesSearched;
		this.time = time;
		this.value = value;
		this.selection = selection;
	}

	public int getStatesSearched() {
		return statesSearched;
	}

	public int getLeavesSearched() {
		return leavesSearched;
	}

	public long getTime() {
		return time;
	}

	public int getValue() {
		return value;
	}

	public Move getSelection() {
		return selection;
	}

}
