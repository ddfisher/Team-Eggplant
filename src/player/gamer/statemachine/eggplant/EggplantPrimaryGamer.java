package player.gamer.statemachine.eggplant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.eggplant.expansion.DepthLimitedExpansionEvaluator;
import player.gamer.statemachine.eggplant.expansion.ExpansionEvaluator;
import player.gamer.statemachine.eggplant.heuristic.Heuristic;
import player.gamer.statemachine.eggplant.heuristic.MobilityHeuristic;
import player.gamer.statemachine.eggplant.heuristic.MobilityType;
import player.gamer.statemachine.eggplant.heuristic.MonteCarloHeuristic;
import player.gamer.statemachine.eggplant.heuristic.OpponentFocusHeuristic;
import player.gamer.statemachine.eggplant.heuristic.PropNetAnalyticsHeuristic;
import player.gamer.statemachine.eggplant.metagaming.EndgameBook;
import player.gamer.statemachine.eggplant.metagaming.OpeningBook;
import player.gamer.statemachine.eggplant.misc.CacheValue;
import player.gamer.statemachine.eggplant.misc.Log;
import player.gamer.statemachine.eggplant.misc.StateMachineFactory;
import player.gamer.statemachine.eggplant.misc.TimeUpException;
import player.gamer.statemachine.eggplant.misc.UpdateMachineException;
import player.gamer.statemachine.eggplant.misc.ValuedMove;
import player.gamer.statemachine.eggplant.ui.EggplantConfigPanel;
import player.gamer.statemachine.eggplant.ui.EggplantDetailPanel;
import player.proxy.WorkingResponseSelectedEvent;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.propnet.BooleanPropNetStateMachine;
import util.statemachine.implementation.propnet.cache.CachedBooleanPropNetStateMachine;
import apps.player.config.ConfigPanel;
import apps.player.detail.DetailPanel;

public class EggplantPrimaryGamer extends StateMachineGamer {

	protected int statesSearched;
	protected int pvStatesSearched;
	protected int leafNodesSearched;
	protected int cacheHits, cacheMisses;
	protected EggplantConfigPanel config = new EggplantConfigPanel();
	protected ExpansionEvaluator expansionEvaluator;
	protected Heuristic heuristic;
	protected OpeningBook openingBook;
	protected EndgameBook endBook;
	protected int maxSearchDepth;
	protected int maxSearchActualDepth;
	protected int numPlayers;
	protected int rootDepth;
	protected ValuedMove bestWorkingMove;
	protected int nextStartDepth;
	protected int minGoal;
	protected int maxGoal;
	protected double avgGoal;
	protected int heuristicUpdateCounter;
	protected HashMap<MachineState, CacheValue> principalMovesCache;
	protected boolean updateStateMachine;
	protected Object updateStateMachineLock;
	
	private final boolean KEEP_TIME = false;
	private final long GRACE_PERIOD = 300;
	private final float PRINCIPAL_MOVE_DEPTH_FACTOR = 0.1f;
	private final float DEPTH_INITIAL_OFFSET = 0.5f;
	private List<String> timeLog = new ArrayList<String>();
	// private final String testers = "mop";
	/*
	 * Heuristic testing codes m - Monte Carlo o - Opponent mobility p - Player
	 * mobility
	 */

	// TODO: Hashcode is NOT overridden by GDLSentence - this will only check if
	// the sentences are actually the same objects in memory

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		Log.println('i', "Starting metagame");
		
//		HeuristicEvaluator evaluator = new HeuristicEvaluator();
//		Heuristic[] hArr = {new SimpleMobilityHeuristic(), new SimpleOpponentMobilityHeuristic()};
//		evaluator.evaluateHeuristic(hArr, 100, getStateMachine(), getCurrentState(), getRole());
//		int turn = 0;
//		double[][] turnValues;
//		int[] turnGoals;
//		do {
//			turnValues = evaluator.getTurnLevels(turn);
//			turnGoals = evaluator.getTurnGoals(turn);
//			System.out.println("Turn " + turn + ": " + Arrays.deepToString(turnValues));
//			System.out.println("Turn " + turn + ": " + Arrays.toString(turnGoals));
//			LogisticClassifier log = new LogisticClassifier();
//			log.learnCoefficients(turnValues, turnGoals);
//			double[] inputs = {8.0, 1.0};
//			System.out.println("Prediction: " + (100*log.value(inputs)));
//			turn++;
//		} while (turnValues != null);
//		System.exit(0);
		
		updateStateMachine = false;
		updateStateMachineLock = new Object();
		Log.println('y', "Before thread init");
		(new Thread() {
			public void run() {
				generateBooleanPropNetStateMachine();
			}
		}).start();
		
		Log.println('y', "After thread init");
		long st, en;
		if (KEEP_TIME) {
			st = System.currentTimeMillis();
		}
		rootDepth = 0;
		nextStartDepth = 1;
		numPlayers = getStateMachine().getRoles().size();
		expansionEvaluator = new DepthLimitedExpansionEvaluator(10);
		principalMovesCache = new HashMap<MachineState, CacheValue>();
		heuristicUpdateCounter = 0;

		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		findGoalBounds(machine, role);

		// minions = new StateMachine[] { machine };// ((BooleanPropNetStateMachine)
		// machine).factor();
		// long start = System.currentTimeMillis();

		/*
		 * openingBook = new OpeningBook(machine, state, role);
		 * openingBook.expandBook(time + (timeout - time) / 2);
		 */

		// ((BooleanPropNetStateMachine) machine).speedTest();
		// minions = new StateMachine[]{machine};
		/*
		if (minions != null && minions.length > 1) {
			Log.println('h', "Switching to factor 0");
			switchStateMachine(minions[0]);
			state = minions[0].getInitialState();
		}
		*/
		
		//((BooleanPropNetStateMachine)machine).speedTest();
		
		endBook = new EndgameBook(numPlayers);
		// endBook.buildEndgameBook(machine, state, role, 6, 4, 8, start +
		// (timeout - start) / 2);
		heuristic = getHeuristic();

		bestWorkingMove = new ValuedMove(-2, machine.getRandomMove(state, role));
		Log.println('y', "Beginning metagame evaluation with machine " + machine);
		while (true) {
			try {
				try {
					heuristic.update(machine, state, role, minGoal - 1, maxGoal + 1,
							0, rootDepth);
					iterativeDeepening(machine, state, role, minGoal - 1, maxGoal + 1,
							true, timeout - GRACE_PERIOD);
					break;
				} catch (Exception ex) {
					if (ex instanceof UpdateMachineException) {
						throw (UpdateMachineException) ex;
					}
					else {
						ex.printStackTrace();
						StateMachineFactory.popMachine();
						throw new UpdateMachineException(false);
					}
				}
			} catch(UpdateMachineException ex) {
				synchronized (updateStateMachineLock) {
					updateStateMachine = false;
				}
				StateMachine newMachine = StateMachineFactory.getCurrentMachine();
				Log.println('y', "Switching to " + newMachine);
				switchStateMachine(newMachine);
				machine = getStateMachine();
				state = getCurrentState();
				role = getRole();
				
				findGoalBounds(machine, role);
				heuristic = getHeuristic();
				Log.println('y', "End switching to " + newMachine);
			}
		}
		if (KEEP_TIME) {
			en = System.currentTimeMillis();
			timeLog.add("Metagaming took " + (en - st) + " ms");
		}
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long start = System.currentTimeMillis();
		leafNodesSearched = statesSearched = pvStatesSearched = 0;
		cacheHits = cacheMisses = 0;
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		bestWorkingMove = new ValuedMove(-2, machine.getRandomMove(state, role));
		if (rootDepth == 0) { // Avoid naively researching
			nextStartDepth += 2;
		}
		try {
			while (true) {
				try {
					if (machine instanceof BooleanPropNetStateMachine) {
						((BooleanPropNetStateMachine) machine).updateSatisfiedLatches(state);
					}
					
					Log.println('i', "State on turn " + rootDepth + " : " + state.getContents());
					try {
						heuristic.update(machine, state, role, minGoal - 1, maxGoal + 1,
								0, rootDepth);
						iterativeDeepening(machine, state, role, minGoal - 1, maxGoal + 1,
								machine.getLegalMoves(state, role).size() == 1, timeout
										- GRACE_PERIOD);
					} catch (Throwable ex) {
						if (ex instanceof UpdateMachineException) {
							throw (UpdateMachineException)ex;
						}
						else {
							ex.printStackTrace();
							StateMachineFactory.popMachine();
							throw new UpdateMachineException(false);
						}
					}
					break;
				} catch (UpdateMachineException ex) {
					synchronized (updateStateMachineLock) {
						updateStateMachine = false;
						StateMachine newMachine = StateMachineFactory.getCurrentMachine();
						Log.println('y', "Switching to " + newMachine);
						switchStateMachine(newMachine);
						machine = getStateMachine();
						state = getCurrentState();
						role = getRole();
						
						findGoalBounds(machine, role);
						heuristic = getHeuristic();
					}
				}
			}
			
			rootDepth++;

			long stop = System.currentTimeMillis();
			if (KEEP_TIME) {
				timeLog.add("Selecting move at depth " + rootDepth + " took "
						+ (stop - start) + " ms");
			}
//			notifyObservers(new EggplantMoveSelectionEvent(
//					bestWorkingMove.move, bestWorkingMove.value, stop - start,
//					statesSearched, leafNodesSearched, cacheHits, cacheMisses));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (bestWorkingMove.move != null)
			return bestWorkingMove.move;
		return new ValuedMove(-2, machine.getRandomMove(state, role)).move;
	}

	protected void iterativeDeepening(StateMachine machine, MachineState state, Role role, int alpha, int beta, boolean preemptiveSearch, 
			long endTime) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, UpdateMachineException {
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
			notifyObservers(new WorkingResponseSelectedEvent(bestWorkingMove.move.getContents().toString()));
			depth = maxSearchDepth = maxSearchActualDepth = nextStartDepth;
		} else { // this state was not previously explored due to alpha-beta
			// pruning; to ensure non-random moves, start at root
			depth = maxSearchDepth = 1;
		}
		Log.println('i', "Turn " + rootDepth + ", starting search at " + depth
				+ " with best = " + bestWorkingMove + "; end book size = "
				+ endBook.book.size());
		boolean hasLost = false, hasWon = bestWorkingMove.value == maxGoal;  //FIXME: is this correct? is this var even set beforehand?!
		int alreadySearched, alreadyPVSearched;
		alreadySearched = alreadyPVSearched = 0;
		long searchStartTime = System.currentTimeMillis();
		long searchEndTime;
		try {
			if (!hasWon) {
				while (depth <= maxSearchDepth) {
					// Check for update to statemachine
					boolean shouldUpdate = false;
					synchronized (updateStateMachineLock) {
						shouldUpdate = updateStateMachine;
					}
					if (shouldUpdate) {
						throw new UpdateMachineException(true);
					}

					expansionEvaluator = new DepthLimitedExpansionEvaluator(depth);
					alreadySearched = statesSearched;
					alreadyPVSearched = pvStatesSearched;
					HashMap<MachineState, CacheValue> currentCache = new HashMap<MachineState, CacheValue>();
					searchStartTime = System.currentTimeMillis();
					ValuedMove move = memoizedAlphaBeta(machine, state, role,
							alpha, beta, 0, DEPTH_INITIAL_OFFSET, currentCache,
							principalMovesCache, endTime);
					if (!preemptiveSearch) {
						bestWorkingMove = move;
						if (bestWorkingMove.move != null)
							notifyObservers(new WorkingResponseSelectedEvent(bestWorkingMove.move.getContents().toString()));
					}
					searchEndTime = System.currentTimeMillis();
					Log.println('i', "Turn " + rootDepth + ", depth " + depth
							+ " (max " + maxSearchActualDepth + "; abs "
							+ (rootDepth + depth) + "); working = " + move
							+ " searched " + (statesSearched - alreadySearched - (pvStatesSearched - alreadyPVSearched))
							+ " new states, "
							+ (pvStatesSearched - alreadyPVSearched)
							+ " additional PV states; "
							+ (int) (1000.0 * (statesSearched - alreadySearched) / (searchEndTime - searchStartTime))
							+ " states / s");
					if (move.value == minGoal) {
						hasLost = true;
						break;
					}

					principalMovesCache = currentCache;

					if (move.value == maxGoal) {
						hasWon = true;
						break;
					}

					depth++;
				}
			}
			// Try to make opponents' life hard / force them to respond
			if (hasLost && !preemptiveSearch) {
				Log.println('i', "Trying desperate measures...");
				if (principalMovesCache.containsKey(state))
					bestWorkingMove = principalMovesCache.get(state).valuedMove;
			} else if (hasWon) {
				Log.println('i', "Found a win at depth " + (rootDepth + depth)
						+ ". Move towards win: " + bestWorkingMove);
// DEBUG		Log.println('i', "Cache (size " + principalMovesCache.size() + "): " + principalMovesCache);
				if (depth == 1) {
					printTimeLog();
				}
			}
			throw new TimeUpException();
		} catch (TimeUpException ex) {
			if (preemptiveSearch) {
				bestWorkingMove = new ValuedMove(-2, machine.getRandomMove(
						state, role));
			}
			searchEndTime = System.currentTimeMillis();
			Log.println('i', "Turn " + rootDepth + ", interrupted at depth " + depth
					+ " (max " + maxSearchActualDepth + "; abs "
					+ (rootDepth + depth) + "); best = " + bestWorkingMove
					+ " searched " + (statesSearched - alreadySearched - (pvStatesSearched - alreadyPVSearched))
					+ " new states, "
					+ (pvStatesSearched - alreadyPVSearched)
					+ " additional PV states, "
					+ (int) (1000.0 * (statesSearched - alreadySearched) / (searchEndTime - searchStartTime))
					+ " states / s");
			nextStartDepth = depth - 2;
			if (nextStartDepth < 1)
				nextStartDepth = 1;
			if (hasLost)
				nextStartDepth = 1;
		} catch (UpdateMachineException ex) {
			nextStartDepth = depth;
			throw ex;
		}
	}

	protected ValuedMove memoizedAlphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta, int actualDepth,
			float pvDepthOffset, HashMap<MachineState, CacheValue> cache, HashMap<MachineState, CacheValue> principalMoves, long endTime) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
		if (System.currentTimeMillis() > endTime)
			throw new TimeUpException();
		if (cache != null) {
			if (cache.containsKey(state)) {
				CacheValue cached = cache.get(state);
				if (alpha >= cached.alpha && beta <= cached.beta) {
//DEBUG				Log.println('a', "Cache hit: " + cached);
					cacheHits++;
					return cached.valuedMove;
				}
			}
			cacheMisses++;
			ValuedMove result = alphaBeta(machine, state, role, alpha, beta,
					actualDepth, pvDepthOffset, cache, principalMoves, endTime);
//DEBUG		Log.println('a', "AlphaBeta returned with " + result + " " + state + " " + cache);
			if (result.move != null) {
				cache.put(state, new CacheValue(result, alpha, beta));
			}
			if (result.value == minGoal && !endBook.book.containsKey(state)) {
				// sure loss
				endBook.book.put(state, new CacheValue(result, alpha, beta));
			} else if (result.value == maxGoal
					&& !endBook.book.containsKey(state)) {
				// sure win - possibly unsafe
//TODO			endBook.book.put(state, new CacheValue(result, alpha, beta));
			}
			return result;
		} else {
			return alphaBeta(machine, state, role, alpha, beta, actualDepth,
					pvDepthOffset, cache, principalMoves, endTime);
		}
	}

	private ValuedMove alphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta, int actualDepth, float pvDepthOffset,
			HashMap<MachineState, CacheValue> cache, HashMap<MachineState, CacheValue> principalMovesCache, long endTime)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
		statesSearched++;

		int perceivedDepth = (int) (actualDepth + pvDepthOffset);

		ValuedMove endLookup = endBook.endgameValue(state, alpha, beta);
		if (endLookup != null) {
//DEBUG		Log.println('a', "At depth " + actualDepth + "; searched " + statesSearched + "; found in EndgameBook");
			return endLookup;
		}

		if (machine.isTerminal(state)) {
//DEBUG		Log.println('a', "At depth " + actualDepth + "; searched " + statesSearched + "; terminal");
			leafNodesSearched++;
			return new ValuedMove(machine.getGoal(state, role), null, rootDepth
					+ actualDepth, true);
		}

		if (actualDepth > maxSearchDepth) {
			maxSearchDepth = actualDepth;
		}

		if (!expansionEvaluator.eval(machine, state, role, alpha, beta, perceivedDepth)) { // expansion should stop
//DEBUG		Log.println('a', "Heuristic; stopping expanding at depth " + actualDepth);
			return new ValuedMove(heuristic.eval(machine, state, role, alpha,
					beta, actualDepth, rootDepth, endTime), null, rootDepth + actualDepth,
					false);
		}

		if (actualDepth > maxSearchActualDepth) {
			maxSearchActualDepth = actualDepth;
		}
		if (actualDepth > perceivedDepth) {
			pvStatesSearched++;
			// Clear cache for pv search
			// cache = null;
		}

		List<Move> possibleMoves = machine.getLegalMoves(state, role);
		
		/*
		if (heuristicUpdateCounter == heuristicUpdateInterval) {
			heuristic.update(machine, state, role, alpha, beta, actualDepth, rootDepth);
			heuristicUpdateCounter = 0;
		}
		else {
			heuristicUpdateCounter++;
		}
		*/
		
//DEBUG	Log.println('a', "At depth " + actualDepth + "; searched " + statesSearched + "; searching " + state + " ; moves: " + possibleMoves);

		// search best move first
		boolean principalMoveFound = false;
		float principalMoveSignificance = 0;
		CacheValue principalMove = principalMovesCache.get(state);
		if (principalMove != null) {
			if (possibleMoves.remove(principalMove.valuedMove.move)) {
				principalMoveFound = true;
				int cachedValue = principalMove.valuedMove.value;
				principalMoveSignificance = cachedValue / (float) (avgGoal);
				possibleMoves.add(0, principalMove.valuedMove.move);
//DEBUG			Log.println('a', "At depth " + actualDepth + "; searched " + statesSearched + " principal move = " + principalMove.valuedMove.move);
			}
		}

		ValuedMove maxMove = new ValuedMove(-3, null);
		for (Move move : possibleMoves) {
//DEBUG		Log.println('a', "Considering move " + move + " at depth " + actualDepth);
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state,
					role, move);
			int minValue = maxGoal + 1;
			int minDepth = rootDepth + actualDepth;
			int newBeta = beta;
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
//DEBUG			Log.println('a', "Considering joint move " + jointMove + " with state = " + nextState);
				ValuedMove bestMove;
				if (principalMoveFound) {
//DEBUG				Log.println('d', "NUIDS : At offset " + pvDepthOffset + "; " + nextState);
					bestMove = memoizedAlphaBeta(machine, nextState, role, alpha, newBeta, actualDepth + 1, pvDepthOffset - principalMoveSignificance
							* PRINCIPAL_MOVE_DEPTH_FACTOR, cache, principalMovesCache, endTime);
				} else {
					bestMove = memoizedAlphaBeta(machine, nextState, role, alpha, newBeta, actualDepth + 1, pvDepthOffset, cache,
							principalMovesCache, endTime);
				}
				if (bestMove.value < minValue) {
//DEBUG				Log.println('a', "Inside min update: best move = " + bestMove + "; previous min value = " + minValue);
					minValue = bestMove.value;
					minDepth = bestMove.depth;
					if (minValue <= alpha)
						break;
					if (minValue < newBeta)
						newBeta = minValue;
				}
			}
			if (principalMoveFound) {
				principalMoveFound = false;
			}
			if (maxMove.value < minValue) {
//DEBUG			Log.println('a', "Outside max update: new best move = " + new ValuedMove(minValue, move, minDepth) + "; previous max move = " + maxMove);
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
	
	public void generateBooleanPropNetStateMachine() {
		Log.println('y', "Threaded BPNSM compute started " + System.currentTimeMillis());
		CachedBooleanPropNetStateMachine bpnet = new CachedBooleanPropNetStateMachine(getRoleName());
		bpnet.initialize(getMatch().getDescription());
		Log.println('y', "Threaded BPNSM compute ended " + System.currentTimeMillis());
	}
	
	public void signalUpdateMachine() {
		synchronized (updateStateMachineLock) {
			updateStateMachine = true;
		}
	}

	private void findGoalBounds(StateMachine machine, Role role) {
		int[] values;
		if (machine instanceof BooleanPropNetStateMachine) { 
			values = ((BooleanPropNetStateMachine)machine).getGoalValues(role);
		}
		else {
			values = new int[]{0, 100};
		}
		minGoal = values[0];
		maxGoal = values[values.length - 1];
		int total = 0;
		for (int i = 0; i < values.length; i++)
			total += values[i];
		avgGoal = total / (double) values.length;
		Log.println('i', "Min: " + minGoal + ", max: " + maxGoal + ", avg: "
				+ avgGoal);
	}
	
	private Heuristic getHeuristic() {
		MobilityHeuristic mob = new MobilityHeuristic(MobilityType.ONE_STEP, numPlayers);
		OpponentFocusHeuristic opp = new OpponentFocusHeuristic(MobilityType.ONE_STEP, numPlayers);
		mob.setAvgGoal((int)avgGoal, minGoal, maxGoal);
		opp.setAvgGoal((int)avgGoal, minGoal, maxGoal);
		return new PropNetAnalyticsHeuristic(minGoal, maxGoal, new Heuristic[] {
				mob, opp, new MonteCarloHeuristic(10, (int)avgGoal)}, new double[] {0.2, 0.2, 0.6});
	}


	public void printTimeLog() {
		if (!KEEP_TIME) {
			Log.println('i', "No timing information kept (turn on KEEP_TIME)");
			return;
		}
		Log.println('i', "\nTiming info:\n---");
		for (String tim : timeLog) {
			Log.println('i', "   " + tim);
		}
		Log.println('i', "---\n");
	}

	@Override
	public String getName() {
		return "EGGPLANT";
	}

	@Override
	public StateMachine getInitialStateMachine() {
		StateMachineFactory.reset();
		StateMachineFactory.setDelegate(this);
		return StateMachineFactory.getCurrentMachine();
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new EggplantDetailPanel();
	}

	@Override
	public ConfigPanel getConfigPanel() {
		return config;
	}

	class HeuristicStats {
		private List<Integer> evals;
		private List<Double> winFractions;
		private boolean cachedMean, cachedDev;
		private double mean, stDev;

		public HeuristicStats() {
			evals = new ArrayList<Integer>();
			cachedMean = cachedDev = false;
		}

		public void update(int eval) {
			update(eval, .5);
		}

		public void update(int eval, double winFraction) {
			evals.add(eval);
			winFractions.add(winFraction);
			cachedMean = cachedDev = false;
		}

		public double mean() {
			if (!cachedMean)
				computeMean();
			return mean;
		}

		public double standardDeviation() {
			if (!cachedDev)
				computeStDev();
			return stDev;
		}

		public double correlation() {
			return 0.0;
		}

		private void computeMean() {
			int total = 0;
			for (Integer i : evals)
				total += i;
			mean = total / (double) evals.size();
		}

		private void computeStDev() {
			if (!cachedMean)
				computeMean();
			double total = 0;
			for (Integer i : evals) {
				double diff = i - mean;
				total += diff * diff;
			}
			stDev = Math.sqrt(total / evals.size());
		}
	}
}
