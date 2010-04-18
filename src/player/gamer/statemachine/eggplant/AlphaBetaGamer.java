package player.gamer.statemachine.eggplant;

import java.util.HashMap;
import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
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

public class AlphaBetaGamer extends StateMachineGamer {
	private int statesSearched;
	private int leafNodesSearched;
	private int cacheHits, cacheMisses;
	// NOTE: Hashcode is NOT overridden by GDLSentence - this will only check if
	// the sentences are actually the same objects in memory
	private EggplantConfigPanel config = new EggplantConfigPanel();

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// do nothing
	}

	/* Implements Minimax search, currently ignores clock */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		HashMap<MachineState, ValuedMove> cache = null;
		if (config.useCache()) {
			cache = new HashMap<MachineState, ValuedMove>();
		}

		leafNodesSearched = statesSearched = 0;
		cacheHits = cacheMisses = 0;

		ValuedMove result = alphaBeta(getStateMachine(), getCurrentState(), getRole(), 0, 100, cache);

		long stop = System.currentTimeMillis();
		notifyObservers(new EggplantMoveSelectionEvent(result.move, result.value, stop-start, statesSearched, leafNodesSearched, cacheHits, cacheMisses));
		return result.move;
	}

	private ValuedMove memoizedAlphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta,
			HashMap<MachineState, ValuedMove> cache) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (cache != null) {
			if (cache.containsKey(state)) {
				cacheHits++;
				return cache.get(state);
			} else {
				cacheMisses++;
				ValuedMove result = alphaBeta(machine, state, role, alpha, beta, cache);
				cache.put(state, result);
				return result;
			}
		} else {
			return alphaBeta(machine, state, role, alpha, beta, cache);
		}
	}

	private ValuedMove alphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta, HashMap<MachineState, ValuedMove> cache)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		statesSearched++;
		if (machine.isTerminal(state)) {
			leafNodesSearched++;
			return new ValuedMove(machine.getGoal(state, role), null);
		}

		ValuedMove maxMove = new ValuedMove(-1, null);
		List<Move> possibleMoves = machine.getLegalMoves(state, role);
		// Collections.shuffle(possibleMoves); // TODO: Remove this line
		for (Move move : possibleMoves) {
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
			// Collections.shuffle(jointMoves); // TODO: Remove this line
			int min = 100;
			int newBeta = beta;
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
				int value = memoizedAlphaBeta(machine, nextState, role, alpha, newBeta, cache).value;
				if (value < min) {
					min = value;
					if (min <= alpha)
						break;
					if (min < newBeta)
						newBeta = min;
				}
			}
			if (min > maxMove.value) {
				maxMove.value = min;
				maxMove.move = move;
				if (maxMove.value >= beta)
					break;
				if (maxMove.value > alpha)
					alpha = maxMove.value;
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
		return "AlphaBeta";
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
