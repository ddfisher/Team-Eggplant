package player.gamer.statemachine.eggplant.completesearch;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.eggplant.misc.ValuedMove;
import player.gamer.statemachine.eggplant.ui.EggplantConfigPanel;
import player.gamer.statemachine.eggplant.ui.EggplantDetailPanel;
import player.gamer.statemachine.eggplant.ui.EggplantMoveSelectionEvent;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import apps.player.config.ConfigPanel;
import apps.player.detail.DetailPanel;

public class MinimaxGamer extends StateMachineGamer {
	private int statesSearched;
	private int leafNodesSearched;
	private int cacheHits, cacheMisses;
	private EggplantConfigPanel config = new EggplantConfigPanel();
	private HashMap<MachineState, ValuedMove> keptCache;
	// TODO: Hashcode is NOT overridden by GDLSentence - this will only check if
	// the sentences are actually the same objects in memory

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// do nothing
		keptCache = new HashMap<MachineState, ValuedMove>();
	}

	/* Implements Minimax search, currently ignores clock */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		HashMap<MachineState, ValuedMove> cache = null;
		if (config.useCache()) {
			// cache = new HashMap<MachineState, ValuedMove>();
			cache = keptCache;
		}

		leafNodesSearched = statesSearched = 0;
		cacheHits = cacheMisses = 0;
		
		ValuedMove result = memoizedMinimax(getStateMachine(), getCurrentState(), getRole(), cache);

		long stop = System.currentTimeMillis();
		//System.out.println("Cache Hit: " + cacheHit + "\tCache Missed: " + cacheMissed);
		notifyObservers(new EggplantMoveSelectionEvent(result.move, result.value, stop-start, statesSearched, leafNodesSearched, cacheHits, cacheMisses));
		return result.move;
	}
	
	private ValuedMove memoizedMinimax(StateMachine machine, MachineState state, Role role, HashMap<MachineState, ValuedMove> cache) throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException {
		if (cache != null) {
			if (cache.containsKey(state)) {
				cacheHits++;
				return cache.get(state);
			}
			else {
				cacheMisses++;
				ValuedMove move = minimax(machine, state, role, cache);
				cache.put(state, move);
				return move;
			}
		}
		else {
			return minimax(machine, state, role, cache);
		}
	}

	private ValuedMove minimax(StateMachine machine, MachineState state, Role role, HashMap<MachineState, ValuedMove> cache) throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException {
		statesSearched++;
		if (machine.isTerminal(state)) {
			leafNodesSearched++;
			return new ValuedMove(machine.getGoal(state, role), null);
		}

		ValuedMove maxMove = new ValuedMove(-1, null);
		List<Move> possibleMoves = machine.getLegalMoves(state, role);
		Collections.shuffle(possibleMoves); // TODO: Remove this line
		for (Move move : possibleMoves) {
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
			Collections.shuffle(jointMoves); // TODO: Remove this line
			int min = 100;
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
				int value = memoizedMinimax(machine, nextState, role, cache).value;
				if (value < min) {
					min = value;
				}
			}
			if (min > maxMove.value) {
				maxMove.value = min;
				maxMove.move = move;
			}
		}
		return maxMove;
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedProverStateMachine();
	}

	@Override
	public String getName() {
		return "Minimax";
	}
	
	@Override
	public DetailPanel getDetailPanel() {
		return new EggplantDetailPanel();
	}

	@Override
	public ConfigPanel getConfigPanel() {
		return config;
	}
}
