package player.gamer.statemachine.eggplant;

import java.util.HashMap;

import player.gamer.statemachine.eggplant.expansion.DepthLimitedExpansionEvaluator;
import player.gamer.statemachine.eggplant.heuristic.Heuristic;
import player.gamer.statemachine.eggplant.heuristic.MobilityHeuristic;
import player.gamer.statemachine.eggplant.heuristic.MobilityType;
import player.gamer.statemachine.eggplant.misc.CacheValue;
import player.gamer.statemachine.eggplant.misc.TimeUpException;
import player.gamer.statemachine.eggplant.misc.ValuedMove;
import player.gamer.statemachine.eggplant.ui.EggplantMoveSelectionEvent;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class MTDFGamer extends AlphaBetaGamer {
  private final long GRACE_PERIOD = 200;
  // TODO: Hashcode is NOT overridden by GDLSentence - this will only check if
  // the sentences are actually the same objects in memory

  @Override
  public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    long start = System.currentTimeMillis();

    leafNodesSearched = statesSearched = 0;
    cacheHits = cacheMisses = 0;

    bestWorkingMove = new ValuedMove(-1, getStateMachine().getRandomMove(getCurrentState(), getRole()));
    maxDepth = 1;
    
    try {
      iterativeDeepening(getStateMachine(), getCurrentState(), getRole(), 0, 100, timeout - GRACE_PERIOD);
    }
    catch (TimeUpException ex) {
    }
    
    long stop = System.currentTimeMillis();
    notifyObservers(new EggplantMoveSelectionEvent(bestWorkingMove.move, bestWorkingMove.value, stop - start, statesSearched, leafNodesSearched, cacheHits,
        cacheMisses));
    return bestWorkingMove.move;
  }
  
  protected void iterativeDeepening(StateMachine machine, MachineState state, Role role, int alpha, int beta, long endTime)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
    // be courteous: if we only have one move and don't use data from previous searches, bunt
    if (machine.getLegalMoves(state, role).size() == 1) { 
      bestWorkingMove = new ValuedMove(-2, machine.getRandomMove(state, role));
      return;
    }
    for (int depth = 1; depth <= maxDepth; depth++) {
      expansionEvaluator = new DepthLimitedExpansionEvaluator(depth);
      heuristic = new MobilityHeuristic(MobilityType.ONE_STEP, numPlayers);
      int alreadySearched = statesSearched;
      //bestMove = mtdf(machine, state, role, bestMove.value == -1 ? 50 : bestMove.value, 0, new HashMap<MachineState, CacheValue>()    , endTime);
      bestWorkingMove = memoizedAlphaBeta(machine, state, role, alpha, beta, 0, new HashMap<MachineState, CacheValue>(), endTime, bestWorkingMove, false);
      System.out.println("After depth " + depth + "; best = " + bestWorkingMove + " searched " + (statesSearched - alreadySearched) + " new states, " + statesSearched + " total states, " + cacheHits + " cache hits");
    }
  }

  private ValuedMove mtdf(StateMachine machine, MachineState state, Role role, int initialGuess, int depth, HashMap<MachineState, CacheValue> cache, long endTime)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
    ValuedMove currMove = new ValuedMove(initialGuess, null); // initial guess
    int lowerBound = 0;
    int upperBound = 100;
    while (lowerBound < upperBound) {
      int guess = currMove.value;
      if (currMove.value == lowerBound)
        guess++;
      currMove = memoizedAlphaBeta(machine, state, role, guess - 1, guess, depth, cache, endTime, bestWorkingMove, false);
      if (currMove.value < guess) {
        upperBound = currMove.value;
      }
      else {
        lowerBound = currMove.value;
      }
    }
    return currMove;
  }

  /* Alternate implementation of alpha beta
  private ValuedMove memoizedAlphaBeta(
      StateMachine machine,
      MachineState state,
      Role role,
      int alpha,
      int beta,
      HashMap<MachineState, CacheValue> cache)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
    if (cache != null) {
      if (cache.containsKey(state)) {
        // Cache stores lower-bound in alpha, upper-bound in beta
        CacheValue cached = cache.get(state);
        if (cached.alpha >= beta) {
          cacheHits++;
          return cached.valuedMove;
        }
        if (cached.beta <= alpha) {
          cacheHits++;
          return cached.valuedMove;
        }
        if (cached.alpha > alpha)
          alpha = cached.alpha;
        if (cached.beta < beta)
          beta = cached.beta;
      }
      cacheMisses++;

      ValuedMove maxMove = alphaBeta(machine, state, role, alpha, beta, cache);
      if (maxMove.value <= alpha) {
        if (cache.containsKey(state)) {
          cache.get(state).beta = maxMove.value;
        }
        else {
          cache.put(state, new CacheValue(maxMove, -1, maxMove.value));
        }
      }
      else if (maxMove.value > alpha && maxMove.value < beta) {
        if (cache.containsKey(state)) {
          cache.get(state).alpha = cache.get(state).beta = maxMove.value;
        }
        else {
          cache.put(state, new CacheValue(maxMove, maxMove.value, maxMove.value));
        }
      }
      else if (maxMove.value >= beta) {
        if (cache.containsKey(state)) {
          cache.get(state).alpha = maxMove.value;
        }
        else {
          cache.put(state, new CacheValue(maxMove, maxMove.value, 101));
        }
      }
      return maxMove;
    } else {
      return alphaBeta(machine, state, role, alpha, beta, cache);
    }
  }

  private ValuedMove alphaBeta(StateMachine machine,
      MachineState state,
      Role role,
      int alpha,
      int beta,
      HashMap<MachineState, CacheValue> cache)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
    statesSearched++;
    if (machine.isTerminal(state)) {
      leafNodesSearched++;
      return new ValuedMove(machine.getGoal(state, role), null);
    }

    ValuedMove maxMove = new ValuedMove(-1, null);
    if (machine.isTerminal(state)) {
      leafNodesSearched++;
      maxMove = new ValuedMove(machine.getGoal(state, role), null);
    }
    else {
      List<Move> possibleMoves = machine.getLegalMoves(state, role);
      //			Collections.shuffle(possibleMoves); // TODO: Remove this line
      for (Move move : possibleMoves) {
        List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
        //				Collections.shuffle(jointMoves); // TODO: Remove this line
        int min = 101;
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
    }
    return maxMove;
  }
   */

  @Override
  public String getName() {
    return "MTD(f)";
  }

}
