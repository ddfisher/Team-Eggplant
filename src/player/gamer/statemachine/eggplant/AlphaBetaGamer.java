package player.gamer.statemachine.eggplant;

import java.util.*;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.eggplant.expansion.DepthLimitedExpansionEvaluator;
import player.gamer.statemachine.eggplant.expansion.ExpansionEvaluator;
import player.gamer.statemachine.eggplant.heuristic.Heuristic;
import player.gamer.statemachine.eggplant.heuristic.MobilityHeuristic;
import player.gamer.statemachine.eggplant.heuristic.MobilityTracker;
import player.gamer.statemachine.eggplant.heuristic.MobilityType;
import player.gamer.statemachine.eggplant.misc.CacheValue;
import player.gamer.statemachine.eggplant.misc.TimeUpException;
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

public class AlphaBetaGamer extends StateMachineGamer {
	protected int statesSearched;
	protected int leafNodesSearched;
	protected int cacheHits, cacheMisses;
	protected EggplantConfigPanel config = new EggplantConfigPanel();
	protected ExpansionEvaluator expansionEvaluator;
	protected Heuristic heuristic;
	protected ValuedMove bestWorkingMove;
	protected int maxDepth;
	protected int numPlayers;
	protected int rootDepth;

	private final long GRACE_PERIOD = 200;

	// TODO: Hashcode is NOT overridden by GDLSentence - this will only check if
	// the sentences are actually the same objects in memory

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// initialize cache, evaluators
		rootDepth = 0;
		StateMachine machine = getStateMachine();
		numPlayers = machine.getRoles().size();
		expansionEvaluator = new DepthLimitedExpansionEvaluator(10);
		/*
		 * try { memoizedAlphaBeta(machine, getCurrentState(), getRole(), 0,
		 * 100, Integer.MIN_VALUE, getCache(), timeout - 50, false); }
		 * catch(TimeUpException e){}
		 */
	}

	/* Implements Minimax search, currently ignores clock */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		HashMap<MachineState, CacheValue> cache = new HashMap<MachineState, CacheValue>();

		leafNodesSearched = statesSearched = 0;
		cacheHits = cacheMisses = 0;

		bestWorkingMove = new ValuedMove(-1, getStateMachine().getRandomMove(getCurrentState(), getRole()));
		maxDepth = 1;
		
		heuristic = new MobilityHeuristic(MobilityType.ONE_STEP, numPlayers);

		try {
			getBestMove(getStateMachine(), getCurrentState(), getRole(), 0, 100, 0, cache, timeout - GRACE_PERIOD);
		} catch (TimeUpException ex) { }

		long stop = System.currentTimeMillis();
		notifyObservers(new EggplantMoveSelectionEvent(bestWorkingMove.move, bestWorkingMove.value, stop - start, statesSearched, leafNodesSearched,
				cacheHits, cacheMisses));
		rootDepth++;
		return bestWorkingMove.move;
	}

	protected void getBestMove(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth,
			HashMap<MachineState, CacheValue> cache, long endTime) throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException, TimeUpException {
		statesSearched++;
		List<Move> possibleMoves = machine.getLegalMoves(state, role);
		// Collections.shuffle(possibleMoves); // TODO: Remove this line
		heuristic.update(machine, state, role, alpha, beta, depth, rootDepth);
		for (Move move : possibleMoves) {
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
			// Collections.shuffle(jointMoves); // TODO: Remove this line
			int min = 100;
			int newBeta = beta;
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
				int value = memoizedAlphaBeta(machine, nextState, role, alpha, newBeta, depth + 1, cache, endTime, bestWorkingMove, false).value;
				if (value < min) {
					min = value;
					if (min <= alpha)
						break;
					if (min < newBeta)
						newBeta = min;
				}
			}
			if (min > bestWorkingMove.value) {
				bestWorkingMove.value = min;
				bestWorkingMove.move = move;
				if (bestWorkingMove.value >= beta)
					break;
				if (bestWorkingMove.value > alpha)
					alpha = bestWorkingMove.value;
			}
		}

	}

	protected ValuedMove memoizedAlphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth,
			HashMap<MachineState, CacheValue> cache, long endTime, ValuedMove primary, boolean debug) throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException, TimeUpException {
		if (System.currentTimeMillis() > endTime)
			throw new TimeUpException();
		if (cache != null) {
			if (cache.containsKey(state)) {
				CacheValue cached = cache.get(state);
				if (alpha >= cached.alpha && beta <= cached.beta) {
					if (debug)
						System.out.println("Cache hit: " + cached);
					cacheHits++;
					return cached.valuedMove;
				} else {
					// Alpha-beta bounds are incompatible
					// System.out.println("Alpha: " + alpha + "\tBeta: " + beta
					// + "\tCached Alpha: " + cached.alpha + "\tCached Beta: " +
					// cached.beta);
				}
			}
			cacheMisses++;
			ValuedMove result = alphaBeta(machine, state, role, alpha, beta, depth, cache, endTime, primary, debug);
			if (debug) {
				System.out.println("AlphaBeta returned with " + result + " " + state + " " + cache);
			}
			if (result.move != null)
				cache.put(state, new CacheValue(result, alpha, beta));
			return result;
		} else {
			return alphaBeta(machine, state, role, alpha, beta, depth, cache, endTime, primary, debug);
		}
	}

	private int getHeuristicIndex(long depth) {
		// System.out.println("index: " + (absDepth + depth) % numPlayers);
		// System.out.println("absDepth: " + absDepth + ", depth " + depth +
		// ", numPlayers: " + numPlayers);
		return (int) ((rootDepth + depth) % numPlayers);
	}

	private ValuedMove alphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth,
			HashMap<MachineState, CacheValue> cache, long endTime, ValuedMove primary, boolean debug) throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException, TimeUpException {
		statesSearched++;
		if (debug)
			System.out.println("At depth " + depth + "; searched " + statesSearched + "; searching " + state);
		if (machine.isTerminal(state)) {
			leafNodesSearched++;
			return new ValuedMove(machine.getGoal(state, role), null);
		}

		if (depth > maxDepth) {
			maxDepth = depth;
		}

		if (!expansionEvaluator.eval(machine, state, role, alpha, beta, depth)) { // expansion
																					// should
																					// stop
			if (debug)
				System.out.println("Stopping expanding at depth " + depth);
			return new ValuedMove(heuristic.eval(machine, state, role, alpha, beta, depth, rootDepth, endTime), null);
		}
		ValuedMove maxMove = new ValuedMove(-1, null);
		List<Move> possibleMoves = machine.getLegalMoves(state, role);
		// Collections.shuffle(possibleMoves); // TODO: Remove this line
		heuristic.update(machine, state, role, alpha, beta, depth, rootDepth);
		if (debug)
			System.out.println("At depth " + depth + "; searched " + statesSearched + "; moves: " + possibleMoves);

		// search best move first
		if (primary != null) {
			if (possibleMoves.remove(primary.move)) {
				possibleMoves.add(0, primary.move);
			}
		}

		for (Move move : possibleMoves) {
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
			int min = 100;
			int newBeta = beta;
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
				int value = memoizedAlphaBeta(machine, nextState, role, alpha, newBeta, depth + 1, cache, endTime, null, debug).value;
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

	private HashMap<MachineState, CacheValue> getCache() {
		HashMap<MachineState, CacheValue> cache = null;
		if (config.useCache()) {
			cache = new HashMap<MachineState, CacheValue>();
			// cache = keptCache;
		}
		return cache;
	}

}
