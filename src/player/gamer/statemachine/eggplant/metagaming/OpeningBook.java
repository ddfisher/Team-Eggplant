package player.gamer.statemachine.eggplant.metagaming;

import java.util.HashMap;
import java.util.List;

import player.gamer.statemachine.eggplant.heuristic.Heuristic;
import player.gamer.statemachine.eggplant.heuristic.MonteCarloHeuristic;
import player.gamer.statemachine.eggplant.misc.Log;
import player.gamer.statemachine.eggplant.misc.TimeUpException;
import player.gamer.statemachine.eggplant.misc.ValuedMove;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class OpeningBook {
	
	private HashMap<MachineState, ValuedMove> cache;
	private int bookDepth;
	private Heuristic heuristic;
	private StateMachine startMachine;
	private MachineState startState;
	private Role role;
	
	public OpeningBook(StateMachine startMachine, MachineState startState, Role role) {
		this.startMachine = startMachine;
		this.startState = startState;
		this.role = role;
		cache = new HashMap<MachineState, ValuedMove>();
		bookDepth = 0;
	}
	
	public ValuedMove cachedValuedMove(StateMachine machine, MachineState state, Role role) {
		return cache.get(state);
	}
	
	public void expandBook(long endTime) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		iterativeDeepening(startMachine, startState, role, 0, 100, endTime);
	}
	
	private void iterativeDeepening(StateMachine machine, MachineState state, Role role, int alpha, int beta, long endTime)
	  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		// dig until you run out of time
	    try {
		    for (int depth = bookDepth + 1; ; depth++) {
		    	HashMap<MachineState, ValuedMove> tCache = new HashMap<MachineState, ValuedMove>();
		    	heuristic = new MonteCarloHeuristic(2);
		    	memoizedMiniMax(machine, state, role, 0, depth, tCache, endTime);
		    	if (tCache.size() == cache.size()) break;
		    	cache = tCache;
		    	bookDepth = depth;
		    	Log.println('o', "Expanded opening book to depth " + bookDepth + 
		    			" with " + (endTime - System.currentTimeMillis()) + "ms left to search");
		    }
	    } catch (TimeUpException e) {
	    	Log.println('o', "Got to depth " + bookDepth + " in opening expansion");
	    }
	  }
	
	private ValuedMove memoizedMiniMax(StateMachine machine, MachineState state, Role role, int depth,
			int maxDepth, HashMap<MachineState, ValuedMove> cache, long endTime) 
	throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
		if (System.currentTimeMillis() > endTime) throw new TimeUpException();
		// if reached depth limit, just eval and return
		if (depth >= maxDepth) return new ValuedMove(heuristic.eval(machine, state, role, 0, 100, depth, 0, endTime), null);
		if (cache != null) {
			if (cache.containsKey(state)) return cache.get(state);
			ValuedMove result = miniMax(machine, state, role, depth, maxDepth, cache, endTime);
			if (result.move != null) cache.put(state, result);
			return result;
		} else {
			return miniMax(machine, state, role, depth, maxDepth, cache, endTime);
		}
	}
	
	private ValuedMove miniMax(StateMachine machine, MachineState state, Role role, int depth,
			int maxDepth, HashMap<MachineState, ValuedMove> cache, long endTime) 
	throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
		if (machine.isTerminal(state)) return new ValuedMove(machine.getGoal(state, role), null);
		ValuedMove maxMove = new ValuedMove(-1, null);
		List<Move> possibleMoves = machine.getLegalMoves(state, role);
		heuristic.update(machine, state, role, 0, 100, depth, 0);

		for (Move move : possibleMoves) {
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
			int min = 100;
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
				int value = memoizedMiniMax(machine, nextState, role, depth + 1, maxDepth, cache, endTime).value;
				if (value < min) min = value;
			}
			if (min > maxMove.value) {
				maxMove.value = min;
				maxMove.move = move;
			}
		}
		return maxMove;
	}
	
	public int bookDepth() { return bookDepth; }
}
