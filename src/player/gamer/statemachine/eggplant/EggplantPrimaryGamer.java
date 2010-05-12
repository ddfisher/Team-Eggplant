package player.gamer.statemachine.eggplant;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.eggplant.expansion.DepthLimitedExpansionEvaluator;
import player.gamer.statemachine.eggplant.expansion.ExpansionEvaluator;
import player.gamer.statemachine.eggplant.heuristic.Heuristic;
import player.gamer.statemachine.eggplant.heuristic.MobilityHeuristic;
import player.gamer.statemachine.eggplant.heuristic.MobilityType;
import player.gamer.statemachine.eggplant.heuristic.NullHeuristic;
import player.gamer.statemachine.eggplant.heuristic.OpponentFocusHeuristic;
import player.gamer.statemachine.eggplant.heuristic.WeightedHeuristic;
import player.gamer.statemachine.eggplant.metagaming.EndgameBook;
import player.gamer.statemachine.eggplant.metagaming.OpeningBook;
import player.gamer.statemachine.eggplant.misc.CacheValue;
import player.gamer.statemachine.eggplant.misc.Log;
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
import util.statemachine.implementation.propnet.BooleanPropNetStateMachine;
import apps.player.config.ConfigPanel;
import apps.player.detail.DetailPanel;

public class EggplantPrimaryGamer extends StateMachineGamer {

	protected int statesSearched;
	protected int leafNodesSearched;
	protected int cacheHits, cacheMisses;
	protected EggplantConfigPanel config = new EggplantConfigPanel();
	protected ExpansionEvaluator expansionEvaluator;
	protected Heuristic heuristic;
	protected OpeningBook openingBook;
	protected EndgameBook endBook;
	protected int maxSearchDepth;
	protected int numPlayers;
	protected int rootDepth;
	protected ValuedMove bestWorkingMove;
	protected int nextStartDepth;
	protected HashMap<MachineState, CacheValue> principalMovesCache;

	private final long GRACE_PERIOD = 200;

	// TODO: Hashcode is NOT overridden by GDLSentence - this will only check if
	// the sentences are actually the same objects in memory

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// initialize cache, evaluators
		rootDepth = 0;
		nextStartDepth = 1;
		numPlayers = getStateMachine().getRoles().size();
		expansionEvaluator = new DepthLimitedExpansionEvaluator(10);
		principalMovesCache = new HashMap<MachineState, CacheValue>();

		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();

//		long start = System.currentTimeMillis();

		/*
		 * openingBook = new OpeningBook(machine, state, role);
		 * openingBook.expandBook(time + (timeout - time) / 2);
		 */

		endBook = new EndgameBook(numPlayers);
//		endBook.buildEndgameBook(machine, state, role, 6, 4, 8, start + (timeout - start) / 2);
		iterativeDeepening(machine, state, role, 0, 100, true, timeout);
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		leafNodesSearched = statesSearched = 0;
		cacheHits = cacheMisses = 0;

		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		bestWorkingMove = new ValuedMove(-2, machine.getRandomMove(state, role));

		try {
			iterativeDeepening(machine, state, role, 0, 100, machine.getLegalMoves(state, role).size() == 1, timeout - GRACE_PERIOD);
			rootDepth++;

			long stop = System.currentTimeMillis();
			notifyObservers(new EggplantMoveSelectionEvent(bestWorkingMove.move, bestWorkingMove.value, stop - start, statesSearched,
					leafNodesSearched, cacheHits, cacheMisses));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return bestWorkingMove.move;
	}

	private Heuristic getPlayerMobilityOpponentFocusHeuristic() {
		return new WeightedHeuristic(new Heuristic[] { new MobilityHeuristic(MobilityType.ONE_STEP, numPlayers),
				new OpponentFocusHeuristic(MobilityType.ONE_STEP, numPlayers) }, new double[] { 0.3, 0.7 });
	}

	protected void iterativeDeepening(StateMachine machine, MachineState state, Role role, int alpha, int beta, boolean preemptiveSearch, long endTime)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int depth;
		/*
		 * int bd = openingBook.bookDepth(); if (rootDepth < bd) { ValuedMove vm
		 * = openingBook.cachedValuedMove(machine, state, role); if (vm != null)
		 * { bestWorkingMove = vm; depth = maxSearchDepth = bd - rootDepth + 1;
		 * } else { depth = maxSearchDepth = 1; // problem if this happens //
		 * System.out.println("openingBook returned null move"); } } else
		 */
		if (principalMovesCache.containsKey(state)) {
			CacheValue cached = principalMovesCache.get(state);
			bestWorkingMove = cached.valuedMove;
			depth = maxSearchDepth = nextStartDepth;
		} else { // this state was not previously explored due to alpha-beta
			// pruning; to ensure non-random moves, start at root
			depth = maxSearchDepth = 1;
		}
		Log.println('i', "Turn " + rootDepth + ", starting search at " + depth + " with best = " + bestWorkingMove + "; end book size = "
				+ endBook.book.size());
		try {
			boolean hasLost = false;
			while (depth <= maxSearchDepth) {
				expansionEvaluator = new DepthLimitedExpansionEvaluator(depth);
				heuristic = getPlayerMobilityOpponentFocusHeuristic();
				int alreadySearched = statesSearched;
				HashMap<MachineState, CacheValue> currentCache = new HashMap<MachineState, CacheValue>();
				ValuedMove move = memoizedAlphaBeta(machine, state, role, alpha, beta, 0, currentCache, principalMovesCache, endTime, false);
				if (!preemptiveSearch) {
					bestWorkingMove = move;
				}
				principalMovesCache = currentCache;
				Log.println('i', "Turn " + rootDepth + ", after depth " + depth + " (abs " + (rootDepth + depth) + "); working = " + move
						+ " searched " + (statesSearched - alreadySearched) + " new states");
				if (move.value == 0) {
					hasLost = true;
					break;
				}
				depth++;
			}
			// Try to make opponents' life hard / force them to respond
			// Iterative blunder approach: give opponent more and more ways to
			// blunder
			if (hasLost && !preemptiveSearch) {
				Log.println('i', "Trying desperate measures...");
				findFarthestLoss(machine, state, role, alpha, beta, depth, endTime, false);
			}
		} catch (TimeUpException ex) {
			if (preemptiveSearch) {
				bestWorkingMove = new ValuedMove(-2, machine.getRandomMove(state, role));
			}
			nextStartDepth = depth - 2;
			if (nextStartDepth < 1)
				nextStartDepth = 1;
		}
	}

	// TODO: Clean up this function
	protected void findFarthestLoss(StateMachine machine, MachineState state, Role role, int alpha, int beta, int firstLosingDepth, long endTime,
			boolean debug) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
		// don't need a heuristic to do anything except report non-loss
		heuristic = new NullHeuristic(50);
		ValuedMove bestMove = null;
		try {
			List<Move> possibleMoves0 = machine.getLegalMoves(state, role);
			expansionEvaluator = new DepthLimitedExpansionEvaluator(firstLosingDepth + 2);
			int minCount = Integer.MAX_VALUE;
			Collections.shuffle(possibleMoves0);
			loop0: for (Move move0 : possibleMoves0) {
				Log.println('f', "Testing move " + move0);
				List<List<Move>> jointMoves0 = machine.getLegalJointMoves(state, role, move0);
				int count = 0;
				for (List<Move> jointMove0 : jointMoves0) {
					MachineState state1 = machine.getNextState(state, jointMove0);
					if (!machine.isTerminal(state1)) {
						List<MachineState> states2 = machine.getNextStates(state1);
						for (MachineState state2 : states2) {
							ValuedMove move = memoizedAlphaBeta(machine, state2, role, 0, 1, 2, new HashMap<MachineState, CacheValue>(),
									principalMovesCache, endTime, false);
							if (move.value == 0) {
								count++;
								Log.println('f', state2 + " leads to loss #" + count);
								if (count > minCount)
									continue loop0;
							}
						}
					} else { // must be a losing joint move; no need to keep
						// searching
						continue loop0;
					}
				}
				if (count < minCount) {
					Log.println('f', "Best = " + move0 + " has " + count + " ways to lose");
					minCount = count;
					bestMove = new ValuedMove(-3, move0);
					if (count == 1) { // we know that every move has at least 1
						// way to lose
						break;
					}
				}
			}
		} catch (TimeUpException ex) {
		}
		if (bestMove != null) {
			bestWorkingMove = bestMove;
			principalMovesCache.put(state, new CacheValue(bestMove, 0, 0));
		}
		throw new TimeUpException();
	}

	protected ValuedMove memoizedAlphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth,
			HashMap<MachineState, CacheValue> cache, HashMap<MachineState, CacheValue> principalMoves, long endTime, boolean debug)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
		if (System.currentTimeMillis() > endTime)
			throw new TimeUpException();
		if (cache != null) {
			if (cache.containsKey(state)) {
				CacheValue cached = cache.get(state);
				if (alpha >= cached.alpha && beta <= cached.beta) {
					Log.println('a', "Cache hit: " + cached);
					cacheHits++;
					return cached.valuedMove;
				}
			}
			cacheMisses++;
			ValuedMove result = alphaBeta(machine, state, role, alpha, beta, depth, cache, principalMoves, endTime, debug);
			if (debug) {
				Log.println('a', "AlphaBeta returned with " + result + " " + state + " " + cache);
			}
			if (result.move != null) {
				cache.put(state, new CacheValue(result, alpha, beta));
			}
			if (result.value == 0 && !endBook.book.containsKey(state)) { // sure
				// loss
				endBook.book.put(state, new CacheValue(result, alpha, beta));
			}
			return result;
		} else {
			return alphaBeta(machine, state, role, alpha, beta, depth, cache, principalMoves, endTime, debug);
		}
	}

	private ValuedMove alphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth,
			HashMap<MachineState, CacheValue> cache, HashMap<MachineState, CacheValue> principalMovesCache, long endTime, boolean debug)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
		statesSearched++;

		ValuedMove endLookup = endBook.endgameValue(state);
		if (endLookup != null) {
			Log.println('a', "At depth " + depth + "; searched " + statesSearched + "; found in EndgameBook");
			return endLookup;
		}

		if (machine.isTerminal(state)) {
			Log.println('a', "At depth " + depth + "; searched " + statesSearched + "; terminal");
			leafNodesSearched++;
			return new ValuedMove(machine.getGoal(state, role), null, rootDepth + depth, true);
		}

		if (depth > maxSearchDepth) {
			maxSearchDepth = depth;
		}

		if (!expansionEvaluator.eval(machine, state, role, alpha, beta, depth)) { // expansion
			// should
			// stop
			Log.println('a', "Heuristic; stopping expanding at depth " + depth);
			return new ValuedMove(heuristic.eval(machine, state, role, alpha, beta, depth, rootDepth, endTime), null, rootDepth + depth, false);
		}
		List<Move> possibleMoves = machine.getLegalMoves(state, role);
		// Collections.shuffle(possibleMoves); // TODO: Remove this line
		heuristic.update(machine, state, role, alpha, beta, depth, rootDepth);
		Log.println('a', "At depth " + depth + "; searched " + statesSearched + "; searching " + state + " ; moves: " + possibleMoves);

		// search best move first
		CacheValue principalMove = principalMovesCache.get(state);
		if (principalMove != null) {
			if (possibleMoves.remove(principalMove.valuedMove.move)) {
				possibleMoves.add(0, principalMove.valuedMove.move);
				Log.println('a', "At depth " + depth + "; searched " + statesSearched + " principal move = " + principalMove.valuedMove.move);
			}
		}

		ValuedMove maxMove = new ValuedMove(-99, null);
		for (Move move : possibleMoves) {
			Log.println('a', "Considering move " + move + " at depth " + depth);
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
			int minValue = 101;
			int minDepth = rootDepth + depth;
			int newBeta = beta;
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
				Log.println('a', "Considering joint move " + jointMove + " with state = " + nextState);
				ValuedMove bestMove = memoizedAlphaBeta(machine, nextState, role, alpha, newBeta, depth + 1, cache, principalMovesCache, endTime,
						debug);
				int bestMoveValue = bestMove.value;
				int bestMoveDepth = bestMove.depth;
				if (bestMoveValue < minValue
						|| (bestMoveValue == minValue && (bestMoveValue >= 50 && bestMoveDepth > minDepth || bestMoveValue <= 50
								&& bestMoveDepth < minDepth))) { // heuristic to
					// break
					// ties
					if (bestMoveValue == minValue) {
						Log.println('a', "Tie broken inside: curr depth " + minDepth + "; best = " + bestMove);
					}
					Log.println('a', "Inside min update: best move = " + bestMove + "; previous min value = " + minValue);
					minValue = bestMoveValue;
					minDepth = bestMoveDepth;
					if (minValue <= alpha)
						break;
					if (minValue < newBeta)
						newBeta = minValue;
				}
			}
			if (maxMove.value < minValue
					|| (maxMove.value == minValue && (maxMove.value >= 50 && minDepth < maxMove.depth || maxMove.value <= 50
							&& minDepth > maxMove.depth))) { // heuristic to
				// break ties
				if (maxMove.value == minValue) {
					Log.println('a', "Tie broken outside: curr depth " + minDepth + "; best = " + maxMove);
				}
				Log.println('a', "Outside max update: new best move = " + new ValuedMove(minValue, move, minDepth) + "; previous max move = "
						+ maxMove);

				maxMove.value = minValue;
				maxMove.depth = minDepth;
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
	public String getName() {
		return "EGGPLANT";
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new BooleanPropNetStateMachine();
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
