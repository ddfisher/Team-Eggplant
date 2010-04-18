package player.gamer.statemachine.eggplant;

import util.observer.Event;
import util.statemachine.Move;

public final class EggplantMoveSelectionEvent extends Event
{

	private final Move selection;
	private final int value;
	private final long time;
	private final int statesSearched;
	private final int leavesSearched;
	private final int cacheHits;
	private final int cacheMisses;
	
	public EggplantMoveSelectionEvent(Move selection, int value, long time, int statesSearched, int leavesSearched, int cacheHits, int cacheMisses) {
		super();
		this.selection = selection;
		this.value = value;
		this.time = time;
		this.statesSearched = statesSearched;
		this.leavesSearched = leavesSearched;
		this.cacheHits = cacheHits;
		this.cacheMisses = cacheMisses;
	}

	public Move getSelection() {
		return selection;
	}

	public int getValue() {
		return value;
	}

	public long getTime() {
		return time;
	}

	public int getStatesSearched() {
		return statesSearched;
	}

	public int getLeavesSearched() {
		return leavesSearched;
	}

	public int getCacheHits() {
		return cacheHits;
	}

	public int getCacheMisses() {
		return cacheMisses;
	}

	
}
